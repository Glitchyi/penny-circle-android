# Build evidence — Penny Circle 0.3.1 (versionCode 4)

## Signed 0.3.1 all-batch design fixes — 14 November 2026

All eight batches were authorized where possible, with logo and scale choices open and a single consolidated validation stage after implementation. The owner subsequently requested release. Signed 0.3.1/code 4 uses the same production certificate; original v0.3.0 assets below remain unchanged. Application source commit: a528e1e0b7fd0b96bb3ebac5c05c044add2542a5.

Consolidated checks: core174passed/one optional online skip, app90passed; total264 passed/one skip, zero failures/errors. Initial release lint found15 numeric Circle format mismatches; corrected placeholders and resumed lint/build tasks passed with zero errors, 69 warnings/two hints. Debug and unsigned ARM64 release assembly passed. Final scoped source/UI corrections passed and are tracked separately in `design-audit/evidence/all-batches/gradle-corrections.log/json` and included in the final artifact check.

Evidence covers exact limits and retained failure drafts, editable seeded OCR review, onboarding/permission controls, measured large-font amounts, protected backup recovery, warm routes and real temporary two-emulator Circle actions. Restoration status and exact final hashes are in [all-batch verification](design-audit/evidence/all-batches/VERIFICATION.md), with machine-readable `artifacts.json`, `unit-results.json`, `contrast.json` and per-device reports alongside. Final signed APK: 77,203,821 bytes, SHA256 `7e02fabd51a36084ca9997658ec4feb2f5ab9d5b22ebc4bf0ab4364f11d25c19`. Production v3 certificate `84e2559af0440e8a514d154904b9a2a203b617a204c8c46968cb3d94782d98fd` matches 0.3.0. Artifact checks include canonical package/name/version/ABI, nondebuggable ARM64 release, 16KB ELF/ZIP alignment, all15 unchanged notices and compiled launch-state fixes.

Scoped lens reviewers checked touched findings; fixed IDs are recorded in `design-audit/fixed.jsonl`. This does not rerun or overwrite the original full audit. Six broad scale-dependent findings remain open in `design-audit/DEFERRED.md`; physical TalkBack/haptics, real OEM/Doze delivery and measured FPS remain unverified. No separate instrumentation suite is configured. Synthetic QA fixtures never contain personal purchases or signing secrets. Corrected native Back/IME/draft/focus checks and real Circle create/join/cancel/rejoin/approve/Retry passed. Original on-device profiles were restored with exact hashes; display settings restored; both emulators closed. Ledger: 126 source-resolved findings, six owner-dependent findings deferred. Raw captures/logs remain local; no full audit or full physical matrix is claimed.

## Historical batch-1 checkpoint — 13–14 November 2026

The counts/hash/captures in this section describe the earlier batch-1 checkpoint only. The all-batch validation above supersedes it for current source.

The owner ratified design-fix batch 1 only and separately requested repair of the blank area during pane swipes. Work is on `codex/design-fix-batch-1`, uncommitted and unpublished. The public signed v0.3.0 APK below is unchanged; logo and scale decisions remain open.

Exact shared budget input preserves paise and explicit zero, rejects invalid/out-of-range values before saving, and checks the optional seven-day calculation. Settings and Circle suggestion forms reuse these helpers; errors can scroll into view and budget preset chips wrap. Display summaries/presets use the shared currency formatter; import summaries reuse the purchase-date formatter while retaining invalid extracted text. The maximum supported purchase amount is measured to fit the actual width/font scale. The details summary reserves room for its existing Change action. Pane gestures compose the adjacent opaque screen during dragging, use a single coordinated settle/commit, restore cancelled gestures, and share per-tab saved state across preview and committed content.

Checks: core163 total,162 passed/one optional online skip; app71 passed; combined233 passed/one skip, zero failures/errors. Release lint: zero errors,50 warnings/two hints. Kotlin compilation and debug assembly passed. Commands used JDK21 and `:core:test :app:testDebugUnitTest :app:lintRelease :app:assembleDebug`, `--max-workers=1`, JVM `-Xmx1536m -Dfile.encoding=UTF-8`, and the ignored local `publicSmsUciPath` corpus for the core suite. App tests/lint/assembly were rerun after the last summary layout correction. No separate Android instrumentation suite is configured.

