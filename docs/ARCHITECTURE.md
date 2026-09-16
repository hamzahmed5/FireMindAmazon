# FireMind Architecture

## System overview

```text
+----------------------------------------+
|            Fire TV app                 |
|  Kotlin 2.4 / Jetpack Compose for TV   |
|                                        |
|  Nav rail  Home  Ask  Browse           |
|            Results  Details  Watchlist |
|                                        |
|  FireMindViewModel                     |
|   - AI-first request                   |
|   - deterministic local fallback       |
|  DataStore (watchlist persistence)     |
+-------------------+--------------------+
                    |
                    |  HTTP/JSON (cleartext permitted only for
                    |  loopback + 10.0.2.2 dev hosts)
                    v
+----------------------------------------+
|        FireMind backend                |
|  Node.js >= 20, zero npm dependencies  |
|                                        |
|  server.js      routing + validation   |
|  lib/catalog.js filtering + ranking    |
|  lib/ai.js      prompt + JSON contract |
|  lib/bedrock.js Bedrock Converse, SigV4|
+-------------------+--------------------+
                    |
                    |  HTTPS, SigV4-signed
                    v
+----------------------------------------+
|        Amazon Bedrock                  |
|  Converse API (model-agnostic)         |
|  credentials from environment only     |
+----------------------------------------+
```

## Request flow: "I want a mind-bending sci-fi movie under two hours"

```text
1. Home screen: mood chip or Ask button  (D-pad, one press)
2. Assistant screen: preset prompt selected (no remote typing needed)
3. FireMindViewModel.ask(query)
     -> FireMindClient.recommend()  POST /api/recommend
4. Backend:
     a. validate query (non-empty, <= 500 chars)
     b. parse intent: mood = Mind-bending, runtimeMax = 120
        ("two hours" -> 120 via word-number parsing)
     c. filter catalog: runtime <= 120 (+ family flag if requested)
     d. if Bedrock configured:
          prompt with catalog + candidate ids -> strict JSON
          validate ids/shape; any violation raises -> caught
        else: deterministic ranking
     e. respond { source, recommendations[] }
5. App renders results with a per-title "why this" line and an
   honest AI / curated-picks badge.
6. Selecting a card opens Details; "Add to Watchlist" persists to
   DataStore; "Recommend something similar" calls /api/similar.
```

Verified on the emulator: the two-hour request returns 104/118/113-minute
titles with no over-cap leak.

## Signing, and how it was verified

`lib/bedrock.js` signs requests itself with `node:crypto` (~40 lines rather
than an SDK). Two details are easy to get wrong, so both are pinned by
tests:

- **The canonical URI is encoded twice.** Every SigV4 service except S3
  signs a twice-encoded path, while the request on the wire carries the
  single-encoded one (`/model/<id>/converse`, with the model id's `:` as
  `%3A`). Signing the wire path produces a valid-looking signature that the
  service rejects with `403 SignatureDoesNotMatch`.
- **`x-amz-content-sha256` is signed.** AWS's own SDK omits it for non-S3
  services; including it is permitted and binds the signature to the exact
  bytes sent.

Correctness is established by comparison against AWS's own signer rather
than by self-consistency. `tools/sigv4-oracle.py` asks botocore to build and
sign the identical Converse request (dummy credentials, no network, pinned
clock), and `tools/sigv4-crosscheck.mjs` recomputes the signature with
FireMind's signer and diffs the two. The resulting signature is frozen into
`test/bedrock.test.js` as a known-answer test, so the suite keeps checking
AWS's value rather than our own.

Neither tool is a runtime dependency: botocore is not required to run or test
the backend. What this does *not* establish is that a given AWS account has
Bedrock model access — see Known limitations in the README.

## Degradation strategy (the product never dead-ends)

Three independent failure points, each with a defined fallback:

| Failure | Behavior |
|---|---|
| Backend unreachable (offline, wrong URL, timeout) | App's local engine ranks the bundled catalog; results still appear, badged *curated picks* |
| AI not configured / credentials missing | Backend serves deterministic ranking; `/api/health` reports `aiConfigured: false` |
| AI call fails, times out, or violates the JSON contract | Backend logs the reason and returns the deterministic fallback in the same response shape |

The local engine and the backend engine implement the same intent parsing
and the same ranking rules, so the experience is consistent regardless of
which path serves the request:

