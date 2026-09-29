/**
 * Bedrock path tests.
 *
 * The AI request path is exercised end-to-end: the real server, the real
 * orchestrator (ai.js), and the real SigV4 signer all run, with only the
 * AWS service replaced by a local stub that speaks the Converse API.
 *
 * That proves signature and request construction, response parsing and
 * validation, and every degradation path - none of which requires AWS
 * credentials.
 *
 * Signature correctness itself is pinned separately against AWS's own
 * signer by the known-answer test below. What still cannot be proven here is
 * that a given AWS account has Bedrock model access enabled; see the README's
 * Known limitations section.
 */
import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { createServer } from "node:http";
import { spawn } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { sigv4Headers, BedrockClient, canonicalUri } from "../lib/bedrock.js";
import { catalog } from "../lib/catalog.js";

const here = path.dirname(fileURLToPath(import.meta.url));
const API_PORT = 8237;
const STUB_PORT = 8238;

let child;
let stub;
let stubMode = "ok";
const calls = [];

const [A, B, C] = catalog;

/** Converse API response envelope around a model text payload. */
function envelope(text) {
  return {
    output: { message: { role: "assistant", content: [{ text }] } },
    stopReason: "end_turn",
    usage: { inputTokens: 10, outputTokens: 20 },
  };
}

/** Model text keyed off which system prompt the orchestrator sent. */
function modelTextFor(body) {
  const system = body?.system?.[0]?.text ?? "";
  if (system.includes("Summarize a movie")) {
    return "A keeper faces the tide and the record it left behind. Spoiler-free by design.";
  }
  if (system.includes("pick titles similar")) {
    return JSON.stringify({
      recommendations: [{ id: C.id, reason: "Shares the source mood.", summary: "Similar pick." }],
    });
  }
  return JSON.stringify({
    recommendations: [
      // Deliberately wrong title: the API must return catalog metadata, not model prose.
      { id: A.id, title: "Totally Invented Title", reason: "Matches your stated mood.", summary: "One tidy sentence." },
      { id: B.id, reason: "Also fits the runtime cap.", summary: "Another tidy sentence." },
      { id: "fv999", reason: "I made this up.", summary: "Not in the catalog." },
    ],
  });
}

