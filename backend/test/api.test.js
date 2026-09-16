import { test, before, after } from "node:test";
import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const PORT = 8137;
let child;

async function call(method, urlPath, body) {
  const resp = await fetch(`http://127.0.0.1:${PORT}${urlPath}`, {
    method,
    headers: body ? { "content-type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  return { status: resp.status, json: await resp.json() };
}

before(async () => {
  child = spawn(process.execPath, [path.join(here, "..", "server.js")], {
    env: { ...process.env, PORT: String(PORT) },
    stdio: "ignore",
  });
  // Wait for readiness
  for (let i = 0; i < 50; i++) {
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
  child.kill();
});

test("health returns ok with catalog size", async () => {
  const { status, json } = await call("GET", "/api/health");
  assert.equal(status, 200);
  assert.equal(json.status, "ok");
  assert.equal(json.catalogSize, 60);
  assert.equal(typeof json.aiConfigured, "boolean");
});

test("recommend returns valid contract for a natural language query", async () => {
  const { status, json } = await call("POST", "/api/recommend", {
    query: "I want a mind-bending sci-fi movie under two hours",
  });
  assert.equal(status, 200);
  assert.equal(json.source, "fallback"); // no AI creds in test env
  assert.ok(json.recommendations.length >= 3 && json.recommendations.length <= 4);
  for (const r of json.recommendations) {
    assert.match(r.id, /^fv\d{3}$/);
    assert.ok(r.title && r.year && r.runtime && r.rating);
    assert.ok(r.reason.includes("You asked for") || r.reason.length > 10);
    assert.ok(r.summary.length > 10);
  }
});

test("recommend validates input", async () => {
  assert.equal((await call("POST", "/api/recommend", {})).status, 400);
  assert.equal((await call("POST", "/api/recommend", { query: "" })).status, 400);
  assert.equal(
    (await call("POST", "/api/recommend", { query: "x".repeat(600) })).status,
    400
  );
});

test("recommend handles malformed JSON", async () => {
  const resp = await fetch(`http://127.0.0.1:${PORT}/api/recommend`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: "{not json",
  });
  assert.equal(resp.status, 400);
});

test("summarize returns description for known id, 404 for unknown", async () => {
  const ok = await call("POST", "/api/summarize", { id: "fv010" });
  assert.equal(ok.status, 200);
  assert.ok(ok.json.summary.length > 10);

  const missing = await call("POST", "/api/summarize", { id: "fv999" });
  assert.equal(missing.status, 404);
});

test("similar returns non-overlapping picks", async () => {
  const { status, json } = await call("POST", "/api/similar", { id: "fv022" });
  assert.equal(status, 200);
  assert.ok(json.recommendations.length >= 3);
  assert.ok(!json.recommendations.some((r) => r.id === "fv022"));
});

test("unknown route is 404", async () => {
  const { status } = await call("GET", "/api/nope");
  assert.equal(status, 404);
});
