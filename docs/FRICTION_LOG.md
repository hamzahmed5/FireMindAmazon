# FireMind Friction Log

Real obstacles encountered while building FireMind on 2026-09-16 (Windows
11, Git Bash, OpenJDK 21, Node 24). Each entry is something that actually
happened and cost time — nothing here is hypothetical or fabricated.

---

## F1 — `sdkmanager --sdk_root` installed into a bogus directory

- **Date:** 2026-09-16
- **Tool:** Android SDK command-line tools (`sdkmanager.bat` 19.0)
- **Task:** Install `platform-tools`, `platforms;android-35`, `build-tools;35.0.0` into `C:\firemind-tools\android-sdk`
- **Expected:** Packages land in the SDK root I passed
- **Actual:** Packages landed in a *literal directory named* `C:firemind-toolsandroid-sdk` created relative to the working directory. The install reported success; the SDK root stayed empty.
- **Error:** No error at all — a plain `BUILD`/download success. Discovered via `sdkmanager --verbose`, which printed `Info: Parsing C:firemind-toolsandroid-sdk\platforms\android-36\package.xml`
- **Root cause:** Passed `--sdk_root=C:\\firemind-tools\\android-sdk` through Git Bash. One layer of escaping collapsed, and then the remaining `\f` / `\a` sequences in the Windows path were consumed as escape sequences by the argument parser, deleting the separators and turning the path relative.
- **Impact:** ~15 minutes; two separate installs went to the wrong place before diagnosis. Silent wrong-path installs are much worse than loud failures.
- **Workaround:** Use forward slashes (`--sdk_root=C:/firemind-tools/android-sdk`), which nothing eats, or run from the SDK directory with no `--sdk_root` at all.
- **Reproduction:** `sdkmanager --sdk_root=C:\\a\\b "platforms;android-36"` from Git Bash on Windows.
- **Actionable recommendation:** Have `sdkmanager` reject or warn about an `--sdk_root` that does not resolve to an absolute path, and log the *resolved* SDK root at INFO level rather than only at `--verbose`.

## F2 — A nonexistent system-image tag silently aborts the whole install transaction

- **Date:** 2026-09-16
- **Tool:** Android SDK command-line tools (`sdkmanager.bat`)
- **Task:** Install `platforms;android-36`, `emulator`, and an Android TV system image in one command
- **Expected:** The valid packages install; the invalid one is reported
- **Actual:** Nothing installed at all. Exit code `0`.
- **Error:** `Warning: Failed to find package 'system-images;android-33;android-tv;x86_64'` followed by a normal-looking exit
- **Root cause:** I assumed a 64-bit host needs the `x86_64` ABI tag. Android TV system images for API 33 are only published as `arm64-v8a` and `x86` — the image is labelled `x86` even though it runs on x86_64 hosts.
- **Impact:** One wasted install cycle; the exit code `0` made it look like a success until the directory listing was checked.
- **Workaround:** Always enumerate real ids with `sdkmanager --list` and match against the published tags instead of guessing.
- **Actionable recommendation:** Return a non-zero exit code when any requested package cannot be resolved, and print the closest matching ids (e.g. "did you mean `system-images;android-33;android-tv;x86`?"). The TV image ABI naming (`x86` for a 64-bit-capable image) also deserves an explicit note in the Fire TV/TV setup documentation.

## F3 — License acceptance reappears for new packages and stops an unattended install

- **Date:** 2026-09-16
- **Tool:** Android SDK command-line tools
- **Task:** Install `emulator` + TV system image after having already run `sdkmanager --licenses`
- **Expected:** Licenses were accepted, so the install proceeds
- **Actual:** The install stopped at a license text prompt, even though input was piped (`yes |`). The output ended mid-license with no completion and no error.
- **Error:** None — output simply ended at the license text.
- **Root cause:** Newly added packages (emulator / TV images) can introduce license revisions that were not present when `--licenses` was last run.
- **Impact:** Two further attempts before recognising the pattern.
- **Workaround:** Re-run `yes | sdkmanager --licenses` immediately before every install batch, and verify with `sdkmanager --list_installed` afterwards.
- **Actionable recommendation:** State in the license output *which* license is new, and make `sdkmanager` fail loudly when stdin is not a TTY rather than exiting silently.

## F4 — Moving SDK directories on Windows: `mv` denied, robocopy required

