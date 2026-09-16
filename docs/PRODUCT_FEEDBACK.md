# FireMind Product Feedback

Feedback for the developer tools, APIs and SDKs actually used to build
FireMind. Written from real usage on 2026-09-16 (Windows 11, Git Bash,
OpenJDK 21, Node 24). Each verdict is based on this project only.

Concrete cost of each issue is recorded in
[FRICTION_LOG.md](FRICTION_LOG.md); this document focuses on the tools
themselves and on specific, actionable suggestions.

---

## What was used, and for what

| Tool / API / SDK | Used for |
|---|---|
| Android SDK command-line tools (`sdkmanager`, `avdmanager`) | Installing platform tools, Android 15/16 platforms, build tools, the emulator, and an Android TV system image; creating the TV AVD |
| Android Gradle Plugin 8.13.2 + Gradle 8.14.5 | Building and packaging the Fire TV app |
| Jetpack Compose for TV (`androidx.tv:tv-material` 1.1.0) | All TV UI: focus-aware buttons/surfaces, the remote-first experience |
| Jetpack DataStore, Navigation Compose, ViewModel, OkHttp, kotlinx.serialization | Persistence, navigation, state, networking, JSON |
| Android TV emulator (`system-images;android-33;android-tv;x86`) | Running and verifying FireMind on a TV environment (Fire OS stand-in) |
| `adb` + `uiautomator` | Installing the APK, driving D-pad input, verifying focus/screens, logcat crash analysis |
| Node.js 20+ (stdlib `http`, `crypto`, `fs`) and its built-in test runner | The backend API, AI orchestration, and 16 tests |
| Amazon Bedrock (Converse API) | AI reasons/summaries — implemented; request/signing/validation/fallback all executed against a local Converse stub with the signature pinned to botocore's. **Never called against live AWS** (no credentials in this environment) |
| Amazon/Android developer documentation | Verifying current guidance for TV manifests, Compose for TV, and Fire TV app expectations |

---

## Android SDK command-line tools

**What worked well**

- `sdkmanager --list` is genuinely authoritative: every package id I needed was discoverable, including the exact ABI tags for TV system images.
- `--list_installed` gave a clean, tabular confirmation of what was on disk — this is the command that caught two silent wrong-path installs.
- `avdmanager create avd -d tv_1080p` produced a working 1080p TV device profile in one command, with no Android Studio involved. Building an entire TV project with only command-line tools is a real, valuable capability.
- `--verbose` output named the exact paths being parsed, which is what made the bogus-directory problem diagnosable.

**What needs improvement**

- **Silent wrong-path installs.** An `--sdk_root` value whose separators were mangled became a *relative* path and the tool cheerfully installed there, reporting success. Rejecting a non-absolute `--sdk_root`, or logging the resolved root at INFO, would eliminate an entire class of confusing failures.
- **Exit code `0` after installing nothing.** A single unresolvable package id aborts the whole transaction, but the process still exits successfully. This is the most damaging behaviour I hit, because scripts and agents naturally trust the exit code.
- **License revisions reappearing mid-workflow.** New packages can carry new license text; an unattended install then stops at a prompt. Telling the user *which* license is new, and failing clearly on non-TTY stdin, would help.
- **No "did you mean" for package ids.** Supplying the nearest matching id (or listing available ids for the requested package family) would have turned a 10-minute detour into a 5-second fix.
- **Windows reliability.** A large SDK relocation failed with `Permission denied` and required `robocopy /MOVE`. Worth a documented note for Windows users with security/AV scanning enabled.

**Would I build with it again?** Yes — the command-line-only path is fast and complete, and the package listing is trustworthy. I would just never again trust its exit code without verifying the install.

---

## Android Gradle Plugin + Gradle + androidx versioning

**What worked well**

- The **AAR metadata check** is excellent engineering: `checkDebugAarMetadata` named the exact offending artifacts, the required `compileSdk` (37), and the required AGP (9.1.0). It failed fast and precisely instead of producing baffling runtime errors.
- Gradle wrapper bootstrapping from a pinned distribution URL meant a reproducible build with a single `./gradlew assembleDebug` and no local Gradle install.
- Fixes were fast and incremental: once pinned, builds ran in seconds and never failed for environmental reasons again.

**What needs improvement**

- **No "last compatible version" guidance.** The recommended action is always "update AGP", which is not always right: adopting AGP 9 means the built-in-Kotlin plugin model, a much larger migration. For a stability-first project, the useful answer was "pin to the previous androidx generation" — and finding that required querying Google Maven and Maven Central metadata by hand.
- **Unresolved-reference errors point away from the cause.** A missing artifact produced `e: MainActivity.kt:12:25 Unresolved reference 'material3'` — a Kotlin compile error for a missing dependency. The Compose BOM constraining versions without adding artifacts is a well-known trap whose error message does not mention dependencies at all.
- **Overload-resolution messages can be actively misleading.** A mixed-literal `mapOf` produced a candidate list about `BigDecimal.times` rather than stating the inferred receiver type. One sentence ("receiver inferred as `Number & Comparable<*>`, which has no `times`") would have saved three build cycles.

