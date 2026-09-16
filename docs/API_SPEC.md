# FireMind Backend API Specification

Base URL (development): `http://localhost:8080` on the host machine.
From an Android TV emulator the app reaches the host at
`http://10.0.2.2:8080`. On Fire TV hardware, use the host's LAN IP.

All requests and responses are `application/json; charset=utf-8`.
There is no authentication in the MVP: the backend is intended to run on a
trusted local network for the demo. Do not expose it to the public
internet without adding auth, rate limiting, and TLS termination.

## Response source semantics

Every recommendation response carries a `source` field:

| `source` | Meaning |
|---|---|
| `"ai"` | Amazon Bedrock generated the reasons/summaries; ids and metadata were validated against the catalog before returning |
| `"fallback"` | Deterministic catalog ranking was used — either no AI is configured, or the AI call failed/returned an invalid contract |

The app renders `"ai"` as an **AI** badge and `"fallback"` as
**curated picks**. The frontend never parses model prose; the backend
always returns the validated JSON contract below.

## Catalog item schema

The catalog is the single source of truth for both app and backend
(`app/src/main/assets/catalog.json`, copied to `backend/data/catalog.json`
and locked by a drift-guard test).

```json
{
  "id": "fv003",
  "title": "Midnight Cartography",
  "year": 2021,
  "genres": ["Sci-Fi", "Mystery"],
  "moods": ["Mind-bending", "Serious"],
  "runtime": 104,
  "familyFriendly": false,
  "rating": 8.4,
  "description": "A mapmaker discovers streets that only exist between 2 and 3 a.m., and something is drawing them."
}
```

All 60 titles are original fictional metadata created for this project.

---

## `GET /api/health`

Liveness plus AI configuration status. The app's About screen calls this.

**Request body:** none.

**200 response**

```json
{
  "status": "ok",
  "aiConfigured": false,
  "model": null,
  "catalogSize": 60
}
```

| Field | Type | Notes |
|---|---|---|
| `status` | string | Always `"ok"` when the process is serving |
| `aiConfigured` | boolean | `true` when AWS credentials are present |
| `model` | string \| null | Bedrock model id, `null` when AI is off |
| `catalogSize` | number | Number of loaded titles |

---

## `POST /api/recommend`

Natural-language viewing request → validated recommendations.

**Request**

```json
{
  "query": "I want something funny under 100 minutes",
  "filters": {
    "runtimeMax": 120,
    "familyOnly": false
  }
}
```

| Field | Required | Notes |
|---|---|---|
| `query` | yes | Non-empty string, max 500 chars |
| `filters.runtimeMax` | no | Minutes cap; also parsed from the query text |
| `filters.familyOnly` | no | Restrict to `familyFriendly: true` titles |

The parser also understands spelled-out runtimes (`"under two hours"` →
120 minutes) and mood synonyms (`relaxing` → `Cozy`, `hilarious` →
`Funny`, `twist`/`smart` → `Mind-bending`).

**200 response**

```json
{
  "source": "fallback",
  "recommendations": [
    {
      "id": "fv002",
      "title": "Quantum Recipe",
      "year": 2022,
      "genres": ["Sci-Fi", "Comedy"],
      "runtime": 96,
      "rating": 7.4,
      "reason": "You asked for a Funny mood and under 100 minutes. \"Quantum Recipe\" fits: funny/whimsical, 96 min, rated 7.4.",
      "summary": "A chaotic home chef invents a microwave that cooks dinner from parallel universes."
    }
  ]
}
```

Returns 3–4 recommendations. The AI path may return 4; the fallback path
returns up to 4.

**Error responses**

| Status | Body | When |
|---|---|---|
| `400` | `{"error":"query is required"}` | Missing/empty query |
| `400` | `{"error":"query too long (max 500 chars)"}` | Query over 500 chars |
| `400` | `{"error":"invalid-json"}` | Body is not valid JSON |
| `400` | `{"error":"payload-too-large"}` | Body over 8 KB |
| `500` | `{"error":"internal error"}` | Unexpected server fault |

A failed or malformed AI response is **not** an error to the client — the
server logs it (`[firemind] AI recommend failed...`) and returns the
deterministic fallback with `source: "fallback"`.

---

## `POST /api/summarize`

Spoiler-safe one-to-two sentence summary for one title.

**Request**

```json
{ "id": "fv010" }
```

**200 response**

```json
{ "summary": "The last diner in low Earth orbit serves one perfect cup of coffee to whoever needs it most." }
```

With AI configured, the summary is model-generated (constrained to the
setup, no spoilers) and bounded to 120 output tokens; otherwise the
catalog description is returned.

**Error responses**

| Status | Body | When |
|---|---|---|
| `400` | `{"error":"invalid-json"}` | Body is not valid JSON |
| `404` | `{"error":"unknown id"}` | Id not present in the catalog |

---

## `POST /api/similar`

Titles similar in mood/genre to a given title.

**Request**

```json
{ "id": "fv022" }
```

**200 response** — same shape as `/api/recommend`.

```json
{
  "source": "fallback",
  "recommendations": [
    {
      "id": "fv003",
      "title": "Midnight Cartography",
      "year": 2021,
      "genres": ["Sci-Fi", "Mystery"],
      "runtime": 104,
      "rating": 8.4,
      "reason": "Similar to \"Deep Field\": shares its mind-bending/serious tone.",
      "summary": "A mapmaker discovers streets that only exist between 2 and 3 a.m., and something is drawing them."
    }
  ]
}
```

The source title is always excluded. Fallback scoring weights mood
overlap ×10, genre overlap ×5, then rating.

**Error responses**

| Status | Body | When |
|---|---|---|
| `400` | `{"error":"invalid-json"}` | Body is not valid JSON |
| `404` | `{"error":"unknown id"}` | Id not present in the catalog |

---

## Unmatched routes

| Status | Body |
|---|---|
| `404` | `{"error":"not found"}` |

Method mismatches on known paths also fall through to this 404 (the MVP
router matches method + path pairs).

---

## AI contract enforcement

When Bedrock is configured, the server sends the model:

- the full compact catalog (ids + metadata),
- the filtered candidate ids it may choose from,
- the user request and active constraints,

and requires exactly:

```json
{"recommendations":[{"id":"fvNNN","reason":"...","summary":"..."}]}
```

The server then:

1. strips markdown fences / surrounding prose and parses the first JSON object,
2. drops any id not in the catalog or already used (deduplication),
3. substitutes a deterministic reason/summary if a field is blank,
4. caps the list at 4,
5. treats *any* violation as `ai-contract-violation` and falls back.

This is why the app can trust the shape it receives.

## Verified behavior

The contract is enforced by `backend/test/api.test.js` (live HTTP against a
spawned server) and `backend/test/catalog.test.js` (engine + parsing).
`npm test` runs 16 tests, including the catalog drift guard.