- **Date:** 2026-09-16
- **Tool:** Git Bash `mv` / `robocopy`
- **Task:** Relocate the mistakenly-nested SDK directories (see F1) into the real SDK root
- **Expected:** `mv firemind-toolsandroid-sdk/* .`
- **Actual:** `mv: cannot move 'firemind-toolsandroid-sdk/platforms' to './platforms': Permission denied` — while sibling directories moved fine
- **Error:** `Permission denied` on a directory that was not in use by any visible process (likely antivirus/OneDrive folder scanning)
- **Impact:** ~5 minutes; also left an empty directory that `rmdir` subsequently refused to delete.
- **Workaround:** `cmd /c robocopy <src> <dst> /E /MOVE`, which tolerates the transient locks.
- **Actionable recommendation:** Not really an SDK issue — but Windows SDK documentation could note that file locks from security tooling make large SDK relocations unreliable and that `robocopy /MOVE` is the dependable approach.

## F5 — Latest androidx and OkHttp require compileSdk 37 + AGP 9.1

- **Date:** 2026-09-16
- **Tool:** Android Gradle Plugin 8.13.2 / androidx / OkHttp
- **Task:** Build with current stable library versions (Compose BOM 2026.09.00, navigation 2.10.1, OkHttp 5.5.0)
- **Expected:** A build with the newest stable releases
- **Actual:** `:app:checkDebugAarMetadata` failed with **27 issues**
- **Error:** `Dependency 'androidx.navigation:navigation-compose-android:2.10.1' requires libraries and applications that depend on it to compile against version 37 or later of the Android APIs` and `requires Android Gradle plugin 9.1.0 or higher` — plus the same for `compose.foundation:1.12.1`, `activity:1.13.0`, and `okhttp-android:5.5.0`
- **Root cause:** The newest androidx generation targets API 37 and AGP 9.x. AGP 8.13.2 (the latest 8.x) cannot satisfy it, and AGP 9 changes the Kotlin plugin model (built-in Kotlin), which is a much larger migration than a hackathon build should take on mid-flight.
- **Impact:** ~20 minutes of version archaeology; three failed builds. Also required querying Google/Maven metadata directly to find the last compatible generation.
- **Workaround:** Pin to the coherent compileSdk-36 generation, verified from live metadata: Compose BOM `2026.06.00` (foundation/ui 1.11.3, material3 1.4.0), navigation `2.9.8`, activity-compose `1.12.4`, lifecycle `2.10.0`, datastore `1.1.7`, core-ktx `1.17.0`, OkHttp `4.12.0`.
- **Actionable recommendation:** The AAR-metadata error is genuinely good; it names the dependency and the required AGP. It would be even better if it stated the recommended *last compatible* version of each offending library, since "upgrade AGP and compileSdk" is not always the right answer — as here, where it would have meant adopting AGP 9's new plugin model.

## F6 — The Compose BOM does not add artifacts

- **Date:** 2026-09-16
- **Tool:** Gradle / Compose BOM
- **Task:** Add `androidx.compose.material3:material3` usage
- **Expected:** The BOM supplying versions is enough
- **Actual:** Kotlin compile failed: `Unresolved reference 'material3'`, `Unresolved reference 'Text'`, `Unresolved reference 'MaterialTheme'`
- **Error:** `e: ... MainActivity.kt:12:25 Unresolved reference 'material3'.`
- **Root cause:** A platform BOM constrains versions but adds no dependencies. `material3` had to be declared explicitly (version supplied by the BOM). Straightforward once seen — but the "Unresolved reference" message points at Kotlin code rather than at the missing dependency, which sends you hunting in the wrong place.
- **Impact:** ~5 minutes.
- **Workaround:** `implementation("androidx.compose.material3:material3")` alongside the BOM.
- **Actionable recommendation:** Documentation could state plainly, next to the BOM example, that each artifact must still be declared. (The BOM page does mention this, but the failure mode is easy to hit.)

## F7 — tv-material focus-colour API mismatch

