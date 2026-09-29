# FireMind

**An AI-powered viewing companion for Amazon Fire TV.**

Tell Fire TV what you are in the mood for in one sentence — get three to four
recommendations, each with a written reason you can accept or reject.

<p align="center">
  <img src="docs/screenshots/ui-tour/11-prime-hero.png" alt="FireMind home screen with featured hero billboard" width="760">
</p>

FireMind is a TV-first Android application built for Fire OS 7 and Fire OS 8
(Fire TV Stick and Fire TV). Instead of endless scrolling, viewers ask in
natural language — *“I want a mind-bending sci-fi movie under two hours”* —
and receive recommendations that reference the request directly. Titles can be
inspected, saved to a persistent watchlist, and explored by similar mood.

Built for the **Build, Ship, Shape: Amazon Developer Hackathon** (Fire TV track).

---

## Features

| Capability | Description |
|---|---|
| **Ask in one sentence** | One D-pad press from home to a full QWERTY keyboard, with ready-prompt chips for one-click requests |
| **Explainable picks** | Every recommendation carries a reason referencing the request — mood, runtime, audience |
| **Honest AI labeling** | Results always show their source: `AI · Amazon Bedrock` or `Curated picks`. The app never disguises fallback output as AI |
| **Persistent watchlist** | Survives app restarts; verified on device with direct DataStore evidence |
| **Discover** | Mood tiles, a featured hero billboard, and poster rails with generated key art |
| **Cloud deployment** | The same backend code runs on AWS Lambda behind an HTTP API, deployed by one script |

## Screens

**Home — two-column layout with featured hero and mood tiles**

![Home](docs/screenshots/ui-tour/00-home-now.png)

**Results — every pick carries its reason**

![Results](docs/screenshots/ui-tour/12-results-stitch.png)

**Ask screen — QWERTY keyboard with a dedicated ENTER key, focus clamped so the
D-pad cannot escape mid-typing**

![Keyboard](docs/screenshots/ui-tour/10-keyboard-enter.png)

**Voice insight panel — the emulator has no microphone, so the panel states its
truth: “VOICE · TYPE FOR NOW”. On Fire TV hardware, Alexa owns the voice channel.**

![Voice panel](docs/screenshots/ui-tour/08-previous-query.png)

**Discover — browse by mood**

![Discover](docs/screenshots/ui-tour/07-discover.png)

## Demo video

A 2:45 end-to-end journey: ask, results with reasons, details, watchlist, app
restart, watchlist persistence. Captured from the Fire OS 7 emulator and
encoded to H.264 for universal playback.

- File: [`docs/demo-video.mp4`](docs/demo-video.mp4) (5.5 MB)
- The debug APK installable on any Fire TV device is available on the
  [Releases page](https://github.com/hamzahmed5/FireMindAmazon/releases/latest).

## Architecture

```
+---------------------------+       HTTP        +--------------------------------+
|  Fire TV app              | ----------------> |  Backend (Node.js, zero deps)  |
|  Kotlin + Compose for TV  |                   |  - SigV4-signed Bedrock calls  |
|  Ask / Results / Details  | <---------------- |  - 60-title curated catalog    |
|  Watchlist (persistent)   |       JSON        |  - honest source labeling      |
+---------------------------+                   +---------------+----------------+
                                                                | same code
                                                                v
                                                +--------------------------------+
                                                |  AWS Lambda + API Gateway      |
                                                |  role: bedrock:InvokeModel     |
                                                +--------------------------------+
```

- **App** — Kotlin, Jetpack Compose for TV, D-pad-first navigation, DataStore persistence
- **Backend** — Node.js with **zero npm dependencies**; AWS Signature V4 is
  implemented directly on `node:crypto` and speaks Bedrock's Converse API
- **Cloud** — one Lambda function, one IAM role with exactly one permission
  (`bedrock:InvokeModel`), deployed by [`tools/deploy-aws.mjs`](tools/deploy-aws.mjs)
- **AI** — Amazon Bedrock (Claude Haiku 4.5 through the `us.` inference profile),
  with a deterministic curated recommender as a labeled fallback

## Getting started

**Backend** (no packages to install):

```bash
cd backend
cp .env.example .env      # add AWS credentials with Bedrock access
npm start                 # serves http://localhost:8080 (health at /api/health)
```

**App**:

```bash
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Fire TV hardware**: download the APK from the
[Releases page](https://github.com/hamzahmed5/FireMindAmazon/releases/latest)
and sideload it with any standard tool (for example the Downloader app);
step-by-step instructions are included in the release notes.

## Verification

- **Backend test suite** — 62/62 passing (`cd backend && npm test`)
- **App unit tests** — 32/32 passing (`./gradlew :app:testDebugUnitTest`)
- **Fire OS journeys** — a scripted 20-check D-pad verification suite
  ([`tools/verify-fireos.sh`](tools/verify-fireos.sh)) executed on Fire OS 7
  (API 28) and Fire OS 8 (API 30) emulators: **pass on both**, including
  kill-and-restart watchlist persistence
- **CI** — a GitHub Actions workflow (`.github/workflows/ci.yml`) runs both
  test suites on every push

## Repository layout

```
app/                  Fire TV application (Kotlin + Compose TV)
backend/              Zero-dependency Node backend (server.js, lib/, test/)
docs/
  demo-video.mp4      2:45 demo journey (H.264)
  screenshots/        UI captures for every screen
  DEPLOY_AWS.md       AWS deployment walkthrough (also automated by script)
  DEMO_SCRIPT.md      Second-by-second narration of the demo video
  FRICTION_LOG.md     31 documented issues and what each taught us
  API_SPEC.md         Backend API contract
tools/
  deploy-aws.mjs      One-command AWS deployment (Lambda, API, IAM role)
  verify-fireos.sh    20-check D-pad verification suite
```

## Design honesty

The results screen always labels its source. During development, the Bedrock
daily token quota on the project's AWS account was zero (an account without a
payment instrument receives no allowance), and the original default model
reached end of life mid-project. The application kept reporting **“Curated
picks”** truthfully and remained fully usable throughout. When Bedrock access
is available, the same screen reads **“AI · Amazon Bedrock”** with no code
changes. The complete investigation is documented in
[docs/FRICTION_LOG.md](docs/FRICTION_LOG.md).

## License

MIT — see [LICENSE](LICENSE).
