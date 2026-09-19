#!/usr/bin/env bash
# Usage: tools/.boot-avd.sh <avd-name> <port>
# Detached boot so tool-call timeouts can never kill the emulator mid-boot.
AVD="$1"; PORT="$2"
export ANDROID_SDK_ROOT="C:/firemind-tools/android-sdk"
ADB="C:/firemind-tools/android-sdk/platform-tools/adb.exe"
EMU="C:/firemind-tools/android-sdk/emulator/emulator.exe"
LOG=".freebuff/boot-$AVD.log"
: > "$LOG"
"$EMU" -avd "$AVD" -no-window -no-audio -no-snapshot -no-boot-anim -port "$PORT" >> "$LOG" 2>&1 &
EMUPID=$!
echo "emulator pid $EMUPID" >> "$LOG"
for i in $(seq 1 100); do
  boot=$("$ADB" -s "emulator-$PORT" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
  if [ "$boot" = "1" ]; then echo "BOOTED after ~$((i*5))s" >> "$LOG"; exit 0; fi
  sleep 5
done
echo "BOOT TIMEOUT (500s)" >> "$LOG"
exit 1
