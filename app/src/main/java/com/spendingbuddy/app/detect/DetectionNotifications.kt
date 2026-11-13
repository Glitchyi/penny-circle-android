package com.spendingbuddy.app.detect

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
import com.spendingbuddy.app.BuddyRepository
import com.spendingbuddy.app.BuddyState
import com.spendingbuddy.app.Destinations
import com.spendingbuddy.app.Expense
import com.spendingbuddy.app.paymentUpdateDurationMillis
import com.spendingbuddy.app.liveProgressAppearance
import com.spendingbuddy.app.rupeeProgressTrackerIcon
import com.spendingbuddy.app.MainActivity
import com.spendingbuddy.app.PendingDetection
import com.spendingbuddy.app.R
import com.spendingbuddy.app.rupees
import com.spendingbuddy.app.spentOn
import com.spendingbuddy.app.todayIndia
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Notifications for detected purchases. On Android 16 an added purchase is posted as a Live Update: an ongoing
 * progress notification showing the day's budget, which the system may show on the lock screen and as a status
 * bar chip. Whether it is promoted is the system's and the user's choice; this code only asks.
 */
object DetectionNotifications {
    private const val CHANNEL = "detected_purchases"
    private const val ADDED_ID = 4201
    private const val LATEST_ADDED_TAG = "latest_budget_update"
    private const val ASK_ID = 4202
    private const val PROGRESS_MAX = 1000
    internal const val ADDED_NOTIFICATION_DURATION_MS = 2 * 60_000L
    private const val ADDED_NOTIFICATION_PREFS = "latest_added_notification"
    private const val KEY_LATEST_EXPENSE_ID = "expense_id"
    private const val KEY_LATEST_UPDATED_AT = "updated_at"
    private val addedNotificationLock = Any()

