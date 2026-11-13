package com.spendingbuddy.app.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Domain validation stays visible in the active form; unexpected storage failures keep a retry path. */
internal suspend fun localOperationError(failureMessage: String, operation: suspend () -> String?): String? = try {
    withContext(Dispatchers.IO) { operation() }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    failureMessage
}
