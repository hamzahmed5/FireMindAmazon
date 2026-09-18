#!/usr/bin/env bash
#
# Fire OS-level verification, repeatable.
#
#   tools/verify-fireos.sh                  # static audit + every AVD below
#   tools/verify-fireos.sh --avd firetv7    # one AVD
#   tools/verify-fireos.sh --static-only    # no emulator, APK/manifest only
#   KEEP_RUNNING=1 tools/verify-fireos.sh   # leave the emulator up to inspect
#
# Fire OS 7 is Android 9 (API 28) and Fire OS 8 is Android 11 (API 30), so the
# AVDs `firetv7` / `firetv8` are the closest thing to Fire TV that can be run
# on a machine without Amazon hardware. This script boots one, installs the
# debug APK, drives the real journey with D-pad key events, and fails if the
# app crashes or a screen is not reached.
#
# What this PROVES: the app installs, launches, renders, navigates and exits
#   cleanly on both Fire OS API levels, with no Google Play services involved.
# What it CANNOT prove: Amazon's own Fire OS build, the Fire TV launcher, or
#   physical remote keycodes. Those need a Fire TV Stick and a screen.
#
set -u

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

# --- config -----------------------------------------------------------------
AVDS_DEFAULT="firetv7 firetv8"
PORT_BASE=5580
APK="app/build/outputs/apk/debug/app-debug.apk"
PKG="com.firemind.app.debug"
ACTIVITY="$PKG/com.firemind.app.MainActivity"
OUT_DIR="app/build/fireos-verify"
STATIC_ONLY=0
AVDS=""

while [ $# -gt 0 ]; do
  case "$1" in
    --avd) AVDS="${AVDS:-} ${2:-}"; shift 2 ;;
    --static-only) STATIC_ONLY=1; shift ;;
    -h|--help) sed -n '2,20p' "$0"; exit 0 ;;
    *) echo "unknown arg: $1" >&2; exit 2 ;;
  esac
done
[ -n "$AVDS" ] || AVDS="$AVDS_DEFAULT"

# MSYS/Git Bash rewrites /sdcard-style arguments; disable that.
export MSYS_NO_PATHCONV=1
export PYTHONIOENCODING=utf-8

ADB="${ADB:-adb}"
EMULATOR="${EMULATOR:-emulator}"
command -v "$ADB" >/dev/null || { echo "adb not on PATH (set ADB=...)" >&2; exit 2; }

mkdir -p "$OUT_DIR"
FAILED=0
RESULT_LINES=""

note() { printf '  %s\n' "$*"; }
fail() { FAILED=1; RESULT_LINES="$RESULT_LINES\n  FAIL  $*"; printf '  FAIL  %s\n' "$*"; }
pass() { RESULT_LINES="$RESULT_LINES\n  pass  $*"; printf '  pass  %s\n' "$*"; }

