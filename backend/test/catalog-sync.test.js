import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const appAsset = path.join(here, "..", "..", "app", "src", "main", "assets", "catalog.json");
const backendCopy = path.join(here, "..", "data", "catalog.json");

/**
 * The app bundles the catalog and the backend serves a copy. If these two
 * files ever diverge, AI-mode and offline-mode recommendations would
 * disagree, so this guard fails the suite on any drift.
 */
test("backend catalog copy matches the app asset byte-for-byte", () => {
  const appBytes = readFileSync(appAsset);
  const backendBytes = readFileSync(backendCopy);
  assert.ok(
    appBytes.equals(backendBytes),
    "backend/data/catalog.json has drifted from app/src/main/assets/catalog.json - " +
      "re-copy it with: cp app/src/main/assets/catalog.json backend/data/catalog.json"
  );
});