async function call(method, urlPath, body) {
  const resp = await fetch(`http://127.0.0.1:${API_PORT}${urlPath}`, {
    method,
    headers: body ? { "content-type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  return { status: resp.status, json: await resp.json() };
}

before(async () => {
  stub = createServer((req, res) => {
    const chunks = [];
    req.on("data", (c) => chunks.push(c));
    req.on("end", () => {
      const raw = Buffer.concat(chunks).toString("utf8");
      calls.push({ path: req.url, headers: req.headers, raw });

      if (stubMode === "http-500") {
        res.writeHead(500, { "content-type": "application/json" });
        return res.end(JSON.stringify({ message: "Internal server error" }));
      }
      let text;
      if (stubMode === "prose") text = "Sure! Try The Lighthouse Algorithm, it is great.";
      else if (stubMode === "empty") text = "";
      else text = modelTextFor(JSON.parse(raw));

      res.writeHead(200, { "content-type": "application/json" });
      res.end(JSON.stringify(envelope(text)));
    });
  });
  await new Promise((r) => stub.listen(STUB_PORT, r));

  child = spawn(process.execPath, [path.join(here, "..", "server.js")], {
    env: {
      ...process.env,
      PORT: String(API_PORT),
      AWS_REGION: "us-east-1",
      AWS_ACCESS_KEY_ID: "AKIDEXAMPLE",
      AWS_SECRET_ACCESS_KEY: "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY",
      BEDROCK_ENDPOINT: `http://127.0.0.1:${STUB_PORT}`,
    },
    stdio: "ignore",
  });

  for (let i = 0; i < 60; i++) {
    try {
      await call("GET", "/api/health");
      return;
    } catch {
      await new Promise((r) => setTimeout(r, 100));
    }
  }
  throw new Error("server did not start");
});

after(() => {
  child?.kill();
  stub?.close();
});

// ---------------------------------------------------------------------------
// Signature construction
// ---------------------------------------------------------------------------

test("sigv4 signatures are scoped to the exact body being sent", () => {
  const body = JSON.stringify({ messages: [{ role: "user" }] });
  const headers = sigv4Headers({
    host: "bedrock-runtime.us-east-1.amazonaws.com",
    path: "/model/anthropic.claude-3-haiku-20240307-v1:0/converse",
    body,
    region: "us-east-1",
    accessKey: "AKIDEXAMPLE",
    secretKey: "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY",
    amzDate: "20260101T000000Z",
    dateStamp: "20260101",
  });

  assert.match(
    headers.Authorization,
    /^AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE\/20260101\/us-east-1\/bedrock\/aws4_request, SignedHeaders=content-type;host;x-amz-content-sha256;x-amz-date, Signature=[0-9a-f]{64}$/
  );
  assert.equal(
    headers["x-amz-content-sha256"],
    createHash("sha256").update(body).digest("hex"),
    "payload hash must cover the transmitted body"
  );
  assert.equal(headers.host, "bedrock-runtime.us-east-1.amazonaws.com");
});

// ---------------------------------------------------------------------------
// Known answer, produced by AWS's own signer
//
// The constants below come from backend/tools/sigv4-oracle.py, which builds
// and signs the identical Converse request with botocore (the library behind
// the AWS CLI), on a pinned clock. Re-derive with:
//
//   python backend/tools/sigv4-oracle.py --json | node backend/tools/sigv4-crosscheck.mjs
//
// This is the only test here that can detect a signature the service would
// reject: a signature can be well-formed, deterministic and self-consistent
// and still be wrong.
// ---------------------------------------------------------------------------
const ORACLE_FIXTURE = {
  host: "bedrock-runtime.us-east-1.amazonaws.com",
  // Wire path: encoded once, exactly as sent over the network.
  path: "/model/anthropic.claude-3-haiku-20240307-v1%3A0/converse",
  body: '{"system": [{"text": "You are FireMind, a TV viewing companion."}], "messages": [{"role": "user", "content": [{"text": "hi"}]}], "inferenceConfig": {"maxTokens": 700, "temperature": 0.4}}',
  region: "us-east-1",
  accessKey: "AKIDEXAMPLE",
  secretKey: "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY",
  amzDate: "20260101T120000Z",
  dateStamp: "20260101",
};
const ORACLE_PAYLOAD_SHA = "ee03f6968b2db7723d3e37abffa903693512be8f32efeffe9b6c807f8185d2d2";
const ORACLE_SIGNATURE = "4b976f7b5a9a72d838f818daff9682383e632648df8e157151556450856efb48";

test("sigv4 signature matches the one AWS's own signer produces", () => {
  const headers = sigv4Headers(ORACLE_FIXTURE);
  assert.equal(headers["x-amz-content-sha256"], ORACLE_PAYLOAD_SHA);
  assert.equal(
    headers.Authorization,
    `AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20260101/us-east-1/bedrock/aws4_request, ` +
      `SignedHeaders=content-type;host;x-amz-content-sha256;x-amz-date, ` +
      `Signature=${ORACLE_SIGNATURE}`
  );
});

test("canonical URI is encoded twice while the wire path stays encoded once", () => {
  // Every SigV4 service except S3 signs a twice-encoded path. Signing the
  // wire path yields a valid-looking signature that Bedrock rejects with
  // 403 SignatureDoesNotMatch, so this rule is pinned explicitly.
  assert.equal(canonicalUri(ORACLE_FIXTURE.path), ORACLE_FIXTURE.path.replace("%3A", "%253A"));

  const signedWirePath = sigv4Headers({
    ...ORACLE_FIXTURE,
    canonicalPath: ORACLE_FIXTURE.path,
  }).Authorization.split("Signature=").pop();
  assert.notEqual(
    signedWirePath,
    ORACLE_SIGNATURE,
    "signing the single-encoded path must not reproduce the accepted signature"
  );
});

test("sigv4 signatures are deterministic and bound to the timestamp", () => {
  const base = {
    host: "bedrock-runtime.us-east-1.amazonaws.com",
    path: "/model/m/converse",
    body: "{}",
    region: "us-east-1",
    accessKey: "AKIDEXAMPLE",
    secretKey: "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY",
    amzDate: "20260101T000000Z",
    dateStamp: "20260101",
  };
  const first = sigv4Headers(base).Authorization;
  assert.equal(sigv4Headers(base).Authorization, first, "same input must sign identically");

  const later = sigv4Headers({ ...base, amzDate: "20260101T000001Z" }).Authorization;
  assert.notEqual(later, first, "a different timestamp must change the signature");

  const otherRegion = sigv4Headers({ ...base, region: "eu-west-1" }).Authorization;
  assert.notEqual(otherRegion, first, "a different region must change the signature");

  const otherKey = sigv4Headers({ ...base, secretKey: "different-secret" }).Authorization;
  assert.notEqual(otherKey, first, "a different secret must change the signature");
});

test("session tokens are carried in the signed headers", () => {
  const headers = sigv4Headers({
    host: "bedrock-runtime.us-east-1.amazonaws.com",
    path: "/model/m/converse",
    body: "{}",
    region: "us-east-1",
    accessKey: "AKIDEXAMPLE",
    secretKey: "s",
    sessionToken: "FQoGZXIvYXdzEXAMPLETOKEN",
    amzDate: "20260101T000000Z",
    dateStamp: "20260101",
  });
  assert.equal(headers["x-amz-security-token"], "FQoGZXIvYXdzEXAMPLETOKEN");
});

test("without an override the client targets the real regional endpoint", () => {
  // The stub-backed tests below all inject an endpoint, so the default
  // derivation is checked separately: a wrong host here would sign a
  // request for a host AWS does not serve.
  const client = new BedrockClient({
    region: "eu-west-1",
    accessKey: "a",
    secretKey: "b",
    endpoint: null,
  });
  assert.equal(client.baseUrl.host, "bedrock-runtime.eu-west-1.amazonaws.com");
  assert.equal(
    new URL(`/model/${encodeURIComponent("m")}/converse`, client.baseUrl).href,
    "https://bedrock-runtime.eu-west-1.amazonaws.com/model/m/converse"
  );
});

test("an unconfigured client refuses to call Bedrock", async () => {
  const client = new BedrockClient({ accessKey: undefined, secretKey: undefined, endpoint: null });
  assert.equal(client.configured, false);
  await assert.rejects(() => client.converse({ prompt: "hi" }), /bedrock-not-configured/);
});

// ---------------------------------------------------------------------------
// Live request path (stub-backed)
// ---------------------------------------------------------------------------

test("health reports AI as configured when credentials are present", async () => {
  const { status, json } = await call("GET", "/api/health");
  assert.equal(status, 200);
  assert.equal(json.aiConfigured, true);
  assert.match(json.model, /anthropic\./); // default may carry a us./global. profile prefix
});

test("the outbound request is correctly signed and shaped", async () => {
  calls.length = 0;
  await call("POST", "/api/recommend", { query: "a mind-bending sci-fi movie under two hours" });

  assert.equal(calls.length, 1, "exactly one Bedrock call expected");
  const sent = calls[0];

  assert.match(sent.path, /^\/model\/.+\/converse$/, "must target the Converse API for the model id");
  assert.equal(sent.headers["content-type"], "application/json");
  assert.equal(
    sent.headers["x-amz-content-sha256"],
    createHash("sha256").update(sent.raw).digest("hex"),
    "the payload hash we signed must match the bytes that arrived"
  );
  assert.match(
    sent.headers.authorization,
    /^AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE\/\d{8}\/us-east-1\/bedrock\/aws4_request, /
  );
  assert.match(sent.headers["x-amz-date"], /^\d{8}T\d{6}Z$/, "x-amz-date must be an ISO8601 basic timestamp");

  const body = JSON.parse(sent.raw);
  assert.equal(body.system[0].text.includes("FireMind"), true, "system prompt must be sent");
  const prompt = body.messages[0].content[0].text;
  assert.match(prompt, /a mind-bending sci-fi movie under two hours/, "user query must reach the model");
  assert.match(prompt, /Candidate ids you may recommend:/, "model must be grounded to candidate ids");
  assert.ok(body.inferenceConfig.maxTokens > 0);
});

test("AI recommendations are accepted and grounded in catalog metadata", async () => {
  stubMode = "ok";
  const { status, json } = await call("POST", "/api/recommend", {
    query: "a mind-bending sci-fi movie under two hours",
  });

  assert.equal(status, 200);
  assert.equal(json.source, "ai", "a successful AI call must be labeled as such");
  assert.equal(json.recommendations.length, 2, "the invented id must be dropped");

  for (const rec of json.recommendations) {
    const fromCatalog = catalog.find((m) => m.id === rec.id);
    assert.ok(fromCatalog, `${rec.id} must exist in the catalog`);
    assert.equal(rec.title, fromCatalog.title, "titles must come from the catalog, not the model");
    assert.equal(rec.runtime, fromCatalog.runtime);
    assert.deepEqual(rec.genres, fromCatalog.genres);
  }
  assert.ok(json.recommendations.every((r) => r.reason && r.summary), "reason and summary are required");
  assert.equal(
    json.recommendations.some((r) => r.title === "Totally Invented Title"),
    false,
    "model-supplied metadata must never reach the client"
  );
});

test("AI summarize returns model text", async () => {
  stubMode = "ok";
  const { status, json } = await call("POST", "/api/summarize", { id: A.id });
  assert.equal(status, 200);
  assert.match(json.summary, /keeper faces the tide/);
});

test("AI similar returns grounded recommendations", async () => {
  stubMode = "ok";
  const { status, json } = await call("POST", "/api/similar", { id: A.id });
  assert.equal(status, 200);
  assert.equal(json.source, "ai");
  assert.equal(json.recommendations[0].id, C.id);
  assert.equal(json.recommendations[0].title, C.title);
});

// ---------------------------------------------------------------------------
// Degradation: any AI failure must leave the product working
// ---------------------------------------------------------------------------

test("a Bedrock 500 degrades to deterministic picks, not an error", async () => {
  stubMode = "http-500";
  const { status, json } = await call("POST", "/api/recommend", {
    query: "a mind-bending sci-fi movie under two hours",
  });

  assert.equal(status, 200, "the client must never see a 5xx because AWS failed");
  assert.equal(json.source, "fallback");
  assert.ok(json.recommendations.length > 0);

  const health = await call("GET", "/api/health");
  assert.equal(health.json.aiConfigured, true, "AI stays configured; only this call degraded");
});

test("prose instead of JSON degrades to deterministic picks", async () => {
  stubMode = "prose";
  const { status, json } = await call("POST", "/api/recommend", { query: "something funny" });
  assert.equal(status, 200);
  assert.equal(json.source, "fallback", "unparseable model output must not be trusted");
  assert.ok(json.recommendations.length > 0);
});

test("empty model output degrades to deterministic picks", async () => {
  stubMode = "empty";
  const { status, json } = await call("POST", "/api/recommend", { query: "something funny" });
  assert.equal(status, 200);
  assert.equal(json.source, "fallback");
});

test("a failed summarize falls back to the catalog description", async () => {
  stubMode = "http-500";
  const { status, json } = await call("POST", "/api/summarize", { id: A.id });
  assert.equal(status, 200);
  assert.equal(json.summary, A.description);
});

test("a failed similar falls back to the local similarity engine", async () => {
  stubMode = "http-500";
  const { status, json } = await call("POST", "/api/similar", { id: A.id });
  assert.equal(status, 200);
  assert.equal(json.source, "fallback");
  assert.ok(json.recommendations.length > 0);
});

test("validation errors still short-circuit before any Bedrock call", async () => {
  stubMode = "ok";
  calls.length = 0;
  const empty = await call("POST", "/api/recommend", { query: "" });
  assert.equal(empty.status, 400);

  const tooLong = await call("POST", "/api/recommend", { query: "x".repeat(501) });
  assert.equal(tooLong.status, 400);

  const missing = await call("POST", "/api/summarize", { id: "nope" });
  assert.equal(missing.status, 404);

  assert.equal(calls.length, 0, "invalid input must never reach Bedrock");
});
