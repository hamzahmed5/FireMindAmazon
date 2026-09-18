/**
 * One router, two hosts.
 *
 * `server.js` (local Node HTTP server) and `lambda.mjs` (API Gateway -> Lambda)
 * both delegate here, so the laptop API and the deployed API cannot drift:
 * identical status codes, identical bodies, identical fallback behavior. The
 * TV app cannot tell which one it is talking to, which is exactly the point.
 *
 * Request in:  { method, path, rawBody, headers }
 * Response out: { status, contentType, body }
 */
import { catalog, fallbackRecommendations, similarById } from "./catalog.js";
import { aiRecommend, aiSummarize, aiSimilar } from "./ai.js";
import { consoleHtml } from "./console.js";

/** Same cap the server has always enforced; API Gateway has its own, larger. */
export const MAX_BODY_BYTES = 8_192;

const json = (status, payload) => ({
  status,
  contentType: "application/json; charset=utf-8",
  body: JSON.stringify(payload),
});

const html = (body) => ({
  status: 200,
  contentType: "text/html; charset=utf-8",
  body,
});

export function createRouter({ bedrock, logger = console }) {
  return async function handle(request) {
    const method = request.method ?? "GET";
    const path = request.path ?? "/";
    const rawBody = request.rawBody ?? "";

    const parseBody = () => {
      const bytes = Buffer.byteLength(rawBody, "utf8");
      if (bytes === 0) return { ok: true, value: {} };
      if (bytes > MAX_BODY_BYTES) return { ok: false, error: "payload-too-large" };
      try {
        return { ok: true, value: JSON.parse(rawBody) };
      } catch {
        return { ok: false, error: "invalid-json" };
      }
    };

    // ---- Console ----------------------------------------------------------
    // The same page is served locally and in the cloud, so a deployment can be
    // watched in a browser too.
    if (method === "GET" && (path === "/" || path === "/console")) {
      return html(consoleHtml());
    }

    // ---- Health -----------------------------------------------------------
    if (method === "GET" && path === "/api/health") {
      return json(200, {
        status: "ok",
        aiConfigured: bedrock.configured,
        model: bedrock.configured ? bedrock.modelId : null,
        catalogSize: catalog.length,
      });
    }

    // ---- Recommend --------------------------------------------------------
    if (method === "POST" && path === "/api/recommend") {
      const parsed = parseBody();
      if (!parsed.ok) return json(400, { error: parsed.error });
      const query = typeof parsed.value.query === "string" ? parsed.value.query.trim() : "";
      if (!query) return json(400, { error: "query is required" });
      if (query.length > 500) return json(400, { error: "query too long (max 500 chars)" });

      const filters = {
        runtimeMax: Number.isFinite(parsed.value.filters?.runtimeMax)
          ? Number(parsed.value.filters.runtimeMax)
          : null,
        familyOnly: parsed.value.filters?.familyOnly === true,
      };

      if (bedrock.configured) {
        try {
          const recommendations = await aiRecommend(bedrock, query, filters);
          return json(200, { source: "ai", recommendations });
        } catch (err) {
          logger.error("[firemind] AI recommend failed, using fallback:", err.message);
        }
      }
      return json(200, {
        source: "fallback",
        recommendations: fallbackRecommendations(query, filters),
      });
    }

    // ---- Summarize --------------------------------------------------------
    if (method === "POST" && path === "/api/summarize") {
      const parsed = parseBody();
      if (!parsed.ok) return json(400, { error: parsed.error });
      const movie = catalog.find((m) => m.id === parsed.value?.id);
      if (!movie) return json(404, { error: "unknown id" });

      if (bedrock.configured) {
        try {
          return json(200, { summary: await aiSummarize(bedrock, movie) });
        } catch (err) {
          logger.error("[firemind] AI summarize failed, using fallback:", err.message);
        }
      }
      return json(200, { summary: movie.description });
    }

    // ---- Similar ----------------------------------------------------------
    if (method === "POST" && path === "/api/similar") {
      const parsed = parseBody();
      if (!parsed.ok) return json(400, { error: parsed.error });
      const movie = catalog.find((m) => m.id === parsed.value?.id);
      if (!movie) return json(404, { error: "unknown id" });

      if (bedrock.configured) {
        try {
          const recommendations = await aiSimilar(bedrock, movie);
          if (recommendations.length > 0) {
            return json(200, { source: "ai", recommendations });
          }
        } catch (err) {
          logger.error("[firemind] AI similar failed, using fallback:", err.message);
        }
      }
      return json(200, { source: "fallback", recommendations: similarById(movie.id) });
    }

    return json(404, { error: "not found" });
  };
}

/**
 * Collect a Node request stream into a string, rejecting an oversized body the
 * same way the router does. Kept here so the server has no routing knowledge.
 */
export function readRequestBody(req, maxBytes = MAX_BODY_BYTES) {
  return new Promise((resolve, reject) => {
    let size = 0;
    const chunks = [];
    req.on("data", (chunk) => {
      size += chunk.length;
      if (size > maxBytes) {
        reject(new Error("payload-too-large"));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on("end", () => resolve(Buffer.concat(chunks).toString("utf8")));
    req.on("error", reject);
  });
}