# --- static audit: what the APK claims, without running it -------------------
static_audit() {
  echo "== static APK audit =="

  if [ ! -f "$APK" ]; then
    fail "APK missing at $APK (run ./gradlew assembleDebug first)"
    return
  fi
  pass "debug APK present ($(du -h "$APK" | cut -f1))"

  local aapt2
  aapt2="$(ls -d "${ANDROID_SDK_ROOT:-$ANDROID_HOME}"/build-tools/*/aapt2.exe \
                 "${ANDROID_SDK_ROOT:-$ANDROID_HOME}"/build-tools/*/aapt2 2>/dev/null | tail -1)"
  if [ -n "${aapt2:-}" ]; then
    # `tr -d '\r'` because aapt2 on Windows emits CRLF and it breaks the
    # line-anchored parsing below.
    local badging
    badging="$("$aapt2" dump badging "$APK" 2>/dev/null | tr -d '\r')"

    grep -q "leanback-launchable-activity" <<<"$badging" &&
      pass "declares a leanback (TV) launcher activity" ||
      fail "no leanback launcher activity in the APK"

    grep -q "banner" <<<"$badging" &&
      pass "declares a TV banner" || fail "no TV banner declared"

    grep -q "not-required: name='android.hardware.touchscreen'" <<<"$badging" &&
      pass "touchscreen not required (installable on a TV)" ||
      fail "touchscreen is required — blocks TV installs"

    local perms
    perms="$(grep -c "uses-permission" <<<"$badging")"
    grep -q "uses-permission: name='android.permission.INTERNET'" <<<"$badging" &&
      pass "requests INTERNET ($perms permissions total)" ||
      fail "INTERNET permission missing"

    # Parsed without single-quote gymnastics so this stays readable.
    local minsdk
    minsdk="$(grep -o "minSdkVersion:'[0-9]*'" <<<"$badging" | grep -o '[0-9]*' | head -1)"
    if [ -z "${minsdk:-}" ]; then
      fail "could not read minSdkVersion from the APK"
    elif [ "$minsdk" -le 28 ]; then
      pass "minSdk $minsdk ≤ 28, so it installs on Fire OS 7 (API 28)"
    else
      fail "minSdk $minsdk is above Fire OS 7 (API 28)"
    fi
  else
    note "aapt2 not found — skipping manifest audit (set ANDROID_SDK_ROOT)"
  fi

  # Play services must not sneak in: Fire OS ships none of it.
  local gms
  gms="$(./gradlew -q :app:dependencies --configuration debugRuntimeClasspath 2>/dev/null \
         | grep -ciE "play-services|com\.google\.android\.gms" || true)"
  [ "$gms" = "0" ] && pass "zero Google Play services in the runtime classpath" ||
    fail "$gms Play services entries in the runtime classpath"
}