**Would I build with it again?** Yes. The version-pinning exercise was tedious but the tooling told me exactly which versions were incompatible, and the resulting build is stable.

---

## Jetpack Compose for TV

**What worked well**

- The TV-first components are genuinely TV-first: `Button`/`Surface` from `tv-material` render a visible focus border and focus scale *by default*. I wrote zero manual focus-visual code, which is a large quality win for a 10-foot UI.
- Standard Compose knowledge transfers directly. `FocusRequester`, `focusGroup()`, `focusProperties`, `LazyRow`, and `LazyVerticalGrid` all behave on TV as on mobile, so the whole navigation model is ordinary Compose.
- D-pad interaction required no special handling for activation: focused buttons respond to the centre key.
- The design system (typography sizes, high-contrast palette) is trivially adaptable to TV-safe values.

**What needs improvement**

- **No first-class way to assert focus movement.** The most serious defect I shipped unknowingly was *focus escaping into the nav rail* on UP — invisible to the compiler and to code review. Being able to write "UP from the keyboard row must land on the prompt-chips row" as a test would have caught it immediately. I ultimately verified focus by stepping the remote and dumping focused-node bounds; that workflow is powerful but manual.
- **Minor API inconsistency:** `SurfaceDefaults.colors(containerColor=…)` versus `ClickableSurfaceDefaults.colors(container=…)` — near-identical factories with different parameter names, costing a compile cycle each time.
- **Focus search can pick geometrically surprising targets** when unrelated focusables (like a nav rail) vertically overlap content rows. Explicit containment solved it, but the default heuristic warranted debugging that the API gives little visibility into.

**Would I build with it again?** Yes, without hesitation — for a TV app it is the right choice, and the default focus affordances are a real time saver.

---

## Android TV emulator (Fire TV stand-in)

**What worked well**

- A `tv_1080p` AVD with an Android TV 13 image reproduced the real interaction model: a leanback launcher, TV-sized layout, and remote input via `adb shell input keyevent`, all headless.
- TV system images include the leanback launcher and TV app expectations, so the app's `LEANBACK_LAUNCHER` intent, banner, and landscape orientation were exercised for real.
- Headless operation (`-no-window`) worked, with screenshots via `exec-out screencap`, which made automated verification feasible.

**What needs improvement**

- **ABI naming is confusing.** TV images for API 33 are published as `arm64-v8a` and `x86` — the `x86` tag runs fine on an x86_64 host, but guessing `x86_64` silently aborts the install (see FRICTION_LOG F2).
- **The need for a Fire TV–specific stand-in is real.** Amazon's Fire TV simulator has been retired, so Android TV images are the practical substitute. Testing at the matching API levels (API 28 for Fire OS 7, API 30 for Fire OS 8) closes much of the gap, but Fire TV launcher behaviour and remote-specific keys remain unverified without hardware.
- **Every TV system image includes Google Play services, unlike Fire OS.** The images report as `sdk_google_atv_x86`; there is no GMS-free `aosp`-style variant for TV the way there is for handhelds (`aosp_atd`). So "does this app run without Play services?" — a hard requirement for Fire OS — cannot be answered by simply launching an emulator. Disabling GMS works, but it also kills the bundled TV launcher, whose crash is easy to mistake for your own app's. A GMS-free TV image, or a clear label on the Google-inclusive tag, would close this directly.

**Would I build with it again?** Yes — emulator-based TV verification with adb-driven input is the fastest way to prove a TV app actually runs, and it caught three defects a compile never would have.

---

## `adb` and `uiautomator`

**What worked well**

- `adb install`, `am start -W`, `dumpsys window`, `logcat`, `input keyevent`, `exec-out screencap`, and `exec-out uiautomator dump` together form a complete, scriptable verification loop with no GUI required.
- `logcat` after clearing gave an unambiguous verdict on crashes: `AndroidRuntime FATAL EXCEPTION` with the precise cast error that pinpointed the unregistered `Application` subclass.
- The **accessibility dump** exposes Compose semantics as real nodes with text, bounds, `clickable`, and `focused` attributes — which is how I proved that focus landed on a specific element, that the watchlist button toggled to "✓ In Watchlist (remove)", and that the results list respected the runtime cap.
- `am start -W` reporting `Status: ok` plus timings made launch health measurable.

**What needs improvement**

- **Node ordering is not layout order.** Nav-rail labels sometimes appeared out of visual sequence, which made text-order assertions unreliable. Asserting on distinct content plus the focused node's bounds is the robust approach.
- **`dumpsys window` focus lines can lag navigation**, so an immediate read can show the previous screen (`topResumedActivity` showed the TV launcher even though the start intent had been accepted). Short settle delays are needed.
- No built-in "assert focus moved from A to B" primitive — which is the same gap noted for Compose TV; a D-pad focus assertion helper would be broadly useful for TV development.

**Would I build with it again?** Yes — this toolchain is what made honest, evidence-based verification possible rather than "it compiles, therefore it works".

---

## Node.js (stdlib HTTP server + built-in test runner)

**What worked well**

