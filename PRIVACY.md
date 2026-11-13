# Penny Circle privacy

Updated 13 November 2026. This describes the Android app in this repository; it is not a promise about Android, a phone vendor, or another circle member's device.

## What stays on your phone

Purchases, limits, check-ins and preferences are stored in Android's private app storage. Payment tracking is optional. If enabled, supported incoming payment SMS and payment-app notifications are parsed on the phone; the original message text is not saved or sent. There is no scan of an old SMS inbox. Parsed payment details and a local duplicate-check reference can be retained.

Shared payment screenshots are processed on-device and shown for review. Screenshot files and raw OCR drafts are not included in Circle synchronization. Imports require your review; they are not silently added. You can also use the app entirely through manual entry.

An automatically added purchase can be undone in Today for ten minutes. During that hold, the purchase and any reopened check-in stay private to this phone. Tapping Keep, or the hold ending, makes it eligible for Circle synchronization. The budget notification defaults to two minutes and can be set to one, two or five minutes. This separate display period does not shorten the ten-minute hold. These timer preferences are stored locally and are not shared with your circle.

## What you share with a circle

Joining uses an invitation code and the admin's approval. The code includes access information: share it only with people you intend to invite. The default visibility is **Totals & status**. **Full details** additionally shares eligible purchase records. Impulse summaries are shared as counts/totals. Circle synchronization never sends screenshots, raw OCR text or raw payment messages.

Connections use authenticated, encrypted iroh transport between phones. Transport may pass through encrypted relays, and transport providers can observe connection metadata. Penny Circle does not require its own hosted account or spending database. Reachability and background delivery depend on both phones, Android restrictions and network availability.

## Google libraries

Screenshot recognition uses bundled, on-device ML Kit. Google's terms describe processing inputs and outputs on-device, while allowing performance/utilization metrics and network requests for updates or compatibility. The Google code-scanner and Play services components also have their own terms. These components are not described as telemetry-free. See [ML Kit terms](https://developers.google.com/ml-kit/terms), [Google APIs terms](https://developers.google.com/terms), and the [third-party notices](docs/third-party-notices.md).

The app includes the applicable notices for offline reading in **Settings → Privacy and licenses**.

## Notifications and deletion

Notifications can show spending amounts on the device's notification surfaces. Android notification and lock-screen settings control their visibility. Android also decides whether a requested Live Update is promoted. The optional reminder timer can start 15, 30 or 60 minutes before the fixed 10 PM IST reminder. Its lead-time preference stays on this phone. Late logging remains possible through the separate next-day 10 AM grace window.

There is no automatic cloud backup or Android device transfer of the app's private data. A backup you explicitly export is a separate local file that you control. Uninstalling or clearing storage deletes this installation's data; it does not erase exported backups or copies already received by other circle members. Keep backups and invitation codes private.
