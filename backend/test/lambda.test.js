/**
 * The deployed shape, tested without AWS.
 *
 * API Gateway events are plain JSON, so the handler can be invoked in-process:
 * no credentials, no network, no deployment needed to prove that the Lambda
 * path answers exactly like the local server. The same router serves both, and
 * these tests are what keeps that true.
 */
import assert from "node:assert/strict";
import { describe, test } from "node:test";

import { handler } from "../lambda.mjs";
import { createRouter } from "../lib/router.js";

/** HTTP API (v2) event, as API Gateway actually delivers it. */
function event(method, path, body) {
  return {
    version: "2.0",
    requestContext: { http: { method } },
    rawPath: path,
    headers: {},
    body: body === undefined ? null : typeof body === "string" ? body : JSON.stringify(body),
  };
}

const silent = { error() {}, warn() {}, log() {} };
const call = async (router, method, path, body) =>
  router({
    method,
    path,
    rawBody: body === undefined ? "" : typeof body === "string" ? body : JSON.stringify(body),
    headers: {},
  });

describe("GET /api/health through the Lambda handler", () => {
  test("returns the same contract as the local server", async () => {
    const res = await handler(event("GET", "/api/health"), { awsRequestId: "test" });
    assert.equal(res.statusCode, 200);
    assert.match(res.headers["content-type"], /application\/json/);
    const body = JSON.parse(res.body);
    assert.equal(body.status, "ok");
    assert.equal(body.catalogSize, 60);
    assert.equal(typeof body.aiConfigured, "boolean");
    // No credentials in the test environment: the deployment's own role would
    // supply them, and the field reports presence, never a promise.
    assert.equal(body.aiConfigured, false);
    assert.equal(body.model, null);
  });
});

describe("recommendations through the Lambda handler", () => {
  test("serves the validated contract from the deterministic engine", async () => {
    const res = await handler(
      event("POST", "/api/recommend", { query: "a mind-bending sci-fi movie under two hours" }),
      { awsRequestId: "test" }
    );
    assert.equal(res.statusCode, 200);
    const body = JSON.parse(res.body);
    assert.equal(body.source, "fallback");
    assert.ok(body.recommendations.length >= 3 && body.recommendations.length <= 4);
    for (const r of body.recommendations) {
      assert.match(r.id, /^fv\d{3}$/);
      assert.ok(r.reason.length > 10 && r.summary.length > 10);
    }
  });

  test("rejects an empty query and an oversized body", async () => {
    const empty = await handler(event("POST", "/api/recommend", {}), {});
    assert.equal(empty.statusCode, 400);
    assert.equal(JSON.parse(empty.body).error, "query is required");

    const huge = await handler(event("POST", "/api/recommend", "x".repeat(9000)), {});
    assert.equal(huge.statusCode, 400);
    assert.equal(JSON.parse(huge.body).error, "payload-too-large");
  });
});

describe("transport details that differ from a local server", () => {
  test("a REST API (v1) event is understood too", async () => {
    const res = await handler(
      { httpMethod: "POST", path: "/api/similar", headers: {}, body: JSON.stringify({ id: "fv022" }) },
      {}
    );
    assert.equal(res.statusCode, 200);
    assert.equal(JSON.parse(res.body).source, "fallback");
  });

  test("base64 bodies decode to the same answer as plain ones", async () => {
    const ev = event("POST", "/api/summarize", { id: "fv010" });
    ev.body = Buffer.from(ev.body, "utf8").toString("base64");
    ev.isBase64Encoded = true;
    const res = await handler(ev, {});
    assert.equal(res.statusCode, 200);
    assert.ok(JSON.parse(res.body).summary.length > 10);
  });

  test("unknown route is 404, and GET / serves the console", async () => {
    const missing = await handler(event("GET", "/api/nope"), {});
    assert.equal(missing.statusCode, 404);
    assert.deepEqual(JSON.parse(missing.body), { error: "not found" });

    const console_ = await handler(event("GET", "/"), {});
    assert.equal(console_.statusCode, 200);
    assert.match(console_.headers["content-type"], /text\/html/);
    assert.ok(console_.body.includes("FireMind backend console"));
  });
});

describe("the AI path through the shared router", () => {
  const stub = (converse) => ({ configured: true, modelId: "stub-model", converse });

  test("a valid model reply is returned as source: ai", async () => {
    const router = createRouter({
      logger: silent,
      bedrock: stub(async () =>
        JSON.stringify({
          recommendations: [
            { id: "fv003", reason: "Matches the mind-bending request.", summary: "A safe summary." },
            { id: "fv002", reason: "Short and funny.", summary: "Another safe summary." },
          ],
        })
      ),
    });

    const res = await call(router, "POST", "/api/recommend", { query: "something clever" });
    assert.equal(res.status, 200);
    const body = JSON.parse(res.body);
    assert.equal(body.source, "ai");
    assert.equal(body.recommendations[0].id, "fv003");
    // Catalog metadata comes from our own catalog, never from the model.
    assert.equal(body.recommendations[0].title, "Midnight Cartography");
  });

  test("a broken model reply degrades to fallback instead of failing", async () => {
    const router = createRouter({
      logger: silent,
      bedrock: stub(async () => "I think you should watch a nice film!"),
    });

    const res = await call(router, "POST", "/api/recommend", { query: "something clever" });
    assert.equal(res.status, 200);
    assert.equal(JSON.parse(res.body).source, "fallback");
  });

  test("hallucinated ids are dropped, not shown", async () => {
    const router = createRouter({
      logger: silent,
      bedrock: stub(async () =>
        JSON.stringify({
          recommendations: [
            { id: "fv999", reason: "Invented title.", summary: "Does not exist." },
            { id: "fv002", reason: "Real title.", summary: "Real summary." },
          ],
        })
      ),
    });

    const res = await call(router, "POST", "/api/recommend", { query: "something clever" });
    const body = JSON.parse(res.body);
    assert.deepEqual(
      body.recommendations.map((r) => r.id),
      ["fv002"]
    );
  });
});
