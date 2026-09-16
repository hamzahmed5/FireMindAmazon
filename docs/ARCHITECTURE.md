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

## Degradation strategy (the product never dead-ends)

Three independent failure points, each with a defined fallback:

| Failure | Behavior |
|---|---|
| Backend unreachable (offline, wrong URL, timeout) | App's local engine ranks the bundled catalog; results still appear, badged *curated picks* |
| AI not configured / credentials missing | Backend serves deterministic ranking; `/api/health` reports `aiConfigured: false` |
| AI call fails, times out, or violates the JSON contract | Backend logs the reason and returns the deterministic fallback in the same response shape |

The local engine and the backend engine implement the same intent parsing
(mood synonyms, digit and word-number runtime caps, family filtering) and
the same ranking rules, so the experience is consistent regardless of
which path serves the request. Both are covered by tests, and the app-side
copy is deliberately small and dependency-free.

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
implementation of signing (covered by a live smoke test against the real
Bedrock endpoint is *not* possible without credentials — see Known
Limitations in the README).
