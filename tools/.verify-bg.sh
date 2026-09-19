#!/usr/bin/env bash
# Launched detached by PowerShell; writes its own log so no shell quoting
# has to survive the hop.
cd "$(dirname "$0")/.."
export ANDROID_SDK_ROOT="C:/firemind-tools/android-sdk"
export ADB="C:/firemind-tools/android-sdk/platform-tools/adb.exe"
export EMULATOR="C:/firemind-tools/android-sdk/emulator/emulator.exe"
export PATH="/c/firemind-tools/android-sdk/platform-tools:/c/firemind-tools/android-sdk/emulator:$PATH"
AVD="${1:-firetv7}"
LOG="${2:-.freebuff/verify-stitch.log}"
bash tools/verify-fireos.sh --avd "$AVD" > "$LOG" 2>&1
echo "EXIT=$?" >> "$LOG"
