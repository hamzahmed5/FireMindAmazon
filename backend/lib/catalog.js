/**
 * FireMind backend catalog loader and deterministic recommendation engine.
 *
 * Mirrors `LocalRecommender.kt` on the app side one-for-one: same intent
 * parsing (moods AND genres), same ranking, same reason wording. Keeping
 * the two in lockstep is what makes backend "fallback" results and the
 * app's offline "curated picks" indistinguishable to the user, and it
 * lets both test suites assert the same expectations. Any behavioral
 * change here must be mirrored there.
 */
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";

const here = path.dirname(fileURLToPath(import.meta.url));

export const catalog = JSON.parse(
  readFileSync(path.join(here, "..", "data", "catalog.json"), "utf8")
);

/** Mood tags that actually exist in the catalog. */
export const CATALOG_MOODS = [
  "Serious", "Cozy", "Funny", "Whimsical",
  "Mind-bending", "Heartfelt", "Exciting", "Tense",
];

/** Genre tags that actually exist in the catalog. */
export const CATALOG_GENRES = [
  "Drama", "Mystery", "Sci-Fi", "Comedy", "Family",
  "Action", "Thriller", "Adventure", "Romance",
];

/**
 * Words that imply a catalog mood. Deliberately absent: "kids",
 * "children", "animated", "family" — those drive the audience filter and
 * the Family *genre*; there is no mood called "Family" in the catalog.
 */
export const MOOD_SYNONYMS = {
  relaxing: "Cozy", chill: "Cozy", calm: "Cozy",
  heartwarming: "Heartfelt", touching: "Heartfelt",
  twist: "Mind-bending", smart: "Mind-bending", clever: "Mind-bending",
  hilarious: "Funny", laugh: "Funny", funny: "Funny",
  thrilling: "Exciting", adrenaline: "Exciting",
  suspense: "Tense", scary: "Tense", spooky: "Tense",
};

/** Words that imply a catalog genre. */
export const GENRE_SYNONYMS = {
  "sci-fi": "Sci-Fi", scifi: "Sci-Fi", "sci fi": "Sci-Fi",
  "science fiction": "Sci-Fi", space: "Sci-Fi",
  comedy: "Comedy", comedies: "Comedy", sitcom: "Comedy",
  mystery: "Mystery", mysteries: "Mystery", whodunit: "Mystery",
  thriller: "Thriller", thrillers: "Thriller",
  action: "Action",
  drama: "Drama", dramas: "Drama",
  romance: "Romance", romantic: "Romance",
  "rom com": "Romance", "rom-com": "Romance", "love story": "Romance",
  adventure: "Adventure", adventures: "Adventure",
  family: "Family", animated: "Family",
};

/** Words that put the request in family-friendly territory. */
export const AUDIENCE_WORDS = [
  "family", "families", "kid", "kids", "child", "children", "animated", "all ages",
];

/** Quick-mood chips shown in the app UI. */
export const MOOD_CHIPS = [
  "Funny", "Exciting", "Family", "Sci-Fi", "Relaxing", "Cozy", "Mind-bending",
];

const WORD_NUMBERS = {
  one: 1, two: 2, three: 3, four: 4, five: 5,
  six: 6, seven: 7, eight: 8, nine: 9, ten: 10,
  a: 1, an: 1, half: 0.5,
};

/**
 * Extract a runtime cap in minutes from natural language: digits
 * ("under 100 minutes", "2 hours") and spelled-out numbers
 * ("under two hours").
 */
export function parseRuntimeMax(query) {
  const lower = String(query ?? "").toLowerCase();

  const minutes = lower.match(/(\d+)\s*(min|minute)/);
  if (minutes) return Number(minutes[1]);

  const wordHours = lower.match(/(one|two|three|four|five|six|seven|eight|nine|ten|a|an|half)\s*(hour|hr)/);
  if (wordHours) return Math.round(WORD_NUMBERS[wordHours[1]] * 60);

  const hours = lower.match(/(\d+)\s*(hour|hr)/);
  if (hours) return Number(hours[1]) * 60;

  return null;
}

/** Detect the intended mood from synonyms, then real catalog mood tags. */
export function parseMood(query) {
  const lower = String(query ?? "").toLowerCase();
  for (const [needle, mood] of Object.entries(MOOD_SYNONYMS)) {
    if (lower.includes(needle)) return mood;
  }
  return CATALOG_MOODS.find((mood) => lower.includes(mood.toLowerCase())) ?? null;
}

/** Detect the intended genre from synonyms, then real catalog genres. */
export function parseGenre(query) {
  const lower = String(query ?? "").toLowerCase();
  for (const [needle, genre] of Object.entries(GENRE_SYNONYMS)) {
    if (lower.includes(needle)) return genre;
  }
  return CATALOG_GENRES.find((genre) => lower.includes(genre.toLowerCase())) ?? null;
}

/** True when the request should be limited to family-friendly titles. */
export function wantsFamilyFriendly(query, familyOnly = false) {
  const lower = String(query ?? "").toLowerCase();
  return familyOnly || AUDIENCE_WORDS.some((word) => lower.includes(word));
}

/**
 * Filter + rank candidates: mood match first, then genre match, then
 * rating. Constraints are relaxed in the documented order when they
 * leave nothing (runtime cap, then audience), so a request can never
 * return zero picks.
 */
export function filterCandidates(query, filters = {}) {
  const mood = parseMood(query);
  const genre = parseGenre(query);
  const runtimeMax = filters.runtimeMax ?? parseRuntimeMax(query);
  const familyFilter = wantsFamilyFriendly(query, filters.familyOnly === true);

  let pool = catalog.slice();
  if (familyFilter) pool = pool.filter((m) => m.familyFriendly);
  if (runtimeMax) pool = pool.filter((m) => m.runtime <= runtimeMax);
  if (pool.length === 0) {
    // Relax the runtime cap; keep the audience constraint.
    pool = catalog.filter((m) => (familyFilter ? m.familyFriendly : true));
  }
  if (pool.length === 0) pool = catalog.slice();

  return pool
    .sort((a, b) => {
      const moodA = mood && a.moods.includes(mood) ? 1 : 0;
      const moodB = mood && b.moods.includes(mood) ? 1 : 0;
      if (moodA !== moodB) return moodB - moodA;
      const genreA = genre && a.genres.includes(genre) ? 1 : 0;
      const genreB = genre && b.genres.includes(genre) ? 1 : 0;
      if (genreA !== genreB) return genreB - genreA;
      return b.rating - a.rating;
    })
    .slice(0, 12);
}

/** Build the deterministic fallback response for a query. */
export function fallbackRecommendations(query, filters = {}, limit = 4) {
  const mood = parseMood(query);
  const genre = parseGenre(query);
  const runtimeMax = parseRuntimeMax(query);
  const familyOnly = filters.familyOnly === true;

  const matched = [
    mood ? `a ${mood} mood` : null,
    genre ? `${genre} titles` : null,
    runtimeMax ? `under ${runtimeMax} minutes` : null,
    familyOnly ? "family-friendly" : null,
  ].filter(Boolean);

  const prefix = matched.length
    ? `You asked for ${matched.join(" and ")}.`
    : "Here are FireMind's top picks.";

  return filterCandidates(query, filters)
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