- **Date:** 2026-09-16
- **Tool:** `androidx.tv:tv-material` 1.1.0
- **Task:** Give the nav rail container a surface colour
- **Expected:** `Surface(colors = ClickableSurfaceDefaults.colors(container = ...))`
- **Actual:** Type mismatch — `ClickableSurfaceColors` vs expected `SurfaceColors`
- **Error:** `Argument type mismatch: actual type is 'ClickableSurfaceColors', but 'SurfaceColors' was expected.` / `No parameter with name 'container' found.`
- **Root cause:** The non-clickable `Surface` overload expects `SurfaceDefaults.colors(containerColor = ...)`, not the clickable defaults (different factory function *and* different parameter name).
- **Impact:** ~5 minutes.
- **Workaround:** `SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)`.
- **Actionable recommendation:** The two near-identical `colors()` factories with different parameter names (`container` vs `containerColor`) is a small API inconsistency that costs every newcomer a compile cycle.

## F8 — Application subclass not registered → crash at launch

- **Date:** 2026-09-16
- **Tool:** Android runtime / AndroidManifest
- **Task:** Launch the assembled debug APK on the TV emulator
- **Expected:** Home screen renders
- **Actual:** App process started, then died immediately; the TV launcher stayed in the foreground
- **Error:** `android.app.Application cannot be cast to com.firemind.app.FireMindApp` (FATAL EXCEPTION: main)
- **Root cause:** I created `FireMindApp` and cast `application as FireMindApp` in `MainActivity`, but never declared `android:name=".FireMindApp"` on `<application>`. The default `android.app.Application` was instantiated instead.
- **Impact:** ~10 minutes; only discoverable by actually running the app — it builds and installs cleanly.
- **Workaround:** Add the manifest attribute; the crash then disappears and the app keeps window focus with zero FATAL lines in logcat.
- **Actionable recommendation:** A lint check for "an `Application` subclass exists in the module but is not referenced by the manifest" would catch this entire class of runtime-only crash before install. (This is exactly the argument for testing the *installed* build rather than trusting a green compile.)

## F9 — `node --test test/` fails on Windows

- **Date:** 2026-09-16
- **Tool:** Node.js 24 built-in test runner
- **Task:** `npm test` → `node --test test/`
- **Expected:** Discover and run both test files
- **Actual:** Crash before any test ran
- **Error:** `Error: Cannot find module 'C:\...\backend\test'` (`MODULE_NOT_FOUND`), surfaced through the CJS loader
- **Root cause:** The trailing-slash directory form is not resolved as a test-discovery root on Windows in this Node version; the path is treated as a module specifier.
- **Impact:** ~5 minutes.
- **Workaround:** `node --test "test/**/*.test.js"` (glob form) — portable and now the `npm test` script.
- **Actionable recommendation:** Accept directory arguments with a trailing separator on all platforms, or emit a clearer message than `MODULE_NOT_FOUND` when `--test` is handed a directory it cannot expand.

## F10 — `Start-Process` does not inherit the shell's exported environment

- **Date:** 2026-09-16
- **Tool:** PowerShell `Start-Process` (used to detach background processes)
- **Task:** Start the backend detached with `PORT=8080`
- **Expected:** Server listens on port 8080
- **Actual:** Server logged `listening on http://localhost:0` and became unreachable; health checks returned empty responses
- **Error:** None — the process started successfully, just on the wrong port (Node treats `PORT=0` as "any free port", so nothing errored)
- **Root cause:** The `PORT` variable was exported in Git Bash; `Start-Process` does not inherit the invoking shell's environment the way a direct child process would.
- **Impact:** ~8 minutes across two failed attempts (the second only diagnosed after capturing stdout to a log file).
- **Workaround:** Set the variable inside the PowerShell invocation (`$env:PORT='8080'; Start-Process ...`), and always redirect stdout/stderr to a log when detaching.
- **Actionable recommendation:** A "detached process writes no logs" habit is the real lesson; redirecting output immediately is what produced the `listening on ...:0` line that solved it.

## F11 — Kotlin `mapOf` with mixed numeric literals breaks arithmetic