    internal const val ACTION_UNDO = "com.spendingbuddy.app.detect.UNDO"
    internal const val ACTION_KEEP = "com.spendingbuddy.app.detect.KEEP"
    internal const val ACTION_ADD = "com.spendingbuddy.app.detect.ADD"
    internal const val ACTION_SKIP = "com.spendingbuddy.app.detect.SKIP"
    internal const val EXTRA_ID = "id"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Detected purchases", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Purchases found in payment messages, with your budget for the day"
            }
        )
    }

    /** True when Android will show this app's Live Updates in the status bar and on the lock screen. */
    fun liveUpdatesAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT >= 36 && runCatching { context.getSystemService(NotificationManager::class.java).canPostPromotedNotifications() }.getOrDefault(false)

    private fun granted(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Budget facts for the notification, worked out once so both Android versions say the same thing. */
    private class Budget(state: BuddyState) {
        val spent = spentOn(state.expenses, todayIndia())
        val limit = state.dailyLimitPaise
        val over = spent > limit
        val headline = if (over) "${rupees(spent - limit)} over today's limit" else "${rupees(limit - spent)} left today"
        val chip = budgetCriticalText(spent, limit)
        val detail = "${rupees(spent.coerceAtLeast(0))} of ${rupees(limit)} spent"
        val progress = budgetProgress(spent, limit)
    }

    @SuppressLint("MissingPermission")
    fun showAdded(context: Context, state: BuddyState, expense: Expense) {
        val app = context.applicationContext
        if (!granted(app)) {
            cancelAllAdded(app)
            return
        }
        ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        synchronized(addedNotificationLock) {
            val prefs = app.getSharedPreferences(ADDED_NOTIFICATION_PREFS, Context.MODE_PRIVATE)
            val previousUpdatedAt = prefs.getLong(KEY_LATEST_UPDATED_AT, Long.MIN_VALUE)
            if (expense.updatedAt < previousUpdatedAt) return
            clearLegacyAddedNotifications(manager)
            prefs.edit()
                .putString(KEY_LATEST_EXPENSE_ID, expense.id)
                .putLong(KEY_LATEST_UPDATED_AT, expense.updatedAt)
                .apply()
            manager.notify(LATEST_ADDED_TAG, ADDED_ID, buildAddedNotification(context, state, expense, paymentUpdateDurationMillis(state.paymentUpdateMinutes)))
        }
    }

    /** Refresh an active card's budget after a different expense changes, without extending its original lifetime. */
    @SuppressLint("MissingPermission")
    fun refreshAdded(context: Context, state: BuddyState) {
        val app = context.applicationContext
        if (!granted(app)) {
            cancelAllAdded(app)
            return
        }
        val manager = app.getSystemService(NotificationManager::class.java) ?: return
        synchronized(addedNotificationLock) {
            val prefs = app.getSharedPreferences(ADDED_NOTIFICATION_PREFS, Context.MODE_PRIVATE)
            val latestId = prefs.getString(KEY_LATEST_EXPENSE_ID, null) ?: return
            val active = runCatching {
                manager.activeNotifications.firstOrNull { it.tag == LATEST_ADDED_TAG && it.id == ADDED_ID }
            }.getOrNull() ?: return
            val old = active.notification
            val deadline = active.postTime + old.timeoutAfter
            val remaining = deadline - System.currentTimeMillis()
            if (old.timeoutAfter <= 0L || remaining <= 0L) {
                manager.cancel(LATEST_ADDED_TAG, ADDED_ID)
                prefs.edit().remove(KEY_LATEST_EXPENSE_ID).remove(KEY_LATEST_UPDATED_AT).apply()
                return
            }
            val expense = state.expenses.firstOrNull { it.id == latestId } ?: run {
                manager.cancel(LATEST_ADDED_TAG, ADDED_ID)
                prefs.edit().remove(KEY_LATEST_EXPENSE_ID).remove(KEY_LATEST_UPDATED_AT).apply()
                return
            }
            ensureChannel(app)
            manager.notify(LATEST_ADDED_TAG, ADDED_ID, buildAddedNotification(app, state, expense, remaining))
        }
    }

    private fun buildAddedNotification(context: Context, state: BuddyState, expense: Expense, timeoutAfterMs: Long): Notification {
        val budget = Budget(state)
        val appearance = liveProgressAppearance(state.theme)
        val what = listOf(rupees(expense.amountPaise), expense.merchant.ifBlank { expense.category }).joinToString(" · ")
        val body = "Added $what. ${budget.detail}."
        val held = expense.heldUntil > System.currentTimeMillis()
        val undo = broadcast(context, ACTION_UNDO, expense.id)
        val keep = broadcast(context, ACTION_KEEP, expense.id)
        val edit = activity(context, Destinations.EDIT_PREFIX + expense.id, expense.id.hashCode())
        val open = activity(context, Destinations.TODAY, 9101)
        return if (Build.VERSION.SDK_INT >= 36) {
            val color = if (budget.over) appearance.danger else appearance.accent
            val style = Notification.ProgressStyle()
                .setProgressSegments(listOf(Notification.ProgressStyle.Segment(PROGRESS_MAX).setColor(color)))
                .setProgress(budget.progress)
                .setProgressTrackerIcon(rupeeProgressTrackerIcon(appearance))
            Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_buddy)
                .setColor(appearance.accent)
                .setContentTitle(budget.headline)
                .setContentText(body)
                .setStyle(style)
                .setShortCriticalText(budget.chip)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .setTimeoutAfter(timeoutAfterMs)
                .setContentIntent(open)
                .addExtras(Bundle().apply { putBoolean(EXTRA_REQUEST_PROMOTED, true) })
                .apply { if (held) addAction(Notification.Action.Builder(null as Icon?, "Undo", undo).build()) }
                .addAction(Notification.Action.Builder(null as Icon?, "Edit", edit).build())
                .apply { if (held) addAction(Notification.Action.Builder(null as Icon?, "Keep", keep).build()) }
                .build()
        } else {
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_buddy)
                .setColor(appearance.accent)
                .setContentTitle(budget.headline)
                .setContentText(body)
                .setProgress(PROGRESS_MAX, budget.progress, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setTimeoutAfter(timeoutAfterMs)
                .setContentIntent(open)
                .apply { if (held) addAction(0, "Undo", undo) }
                .addAction(0, "Edit", edit)
                .apply { if (held) addAction(0, "Keep", keep) }
                .build()
        }
    }

    @SuppressLint("MissingPermission")
    fun showAsk(context: Context, pending: PendingDetection) {
        if (!granted(context)) return
        ensureChannel(context)
        val who = pending.merchant.ifBlank { "a payment" }
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_buddy)
            .setContentTitle("${rupees(pending.amountPaise)} · $who. Add it?")
            .setContentText("${pending.why}. Nothing is saved until you choose.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${pending.why}. Nothing is saved until you choose."))
            .setAutoCancel(true)
            .setContentIntent(activity(context, Destinations.TODAY, 9101))
            .addAction(0, "Add", broadcast(context, ACTION_ADD, pending.id))
            .addAction(0, "Skip", broadcast(context, ACTION_SKIP, pending.id))
            .build()
        NotificationManagerCompat.from(context).notify(pending.id, ASK_ID, notification)
    }

    fun cancelAdded(context: Context, expenseId: String) {
        val app = context.applicationContext
        val manager = app.getSystemService(NotificationManager::class.java) ?: return
        synchronized(addedNotificationLock) {
            // Remove a per-expense notification posted by an older app version, if one is still active.
            manager.cancel(expenseId, ADDED_ID)
            val prefs = app.getSharedPreferences(ADDED_NOTIFICATION_PREFS, Context.MODE_PRIVATE)
            if (!isCurrentAddedNotification(prefs.getString(KEY_LATEST_EXPENSE_ID, null), expenseId)) return
            manager.cancel(LATEST_ADDED_TAG, ADDED_ID)
            prefs.edit().remove(KEY_LATEST_EXPENSE_ID).remove(KEY_LATEST_UPDATED_AT).apply()
        }
    }

    private fun cancelAllAdded(context: Context) {
        val app = context.applicationContext
        val manager = app.getSystemService(NotificationManager::class.java) ?: return
        synchronized(addedNotificationLock) {
            manager.cancel(LATEST_ADDED_TAG, ADDED_ID)
            clearLegacyAddedNotifications(manager)
            app.getSharedPreferences(ADDED_NOTIFICATION_PREFS, Context.MODE_PRIVATE).edit()
                .remove(KEY_LATEST_EXPENSE_ID).remove(KEY_LATEST_UPDATED_AT).apply()
        }
    }

    private fun clearLegacyAddedNotifications(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        runCatching {
            manager.activeNotifications
                .filter { it.id == ADDED_ID && it.tag != LATEST_ADDED_TAG }
                .forEach { manager.cancel(it.tag, it.id) }
        }
    }
    fun cancelAsk(context: Context, pendingId: String) = NotificationManagerCompat.from(context).cancel(pendingId, ASK_ID)

    private fun broadcast(context: Context, action: String, id: String): PendingIntent = PendingIntent.getBroadcast(
        context, id.hashCode(),
        Intent(context, DetectionActionReceiver::class.java).setAction(action).putExtra(EXTRA_ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun activity(context: Context, destination: String, requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context, requestCode,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // A distinct action keeps this intent separate from the other notification intents.
            action = "com.spendingbuddy.app.OPEN.$destination"
            putExtra(Destinations.EXTRA, destination)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Notification.EXTRA_REQUEST_PROMOTED_ONGOING (API 36): asks the system to show this as a Live Update. */
    private const val EXTRA_REQUEST_PROMOTED = "android.requestPromotedOngoing"
}

/** The latest shared notification is the only one cancelled by an expense action. */
internal fun isCurrentAddedNotification(latestExpenseId: String?, requestedExpenseId: String): Boolean =
    latestExpenseId == requestedExpenseId

/** Keep status-chip critical text compact while the notification title carries the exact rupee amount. */
internal fun budgetCriticalText(spentPaise: Long, limitPaise: Long): String = when {
    limitPaise <= 0L && spentPaise > 0L -> "Over"
    limitPaise <= 0L -> "₹0"
    spentPaise > limitPaise -> "Over"
    else -> {
        val percentage = BigDecimal.valueOf(spentPaise.coerceAtLeast(0L))
            .multiply(BigDecimal.valueOf(100L))
            .divide(BigDecimal.valueOf(limitPaise), 0, RoundingMode.DOWN)
            .toInt().coerceIn(0, 100)
        "$percentage%"
    }
}

private fun budgetProgress(spentPaise: Long, limitPaise: Long): Int {
    if (limitPaise <= 0L) return if (spentPaise > 0L) 1000 else 0
    if (spentPaise >= limitPaise) return 1000
    if (spentPaise <= 0L) return 0
    return BigDecimal.valueOf(spentPaise)
        .multiply(BigDecimal.valueOf(1000L))
        .divide(BigDecimal.valueOf(limitPaise), 0, RoundingMode.DOWN)
        .toInt().coerceIn(0, 999)
}

/** Handles the buttons on detection notifications. Not exported: only this app's own notifications can reach it. */
class DetectionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val id = intent?.getStringExtra(DetectionNotifications.EXTRA_ID) ?: return
        val app = context.applicationContext
        val repo = BuddyRepository.get(app)
        when (intent.action) {
            DetectionNotifications.ACTION_UNDO -> { repo.undoAuto(id); DetectionNotifications.cancelAdded(app, id) }
            DetectionNotifications.ACTION_KEEP -> { repo.keepAuto(id); DetectionNotifications.cancelAdded(app, id) }
            DetectionNotifications.ACTION_SKIP -> { repo.skipPending(id); DetectionNotifications.cancelAsk(app, id) }
            DetectionNotifications.ACTION_ADD -> {
                val added = repo.addPending(id)
                DetectionNotifications.cancelAsk(app, id)
                if (added != null) DetectionNotifications.showAdded(app, repo.state.value, added)
            }
        }
    }
}
