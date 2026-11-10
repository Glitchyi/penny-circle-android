# Build evidence — Penny Circle 0.2.0 (versionCode 2)

Package/applicationId `com.spendingbuddy.app` (unchanged), minSdk 33, targetSdk 36.

## Status: core tests, app unit tests and release lint pass; not yet signed or run on a device

Run on 10 November 2026 on macOS (Apple Silicon) with Temurin JDK 21.0.13, Gradle 8.13, Android SDK platform 36 and Build Tools 36.0.0, using `--no-daemon`. The source compiled on the first real build. This is a JVM unit-test, lint and compile pass only: the app has not been signed, installed or run on a device, and the two-phone/circle behaviour is untested.

## Commands to run

Requires JDK 21 with javac, Android SDK platform 36, Build Tools 36.0.0, and network access for Gradle 8.13, AGP 8.13.0 and Kotlin 2.2.21.

```sh
./gradlew :core:test :app:testDebugUnitTest :app:lintRelease
./gradlew :app:assembleRelease
export ANDROID_HOME=/path/to/android-sdk
export KEYSTORE=/private/location/spending-buddy-release.jks
export PASSWORD_FILE=/private/location/password.txt
./build-and-sign.sh
```

## Results

| Check | Result |
|---|---|
| `:core:test` | passed: 75 tests, 0 failures, 0 errors, 1 skipped (the opt-in online iroh test) |
| `:app:testDebugUnitTest` | passed: 30 tests, 0 failures, 0 errors |
| `:app:lintRelease` | passed: 0 errors, 34 warnings, 1 hint (mostly dependency-version, `UseKtx`, `ObsoleteSdkInt`, `IconLauncherShape` and `StaticFieldLeak` warnings) |
| `:app:assembleRelease` | passed: unsigned APK `app-release-unsigned.apk`, 102,617,425 bytes (about 102.6 MB; R8 is off) |
| zipalign / 16 KB alignment | not yet run |
| `apksigner verify --print-certs` | not yet run |
| Certificate SHA-256 | not yet recorded |
| APK size and SHA-256 | not yet recorded |
| Install and launch on Xiaomi 17 / OnePlus Nord 6e | not yet run |
| 3+ member circle sync on real phones | not yet run |

Re-run later on 10 November 2026 after adding purchase delete with Undo and the QR join code: the same three checks passed with the same counts (75 core, 30 app, lint 0 errors / 34 warnings), and `:app:assembleDebug` produced a debug-signed APK (121,648,388 bytes, `apksigner verify` ok, SHA-256 `0ea85d97...4f59d6`). A debug APK cannot update a release-signed install.

Test files: core `DomainTest` 6, `StateCodecTest` 15, `SyncProtocolTest` 39, `PresetsTest` 6, `WeekRangesTest` 7, `IrohTransportIntegrationTest` 2 (1 skipped); app `TransactionParserScreenshotImporterTest` 13, `RotaryDialTest` 9, `PennyPaletteContrastTest` 4, `CircleRuntimeTest` 4.

Build environment notes: the Gradle wrapper's own download timed out here, so Gradle 8.13 was fetched with `curl` and its SHA-256 (`20f1b117...aed78`) matched the published value; `networkTimeout` in `gradle/wrapper/gradle-wrapper.properties` was raised to 120 s. JDK 21 was installed from the Adoptium tarball with its checksum verified.

## Known limits

- iroh transport is per-member direct QUIC. The Kotlin bindings (`computer.iroh` 1.2.0) do not expose iroh-gossip, so there is no gossip topic. This was verified by inspecting the published sources jar and AAR, not by a running build.
- Joining, admin approval and sync need peers to be reachable; an offline admin cannot approve joins. Android battery policies may delay background work and notifications. Nothing here has been tested under Doze.
- Real Google Pay screenshot layouts, haptics and the share-menu flow are unverified on the users' phones.

## Signing caveat

A valid signature from the private release key does not remove sideload, Play Protect, OEM security or developer-trust warnings, and is not Google Play certification or a security audit. Do not disable Play Protect or other security controls to install. Keep the keystore and password outside Git and reuse the same key for updates.
