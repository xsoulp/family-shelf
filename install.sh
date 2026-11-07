#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
ADB="$SDK/platform-tools/adb"
TARGET="${1:-${ANDROID_TV_TARGET:-}}"
[[ -n "$TARGET" ]] || { echo "Usage: bash install.sh TV_HOST:PORT (or set ANDROID_TV_TARGET)"; exit 1; }
APK="app/build/outputs/apk/debug/app-debug.apk"
[[ -x "$ADB" ]] || { echo "ADB not found: $ADB"; exit 1; }
[[ -f "$APK" ]] || { echo "Build first: bash build.sh"; exit 1; }
"$ADB" connect "$TARGET"
"$ADB" -s "$TARGET" get-state
current_home="$("$ADB" -s "$TARGET" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME)"
disabled_stock="$("$ADB" -s "$TARGET" shell pm list packages -d com.google.android.apps.tv.launcherx)"
if [[ "$current_home" == *com.xsoulp.livingroom* || "$disabled_stock" == *package:com.google.android.apps.tv.launcherx* ]]; then
    AAPT="$SDK/build-tools/36.0.0/aapt"
    [[ -x "$AAPT" ]] || { echo "Cannot verify Home compatibility: aapt is missing."; exit 1; }
    home_enabled="$("$AAPT" dump xmltree "$APK" AndroidManifest.xml | awk '/E: activity-alias/ {alias=1} alias && /android:enabled/ {print; exit}')"
    if [[ "$home_enabled" != *0xffffffff* ]]; then
        echo 'Family Shelf is in use as Home. Build its Home-enabled update first:'
        echo 'bash build.sh -PhomeEnabled=true'
        echo 'Or run bash restore-google-tv.sh before installing the prototype-only APK.'
        exit 1
    fi
fi
"$ADB" -s "$TARGET" install -r "$APK"
"$ADB" -s "$TARGET" shell am start -n com.xsoulp.livingroom/.MainActivity
