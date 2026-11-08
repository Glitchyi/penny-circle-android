# Spending Buddy — native source

A private two-person spending accountability app written in Kotlin and Jetpack Compose. This is not a web app, React Native project or Expo bundle.

The objective, completed scope, and actionable pending work are maintained in [`TODO.md`](TODO.md). The working preferences for future agents are in [`AGENTS.md`](AGENTS.md), and the durable implementation direction is in [`plan.md`](plan.md).

## Build

Use JDK 17 or 21 **with javac**, Android SDK platform 36 and Build Tools 36.0.0. The Gradle wrapper pins Gradle 8.13; AGP 8.13.0 and Kotlin/Compose 2.2.10 are pinned in the root build file. Set ANDROID_HOME or create your own local.properties with sdk.dir.

```sh
./gradlew :app:assembleRelease :app:testDebugUnitTest :app:lintRelease
```

This produces an unsigned release APK at app/build/outputs/apk/release/app-release-unsigned.apk. Restore the separately provided signing backup outside this source directory, then run:

```sh
export ANDROID_HOME=/path/to/android-sdk
export KEYSTORE=/private/location/spending-buddy-release.jks
export PASSWORD_FILE=/private/location/password.txt
./build-and-sign.sh
```

Never commit the keystore or password. To ship an update, increment versionCode and versionName in app/build.gradle.kts and use the original release key. Replacing the signing key prevents ordinary updates to existing installations.

## Architecture

Models.kt defines INR/paise, expense records, import candidates, check-ins and streak rules. BuddyRepository.kt handles atomic local persistence and private import batches. The `ui/` package contains the native Today, Activity, Buddy, Settings, limits, onboarding, pairing, and import-review flows. RotaryDial.kt implements the optional rotary gesture and haptics. ScreenshotImporter.kt uses bundled ML Kit on-device text recognition and a conservative review-only transaction parser. The peer-sync package and Android workers handle direct iroh communication, reachability, and approximate notifications. MainActivity.kt receives image shares, launches Android's photo picker, and handles invite/deep-link entry.

Temporary screenshot copies and raw OCR text are not included in synchronization payloads. All saved expense entries require review or explicit manual entry. Local app backup is disabled; uninstalling/clearing storage destroys un-synchronized data.

## Peer-to-peer architecture
The v0.2 build is local-first and uses iroh QUIC for direct peer-to-peer synchronization between the two phones. Pairing uses a short-lived `sb1.` invite containing an iroh ticket and a random pairing secret. Expense/check-in snapshots and approved events are exchanged directly; raw screenshots and OCR drafts stay on-device.

The Android app keeps a persistent iroh identity in Android Keystore-backed storage, runs a short foreground reachability window, and uses constrained background work for periodic reconciliation. Android battery policies can delay background sync and notifications; the app does not promise instant delivery or Android Live Updates. Unlinking clears the local peer relationship.

No hosted backend or server credentials are required for the v0.2 peer flow.

## Tests and limits

Unit tests cover exact INR conversion, refunds/transfers, grace deadlines, streak behavior, rotary angle math and screenshot parsing safeguards. The delivered build evidence reports actual test/lint/signature results and any emulator/device limitations. Screenshots from the users' actual Google Pay layouts are still needed for layout-specific OCR tuning. The app is an early beta and not independently security audited.
