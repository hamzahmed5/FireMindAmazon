/**
 * Minimal .env loader - zero dependencies.
 *
 * Credentials may come from a real shell environment or from a gitignored
 * `backend/.env` file (see .env.example). Variables already present in
 * process.env always win, so an exported shell value is never clobbered by
 * the file.
 *
 * Only simple KEY=VALUE lines are supported - no quoting rules, no variable
 * expansion. That is all this project needs, and the parsing stays obvious.
 * Session tokens contain `=` padding, so the value is everything after the
 * FIRST `=`.
 */
import { existsSync, readFileSync } from "node:fs";

/** Parse KEY=VALUE text into an object, ignoring blanks and `#` comments. */
export function parseDotEnv(text) {
  const vars = {};
  for (const rawLine of text.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#")) continue;
    const eq = line.indexOf("=");
    if (eq <= 0) continue;
    const key = line.slice(0, eq).trim();
    let value = line.slice(eq + 1).trim();
    const quoted =
      value.length >= 2 &&
      ((value.startsWith('"') && value.endsWith('"')) ||
        (value.startsWith("'") && value.endsWith("'")));
    if (quoted) value = value.slice(1, -1);
    vars[key] = value;
  }
  return vars;
}

/**
 * Resolve the listening port from an environment value.
 *
 * Defensive on purpose: some launchers and shells export `PORT=0` (or an empty
 * string), and `Number("0")` is 0 - which makes Node bind a RANDOM free port.
 * The server then looks healthy while being unreachable at the documented
 * address, so anything that is not a usable TCP port falls back to the default.
 */
export function resolvePort(raw, fallback = 8080) {
  const parsed = Number.parseInt(String(raw ?? "").trim(), 10);
  return Number.isInteger(parsed) && parsed > 0 && parsed <= 65535 ? parsed : fallback;
}

/**
 * Load `filePath` into process.env without overriding existing variables.
 * Returns the names of the variables actually set - never their values, so
 * this is safe to log.
 */
export function loadDotEnv(filePath) {
  if (!existsSync(filePath)) return [];
  const applied = [];
  for (const [key, value] of Object.entries(
    parseDotEnv(readFileSync(filePath, "utf8")),
  )) {
    if (!process.env[key]) {
      process.env[key] = value;
      applied.push(key);
    }
  }
  return applied;
}