- **Moods** — curated synonyms (`relaxing` → `Cozy`, `hilarious` →
  `Funny`) then direct matches against mood tags that exist in the catalog
- **Genres** — curated synonyms (`space`, `scifi` → `Sci-Fi`;
  `romantic` → `Romance`) then direct matches against real genre tags
- **Runtime cap** — digits and spelled-out numbers (`under 2 hours`,
  `under two hours` → 120 minutes)
- **Audience** — family-friendly filtering from an explicit flag or
  audience wording (`family`, `kids`, `children`, `animated`)
- **Ranking** — mood match, then genre match, then rating; ties keep
  catalog order, so results are deterministic

Intent parsing deliberately matches only against tags that exist in the
catalog, so a chip or phrase can never "match" a tag that no title
carries. The UI chips are covered by an invariant test asserting that each
one resolves to a real mood or genre — that test caught two chips
(`Sci-Fi`, `Family`) that previously constrained nothing because the
engine had no genre concept (see FRICTION_LOG.md F17).

Both engines are covered by mirrored test suites (32 app tests, 21
backend tests), and the app-side copy is deliberately small and
dependency-free. Keeping them in step is a manual discipline: changing
one means changing the other, and the synonym tables were verified
identical when this document was written.

## UI and focus model (TV-first)

- Single-activity Compose app; `NavHost` routes: `home`, `assistant`,
  `results`, `browse`, `details/{movieId}`, `watchlist`, `settings`.
- A persistent left nav rail is reachable with LEFT; content is reachable
  with RIGHT.
- Every destination claims initial focus on its primary action via
  `FocusRequester` (Ask button, first recommendation card, watchlist
  toggle, first catalog card), so the remote is never aimless.
- `focusGroup()` on the prompt-chips row and keyboard rows keeps UP/DOWN
  navigation inside the Assistant screen; the chips row blocks upward
  escape with `focusProperties { up = FocusRequester.Cancel }`, which
  prevents the classic "D-pad jumps into the nav rail" defect. Found and
  fixed on the emulator (see FRICTION_LOG.md).
- All interactive elements are `Surface`/`Button` from
  `androidx.tv:tv-material`, which provide focus scale and focus border
  rendering by default — no mouse or touch path required.

## State model

`AssistantUiState` is explicit and exhaustive: `Idle`, `Loading`,
`Results(query, response, fromAi)`, `Error(message)`. The results screen
renders a spinner while loading and never blocks the main thread (network
work runs on `Dispatchers.IO` inside the ViewModel's coroutine scope, with
4s connect / 20s read timeouts).

## Data

- **Catalog**: 60 original fictional titles. Canonical copy ships in the
  app's assets; the backend serves a copy at `backend/data/catalog.json`.
  A drift-guard test asserts the two files are byte-identical, so they
  cannot silently diverge.
- **Watchlist**: Jetpack DataStore preferences, a set of title ids.
  Verified to survive `force-stop` and relaunch.
- **No user data leaves the device** beyond the query text sent to the
  backend. There is no account system, no analytics, no tracking.

## Security posture

| Concern | Approach |
|---|---|
| Credentials | Environment variables only (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, optional `AWS_SESSION_TOKEN`). Nothing hardcoded; the app holds no secrets at all |
| Cleartext HTTP | Restricted by `network_security_config.xml` to `10.0.2.2`, `localhost`, `127.0.0.1` — dev hosts only. Production would use HTTPS |
| Request handling | Body size capped at 8 KB; JSON parse errors and missing/invalid fields rejected with 400; unknown ids rejected with 404 |
| AI prompt injection | Model output is never trusted structurally: ids are validated against the catalog, unknown ids dropped, and any contract violation falls back. The model cannot inject titles or executable content into the UI |
| Sensitive logging | Server logs AI failure reasons only; no credentials or request bodies are logged |
| Secrets in repo | `.gitignore` covers `local.properties`, `.env*`, keystores, credential JSON. Audited across the full git history (see FRICTION_LOG/README notes) |

## Why zero npm dependencies

The backend uses only `node:http`, `node:crypto`, and `node:fs`. Benefits:
no supply-chain surface, `npm install` is a no-op for judges, and SigV4
signing is ~40 readable lines rather than an SDK. The tradeoff is manual
implementation of signing, which is why it is cross-checked against
botocore and pinned with a known-answer test (see *Signing, and how it was
verified* above) — a hand-rolled signer that is merely self-consistent
proves nothing.
