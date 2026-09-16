#!/usr/bin/env python3
"""
SigV4 oracle - dev tool, NOT part of the shipped backend.

FireMind's Bedrock adapter signs requests with its own SigV4 implementation
(node:crypto only, no AWS SDK). This script asks AWS's own signing code
(botocore - the library behind the AWS CLI and boto3) to build and sign the
same Converse request, then emits the authoritative host, path, timestamp,
body and signature as JSON.

Feeding that JSON to backend/tools/sigv4-crosscheck.mjs recomputes the
signature with FireMind's signer and compares the two. Matching signatures
mean our canonical request is byte-identical to AWS's, which is evidence the
hand-rolled signer is actually correct rather than merely self-consistent.

No credentials or network are involved: the client is given dummy keys, and
a before-send hook captures the prepared request instead of transmitting it.

Usage (botocore is deliberately NOT a runtime project dependency):

    pip install botocore
    python backend/tools/sigv4-oracle.py --json | node backend/tools/sigv4-crosscheck.mjs

The fixture below is mirrored by KNOWN_ANSWER in backend/test/bedrock.test.js.
Change one, change the other.
"""
import datetime
import hashlib
import io
import json
import sys

import botocore.auth as botocore_auth
from botocore.awsrequest import AWSResponse
from botocore.config import Config
from botocore.session import Session

# Pin the signing clock. botocore reads it through this module-level name, so
# the emitted signature becomes a stable known-answer value.
botocore_auth.get_current_datetime = lambda: datetime.datetime(
    2026, 1, 1, 12, 0, 0, tzinfo=datetime.timezone.utc
)

# Pinned signing clock, so the emitted signature is a stable known-answer
# value that can be frozen into the test suite.
FIXED_AMZ_DATE = "20260101T120000Z"

# --- Fixture: mirrored by the known-answer test in backend/test/bedrock.test.js
REGION = "us-east-1"
ACCESS_KEY = "AKIDEXAMPLE"
SECRET_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY"
MODEL_ID = "anthropic.claude-3-haiku-20240307-v1:0"
SYSTEM_TEXT = "You are FireMind, a TV viewing companion."
PROMPT_TEXT = "hi"
MAX_TOKENS = 700
TEMPERATURE = 0.4
# ---------------------------------------------------------------------------

ENVELOPE = {
    "output": {"message": {"role": "assistant", "content": [{"text": "ok"}]}},
    "stopReason": "end_turn",
    "usage": {"inputTokens": 1, "outputTokens": 1},
}


class RawBody:
    """Minimal raw-response body: botocore reads it via .stream()."""

    def __init__(self, data: bytes):
        self._buffer = io.BytesIO(data)

    def stream(self, amt: int = 1024, **_kwargs):
        while True:
            chunk = self._buffer.read(amt)
            if not chunk:
                return
            yield chunk


def build_signed_request() -> dict:
    captured = {}

    session = Session()
    client = session.create_client(
        "bedrock-runtime",
        region_name=REGION,
        # The real host, so the signed Host header matches production. The
        # before-send hook below means nothing is ever transmitted.
        endpoint_url=f"https://bedrock-runtime.{REGION}.amazonaws.com",
        aws_access_key_id=ACCESS_KEY,
        aws_secret_access_key=SECRET_KEY,
        config=Config(retries={"max_attempts": 0}, connect_timeout=1, read_timeout=1),
    )

    def inject_content_sha256(request, **_kwargs):
        """Align the signed header sets before signing.

        botocore omits x-amz-content-sha256 for non-S3 services while
        FireMind signs it, so inject it here to make the resulting
        signatures directly comparable.
        """
        body = request.body
        if body is None:
            body = b""
        if isinstance(body, str):
            body = body.encode("utf-8")
        request.headers["x-amz-content-sha256"] = hashlib.sha256(body).hexdigest()

    def capture(request, **_kwargs):
        captured["url"] = request.url
        captured["headers"] = {
            k.lower(): (v.decode("utf-8") if isinstance(v, bytes) else v)
            for k, v in dict(request.headers).items()
        }
        captured["body"] = request.body
        return AWSResponse(
            request.url,
            200,
            {"content-type": "application/json"},
            RawBody(json.dumps(ENVELOPE).encode("utf-8")),
        )

    client.meta.events.register("before-sign.bedrock-runtime.Converse", inject_content_sha256)
    client.meta.events.register("before-send.bedrock-runtime.Converse", capture)

    client.converse(
        modelId=MODEL_ID,
        system=[{"text": SYSTEM_TEXT}],
        messages=[{"role": "user", "content": [{"text": PROMPT_TEXT}]}],
        inferenceConfig={"maxTokens": MAX_TOKENS, "temperature": TEMPERATURE},
    )

    headers = captured["headers"]
    body = captured["body"]
    if isinstance(body, bytes):
        body = body.decode("utf-8")
    authorization = headers["authorization"]
    url = captured["url"]
    path = url.split(f"bedrock-runtime.{REGION}.amazonaws.com", 1)[1]

    return {
        "host": f"bedrock-runtime.{REGION}.amazonaws.com",
        "path": path,
        "region": REGION,
        "access_key": ACCESS_KEY,
        "secret_key": SECRET_KEY,
        "amz_date": headers["x-amz-date"],
        "payload_sha": headers["x-amz-content-sha256"],
        "signed_headers": authorization.split("SignedHeaders=")[1].split(",")[0],
        "credential_scope": authorization.split("Credential=")[1].split(",")[0],
        "signature": authorization.rsplit("Signature=", 1)[1],
        "authorization": authorization,
        "body": body,
    }


def main() -> int:
    fixture = build_signed_request()
    if "--json" in sys.argv:
        print(json.dumps(fixture))
        return 0

    for key in (
        "host", "path", "amz_date", "payload_sha", "signed_headers",
        "credential_scope", "signature",
    ):
        print(f"ORACLE_{key.upper()}={fixture[key]}")
    print("ORACLE_BODY=" + fixture["body"])
    return 0


if __name__ == "__main__":
    sys.exit(main())
