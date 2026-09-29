/**
 * Amazon Bedrock adapter using the model-agnostic Converse API.
 *
 * Zero npm dependencies: AWS Signature Version 4 is implemented here with
 * node:crypto. Credentials come exclusively from environment variables
 * (AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY / AWS_REGION, optional
 * AWS_SESSION_TOKEN) - never from this repository.
 *
 * BEDROCK_ENDPOINT is a TEST-ONLY override that redirects calls to a local
 * stub implementing the Converse API, so the AI path can be exercised in
 * CI without AWS credentials. It is never set in production; leaving it
 * unset always targets the real regional Bedrock endpoint.
 */
import { createHash, createHmac } from "node:crypto";

const SERVICE = "bedrock";

function sha256Hex(data) {
  return createHash("sha256").update(data).digest("hex");
}

function hmac(key, data) {
  return createHmac("sha256", key).update(data).digest();
}

/**
 * SigV4 canonical URI: each path segment is URI-encoded a second time.
 *
 * This is the documented SigV4 rule for every service except S3, and it is
 * subtle: the request on the wire carries the single-encoded path, while the
 * canonical request signs the twice-encoded one. Signing the wire path
 * instead produces a well-formed signature that the service rejects with
 * 403 SignatureDoesNotMatch - verified against AWS's own signer by
 * backend/tools/sigv4-crosscheck.mjs.
 */
export function canonicalUri(encodedPath) {
  return encodedPath.replace(/%/g, "%25");
}

/** AWS Signature Version 4 headers for a POST request. */
export function sigv4Headers({ host, path: reqPath, canonicalPath, body, region, accessKey, secretKey, sessionToken, amzDate, dateStamp }) {
  const payloadHash = sha256Hex(body);
  const canonicalHeaders =
    `content-type:application/json\n` +
    `host:${host}\n` +
    `x-amz-content-sha256:${payloadHash}\n` +
    `x-amz-date:${amzDate}\n`;
  const signedHeaders = "content-type;host;x-amz-content-sha256;x-amz-date";

  const canonicalRequest = [
    "POST",
    canonicalPath ?? canonicalUri(reqPath),
    "",
    canonicalHeaders,
    signedHeaders,
    payloadHash,
  ].join("\n");

  const credentialScope = `${dateStamp}/${region}/${SERVICE}/aws4_request`;
  const stringToSign = [
    "AWS4-HMAC-SHA256",
    amzDate,
    credentialScope,
    sha256Hex(canonicalRequest),
  ].join("\n");

  const kDate = hmac(`AWS4${secretKey}`, dateStamp);
  const kRegion = hmac(kDate, region);
  const kService = hmac(kRegion, SERVICE);
  const kSigning = hmac(kService, "aws4_request");
  const signature = createHmac("sha256", kSigning).update(stringToSign).digest("hex");

  const authHeader =
    `AWS4-HMAC-SHA256 Credential=${accessKey}/${credentialScope}, ` +
    `SignedHeaders=${signedHeaders}, Signature=${signature}`;

  const headers = {
    "content-type": "application/json",
    host,
    "x-amz-content-sha256": payloadHash,
    "x-amz-date": amzDate,
    Authorization: authHeader,
  };
  if (sessionToken) headers["x-amz-security-token"] = sessionToken;
  return headers;
}

/**
 * Bedrock client. Constructed with explicit config so tests can inject it.
 */
export class BedrockClient {
  constructor({
    region = process.env.AWS_REGION ?? "us-east-1",
    modelId = process.env.BEDROCK_MODEL_ID ?? "us.anthropic.claude-haiku-4-5-20251001-v1:0",
    accessKey = process.env.AWS_ACCESS_KEY_ID,
    secretKey = process.env.AWS_SECRET_ACCESS_KEY,
    sessionToken = process.env.AWS_SESSION_TOKEN,
    timeoutMs = Number(process.env.BEDROCK_TIMEOUT_MS ?? 12_000),
    endpoint = process.env.BEDROCK_ENDPOINT || null,
  } = {}) {
    this.region = region;
    this.modelId = modelId;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.sessionToken = sessionToken;
    this.timeoutMs = timeoutMs;
    // Real regional endpoint by default; a stub URL only when explicitly set.
    this.baseUrl = endpoint
      ? new URL(endpoint)
      : new URL(`https://bedrock-runtime.${region}.amazonaws.com`);
  }

  get configured() {
    return Boolean(this.accessKey && this.secretKey);
  }

  /**
   * Invoke the Converse API. Returns the model text, or throws with a
   * concise message that the server logs and converts into fallback mode.
   */
  async converse({ system, prompt, maxTokens = 700 }) {
    if (!this.configured) throw new Error("bedrock-not-configured");

    // Host (and port, for a stub) must match what is signed, so it is always
    // derived from the URL actually being called.
    const host = this.baseUrl.host;
    // Wire path is encoded once; the signer twice-encodes it for the
    // canonical request (see canonicalUri).
    const reqPath = `/model/${encodeURIComponent(this.modelId)}/converse`;
    const body = JSON.stringify({
      system: system ? [{ text: system }] : undefined,
      messages: [{ role: "user", content: [{ text: prompt }] }],
      inferenceConfig: { maxTokens, temperature: 0.4 },
    });

    const now = new Date();
    const amzDate = now.toISOString().replace(/[:-]|\.\d{3}/g, "");
    const dateStamp = amzDate.slice(0, 8);

    const headers = sigv4Headers({
      host,
      path: reqPath,
      body,
      region: this.region,
      accessKey: this.accessKey,
      secretKey: this.secretKey,
      sessionToken: this.sessionToken,
      amzDate,
      dateStamp,
    });

    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), this.timeoutMs);
    try {
      const resp = await fetch(new URL(reqPath, this.baseUrl), {
        method: "POST",
        headers,
        body,
        signal: controller.signal,
      });
      const text = await resp.text();
      if (!resp.ok) {
        throw new Error(`bedrock-http-${resp.status}: ${text.slice(0, 300)}`);
      }
      const data = JSON.parse(text);
      const out = data?.output?.message?.content
        ?.map((c) => c.text ?? "")
        .join("")
        .trim();
      if (!out) throw new Error("bedrock-empty-output");
      return out;
    } finally {
      clearTimeout(timer);
    }
  }
}