Synthetic emulator checks: decimal budget save/reopen300.50/2100.01; invalid text leaves30050/210001 paise stored; explicit zero save/reopen; exact7×300.50=2103.50; both errors and all four presets plus Save reachable at360dp/200% text. Light Horizon max99999999.99 fits at360/411dp and100/200% text; delete/re-add, keypad/dial and reduced motion retain the exact value/announcement. Dark max input and grouped details summary were checked at360dp/200%. Onboarding grouped presets and import summaries were exercised with local fixtures; valid import displayed₹1,234.50 and Fri,13Nov2026, unreadable amount/date remained raw, and no import was saved. This is display verification, not a fresh OCR/parser evaluation.

Fresh sampled mid-drag captures show Today→Activity, Activity→Circle and Circle→Activity content filling the exposed area; a short swipe returned to its source. A temporary date-only synthetic fixture allowed selecting Activity's2–8Nov week; leaving for Today and swiping back retained that week. The original app-local data was restored from its on-device snapshot; no clock, permission, physical phone, private backup or signing key was changed. Both emulators' size/density/font/animation settings are restored at completion. Frame-by-frame continuity/FPS, physical TalkBack/haptics and real-device behavior remain unverified. Circle suggestion fractional prefill/zero validation is covered by shared helper tests and source inspection; no live suggestion was sent.

Exact APK hash, source hashes, captures and scoped regression review are in `design-audit/evidence/fix-1/`; the completed audit's original verdicts remain historical. `design-audit/fixed.jsonl` records the eleven batch-1 IDs after verification. The owner subsequently authorized all eight batches where possible; see the current checkpoint above.

