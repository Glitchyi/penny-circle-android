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

## Limits

This verifies technical promotion on one API 36.1 Google APIs emulator only. It does not establish behavior on API 36.0, Xiaomi HyperOS, OnePlus OxygenOS, other OEM eligibility rules, or a real device. The test does not establish whether auto-adding a completed purchase meets Android's intended Live Update usage criteria. No platform or user setting was bypassed to force promotion.
