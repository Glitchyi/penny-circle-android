# Spending Buddy native implementation

Implement the user's approved product brief in /home/ubuntu/spending-buddy/product-brief.md as a Kotlin/Jetpack Compose Android app, not an Expo app. Deliver a real signed APK and native source. The following undecided settings use privacy-preserving, editable defaults: on-device screenshot OCR; personal custom presets; category and reason for each purchase; honest-logging streaks, with no budget/nudge penalty; Monday–Sunday budgets; 10 AM Asia/Kolkata next-day grace; owner approval of budget suggestions.

## Native structure

The Gradle root pins AGP 8.13.0, Gradle 8.13 and Kotlin/Compose 2.2.10. app/ contains manifest/resources and com.spendingbuddy.app Kotlin files. Models.kt defines local domain records and strict rupee/paise and streak rules. BuddyRepository.kt stores confirmed records and import drafts privately, exposes observable state and optional authenticated sync. Screens.kt contains the phone-first UI. RotaryDial.kt owns pointer gestures, fixed stop, spring return and haptics. ScreenshotImporter.kt extracts local image text with a bundled ML Kit model, creates editable candidates, handles multiple screenshots and possible duplicates. ReminderWorker.kt handles approximate background reminders/sync and standard notifications. MainActivity.kt handles Android shared-image intents and app entry. Unit tests cover domain rules and parser safeguards.

## Backend boundary

Deploy an authenticated Manus-hosted sync API only if the user approves the pending request. Screenshots never go to it. Do not embed private server credentials in the APK. If a deployed endpoint is unavailable, local logging remains functional and Friend explicitly shows that synchronization needs setup, never a fake partner or a success message. Background WorkManager delivery is approximate and is not instant remote push. Instant push needs a configured provider and native credentials; disclose any gap.

## Design

Use the requested playful, tactile rotary-phone interaction with forest-green, warm-cream and bright lime accents. Red is reserved for impulse labels and overspending alerts, always paired with text. Large rounded cards, a bold amount above the wheel, friendly short copy and native system sans typography make one-handed use comfortable. The dial is the signature, not an illustration of a working control. Nonzero holes track clockwise movement to a stop; early release commits nothing; valid release commits one digit and springs home. Zero is a central tap. Include undo, clear, normal keypad, paise entry and accessible alternative digit controls. Native haptic ticks respect the user's toggle and system settings. Avoid clutter and shame-based messages.

## Delivery constraints

Use a newly generated release key, preserve it privately for updates, verify the APK with apksigner, and disclose that a valid signature does not establish Play Store trust or remove sideload warnings. Never disable Play Protect, antivirus or other security controls. Physical-phone notification/haptic behavior remains unverified until the users try the build.
