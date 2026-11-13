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
        val circle = BuddyRepository.get(this).state.value.group?.name?.ifBlank { getString(R.string.notification_link_circle_fallback) } ?: getString(R.string.notification_link_circle_fallback)
        val notification: Notification = NotificationCompat.Builder(this, BuddyNotifications.LINK_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_buddy)
            .setContentTitle(getString(R.string.notification_link_title, circle))
            .setContentText(getString(R.string.notification_link_body))
            .setStyle(NotificationCompat.BigTextStyle())
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(NotificationCompat.Action.Builder(0, getString(R.string.notification_turn_off), stopIntent()).build())
            .build()
        startForeground(BuddyNotifications.SERVICE_NOTIFICATION_ID, notification)
        manager.setServiceHolder(true)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { manager.setStayReachable(false); stopSelf() }
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
