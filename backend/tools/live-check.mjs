/**
 * Live Bedrock check - run this once your AWS account can invoke Bedrock.
 *
 *   cd backend && node tools/live-check.mjs
 *
 * Reads the same environment as server.js (AWS_ACCESS_KEY_ID etc., or a
 * backend/.env file), makes one real Converse call through the real
 * BedrockClient, and prints a one-line verdict. No secrets are printed.
 *
 * Exit codes: 0 = live AI works, 1 = AI call failed (reason is printed -
 * typical cases are the new-account verification hold, missing model
 * access, or expired session credentials).
 */
import { readFileSync, existsSync } from "node:fs";
import { BedrockClient } from "../lib/bedrock.js";

if (existsSync(new URL("../.env", import.meta.url))) {
  for (const line of readFileSync(new URL("../.env", import.meta.url), "utf8").split("\n")) {
    const i = line.indexOf("=");
    if (i > 0 && !process.env[line.slice(0, i)]) {
      process.env[line.slice(0, i)] = line.slice(i + 1).trim();
    }
  }
}

const client = new BedrockClient();
if (!client.configured) {
  console.log("NOT CONFIGURED - no AWS credentials in the environment or backend/.env");
  process.exit(1);
}

console.log(`model:  ${client.modelId}`);
console.log(`region: ${client.region}`);
process.stdout.write("calling real Bedrock Converse ... ");

try {
  const text = await client.converse({
    system: 'Reply with ONLY JSON: {"ok":true}',
    prompt: "ping",
    maxTokens: 4096,
  });
  console.log("SUCCESS");
  console.log(`model replied (${text.length} chars): ${text.slice(0, 120)}`);
  console.log("\nLive AI is working. Restart the backend and the badge will show AI.");
  process.exit(0);
} catch (err) {
  console.log("FAILED");
  const msg = String(err.message ?? err);
  if (msg.includes("being verified")) {
    console.log("-> AWS new-account verification hold (usually clears within ~2h).");
    console.log("   Re-run this command later; nothing to fix in the project.");
  } else if (msg.includes("AccessDenied") || msg.includes("not authorized")) {
    console.log("-> Model access is not enabled for this account/region.");
    console.log("   AWS Console -> Bedrock -> Model access -> request the model.");
  } else if (msg.includes("ExpiredToken") || msg.includes("expired")) {
    console.log("-> The session credentials expired. Re-run `aws login`, then copy");
    console.log("   fresh values into backend/.env (see backend/.env.example).");
  } else if (msg.includes("SignatureDoesNotMatch")) {
    console.log("-> Signature rejected - this would mean the signer regressed.");
    console.log("   Run: python backend/tools/sigv4-oracle.py --json | node backend/tools/sigv4-crosscheck.mjs");
  } else {
    console.log(`-> ${msg.slice(0, 400)}`);
  }
  process.exit(1);
}
