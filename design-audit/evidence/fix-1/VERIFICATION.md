# Batch 1 verification

Owner approved batch 1 only on 13 November 2026; logo and scale choices remain open. Branch: `codex/design-fix-batch-1`. A subsequent explicit request adds the separate top-level swipe blank-space defect. No publication is part of this checkpoint.

## Baseline

The original debug APK is SHA-256 `4F4B405541CB3A0EA40FBA6C969813A99581246B9AD77B00514C3FE66333D6A0`. `before-limits.png` shows ungrouped ₹1000 and whole-rupee prefills. `before-maximum-360-font2.*` copies the original audit's runtime evidence 24, whose full semantic amount is 99999999.99 while the last digit is clipped. `before-swipe-gap.png` is a fresh capture during a 1.6-second Today→Activity gesture: source content moves left while the right side exposes only the background.

Only synthetic emulator data is used. Emulator-5554's app-local state was copied on-device to `files/buddy-state-before-design-fix-1.json`; the prior production-test backups are preserved. Initial limits are 30000/210000 paise, one ₹123 purchase, Signal Blue theme and a paired synthetic circle. Initial display: physical1080×2400/density420, no overrides; font1.0; animator setting absent. No physical phone is involved.

## Completed batch-1 checkpoint, before scope expansion

- Core suite: 163 tests, 162 passed and one optional online skip, no failures/errors. Budget-specific tests: seven passed, covering exact prefill round trips, malformed/negative/subpaise/oversized input, zero and checked seven-day calculation boundaries.
- App suite:71 passed; combined233 passed/one optional skip. Release lint:zero errors,50 warnings/two hints. Debug assembly and Kotlin compilation passed. Final batch-1 APK:122877224 bytes, SHA-256 `8A95B4E7AF2F8CC0B505A2032DD50807D5FD13E9579064009FF2F6B854C71F5D`.
- Runtime: exact daily/weekly decimals survived save/reopen; invalid text retained stored30050/210001; explicitzero saved/reopened;7×300.50 yielded2103.5. At360dp/font2, both errors, all four wrapped presets and Save were reachable after scrolling.
- Light Horizon amount99999999.99 fits at360/411dp,font1/2; deletion/re-add,Dial and reduced motion preserve the value/announcement. Dark360/font2 fit passed. The grouped details summary initially hid Change; weighted summary text fixed it, and the final APK showed Change and returned to the same full amount.
- Seeded import display:₹1234.50 became₹1,234.50 with Fri,13Nov2026; invalid12.?x/unreadable-date stayed raw in Review purchases. No import was saved and no fresh OCR evaluation was performed. Seeded onboarding showed grouped budget presets; setup was not completed. Original app-local snapshots restored all synthetic data afterward.
- Mid-drag captures show both current/adjacent panes in three directions. Short-swipe cancellation restored its source. A date-only synthetic fixture enabled selecting2–8Nov in Activity; that week survived leaving for Today and swiping back. No device clock change was made. Original data was restored afterward; no live suggestion/nudge was sent.
- Scoped brand, interaction and visual rechecks found no remaining batch-1 issue in the exercised states. Circle suggestion precision/zero use the tested shared helpers and were inspected in source; no live suggestion was sent. Physical TalkBack/haptics, frame-by-frame/FPS and OEM behavior remain unverified.

The owner then expanded implementation to all eight batches and asked for one consolidated test/build/emulator pass at the end. The current source is newer than this checkpoint; these counts/hash describe batch1 only, not the untested expanded changes. Progress is in `../../IMPLEMENTATION-PLAN.md`.

The original full audit remains historical. This file and `fixed.jsonl` describe only subsequent scoped fixes; blocked accessibility/device checks are not upgraded to passes.
