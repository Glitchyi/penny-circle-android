# Spending Buddy native implementation

The current objective and actionable pending work are maintained in [TODO.md](TODO.md). Agent-specific product and engineering preferences are in [AGENTS.md](AGENTS.md). This file records the durable implementation direction; `TODO.md` records release status and follow-up work.

Implement the user's approved product brief in /home/ubuntu/spending-buddy/product-brief.md as a Kotlin/Jetpack Compose Android app, not an Expo app. Deliver a real signed APK and native source. The product objective is to help two friends log every purchase or an explicit no-spend day, see daily/weekly limits, finish honestly, and encourage one another in a friendly notebook-like experience. The following defaults are privacy-preserving and editable: on-device screenshot OCR; personal custom presets; category and reason for each purchase; honest-logging streaks, with no budget/nudge penalty; Monday–Sunday budgets; 10 AM Asia/Kolkata next-day grace; owner approval of budget suggestions.

## Native structure

The Gradle root pins AGP 8.13.0, Gradle 8.13 and Kotlin/Compose 2.2.10. `app/` contains manifest/resources and the Android entry point; `core/` contains portable domain and sync protocol code. Models.kt defines local domain records and strict rupee/paise and streak rules. BuddyRepository.kt stores confirmed records and import drafts privately. The `ui/` package contains the phone-first screens. RotaryDial.kt owns the optional pointer gestures, fixed stop, spring return, and haptics. ScreenshotImporter.kt extracts local image text with a bundled ML Kit model, creates editable candidates, handles multiple screenshots and possible duplicates. The peer-sync package, BuddyLinkService, and workers handle direct iroh communication and approximate notifications. MainActivity.kt handles Android shared-image intents and app entry. Unit tests cover domain, protocol, transport, and parser safeguards.

## Peer-to-peer boundary

Use iroh QUIC for direct phone-to-phone synchronization. No hosted backend is required for the current architecture. Screenshots never leave the device. Do not embed server credentials in the APK. If the peer is unreachable, local logging remains functional and Buddy explicitly shows the real connection state, never a fake partner or a success message. Background WorkManager delivery is approximate and is not instant remote push; disclose any gap.

## Design

Use the requested playful, tactile rotary-phone interaction with forest-green, warm-cream and bright lime accents. Red is reserved for impulse labels and overspending alerts, always paired with text. Large rounded cards, a bold amount above the wheel, friendly short copy and native system sans typography make one-handed use comfortable. The dial is the signature, not an illustration of a working control. Nonzero holes track clockwise movement to a stop; early release commits nothing; valid release commits one digit and springs home. Zero is a central tap. Include undo, clear, normal keypad, paise entry and accessible alternative digit controls. Native haptic ticks respect the user's toggle and system settings. Avoid clutter and shame-based messages.

## Delivery constraints

Use the existing private release key for compatible updates, preserve it outside GitHub, verify the APK with apksigner, and disclose that a valid signature does not establish Play Store trust or remove sideload warnings. Never disable Play Protect, antivirus or other security controls. Physical-phone notification, haptic, OCR, and peer-sync behavior remain pending until the users try the build on both phones; see [TODO.md](TODO.md).