- **Date:** 2026-09-16
- **Tool:** Kotlin 2.4.20
- **Task:** Map spelled-out numbers to values, including a fractional one: `mapOf("one" to 1, ..., "half" to 0.5)`
- **Expected:** `Map<String, Double>` (or at least numeric values usable in `n * 60`)
- **Actual:** Compile error on `n * 60`
- **Error:** `None of the following candidates is applicable: fun BigDecimal.times(other: BigDecimal)` — a confusing candidate list that hints at a *string* problem, not a numeric one
- **Root cause:** Mixed `Int`/`Double` literals infer the common supertype `Number & Comparable<*>`, which has no arithmetic operators. Annotating the declared type as `Map<String, Double>` was not sufficient to coerce the literals; writing them as `1.0`, `2.0`, … `0.5` fixed it.
- **Impact:** ~5 minutes; three failed builds while chasing the misleading error.
- **Workaround:** Declare `val wordNumbers: Map<String, Double>` *and* use `Double` literals throughout.
- **Actionable recommendation:** The overload-resolution error could state the inferred receiver type ("receiver inferred as `Number & Comparable<*>`, which has no `times`") — that single sentence would have made the cause obvious.

## F12 — "under two hours" was silently ignored (found by running the app)

- **Date:** 2026-09-16
- **Tool:** FireMind app + backend (own code) — found during emulator verification
- **Task:** Demo query: "I want a mind-bending sci-fi movie under two hours"
- **Expected:** All results ≤ 120 minutes
- **Actual:** A 131-minute title appeared in the results
- **Error:** No error — the runtime constraint was simply never applied
- **Root cause:** The runtime parser matched digits only (`(\d+)\s*(hour|hr)`), so the spelled-out "two" produced no cap at all. The first version of this bug existed in both the app and the backend.
- **Impact:** A correctness bug that a compile, unit-test-free pass, or code read would likely have missed; caught only by reading the actual rendered results on the TV.
- **Workaround:** Word-number mapping (`one`…`ten`, plus `a`/`an`/`half`) in both implementations, with a regression test asserting every result respects the cap.
- **Actionable recommendation:** This is the strongest argument in this log for running the product on the target platform before claiming a feature works. It also shows why the same logic living in two languages (Kotlin + JS) needs mirrored tests.

## F13 — D-pad focus escaped into the nav rail

- **Date:** 2026-09-16
- **Tool:** Jetpack Compose for TV (focus system)
- **Task:** Navigate from the Assistant keyboard up to the preset-prompt row
- **Expected:** UP moves focus to the chips directly above
- **Actual:** Focus jumped left into the nav rail, skipping the chips entirely; on several screens the first focus after navigation landed on the rail rather than the content
- **Error:** None — pure geometry/behaviour defect, only visible by driving the remote
- **Root cause:** No initial `FocusRequester` per destination, and no focus-group containment, so one-dimensional focus search considered the rail (which vertically overlaps the content rows) the nearest candidate for "up".
- **Impact:** A core interaction was effectively broken for remote users; invisible in code review and in a compile.
- **Workaround:** Every destination claims initial focus on its primary action; `focusGroup()` on the chip and keyboard rows; `focusProperties { up = FocusRequester.Cancel }` on the topmost chips row to block escape. Verified by stepping the D-pad and dumping the focused node's bounds after each press.
- **Actionable recommendation:** TV focus deserves first-class test tooling — asserting "UP from element X reaches element Y" as a unit test would have caught this in seconds instead of a manual key-by-key session.

## F14 — Detached emulator/servers died with the shell

- **Date:** 2026-09-16
- **Tool:** Git Bash job control, Android emulator
- **Task:** Keep the emulator and backend running across tool invocations
- **Expected:** A `&`-backgrounded process keeps running after the command returns
- **Actual:** The emulator vanished from `adb devices` and the backend with it; a 60s foreground emulator run looked like a hang
- **Error:** No error output; only an empty `adb devices` list
- **Root cause:** Background jobs are tied to the invoking shell/process tree, which is torn down when the command finishes or times out.
- **Impact:** ~15 minutes of confusion, including one unexplained emulator death.
- **Workaround:** Launch long-lived processes with PowerShell `Start-Process` and redirect their output to log files, then poll for readiness (`sys.boot_completed`, `adb wait-for-device`, `/api/health`) in separate invocations.
- **Actionable recommendation:** For automation-facing docs, recommend explicit detachment plus log redirection plus readiness polling as the standard pattern for emulators and local servers.

## F15 — `uiautomator dump` node ordering is not reliable

