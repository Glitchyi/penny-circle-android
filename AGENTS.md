# AGENTS.md — Penny Circle working agreement (formerly Spending Buddy)

> **In-progress work (2026-11-13):** read `HANDOFF-AUTODETECT.md` first. It lists what is built but not yet
> committed, what was and was not tested, and the next steps.

## Product intent

Penny Circle (formerly Spending Buddy; the Kotlin package and applicationId stay `com.spendingbuddy.app` so installs and the signing key keep working) is a private accountability app for a small group of friends (a circle, small by design, default cap 12), created and joined with a shareable group code. It began as a two-friend app; the group model supersedes that. Its job is to make daily expense logging easy, honest, and motivating: record every purchase, explicitly record no spending, finish the day, see daily/weekly limits, and encourage the other members. It is not intended to become a general finance platform or a social network.

The app is native Android in Kotlin and Jetpack Compose. Both users have Android 16-or-later phones, currently a Xiaomi 17 and a OnePlus Nord 6e. Prefer native Android patterns and system integrations over cross-platform substitutes.

## UX preferences

Latest Today direction (2026-11-13, supersedes the earlier horizon/gauge hero): bold spending figures, Monday–Sunday week rings, restrained flat dark panels, hairline borders and mono labels. Automatic payment tracking is the primary logging path once enabled; manual entry remains available as a quieter fallback. Finishing today uses a swipe with a tap/accessibility review alternative. The finish celebration uses a designed rocket with a brief confetti burst; reduced motion uses a still acknowledgement.

Treat the app as a friendly shared school notebook: clear, playful, warm, and interactive without being childish or shame-based. Visual direction (revised 2026-11-10, replaces the light-only decision of 2026-11-09): the "Light Horizon" system in four themes the user picks during onboarding and can change in Settings: Light Horizon (white, hairline borders, violet accent) and three dark themes, Violet Night, Mint Night and Signal Blue. One accent per theme, hairline borders, Geist and Geist Mono type, Swiss-style rules and numbered labels, a centred total with a glowing horizon, hard offset shadows only on pressable buttons. Colour is never the only signal in any theme, and every text pair must pass the contrast tests in ThemeContrastTest and CategoryContrastTest. Colour is never the only signal. Use a clean phone-first layout with a standard Material 3 navigation bar and familiar Android behavior. Today should be the primary surface, with Activity and Buddy as the other top-level destinations. Keep the primary action obvious, use short steps, preserve input when navigating back, and keep success feedback non-blocking.

The rotary telephone interaction is a fun optional feature, not the main interaction or visual identity. The normal keypad must remain the default/faster path. When the dial is used, digits 1–9 are selected by placing a finger in a numbered hole, rotating clockwise to the fixed stop, releasing, and letting the wheel spring back with haptic detents. Tapping the centre enters zero. Always retain an accessible keypad and clear/undo/decimal alternatives.

Use presets and sensible defaults to minimize typing. Purchases need a category and a reason, with presets such as food, transport, shopping, bills, essential, convenience, social, and impulse. A manually entered merchant/reason should be saveable as a personal preset. Impulse must be visually marked in red and also written as text; color must never be the only signal.

Support daily and Monday–Sunday weekly limits, explicit no-spend days, a 10 AM Asia/Kolkata next-day grace window, honest logging, a one-day grace concept where applicable, friendly nudges, and countdown context. Budget suggestions belong to the owner of the limits, who must explicitly accept them. Never shame a user for overspending or late logging; state facts and next steps calmly.

## Privacy and architecture

Use local-first storage and direct peer-to-peer synchronization between members' phones over iroh (a mesh; no member is a server). A group is joined by a shared group code carrying a bootstrap ticket and a secret; members propagate membership to each other. Do not add a hosted backend unless the user explicitly changes the architecture decision. Never embed server credentials, signing passwords, or personal data in source code.

Screenshots and raw OCR text are sensitive and must remain on-device. Google Pay integration means Android share-menu screenshot intake plus on-device OCR and an editable review flow; it is not a direct Google Pay API integration. Multiple screenshots need an Add more/Continue flow. Never silently save OCR candidates, silently remove duplicates, or upload screenshots.

Automatic tracking (decided by the user on 2026-11-13, replaces "never save without review" for this one path): when the user turns it on, payment SMS and payment-app notifications are parsed on the phone and clear shop payments are added on their own, marked "Auto". The message text is never stored or sent. An auto-added purchase can be undone for 10 minutes and is not shared with the circle until that window ends or the user taps Keep. Unclear payments (a person, a new name, cash, no shop name, Rs 20,000 or more) are asked first; refunds, credits, failed payments and OTPs are never added. Screenshot/OCR import keeps its review-first rule. Impulse totals (this week's impulse spend and count, days in a row without one) are shared with the circle as numbers only.

Sync only reviewed records, auto-added records whose undo window has ended, and explicitly supported accountability events. Be honest about peer reachability, Android battery restrictions, approximate WorkManager timing, and notification limits. Do not claim instant push, promoted Live Updates, or guaranteed background delivery unless it has been implemented and verified on real devices.

Keep Android backup/device-transfer exclusions aligned with the privacy model. Keep the release keystore and password outside Git; preserve the same signing key for updates. Do not weaken Play Protect, antivirus, or other Android security controls to make installation easier.

Release signing decision (2026-11-13): the original off-laptop backup was unavailable, and the user explicitly authorized a new production key. The new key is kept outside this repository under `../private-signing/`; preserve it for every subsequent production update. Its certificate differs from earlier debug/original-release builds, so migrating those installs requires a user-approved fresh install. Do not generate further replacement keys automatically.

## Engineering preferences

Prefer small, testable domain functions and explicit state transitions. Preserve existing local data during migrations and reinstalls where possible. Add unit coverage for currency precision, grace deadlines, streak behavior, parser safeguards, sync message validation, event de-duplication, and pairing-secret handling. Re-run core tests, Android tests, lint, release signing, and alignment verification after substantial changes.

When changing navigation or UI, check small phones, dark mode, large font scaling, TalkBack labels, touch target sizes, and reduced-motion settings. Use standard Material components unless there is a clear reason not to. Keep the dial as a contained feature rather than allowing it to dominate every screen.

Before considering work complete, update `TODO.md`, `README.md`, and `BUILD-EVIDENCE.md` when the change affects scope, build commands, privacy, device testing, or known limitations. Keep documentation factual and distinguish implemented behavior from planned behavior.