# --- device helpers ---------------------------------------------------------
wait_boot() { # port
  local serial="emulator-$1"
  "$ADB" -s "$serial" wait-for-device >/dev/null 2>&1
  local i=0
  while [ $i -lt 90 ]; do
    [ "$("$ADB" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && return 0
    sleep 2; i=$((i + 1))
  done
  return 1
}

screen_dump() { # serial, file
  "$ADB" -s "$1" shell uiautomator dump /sdcard/fm-verify.xml >/dev/null 2>&1
  "$ADB" -s "$1" pull /sdcard/fm-verify.xml "$2" >/dev/null 2>&1
}

probe_screen() { # serial, file -> prints a screen name
  python - "$2" <<'PY'
import re, sys
try:
    xml = open(sys.argv[1], encoding="utf-8", errors="replace").read()
except OSError:
    print("NO-DUMP"); raise SystemExit
# text + bounds together, because the nav rail shows the label "Watchlist" on
# EVERY screen - matching that text alone would report WATCHLIST everywhere.
try:
    nodes = [(m.group(1), int(m.group(2)))
             for m in re.finditer(r'text="([^"]*)"[^>]*?bounds="\[(\d+),', xml)]
except Exception:
    print("NO-DUMP"); raise SystemExit
texts = [t for t, _ in nodes]
has = lambda s: any(s in t for t in texts)
# The Watchlist screen's own header sits in the content area, right of the rail
# (rail is 112dp wide; its labels live under x=240px even at 1080p/2x).
content_header = any(t.strip() == "Watchlist" and x > 240 for t, x in nodes)
if has("In Watchlist") or has("Add to Watchlist"): print("DETAILS")
elif has("For: ") or has("curated picks"): print("RESULTS")
elif has("About FireMind"): print("ABOUT")
elif has("Quick moods"): print("HOME")
elif has("Browse the catalog"): print("BROWSE")
elif has("Pick a prompt"): print("ASK")
elif has("Nothing saved yet") or content_header: print("WATCHLIST")
else: print("UNKNOWN")
PY
}

# The DataStore file the app itself writes, read on-device: direct proof that a
# watchlist title was persisted, independent of what any screen shows.
saved_ids() { # serial -> comma-separated catalog ids found on disk
  "$ADB" -s "$1" shell run-as "$PKG" cat files/datastore/watchlist.preferences_pb 2>/dev/null \
    | tr -d '\r' | grep -o 'fv[0-9][0-9][0-9]' | sort -u | tr '\n' ','
}

key() { "$ADB" -s "$1" shell input keyevent "$2" >/dev/null 2>&1; sleep 1; }

# A single dump can catch a screen mid-recomposition (a state change rendered a
# moment after the key event), which shows up as a flaky FAIL on a step that in
# fact worked. Poll instead of sampling once.
wait_text() { # serial, text, attempts (default 5, ~2s apart)
  local i=0 attempts="${3:-5}"
  while [ "$i" -lt "$attempts" ]; do
    screen_dump "$1" "$OUT_DIR/dump.xml"
    grep -q "$2" "$OUT_DIR/dump.xml" && return 0
    sleep 2
    i=$((i + 1))
  done
  return 1
}

expect() { # serial, label, expected-screen
  screen_dump "$1" "$OUT_DIR/dump.xml"
  local got
  got="$(probe_screen "$1" "$OUT_DIR/dump.xml")"
  if [ "$got" = "$3" ]; then pass "$2 → $got"
  else fail "$2 → expected $3, got $got"; fi
}

# Nav-rail focus behaviour is not identical across Android versions (see
# F13/F24), and nothing exposes the focused node. So instead of hard-coding
# one sequence, try the plausible ones and record which worked - a guess that
# happens to pass is worth less than a search that reports its sequence.
seek() { # serial, label, target-screen, sequence... (space-separated keyevents)
  local serial="$1" label="$2" target="$3"
  shift 3
  local seq got=""
  for seq in "$@"; do
    key "$serial" 21                       # from content back to the rail
    # shellcheck disable=SC2086
    for k in $seq; do key "$serial" "$k"; done
    key "$serial" 66
    sleep 3
    screen_dump "$serial" "$OUT_DIR/dump.xml"
    got="$(probe_screen "$serial" "$OUT_DIR/dump.xml")"
    if [ "$got" = "$target" ]; then
      pass "$label → $got (keys: $seq)"
      return 0
    fi
    # Escape any pushed detail screen before the next attempt.
    key "$serial" 4
  done
  fail "$label → expected $target, last screen $got"
  return 1
}

# --- one AVD ----------------------------------------------------------------
verify_avd() { # avd-name, index
  local avd="$1" idx="$2"
  local port=$((PORT_BASE + idx * 2))
  local serial="emulator-$port"
  echo
  echo "== $avd (serial $serial) =="

  local api
  api="$("$EMULATOR" -avd "$avd" -no-window -no-audio -no-snapshot -no-boot-anim \
         -port "$port" >/dev/null 2>&1 & echo started)"
  note "booting $avd ..."
  if ! wait_boot "$port"; then
    fail "$avd: emulator did not finish booting"
    "$ADB" -s "$serial" emu kill >/dev/null 2>&1
    return
  fi
  pass "$avd: booted"
  note "model: $("$ADB" -s "$serial" shell getprop ro.product.model 2>/dev/null | tr -d '\r')" \
       "| API $("$ADB" -s "$serial" shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')"

  "$ADB" -s "$serial" install -r "$APK" >/dev/null 2>&1 &&
    pass "$avd: APK installed" || fail "$avd: install failed"

  # Hermetic start: an AVD keeps its user data between runs, so a watchlist
  # file left by an earlier run would let the persistence checks below pass on
  # their own. Clearing app data first makes "saved" mean saved THIS run.
  "$ADB" -s "$serial" shell pm clear "$PKG" >/dev/null 2>&1
  local ids_initial
  ids_initial="$(saved_ids "$serial")"
  [ -z "$ids_initial" ] && pass "$avd: empty watchlist before the journey" ||
    fail "$avd: stale app data survived pm clear ('$ids_initial')"

  "$ADB" -s "$serial" logcat -c >/dev/null 2>&1
  "$ADB" -s "$serial" shell am start -n "$ACTIVITY" >/dev/null 2>&1
  sleep 6
  expect "$serial" "$avd: launch" "HOME"

  # The journey: chip → Results → Details → Watchlist toggle.
  key "$serial" 20; key "$serial" 66; sleep 4
  expect "$serial" "$avd: mood chip" "RESULTS"

  key "$serial" 66; sleep 4
  expect "$serial" "$avd: open details" "DETAILS"

  key "$serial" 66; sleep 3
  wait_text "$serial" 'In Watchlist' 5 &&
    pass "$avd: watchlist toggle" || fail "$avd: watchlist toggle did not change state"

  # Best-effort only: a screenshot of where the journey stopped, for a human to
  # look at if something above failed. Never asserted on.
  "$ADB" -s "$serial" shell screencap -p /sdcard/fm-step.png >/dev/null 2>&1
  "$ADB" -s "$serial" pull /sdcard/fm-step.png "$OUT_DIR/$avd-after-toggle.png" >/dev/null 2>&1

  local ids_before count
  ids_before="$(saved_ids "$serial")"
  # Exactly one: the toggle on one details screen must save one title, and the
  # app started empty, so anything else means a real bug.
  count="$(printf '%s' "$ids_before" | tr -cd ',' | wc -c | tr -d ' ')"
  [ "$count" = "1" ] &&
    pass "$avd: exactly one title saved to disk (${ids_before%,})" ||
    fail "$avd: expected exactly 1 saved title, DataStore holds '$ids_before'"

  # Rail: clamp at the top, then step down. The order is Home, Ask, Browse,
  # Watchlist, About - the clamp is what makes the count deterministic.
  seek "$serial" "$avd: watchlist screen" "WATCHLIST" \
    "19 19 19 19 19 19 19 19 19 19 20 20 20" \
    "19 19 19 19 19 19 19 19 19 19 20 20 20 20" \
    "19 19 19 19 19 19 19 19 19 19 20 20" \
    "19 19 19 19 19 19 19 19 19 19 20" \
    "19 19 19 19 19 19 19 19 19 19"

  # Persistence across a cold start on the same device.
  "$ADB" -s "$serial" shell am force-stop "$PKG" >/dev/null 2>&1
  sleep 2
  "$ADB" -s "$serial" shell am start -n "$ACTIVITY" >/dev/null 2>&1
  sleep 7

  local ids_after
  ids_after="$(saved_ids "$serial")"
  [ -n "$ids_after" ] && [ "$ids_after" = "$ids_before" ] &&
    pass "$avd: same ids still on disk after restart (${ids_after%,})" ||
    fail "$avd: disk ids changed across restart ('$ids_before' -> '$ids_after')"

  seek "$serial" "$avd: watchlist survives restart" "WATCHLIST" \
    "19 19 19 19 19 19 19 19 19 19 20 20 20" \
    "19 19 19 19 19 19 19 19 19 19 20 20 20 20" \
    "19 19 19 19 19 19 19 19 19 19 20 20" \
    "19 19 19 19 19 19 19 19 19 19 20" \
    "19 19 19 19 19 19 19 19 19 19"

  # Crashes attributable to this app.
  local crashes
  crashes="$("$ADB" -s "$serial" logcat -d -b crash 2>/dev/null | grep -ci "$PKG" || true)"
  [ "${crashes:-0}" = "0" ] && pass "$avd: no crash-buffer entries for this app" ||
    fail "$avd: $crashes crash-buffer entries for this app"

  "$ADB" -s "$serial" shell screencap -p /sdcard/fm-verify.png >/dev/null 2>&1
  "$ADB" -s "$serial" pull /sdcard/fm-verify.png "$OUT_DIR/$avd.png" >/dev/null 2>&1 &&
    note "screenshot: $OUT_DIR/$avd.png"

  if [ "${KEEP_RUNNING:-0}" = "1" ]; then
    note "KEEP_RUNNING=1 — leaving $serial up"
  else
    "$ADB" -s "$serial" emu kill >/dev/null 2>&1
    note "emulator stopped"
  fi
}

static_audit
if [ "$STATIC_ONLY" = "0" ]; then
  idx=0
  for avd in $AVDS; do
    verify_avd "$avd" "$idx"
    idx=$((idx + 1))
  done
fi

echo
echo "================ summary ================"
printf "%b\n" "$RESULT_LINES" | sed '/^$/d'
echo "========================================="
if [ "$FAILED" = "0" ]; then
  echo "Fire OS-level verification: PASS"
else
  echo "Fire OS-level verification: FAIL"
fi
exit "$FAILED"
