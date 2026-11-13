package com.spendingbuddy.app.ui

import android.content.Context
import com.spendingbuddy.app.R

/** Domain outcomes stay compatible; the UI owns the complete displayed validation copy. */
internal fun Context.finishDayMessage(message: String?): String? = when (message) {
    null -> null
    "Choose a valid date" -> getString(R.string.completion_error_date)
    "You can't finish a day that hasn't happened yet" -> getString(R.string.completion_error_future)
    "This day is already finished" -> getString(R.string.completion_error_done)
    else -> message // Already-localized storage failure from localOperationError.
}
