# Penny Circle — scope and remaining work

Dated results belong in [BUILD-EVIDENCE.md](BUILD-EVIDENCE.md). [HANDOFF-AUTODETECT.md](HANDOFF-AUTODETECT.md) records continuity; historical counts do not describe the current tree.

## Implemented and exercised

- [x] Native Android local-first expense/no-spend notebook, Today/Activity/Circle, daily and Monday–Sunday limits, four themes and optional rotary amount entry.
- [x] Optional incoming SMS/payment-notification tracking with local parsing, no old-inbox scan and no stored/shared message text. Clear payments auto-add; uncertain/person payments ask; refunds, credits, failures and OTPs are excluded.
- [x] Ten-minute Undo/share hold; prior finished-day status/streak preserved for peers while held. Real two-emulator SMS/Keep test confirmed prior Rs45 finished state remained private until Keep released Rs122 and reopened status.
- [x] Review-first screenshot/OCR batches with Add more/Continue and on-device processing.
- [x] Five-step optional-tracking onboarding, independent zero-capable limits, editable purchase details/delete Undo and real startup loading/retry state.
- [x] Native glass-like tab bar, resistant body-pane swipes, chart-only Activity week swipes, haptic thresholds and reduced-motion alternatives. Early release/cancel produced no check-in; completed finish swipe produced one in emulator QA.
- [x] Real two-emulator circle creation, invitation, waiting request, admin approval and matching two-member membership; bidirectional totals/check-ins and Full details record delivery. Default totals-only snapshots contained no purchase records.
- [x] API 36.1 native promoted ProgressStyle notification flag and visible progress observed with synthetic payment/reminder QA. Native cards followed Mint to Signal Blue without extending expiry; this remains emulator evidence only.
- [x] Public synthetic parser corpus and aggregate external corpus evaluation; licenses and reproducible commands documented. These are not live-bank certification.
- [x] Windows build/sign pipeline executed successfully: tests/lint, ARM64 release checks, external signing files, ZIP alignment/signature verification and public hash/metadata output.
- [x] Settings privacy/licenses and bundled third-party notices, including Google ML Kit diagnostic/usage disclosure.
- [x] Earlier Xiaomi debug installation/open smoke check recorded; it does not establish current production behavior.

## Release verification completed

- [x] Timer-choice functional QA: one-minute payment duration and 15-minute reminder lead persisted after UI save/restart; native timeout PT1M remained separate from a 599970 ms hold. All original test state restored without clock/display/permission changes.
- [x] Theme-following native progress artwork verified Mint→Signal Blue; original expiry preserved within about 10 ms. Semantic red marks over-budget payments.
- [x] Native promoted live-progress notifications are exercised on API 36.1 with synthetic payments/reminder QA. Theme-artwork visual verification is recorded above; real-device/OEM scheduled posting remains below. The reminder target is fixed 10 PM IST and next-day 10 AM grace remains separate.
- [x] Final checks: 227 tests passed, one optional skip; lint zero errors, 50 warnings and two hints (the extra warning is UseKtx bitmap style).
- [x] Signed production 0.3.0 ARM64 APK verified: v3 certificate, four native ELF libraries ≥16 KB, ZIP 16 KB alignment, embedded source revision and public hash/metadata.
- [x] Public repository/main/tag and GitHub v0.3.0 release published with APK, SHA-256 and public metadata; GitHub asset digest matches. Independent unauthenticated public download hashing matched the local APK.

## Device and product limits

- [ ] Real Android 16 payment-app listener delivery, permissions/revocation, promoted notification behavior, Xiaomi/OnePlus battery restrictions and Doze timing.
- [ ] Actual ten-minute scheduled hold expiry across peers; foreground Keep was verified, expiry scheduling was not.
- [ ] Physical-device layouts, TalkBack, haptics, reduced motion and FPS/performance. Emulator theme/360dp/200% font checks are recorded separately.
- [ ] Representative user screenshot OCR and Android single/multiple-image share flows on both phones, keeping private data on-device.
- [ ] Three-or-more-member circle, offline admin, handover/leave, nudges, budget-suggestion acceptance, acknowledgements and duplicate suppression under real network interruption.
- [ ] Investigate observed adb character-entry/focus interruption in Settings/long join-code input. Clipboard paste worked; a normal-keyboard defect is not established.
- [ ] Confirm/document the product decision about grace-completed days and streaks.
- [ ] Preserve external production key for updates and keep private keys/passwords/personal captures out of Git; refresh dependency notices with upgrades.

Transport is direct per-member iroh QUIC. Running gossip forwarding, instant delivery and a hosted application backend are not implemented. Release evidence and remaining device limits are recorded in BUILD-EVIDENCE.md.
