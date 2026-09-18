/**
 * FireMind backend - local Node HTTP server.
 *
 * This file is the local transport only: it reads the port, loads config and
 * hands every request to lib/router.js, which is the single source of routing
 * truth shared with the Lambda deployment (lambda.mjs). Endpoints and behavior
 * are documented in docs/API_SPEC.md:
 *
 *   GET  /            browser console (lib/console.js)
 *   GET  /api/health
 *   POST /api/recommend { query, filters? }
 *   POST /api/summarize { id }
 *   POST /api/similar   { id }
 *
 * AI comes from Amazon Bedrock when credentials are configured; otherwise every
 * endpoint serves the deterministic local fallback so the product always works.
 * No secrets are ever read from the repository.
 */
import { createServer } from "node:http";
import { BedrockClient } from "./lib/bedrock.js";
import { loadDotEnv, resolvePort } from "./lib/env.js";
import { createRouter, readRequestBody } from "./lib/router.js";

// A gitignored backend/.env is loaded here so the documented "copy
// .env.example to .env" flow actually enables AI. Shell variables win.
const fromDotEnv = loadDotEnv(new URL("./.env", import.meta.url));

const PORT = resolvePort(process.env.PORT);
const bedrock = new BedrockClient();
const handle = createRouter({ bedrock });

const server = createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host ?? "localhost"}`);

  try {
    let rawBody = "";
    if (req.method === "POST") {
      try {
        rawBody = await readRequestBody(req);
      } catch (err) {
        res.writeHead(400, { "content-type": "application/json; charset=utf-8" });
        return res.end(JSON.stringify({ error: String(err.message) }));
      }
    }

    const result = await handle({
      method: req.method,
      path: url.pathname,
      rawBody,
      headers: req.headers,
    });
    res.writeHead(result.status, { "content-type": result.contentType });
    res.end(result.body);
  } catch (err) {
    console.error("[firemind] unhandled error:", err);
    res.writeHead(500, { "content-type": "application/json; charset=utf-8" });
    res.end(JSON.stringify({ error: "internal error" }));
  }
});

server.listen(PORT, () => {
  console.log(`[firemind] backend listening on http://localhost:${PORT}`);
  console.log(
    `[firemind] AI mode: ${bedrock.configured ? `Bedrock (${bedrock.modelId})` : "disabled - deterministic fallback active"}`
  );
  if (fromDotEnv.length > 0) {
    // Names only - values are never logged.
    console.log(`[firemind] loaded from backend/.env: ${fromDotEnv.join(", ")}`);
  }
  if (process.env.BEDROCK_ENDPOINT) {
    console.warn(
      `[firemind] WARNING: BEDROCK_ENDPOINT is set to ${process.env.BEDROCK_ENDPOINT}. ` +
        "This TEST-ONLY override redirects signed Bedrock requests away from AWS; " +
        "signed requests would be sent there. Unset it in any real deployment."
    );
  }
});

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, () => {
    server.close(() => process.exit(0));
  });
}
