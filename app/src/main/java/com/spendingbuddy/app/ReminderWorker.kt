package com.spendingbuddy.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object BuddyNotifications {
    private const val NUDGE_CHANNEL = "buddy_nudges"
    private const val ACTIVITY_CHANNEL = "buddy_activity"
    private const val CHECK_IN_CHANNEL = "buddy_check_ins"
    const val LINK_CHANNEL = "buddy_link"
    private const val NUDGE_NOTIFICATION_ID = 4101
    private const val CHECK_IN_NOTIFICATION_ID = 4102
    const val SERVICE_NOTIFICATION_ID = 4103
    private const val ACTIVITY_NOTIFICATION_ID = 4104
    private const val JOIN_REQUEST_NOTIFICATION_ID = 4105

    fun ensureChannels(context: Context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(NUDGE_CHANNEL, context.getString(R.string.notification_channel_nudges), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.notification_channel_nudges_description)
                },
                NotificationChannel(ACTIVITY_CHANNEL, context.getString(R.string.notification_channel_activity), NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(LINK_CHANNEL, context.getString(R.string.notification_channel_link), NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(CHECK_IN_CHANNEL, context.getString(R.string.notification_channel_check_ins), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.notification_channel_check_ins_description)
                }
            )
        )
    }

    @SuppressLint("MissingPermission")
    fun showNudge(context: Context, nudge: Nudge) {
        if (!notificationsGranted(context)) return
        ensureChannels(context)
        val now = System.currentTimeMillis()
        val builder = baseBuilder(context, NUDGE_CHANNEL)
            .setSmallIcon(com.spendingbuddy.app.R.drawable.ic_stat_buddy)
            .setContentTitle(context.getString(R.string.notification_nudge_title, nudge.sender))
            .setContentText(context.resources.getQuantityString(R.plurals.notification_nudge_body, nudge.minutes, nudge.minutes))
            .setAutoCancel(true)
            .addAction(NotificationCompat.Action.Builder(0, context.getString(R.string.notification_add_purchase), mainActivityIntent(context, Destinations.ADD)).build())
            .addAction(NotificationCompat.Action.Builder(0, context.getString(R.string.notification_finish_today), mainActivityIntent(context, Destinations.TODAY)).build())
        if (nudge.endsAt > now) {
            builder.setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(nudge.endsAt)
                .setShowWhen(true)
        } else {
            // Do not give a late notification a negative countdown.
            builder.setUsesChronometer(false).setWhen(now).setShowWhen(false)
        }
        builder.setTimeoutAfter((nudge.endsAt - now).coerceAtLeast(0L))
        NotificationManagerCompat.from(context).notify(NUDGE_NOTIFICATION_ID, builder.build())
    }

    @SuppressLint("MissingPermission")
    fun notifyCheckIn(context: Context, title: String, body: String) {
        if (!notificationsGranted(context)) return
        ensureChannels(context)
        val notification = baseBuilder(context, CHECK_IN_CHANNEL)
            .setSmallIcon(com.spendingbuddy.app.R.drawable.ic_stat_buddy)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(CHECK_IN_NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission")
    fun notifyActivity(context: Context, title: String, body: String, memberId: String = "") {
        if (!notificationsGranted(context)) return
        ensureChannels(context)
        // One notification per member and kind of news, so several members' updates do not replace each other.
        NotificationManagerCompat.from(context).notify(
            ACTIVITY_NOTIFICATION_ID + (title.hashCode() xor memberId.hashCode()).and(0xFFFF) * 10,
            baseBuilder(context, ACTIVITY_CHANNEL).setSmallIcon(com.spendingbuddy.app.R.drawable.ic_stat_buddy)
                .setContentTitle(title).setContentText(body).setAutoCancel(true)
                .setContentIntent(mainActivityIntent(context, Destinations.BUDDY)).build()
        )
    }

    /** Admin only: a new join request is waiting. Honest copy: the requester sees the decision on their next check, not instantly. */
    @SuppressLint("MissingPermission")
    fun notifyJoinRequest(context: Context, name: String) {
        if (!notificationsGranted(context)) return
        ensureChannels(context)
        NotificationManagerCompat.from(context).notify(
            JOIN_REQUEST_NOTIFICATION_ID,
            baseBuilder(context, ACTIVITY_CHANNEL).setSmallIcon(com.spendingbuddy.app.R.drawable.ic_stat_buddy)
                .setContentTitle(context.getString(R.string.notification_join_title, name))
                .setContentText(context.getString(R.string.notification_join_body))
                .setAutoCancel(true)
                .setContentIntent(mainActivityIntent(context, Destinations.BUDDY)).build()
        )
    }

    fun cancelNudge(context: Context) {
        NotificationManagerCompat.from(context).cancel(NUDGE_NOTIFICATION_ID)
    }

    fun schedule(context: Context, enabled: Boolean, finishCountdownLeadMinutes: Int = DEFAULT_FINISH_COUNTDOWN_LEAD_MINUTES) {
        require(finishCountdownLeadMinutes in FINISH_COUNTDOWN_LEAD_MINUTES)
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            // The daily names include their target date, so tags cancel all pending dates.
            workManager.cancelAllWorkByTag(EVENING_WORK_TAG)
            workManager.cancelAllWorkByTag(DEADLINE_WORK_TAG)
            workManager.cancelAllWorkByTag(PRE_REMINDER_WORK_TAG)
            return
        }
        ensureChannels(context)
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequest.Builder(ReminderWorker::class.java, 15, TimeUnit.MINUTES)
                .addTag(PERIODIC_WORK_NAME)
                .setInputData(workDataOf(KEY_KIND to KIND_PERIODIC))
                .build()
        )
        reschedulePreReminder(context, finishCountdownLeadMinutes)
        enqueueDailyAt(workManager, EVENING_WORK_NAME, EVENING_WORK_TAG, LocalTime.of(22, 0), KIND_EVENING)
        enqueueDailyAt(workManager, DEADLINE_WORK_NAME, DEADLINE_WORK_TAG, LocalTime.of(10, 0), KIND_DEADLINE)
    }

    private fun baseBuilder(context: Context, channel: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, channel)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(mainActivityIntent(context, Destinations.TODAY))

    private fun mainActivityIntent(context: Context, destination: String = Destinations.TODAY): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = notificationActivityAction(destination)
            putExtra(Destinations.EXTRA, destination)
        }
        return PendingIntent.getActivity(
            context,
            9100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notificationsGranted(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun enqueueDailyAt(workManager: WorkManager, name: String, tag: String, target: LocalTime, kind: String, replace: Boolean = false) {
        val now = ZonedDateTime.now(INDIA_ZONE)
        var next = now.with(target)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMillis().coerceAtLeast(0L)
        val request = OneTimeWorkRequest.Builder(ReminderWorker::class.java)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_KIND to kind))
            .addTag(tag)
            .build()
        // A dated name lets the worker queue tomorrow without replacing a pending request
        // whenever the approximate periodic pass runs.
        workManager.enqueueUniqueWork("$name-${next.toLocalDate()}", if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP, request)
    }

    fun reschedulePreReminder(context: Context, leadMinutes: Int) {
        require(leadMinutes in FINISH_COUNTDOWN_LEAD_MINUTES)
        val workManager = WorkManager.getInstance(context.applicationContext)
        workManager.cancelAllWorkByTag(PRE_REMINDER_WORK_TAG)
        enqueueDailyAt(workManager, PRE_REMINDER_WORK_NAME, PRE_REMINDER_WORK_TAG, finishCountdownStartTime(leadMinutes), KIND_PRE_REMINDER, replace = true)
    }

    /** Requeue only a targeted daily request; periodic passes never replace daily work. */
    internal fun rescheduleDaily(context: Context, kind: String?, leadMinutes: Int = DEFAULT_FINISH_COUNTDOWN_LEAD_MINUTES) {
        when (kind) {
            KIND_EVENING -> enqueueDailyAt(
                WorkManager.getInstance(context.applicationContext),
                EVENING_WORK_NAME,
                EVENING_WORK_TAG,
                LocalTime.of(22, 0),
                KIND_EVENING
            )
            KIND_PRE_REMINDER -> enqueueDailyAt(
                WorkManager.getInstance(context.applicationContext),
                PRE_REMINDER_WORK_NAME,
                PRE_REMINDER_WORK_TAG,
                finishCountdownStartTime(leadMinutes),
                KIND_PRE_REMINDER,
                replace = true
            )
            KIND_DEADLINE -> enqueueDailyAt(
                WorkManager.getInstance(context.applicationContext),
                DEADLINE_WORK_NAME,
                DEADLINE_WORK_TAG,
                LocalTime.of(10, 0),
                KIND_DEADLINE
            )
        }
    }

    private const val PERIODIC_WORK_NAME = "buddy_reminders_periodic"
    private const val EVENING_WORK_NAME = "buddy_reminders_22h"
    private const val PRE_REMINDER_WORK_NAME = "buddy_reminders_2130h"
    private const val DEADLINE_WORK_NAME = "buddy_reminders_10h"
    private const val EVENING_WORK_TAG = "buddy_reminders_22h_tag"
    private const val PRE_REMINDER_WORK_TAG = "buddy_reminders_2130h_tag"
    private const val DEADLINE_WORK_TAG = "buddy_reminders_10h_tag"
    private const val KEY_KIND = "reminder_kind"
    private const val KIND_PERIODIC = "periodic"
    private const val KIND_PRE_REMINDER = "pre_reminder"
    private const val KIND_EVENING = "evening"
    private const val KIND_DEADLINE = "deadline"
}

class ReminderWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val repo = BuddyRepository.get(context)
        val state = repo.state.value

        // The notification uses the system chronometer. This bounded refresh posts/cancels it on scheduled passes;
        // there is no per-second worker or exact-alarm loop.
        runCatching { FinishCountdownNotifications.refresh(context, state) }

        // WorkManager may have queued a request before the user disabled reminders.
        // Re-read local state before doing any sync or notification work.
        if (!state.reminders) {
            BuddyNotifications.cancelNudge(context)
            BuddyNotifications.schedule(context, false)
            return Result.success()
        }

        val now = ZonedDateTime.now(INDIA_ZONE)
        val today = now.toLocalDate().toString()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        BuddyNotifications.ensureChannels(context)

        if (now.hour == 22) {
            val ownCheckInComplete = state.checkIns.any { it.date == today }
            if (!ownCheckInComplete && markOnce(prefs, KEY_CHECK_IN_DATE, today)) {
                BuddyNotifications.notifyCheckIn(
                    context,
                    context.getString(R.string.notification_quick_title),
                    context.getString(R.string.notification_quick_body)
                )
            }

            val candidates = com.spendingbuddy.app.p2p.nudgeCandidates(state, today, System.currentTimeMillis())
            if (candidates.isNotEmpty() && markOnce(prefs, KEY_NUDGE_DATE, today)) {
                val names = candidates.map { state.friends[it.endpointId]?.name?.ifBlank { null } ?: it.displayName }
                BuddyNotifications.notifyCheckIn(
                    context,
                    if (names.size == 1) context.getString(R.string.notification_friend_prompt_title, names[0])
                    else context.resources.getQuantityString(R.plurals.notification_friends_prompt_title, names.size, names.size),
                    context.getString(R.string.notification_friend_prompt_body)
                )
            }
        }

        // Targeted WorkManager requests are approximate and may run late under Android restrictions.
        // After expiry, be explicit that late logs remain allowed rather than saying it is ending.
        val targetedDeadline = inputData.getString("reminder_kind") == "deadline" && now.hour >= 10
        if (targetedDeadline || (now.hour == 10 && now.minute < 20)) {
            val yesterday = now.toLocalDate().minusDays(1).toString()
            val ownYesterdayIncomplete = state.checkIns.none { it.date == yesterday }
            if (ownYesterdayIncomplete && markOnce(prefs, KEY_DEADLINE_DATE, today)) {
                BuddyNotifications.notifyCheckIn(
                    context,
                    context.getString(R.string.notification_deadline_title),
                    context.getString(R.string.notification_deadline_body)
                )
            }
        }

        BuddyNotifications.rescheduleDaily(context, inputData.getString("reminder_kind"), state.finishCountdownLeadMinutes)
        return Result.success()
    }

    private fun markOnce(prefs: android.content.SharedPreferences, key: String, value: String): Boolean {
        synchronized(prefs) {
            if (prefs.getString(key, null) == value) return false
            prefs.edit().putString(key, value).apply()
            return true
        }
    }

    private companion object {
        const val PREFS_NAME = "buddy_notification_dedup"
        const val KEY_CHECK_IN_DATE = "local_check_in_date"
        const val KEY_NUDGE_DATE = "friend_nudge_prompt_date"
        const val KEY_DEADLINE_DATE = "grace_deadline_date"
    }
}
