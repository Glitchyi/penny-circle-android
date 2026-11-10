# Penny Circle v0.2 — objective and pending work

Status legend: items marked "written, awaiting first build" exist in source but have NOT been compiled, tested or run on a device. Nothing here is verified.

## Objective

Build a private, local-first native Android app for a small circle of friends (one circle per phone, up to 12 members) to log every purchase—or explicitly confirm no spending—within seconds, review daily and weekly limits, maintain an honest streak, and encourage one another without shame. The app should feel like a friendly shared notebook rather than a bank dashboard.

The primary experience is the Today screen: see what is spent, add a purchase, finish the day, and see the circle's check-in status. The rotary telephone control is an optional playful amount-entry mode, not the application's main identity or required path.

Phones communicate directly over per-member iroh QUIC connections. No hosted backend is required. Screenshots and raw OCR drafts stay on-device; only reviewed, approved records and explicitly supported accountability events may sync.

## Written in v0.2 source (awaiting first build)

- [x] Written, awaiting first build: rename to Penny Circle (package/applicationId `com.spendingbuddy.app` unchanged); light-only slate theme with multi-colour accents; bundled Baloo 2 and Mukta fonts.
- [x] Written, awaiting first build: native Kotlin/Jetpack Compose project targeting Android 16 (minSdk 33), Kotlin 2.2.21, JDK 21.
- [x] Written, awaiting first build: INR/paise model, daily and Monday–Sunday weekly limits, categories, reasons, preset data, no-spend completion, streak rules, refunds/transfers, 10 AM Asia/Kolkata grace.
- [x] Written, awaiting first build: circle model: one circle per phone, max 12 members, admin-approved joins via a 24h `pc1.` code, admin = creator and transferable (oldest-member fallback), leave-only, per-member visibility (Totals & status default, Full details), rate-limited nudges, owner-accepted budget suggestions, v2 to v3 state migration.
- [x] Written, awaiting first build: per-member direct iroh QUIC transport (bindings 1.2.0 lack iroh-gossip), signed/validated sync messages with size bounds, event de-duplication and acknowledgements; persistent identity, foreground reachability window, background reconciliation, notifications.
- [x] Written, awaiting first build: Phase 1 hardening: refund import kind, parser fixes, corrupt-state backup and write block, validated restore, keypad as default entry, accessibility improvements.
- [x] Written, awaiting first build: Today, Activity, Circle, Settings, limits, setup and import-review screens; optional rotary dial; share-menu screenshot import with on-device ML Kit OCR and editable review.
- [x] Written, awaiting first build: privacy exclusions for backup/device transfer; signing credentials kept outside the source tree.
- [x] Test sources written (not yet run): core domain, state codec, sync protocol, iroh transport integration; app circle runtime, palette contrast, rotary dial, parser/importer.

## Pending implementation work

### First build

- [x] First compile pass: `:core:test` (75 pass, 1 skipped), `:app:testDebugUnitTest` (30 pass) and `:app:lintRelease` (0 errors, 34 warnings) passed on 2026-11-10 with JDK 21; recorded in `BUILD-EVIDENCE.md`. Still open: `assembleRelease`, signing and device runs.

### Product and UX

- [x] Built and unit-tested, not yet tried on a phone: join code shown as a QR on the admin's invite screen and 'Scan a QR code' on the join screen (Google code scanner), alongside system share and paste.
- [x] Written, awaiting first build: Presets editor (add, edit, set default, delete with Undo) and 'Save as preset' in the purchase flow; presets stay on the phone and are never synced.
- [x] Written, awaiting first build: Activity swipeable `HorizontalPager` for current and prior weeks with date headers, day grouping and chart interaction. The old 'Where it went' category breakdown was dropped and could be restored.
- [x] Written, awaiting first build: Reduced-motion-aware stamp animation and finish-day haptic timing.
- [x] Written, awaiting first build: save-with-Undo snackbar and visible save/import/backup failures. Purchase delete added: tap a purchase on Today or Activity, Delete, then Undo in the snackbar (restores the check-in too). Still open: editing a saved purchase, and discard-import has no Undo because image files are removed.
- [x] Written, awaiting first build: corrupt-state dialog (start fresh or restore a backup; a `.corrupt` copy is kept).
- [ ] Decide whether streaks count a day completed under grace=true.
- [x] Written, awaiting first build: adaptive launcher icon (indigo background, ring of coloured dots, monochrome layer). Not rendered or viewed yet; old PNGs remain as unused fallbacks.
- [x] Written, awaiting first build: circle-size cap UX (n of 12, full state, Approve disabled at the cap).
- [x] Written, awaiting first build: admin-offline limits surfaced on invite, join-waiting and Circle screens; no instant-delivery claims.
- [ ] Accessibility pass: TalkBack labels, touch targets, focus order, light-theme contrast, font scaling, dial alternatives.

### Peer-to-peer validation

- [ ] Real multi-phone circle testing with 3+ members: join code, admin approval, admin offline, leave and admin handover, visibility switch, nudges, budget suggestions.
- [ ] Verify snapshots, acknowledgements, duplicate suppression and stale-nudge suppression across phones.
- [ ] Battery/Doze testing: foreground reachability, background reconciliation, reboot rescheduling, denied notification permission, Xiaomi/OnePlus OEM restrictions.
- [ ] Confirm user-facing copy never claims instant push or Android Live Updates.

### OCR decision (2026-11-09)

- Keep bundled ML Kit on-device OCR plus the rule-based parser. A GLM-OCR (about 0.9B, MIT) on-device second pass was researched and rejected for now: about 1.4 GB of model files (Q8 GGUF plus projector), no verified Android runtime, unmeasured but likely slow, and its benchmarks are for documents, not clean phone UI screenshots. Revisit if a LiteRT/MNN build appears. Android's built-in on-device AI (AICore/Gemini Nano via ML Kit GenAI) is unverified on the Xiaomi 17 and OnePlus Nord 6e and is not used. Screenshots never leave the phone.

### Import and device testing

- [ ] OCR against real Google Pay screenshots from the users: multiple images, layouts, crops, incoming payments, refunds, missing dates.
- [ ] Verify `ACTION_SEND` and `ACTION_SEND_MULTIPLE` on Xiaomi and OnePlus.
- [ ] Verify rotary and keypad haptics, edge-to-edge layout and large-font layout on physical phones.

### Release and maintenance

- [ ] Build, sign (`./build-and-sign.sh`), `apksigner verify` and verify 16 KB alignment; record actual results in `BUILD-EVIDENCE.md`.
- [ ] Install the signed APK on both phones and record results.
- [ ] Bump `versionCode`/`versionName` for the next release with the same private signing key.
- [ ] Keep `private-signing/`, passwords, SDK files, APKs and personal screenshots out of Git.
- [ ] Review iroh and Android dependency updates before upgrading and repeat multi-phone sync and privacy checks.

## Definition of done for the next release

The source compiles and its tests and lint pass on JDK 21; a 3+ member circle works on real phones without a server (approval, admin offline, leave/handover, visibility); the pending UX items above are done; representative screenshot imports are reviewed successfully; battery behaviour is documented from device testing; and a freshly signed APK is recorded in `BUILD-EVIDENCE.md` with real results.