- **Date:** 2026-09-16
- **Tool:** `adb exec-out uiautomator dump`
- **Task:** Assert which screen is showing and which element holds focus
- **Expected:** Stable, hierarchical ordering of nodes
- **Actual:** Nav-rail labels sometimes appeared out of their visual order in the dump, making text-order assertions misleading
- **Error:** None; the data was present, just ordered differently than the layout
- **Root cause:** The accessibility tree order does not strictly mirror visual layout order for Compose semantics.
- **Impact:** A few minutes of misreading screen state during verification.
- **Workaround:** Assert on the *focused node's bounds* plus the presence of distinctive text, rather than on the order of text nodes.
- **Actionable recommendation:** Worth knowing for anyone writing adb-driven UI checks: verify state via focused bounds + unique content, not node sequence.

---

## F16 — R8 floods the release build output with Kotlin metadata warnings

- **Date:** 2026-09-16
- **Tool:** R8 (via AGP 8.13.2) with Kotlin 2.4.20
- **Task:** `./gradlew assembleRelease` (minified, resource-shrunk build)
- **Expected:** A clean release build log
- **Actual:** The build **succeeded** and produced a working 1.4 MB APK, but stderr contained dozens of repetitions of: `WARNING: R8: An error occurred when parsing kotlin metadata. This normally happens when using a newer version of kotlin than the kotlin version released when this version of R8 was created.`
- **Error:** Warning only — the build result was correct and the minified APK ran on the emulator (catalog deserialization verified)
- **Root cause:** The bundled R8 in AGP 8.13.2 predates Kotlin 2.4.20's metadata version, so R8 cannot parse the metadata it reads. It affects only metadata-dependent optimizations (not code shrinking itself) in this project, since the serialization keep rules are explicit.
- **Impact:** Low functionally, but high noise: the repeated block drowned out the actual `BUILD SUCCESSFUL` line, so the build result had to be re-checked with the warnings filtered out. A judge encountering this could easily read it as a failure.
- **Workaround:** Filter the noise (`grep -v "WARNING: R8: An error occurred"`) when reading build output; keep explicit `-keep` rules for kotlinx-serialization rather than relying on R8's metadata analysis.
- **Actionable recommendation:** R8 should emit this as a single summarized warning (with a count and a "suppressed N more" note) rather than one block per affected class, and it should state explicitly that the build is still valid. Documenting the Kotlin↔R8 compatibility table directly in the warning (the URL is included, which is good) would also help.

---

## F17 — A chip that promised Sci-Fi delivered no Sci-Fi (found by writing tests)

- **Date:** 2026-09-16
- **Tool:** FireMind app + backend (own code) — found while adding unit tests
- **Task:** Write app-side tests for the deterministic engine, matching the backend suite
- **Expected:** The catalog's mood tags and the Home screen's mood chips describe the same thing
- **Actual:** Writing an invariant test ("every chip resolves to a real catalog mood or genre") failed: the catalog has **8 moods** (Serious, Cozy, Funny, Whimsical, Mind-bending, Heartfelt, Exciting, Tense) and **9 genres**, but two of the seven chips — **"Family" and "Sci-Fi" — are genres, not moods**, and the engine had **no genre concept at all**. Tapping "Sci-Fi" returned the top-rated titles regardless of genre. Separately, the mood-synonym tables in the app and backend had drifted: the app was missing `sci-fi`, `space`, `clever`, `scary`, `animated` and others, so the same sentence produced different results depending on whether the AI backend or the on-device engine answered.
- **Error:** No runtime error at all. The engine simply matched a nonexistent mood tag (yielding zero mood matches) and fell through to "sort by rating", which looks plausible on screen — a silently wrong answer, not a crash.
- **Root cause:** The mood chips were written as UI labels rather than as references to real catalog tags, and the recommendation engine ranked only on `moods`, never on `genres`. Nothing tested the chip → data contract, so the gap was invisible.
- **Impact:** A core feature was quietly degraded: a viewer asking for sci-fi got a drama. The demo query "mind-bending sci-fi under two hours" returned one non-sci-fi, non-mind-bending title (a Drama/Mystery).
- **Workaround / fix:** Added first-class genre intent to both engines (`parseGenre` + genre synonyms), restricted mood matching to tags that actually exist in the catalog, removed the dead `kids`/`children`/`animated` → mood mappings in favour of the audience filter and the Family genre, used genre as a ranking tiebreaker after mood, named the detected genre in the reason string, and made the tables identical in both languages. The demo query now returns three titles that each match mood, genre and runtime.
- **Actionable recommendation:** Derive UI filter labels from the data instead of hand-writing them, and add a "filter option resolves to real data" invariant test — it is a two-line test that caught a user-visible recommendation defect that compile-checking, manual clicking, and even the original backend suite had all missed.
---