- Zero dependencies proved to be an asset rather than a limitation: `node:http`, `node:crypto`, and `node:fs` covered the whole backend, and `npm install` is a no-op for judges.
- `node:crypto` was sufficient to implement **AWS Signature Version 4 by hand** in about 40 readable lines — no AWS SDK, no dependency surface, fully auditable signing code.
- The built-in test runner is excellent: `node --test` with `assert/strict` gave 16 real tests (engine behaviour, HTTP contract, validation, error paths) with zero configuration and zero install.
- Global `fetch` and `AbortController` made the Bedrock Converse call short and gave clean timeout handling.

**What needs improvement**

- **`node --test test/` fails on Windows** with a `MODULE_NOT_FOUND` error routed through the CJS loader; the glob form `node --test "test/**/*.test.js"` works. Accepting a trailing-slash directory (or a clearer message) would help.
- Test runner output interleaved with child-process failures can be terse; a spawn-based integration test that fails to start its server reports `server did not start` without echoing the child's stdout (I set `stdio: "ignore"`, which was my choice, but a documented "capture child output on failure" pattern would be welcome).

**Would I build with it again?** Yes — for a demo backend, the standard library plus built-in tests is a strong default, and it removes an entire supply-chain consideration from a hackathon submission.

---

## Amazon Bedrock (Converse API)

**What worked well**

- The **Converse API** is the right abstraction: a single model-agnostic request/response shape (`messages` / `output.message.content[].text`) meant the integration has no model-specific payload schema, so switching models is an environment variable change.
- API documentation for signing and request shape was sufficient to implement SigV4 without the AWS SDK.
- Credentials are cleanly externalised as environment variables, which matched the security rule that the client app must never hold secrets.

**What needs improvement**

- **Now exercised against live AWS — up to the account's quota.** With real session credentials the signed requests are *accepted* by the service (no `SignatureDoesNotMatch`), and the model ids validate. Two new-account gates then appeared in sequence — an account verification hold, followed by a near-zero daily token budget that applies to every model — so a completed AI answer is still pending on Amazon's side, not the integration's. `backend/tools/live-check.mjs` reports which gate currently applies in plain language.
- **The degradation design earned its keep during the live test.** The moment AWS refused the call, the backend logged the exact AWS error message and served the deterministic picks in the same response shape. A viewer saw nothing but working recommendations — which is the difference between a graceful degradation and a broken demo.
- **The SigV4 canonical-URI rule is a documentation trap.** Every service except S3 signs a **twice**-URI-encoded path while sending the single-encoded one. The rule is stated in the SigV4 reference but is easy to miss next to the *request* being correct and the signature still being a plausible 64-hex string. When it is wrong, the service returns `403 SignatureDoesNotMatch` with no indication of *which* component of the canonical request differed. Documenting the rule next to the Bedrock endpoint documentation — and naming the mismatched component in the 403 message — would have saved a real debugging session.
- **`x-amz-content-sha256` is not signed by botocore for non-S3 services,** which makes "diff my signature against the SDK's" harder than it should be: the two sign different header sets until you align them by hand. A documented canonical request (or a debug mode that echoes the exact string-to-sign) would remove that step.
- **The local testing story is genuinely thin — but cheap to fix.** The improvement I suggested while building this turned out to be the thing that made verification possible at all: a ~40-line `node:http` stub that returns a Converse-shaped envelope let the whole AI path run with no account, no credentials and no network. That deserves to be a documented, supported option rather than something each developer invents.
- **New-account gating is invisible until it isn't.** `aws login` succeeds, `sts get-caller-identity` answers, every other service works — and Bedrock alone returns a 403 asking you to wait, then a 429 asking you to wait longer. None of this is visible at signup. Telling builders about the verification window and starter token budget at account creation would save a day of collective guessing per hackathon cohort.

**Would I build with it again?** Yes — the Converse API and SigV4 design are pleasant, and the model-agnostic shape is exactly what a small project wants. I would verify it live with credentials before submitting.

---

## Amazon/Android documentation and Fire TV guidance

**What worked well**

- The Compose for TV release notes and the "Use Jetpack Compose on Android TV" training page gave correct, current guidance (stable `tv-material` 1.0+, minimum API level, the `LEANBACK_LAUNCHER` category requirement, and the leanback/touchscreen `uses-feature` declarations needed for a TV app that also installs for development).
- Amazon's own Fire TV + Kotlin/Compose guidance confirmed that a Compose TV app is a supported path for Fire TV, which is why I chose Kotlin over cross-platform alternatives.

**What needs improvement**

- **Fire OS version skew is hard to pin down quickly.** The relationship between Fire OS generations and their underlying Android API levels is scattered, and it directly determines `minSdk`. A single canonical compatibility table (Fire OS version → Android API level → supported devices) would remove guesswork; I settled on `minSdk 23` as a deliberately conservative floor.
- **The retired Fire TV simulator is a gap.** With no first-party simulator, developers fall back to Android TV images and cannot fully verify Fire OS-specific behaviour. Clear, maintained guidance on the recommended local test target for Fire TV would be valuable.

**Would I build for it again?** Yes — TV development with Compose is a good experience, and the deterministic degradation strategy means a Fire TV app can ship without an AI service ever being a hard dependency.
