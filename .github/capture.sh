#!/usr/bin/env bash
# Installs the debug app on the running emulator, then records the screenshot and a short demo.
set -x
OUT=out
APK=submissions/horace/app/composeApp/build/outputs/apk/debug/composeApp-debug.apk
PKG=ke.droidcon.hakka

adb wait-for-device
adb shell settings put secure show_ime_with_hard_keyboard 0
adb install -r "$APK"
adb shell cmd uimode night no


type_digits() {
  local digits=$1
  for ((i = 0; i < ${#digits}; i++)); do
    adb shell input text "${digits:i:1}"
    sleep 0.7
  done
}

tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null
  adb pull /sdcard/ui.xml "$OUT/ui.xml" >/dev/null
  local bounds
  bounds=$(grep -o "text=\"$1\"[^>]*bounds=\"[^\"]*\"" "$OUT/ui.xml" | head -1 | grep -o 'bounds="[^"]*"' | grep -o '[0-9]\+')
  if [ -z "$bounds" ]; then
    # Compose may put the text on a child node; fall back to the clickable parent's bounds.
    bounds=$(python3 - "$OUT/ui.xml" "$1" <<'PY'
import re, sys
xml = open(sys.argv[1]).read()
m = re.search(r'text="%s".*?bounds="(\[\d+,\d+\]\[\d+,\d+\])"' % re.escape(sys.argv[2]), xml)
print(" ".join(re.findall(r"\d+", m.group(1))) if m else "")
PY
)
  fi
  read -r x1 y1 x2 y2 <<<"$(echo $bounds)"
  adb shell input tap $(((x1 + x2) / 2)) $(((y1 + y2) / 2))
}

# Light mode, partially filled: two filled, one focused, three empty.
adb shell am start -W -n "$PKG/.MainActivity"
sleep 4
type_digits 21
sleep 1
adb exec-out screencap -p > "$OUT/screenshot.png"
adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml "$OUT/ui-partial.xml"

# Dark mode, for checking only.
adb shell cmd uimode night yes
sleep 3
adb exec-out screencap -p > "$OUT/screenshot-dark.png"
adb shell cmd uimode night no
sleep 3

# Demo: fresh launch, wrong code, paste-free retype, correct code, wait for resend.
adb shell am force-stop "$PKG"
adb shell screenrecord --bit-rate 2000000 --time-limit 60 /sdcard/demo.mp4 &
REC=$!
sleep 1
adb shell am start -W -n "$PKG/.MainActivity"
sleep 3
type_digits 654321
sleep 1
tap_text Submit
sleep 2
adb exec-out screencap -p > "$OUT/after-wrong.png"
type_digits 123456
sleep 1
tap_text Submit
sleep 2
adb exec-out screencap -p > "$OUT/after-right.png"
sleep 25
adb exec-out screencap -p > "$OUT/after-countdown.png"
sleep 2
adb shell pkill -INT screenrecord
wait $REC
sleep 2
adb pull /sdcard/demo.mp4 "$OUT/demo.mp4"
ls -la "$OUT"
