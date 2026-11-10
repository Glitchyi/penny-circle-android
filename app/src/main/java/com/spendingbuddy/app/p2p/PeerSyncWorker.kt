package com.spendingbuddy.app.p2p

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class PeerSyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val manager = PeerSyncManager.get(applicationContext)
        manager.setWorkerHolder(true)
        return try {
            // Also resumes a pending join: with no circle yet, one sync pass re-sends the join request.
            manager.ensureIdentity()
            manager.syncForWorker()
            Result.success()
        } finally {
            manager.setWorkerHolder(false)
        }
    }
    companion object {
        private const val NAME = "buddy_peer_sync"
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<PeerSyncWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            )
        }
    }
}

class BuddyBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext
        PeerSyncWorker.schedule(app)
        com.spendingbuddy.app.BuddyNotifications.schedule(app, com.spendingbuddy.app.BuddyRepository.get(app).state.value.reminders)
        if (com.spendingbuddy.app.BuddyRepository.get(app).state.value.stayReachable) {
            runCatching { androidx.core.content.ContextCompat.startForegroundService(app, Intent(app, BuddyLinkService::class.java)) }
        }
    }
}
