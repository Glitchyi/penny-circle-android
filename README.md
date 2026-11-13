# Penny Circle

A private spending notebook for a small circle of friends, built for Android with Kotlin and Jetpack Compose. Log purchases, explicitly record no-spend days, finish the day, and encourage each other without shame. One circle holds up to 12 members.

## Install

Penny Circle 0.3.1 targets Android 13 or later. The production APK supports ARM64 phones; debug builds also support x86-64 emulators. Get distribution artifacts from [GitHub Releases](https://github.com/Glitchyi/penny-circle-android/releases/latest). The signed [0.3.1 release](https://github.com/Glitchyi/penny-circle-android/releases/tag/v0.3.1) is public with APK, SHA-256 and public metadata assets. Final artifact checks are recorded in [BUILD-EVIDENCE.md](BUILD-EVIDENCE.md).

Install the APK using Android's normal installation flow. Existing development/debug-certificate installations require a user-approved fresh install once to move to the production certificate. Uninstalling removes local app data. Subsequent production updates must reuse the same production signing key. Keep Android security protections enabled.

The app name is Penny Circle; its package remains `com.spendingbuddy.app`.

## Design fixes in 0.3.1

The owner authorized all eight audit batches where possible. These changes are included in signed 0.3.1/code 4, using the existing production certificate. The prior 0.3.0 assets remain unchanged. Logo, radius, shadow and figure-scale choices remain open; F-Droid is deferred.

Implemented changes preserve exact paise and zero-capable limits; fill adjacent panes during swipes; retain editable purchase, import and settings drafts on recoverable errors; separate notification destinations; clarify Circle progress, cancellation and confirmations; and improve shared controls, theme roles, accessibility labels and Android string catalogs. Protected local-data recovery requires an explicit successful restore or confirmed fresh start before writes resume.

The consolidated suite passed 264 tests with one optional online skip. Release lint has zero errors, 69 warnings and two hints, and debug assembly and signed ARM64 release verification passed. Final emulator regression and correction results, artifact hashes and remaining device limitations are recorded in [all-batch verification](design-audit/evidence/all-batches/VERIFICATION.md). These scoped checks do not replace a fresh whole-app design audit or physical-device TalkBack, haptic, OEM and frame-timing testing.

## Everyday use

- **Today:** bold spending totals, daily and Monday–Sunday weekly limits, week rings, impulse habit totals, and swipe to finish. Tap the finish control for an accessible review alternative. Manual keypad entry is always available; the rotary dial is optional.
- **Optional automatic tracking:** incoming payment SMS and selected payment-app notifications are parsed locally. Clear shop payments become Auto purchases; uncertain or person payments ask first. Refunds, credits, failures and OTPs are excluded. No old SMS inbox scan occurs, and message text is never stored or shared.
- **Two separate timers:** the private payment budget notification can last 1, 2 or 5 minutes (default 2). An automatic purchase retains its ten-minute Undo and circle-sharing hold even after that notification disappears. Tap Keep to share sooner. Until release, circle members retain the prior finished-day status and streak.
- **Screenshots:** share one or more payment screenshots or use the photo picker. On-device OCR creates editable candidates; Add more/Continue supports batches. Review and save explicitly. This is screenshot intake, not a Google Pay API integration.
- **Reminders:** optionally enable the timer in Settings → Reminders and choose a 15, 30 or 60 minute lead (default 30) before the fixed 10 PM IST reminder. The default window is 9:30–10 PM; the timer stops when the day is finished. The next-day 10 AM IST grace deadline is separate; the timer does not run through that grace window.
- **Appearance and navigation:** four themes, Geist typography, a native glass-like tab bar, resistant body-pane swipes and gesture haptics. The surface uses native Compose drawing rather than backdrop blur. Reduced motion removes gesture travel and uses a still finish acknowledgement.

Daily and weekly limits are independent and may be zero. Android permissions and source access remain optional and user-controlled. WorkManager timing, background delivery and Android Live Update promotion depend on the system; none is guaranteed.

## A private circle

Create a circle, share its `pc1.` invitation, and approve each join request. Codes and pending requests expire after 24 hours. A code alone does not admit someone. Each member chooses **Totals & status** (default, no purchase records) or **Full details** (also reviewed purchases). Budget suggestions take effect only after their owner accepts them.

Phones synchronize directly over per-member iroh QUIC connections. No hosted application backend stores a copy. Running iroh-gossip transport is not implemented; do not assume mesh-wide forwarding or instant delivery. Members need to be reachable for updates and approvals.

Screenshots and raw OCR remain on-device and are excluded from peer payloads. Android app backup/device transfer is disabled. Google ML Kit processes OCR on-device, but its SDK may send diagnostic and usage metrics to Google. Read [PRIVACY.md](PRIVACY.md) and [third-party notices](docs/third-party-notices.md), also available in Settings → Privacy / Licenses. Those documents link the relevant primary terms and SDK disclosures.

## Build and sign

Use JDK 21 **with javac**, Android SDK platform 36 and Build Tools 36.0.0. Configure `JAVA_HOME` and `ANDROID_HOME`, or put `sdk.dir` in an untracked `local.properties`. The Gradle wrapper and plugin versions are pinned in the repository.

```powershell
.\gradlew.bat :core:test :app:testDebugUnitTest :app:lintRelease :app:assembleDebug
.\build-and-sign.ps1
```

The Windows release script runs core/app unit tests and release lint before assembly, requires an ARM64-only nondebuggable APK, aligns its ZIP entries for 16 KB pages, signs with the external production key, verifies the signature/alignment, and emits `release/Penny-Circle-<version>-arm64.apk`, its `.sha256`, and public metadata. It also verifies every ARM64 ELF load segment is aligned for at least 16 KB.

Defaults are `%USERPROFILE%\private-signing\penny-circle-release.p12`, its adjacent `password.txt`, and alias `pennycircle`. Override with `-KeyStore`, `-PasswordFile`, `-KeyAlias`, `-AndroidSdk` or `-JavaHome`. Optional `-PublicSmsUciPath` supplies the external aggregate evaluation corpus. Passwords are read by apksigner from the password file; the script neither generates a key nor falls back to debug signing.

The new production key was explicitly authorized by the owner and is stored outside Git. Reuse it for future updates. Never add signing files, passwords or personal screenshots to source control. Private-key patterns are excluded in `.gitignore`; release APKs and captures are also ignored. The older Bash signing helper requires its own explicit external key/alias configuration; the Windows script is the documented 0.3.0 distribution path.

## Evidence and limitations

See [BUILD-EVIDENCE.md](BUILD-EVIDENCE.md) for dated test/build results, [TODO.md](TODO.md) for remaining work, and [HANDOFF-AUTODETECT.md](HANDOFF-AUTODETECT.md) for implementation continuity. Historical test totals are not a claim about the current tree.

Synthetic two-emulator testing exercised real create/invite/join/approval, bidirectional totals/check-ins, full-detail sharing, and payment-hold privacy followed by Keep. API 36.1 emulator evidence observed native promoted progress notifications. Payment duration and reminder lead choices passed UI persistence checks, including a one-minute native timeout while the ten-minute hold remained intact. Original circular ₹ artwork and native progress colors follow the selected theme, with semantic red for over-budget payments; theme changes refresh cards without extending expiry. API 36.1 native cards followed Mint to Signal Blue while retaining their original expiry. Real payment-app delivery, physical-device feature behavior, TalkBack, FPS, OEM battery restrictions and Doze scheduling remain unverified. Public parser corpora are useful safeguards, not live-bank certification. The app is an early beta and has not had an independent security audit.