## F18 — The SigV4 signature was well-formed, deterministic… and wrong

- **Date:** 2026-09-16
- **Tool:** FireMind's own signer (`backend/lib/bedrock.js`) cross-checked against botocore 1.43.95 (the library behind the AWS CLI)
- **Task:** Prove the hand-rolled SigV4 implementation is *correct*, not merely self-consistent. The existing tests asserted the Authorization header's shape, determinism, and that the payload hash covered the body — all of which passed.
- **Expected:** For an identical host, path, body, timestamp, region and credentials, our signature equals the one AWS's own signer produces.
- **Actual:** The signatures **differed**. Everything visible matched — same signed-header set (`content-type;host;x-amz-content-sha256;x-amz-date`), same credential scope, same payload hash — yet the signature was wrong. The cause: SigV4 signs a **twice-URI-encoded** canonical path for every service except S3, while the request on the wire carries the single-encoded path. We signed the wire path. Encoding the path a second time (`%3A` → `%253A`) reproduced botocore's signature byte-for-byte.
- **Error:** None locally — every test passed. Against the live service this would have been `403 SignatureDoesNotMatch` on **every** Bedrock call, with no local symptom whatsoever.
- **Root cause:** The canonical URI rule is easy to miss precisely because both paths are *valid URLs* and the signature is a valid-looking 64-hex string either way. Nothing about the output signals which one was signed.
- **Impact:** Potentially severe. The entire AI path would have failed the first time it met real AWS, and because failures degrade silently to the deterministic fallback by design, it would have presented as "AI appears to be off" rather than as a bug — the demo would have looked fine while the flagship feature never once worked. This is exactly what "implemented but never executed" conceals.
- **Workaround / fix:** Added `canonicalUri()` (encode the already-encoded path a second time) and used it for the canonical request while leaving the wire path untouched. Pinned it with a known-answer test asserting botocore's exact signature, plus a second test asserting that signing the *wire* path does **not** reproduce it, so a regression cannot pass quietly.
- **Actionable recommendation:** Cross-check any hand-rolled SigV4 against an official SDK signer with the clock pinned. AWS's public conformance test suite is not sufficient here: it exercises the shared algorithm with service-agnostic canonicalization, and the defect above lives in a service-specific detail. Also worth knowing: botocore omits `x-amz-content-sha256` for non-S3 services, so aligning the header set explicitly is required before the two signatures are comparable at all.

---

## F19 — The "Android TV" emulator images ship with Google Play services

- **Date:** 2026-09-16
- **Tool:** Android SDK TV system images (`system-images;android-28;android-tv;x86`, `android-30`)
- **Task:** Prove FireMind runs on Fire OS, which ships **no** Google Play services
- **Expected:** An AOSP-style TV image without Google services, as the handheld `aosp_atd` images provide
- **Actual:** Every TV image available is a Google variant — the installed system reports `sdk_google_atv_x86`, and `pm list packages` confirms `com.google.android.gms` is present. So launching there proves nothing about Fire OS's GMS-free environment. Disabling `com.google.android.gms` and `com.android.vending` to simulate it then produced a `FATAL EXCEPTION` on the next launch — from the TV **launcher**, not the app.
- **Error:** `FATAL EXCEPTION: AsyncDvrDbTask-0` / `Process: com.android.tv, PID: 4473` — while FireMind itself logged `Displayed com.firemind.app.debug/…MainActivity: +2s507ms` with zero exceptions.
- **Root cause:** There is no GMS-free TV system image in the SDK repository, and the bundled TV launcher hard-depends on Play services, so removing them kills the launcher. A blunt `grep -c "FATAL EXCEPTION"` cannot tell the two apart.
- **Impact:** Two problems at once: the Fire OS claim cannot be tested on the default TV images, and the decoy crash invites either a false negative ("our app crashed") or a false positive if the count is ignored entirely.
- **Workaround / fix:** Disable Google services, then attribute each crash by **process name** instead of counting exceptions; separately confirm the app's dependency graph is GMS-free (`./gradlew :app:dependencies`) and that `aapt2 dump badging` shows only the expected permissions and a `leanback-launchable-activity`. With services disabled the app rendered all 88 UI nodes and staged no exceptions of its own.
- **Actionable recommendation:** Publish a GMS-free AOSP TV image (or label the `android-tv` tag as Google-inclusive at install time). For app teams, make "does this APK depend on Play services?" a lint or a `badging` flag rather than something each developer rediscovers while chasing a launcher crash.

