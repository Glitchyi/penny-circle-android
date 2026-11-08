package com.spendingbuddy.app.p2p

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.spendingbuddy.app.BuddyRepository
import com.spendingbuddy.app.BuddyNotifications
import com.spendingbuddy.app.R

class BuddyLinkService : Service() {
    private val manager by lazy { PeerSyncManager.get(this) }
    override fun onCreate() {
        super.onCreate()
        BuddyNotifications.ensureChannels(this)
        val friend = BuddyRepository.get(this).state.value.peer?.name?.ifBlank { "your friend" } ?: "your friend"
        val notification: Notification = NotificationCompat.Builder(this, BuddyNotifications.LINK_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_buddy)
            .setContentTitle("Reachable for $friend")
            .setContentText("Nudges and updates arrive right away.")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(NotificationCompat.Action.Builder(0, "Turn off", stopIntent()).build())
            .build()
        startForeground(BuddyNotifications.SERVICE_NOTIFICATION_ID, notification)
        manager.setServiceHolder(true)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_STICKY
    }
    override fun onDestroy() {
        manager.setServiceHolder(false)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun stopIntent(): android.app.PendingIntent = android.app.PendingIntent.getService(
        this, 9701, Intent(this, BuddyLinkService::class.java).setAction(ACTION_STOP),
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
    )
    companion object { const val ACTION_STOP = "com.spendingbuddy.app.p2p.STOP_SERVICE" }
}
