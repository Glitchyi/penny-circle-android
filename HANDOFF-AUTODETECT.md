# Penny Circle — completed 0.3.1 design fixes and release

14 November 2026. Owner authorized all eight design-fix batches where possible, one consolidated final validation stage, then explicitly requested “speed it up and lets release”. Release: https://github.com/Glitchyi/penny-circle-android/releases/tag/v0.3.1 . The repository remains public. Application source commit `a528e1e0b7fd0b96bb3ebac5c05c044add2542a5`; later documentation commits may follow. No new signing key was generated.

Signed `release/Penny-Circle-0.3.1-arm64.apk`:77,203,821bytes; SHA256 `7e02fabd51a36084ca9997658ec4feb2f5ab9d5b22ebc4bf0ab4364f11d25c19`; version0.3.1/code 4, package com.spendingbuddy.app, Android 13+/min33/target36, ARM64-only/nondebuggable. Existing production certificate 84e2559af0440e8a514d154904b9a2a203b617a204c8c46968cb3d94782d98fd verifiedv3; native4ELF+ZIP16KB checks passed. Signing files/password remain outside Git; never print them. Source identity is recorded in external metadata, not an embedded revision asset.

Consolidated final tests:264 passed/one optional online iroh skip; release lint0 errors/69 warnings/two hints. Initial15 numeric Circle string-format errors were corrected without suppression. Focused corrections and versioned production pipeline passed. Corrected UI debug hash4dd85b7004e7d745a2039017b802b9add7493ccfb2af6923e2c3abe97204ed7c,123261452bytes/version0.3.0; final production differs by0.3.1 version bump and final required-field hints. Automated/artifact/source evidence: design-audit/evidence/all-batches/VERIFICATION.md and artifacts.json. Current fixed ledger126; six broad owner-scale findings remain open in DEFERRED.md. Original audit verdicts remain historical.

Emulator checks passed onboarding/permission Review/errors, exact limits and failure draft retry, max amount360dp/font2, editable seeded OCR five-field review/save, protected backup recovery and preserved original bytes, warm routing, native modal Back/IME-first/draft reopen/keyboard focus, actual two-emulator Circle create/invite/join/cancel/rejoin/approve/Retry and bounded bidirectional snapshots. No fresh OCR recognition or full physical TalkBack/haptics/FPS/OEM/Doze matrix was claimed. Logo/scales and F-Droid remain deferred; a nonblocking no-circle illustration accent2 observation is retained for the next audit.

Both original synthetic paired profiles restored byte-for-byte from files/buddy-state-before-final-suite.json before launch; hashes5554=75e444a7ec8a78ea4116d3a638d4cfb716a928f53aa57e9dde53b2201cc5dba6 and5556=cb09a6b28b7514e44e603e96138b054dea41ac8ca07f2b291071d63e1dc12bf6. Both display1080x2400/density420,font1.0,animatornull restored. Both emulators closed gracefully; Gradle daemon stopped. Preserve all older on-device before-production-tests/before-design-fix backups. No physical phone data, clock or permission settings changed. Local audit captures/raw logs, installed skills and Codex attachment folders are not release assets.


---

# Penny Circle — completed 0.3.0 release handoff

Completed 13 November 2026. Public repository: https://github.com/Glitchyi/penny-circle-android . Published release: https://github.com/Glitchyi/penny-circle-android/releases/tag/v0.3.0 . Source and tag v0.3.0 identify `46b4ffe5b9cb696f9aaf3a580a5a9a5ead3e4b01`; later documentation-only commits may follow on main.

The signed ARM64 APK is `release/Penny-Circle-0.3.0-arm64.apk`, 76,458,349 bytes, SHA256 `9bd88986af8ed04cb4ea690fec78e0625332661eceab4c11fb708a06c9a23e7e`. It is nondebuggable, Android 13+/min33/target36. v3 signature, four ARM64 native ELF libraries >=16KB, ZIP16KB alignment, all15 notice assets and embedded source revision passed verification. GitHub contains only APK/hash/public metadata assets; a separately downloaded unauthenticated public APK matched the local hash. Final tests: core158pass1optional online skip +app69pass =227pass1skip; lint0 errors50warnings2hints. BUILD-EVIDENCE.md has details and limitations. No new app build is needed for documentation-only follow-up.

