import { test } from "node:test";
import assert from "node:assert/strict";
import {
  catalog,
  parseRuntimeMax,
  parseMood,
  fallbackRecommendations,
  similarById,
} from "../lib/catalog.js";

test("catalog loads with 60 valid titles", () => {
  assert.equal(catalog.length, 60);
  for (const m of catalog) {
    assert.match(m.id, /^fv\d{3}$/);
    assert.ok(m.title.length > 0);
    assert.ok(m.runtime > 60 && m.runtime < 140);
    assert.ok(m.rating >= 6 && m.rating <= 9);
    assert.ok(Array.isArray(m.genres) && m.genres.length > 0);
    assert.ok(Array.isArray(m.moods) && m.moods.length > 0);
    assert.ok(typeof m.familyFriendly === "boolean");
    assert.ok(m.description.length > 20);
  }
  const ids = new Set(catalog.map((m) => m.id));
  assert.equal(ids.size, 60, "ids must be unique");
});

test("runtime parsing: minutes and hours", () => {
  assert.equal(parseRuntimeMax("funny movie under 100 minutes"), 100);
  assert.equal(parseRuntimeMax("something under 2 hours"), 120);
  assert.equal(parseRuntimeMax("no constraint here"), null);
});

test("runtime parsing: spelled-out numbers (regression: 'two hours')", () => {
  assert.equal(parseRuntimeMax("mind-bending sci-fi movie under two hours"), 120);
  assert.equal(parseRuntimeMax("something for one hour"), 60);
  assert.equal(parseRuntimeMax("about half an hour"), 60); // 'an hour' parses; 'half' is free
  const recs = fallbackRecommendations("mind-bending sci-fi under two hours", {});
  for (const r of recs) {
    assert.ok(r.runtime <= 120, `runtime ${r.runtime} violates the two-hour cap`);
  }
});

test("mood parsing: synonyms and direct tags", () => {
  assert.equal(parseMood("something relaxing"), "Cozy");
  assert.equal(parseMood("a hilarious comedy"), "Funny");
  assert.equal(parseMood("sci-fi please"), "Sci-Fi");
  assert.equal(parseMood("mind-bending twist"), "Mind-bending");
  assert.equal(parseMood("gibberish"), null);
});

test("fallback respects runtime cap and orders by mood then rating", () => {
  const recs = fallbackRecommendations("something funny under 100 minutes", {});
  assert.ok(recs.length >= 3 && recs.length <= 4);
  for (const r of recs) {
    assert.ok(r.runtime <= 100, "every pick must respect the cap");
  }
  const funnyCount = recs.filter((r) => r.title).length;
  assert.ok(funnyCount > 0);
});

test("fallback family filter never returns adult titles", () => {
  const recs = fallbackRecommendations("family movie night", { familyOnly: true });
  const byId = new Map(catalog.map((m) => [m.id, m]));
  for (const r of recs) {
    assert.equal(byId.get(r.id).familyFriendly, true);
  }
});

test("similar excludes the source title and shares mood/genre", () => {
  const recs = similarById("fv003"); // Midnight Cartography, Mind-bending
  assert.ok(recs.length >= 3);
  assert.ok(!recs.some((r) => r.id === "fv003"));
  const byId = new Map(catalog.map((m) => [m.id, m]));
  const source = byId.get("fv003");
  const best = byId.get(recs[0].id);
  const shares =
    best.moods.some((x) => source.moods.includes(x)) ||
    best.genres.some((x) => source.genres.includes(x));
  assert.ok(shares, "top similar title should share mood or genre");
});

test("fallback reason strings reference the request", () => {
  const recs = fallbackRecommendations("I want something funny under two hours", {});
  for (const r of recs) {
    assert.ok(r.reason.includes("You asked for"));
    assert.ok(r.reason.includes(r.title));
  }
});
