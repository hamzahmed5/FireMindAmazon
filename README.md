# FireMind 🔥🧠

> **Stop scrolling. Start asking.** Tell Fire TV what you're in the mood for in one sentence — get 3–4 honest picks, each with a written reason.

<p align="center">
  <img src="docs/screenshots/ui-tour/12-results-stitch.png" alt="FireMind results screen with honest picks and reasons" width="720">
</p>

**FireMind** is a TV-first Android app built for Amazon Fire TV (Fire OS 7 & 8).
Instead of endless scrolling, viewers ask in natural language — *"I want a
mind-bending sci-fi movie under two hours"* — and get 3–4 recommendations,
each with an explicit explanation of why it matches. Titles can be inspected,
saved to a persistent watchlist, and explored by similar mood.

Built for the **Build, Ship, Shape: Amazon Developer Hackathon** (Fire TV track).

---

## ✨ What it does

| | |
|---|---|
| 🎙️ **Ask in one sentence** | One D-pad press from home to a full QWERTY, with ready-prompt chips for one-click asks |
| 🧠 **Honest picks** | Every result shows its source: `AI · Amazon Bedrock` or `Curated picks` — the app never pretends |
| 💬 **Why This?** | Every recommendation carries a reason referencing the request (mood, runtime, audience) |
| 🔖 **Watchlist** | Persists across app restarts — verified on-device |
| 🧭 **Discover** | Browse by mood tiles, featured hero billboard, poster rails |
| ☁️ **Cloud-ready** | The same backend code runs on AWS Lambda + API Gateway |

## 📱 Screens

| Home (two-column Stitch design) | Ask screen with keyboard |
|---|---|
| ![Home](docs/screenshots/ui-tour/11-prime-hero.png) | ![Keyboard](docs/screenshots/ui-tour/10-keyboard-enter.png) |

| Results with reasons | Voice panel (honest by design) |
|---|---|
| ![Results](docs/screenshots/ui-tour/12-results-stitch.png) | ![Voice](docs/screenshots/ui-tour/08-previous-query.png) |

*The emulator has no microphone, so the voice panel says "VOICE · TYPE FOR NOW" —
on real Fire TV hardware, Alexa owns the voice channel.*

## 🎬 Demo video

A 2:45 full journey (ask → picks → details → watchlist → app restart → watchlist survives):

- **Watch:** `docs/demo-video.mp4` (H.264, 5.5 MB — plays in every player)
- Or download directly: [docs/demo-video.mp4](docs/demo-video.mp4)

## 🏗️ Architecture

```
┌─────────────────────────┐      HTTP       ┌──────────────────────────────┐
│  Fire TV app (Kotlin +  │ ──────────────► │  backend (Node, ZERO deps)   │
│  Jetpack Compose for TV)│                 │  ├─ SigV4-signed Bedrock     │
│  ├─ Ask / Results       │ ◄────────────── │ ├─ 60-title catalog          │
│  ├─ Watchlist (persist) │      JSON       │ └─ honest source badge       │
│  └─ Discover / Details  │                 └──────────────┬───────────────┘
└─────────────────────────┘                                │ same code
                                                           ▼
                                            ┌──────────────────────────────┐
                                            │  AWS Lambda + API Gateway    │
                                            │  role: bedrock:InvokeModel   │
                                            └──────────────────────────────┘
```

- **App:** Kotlin, Jetpack Compose for TV, D-pad-first navigation, DataStore persistence
- **Backend:** Node.js with **zero npm dependencies** — AWS SigV4 signing is
  hand-implemented on `node:crypto`, speaking Bedrock's Converse API directly
- **Cloud:** one Lambda, one role with exactly one permission (`bedrock:InvokeModel`),
  deployed by [`tools/deploy-aws.mjs`](tools/deploy-aws.mjs)
- **AI:** Amazon Bedrock (Claude Haiku 4.5 via the `us.` inference profile);
  falls back to a deterministic curated recommender that is labeled honestly in the UI

## 🚀 Run it yourself

**Backend** (any machine, no packages to install):

```bash
cd backend
cp .env.example .env        # add AWS credentials with Bedrock access
npm start                   # → http://localhost:8080  (health: /api/health)
```

**App** (Android Studio, or command line):

```bash
./gradlew :app:assembleDebug     # → app/build/outputs/apk/debug/app-debug.apk
adb install app/build/outputs/apk/debug/app-debug.apk
```

**On a Fire TV stick:** install the APK from the
[Releases page](https://github.com/hamzahmed5/FireMindAmazon/releases/latest)
with any sideloading tool (e.g. the Downloader app) — full instructions are in the release notes.

## ✅ Verification (real devices, real checks)

- **Backend test suite:** 62/62 passing (`cd backend && npm test`)
- **App unit tests:** 32/32 passing (`./gradlew :app:testDebugUnitTest`)
- **Fire OS journeys:** a scripted 20-check D-pad journey (`tools/verify-fireos.sh`)
  on **Fire OS 7 (API 28)** and **Fire OS 8 (API 30)** emulators: **PASS on both**,
  including kill → restart watchlist persistence
- **CI workflow included** (`.github/workflows/ci.yml`) — runs both test suites on every push

## 📂 Repo map

```
app/                 Fire TV app (Kotlin + Compose TV)
backend/             Zero-dependency Node backend (server.js, lib/, test/)
docs/
  ├─ demo-video.mp4  The 2:45 demo journey (H.264)
  ├─ screenshots/    UI captures for every screen (Stitch design)
  ├─ DEPLOY_AWS.md   Step-by-step AWS deployment (also automated)
  ├─ DEMO_SCRIPT.md  What the demo video shows, second by second
  ├─ FRICTION_LOG.md 31 real bugs/walls hit and what they taught us
  └─ API_SPEC.md     Backend API contract
tools/
  ├─ deploy-aws.mjs  One-command AWS deploy (Lambda + API + role)
  └─ verify-fireos.sh The 20-check D-pad verification suite
```

## 🧠 Design honesty

The results screen always labels its source. During development, the Bedrock
daily quota on our AWS account was **zero tokens** (account without a payment
instrument) — the app never hid that: it kept saying **"Curated picks"** and
kept working. When AI is available, the same screen says **"AI · Amazon Bedrock"**.
See [docs/FRICTION_LOG.md](docs/FRICTION_LOG.md) for the full story, including
the retired-model and inference-profile findings.

## 📄 License

MIT — see [LICENSE](LICENSE).

---

<p align="center"><sub>Built with 🔥 for the Amazon Developer Hackathon — Fire TV track</sub></p>
