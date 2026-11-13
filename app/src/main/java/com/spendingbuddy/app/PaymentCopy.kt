package com.spendingbuddy.app

import android.content.Context

/** Existing local detection reasons become app copy at the Android presentation boundary. */
internal fun Context.paymentReviewReason(reason: String): String = when (reason) {
    "Cash withdrawal" -> getString(R.string.payment_reason_cash)
    "No shop name in the message" -> getString(R.string.payment_reason_no_shop)
    "Large amount" -> getString(R.string.payment_reason_large)
    "Looks like a payment to a person" -> getString(R.string.payment_reason_person)
    "Bank transfer. Check what this was for" -> getString(R.string.payment_reason_transfer)
    "New name. Add it once and it will be automatic next time" -> getString(R.string.payment_reason_new_name)
    else -> reason // Keep an older on-device draft's explanation intact.
}
