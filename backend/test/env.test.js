/**
 * Tests for the shared .env loader.
 *
 * This exists because the documented setup flow ("copy .env.example to
 * .env") silently did nothing: the server read process.env only, so AI mode
 * stayed off with no error. The loader is shared by server.js and
 * tools/live-check.mjs so the two can never disagree again.
 */
import assert from "node:assert/strict";
import { mkdtempSync, writeFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { after, before, describe, test } from "node:test";

import { loadDotEnv, parseDotEnv, resolvePort } from "../lib/env.js";

describe("resolvePort", () => {
  test("accepts a usable port and ignores the documented default", () => {
    assert.equal(resolvePort("8080"), 8080);
    assert.equal(resolvePort(" 3000 "), 3000);
    assert.equal(resolvePort("65535"), 65535);
  });

  test("falls back instead of binding a random port", () => {
    // PORT=0 is exported by some launchers; Number("0") would silently bind a
    // random free port, leaving the documented address dead with no error.
    assert.equal(resolvePort("0"), 8080);
    assert.equal(resolvePort(""), 8080);
    assert.equal(resolvePort("   "), 8080);
    assert.equal(resolvePort(undefined), 8080);
    assert.equal(resolvePort(null), 8080);
    assert.equal(resolvePort("not-a-port"), 8080);
    assert.equal(resolvePort("70000"), 8080);
    assert.equal(resolvePort("-1"), 8080);
  });

  test("honours a custom fallback", () => {
    assert.equal(resolvePort("0", 9999), 9999);
  });
});

describe("parseDotEnv", () => {
  test("parses simple KEY=VALUE lines", () => {
    assert.deepEqual(parseDotEnv("AWS_REGION=us-east-1\nPORT=8080"), {
      AWS_REGION: "us-east-1",
      PORT: "8080",
    });
  });

  test("ignores comments, blanks and lines without a key", () => {
    const text = ["# comment", "", "   ", "not-a-pair", "=no-key", "A=1"].join("\n");
    assert.deepEqual(parseDotEnv(text), { A: "1" });
  });

  test("keeps base64 session tokens intact (the `=` padding)", () => {
    // A truncated session token is the failure mode that would look like an
    // auth error rather than a parsing error.
    const token = "IQoJb3JpZ2luX2VjEAEaCXVzLWVhc3QtMQ==+tail/tok==";
    assert.deepEqual(parseDotEnv(`AWS_SESSION_TOKEN=${token}`), {
      AWS_SESSION_TOKEN: token,
    });
  });

  test("strips one layer of surrounding quotes", () => {
    assert.deepEqual(parseDotEnv(`A="quoted value"\nB='single'`), {
      A: "quoted value",
      B: "single",
    });
  });

  test("tolerates CRLF line endings", () => {
    assert.deepEqual(parseDotEnv("A=1\r\nB=2\r\n"), { A: "1", B: "2" });
  });
});

describe("loadDotEnv", () => {
  let dir;
  let envPath;
  const saved = {};

  before(() => {
    dir = mkdtempSync(join(tmpdir(), "firemind-env-"));
    envPath = join(dir, ".env");
  });

  after(() => {
    rmSync(dir, { recursive: true, force: true });
  });

  test("returns [] for a missing file instead of throwing", () => {
    assert.deepEqual(loadDotEnv(join(dir, "does-not-exist")), []);
  });

  test("applies file values and reports the names it set", () => {
    const keys = ["FIREMIND_TEST_A", "FIREMIND_TEST_B"];
    for (const key of keys) saved[key] = process.env[key];
    writeFileSync(envPath, "FIREMIND_TEST_A=from-file\nFIREMIND_TEST_B=2\n");
    for (const key of keys) delete process.env[key];

    const applied = loadDotEnv(envPath);

    assert.deepEqual(applied.sort(), keys);
    assert.equal(process.env.FIREMIND_TEST_A, "from-file");
    assert.equal(process.env.FIREMIND_TEST_B, "2");

    for (const key of keys) {
      if (saved[key] === undefined) delete process.env[key];
      else process.env[key] = saved[key];
    }
  });

  test("a real shell variable wins over the file", () => {
    writeFileSync(envPath, "FIREMIND_TEST_C=from-file\n");
    const previous = process.env.FIREMIND_TEST_C;
    process.env.FIREMIND_TEST_C = "from-shell";

    const applied = loadDotEnv(envPath);

    assert.equal(process.env.FIREMIND_TEST_C, "from-shell");
    assert.ok(!applied.includes("FIREMIND_TEST_C"), "shell value must not be overridden");

    if (previous === undefined) delete process.env.FIREMIND_TEST_C;
    else process.env.FIREMIND_TEST_C = previous;
  });

  test("never returns values, only names", () => {
    writeFileSync(envPath, "FIREMIND_TEST_SECRET=super-secret-value\n");
    const previous = process.env.FIREMIND_TEST_SECRET;
    delete process.env.FIREMIND_TEST_SECRET;

    const applied = loadDotEnv(envPath);

    assert.deepEqual(applied, ["FIREMIND_TEST_SECRET"]);
    assert.ok(!JSON.stringify(applied).includes("super-secret-value"));

    if (previous === undefined) delete process.env.FIREMIND_TEST_SECRET;
    else process.env.FIREMIND_TEST_SECRET = previous;
  });
});
