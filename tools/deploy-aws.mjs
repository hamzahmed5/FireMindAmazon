#!/usr/bin/env node
/**
 * Deploy the FireMind backend to AWS: Lambda + API Gateway (HTTP API).
 *
 *   node tools/deploy-aws.mjs --package-only   # build the zip, touch nothing in AWS
 *   node tools/deploy-aws.mjs                  # create/update everything, print the URL
 *
 * What it creates, all idempotently (re-running updates in place):
 *   1. an IAM role whose only AWS permission is bedrock:InvokeModel,
 *   2. a Lambda function running the same code as the local server,
 *   3. an HTTP API in front of it, throttled and concurrency-capped.
 *
 * Credentials come from the function's execution role, so the cloud deployment
 * has NO AWS keys in it at all, and the hourly `aws login` refresh that the
 * local setup needs does not apply. The account's Bedrock quota still does: a
 * deployment does not buy tokens.
 *
 * Requires the AWS CLI v2 (`aws --version`) with credentials that may create
 * IAM roles, Lambda functions and API Gateway APIs. Everything is reversible:
 * deleting the role, the function and the API removes every trace.
 */
import { execFileSync } from "node:child_process";
import { mkdirSync, readFileSync, writeFileSync, existsSync, statSync } from "node:fs";
import { dirname, join, relative } from "node:path";
import { fileURLToPath } from "node:url";

import { createZip } from "./lib/zip.mjs";

const REPO = join(dirname(fileURLToPath(import.meta.url)), "..");
const BACKEND = join(REPO, "backend");
const OUT_DIR = join(REPO, "build", "lambda");
const ZIP_PATH = join(OUT_DIR, "function.zip");

const FUNCTION_NAME = process.env.FIREMIND_FUNCTION_NAME ?? "firemind-backend";
const ROLE_NAME = process.env.FIREMIND_ROLE_NAME ?? "firemind-lambda-role";
const POLICY_NAME = "firemind-bedrock-invoke";
const API_NAME = process.env.FIREMIND_API_NAME ?? "firemind-api";
const RUNTIME = process.env.FIREMIND_RUNTIME ?? "nodejs20.x";
const ARCH = process.env.FIREMIND_ARCH ?? "arm64";
const TIMEOUT = 30; // the model call itself allows up to 12s
const MEMORY = 256;

/** Files that go into the deployment package - an allowlist, never a glob. */
const PACKAGE_FILES = [
  "lambda.mjs",
  "package.json",
  "lib/ai.js",
  "lib/bedrock.js",
  "lib/catalog.js",
  "lib/console.js",
  "lib/env.js",
  "lib/router.js",
  "data/catalog.json",
];

const PACKAGE_ONLY = process.argv.includes("--package-only");
const SKIP_CAP = process.argv.includes("--no-cap");

function aws(args, { allowFailure = false, input } = {}) {
  try {
    return execFileSync("aws", args, {
      encoding: "utf8",
      input,
      stdio: ["pipe", "pipe", "pipe"],
    }).trim();
  } catch (err) {
    const message = (err.stderr || err.message || "").toString().trim();
    if (allowFailure) return { failed: true, message };
    throw new Error(`aws ${args.slice(0, 3).join(" ")} failed:\n${message}`);
  }
}

const jq = (args) => JSON.parse(aws([...args, "--output", "json"]));

function step(text) {
  console.log(`\n== ${text}`);
}

// --------------------------------------------------------------------------
// 1. Package
// --------------------------------------------------------------------------
function buildPackage() {
  step("building the deployment package");

  const entries = PACKAGE_FILES.map((name) => {
    const abs = join(BACKEND, name);
    if (!existsSync(abs)) throw new Error(`missing package file: ${name}`);
    // A guard with teeth: the deployment package must never contain secrets.
    if (/(^|\/)\.env/.test(name)) throw new Error(`refusing to package ${name}`);
    return { name, data: readFileSync(abs) };
  });

  const zip = createZip(entries);
  mkdirSync(OUT_DIR, { recursive: true });
  writeFileSync(ZIP_PATH, zip);

  for (const name of PACKAGE_FILES) {
    const size = statSync(join(BACKEND, name)).size;
    console.log(`   + ${name} (${size} B)`);
  }
  console.log(`   = ${relative(REPO, ZIP_PATH)} (${(zip.length / 1024).toFixed(1)} KiB)`);
  console.log("   no .env, no credentials: the Lambda role supplies them");
  return zip;
}

