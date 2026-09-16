/**
 * FireMind backend catalog loader and deterministic recommendation engine.
 * Mirrors the app-side logic so offline behavior is identical.
 */
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";

const here = path.dirname(fileURLToPath(import.meta.url));

export const catalog = JSON.parse(
  readFileSync(path.join(here, "..", "data", "catalog.json"), "utf8")
);

export const MOOD_SYNONYMS = {
  relaxing: "Cozy", chill: "Cozy", calm: "Cozy",
  heartwarming: "Heartfelt", touching: "Heartfelt",
  twist: "Mind-bending", smart: "Mind-bending", clever: "Mind-bending",
  hilarious: "Funny", laugh: "Funny", comedy: "Funny",
  thrilling: "Exciting", adrenaline: "Exciting", action: "Exciting",
  kids: "Family", children: "Family", animated: "Family",
  scifi: "Sci-Fi", "sci-fi": "Sci-Fi", space: "Sci-Fi",
  mystery: "Tense", suspense: "Tense", scary: "Tense",
};

export const MOOD_CHIPS = [
  "Funny", "Exciting", "Family", "Sci-Fi", "Relaxing", "Cozy", "Mind-bending",
];

/** Extract runtime cap in minutes from natural language ("under 100 minutes", "2 hours"). */
export function parseRuntimeMax(query) {
  const lower = query.toLowerCase();
  const minutes = lower.match(/(\d+)\s*(min|minute)/);
  if (minutes) return Number(minutes[1]);
  const hours = lower.match(/(\d+)\s*(hour|hr)/);
  if (hours) return Number(hours[1]) * 60;
  return null;
}

/** Detect mood intent from free text using synonyms then direct tags. */
export function parseMood(query) {
  const lower = query.toLowerCase();
  for (const [needle, mood] of Object.entries(MOOD_SYNONYMS)) {
    if (lower.includes(needle)) return mood;
  }
  return MOOD_CHIPS.find((mood) => lower.includes(mood.toLowerCase())) ?? null;
}

/** Filter + rank candidates deterministically. Returns catalog entries. */
export function filterCandidates(query, filters = {}) {
  const lower = (query ?? "").toLowerCase();
  const mood = parseMood(lower);
  let runtimeMax = filters.runtimeMax ?? parseRuntimeMax(lower);
  const familyOnly = filters.familyOnly === true || lower.includes("family") || lower.includes("kid");

  let pool = catalog.slice();
  if (familyOnly) pool = pool.filter((m) => m.familyFriendly);
  if (runtimeMax) pool = pool.filter((m) => m.runtime <= runtimeMax);
  if (pool.length === 0) {
    // Relax constraints in the documented order: runtime first, then family.
    pool = catalog.filter((m) => (familyOnly ? m.familyFriendly : true));
    runtimeMax = null;
  }
  if (pool.length === 0) pool = catalog.slice();

  return pool
    .sort((a, b) => {
      const moodA = mood && a.moods.includes(mood) ? 1 : 0;
      const moodB = mood && b.moods.includes(mood) ? 1 : 0;
      if (moodA !== moodB) return moodB - moodA;
      return b.rating - a.rating;
    })
    .slice(0, 12);
}

/** Build the deterministic fallback response for a query. */
export function fallbackRecommendations(query, filters = {}, limit = 4) {
  const lower = (query ?? "").toLowerCase();
  const mood = parseMood(lower);
  const runtimeMax = filters.runtimeMax ?? parseRuntimeMax(lower);
  const familyOnly = filters.familyOnly === true;

  const matched = [
    mood ? `a ${mood} mood` : null,
    runtimeMax ? `under ${runtimeMax} minutes` : null,
    familyOnly ? "family-friendly" : null,
  ].filter(Boolean);

  const prefix = matched.length
    ? `You asked for ${matched.join(" and ")}.`
    : "Here are FireMind's top picks.";

  return filterCandidates(lower, filters)
    .slice(0, limit)
    .map((m) => ({
      id: m.id,
      title: m.title,
      year: m.year,
      genres: m.genres,
      runtime: m.runtime,
      rating: m.rating,
      reason: `${prefix} "${m.title}" fits: ${m.moods.join("/").toLowerCase()}, ${m.runtime} min, rated ${m.rating}.`,
      summary: m.description,
    }));
}

/** Local similar-title scoring (used when AI is unavailable). */
export function similarById(id, limit = 4) {
  const source = catalog.find((m) => m.id === id);
  if (!source) return [];
  return catalog
    .filter((m) => m.id !== id)
    .map((m) => ({
      m,
      score:
        m.moods.filter((x) => source.moods.includes(x)).length * 10 +
        m.genres.filter((x) => source.genres.includes(x)).length * 5 +
        m.rating,
    }))
    .sort((a, b) => b.score - a.score)
    .slice(0, limit)
    .map(({ m }) => ({
      id: m.id,
      title: m.title,
      year: m.year,
      genres: m.genres,
      runtime: m.runtime,
      rating: m.rating,
      reason: `Similar to "${source.title}": shares its ${m.moods.join("/").toLowerCase()} tone.`,
      summary: m.description,
    }));
}
