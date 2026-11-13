package com.spendingbuddy.app

/** Extras do not distinguish PendingIntents. Keep every destination's identity independent. */
internal fun notificationActivityAction(destination: String): String =
    "com.spendingbuddy.app.OPEN.$destination"

/** Select the tab before opening a notification's sheet, including in an already-running app. */
internal fun notificationTopLevelRoute(destination: String?): String? = when (destination) {
    Destinations.TODAY, Destinations.ADD -> Destinations.TODAY
    Destinations.ACTIVITY -> Destinations.ACTIVITY
    Destinations.CIRCLE, Destinations.BUDDY -> Destinations.CIRCLE
    else -> null
}
