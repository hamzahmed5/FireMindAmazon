/**
 * SigV4 cross-check - dev tool, NOT part of the shipped backend.
 *
 * Reads the JSON emitted by backend/tools/sigv4-oracle.py (a request signed
 * by AWS's own botocore library) on stdin, recomputes the signature with
 * FireMind's signer for the identical host, path, body and timestamp, and
 * compares them.
 *
 *   python backend/tools/sigv4-oracle.py --json | node backend/tools/sigv4-crosscheck.mjs
 *
 * Exits non-zero on any mismatch, so it can be used as a gate when the
 * signing implementation changes.
 */
import { createHash } from "node:crypto";
import { sigv4Headers } from "../lib/bedrock.js";

let input = "";
for await (const chunk of process.stdin) input += chunk;

const oracle = JSON.parse(input);
const bodyHash = createHash("sha256").update(oracle.body).digest("hex");

const ours = sigv4Headers({
  host: oracle.host,
  path: oracle.path,
  body: oracle.body,
  region: oracle.region,
  accessKey: oracle.access_key,
  secretKey: oracle.secret_key,
  amzDate: oracle.amz_date,
  dateStamp: oracle.amz_date.slice(0, 8),
});

const ourSignature = ours.Authorization.split("Signature=").pop();

const checks = [
  ["payload hash covers the same bytes", bodyHash === oracle.payload_sha],
  ["payload hash is signed", ours["x-amz-content-sha256"] === oracle.payload_sha],
  ["signed header set matches AWS", ours.Authorization.includes(`SignedHeaders=${oracle.signed_headers}`)],
  ["credential scope matches AWS", ours.Authorization.includes(`Credential=${oracle.credential_scope}`)],
  ["signature matches AWS byte-for-byte", ourSignature === oracle.signature],
];

console.log(`AWS host:        ${oracle.host}`);
console.log(`AWS path:        ${oracle.path}`);
console.log(`AWS amz-date:    ${oracle.amz_date}`);
console.log(`AWS signed:      ${oracle.signed_headers}`);
console.log(`AWS signature:   ${oracle.signature}`);
console.log(`ours signature:  ${ourSignature}`);
console.log("");
let failed = 0;
for (const [label, ok] of checks) {
  console.log(`${ok ? "PASS" : "FAIL"}  ${label}`);
  if (!ok) failed += 1;
}
console.log("");
console.log(failed === 0 ? "SigV4 cross-check: MATCH" : `SigV4 cross-check: ${failed} MISMATCH(es)`);
process.exit(failed === 0 ? 0 : 1);
