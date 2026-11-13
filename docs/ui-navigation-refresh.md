# Onboarding, startup and navigation refresh

Updated 2026-11-13 for the 0.3.0 source. Dated checks and final artifact status are in [BUILD-EVIDENCE.md](../BUILD-EVIDENCE.md).

## Onboarding and startup

Five onboarding steps keep tracking optional and show honest payment-source access/readiness. Daily and weekly limits are independent and may be zero. Saved purchases support editing/impulse changes, with deletion separated and reversible through Undo.

StartupSurface reflects actual required local initialization or retryable failure; there is no fixed splash delay or network-reachability gate. Repository/native initialization runs off the main thread and optional startup work is isolated. Retry preserves saved data. Reduced motion uses a static startup glyph.

## Navigation contract

Today → Activity → Circle share one persistent native Compose tab bar outside NavHost. Opaque full-width tab transitions take about 300 ms without alpha/scale; pushed panes use smaller parallax. Onboarding steps do not fade. State is preserved through Back navigation. Android reduced-motion changes are observed live.

The glass-like bar uses tint, a border/highlight, restrained depth and an accent selection capsule; it does not sample the backdrop. Shadows are clipped out of the glass interior. Large fonts use a 96dp bar with vertical icon/label layout; standard size is 68dp. Labels, tab semantics and touch targets remain present.

Body-pane drags follow the finger with resistance, commit at displacement thresholds and spring back when cancelled. Haptics mark threshold/completion feedback. The recognizer respects child consumption, Android system edges, sheets and controls. Activity week changes are scoped to the chart; body swipes outside it navigate top-level panes. Today finish slider keeps priority. Reduced motion removes travel/spring effects while tap alternatives remain usable.

## Evidence and limits

Earlier emulator checks covered onboarding, source permission readiness, purchase edit/impulse/delete Undo, all themes and 360dp/200% font reachability. Slowed animation captures showed opaque page travel and an anchored bar; live reduced-motion changes kept controls usable. These are dated checks, not blanket evidence for every later change.

Current finish-gesture checks recorded no check-in on early release/cancel and exactly one on completed swipe. Two-emulator Circle UI checks exercised create/invite/join/pending/admin approval and actual bidirectional synchronization, including default totals-only privacy, full records and the auto-payment hold/Keep boundary.

A transient adb character-entry/focus interruption occurred during Settings/long code input. In-app clipboard paste successfully entered the full invitation and joined. Normal keyboard behavior needs a dedicated reproduction before calling this an app keyboard defect.

Final latest-build gesture/large-font checks belong in BUILD-EVIDENCE. FPS, TalkBack, injected startup failure, physical-device motion/performance and OEM background behavior remain unverified.

## Timer and live-progress previews

Onboarding previews match the native card roles: a private payment budget update with 1/2/5 minute duration choices (default 2), and an optional reminder countdown with a 15/30/60 minute lead (default 30) to fixed 10 PM IST. Both cards use original circular ₹ progress-head artwork. These local display choices never change the ten-minute Undo/share hold or next-day 10 AM grace. Final selectable-timer/artwork validation remains pending its dated evidence.
