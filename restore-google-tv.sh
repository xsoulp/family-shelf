#!/usr/bin/env bash
# Restore the stock Home app on the Xiaomi box; leave Family Shelf installed.
set -euo pipefail
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
ADB="$SDK/platform-tools/adb"
TARGET="${1:-${ANDROID_TV_TARGET:-}}"
[[ -n "$TARGET" ]] || { echo "Usage: bash restore-google-tv.sh TV_HOST:PORT (or set ANDROID_TV_TARGET)"; exit 1; }
"$ADB" connect "$TARGET"
"$ADB" -s "$TARGET" shell pm enable --user 0 com.google.android.apps.tv.launcherx
"$ADB" -s "$TARGET" shell pm enable --user 0 com.google.android.tungsten.setupwraith
"$ADB" -s "$TARGET" shell cmd role add-role-holder --user 0 android.app.role.HOME com.google.android.apps.tv.launcherx 0
"$ADB" -s "$TARGET" shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity
"$ADB" -s "$TARGET" shell input keyevent 3
"$ADB" -s "$TARGET" shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
