# Build evidence — Spending Buddy 0.1.0

Built on 8 November 2026. Native Kotlin/Jetpack Compose application, package `com.spendingbuddy.app`, versionCode 1, minSdk 33 and targetSdk 36. APK size: 89,723,380 bytes (89.7 MB).

## Completed checks

The final corrected source completed `:app:assembleRelease`, `:app:testDebugUnitTest` and `:app:lintRelease` successfully. Native unit tests: 17 tests, 0 failures/errors. The tests cover exact INR/paise, grace deadlines, streaks, refunds/transfers, rotary angle math and conservative transaction parsing. Lint completed with no errors; its exact summary is: 0 errors, 24 warnings, 3 hints

The optional backend passed `node --check server.mjs` and its source-only mocked tests: 4 tests, 0 failures. No live OAuth, database migration, hosted endpoint or remote push test was performed.

The APK was aligned using `zipalign -P 16` before signing. Its final alignment verification succeeded. The 64-bit ARM and x86 native library ELF LOAD segments also reported 0x4000 (16 KB) alignment.

`apksigner verify --verbose --print-certs` succeeded. The APK verifies with APK Signature Scheme v3, one signer, RSA 3072-bit key. The certificate is a self-signed private-app release certificate, not Google Play certification or a privacy audit.

Certificate SHA-256: `08a20d0c4606f5b097c766ad941d4bc7ffad0d306b4f5e900ab1f1bd1015ba31`

APK SHA-256: `24e0e28f662f9988b1f0755494272685225ee3c204625e69ed590f096837ae59`

## Runtime and backend limitations

An Android 16 emulator was prepared without hardware acceleration. It did not finish booting in the allotted smoke-test period. The signed APK installation attempt returned `Error: device is still booting.` This is not an APK install-validation failure, but it means successful install/launch has not been demonstrated in this environment. No runtime screenshot is delivered as proof.

Real Xiaomi/OnePlus haptics, the complete share-menu flow, real Google Pay screenshot layouts and notification scheduling remain unverified on the users' phones. Do not claim the 17 unit tests are a full native UI/device acceptance pass.

Backend deployment permission remained pending. This delivered APK leaves friend sync unconfigured by default. Local features are implemented; cross-phone sharing, budget suggestions and remote nudges need an authenticated deployed backend. Instant FCM push is not configured, and approximate polling is not equivalent to instant push.

Signing does not guarantee removal of sideload, Play Protect, OEM security or Android developer-trust warnings. The user should not disable security protection to install this build.
