# Android Live Update validation (2026-11-13)

## Platform requirements

Android's official Live Update guide lists supported notification styles (`ProgressStyle` included), the manifest permission `POST_PROMOTED_NOTIFICATIONS`, a promotion request, an ongoing notification with a title, and several exclusions such as custom views, group summaries, colorized notifications, and minimum-importance channels. The system may apply additional OEM criteria. [Create Live Update notifications](https://developer.android.com/develop/ui/views/notifications/live-update)

`Notification.ProgressStyle` exists from API 36. The explicit `EXTRA_REQUEST_PROMOTED_ONGOING` field and `Notification.Builder.setRequestPromotedOngoing` are documented from version 36.1. `hasPromotableCharacteristics()` checks technical eligibility; `NotificationManager.canPostPromotedNotifications()` checks whether promotion is allowed by user/app settings; `FLAG_PROMOTED_ONGOING` is the system's indication that the notification was promoted. [Notification API](https://developer.android.com/reference/android/app/Notification), [Notification.Builder API](https://developer.android.com/reference/android/app/Notification.Builder), [ProgressStyle API](https://developer.android.com/reference/android/app/Notification.ProgressStyle)

The Android guide describes Live Updates as ongoing, user-initiated, and time-sensitive activities, and advises against using them for past events or ambient information. Whether Penny Circle's brief auto-add/undo interval fits that product guidance is a judgment call; technical promotion does not establish policy suitability. [Usage criteria](https://developer.android.com/develop/ui/views/notifications/live-update#usage-criteria)

## Emulator run

- Device: `emulator-5556`, Android 16 QPR1, API 36.1, build `google/sdk_gphone64_x86_64/emu64xa:16/BE4B.251210.005/14574095:userdebug/dev-keys`.
- App: Penny Circle `0.3.0`, version code 3, debug APK SHA-256 `E958B3DDC17B24DED12AC99EE5C13C6FFE401FA3DF7FA61E5B1451DBDD562CA0`.
- Only synthetic data was used: a fresh profile called `Synthetic` and one synthetic completed-payment SMS. No real messages or phone data were involved.
- `POST_NOTIFICATIONS` was granted through Android's runtime prompt. The app's setup screen reported Live Updates `Optional · Allowed`.
- `dumpsys notification` reported `ONGOING_EVENT|ONLY_ALERT_ONCE|PROMOTED_ONGOING`, `android.styledByProgress=true`, progress `480/1000`, `android.requestPromotedOngoing=true`, a non-minimum importance channel, and the Undo/Edit/Keep actions.
- The expanded Android notification shade visibly showed the native progress card and actions. On the home screen, the status bar showed the Penny Circle monochrome notification icon; no readable critical-text chip was visible in the capture. The system's promoted flag is positive evidence of promotion, but the chip text presentation was not verified visually.
- Synthetic screenshots and the filtered `dumpsys` excerpt are in the ignored `captures/production-readiness-2026-11-13/` directory.

## Final 0.3.0 follow-up

Theme-aware debug SHA-256: `4F4B405541CB3A0EA40FBA6C969813A99581246B9AD77B00514C3FE66333D6A0`. The signed ARM64 distribution is separately identified in `BUILD-EVIDENCE.md`.

- The latest payment card uses a circular ₹ tracker at the current progress position, with the selected theme's exact accent and readable on-accent glyph. The onboarding samples use the same original geometry. Android supports this overlay through [setProgressTrackerIcon](https://developer.android.com/reference/android/app/Notification.ProgressStyle#setProgressTrackerIcon(android.graphics.drawable.Icon)).
- Actual synthetic SMS tests showed native Mint Night (`#00E599`) and Signal Blue (`#4D9BFF`) cards. Changing the theme while the card was active changed its coin/bar and retained promotion. Timeout decreased from 5 minutes to 3m50.447s instead of restarting; the reconstructed expiry matched within approximately 10 ms.
- Settings offer payment durations 1/2/5 minutes and reminder leads 15/30/60 minutes. A one-minute payment selection and 15-minute lead survived app restart; the one-minute native card showed `timeout=PT1M`. Its purchase hold was approximately 599,970 ms from notification posting, so notification display does not shorten the ten-minute privacy hold. Tests cover all preset durations and reminder boundaries.
- Earlier default-timer checks established actual two-minute card expiry while the purchase remained held. Undo of an older held purchase refreshed the surviving budget without extending its deadline. The earlier 9:31 PM reminder was promoted, its review link targeted the right day, completion/disabled reminders cancelled it, and Unpin suppression survived restart.
- Native timer posting remains approximate WorkManager work. The reminder stays at 10 PM IST, with configurable lead time; next-day 10 AM grace remains separate. A real-clock 60-minute attempt first honored an existing same-day dismissal marker. For a controlled synthetic test, the app-local suppression XML was snapshotted, only its dismissal marker temporarily cleared, and the exact XML restored afterward. During 9:33–9:34 PM IST, the native Signal Blue timer showed the matching ₹ coin/bar and 25:33 remaining to 10 PM. Dumpsys confirmed `when=1794587400000`, countdown chronometer and `PROMOTED_ONGOING`. Disabling the timer cancelled it; original Light Horizon/payment2/lead30/reminders-off/countdown-off/two-expense circle state was restored.
- Synthetic expenses were undone and the original two-expense paired profile/settings restored after the payment/theme tests. No physical phone was involved; the newest payment/theme run changed no clock, display or Android permission settings.

## Limits

This verifies technical promotion on one API 36.1 Google APIs emulator only. It does not establish behavior on API 36.0, Xiaomi HyperOS, OnePlus OxygenOS, other OEM eligibility rules, or a real device. The test does not establish whether auto-adding a completed purchase meets Android's intended Live Update usage criteria. No Android promotion permission or eligibility check was bypassed; controlled app-local fixtures were restored.
