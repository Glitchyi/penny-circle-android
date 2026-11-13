package com.spendingbuddy.app

import android.content.Context

/**
 * Tiny synchronous copy of the chosen theme id. The full app state loads asynchronously, but the
 * window background and system bar style must be right on the very first frame, so [MainActivity]
 * reads this in onCreate. The app state stays the source of truth; the UI keeps this copy in sync.
 */
object ThemeStore {
    private const val FILE = "penny_ui"
    private const val KEY = "theme"
    const val DEFAULT_ID = "light_horizon"

    fun load(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null) ?: DEFAULT_ID

    fun save(context: Context, id: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, id).apply()
    }
}
