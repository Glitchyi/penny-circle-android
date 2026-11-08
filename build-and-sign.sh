#!/usr/bin/env bash
# Builds, aligns (16 KB pages for Android 15+ devices), signs and verifies the release APK.
# Requires the private signing backup restored to ../private-signing/ (never generate a new key for an update).
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK}"
KEYSTORE="${KEYSTORE:-../private-signing/spending-buddy-release.jks}"
PASSWORD_FILE="${PASSWORD_FILE:-../private-signing/password.txt}"
ALIAS="${KEY_ALIAS:-spendingbuddy}"
BUILD_TOOLS="${BUILD_TOOLS:-36.0.0}"
if [[ ! -f "$KEYSTORE" || ! -f "$PASSWORD_FILE" ]]; then
  printf 'Restore the private signing backup first. Do not generate a different key for an update.\n' >&2
  exit 1
fi
VERSION=$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts | head -1)
# :core:test includes real two-endpoint iroh tests on the JVM (offline, direct addresses).
./gradlew :core:test :app:testDebugUnitTest :app:lintRelease :app:assembleRelease --console=plain
mkdir -p release
"$ANDROID_HOME/build-tools/$BUILD_TOOLS/zipalign" -P 16 -f 4 app/build/outputs/apk/release/app-release-unsigned.apk release/spending-buddy-aligned.apk
IFS= read -r BUDDY_SIGN_PASSWORD < "$PASSWORD_FILE"
export BUDDY_SIGN_PASSWORD
"$ANDROID_HOME/build-tools/$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-key-alias "$ALIAS" --ks-pass env:BUDDY_SIGN_PASSWORD --key-pass env:BUDDY_SIGN_PASSWORD --v4-signing-enabled false --out "release/SpendingBuddy-$VERSION.apk" release/spending-buddy-aligned.apk
unset BUDDY_SIGN_PASSWORD
rm -f release/spending-buddy-aligned.apk
"$ANDROID_HOME/build-tools/$BUILD_TOOLS/apksigner" verify --verbose --print-certs "release/SpendingBuddy-$VERSION.apk"
"$ANDROID_HOME/build-tools/$BUILD_TOOLS/zipalign" -c -P 16 -v 4 "release/SpendingBuddy-$VERSION.apk" | tail -1
sha256sum "release/SpendingBuddy-$VERSION.apk" > "release/SpendingBuddy-$VERSION.apk.sha256"
printf '\nSigned APK: %s/release/SpendingBuddy-%s.apk\n' "$PWD" "$VERSION"
