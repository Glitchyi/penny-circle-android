package com.spendingbuddy.app

/**
 * Intent extra used by notifications (and the share sheet) to open a specific place in the app.
 * MainActivity reads [EXTRA] from its launch/new intent and navigates accordingly.
 */
object Destinations {
    const val EXTRA = "com.spendingbuddy.app.DESTINATION"
    const val TODAY = "today"
    /** Opens the Add purchase sheet on Today. */
    const val ADD = "add"
    const val ACTIVITY = "activity"
    const val CIRCLE = "circle"
    /** Old name of the Circle tab, kept so notifications created before the rename still open it. */
    const val BUDDY = "buddy"
    /** Opens the screenshot import review flow. */
    const val IMPORT = "import"
    const val SETTINGS = "settings"
    /** Opens the automatic tracking settings. */
    const val AUTO_DETECT = "auto_detect"
    /** Followed by a purchase id: opens that purchase in the edit sheet. */
    const val EDIT_PREFIX = "edit:"
    /** Followed by an ISO date: opens that day's finish review sheet. */
    const val FINISH_PREFIX = "finish:"
}
