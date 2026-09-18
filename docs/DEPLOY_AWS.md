# Deploying the FireMind backend to AWS

The app currently talks to a Node server on a laptop over Wi-Fi
(`http://192.168.0.103:8080`). This document moves that server into AWS:

```text
Fire TV app  ->  API Gateway (HTTPS)  ->  Lambda  ->  Amazon Bedrock
```

Same code, same endpoints, same responses. The app only changes its base URL.

**What this fixes, and what it does not:**

| | Local laptop | Deployed on AWS |
|---|---|---|
| Reachable from a Fire TV anywhere | no, same Wi-Fi only | **yes, real HTTPS URL** |
| Credentials | `aws login`, expires ~every hour | **function role, no keys stored, nothing to refresh** |
| Uses named AWS services | no | **API Gateway + Lambda + Bedrock** |
| Bedrock daily token quota | account-level | **account-level — unchanged** |
| Cold start | none | ~1 s on the first request after idle |

Deploying does **not** buy tokens. The quota wall is per account, so a
throttled laptop and a throttled Lambda are throttled identically.

---

## What only you can do

1. **Have Bedrock working** in the account (you do — model access is automatic
   now; the remaining gate is the daily quota).
2. **Give the terminal AWS credentials.** For this project the usual way is:

   ```bash
   aws login
   aws sts get-caller-identity     # must print your account, not "expired"
   ```

3. **Make sure those credentials may create resources.** The script needs:

   - `iam:CreateRole`, `iam:GetRole`, `iam:AttachRolePolicy`, `iam:PutRolePolicy`
   - `lambda:CreateFunction`, `UpdateFunctionCode`, `UpdateFunctionConfiguration`,
     `GetFunction`, `AddPermission`, `PutFunctionConcurrency`
   - `apigateway:POST`, `apigateway:GET`, `apigateway:PATCH`
   - `sts:GetCallerIdentity`

   On a throwaway hackathon account `AdministratorAccess` covers all of it.
   If you would rather not grant IAM permissions, use the console path below,
   or create the role by hand and pass its ARN with
   `FIREMIND_ROLE_NAME=<existing-role>`.

That is the whole list. Everything else is one command.

---

## Path A — one command (recommended)

```bash
node tools/deploy-aws.mjs                 # build, create, update, print the URL
node tools/deploy-aws.mjs --package-only  # build the zip and touch nothing in AWS
node tools/deploy-aws.mjs --no-cap        # skip the concurrency cap
```

It is idempotent: run it twice and the second run updates in place rather than
creating duplicates. It creates:

1. **IAM role `firemind-lambda-role`** — logs, plus an inline policy granting
   exactly `bedrock:InvokeModel` for the configured model. Nothing else.
2. **Lambda `firemind-backend`** — Node 20, `arm64`, 30 s timeout, 256 MiB,
   handler `lambda.handler`, environment `AWS_REGION`, `BEDROCK_MODEL_ID`,
   `BEDROCK_TIMEOUT_MS`. **No AWS keys are set**: the role supplies short-lived
   credentials per invocation.
3. **HTTP API `firemind-api`** — one `$default` route in front of the function,
   throttled to **5 req/s steady, burst 10**, with Lambda reserved concurrency
   capped at **2**. Anyone who finds the URL cannot run up a bill.

It finishes with a smoke test against the deployed URL and prints the exact
`FIREMIND_BACKEND_URL=...` command for the app.

The package is built by `tools/lib/zip.mjs`, not `Compress-Archive`, because
PowerShell writes entry names with backslashes and Lambda then cannot resolve
its own modules.

## Path B — clicking in the console

<details>
<summary>If you would rather do it by hand</summary>

1. **Lambda → Create function → Author from scratch**
   - Name: `firemind-backend`
   - Runtime: **Node.js 20.x**, Architecture: **arm64**
   - Permissions: *Create a new role with basic Lambda permissions*
2. **Upload the code.** First build the zip locally:
   `node tools/deploy-aws.mjs --package-only` → `build/lambda/function.zip`.
   Then: *Upload from → .zip file*.
3. **Runtime settings → Edit** → Handler: **`lambda.handler`** → Save.
4. **Configuration → General → Edit** → Timeout **30 s**, Memory **256 MB**.
5. **Configuration → Environment variables → Edit**:
   `AWS_REGION` = your region, `BEDROCK_MODEL_ID` = your model id,
   `BEDROCK_TIMEOUT_MS` = `12000`. (Do **not** add AWS keys here.)
6. **Configuration → Permissions → the role → Add permissions → Create inline
   policy** → JSON. The model id sits inside the ARN, so substitute the one you
   set in step 5:
   ```json
   { "Version": "2012-10-17", "Statement": [ { "Effect": "Allow",
     "Action": ["bedrock:InvokeModel"],
     "Resource": ["arn:aws:bedrock:*::foundation-model/anthropic.claude-3-haiku-20240307-v1:0"] } ] }
   ```
7. **Create the API**: API Gateway → **Create API → HTTP API → Add integration →
   Lambda** → pick `firemind-backend` → Name `firemind-api` →
   *Configure routes*: method `ANY`, resource `/{proxy+}` (plus one for `/`) →
   *Create*.
8. **Stages → `$default` → Edit** → enable **Auto-deploy**, and set throttling
   (5 req/s, burst 10).
9. **Copy the Invoke URL**, then test it in a browser: the console page opens at
   the root, and `/api/health` answers JSON.

</details>

---

## Point the app at the deployment

```bash
FIREMIND_BACKEND_URL="https://<api-id>.execute-api.<region>.amazonaws.com" ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The app's base URL is a build-time value (`FIREMIND_BACKEND_URL`), so no code
change is needed. The URL is **HTTPS**, so the cleartext exception that the
local LAN setup needed does not apply, and `network_security_config.xml` can
stay as it is.

To go back to your laptop, build again without the variable.

## Security posture, stated plainly

- **No long-lived credentials anywhere** in the deployment: the function has a
  role, the role has one action, and the credentials are minted per call.
- **The endpoint is public.** Anyone with the URL can call it. The blast radius
  is bounded by the 5 req/s throttle, the burst of 10 and the reserved
  concurrency of 2 — but the Bedrock spend is yours.
- **If you need it closed**, add an API key requirement in API Gateway and send
  the key from the app. Note honestly that a key shipped inside an APK is not a
  secret: it stops crawlers and casual abuse, not a determined person. The
  cleaner fix is Cognito or a signed token — out of scope for a hackathon demo.
- **Delete it when the demo is over** (Lambda and API Gateway have free tiers;
  Bedrock usage is what costs money):

  ```bash
  aws apigatewayv2 delete-api --api-id <api-id>
  aws lambda delete-function --function-name firemind-backend
  aws iam delete-role-policy --role-name firemind-lambda-role --policy-name firemind-bedrock-invoke
  aws iam detach-role-policy --role-name firemind-lambda-role \
      --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
  aws iam delete-role --role-name firemind-lambda-role
  ```

## How this was verified without deploying

API Gateway events are plain JSON, so the deployed shape is testable
in-process: `backend/test/lambda.test.js` invokes the real handler with real
event objects and asserts the same contract the local server serves (health,
recommendations, errors, the console page, both payload formats, base64
bodies), plus the AI contract with a stubbed Bedrock. On top of that, the built
zip is extracted and the **extracted** `lambda.mjs` is imported and invoked, so
the package — module paths, catalog data, ESM resolution — is proven to work
before anything touches AWS.

What cannot be verified here is the deployment itself, because that needs your
credentials: IAM creation, the runtime's role credentials, and the gateway
wiring are exercised for the first time on the first real run.
