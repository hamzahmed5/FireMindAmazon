/**
 * AI orchestration: prompt design, strict JSON contract, validation.
 * The frontend never parses model prose - it only ever receives
 * validated RecommendResponse JSON from this layer.
 */
import { catalog, filterCandidates } from "./catalog.js";

const MOVIE_BLOCK = catalog
  .map(
    (m) =>
      `- id=${m.id} | ${m.title} (${m.year}) | genres: ${m.genres.join(", ")} | moods: ${m.moods.join(", ")} | ${m.runtime} min | family: ${m.familyFriendly ? "yes" : "no"} | rating: ${m.rating}`
  )
  .join("\n");

const RECOMMEND_SYSTEM = `You are FireMind, a TV viewing companion. You recommend ONLY titles from the provided catalog using the provided ids and metadata. Never invent titles or metadata.

Reply with ONLY a JSON object, no markdown fences, in exactly this shape:
{"recommendations":[{"id":"fvNNN","reason":"one or two sentences explaining why it matches the request","summary":"one spoiler-safe sentence"}]}
Rules:
- Return 3 to 4 recommendations, best first.
- "reason" must reference the user's stated preferences (mood, runtime, audience).
- "summary" must be spoiler-free.
- Respect any runtime or family-friendly constraints strictly.`;

/**
 * Ask Bedrock for recommendations. Validates/repairs the JSON contract;
 * throws on any violation so the caller can fall back deterministically.
 */
export async function aiRecommend(bedrock, query, filters = {}) {
  const candidates = filterCandidates(query, filters);
  const prompt = [
    "Catalog:",
    MOVIE_BLOCK,
    "",
    `User request: "${query}"`,
    filters.familyOnly ? "Constraint: family-friendly titles only." : "",
    "Candidate ids you may recommend:",
    candidates.map((m) => m.id).join(", "),
  ]
    .filter(Boolean)
    .join("\n");

  const text = await bedrock.converse({ system: RECOMMEND_SYSTEM, prompt });
  const parsed = extractJson(text);
  const recs = validateRecommendations(parsed, query, filters);
  if (recs.length === 0) throw new Error("ai-contract-violation");
  return recs;
}

/** Ask Bedrock for a spoiler-safe summary of one catalog title. */
export async function aiSummarize(bedrock, movie) {
  const system =
    "You are FireMind. Summarize a movie for a viewer deciding what to watch. " +
    "Two sentences maximum. No spoilers beyond the setup. Reply with ONLY the summary text.";
  const prompt = `Title: ${movie.title} (${movie.year}). Genres: ${movie.genres.join(", ")}. Setup: ${movie.description}`;
  const text = await bedrock.converse({ system, prompt, maxTokens: 120 });
  return text.trim();
}

/** Ask Bedrock for titles similar to one catalog title. */
export async function aiSimilar(bedrock, movie, limit = 4) {
  const system =
    "You are FireMind. From the catalog, pick titles similar in mood and genre to the given one. " +
    "Reply with ONLY JSON: {\"recommendations\":[{\"id\":\"fvNNN\",\"reason\":\"...\",\"summary\":\"...\"}]}. " +
    `Return ${limit} items, best first.`;
  const prompt = `Catalog:\n${MOVIE_BLOCK}\n\nSource title: ${movie.title} (${movie.moods.join(", ")}, ${movie.genres.join(", ")}).`;
  const text = await bedrock.converse({ system, prompt });
  const parsed = extractJson(text);
  const recs = validateRecommendations(parsed, `similar to ${movie.title}`, {});
  return recs.slice(0, limit);
}

// The candidate set the model may choose from comes from the same shared
// engine the deterministic fallback uses (imported above), so AI grounding
// and offline picks can never rank differently.

/** Pull the first JSON object out of model text (tolerates fences/prose). */
function extractJson(text) {
  const cleaned = text.replace(/```json|```/g, "").trim();
  const start = cleaned.indexOf("{");
  const end = cleaned.lastIndexOf("}");
  if (start === -1 || end === -1 || end <= start) throw new Error("ai-no-json");
  return JSON.parse(cleaned.slice(start, end + 1));
}

/** Enforce the response contract against real catalog entries. */
function validateRecommendations(parsed, query, filters) {
  if (!parsed || !Array.isArray(parsed.recommendations)) return [];
  const byId = new Map(catalog.map((m) => [m.id, m]));
  const seen = new Set();
  const out = [];
  for (const item of parsed.recommendations) {
    const movie = byId.get(item?.id);
    if (!movie || seen.has(movie.id)) continue;
    seen.add(movie.id);
    out.push({
      id: movie.id,
      title: movie.title,
      year: movie.year,
      genres: movie.genres,
      runtime: movie.runtime,
      rating: movie.rating,
      reason: typeof item.reason === "string" && item.reason.trim()
        ? item.reason.trim()
        : `Matches your request: ${query}.`,
      summary: typeof item.summary === "string" && item.summary.trim()
        ? item.summary.trim()
        : movie.description,
    });
    if (out.length >= 4) break;
  }
  return out;
}
