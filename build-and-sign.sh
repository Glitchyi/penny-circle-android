#!/usr/bin/env bash
# Builds, aligns (16 KB pages for Android 15+ devices), signs and verifies the release APK.
# Requires Penny Circle's authorized production signing backup outside Git; reuse it for every update.
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK}"
KEYSTORE="${KEYSTORE:-../private-signing/penny-circle-release.p12}"
PASSWORD_FILE="${PASSWORD_FILE:-../private-signing/password.txt}"
ALIAS="${KEY_ALIAS:-pennycircle}"
BUILD_TOOLS="${BUILD_TOOLS:-36.0.0}"
if [[ ! -f "$KEYSTORE" || ! -f "$PASSWORD_FILE" ]]; then
  printf 'Restore the private signing backup first. Do not generate a different key for an update.\n' >&2
  exit 1
fi
VERSION=$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts | head -1)
# :core:test includes real two-endpoint iroh tests on the JVM (offline, direct addresses).
./gradlew :core:test :app:testDebugUnitTest :app:lintRelease :app:assembleRelease --console=plain
mkdir -p release
"$ANDROID_HOME/build-tools/$BUILD_TOOLS/zipalign" -P 16 -f 4 app/build/outputs/apk/release/app-release-unsigned.apk release/penny-circle-aligned.apk
"$ANDROID_HOME/build-tools/$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-key-alias "$ALIAS" --ks-pass "file:$PASSWORD_FILE" --v4-signing-enabled false --out "release/Penny-Circle-$VERSION-arm64.apk" release/penny-circle-aligned.apk
rm -f release/penny-circle-aligned.apk
"$ANDROID_HOME/build-tools/$BUILD_TOOLS/apksigner" verify --verbose --print-certs "release/Penny-Circle-$VERSION-arm64.apk"
"$ANDROID_HOME/build-tools/$BUILD_TOOLS/zipalign" -c -P 16 -v 4 "release/Penny-Circle-$VERSION-arm64.apk" | tail -1
sha256sum "release/Penny-Circle-$VERSION-arm64.apk" > "release/Penny-Circle-$VERSION-arm64.apk.sha256"
printf '\nSigned APK: %s/release/Penny-Circle-%s-arm64.apk\n' "$PWD" "$VERSION"
