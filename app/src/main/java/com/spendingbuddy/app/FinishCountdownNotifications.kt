package com.spendingbuddy.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

/** An optional pre-reminder timer. Late logging remains open through the next-day 10 AM grace. */
object FinishCountdownNotifications {
    private const val CHANNEL = "finish_day_countdown"
    private const val NOTIFICATION_ID = 4301
    private const val PROGRESS_MAX = 1000
    private const val PREFS = "finish_countdown_notifications"
    private const val KEY_SUPPRESSED_DATE = "suppressed_date"
    private const val EXTRA_DATE = "finish_countdown_date"
    private const val ACTION_UNPIN = "com.spendingbuddy.app.finish_countdown.UNPIN"
    private const val ACTION_DISMISSED = "com.spendingbuddy.app.finish_countdown.DISMISSED"
    private const val REQUEST_BASE = 9300

    @SuppressLint("MissingPermission")
    fun refresh(context: Context, state: BuddyState, now: Instant = Instant.now()) {
        val app = context.applicationContext
        val window = activeFinishCountdownWindow(
            now = now,
            enabled = state.finishCountdown && state.reminders && state.onboardingDone,
            completedDays = state.checkIns.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet(),
            leadMinutes = state.finishCountdownLeadMinutes
        )
        if (window == null || !notificationsGranted(app) || isSuppressed(app, window.day)) {
            cancel(app)
            return
        }
        ensureChannel(app)
        val reminderMillis = window.reminderAt.toEpochMilli()
        val progress = finishCountdownProgress(window, now)
        val title = context.getString(R.string.notification_countdown_title, window.day.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()))
        val review = activityIntent(app, Destinations.FINISH_PREFIX + window.day, requestCode(window.day))
        val unpin = broadcastIntent(app, ACTION_UNPIN, window.day)
        val delete = broadcastIntent(app, ACTION_DISMISSED, window.day)
        val body = context.getString(R.string.notification_countdown_body)
        val manager = app.getSystemService(NotificationManager::class.java) ?: return
        val appearance = liveProgressAppearance(state.theme)

        val notification = if (Build.VERSION.SDK_INT >= 36) {
            Notification.Builder(app, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_buddy)
                .setColor(appearance.accent)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(Notification.ProgressStyle()
                    .setProgressSegments(listOf(Notification.ProgressStyle.Segment(PROGRESS_MAX).setColor(appearance.accent)))
                    .setProgress(progress)
                    .setProgressTrackerIcon(rupeeProgressTrackerIcon(appearance)))
                .setWhen(reminderMillis)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .setTimeoutAfter((reminderMillis - now.toEpochMilli()).coerceAtLeast(0L))
                .setContentIntent(review)
                .setDeleteIntent(delete)
                .addExtras(Bundle().apply { putBoolean(EXTRA_REQUEST_PROMOTED, true) })
                .addAction(Notification.Action.Builder(null as Icon?, context.getString(R.string.notification_unpin), unpin).build())
                .build()
        } else {
            NotificationCompat.Builder(app, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_buddy)
                .setColor(appearance.accent)
                .setContentTitle(title)
                .setContentText(body)
                .setProgress(PROGRESS_MAX, progress, false)
                .setWhen(reminderMillis)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setTimeoutAfter((reminderMillis - now.toEpochMilli()).coerceAtLeast(0L))
                .setContentIntent(review)
                .setDeleteIntent(delete)
                .addAction(0, context.getString(R.string.notification_unpin), unpin)
                .build()
        }
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.notification_channel_countdown), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.notification_channel_countdown_description)
            }
        )
    }

    private fun notificationsGranted(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private fun isSuppressed(context: Context, day: LocalDate): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SUPPRESSED_DATE, null) == day.toString()

    private fun suppressAndCancel(context: Context, day: LocalDate) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_SUPPRESSED_DATE, day.toString()).apply()
        cancel(context)
    }

    private fun requestCode(day: LocalDate): Int = REQUEST_BASE + (day.toString().hashCode().and(0x7FFF))

    private fun broadcastIntent(context: Context, action: String, day: LocalDate): PendingIntent = PendingIntent.getBroadcast(
        context, requestCode(day) + if (action == ACTION_UNPIN) 0x10000 else 0x20000,
        Intent(context, FinishCountdownActionReceiver::class.java).setAction(action).putExtra(EXTRA_DATE, day.toString()),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun activityIntent(context: Context, destination: String, requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = "com.spendingbuddy.app.OPEN_FINISH.$destination"
            putExtra(Destinations.EXTRA, destination)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    internal fun onAction(context: Context, intent: Intent?) {
        val date = runCatching { LocalDate.parse(intent?.getStringExtra(EXTRA_DATE)) }.getOrNull() ?: return
        when (intent?.action) {
            ACTION_UNPIN, ACTION_DISMISSED -> suppressAndCancel(context.applicationContext, date)
        }
    }

    private const val EXTRA_REQUEST_PROMOTED = "android.requestPromotedOngoing"
}

class FinishCountdownActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) = FinishCountdownNotifications.onAction(context, intent)
}
