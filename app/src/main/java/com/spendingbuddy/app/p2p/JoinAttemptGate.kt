package com.spendingbuddy.app.p2p

/** Serializes cancellation with accepting a join reply; a late reply cannot revive a cancelled request. */
internal class JoinAttemptGate {
    private var generation = 0L

    @Synchronized fun begin(replace: () -> Unit = {}): Long {
        generation++
        replace()
        return generation
    }
    @Synchronized fun current(): Long = generation
    @Synchronized fun cancel(clear: () -> Unit) { generation++; clear() }

    @Synchronized fun <T> ifCurrent(attempt: Long, block: () -> T): T? =
        if (attempt == generation) block() else null
}
