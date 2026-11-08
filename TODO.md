# Spending Buddy v0.2 — objective and pending work

## Objective

Build a private, local-first native Android app for exactly two friends to log every purchase—or explicitly confirm no spending—within seconds, review daily and weekly limits, maintain an honest streak, and encourage each other without shame. The app should feel like a friendly shared notebook rather than a bank dashboard.

The primary experience is the Today screen: see what is spent, add a purchase, finish the day, and understand the friend's current check-in status. The rotary telephone control is an optional playful amount-entry mode, not the application's main identity or required path.

The two phones communicate directly over iroh peer-to-peer transport. No hosted backend is required. Screenshots and raw OCR drafts stay on-device; only reviewed, approved records and explicitly supported accountability events may sync.

## Completed in v0.2 source

- [x] Native Kotlin/Jetpack Compose Android project targeting Android 16, with Android 13+ runtime support.
- [x] Local-first INR/paise domain model, daily and Monday–Sunday weekly limits, categories, reasons, custom preset data, no-spend completion, honest streak rules, refunds/transfers, and 10 AM Asia/Kolkata grace handling.
- [x] Today, Activity, Buddy, Settings, Spending limits, Onboarding, Invite, Join, and screenshot-import review routes with standard Material 3 navigation.
- [x] Keypad-first amount entry plus optional rotary dial: digits 1–9 require clockwise movement to the stop and release, zero is a centre tap, the wheel springs home, and haptics respect app/system settings.
- [x] Android share-menu intake for one or multiple Google Pay screenshots, persistent local import drafts, on-device ML Kit OCR, editable candidates, duplicate flags, add-more flow, explicit review/save, retry, and removal.
- [x] iroh QUIC pairing and direct peer sync with versioned messages, invite expiry, pairing-secret comparison, revision ordering, event de-duplication, acknowledgements, unlink handling, friend snapshots, nudges, and limit-suggestion protocol messages.
- [x] Android peer lifecycle plumbing: persistent peer identity, foreground reachability window, constrained background reconciliation, boot rescheduling, notifications, and explicit limitations around battery policies and delivery timing.
- [x] Privacy protections: raw screenshots and OCR drafts remain local, backup/device-transfer exclusions are explicit, and signing credentials are outside the source tree.
- [x] Core tests, Android unit tests, Compose compilation, and lint passed before the documentation update.

## Pending implementation work

### Product and UX

- [ ] Wire the Buddy “Suggest new limits” action to a visible suggestion sheet and `PeerSyncManager.suggestBudget(dailyPaise, weeklyPaise)`, with clear pending/accepted/declined states.
- [ ] Add a dedicated Presets editor: create, rename, edit category/reason, choose a default, and delete with confirmation. A manually entered merchant/reason should be savable as a preset from the purchase flow.
- [ ] Complete Activity's swipeable `HorizontalPager` for current and prior weeks, including date-range headers, previous/next controls, day grouping, and tap-to-scroll chart interaction.
- [ ] Finish the reduced-motion-aware stamp animation and verify the finish-day impact/haptic timing.
- [ ] Polish snackbar/Undo behavior for save, delete, discard-import, and restore flows; keep destructive confirmations in dialogs only where needed.
- [ ] Run an accessibility pass: TalkBack labels, minimum touch targets, focus order, contrast in light/dark mode, font scaling, and rotary-dial alternative controls.

### Peer-to-peer validation

- [ ] Pair two real Android 16 phones and verify invite creation, QR/share join, pairing-secret rejection, unlink, and re-pair behavior.
- [ ] Verify expense/check-in snapshots, event acknowledgements, duplicate suppression, limit suggestions, nudge countdowns, and stale-nudge suppression across both phones.
- [ ] Test foreground reachability, background reconciliation, reboot rescheduling, Doze, battery optimization, denied notification permission, and Xiaomi/OnePlus OEM restrictions.
- [ ] Confirm that all user-facing copy accurately describes approximate delivery and never claims instant push or Android Live Updates.

### Import and device testing

- [ ] Test OCR against representative Google Pay screenshots from both users, including multiple screenshots, different languages/layouts, cropped images, incoming payments, refunds, and missing/relative dates.
- [ ] Verify the Android share sheet for `ACTION_SEND` and `ACTION_SEND_MULTIPLE` on Xiaomi and OnePlus devices.
- [ ] Verify rotary haptic feel, keypad haptics, stamp feedback, edge-to-edge layout, dark mode, and large-font layout on physical phones.

### Release and maintenance

- [ ] Rebuild the signed v0.2 APK after the final UI and peer-sync changes, then rerun core tests, app tests, lint, `apksigner`, and 16 KB page alignment verification.
- [ ] Install the rebuilt signed APK on both test phones and record the actual results in `BUILD-EVIDENCE.md`.
- [ ] Increment `versionCode`/`versionName` for the next release while preserving the existing private signing key.
- [ ] Keep `private-signing/`, passwords, local SDK files, APKs, screenshots containing personal data, and generated build directories out of GitHub.
- [ ] Review iroh and Android dependency updates before upgrading; repeat the two-device sync and privacy checks after any transport or lifecycle change.

## Definition of done for the next release

The next release is ready when the pending Buddy/preset/pager UX flows are complete, two real phones can pair and reconcile records without a server, representative screenshot imports are reviewed successfully, notifications and battery behavior are documented from device testing, and a newly signed APK passes the complete build evidence checklist.
