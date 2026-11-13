package com.spendingbuddy.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal enum class RecoveryKind { Restore, FreshStart }
internal data class RecoveryOperation(val generation: Long, val kind: RecoveryKind)

/** One recovery owns the file until its IO finishes; delayed cleanup cannot release its successor. */
internal class RecoveryOperationGate {
    private var generation = 0L
    private val operation0 = MutableStateFlow<RecoveryOperation?>(null)
    val operation: StateFlow<RecoveryOperation?> get() = operation0

    @Synchronized fun begin(kind: RecoveryKind): RecoveryOperation? {
        if (operation0.value != null) return null
        return RecoveryOperation(++generation, kind).also { operation0.value = it }
    }

    @Synchronized fun requireCurrent(operation: RecoveryOperation) {
        check(operation0.value == operation) { "Recovery operation is no longer current" }
    }

    @Synchronized fun finish(operation: RecoveryOperation) {
        if (operation0.value == operation) operation0.value = null
    }
}
