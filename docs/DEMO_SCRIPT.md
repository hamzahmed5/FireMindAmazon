# FireMind Demo Script

Target length: **90–150 seconds** (hard limit: under 3 minutes).
Every action below is behavior already verified on the Fire OS emulators
(firetv7 / API 28 and firetv8 / API 30, 20-check journey suite, both PASS);
nothing in this script requires a feature that does not exist.

Recording is a human step — no video file is produced by the build.

## Pre-flight checklist

Before recording:

1. Start the backend: `cd backend && npm start` (port 8080).
   - Without working Bedrock credentials it serves the deterministic engine
     and results are labelled **Curated picks**.
   - With credentials (and quota available), results are labelled
     **AI · Amazon Bedrock** and reasons are model-generated. Choose one and
     be honest on camera — the badge on screen matches reality either way.
2. Start the TV environment with a **visible window**:
   ```bash
   emulator -avd firetv7 -no-audio -no-snapshot -no-boot-anim -port 5580
   ```
3. Install the current build and launch:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n com.firemind.app.debug/com.firemind.app.MainActivity
   ```
4. Clear the watchlist if you want a clean start (uninstall/reinstall, or
   remove titles from the Watchlist screen).
5. Confirm the top bar shows the green **Online** pill — that single pill
   proves device→backend connectivity. (It appears when `/api/health` answers.)
6. Record at 1080p, landscape, with the emulator window visible so judges
   can see this is genuinely running on a TV environment.

Total screen-recording budget: ~120s. Keep talking brief and let the UI
carry the story.

## Navigation (new Stitch UI)

The nav is a **top bar**: FIREMIND logo + Online pill on the left, then
**ASK · DISCOVER · WATCHLIST · SETTINGS** tabs. From anywhere in content,
press **UP** once to reach the bar, then LEFT/RIGHT between tabs, OK to
select. On first launch focus sits on the prompt bar; DOWN moves into
content.

## Shot list

| # | Time | On screen | Narration |
|---|---|---|---|
| 1 | 0:00–0:12 | App launches → Home: cyan **prompt bar** ("What do you want to watch?"), **QUICK MOODS** tiles, green **Online** pill in the top bar | "FireMind is an AI viewing companion built for Fire TV. Instead of scrolling through endless content, you just tell it how you feel." |
| 2 | 0:12–0:22 | Show the mood tiles (Funny, Action, Family, Sci-Fi, Relaxing) — mention the prompt bar takes any free-text ask too | "Remote typing is slow, so FireMind leads with one-press moods — and the big prompt bar still takes any sentence you type." |
| 3 | 0:22–0:35 | Press OK on **FUNNY** → brief loading → Results | "One press. FireMind reads the mood and picks for me." |
| 4 | 0:35–0:50 | Results list: 4 titles, each with runtime, genres, star rating, and an explicit **reason** line under the title | "It returns a small set of picks, and every one explains *why* it matches — no mystery, no endless scroll." |
| 5 | 0:50–1:02 | Show the badge above the list: **Curated picks** (or **AI · Amazon Bedrock** if the quota is live) | "The badge is honest about which brain answered — Amazon's AI when it's available, a built-in engine when it's not. It never dead-ends." |
| 6 | 1:02–1:20 | OK on a title → **Details**: title, year, genres, runtime, star rating, description; then **Add to Watchlist** → button flips to **✓ In Watchlist (remove)** | "Every title opens a details view with a spoiler-safe description, and one press saves it to the watchlist." |
| 7 | 1:20–1:32 | Select **recommend something similar** → a rail of similar titles appears | "And if you like something, FireMind recommends more like it." |
| 8 | 1:32–1:48 | Press **Back** to return. Force-stop and relaunch the app, then UP → **WATCHLIST** tab → the saved title is still there | "The watchlist is stored on the device — watch it survive a full restart." |
| 9 | 1:48–2:00 | UP → **SETTINGS** tab → **About**: status **"backend online, AI enabled"** (or "AI disabled (fallback mode)") | "The app tells you exactly where it stands. If the AI is unavailable, the built-in engine keeps FireMind useful instead of broken." |
| 10 | 2:00–2:15 | Optional: stop the backend, ask again — results still appear, badge says **Curated picks**, top pill flips to **Offline** | "Even fully offline, FireMind still works — the same recommendation logic runs on the device." |
| 11 | 2:15–2:30 | Return Home, focus on the prompt bar | "FireMind turns Fire TV from a content browser into an intelligent viewing companion. Thanks for watching." |

Trim shot 10 first if you need to save time; shots 1–9 are the core story.

## Recording tips

- Keep the focus ring visible at all times — the cyan D-pad focus border is
  the clearest proof the app is remote-driven.
- Pause ~0.5s after each keypress so viewers can see focus move before the
  screen changes.
- Do not cut on the loading state; a brief spinner that resolves is good
  evidence the UI never freezes.
- No background music you do not have rights to. Ambient room tone or
  silence is fine.
- Burn in no personal information: the emulator backend URL is
  `10.0.2.2:8080` (loopback alias) — safe to show. Do not show personal
  AWS account pages.
- Upload to YouTube or Vimeo as **public**, then paste the URL into the
  Devpost submission and this README.

## Claims to keep accurate on camera

State only what the recorded build does:

- Runs on **Fire OS emulator AVDs** (API 28 "Fire OS 7" and API 30
  "Fire OS 8"), verified end-to-end by the 20-check journey suite
  (`tools/verify-fireos.sh`). Real Fire TV hardware validation is listed
  as a known limitation — no device was available.
- The backend runs **locally** in the recorded session and is also
  deployed to **AWS Lambda + API Gateway + Bedrock** (same code, zero
  stored credentials). Say which one you are showing — the About screen
  and Online pill make it visible.
- The AI is **Amazon Bedrock** (Claude) via the model-agnostic Converse
  API. When the account quota blocks it, the app serves the deterministic
  engine and says so on the badge — show whichever state is on screen and
  name it.
- The catalog is **60 original fictional titles** — no real movie metadata
  or copyrighted artwork is displayed.
