/**
 * FireMind backend - zero-dependency Node HTTP server.
 *
 * Endpoints (see docs/API_SPEC.md):
 *   GET  /api/health
 *   POST /api/recommend { query, filters? }
 *   POST /api/summarize { id }
 *   POST /api/similar   { id }
 *
 * AI comes from Amazon Bedrock when credentials are configured; otherwise
 * every endpoint serves the deterministic local fallback so the product
 * always works. No secrets are ever read from the repository.
 */
import { createServer } from "node:http";
import { catalog, fallbackRecommendations, similarById } from "./lib/catalog.js";
import { BedrockClient } from "./lib/bedrock.js";
import { aiRecommend, aiSummarize, aiSimilar } from "./lib/ai.js";

const PORT = Number(process.env.PORT ?? 8080);
const bedrock = new BedrockClient();

const jsonHeaders = { "content-type": "application/json" };

function send(res, status, payload) {
  res.writeHead(status, jsonHeaders);
  res.end(JSON.stringify(payload));
}

function readBody(req, maxBytes = 8_192) {
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
    req.on("end", () => {
      if (chunks.length === 0) return resolve({});
      try {
        resolve(JSON.parse(Buffer.concat(chunks).toString("utf8")));
      } catch {
        reject(new Error("invalid-json"));
      }
    });
    req.on("error", reject);
  });
}

const server = createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host ?? "localhost"}`);

  try {
    // ---- Health ---------------------------------------------------------
    if (req.method === "GET" && url.pathname === "/api/health") {
      return send(res, 200, {
        status: "ok",
        aiConfigured: bedrock.configured,
        model: bedrock.configured ? bedrock.modelId : null,
        catalogSize: catalog.length,
      });
    }

    // ---- Recommend ------------------------------------------------------
    if (req.method === "POST" && url.pathname === "/api/recommend") {
      let body;
      try {
        body = await readBody(req);
      } catch (err) {
        return send(res, 400, { error: String(err.message) });
      }
      const query = typeof body.query === "string" ? body.query.trim() : "";
      if (!query) {
        return send(res, 400, { error: "query is required" });
      }
      if (query.length > 500) {
        return send(res, 400, { error: "query too long (max 500 chars)" });
      }
      const filters = {
        runtimeMax: Number.isFinite(body.filters?.runtimeMax)
          ? Number(body.filters.runtimeMax)
          : null,
        familyOnly: body.filters?.familyOnly === true,
      };

      if (bedrock.configured) {
        try {
          const recommendations = await aiRecommend(bedrock, query, filters);
          return send(res, 200, { source: "ai", recommendations });
        } catch (err) {
          console.error("[firemind] AI recommend failed, using fallback:", err.message);
        }
      }
      return send(res, 200, {
        source: "fallback",
        recommendations: fallbackRecommendations(query, filters),
      });
    }

    // ---- Summarize ------------------------------------------------------
    if (req.method === "POST" && url.pathname === "/api/summarize") {
      let body;
      try {
        body = await readBody(req);
      } catch (err) {
        return send(res, 400, { error: String(err.message) });
      }
      const movie = catalog.find((m) => m.id === body?.id);
      if (!movie) return send(res, 404, { error: "unknown id" });

      if (bedrock.configured) {
        try {
          const summary = await aiSummarize(bedrock, movie);
          return send(res, 200, { summary });
        } catch (err) {
          console.error("[firemind] AI summarize failed, using fallback:", err.message);
        }
      }
      return send(res, 200, { summary: movie.description });
    }

    // ---- Similar --------------------------------------------------------
    if (req.method === "POST" && url.pathname === "/api/similar") {
      let body;
      try {
        body = await readBody(req);
      } catch (err) {
        return send(res, 400, { error: String(err.message) });
      }
      const movie = catalog.find((m) => m.id === body?.id);
      if (!movie) return send(res, 404, { error: "unknown id" });

      if (bedrock.configured) {
        try {
          const recommendations = await aiSimilar(bedrock, movie);
          if (recommendations.length > 0) {
            return send(res, 200, { source: "ai", recommendations });
          }
        } catch (err) {
          console.error("[firemind] AI similar failed, using fallback:", err.message);
        }
      }
      return send(res, 200, { source: "fallback", recommendations: similarById(movie.id) });
    }

    return send(res, 404, { error: "not found" });
  } catch (err) {
    console.error("[firemind] unhandled error:", err);
    return send(res, 500, { error: "internal error" });
  }
});

server.listen(PORT, () => {
  console.log(`[firemind] backend listening on http://localhost:${PORT}`);
  console.log(
    `[firemind] AI mode: ${bedrock.configured ? `Bedrock (${bedrock.modelId})` : "disabled - deterministic fallback active"}`
  );
});

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, () => {
    server.close(() => process.exit(0));
  });
}
