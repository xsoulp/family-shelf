#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
if [[ ! -f "$SDK/platforms/android-36/android.jar" || ! -d "$SDK/build-tools/36.0.0" ]]; then
    echo "Android SDK 36 and Build Tools 36.0.0 are required."
    echo 'Install with: sdkmanager "platforms;android-36" "build-tools;36.0.0"'
    echo 'Set ANDROID_HOME if the SDK is outside ~/Android/Sdk.'
    exit 1
fi
printf 'sdk.dir=%s\n' "$SDK" > local.properties
./gradlew --no-daemon --console=plain assembleDebug lintDebug "$@"
mkdir -p artifacts
output="artifacts/living-room-tv-debug.apk"
for arg in "$@"; do
    if [[ "$arg" == "-PhomeEnabled=true" ]]; then output="artifacts/living-room-tv-home-debug.apk"; fi
done
cp app/build/outputs/apk/debug/app-debug.apk "$output"
sha256sum "$output"
echo "APK: $PWD/$output"
