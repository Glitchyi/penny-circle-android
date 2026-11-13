package com.spendingbuddy.app.detect

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.spendingbuddy.app.AUTO_HOLD_MS
import com.spendingbuddy.app.BuddyRepository
import java.util.concurrent.TimeUnit

/**
 * Ends the undo window of auto-added purchases, which lets the next circle sync include them. WorkManager timing
 * is approximate: Android may run this late, and a purchase is never shared before its window has ended.
 */
class HoldReleaseWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        BuddyRepository.get(applicationContext).releaseHolds()
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = OneTimeWorkRequest.Builder(HoldReleaseWorker::class.java)
                .setInitialDelay(AUTO_HOLD_MS + 5_000L, TimeUnit.MILLISECONDS)
                .build()
            // One request per detection, so an earlier purchase is not kept waiting by a later one.
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork("hold_release_${System.currentTimeMillis()}", ExistingWorkPolicy.KEEP, request)
        }
    }
}
