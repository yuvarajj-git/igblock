#!/usr/bin/env bash
# Usage: ./setup.sh path/to/app.apk
# Installs IgCap, grants usage access and makes it Device Owner. Phone must be connected with USB debugging on.
set -euo pipefail
APK="${1:?usage: ./setup.sh app.apk}"
PKG=com.example.igcap
ADMIN="$PKG/.AdminReceiver"

command -v adb >/dev/null || { echo "adb not found. Install Android platform-tools."; exit 1; }
adb get-state >/dev/null 2>&1 || { echo "No phone found. Plug it in and accept the USB debugging prompt."; exit 1; }

echo "== Install"
adb install -r "$APK"

echo "== Usage access"
adb shell appops set "$PKG" GET_USAGE_STATS allow

echo "== Device owner"
if ! adb shell dpm set-device-owner "$ADMIN"; then
  cat <<'EOF'

set-device-owner failed. On Redmi/HyperOS the usual causes are:
  1. Accounts on the phone. Remove ALL of them temporarily:
     Settings > Accounts & sync (Google, Mi Account, etc). You can add them back afterwards.
  2. Settings > Additional settings > Developer options: turn on BOTH
     "USB debugging" and "USB debugging (Security settings)".
  3. Settings > Passwords & security > Second space / Dual apps: remove any extra user.
Then run this script again.
EOF
  exit 1
fi

echo "== Start"
adb shell am start -n "$PKG/.MainActivity" >/dev/null
echo "Done. IgCap is now device owner. Open the IgCap app to see status."
