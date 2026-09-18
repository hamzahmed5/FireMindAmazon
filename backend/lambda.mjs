/**
 * FireMind backend on AWS Lambda, behind API Gateway.
 *
 *   TV app -> API Gateway (HTTPS) -> this handler -> Amazon Bedrock
 *
 * The routing is the same module the local server uses (lib/router.js), so the
 * deployed API and the laptop API answer identically and the app needs only a
 * different base URL.
 *
 * Credentials: none are stored here. Lambda sets AWS_ACCESS_KEY_ID /
 * AWS_SECRET_ACCESS_KEY / AWS_SESSION_TOKEN / AWS_REGION in the environment
 * from the function's execution role, and lib/bedrock.js already reads exactly
 * those variables. So the AI calls are made with short-lived role credentials
 * that AWS mints per invocation - which also removes the hourly `aws login`
 * refresh the local setup needs. The role still needs bedrock:InvokeModel, and
 * the account's Bedrock quota still applies: a deployment does not buy tokens.
 *
 * Supports both payload formats - HTTP API (v2, used by tools/deploy-aws.mjs)
 * and REST API (v1) - so either gateway works.
 */
import { BedrockClient } from "./lib/bedrock.js";
import { createRouter } from "./lib/router.js";

const bedrock = new BedrockClient();
const handle = createRouter({ bedrock });

/** Normalize the two API Gateway event shapes into one request. */
function toRequest(event) {
  // v2: requestContext.http.{method,path}; v1: httpMethod/path.
  const method = event.requestContext?.http?.method ?? event.httpMethod ?? "GET";
  const path = event.rawPath ?? event.path ?? "/";
  const rawBody =
    event.body == null
      ? ""
      : event.isBase64Encoded
        ? Buffer.from(event.body, "base64").toString("utf8")
        : event.body;

  return { method, path, rawBody, headers: event.headers ?? {} };
}

export async function handler(event, context) {
  // Cold-start visibility in CloudWatch: says whether the role's credentials
  // were picked up, without ever logging a value.
  console.log(
    JSON.stringify({
      requestId: context?.awsRequestId,
      method: event.requestContext?.http?.method ?? event.httpMethod ?? "GET",
      path: event.rawPath ?? event.path ?? "/",
      aiConfigured: bedrock.configured,
      model: bedrock.configured ? bedrock.modelId : null,
    })
  );

  const result = await handle(toRequest(event));

  return {
    statusCode: result.status,
    headers: {
      "content-type": result.contentType,
      // The TV app is a native client and sends no Origin, so this only ever
      // applies to the browser console at GET /.
      "cache-control": "no-store",
    },
    body: result.body,
  };
}