**Current checkpoint: 13 November 2026.** This section supersedes the 0.2.0 records below. Early beta: emulator results do not establish TalkBack, frame rate, physical haptic quality, real payment-app delivery, OEM battery behavior or Doze scheduling. The signed ARM64 release is public at [v0.3.0](https://github.com/Glitchyi/penny-circle-android/releases/tag/v0.3.0). Final signing/build checks passed; GitHub asset digest matches the local APK. An independent unauthenticated public download was hashed and matched the local APK exactly.

Package/applicationId remains `com.spendingbuddy.app`; minSdk 33 (Android 13+), targetSdk 36. Production release is ARM64-only for the intended phones. Debug retains ARM64 and x86_64 for emulator testing; the earlier x86_64 JNA 4KB ELF alignment issue must not be described as fixed by ZIP alignment or included in the production ABI claim.

## Automated checkpoint

JDK 21 with javac, Gradle wrapper, SDK 36 and Build Tools 36.0.0. Core/app tests and release lint passed for the timer-choice/circular-₹-artwork development build:

| Check | Result |
| --- | --- |
| Core JVM | 159 total: 158 passed, 1 optional online iroh test skipped; no failures/errors |
| App JVM | 69 passed |
| Combined | 227 passed, 1 optional skip |
| Release lint | 0 errors, 50 warnings, 2 hints; not warning-free |
| Debug SHA-256 | `4F4B405541CB3A0EA40FBA6C969813A99581246B9AD77B00514C3FE66333D6A0` |
| Final signed ARM64 release | 76,458,349 bytes (76.5 MB / 72.9 MiB); nondebuggable; v3 signature verified |
| Release SHA-256 | `9bd88986af8ed04cb4ea690fec78e0625332661eceab4c11fb708a06c9a23e7e` |
| Alignment | Four ARM64 ELF libraries have PT_LOAD alignment ≥16 KB; APK ZIP 16 KB verification passed |

That debug checkpoint includes payment durations 1/2/5 minutes (default2), reminder leads 15/30/60 minutes (default30), and original circular ₹ progress-head artwork. A subsequent theme-following artwork implementation shares exact AppTheme accent/onAccent/danger colors and programmatic circular ₹ geometry between previews and native cards; the intermediate neutral resource was removed. Both payment and reminder bars use the selected theme accent, with semantic red for an over-budget payment. Theme changes refresh active cards without resetting expiry. The theme-aware build passed. API36.1 native QA showed Mint (`0xff00e599`) and Signal Blue (`0xff4d9bff`) coins/bars. Changing the active card from Mint to Signal Blue reduced timeout from 5 minutes to 3m50.447s, preserving its original deadline. The hash above identifies this theme-aware debug checkpoint.

## Circle runtime integration

Same debug 0.3.0/code 3 on emulator-5554/API 36 and emulator-5556/API 36.1. Synthetic Demo and Synthet profiles only. Actual UI flow created SyntheticCircle, generated an invitation, pasted it on the second emulator, sent a request, displayed pending state independently on both devices, and approved it on the admin UI. Independent run-as reads confirmed the same group and two members. No paired state was seeded.

- Demo logged Rs123 Food/Essential and finished by swipe. Synthet received total 12300 paise/check-in/streak1 with default TOTALS_STATUS and zero purchase records.
- Demo changed visibility to FULL_DETAILS through Circle settings; Synthet received one record and total 12300 paise.
- Synthet logged Rs45 Transport/Essential and finished; Demo received 4500 paise/check-in1 with zero records under totals-only visibility.
- A synthetic bank SMS delivered Rs77 Swiggy to Synthet. Local total became Rs122 and the day reopened. During its ten-minute hold Demo still saw Rs45, check-in1, streak1. In-app Keep released Rs122,check-in0,streak0 within several seconds; purchase records remained absent under totals-only visibility.

An initial transient sync-problem message cleared and peers later showed Reachable now. Evidence is in ignored `captures/production-readiness-2026-11-13/circle/REPORT.md`, fresh XML and synthetic captures. These checks cover foreground direct per-member iroh QUIC, not running gossip forwarding, three-or-more-member/offline/OEM delivery, or scheduled hold expiry. No physical device or personal backup was exported. Existing on-device backup files were preserved.

## Native notification integration

API36.1 synthetic SMS tests observed the promoted ProgressStyle flag and visible native progress. The default two-minute payment card expired while the ten-minute expense hold remained active. Undo of an older held purchase refreshed the surviving card's budget without resetting its deadline (46.873 seconds remaining in the recorded check). Real notification UI Undo was exercised.

At 9:31 PM IST the opt-in reminder card was promoted. Its review link opened the correct finish sheet; completing the day or disabling reminders cancelled it. Unpin suppression survived restart. Temporary clock/permission changes were restored, including automatic time/timezone and the original paired synthetic state.

The fixed reminder target is 22:00 IST; selectable lead defaults to 30 minutes. Next-day 10 AM grace and the ten-minute privacy hold are separate. Timer-choice functional QA passed: the one-minute payment duration and 15-minute reminder lead were selected through UI, saved and retained after restart. The card reported PT1M while the separate hold remained 599970 ms. Original state was restored without clock, display or permission changes. This does not establish visual checks for every duration/lead combination. Native theme QA passed: Mint #00e599 to Signal Blue #4d9bff changed the active card colors while its timeout decreased from five minutes to 3m50.447s, preserving the expiry within about 10 ms. The 60-minute lead also persisted through UI; original per-day Unpin suppression was honored. Onboarding previews have been visually inspected. WorkManager posting remains approximate; no guaranteed promotion, exact alarm or Doze claim is made.

## UI and disclosure checks

Final themed countdown visual check also passed at the real 9:33–9:34 PM IST clock with the 60-minute lead. After snapshotting and temporarily clearing only the synthetic profile's existing app-local per-day dismissal marker, the native Signal Blue card showed its ₹ head, matching bar and 25:33 remaining to 10 PM. Dumpsys confirmed the 22:00 IST `when`, countdown chronometer and promoted ongoing flag. Exact prior suppression XML and Light Horizon/payment2/lead30/reminders/countdown-off preferences were restored; no countdown remained, two expenses/circle persisted, and Android clock/display/permissions were unchanged. No Android eligibility or promotion permission was bypassed.

Emulator observations cover all four themes at 360dp/200% font, readable labels in the 96dp large-text bar, reduced-motion usability, and offline Settings privacy/license reading. Body swipes navigate panes; Activity chart-card swipes retain week navigation. Finish cancellation and early release each left zero check-ins; completed swipe produced exactly one. Onboarding live-progress previews were visually inspected. These are targeted observations, not FPS measurements, TalkBack verification or physical-device haptic assessment.

ADB character-event input into Settings/long invitation text retained only the first few characters and temporarily disrupted controls; restart recovered navigation. Authenticated local emulator clipboard plus in-app Paste entered the full invitation and joined. Normal keyboard failure has not been established and needs a dedicated reproduction.

## Signing and publication boundary

The owner explicitly authorized a new production key outside Git and final commit/push/public-repository/GitHub Release publication after validation. The private `.p12` and password remain external; no password is recorded here. Production certificate SHA-256 is `84E2559AF0440E8A514D154904B9A2A203B617A204C8C46968CB3D94782D98FD`. Existing debug-certificate installs need a user-approved fresh install to switch certificates; future production updates must reuse this key.

Temporary powercfg changes were restored and queried: saved scheme `6fecc5ae-f350-48a5-b669-b472cb895ccf`, sleep AC/DC0 seconds and display AC900/DC600 seconds. No physical phone was connected or modified in this release run.

`build-and-sign.ps1` validates tests/lint, nondebuggable ARM64 packaging, native ELF and 16 KB APK ZIP alignment, external signing, signature and public metadata/hash output. The actual signing pipeline exited 0 using the same production key after its password-file handling was corrected. APK inspection confirmed label Penny Circle, minSdk 33, targetSdk 36, nondebuggable, 15 license assets, four ARM64 native libraries and no other ABI. Embedded VCS metadata exactly matches source/tag v0.3.0 commit `46b4ffe5b9cb696f9aaf3a580a5a9a5ead3e4b01`. The repository was renamed to `Glitchyi/penny-circle-android`, made public, and main/tag pushed. GitHub release v0.3.0 is neither draft nor prerelease and contains the APK, SHA-256 file and public metadata. All three assets uploaded; GitHub APK asset digest matches the SHA-256 above. Unauthenticated public-download hashing passed: the 76,458,349-byte download exactly matched the local SHA-256.

---

# Historical evidence — 0.2.0 and earlier

Everything below is a historical record of the stated older checkpoint. Old “current”, key-unavailable, timer-window, test-count and promotion-unverified statements do not override the 0.3.0 section above.
# Build evidence — Penny Circle 0.2.0 (versionCode 2)

Package/applicationId `com.spendingbuddy.app` (unchanged), minSdk 33, targetSdk 36.

## Current UX refresh validation — 13 November 2026

Implemented five-step onboarding with saved input, exact currency validation, four theme choices, explicit payment-source permissions and a final review. Startup now renders actual loading/retry state while local initialization runs off the main thread; network reachability does not gate entry. Purchase details use a scrollable action sheet. Tabs use opaque horizontal slides with a persistent navigation bar; pushed/back panes use directional parallax. Onboarding stages slide without crossfading. Page content remains readable during its short settling motion. Reduced-motion preference changes are observed while the app is running.

The full command shown in the earlier section below passed with the optional public UCI dataset supplied. App unit tests, lint and both builds were rerun after the final UI changes.

| Check | Current result |
|---|---|
| Core JVM | 155 total: 154 passed, 1 optional online iroh test skipped; no failures/errors |
| App JVM | 64 passed; includes navigation direction, theme/category contrast and responsive checks |
| Combined | 218 passed, 1 skipped |
| Release lint | 0 errors, 41 warnings, 1 hint |
| APKs | Debug and unsigned release built; debug v2 signature verified |
| ZIP alignment | `zipalign -c -P 16 4` exited 0 for both APKs |
| Instrumentation | `:app:connectedDebugAndroidTest` completed with emulator serial explicitly selected; the project contains no instrumentation test sources, so this is not device-test coverage |

Current artifacts (supersede the earlier hashes below):

- Debug: **122,542,201 bytes (122.5 MB / 116.9 MiB)**, SHA-256 `769A0D760662781B6BFC1ACFD84FBECBF9CD564DAA86434B823621CAD834D644`.
- Unsigned release: **102,449,457 bytes**, SHA-256 `54DE534E84187BE2E07D4192661FC2AE2B46AA2D6A3FCE6363AC3E4B01D86999`.
- Release signing remains unavailable because the existing private signing backup is absent. No replacement key was generated. Native dependencies are unchanged; the x86_64 JNA 4 KB ELF alignment limitation described below remains.

### UX emulator observations

Subsequent physical installation smoke check: at the user's request, uninstalled the old app from the Xiaomi and installed the current debug artifact above fresh. USB installation initially returned `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`; a normal retry succeeded once the user approved the prompt. Package Manager reports 0.2.0/code 2, and `am start -W` returned Status ok for MainActivity. No security controls were changed. A copy is in `Download/Penny-Circle-0.2.0.apk` on the phone for the user to share. No phone screenshots were captured; this confirms installation/launch only.

Only `emulator-5554` was used for the UX checks below, before that subsequent physical installation. Explicit user permission covered synthetic UI captures, stored in ignored `captures/ux-refresh-2026-11-13/`; the user's attached purchase screenshot was not copied into the repository.

- Completed all five onboarding steps through the manual path without permission prompts. Blank name and invalid currency precision were blocked; the error is visible next to the footer even when theme choices are scrolled. Name survived Back and landscape/portrait recreation.
- Explicit automatic setup opened Android notification-access settings. Returning without access preserved the setup guidance. SMS denial retained the manual alternative; allowing SMS changed the source to Allowed and enabled Continue. Notification permission was requested only after its button was tapped. Automatic preference, exact limits and disabled reminders persisted on completion.
- Settings reported Ready with one source plus notifications, without requiring optional Live Updates. Revoking the only source produced Today's Connect payment alerts notice and a setup action.
- Inspected the purchase sheet in all four themes. Synthetic Edit and impulse changes persisted; Delete/Undo restored the purchase. The amount, merchant, note, date and source are separated from the action rows. System icons now remain readable in dark sheets.
- At 360dp width and 200% font, onboarding header/footer and permission cards remained readable, manual fallback completed setup, purchase details/actions scrolled, and Circle's Create/Join actions were reachable. Large-text purchase rows now stack their amount/reason instead of squeezing words into a narrow column.
- Inspected a deliberately slowed transition frame. It exposed text showing through the previous tab crossfade; removing tab alpha/scale and onboarding fades eliminated that overlap in the final frame. The navigation bar stayed anchored. With animator scale set to zero while the app remained running, tab/settings navigation and purchase-sheet actions remained usable. These observations are not frame-rate or real-phone smoothness measurements.
- Restored the emulator's original state and theme preference, then launched the new APK: 6 original expenses, Mint Night, completed onboarding, and no `ux-demo-*` fixture IDs. SMS, notification and listener permissions are back on; display is 1080×2400 at density 420, font scale 1.0 and animator scale unset/default. Returned to portrait. The clock was not changed in this pass.

Startup failure/retry was implemented but not fault-injected. Actual TalkBack operation, real payment-app notification delivery, OEM behavior, promoted Live Updates and real-phone motion/performance remain unverified. This refresh does not expand the older parser, countdown or peer-device evidence below.

## Earlier automatic-tracking validation — 13 November 2026 (before UX refresh)

Later physical-device smoke check: the debug APK installed successfully on Xiaomi model `24129PN74I`, Android 16 / API 36, arm64, 4 KB pages. The user replaced the USB cable; a normal streamed installation returned `Success`. Package Manager confirms version 0.2.0 / code 2 installed for user 0, and MainActivity is running. An earlier query found no existing package, so no old installation/data was removed. This verifies installation and a running activity only; permissions, payment detection, sync, haptics and Live Update promotion on this phone remain untested. No phone screenshots or spending data were exported.

Windows, Temurin JDK 21, Gradle 8.13, Android SDK/Build Tools 36. Full command passed:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
.\gradlew.bat :core:test :app:testDebugUnitTest :app:lintRelease :app:assembleDebug :app:assembleRelease '-PpublicSmsUciPath=C:/Users/User/spending-buddy-android/.gradle/public-sms-datasets/extracted/SMSSpamCollection' --console=plain -q
```

The optional UCI file is downloaded separately; reproducible instructions, licenses and hashes are in `core/src/test/resources/sms-corpus/README.md`. Default tests need no network and skip this optional evaluation when its path is absent.

| Check | Current result |
|---|---|
| Core | 152 tests: 151 passed, 1 optional online iroh test skipped; 0 failures/errors |
| App JVM tests | 61 passed; includes theme/category contrast, rocket math and responsive checks |
| Combined | 212 passed, 1 skipped |
| Release lint | passed: 0 errors, 42 warnings, 1 hint; not warning-free |
| Debug APK | built, debug signature verified (v2), installed/launched on API 36 emulator |
| Release APK | built unsigned; private release signing not performed |
| ZIP alignment | `zipalign -c -P 16 4` exit 0 for both APKs |
| Native ELF alignment | all arm64 libraries have PT_LOAD alignment >=16 KB; x86_64 JNA `libjnidispatch.so` is 4 KB (also reported by lint). ZIP alignment does not repair this native-library limitation |

Artifacts:

- Debug: 122,277,381 bytes (122.3 MB / 116.6 MiB), SHA-256 `FCF9F8E8F247A01844484D31E9EA701EFA9971BF464136D751932EA3F899FC5F`.
- Unsigned release: 102,252,849 bytes, SHA-256 `92FE2A48BCB79B31ACE21668F83880CE1486617DBA30B6B467471652E87E082C`.
- The existing release key was not found in the expected backup location or usual local folders; `HANDOFF.md` states it was kept off this laptop. No replacement key was generated. Version remains 0.2.0 / code 2.

### Public dataset evaluation

- Pinned [Indian Payment SMS Corpus](https://github.com/skyaara/indian-payment-sms-dataset), CC BY 4.0, 172 synthetic redacted rows: all 78 labeled debits parsed with exact gold paise; none of 94 non-bookable rows was auto-added. Five previously missed grammars were repaired. Numeric variants share 58 seed templates, so these are not 172 independent live-bank observations. The fixture and attribution are test-only.
- Canonical [UCI SMS Spam Collection](https://archive.ics.uci.edu/dataset/228/sms+spam+collection), also linked from Kaggle: 5,574 rows (4,827 ham / 747 spam), 0 parsed payment candidates, 0 auto-add candidates, 0 confirmation candidates. Its ham/spam labels do not establish payment accuracy, and it is not representative of modern Indian payment alerts. Raw public texts remain in ignored `.gradle/public-sms-datasets/`; only aggregate output enters test reports.
- Added regressions for negated/future debits, actual OTP prompts versus security footers, malformed monetary tokens, distant balance labels, person/transfer confirmation, card-bill double counting, and reward-eligibility text. Provenance limits are recorded in `docs/autodetect-datasets.md` and `docs/autodetect-parser-sources.md`.

### Emulator observations

The UI checks below used only `emulator-5554`, before the later physical-device installation smoke check above. Synthetic Demo records were created for UI evidence, with the original emulator state backed up on-device. The user explicitly approved exporting these synthetic UI captures into ignored `captures/`.

- Today inspected in all four themes. Early swipe release kept the day open; tapping opened review; completing a swipe created exactly one check-in. TalkBack activation semantics were inspected in code/tree; actual TalkBack use is not tested.
- Synthetic bank SMS auto-added with a hold and reopened the local finished day. Raw message text was absent from the saved state. Real notification-shade Undo restored the prior check-in; Keep released the hold; Skip removed a pending candidate; Add saved a reviewed candidate without a hold.
- Finish-day countdown: temporarily set emulator clock to 9 AM IST the following day, then restored clock/automatic time. The notification displayed a native remaining-time counter and progress; tapping it opened the previous day's finish review; confirming that day canceled it. Unpin removed it and suppressed reposting across restart inside the same window.
- Final full-motion rocket/confetti frames inspected. At 360dp width and 200% font, hero labels and stat tiles stack and the finish control remains reachable by scrolling. With the system animator scale zero, swiping still completed exactly once; the capture showed the finished state. The brief still-art branch was inspected in code, rather than captured frame-by-frame. Original app state, animation/font/display settings and automatic clock were restored after testing. No real-device haptic feel or frame-rate measurement was made.

Not tested: real payment-app notification listener delivery, OEM restrictions, permission revocation, scheduled ten-minute worker execution under Doze, held-purchase sync across actual peers, real-phone accessibility, or promoted Live Update status-bar chips. Countdown posting uses approximate WorkManager timing; a visible progress notification is not evidence of promotion.

## Historical baseline (10 November 2026)

The results below were first recorded on 10 November 2026 on macOS (Apple Silicon) with Temurin JDK 21.0.13, Gradle 8.13, Android SDK platform 36 and Build Tools 36.0.0, using `--no-daemon`. They describe that earlier source revision only. The newer automatic-tracking work has separate evidence in `HANDOFF-AUTODETECT.md`; do not use these counts as current-tree results.

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

## 13 November 2026 — automatic-tracking work (handoff-reported)

`HANDOFF-AUTODETECT.md` reports a later baseline of 187 tests passing across `:core:test`, `:app:testDebugUnitTest` and a clean `:app:lintRelease`. The handoff explicitly says the changes made after that run (including swipe-to-finish and quieter manual-entry action) had not yet been rechecked. Treat 187 as handoff-reported, not independently revalidated here. The same handoff records limited SMS-path emulator checks and untested notification-listener, action-shade, release-worker, and multi-phone behavior.

The handoff reports that a Live Update notification was posted but not promoted on its API 36.0 emulator. Promotion, behavior on Xiaomi HyperOS and OnePlus OxygenOS, real phones, and actual payment-app notification parsing remain unverified. See the handoff's "Not checked" section before making claims.

The handoff also reports a synthetic parser corpus covering 15 banks and Google Pay, PhonePe, Paytm and Amazon. These are representative test cases, not real user messages, bank certification, or proof that notifications arrive from those apps. HDFC (`com.hdfcbank.android.now`), Kotak (`com.kotak.bank.mobile`) and Jupiter (`money.jupiter`) package identifiers were checked against [HDFC Bank's Play listing](https://play.google.com/store/apps/details?id=com.hdfcbank.android.now), [Kotak's Play listing](https://play.google.com/store/apps/details?id=com.kotak.bank.mobile) and [Jupiter's Play listing](https://play.google.com/store/apps/details?id=money.jupiter). The older Kotak package remains as a compatibility entry. Fi was removed after its discontinuation was confirmed by [Federal Bank's fintech partnerships notice](https://www.federal.bank.in/fintech-partnerships). These checks establish package IDs and discontinuation only, not end-to-end notification behavior.

Test files: core `DomainTest` 6, `StateCodecTest` 15, `SyncProtocolTest` 39, `PresetsTest` 6, `WeekRangesTest` 7, `IrohTransportIntegrationTest` 2 (1 skipped); app `TransactionParserScreenshotImporterTest` 13, `RotaryDialTest` 9, `PennyPaletteContrastTest` 4, `CircleRuntimeTest` 4.

Build environment notes: the Gradle wrapper's own download timed out here, so Gradle 8.13 was fetched with `curl` and its SHA-256 (`20f1b117...aed78`) matched the published value; `networkTimeout` in `gradle/wrapper/gradle-wrapper.properties` was raised to 120 s. JDK 21 was installed from the Adoptium tarball with its checksum verified.

## Known limits

- iroh transport is per-member direct QUIC. Signed broadcast messages and a group-secret-derived topic identifier are present in source, but the Kotlin bindings (`computer.iroh` 1.2.0) do not expose a running iroh-gossip transport. This was verified by inspecting the published sources jar and AAR, not by a running build.
- Joining, admin approval and sync need peers to be reachable; an offline admin cannot approve joins. Android battery policies may delay background work and notifications. Nothing here has been tested under Doze.
- Real Google Pay screenshot layouts, haptics and the share-menu flow are unverified on the users' phones.
- Real payment-app notification detection, the scheduled 10-minute hold release, actual held-purchase peer sync, and Live Update promotion remain unverified end-to-end. Synthetic emulator notification actions and the finish-countdown interactions are covered by the current validation above.

## Signing caveat

A valid signature from the private release key does not remove sideload, Play Protect, OEM security or developer-trust warnings, and is not Google Play certification or a security audit. Do not disable Play Protect or other security controls to install. Keep the keystore and password outside Git and reuse the same key for updates.