---

## F20 — Silent empty output: MSYS path mangling and console encoding

- **Date:** 2026-09-16
- **Tool:** `adb` (Git Bash on Windows) + `uiautomator dump` + Python
- **Task:** Read the device's UI hierarchy over adb to automate screen verification
- **Expected:** `adb shell cat /sdcard/window_dump.xml` returns the XML
- **Actual:** It returned **nothing, silently**. MSYS had rewritten the argument `/sdcard/window_dump.xml` into `C:/Program Files/Git/sdcard/window_dump.xml`, so adb was asked to read a nonexistent local path. Once that was fixed, the next run failed with `UnicodeEncodeError: 'charmap' codec can't encode character '\u2605'` — the ★ in the rating text, unrepresentable in the Windows console's cp1252 — which looked like a dump failure but was a *printing* failure.
- **Error:** Empty stdout with exit code 0, then `UnicodeEncodeError: 'charmap' codec can't encode character '\u2605' in position 94`
- **Root cause:** Two independent Windows-only traps: MSYS auto-converts Unix-looking paths in arguments, and Python's stdout defaults to the console codepage rather than UTF-8.
- **Impact:** Both failures are indistinguishable from "the app rendered nothing" / "the hierarchy is empty", which is the worst possible symptom during UI verification — it points at the app rather than at the tooling, and cost several round-trips.
- **Workaround / fix:** `export MSYS_NO_PATHCONV=1`, prefer `adb pull` over `adb shell cat`, and set `PYTHONIOENCODING=utf-8` (or ASCII-escape output) when printing device text.
- **Actionable recommendation:** Path conversion should be opt-in rather than automatic, or MSYS should refuse to rewrite an argument whose target is obviously a device path; and CLIs that print device-sourced Unicode should default to UTF-8 instead of the local codepage.

---


## Summary

Twenty real obstacles, of five kinds:

1. **Tooling/argument-handling traps** (F1, F2, F3, F9, F10): all silent or misleading failures — wrong paths, exit code `0` after installing nothing, servers bound to the wrong port.
2. **Version/compatibility walls** (F5, F6, F7, F11): legitimate metadata-driven pinning work, with error messages that pointed at Kotlin/Java symptoms rather than the dependency or typing cause.
3. **Defects only a real run could reveal** (F8, F12, F13, F14): an install-time crash, a silently ignored user constraint, a broken D-pad path, and background processes dying with the shell. None of these would have been caught by compiling, and the runtime query bug (F12) would have shipped in a "working" build.
4. **Verification-tooling quirks** (F15, F16): assertion-unfriendly dump ordering and a release-build warning flood. Neither broke anything, but both made "is this actually working?" harder to answer than it should be.
5. **A defect found only by writing tests** (F17): two Home-screen chips referenced genres that the engine did not understand, so "Sci-Fi" returned dramas. Like F12 and F13 it was invisible to the compiler — but unlike those, it surfaced from an invariant test rather than from manual use.
6. **Invalidating an independent implementation** (F18, F19, F20): the SigV4 signature that looked perfect but would have 403'd against real AWS, the Google-inclusive "Android TV" images that cannot prove Fire OS behavior, and two Windows traps whose symptom — empty output — imitates an app failure.

The F18 finding is the one worth dwelling on. Every test involved passed, the build was clean, the code read correctly, and the output was a plausible 64-character signature. Nothing short of comparing it against AWS's own signer could distinguish right from wrong, which is precisely why "implemented but never executed" deserves to be stated as a limitation rather than treated as done.

Every entry above was fixed and re-verified before moving on. Current state: 39/39 backend tests and 32/32 app tests passing, the Bedrock path executed end-to-end against a Converse stub with the signature pinned to AWS's own output, a full D-pad journey confirmed on the emulator, and the app installed and driven on Fire OS 7 (API 28) and Fire OS 8 (API 30) equivalents — including with Google services disabled.