// --------------------------------------------------------------------------
// 2. IAM role
// --------------------------------------------------------------------------
function ensureRole(account) {
  step(`IAM role ${ROLE_NAME}`);
  const existing = aws(["iam", "get-role", "--role-name", ROLE_NAME, "--query", "Role.Arn", "--output", "text"], {
    allowFailure: true,
  });
  if (typeof existing === "string" && existing.startsWith("arn:")) {
    console.log(`   exists: ${existing}`);
    return existing;
  }

  const trustPath = join(OUT_DIR, "trust-policy.json");
  writeFileSync(
    trustPath,
    JSON.stringify(
      {
        Version: "2012-10-17",
        Statement: [
          {
            Effect: "Allow",
            Principal: { Service: "lambda.amazonaws.com" },
            Action: "sts:AssumeRole",
          },
        ],
      },
      null,
      2
    )
  );
  const created = aws([
    "iam",
    "create-role",
    "--role-name",
    ROLE_NAME,
    "--description",
    "FireMind backend execution role: logs plus bedrock:InvokeModel, nothing else",
    "--assume-role-policy-document",
    `file://${trustPath.replace(/\\/g, "/")}`,
    "--query",
    "Role.Arn",
    "--output",
    "text",
  ]);
  console.log(`   created: ${created}`);

  aws([
    "iam",
    "attach-role-policy",
    "--role-name",
    ROLE_NAME,
    "--policy-arn",
    "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole",
  ]);
  console.log("   attached: AWSLambdaBasicExecutionRole (CloudWatch logs)");

  // The whole point of the role: exactly one AWS service action, scoped to
  // whichever model the function is configured with.
  const policyPath = join(OUT_DIR, "bedrock-policy.json");
  writeFileSync(
    policyPath,
    JSON.stringify(
      {
        Version: "2012-10-17",
        Statement: [
          {
            Effect: "Allow",
            Action: ["bedrock:InvokeModel"],
            Resource: [
              `arn:aws:bedrock:*::foundation-model/${modelId()}`,
              `arn:aws:bedrock:*:${account}:inference-profile/*`,
            ],
          },
        ],
      },
      null,
      2
    )
  );
  aws([
    "iam",
    "put-role-policy",
    "--role-name",
    ROLE_NAME,
    "--policy-name",
    POLICY_NAME,
    "--policy-document",
    `file://${policyPath.replace(/\\/g, "/")}`,
  ]);
  console.log(`   inline policy ${POLICY_NAME}: bedrock:InvokeModel only`);
  return created;
}

function modelId() {
  if (process.env.BEDROCK_MODEL_ID) return process.env.BEDROCK_MODEL_ID;
  for (const file of [".env", ".env.example"]) {
    const path = join(BACKEND, file);
    if (!existsSync(path)) continue;
    const match = readFileSync(path, "utf8").match(/^BEDROCK_MODEL_ID=(.+)$/m);
    if (match) return match[1].trim();
  }
  return "us.anthropic.claude-haiku-4-5-20251001-v1:0";
}

// --------------------------------------------------------------------------
// 3. Lambda function
// --------------------------------------------------------------------------
function ensureFunction(roleArn) {
  step(`Lambda function ${FUNCTION_NAME}`);
  const exists = aws(
    ["lambda", "get-function", "--function-name", FUNCTION_NAME, "--query", "Configuration.FunctionArn", "--output", "text"],
    { allowFailure: true }
  );

  if (typeof exists === "string" && exists.startsWith("arn:")) {
    console.log(`   exists: ${exists}`);
    aws(["lambda", "update-function-code", "--function-name", FUNCTION_NAME, "--zip-file", `fileb://${ZIP_PATH.replace(/\\/g, "/")}`]);
    console.log("   code updated");
    aws(["lambda", "wait", "function-updated", "--function-name", FUNCTION_NAME]);
  } else {
    aws([
      "lambda",
      "create-function",
      "--function-name",
      FUNCTION_NAME,
      "--runtime",
      RUNTIME,
      "--architectures",
      ARCH,
      "--handler",
      "lambda.handler",
      "--role",
      roleArn,
      "--timeout",
      String(TIMEOUT),
      "--memory-size",
      String(MEMORY),
      "--zip-file",
      `fileb://${ZIP_PATH.replace(/\\/g, "/")}`,
      "--query",
      "FunctionArn",
      "--output",
      "text",
    ]);
    console.log("   created");
    aws(["lambda", "wait", "function-active", "--function-name", FUNCTION_NAME]);
  }

  aws([
    "lambda",
    "update-function-configuration",
    "--function-name",
    FUNCTION_NAME,
    "--runtime",
    RUNTIME,
    "--timeout",
    String(TIMEOUT),
    "--memory-size",
    String(MEMORY),
    "--environment",
    JSON.stringify({
      Variables: {
        // Credentials are deliberately absent: the role provides them, and
        // AWS_REGION is absent because Lambda REJECTS it as a reserved key
        // (InvalidParameterValueException) - the runtime always sets it to the
        // function's own region, which is exactly what the signer needs.
        BEDROCK_MODEL_ID: modelId(),
        BEDROCK_TIMEOUT_MS: "12000",
      },
    }),
  ]);
  aws(["lambda", "wait", "function-updated", "--function-name", FUNCTION_NAME]);
  console.log(`   config: ${RUNTIME}, ${ARCH}, ${TIMEOUT}s, ${MEMORY} MiB, model ${modelId()}`);

  if (!SKIP_CAP) {
    // An open HTTPS endpoint that spends money must not be able to spend much.
    // Best effort, not a hard requirement: a brand-new account can have an
    // account concurrency limit of only 10, and reserving 2 would push the
    // unreserved remainder below AWS's minimum of 10. The stage throttle below
    // is the guard that always exists.
    const cap = aws(
      [
        "lambda",
        "put-function-concurrency",
        "--function-name",
        FUNCTION_NAME,
        "--reserved-concurrent-executions",
        "2",
      ],
      { allowFailure: true }
    );
    if (typeof cap === "string") {
      console.log("   reserved concurrency capped at 2");
    } else {
      const reason = cap.message.split("\n")[0].replace(/^aws: \[ERROR\]: /, "");
      console.log(`   concurrency cap not applied - ${reason}`);
      console.log("   (stage throttling below is still in force)");
    }
  }

  return aws(["lambda", "get-function", "--function-name", FUNCTION_NAME, "--query", "Configuration.FunctionArn", "--output", "text"]);
}

