# Penny Circle — native source

A private spending accountability app for a small friend circle (up to 12 people) written in Kotlin and Jetpack Compose. This is not a web app, React Native project or Expo bundle.

The objective, completed scope, and actionable pending work are maintained in [`TODO.md`](TODO.md). The working preferences for future agents are in [`AGENTS.md`](AGENTS.md), and the durable implementation direction is in [`plan.md`](plan.md).

## Build

Use JDK 21 **with javac** (required by the iroh 1.2.0 bindings), Android SDK platform 36 and Build Tools 36.0.0. The Gradle wrapper pins Gradle 8.13; AGP 8.13.0 and Kotlin/Compose 2.2.21 are pinned in the root build file. Set ANDROID_HOME or create your own local.properties with sdk.dir.

```sh
./gradlew :core:test :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
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

Models.kt defines INR/paise, expense records, import candidates, check-ins and streak rules. BuddyRepository.kt handles atomic local persistence and private import batches. The `ui/` package contains the Today, Activity, Circle, Settings, limits, setup and import-review screens, with a light-only slate theme and multi-colour accents (bundled Baloo 2 and Mukta fonts in `res/font`). RotaryDial.kt implements the optional rotary gesture and haptics. ScreenshotImporter.kt uses bundled ML Kit on-device text recognition and a conservative review-only transaction parser. The peer-sync package and Android workers handle direct iroh communication, reachability, and approximate notifications. MainActivity.kt receives image shares, launches Android's photo picker, and handles invite/deep-link entry.

Temporary screenshot copies and raw OCR text are not included in synchronization payloads. All saved expense entries require review or explicit manual entry. Local app backup is disabled; uninstalling/clearing storage destroys un-synchronized data.

## Implementation status

The 0.2.0 source below is **written but not yet compiled or device-tested**; see [`BUILD-EVIDENCE.md`](BUILD-EVIDENCE.md). Treat everything as unverified until a first build passes.

The app is named Penny Circle; the package and applicationId remain `com.spendingbuddy.app`.

## Circle and peer-to-peer design

- One circle per phone, at most 12 members. The admin is the creator; admin can be transferred, with an oldest-member fallback. Members can only leave (no removal).
- Joining uses a shareable `pc1.` code valid for 24 hours. A joiner's request is queued for the admin, who must approve it. The code carries no secret.
- Each member chooses visibility: Totals & status (default) or Full details. Nudges are rate-limited. Budget suggestions apply only after the limit owner accepts them.
- Transport is iroh QUIC with direct per-member connections. The Kotlin bindings (`computer.iroh` 1.2.0) do not expose iroh-gossip (checked in the published sources jar and AAR), so gossip is not used. No hosted backend or server credentials exist.
- Persistent iroh identity, a short foreground reachability window and constrained background reconciliation are implemented in code. Battery policies can delay sync and notifications; the app does not promise instant delivery or Android Live Updates. An offline admin cannot approve joins.
- Join-code QR display/scan was dropped; the code is shared as text.

Phase 1 hardening (written): refund import kind, parser fixes, corrupt-state backup with a write block, validated restore, sync message bounds, keypad as default amount entry, and accessibility improvements.

## Tests and limits

Limits: a circle holds at most 12 members; at 12 the admin cannot issue codes or approve requests until someone leaves. Join requests wait up to 24 hours and only the admin can approve them, which needs the admin's app open, Stay reachable on, or the roughly 15 minutes after a code is shown. Delivery is approximate, not instant.

Test sources exist for the domain model, state codec/migration, sync protocol and circle rules, iroh transport (integration), circle runtime, palette contrast, rotary dial math and screenshot parsing. None have been run for this version. Screenshots from the users' actual Google Pay layouts are still needed for OCR tuning. The app is an early beta and not independently security audited.