Latest user requests are implemented: five-step onboarding with actual startup loading, payment setup/demo and permission highlights; refined purchase sheets; resistant pane/finish swipes with haptics; glass-like bar; rocket/confetti; local payment parsing and a separate10-minute private Undo/share hold; real two-emulator Circle flow. Payment notification choices1/2/5 minutes/default2 and countdown leads15/30/60/default30 before fixed22:00IST are local preferences. Grace remains next-day10AM. Circular ₹ tracker heads and progress bars follow the selected theme in native cards and previews. Native Mint→SignalBlue changed color and preserved the existing expiry. Timer choice persistence and distinct native timeout/hold were checked. Final real-clock60-minute countdown showed the theme-matched ₹ coin/bar and chronometer to22:00; its synthetic per-day dismissal XML was snapshotted, temporarily cleared for the test, and restored exactly. No Android promotion permission/eligibility was bypassed.

The owner explicitly authorized new production signing keys, committing/pushing main, public visibility and release publication. Those actions are complete. The new key/password remain outside Git under `C:\Users\User\private-signing\`; preserve this key for future updates. Certificate SHA256 `84E2559AF0440E8A514D154904B9A2A203B617A204C8C46968CB3D94782D98FD`. Switching from earlier debug/original certificates requires a user-approved fresh install and local backup first. No physical phone was touched in this release run. Product name is Penny Circle; internal applicationId/package remains `com.spendingbuddy.app`.

Both emulators run the current theme-aware debug code and retain a synthetic paired circle. Preserve5554's on-device backups. Newest payment/theme tests restored5556 to LightHorizon/payment2/lead30/reminders&countdownoff/twoexpenses/trackingon. Temporary powercfg changes have been restored and queried: saved scheme6fecc5ae-f350-48a5-b669-b472cb895ccf, sleepAC/DC0seconds, displayAC900/DC600seconds. Do not kill the stale keep-awake PID. Captures, downloaded raw datasets, APKs and all signing material remain ignored/private. Local signing-check APK intermediates may remain in ignored release/; the distributable is the explicitly named Penny-Circle APK above.

Remaining device/product limitations are tracked in TODO.md: real payment-app/OEM delivery, physical haptics/TalkBack/FPS, Doze/ten-minute scheduled peer expiry, 3+ member/offline scenarios and independent security review. This is a signed distributable early beta, not a claim of those unperformed checks.

The following sections are historical session notes. Their no-commit, missing-key, pending release and older test/timer wording does not override this completed checkpoint.

---
# Handoff: automatic tracking, week rings, impulse habit (2026-11-13)

Read this with `AGENTS.md` (working agreement), `HANDOFF.md` (repo rules) and `HANDOFF-REDESIGN.md` (design system).
This file covers the work started on 2026-11-13 after commit `02a94f2`. **Nothing in it is committed yet.**
The user said: do not commit unless asked. Work directly on `main`, no PRs. AI attribution lines in commits are allowed.

## Later UX refresh (2026-11-13)

### Active production preparation (later 2026-11-13, not complete)

Latest steering: the user explicitly authorized committing/pushing to GitHub, making the repository public and publishing a release with the built APK **once complete**. This supersedes the earlier no-commit instruction for this publication. They also asked for Penny Circle branding everywhere (keep internal Android package ID stable) and timer choices: payment updates 1/2/5 minutes, pre-reminder lead 15/30/60 minutes; defaults remain 2 and 30, reminder remains 10 PM IST and grace remains next-day 10 AM. `bank_corpus` is implementing these local preference choices and tests now; do not start the final release build until that change is validated. Screenshots of current Today, permission highlights and Live progress have been shared in chat.

Latest completed checkpoint: debug 0.3.0/code3 APK 122,828,976 bytes, SHA-256 `0FE53105FA59C4D0373FA0BE451597764F9EBC1213E30610E39ADBEC9D0D3BE6`; core154 + app69 pass, 1 optional online skip; lint0 errors/49 warnings/2 hints. This APK includes the current budget-card refresh fix and revised fixed timers, before customizable options.

Critical emulator checks passed: real Circle create/invite/pending/admin approve, bidirectional totals/check-in sync, totals-only vs full-details privacy, actual held SMS purchase preserving peer finished status until Keep (see ignored `captures/production-readiness-2026-11-13/circle/REPORT.md`). On API36.1 the latest budget card is promoted, expires after2 minutes while the expense remains held10 minutes, and Undo of an older held purchase refreshes the surviving card's budget without resetting its deadline (PT46.873S remaining). The 9:31 PM timer is promoted, review deep link opens the correct finish sheet, completion/disabled reminders cancel it, Unpin suppression survives restart. 5556's original paired synthetic state and automatic IST clock/permissions were restored. Root tested all4 themes at360dp/200% font, the96dp bar labels are readable; native day cancel/early-release both left0 check-ins, completed swipe produced exactly1. Activity body swipes now change panes while chart-card swipes retain week navigation. Both emulators remain paired with synthetic data; no phone data touched.

Licensing/privacy and signing pipeline: 15 bundled license assets include119 dependency metadata entries/51 original notices; Settings→Privacy and licenses reader is implemented and verified. `PRIVACY.md` discloses ML Kit diagnostic metrics separately from local processing. Key-file extensions and release output are ignored. `build-and-sign.ps1` signs only after tests/lint, requires ARM64 non-debuggable APK, verifies native ELF and APK ZIP16KB alignment and emits APK/hash/public certificate metadata. Source/artifact naming now Penny Circle; Android package remains `com.spendingbuddy.app`. Source has not been committed or published yet. GitHub connector returns404 for this private repo; authenticated `gh` CLI has ADMIN access and SSH `git ls-remote` works. Current remote is `git@github.com:Glitchyi/spending-buddy-android.git`; no existing releases/tags. The explicit public/publish action remains to do after final checks.

The user requested onboarding Live Update preview, granted-permission highlights, actual Live Update testing on a newer emulator, real two-emulator Circle tests, a production build **at the end**, finish-swipe haptic resistance, and resistant pane swipes plus an Apple-guided glass-like navigation bar. They explicitly requested fast subagents, authorized new release keys, and went out asking to keep the laptop awake and complete the work. Do not ask for more routine choices. No phone is currently connected; do not alter physical-phone data.

- Version is being prepared as 0.3.0/code 3. Release is ARM64-only; debug retains ARM64+x86_64. `NewApi` lint suppression was removed. The newest debug APK at this point is 120,899,733 bytes, SHA-256 `E958B3DDC17B24DED12AC99EE5C13C6FFE401FA3DF7FA61E5B1451DBDD562CA0`. Core 155 total/154 pass/1 online skip, app 67 pass; lint 0 errors, 43 warnings, 2 hints. This is a development checkpoint, not final production evidence.
- User-authorized new key exists outside Git at `C:\Users\User\private-signing\penny-circle-release.p12`, password file `password.txt`, alias `pennycircle`, certificate SHA-256 `84E2559AF0440E8A514D154904B9A2A203B617A204C8C46968CB3D94782D98FD`, valid through March 2054. Never print the password. `build-and-sign.ps1` is prepared and syntax checked; it has NOT been run. The final build must happen only after feature/integration validation. It verifies ARM64 ELF load alignment and APK ZIP alignment, signs, verifies and writes hashes/metadata in ignored `release/`.
- `bank_corpus` owns emulator-5556, the newly provisioned `penny_liveupdates` AVD (Android 16 QPR1/API 36.1, SDK 36 + minor 1, RAM1536/2 cores) for real synthetic SMS/progress/promotion diagnostics. Root owns emulator-5554 for UI QA. `fast_release_tools` has prepared fresh-XML Circle UI helpers and will own both for actual create/join/approve/sync testing after handoff. No paired states may be fabricated as test proof.
- Root backed up the user's current empty-spending emulator-5554 profile on-device at `files/buddy-state-before-production-tests.json` and `files/penny-ui-before-production-tests.xml`, then used a synthetic Demo onboarding fixture for UI tests. Do not export personal records. Current captures are ignored under `captures/production-readiness-2026-11-13/`.
- **Power restoration required at the end:** a background wake helper did not persist. Native `powercfg` temporarily sets AC and battery idle sleep/display timeouts to zero. Exact original scheme and all four values are saved in `captures/production-readiness-2026-11-13/power-settings.json`; restore with `/setacvalueindex` and `/setdcvalueindex` for `SUB_SLEEP STANDBYIDLE` and `SUB_VIDEO VIDEOIDLE`, then `/setactive` that saved scheme. Do not leave these changes after completion. The `keep-awake.pid` is stale; do not kill an unrelated reused PID.
- Pending: visual QA of new bar/swipes at normal/small/200%/RTL/reduced motion, finish cancellation and single completion, actual notification eligibility/promotion on 36.1, real Circle exchange/held privacy, final checks and signed release, docs/artifact evidence, restoring power settings. No commit unless asked.

Later user-requested demo reset: cleared only `emulator-5554` Penny Circle app data with `pm clear`, then launched the existing latest APK at welcome step 1/5. Confirmed zero expenses, onboarding incomplete and tracking off. The earlier restored six-record emulator state and its app-private backups are gone by explicit request. No simulated messages have been sent in this fresh session; the user wants to do that later. The physical phone was not touched.

Subsequent Xiaomi reinstall request completed: the user explicitly requested uninstalling the old app and installing fresh. Normal USB installation initially returned `INSTALL_FAILED_USER_RESTRICTED`, then succeeded once the user was ready to approve the phone's prompt. Latest UX debug APK (122,542,201 bytes, SHA-256 `769A0D760662781B6BFC1ACFD84FBECBF9CD564DAA86434B823621CAD834D644`) is installed as 0.2.0/code 2; `am start -W` confirmed MainActivity opened. No Android security controls were changed and no phone screenshots were captured. A copy is also in the phone's `Download/Penny-Circle-0.2.0.apk` for sharing. Explorer was launched selecting the PC debug APK; computer-use approval blocked inspecting Explorer, so its selection was not visually verified. Real-phone feature checks remain pending.

The current tree also includes the onboarding/payment setup and navigation refresh. `OnboardingFlow` has five steps and keeps tracking optional; source rows describe setup/readiness honestly. `PurchaseDetailsSheet` supports editing and impulse changes, with delete separated from the reversible edit flow. `AppNav` keeps one tab bar outside the `NavHost`; tabs use opaque full-width horizontal slides around 300 ms without alpha/scale, while pushed panes retain parallax. Onboarding stages do not fade. Reduced-motion follows Android setting changes live. Back handles explicitly and preserves state. Startup is gated on real local initialization state and shows `StartupSurface` while loading or retryable failure; initialization work is dispatched off the main thread, optional startup tasks are isolated, and peer/network reachability does not gate the app. Zero and independent daily/weekly limits remain allowed.

Emulator validation is complete for the stated onboarding, source-permission, purchase-detail, large-font, motion and restoration scenarios. Full current validation: 218 passed and 1 optional online iroh test skipped; core 154 passed, UCI evaluation passed; app 64 passed. Lint: 0 errors, 41 warnings, 1 hint. Debug and unsigned release built, and the final UI rebuild passed. See `BUILD-EVIDENCE.md` for exact run details. Real-phone behavior, TalkBack, FPS/performance and release signing are still limitations.

The latest UI refinements include opaque 300 ms full-width horizontal tab slides (no alpha/scale), smaller pushed-pane parallax, and no onboarding-stage fades, plus wrap-safe detail labels, less squeezed purchase-row labels, a responsive header/footer and sheet system icons, and scrollable no-circle setup actions. Reduced-motion preference observes Android setting changes live. Emulator checks at 360dp/200% font passed. A frame captured with animations slowed to 5x showed clean page travel without text bleed and an anchored tab bar. Changing animator duration scale to zero while the app remained alive left tabs, Settings and purchase sheet usable; removing the setting restored its original null/default. Emulator state was restored (six expenses, mint_night, onboarding complete, no UX-demo IDs, original permissions, 1080×2400/density 420/font 1.0, portrait); no clock change was made. See `BUILD-EVIDENCE.md`. FPS, TalkBack, startup retry under injected failure and real-phone behavior are not established.

## STATE WHEN THE PREVIOUS AGENT STOPPED (usage limit, 2026-11-13) - read this first

### Continuation on 2026-11-13 (current tree, still uncommitted)

- Added `BankSmsCorpusTest`: synthetic representative bank/payment-app cases, malformed-number safeguards, explicit pending/OTP rejection and merchant-footer coverage. These are not verified live bank templates; see `docs/autodetect-parser-sources.md` for the official-source coverage limits.
- Fixed held-purchase privacy: the circle retains the previous finished-day status and streak during the undo window. Multiple held purchases can be undone in either order; Keep or expiry reveals the reopening. Explicit person payments ask even if a matching merchant rule exists, and built-in merchant names require a whole word or exact match.
- Reviewed swipe state, early release, large text, accessibility activation and RTL positioning. Manual entry remains a quiet secondary button when tracking is enabled.
- User additionally requested a better rocket and confetti: native Canvas rocket and celebration updated, with a still reduced-motion acknowledgement.
- Added licensed public dataset evaluation: pinned 172-row Indian synthetic corpus (78/78 gold debit amounts; 94/94 negative rows never auto-add) and optional UCI/Kaggle-linked 5,574-row aggregate probe (0 payment candidates). Raw UCI text is ignored, not in app/source. The five missed debit grammars found by the external corpus are fixed; transfers/card-bill payments ask first.
- Added small-detail fades/real OCR processing feedback and large-font layout fixes. Rocket now has a tapered plume and 44 visible confetti pieces.
- Added default-off **Settings → Reminders → Finish-day countdown**: 10 PM–10 AM IST, system countdown/progress, correct-day review deep link, Unpin suppression and immediate cancellation when finished. Uses existing approximate WorkManager passes, no exact alarms/per-second workers. Actual counter, review, completion cancellation and Unpin/restart were verified on the API 36 emulator by temporarily changing its clock and restoring it.
- Earlier continuation baseline: 212 tests passed, 1 optional online test skipped; lint 0 errors / 42 warnings / 1 hint; debug and unsigned release built. Superseded by the current final-validation result in the later UX refresh note above and `BUILD-EVIDENCE.md`.
- Release key was not found in the expected local backup location or the usual Documents/Downloads/Desktop folders. The older `HANDOFF.md` says it was kept off this laptop. Do not generate a replacement key for updates.
- Final UI QA: all four themes, settled 360dp/200% font layout, normal rocket/confetti frames, reduced-motion completion, and notification-shade Undo/Keep/Add/Skip checked on the emulator. Countdown review/cancel/Unpin tested inside a simulated grace window. Original app state/display/animation/clock settings restored. Synthetic captures are ignored under `captures/autodetect-2026-11-13/`, with explicit user permission; no personal screenshot export was authorized.

### Next work from this continuation

Later on 2026-11-13, the user authorized deleting any old Penny Circle data and installing fresh on the Xiaomi. Package queries found no old package. After replacing the cable, a normal debug APK installation succeeded; Package Manager confirms 0.2.0/code 2 installed and MainActivity running. No old app data was removed, no security controls were disabled, and no phone screenshots were exported. The following feature checks remain pending.

1. Real Android 16 device validation: payment-app listener delivery, notification permission revocation, Live Update promotion, TalkBack and OEM battery behavior. None is proven by the emulator results.
2. Scheduled hold release/countdown delivery under Doze and actual multi-peer held-purchase sync. Both peers must use the same build until the canonical-byte signing compatibility risk below is fixed.
3. Real OCR import transitions with the new loading feedback and representative user screenshot layouts, keeping their data on-device.
4. Restore the existing release signing backup before producing an update signed with that key; native x86 JNA 4 KB ELF alignment remains a known emulator ABI issue. Arm64 native and APK ZIP alignment checks passed.
5. No commit or PR was made. Commit only if the user asks.

The bullets below record the previous stop, before this continuation.

- Built after the 187-test run and **compiled and installed, but not looked at**: `SwipeToFinish` in
  `ui/TodayParts.kt` (replaces `FinishRow` on Today; swipe finishes the day through the new `onFinishNow`
  parameter of `TodayScreen`, tap opens the review sheet) and the quieter "Add by hand" button (`SecondaryButton`
  when `state.autoDetect` is on). The last screenshot taken was probably the splash screen. Check both on the
  emulator: drag the handle, check it springs back when released early, check TalkBack activate opens the sheet.
- Two helper agents were started and then stopped or cut off part-way. **Their files may be half-edited:**
  1. Bank SMS corpus: `core/src/test/kotlin/com/spendingbuddy/app/BankSmsCorpusTest.kt` (may not exist or may be
     partial) and parser edits inside `PaymentMessageParser` in `core/.../Detection.kt`. Run `:core:test` first.
     If `Detection.kt` does not compile or `DetectionTest` fails, review `git diff` for that file; the version
     described in this document passed all tests. The brief for that work is user request 9 below.
  2. Docs: `README.md`, `TODO.md`, `BUILD-EVIDENCE.md`, `HANDOFF-REDESIGN.md` may be partly updated. Read the
     `git diff` of each and finish or correct them against this file.
- Tests and lint have **not** been re-run since the swipe and button change. Re-run the full command below.
- Still nothing is committed. Ask the user before committing.

## What the user asked for (their words, in order)

1. Drop the budget arc on Today for a better design. They chose **week rings**.
2. Integrate with Android: when the app thinks a purchase was made, show Android **Live Updates** with the day's
   remaining or used budget.
3. **Parse payment messages locally** (SMS and notifications). "Everything should be local."
4. Decisions made in a question round:
   - Sources: payment-app notifications **and** SMS.
   - On detection: **add automatically, allow undo**.
   - Circle: auto-added purchases are shared only **after a 10 minute undo window** (or at once on Keep).
   - Unclear payments (to a person, no shop name): **ask instead of adding**. Refunds/credits are never added.
   - Category: **learn from history and let the user customise** (rules page).
   - No catch-up scan of old SMS.
5. "Focus the sms and notification parsing [as] the default method"; typing a purchase in is the fallback.
6. "Bring in styles from the railway app and cursor": flat near-black panels, hairline borders, tight type, mono
   labels, very little glow. Borrow the traits, never their branding.
7. "More bold styling to better show the numbers and importance of being in limit."
8. "If there were any frivolous purchases we should make a habit of removing that and all of this should be synced
   with the friends in the circles." Implemented as an **impulse-free habit** (interpretation, not confirmed by the
   user): count impulse purchases and days in a row without one, show it on Today, share the totals with the circle.
9. Validate parsing with SMS formats from all major banks, plus generic messages to test false positives.
10. A swipe gesture to finish today; the "Add by hand" button is too bright.

## What is built (all uncommitted, in the working tree)

### core (pure Kotlin, unit tested)
- `core/.../Detection.kt` (new): `PaymentMessageParser.parse(text, title)`, `applyDetection`, `guessCategory`,
  `reasonToAsk`, `isDuplicateDetection`, `undoAutoExpense`, `keepAutoExpense`, `releaseExpiredHolds`,
  `heldExpenses`, `addPendingDetection`, `skipPendingDetection`, `upsertMerchantRule`, `deleteMerchantRule`,
  `merchantKey`, `looksLikeBusinessSender`. Constants: `ASK_ABOVE_PAISE` (Rs 20,000), `DUPLICATE_WINDOW_MS` (3 min),
  `MANUAL_DUPLICATE_WINDOW_MS` (10 min).
- `Models.kt`: `Expense` gained `source`, `heldUntil`, `needsCategory`. New `MerchantRule`, `PendingDetection`,
  `DetectionMark`, `ImpulseStats`, `impulseStats()`, `isImpulse()`. `BuddyState` gained `autoDetect`,
  `autoDetectOffered`, `merchantRules`, `pendingDetections`, `recentDetections`, `heldCheckIns`.
  `FriendSnapshot` gained `impulseWeekPaise`, `impulseWeekCount`, `impulseFreeDays` (-1 = older app).
  `STATE_SCHEMA_VERSION` is still 3: every new field has a default, so old files load.
- `p2p/SyncProtocol.kt`: `snapshotFor` leaves out purchases whose `heldUntil` is in the future (also from the
  totals), strips the local-only fields, and carries the three impulse numbers. `validateMessage` bounds them.
- `StateCodec.encodeBackup`: drops pending detections, marks, held check-ins and holds; keeps merchant rules.
- Tests: `core/src/test/.../DetectionTest.kt`.

### app
- `detect/PurchaseDetector.kt`: entry point, single worker thread, `PAYMENT_APPS` and `SMS_APPS` package lists.
- `detect/PaymentNotificationListener.kt`: `NotificationListenerService`, reads only the listed apps.
- `detect/SmsReceiver.kt`: `SMS_RECEIVED` receiver; drops ordinary phone-number senders unread.
- `detect/DetectionNotifications.kt`: the Live Update (platform `Notification.ProgressStyle`, API 36, with the
  `android.requestPromotedOngoing` extra and `setShortCriticalText`), the "Add it?" notification, and
  `DetectionActionReceiver` (Undo / Keep / Add / Skip).
- `detect/HoldReleaseWorker.kt`: WorkManager job that ends undo windows.
- `AndroidManifest.xml`: `RECEIVE_SMS`, `POST_PROMOTED_NOTIFICATIONS`, the listener service, both receivers.
- `BuddyRepository.kt`: `setAutoDetect`, `dismissAutoDetectOffer`, `recordDetection`, `undoAuto`, `keepAuto`,
  `releaseHolds`, `addPending`, `skipPending`, `saveMerchantRule`, `removeMerchantRule`, `setImpulse`.
  `saveExpense` clears `needsCategory` and learns a merchant rule when an auto-added purchase is corrected.
- `MainActivity.kt`: `requestSms`, `openNotificationAccess`, `openLiveUpdateSettings`, `openAppSettings`.
- `Destinations.kt`: `AUTO_DETECT`, `EDIT_PREFIX` ("edit:<purchase id>").
- UI: `ui/TodayHero.kt` (new: hero, limit bar, week rings, the three notice cards), `ui/AutoDetectScreen.kt` (new),
  `ui/Screens.kt` (Today uses the hero; `AutoActions`; smaller add button when tracking is on; edit keeps hidden
  fields), `ui/AppNav.kt` (route `autodetect`, edit sheet, purchase dialog got Edit and Mark as impulse),
  `ui/SettingsScreens.kt` (row "Automatic tracking"; row helpers are now `internal`), `ui/CircleScreen.kt`
  (impulse line per member), `ui/Components.kt` ("Auto" and "Needs category" tags, empty state text),
  `ui/GlassSurface.kt` (flat panel, no shadow in dark), `ui/AuroraBackground.kt` (static: one glow and a dot grid).
- `ui/BudgetGauge.kt` and `ui/HorizonArc.kt` are no longer used by Today. `budgetIntensity` in `HorizonArc.kt` is
  still used. They can be deleted once the user is happy with the rings.

## Checked, and how

- `:core:test :app:testDebugUnitTest :app:lintRelease` passed on 2026-11-13: 187 tests, 0 failures, lint clean.
- On the emulator (`emulator-5554`, AVD `penny_test`, Android 16 / API 36.0, Violet Night theme), with real
  `adb emu sms send`:
  - HDFC UPI SMS to Swiggy: added, Today total updated, held card with Undo/Keep shown, notification posted as an
    ongoing progress notification with title "Rs 5 over today's limit".
  - ICICI SMS to an unknown name: "Add it?" notification and in-app card; Add saved it as Other / Needs category.
  - SMS from a phone number and an OTP SMS: ignored.
  - In-app Add and Undo buttons: work, and they cancel the matching notifications.

## NOT checked (say "not tested" if asked)

- **Live Update promotion.** On the API 36.0 emulator the notification is posted but not promoted (no status bar
  chip): that image has no `POST_PROMOTED_NOTIFICATIONS` app op. It needs an Android 16 QPR1 (API 36.1) image or a
  real phone. Behaviour on Xiaomi HyperOS and OnePlus OxygenOS is unknown.
- The notification listener path with a real payment app (only the SMS path ran). Package names in
  `PAYMENT_APPS` are from memory and not verified against the Play Store.
- Notification action buttons (Undo/Keep/Add/Skip pressed in the shade), the Edit deep link, the rules sheet,
  the 10 minute release job, and a real circle sync of a held purchase (unit tests cover the snapshot logic only).
- Light Horizon, Mint Night and Signal Blue after the restyle; small screens; large fonts; TalkBack.
- Any real phone. Sideloaded apps on Xiaomi/OnePlus usually need "Allow restricted settings" before notification
  access or SMS can be granted.
- Real bank messages. The parser was written against sample formats, not the user's inbox.

## Known risks

- **Mixed app versions break gossip sync.** Broadcast messages are signed over the re-encoded message with
  `encodeDefaults = true`. New fields (`Expense.source/heldUntil/needsCategory`, the impulse fields) change the
  bytes, so an old app and a new app cannot verify each other's broadcasts. Direct sync is unaffected. Both phones
  must install the same build. Fixing it properly means signing the received bytes instead of re-encoding.
- Two equal payments within 3 minutes with no reference number are treated as one.
- The 2026-11-13 automatic-tracking exception (auto-add with undo and a ten-minute circle hold) is now documented in `AGENTS.md`; screenshot/OCR imports remain review-first.
- Fixed in the continuation: held purchases no longer expose a reopened check-in/streak to the circle before Keep or expiry. Tests cover multiple holds and undo order.

## Next steps, in order

1. Bank SMS corpus: add tests with formats from the major Indian banks and wallets, plus generic messages that
   must not match (user request 9). Fix the parser for anything that fails.
2. Swipe to finish today (user request 10) and a quieter "Add by hand" button (the accent gradient is too bright).
3. Update `AGENTS.md`, `README.md`, `TODO.md`, `BUILD-EVIDENCE.md`, `HANDOFF-REDESIGN.md` for the new scope and
   privacy wording (SMS permission, notification access, auto-add rule change).
4. Screenshot the other three themes and fix contrast if needed (`ThemeContrastTest`).
5. Ask the user before committing. Suggested message: "Add on-device purchase detection, week rings and impulse habit".

## How to build and run

PowerShell 5.1 on Windows 11 (no `&&`):

```
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
.\gradlew.bat :core:test :app:testDebugUnitTest :app:lintRelease :app:assembleDebug --console=plain -q
```

Emulator: `%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe -avd penny_test -no-snapshot -no-boot-anim -gpu host`.
adb: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`.

Test a detection on the emulator (test device only, never on the user's phones without a yes):

```
adb shell pm grant com.spendingbuddy.app android.permission.RECEIVE_SMS
adb shell pm grant com.spendingbuddy.app android.permission.POST_NOTIFICATIONS
adb shell cmd notification allow_listener com.spendingbuddy.app/com.spendingbuddy.app.detect.PaymentNotificationListener
adb emu sms send HDFCBK "Sent Rs.25.00 From HDFC Bank A/C *1234 To SWIGGY On 13/11/26 Ref 123456789012"
```

Automatic tracking must be switched on in the app first (Settings, Automatic tracking).

## Rules that still apply

- No backend. Parsing stays on the phone. Never store or send message text.
- Never uninstall, clear data, or reinstall over a different signature on the user's real phones without a yes.
- Do not turn off Play Protect or other security controls. Release keystore stays outside Git.
- Colour is never the only signal. 48dp touch targets. Reduced motion falls back to `snap()`.
- Say "not tested" for anything not tested.