// --------------------------------------------------------------------------
// 4. API Gateway
// --------------------------------------------------------------------------
function ensureApi(functionArn, region, account) {
  step(`HTTP API ${API_NAME}`);
  const existing = aws(
    ["apigatewayv2", "get-apis", "--query", `Items[?Name=='${API_NAME}'].ApiId`, "--output", "text"],
    { allowFailure: true }
  );

  let apiId = typeof existing === "string" ? existing.trim() : "";
  if (apiId && apiId !== "None") {
    console.log(`   exists: ${apiId}`);
  } else {
    // Quick create wires the $default route, the AWS_PROXY integration and the
    // stage in one call, so there is no half-built gateway to reason about.
    apiId = aws([
      "apigatewayv2",
      "create-api",
      "--name",
      API_NAME,
      "--protocol-type",
      "HTTP",
      "--target",
      functionArn,
      "--query",
      "ApiId",
      "--output",
      "text",
    ]);
    console.log(`   created: ${apiId}`);
  }

  const statement = aws(
    [
      "lambda",
      "add-permission",
      "--function-name",
      FUNCTION_NAME,
      "--statement-id",
      "firemind-apigateway-invoke",
      "--action",
      "lambda:InvokeFunction",
      "--principal",
      "apigateway.amazonaws.com",
      "--source-arn",
      `arn:aws:execute-api:${region}:${account}:${apiId}/*`,
      "--query",
      "Statement",
      "--output",
      "text",
    ],
    { allowFailure: true }
  );
  console.log(
    typeof statement === "string" && statement.startsWith("arn:")
      ? "   invoke permission granted"
      : "   invoke permission already present"
  );

  // Throttle the stage: anyone who finds the URL cannot run up a bill.
  aws([
    "apigatewayv2",
    "update-stage",
    "--api-id",
    apiId,
    "--stage-name",
    "$default",
    "--default-route-settings",
    "ThrottlingBurstLimit=10,ThrottlingRateLimit=5",
  ]);
  console.log("   stage throttled: 5 req/s steady, burst 10");

  return `https://${apiId}.execute-api.${region}.amazonaws.com`;
}

// --------------------------------------------------------------------------
// main
// --------------------------------------------------------------------------
async function main() {
  const zip = buildPackage();
  if (PACKAGE_ONLY) {
    console.log("\n--package-only: nothing in AWS was touched.");
    return;
  }

  step("checking AWS credentials");
  const identity = jq(["sts", "get-caller-identity"]);
  const region = process.env.AWS_REGION ?? process.env.AWS_DEFAULT_REGION ?? "us-east-1";
  console.log(`   account ${identity.Account}`);
  console.log(`   region  ${region}`);
  if (identity.Arn?.endsWith(":root")) {
    console.log("   note: you are using the account ROOT user; an IAM user is safer");
  }

  const roleArn = ensureRole(identity.Account);
  console.log("   (IAM role changes can take ~10s to become usable)");
  execFileSync(process.execPath, ["-e", "setTimeout(()=>{}, 10000)"]);
  const functionArn = ensureFunction(roleArn);
  const url = await ensureApi(functionArn, region, identity.Account);

  step("deployed");
  console.log(`   URL: ${url}`);
  console.log(`   health: ${url}/api/health`);
  console.log(`   console: ${url}/`);

  console.log("\n== smoke test (no credentials needed)");
  for (const [label, path, init] of [
    ["health", "/api/health", {}],
    [
      "recommend",
      "/api/recommend",
      {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ query: "something funny and short for the family" }),
      },
    ],
  ]) {
    try {
      const resp = await fetch(`${url}${path}`, init);
      const text = await resp.text();
      console.log(`   ${label}: ${resp.status} ${text.slice(0, 160)}`);
    } catch (err) {
      console.log(`   ${label}: request failed - ${err.message}`);
    }
  }

  console.log(`
Point the app at it with:

  FIREMIND_BACKEND_URL="${url}" ./gradlew assembleDebug

That URL is HTTPS, so no cleartext exception is needed.`);
}

main().catch((err) => {
  console.error(`\ndeploy failed: ${err.message}`);
  process.exit(1);
});
