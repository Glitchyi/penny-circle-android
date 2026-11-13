# UI loading and motion audit

Updated 2026-11-13. See [ui-navigation-refresh.md](ui-navigation-refresh.md) for the navigation contract and [BUILD-EVIDENCE.md](../BUILD-EVIDENCE.md) for dated checks.

## Behavior

- Today uses entry motion, changing totals, rings and expanding purchase rows. These are presentation effects, not loaders. Auto cards distinguish the ten-minute Undo/share hold from the private budget notification (1, 2 or 5 minutes, default 2).
- Add purchase uses keypad and two-step transitions with inline validation. Local input has no artificial spinner. Finish swipe has a tap review alternative, early-release spring-back and completion feedback. The rocket/confetti celebration becomes a brief still acknowledgement under reduced motion.
- Activity derives weeks from local state. Horizontal week gestures belong to its chart; body-pane navigation remains available outside that chart.
- Circle invitation/join operations have actual progress and pending approval text; busy actions disable controls. Runtime tests exercised real requests and approvals rather than seeded peers.
- Screenshot import shows actual OCR processing/ready/failure state. Processing has an accessible indicator; new image rows use brief content fades. Reduced motion uses a still reading glyph.
- Settings changes are local. Android motion-setting changes are observed live. The glass-like tab bar preserves visible labels; large fonts use vertical icon/label arrangement in a 96dp bar.
- The opt-in timer offers a 15, 30 or 60 minute lead (default 30) before the fixed 10 PM IST reminder. It is separate from next-day 10 AM grace and payment Undo. Scheduled posting remains approximate.

## Verified scope

Earlier emulator work reviewed settled 360dp/200% font layouts, all themes, rocket/confetti frames, reduced-motion navigation and purchase-detail edit/delete Undo. A slowed transition capture showed opaque page travel without outgoing-text bleed. These observations remain tied to their dated builds.

Current finish-slider checks produced zero check-ins for cancellation/early release and one for completion. Two-emulator Circle testing verified totals/check-ins, full-detail records and held-payment privacy through Keep. API 36.1 testing observed native promoted progress. Original circular ₹ progress-head artwork is implemented for both native live-progress cards and onboarding previews; final artwork and selectable timer checks require separate final-build evidence.

## Remaining checks

Do not treat a splash/entrance frame as a settled layout or a still image as proof of frame rate. Final gesture/bar changes need current-build small-screen/large-font/reduced-motion checks. TalkBack, physical-device haptics/FPS/OEM behavior and representative private screenshot OCR flows remain unverified. Startup retry under injected failure and exhaustive Circle action feedback are outside the completed motion pass.
