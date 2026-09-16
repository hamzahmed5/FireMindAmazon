# FireMind Demo Script

Target length: **90–150 seconds** (hard limit: under 3 minutes).
Every action below is behavior already verified on the Android TV emulator;
nothing in this script requires a feature that does not exist.

Recording is a human step — no video file is produced by the build.

## Pre-flight checklist

Before recording:

1. Start the backend: `cd backend && npm start` (port 8080).
   - Without AWS credentials it runs in fallback mode and the app labels
     results **curated picks**.
   - With credentials configured, results are labelled **AI** and reasons
     are model-generated. Choose one and be honest about it on camera —
     the badge on screen matches reality either way.
2. Start the TV environment:
   ```bash
   emulator -avd firetv_demo
   ```
   (or `adb connect <FIRE_TV_IP>:5555` for real hardware)
3. Install the current build:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. Clear the watchlist if you want a clean start (uninstall/reinstall, or
   remove titles from the Watchlist screen).
5. Confirm the About screen reads **"backend online"** — that single line
   proves device→backend connectivity.
6. Record at 1080p, landscape, with the emulator window visible so judges
   can see this is genuinely running on a TV environment.

Total screen-recording budget: ~120s. Keep talking brief and let the UI
carry the story.

## Shot list

| # | Time | On screen | Narration |
|---|---|---|---|
| 1 | 0:00–0:12 | TV launcher → FireMind launches. Home screen with **What do you want to watch?** and focus sitting on **Ask FireMind** | "FireMind is an AI viewing companion built for Fire TV. Instead of scrolling through endless content, you just tell it what you want." |
| 2 | 0:12–0:22 | Press centre on **Ask FireMind** → Assistant screen. Show the preset prompts and the on-screen keyboard without using them | "Remote typing is slow, so FireMind leads with ready-made requests — and still gives you a full on-screen keyboard for custom asks." |
| 3 | 0:22–0:35 | Select the prompt **"I want a mind-bending sci-fi movie under two hours"**. Loading state appears briefly, then results | "One press. FireMind understands mind-bending sci-fi, and respects the two-hour limit." |
| 4 | 0:35–0:50 | Results list: 3–4 titles, each with runtime, genres, rating and an explicit **Why FireMind recommends it** line. Point out every runtime is ≤ 120 min | "It returns a small set of picks, and every one explains *why* it matches — see the runtimes: nothing over two hours slipped through." |
| 5 | 0:50–1:02 | Scroll the badge: **curated picks** (or **AI**, if Bedrock credentials are configured) | "These come from the backend's ranking. With Amazon Bedrock configured, the same results arrive as AI-generated reasons — the badge is honest about which path answered." |
| 6 | 1:02–1:20 | Select a title → **Details**: title, year, genres, runtime, rating, description; then press **Add to Watchlist**; watch the button change to **✓ In Watchlist (remove)** | "Every title opens a details view with a spoiler-safe description, and one press saves it to the watchlist." |
| 7 | 1:20–1:32 | Select **Recommend something similar** → a rail of similar titles appears | "And if you like something, FireMind recommends more like it." |
| 8 | 1:32–1:48 | Press **Back** twice to return. Then force-stop and relaunch the app (or power-cycle the emulator), navigate to **Watchlist**, and show the saved title still there | "The watchlist is stored on the device — watch it survive a full restart." |
| 9 | 1:48–2:00 | Open **About**: show the backend URL and **Status: backend online, AI disabled (fallback mode)** (or "AI enabled") | "The app tells you exactly where it stands and degrades gracefully: if the AI is unavailable, deterministic picks keep FireMind useful instead of broken." |
| 10 | 2:00–2:15 | Optional: cut Wi-Fi / stop the backend, ask again, and show results still appear (badged **curated picks**) | "Even fully offline, FireMind still works — the same recommendation logic runs on the device." |
| 11 | 2:15–2:30 | Return to Home, focus on **Ask FireMind** | "FireMind turns Fire TV from a content browser into an intelligent viewing companion. Thanks for watching." |

Trim shot 10 first if you need to save time; shots 1–9 are the core story.

## Recording tips

- Keep the focus ring visible at all times — the D-pad focus border is the
  clearest proof the app is remote-driven.
- Pause ~0.5s after each keypress so viewers can see focus move before the
  screen changes.
- Do not cut on the loading state; a brief spinner that resolves is good
  evidence the UI never freezes.
- No background music you do not have rights to. Ambient room tone or
  silence is fine.
- Burn in no personal information: check the About screen and any network
  addresses shown on camera (use `10.0.2.2` or a lab LAN IP).
- Upload to YouTube or Vimeo as **public**, then paste the URL into the
  Devpost submission and this README.

## Claims to keep accurate on camera

State only what the recorded build does:

- Works on **Android TV 13 (API 33) emulator** — tested there. Real Fire TV
  hardware validation is listed as a known limitation.
- AI runs through a **local backend**; with Bedrock credentials it is
  live, without them it is the deterministic engine. Say which one you are
  showing.
- The catalog is **60 original fictional titles** — no real movie metadata
  or copyrighted artwork is displayed.
