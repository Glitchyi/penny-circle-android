package com.spendingbuddy.app.detect

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.spendingbuddy.app.BuddyRepository
import com.spendingbuddy.app.DetectionOutcome
import com.spendingbuddy.app.PaymentMessageParser
import java.util.concurrent.Executors

/**
 * Turns a payment message into a purchase, entirely on this phone. The message text is parsed in memory and then
 * dropped: only the amount, the shop name and the time are kept. Nothing here uses the network.
 */
object PurchaseDetector {
    /** Messages are handled one at a time, off the main thread, so two alerts for one payment cannot race. */
    val worker = Executors.newSingleThreadExecutor()

    /** Call on [worker]. [title] is a notification title that is part of the message (not a sender name). */
    fun onMessage(context: Context, text: String, title: String = "") {
        val app = context.applicationContext
        val repo = BuddyRepository.get(app)
        if (!repo.state.value.autoDetect) return
        val payment = PaymentMessageParser.parse(text, title) ?: return
        when (val outcome = repo.recordDetection(payment)) {
            is DetectionOutcome.Added -> {
                DetectionNotifications.showAdded(app, repo.state.value, outcome.expense)
                HoldReleaseWorker.schedule(app)
            }
            is DetectionOutcome.Asked -> DetectionNotifications.showAsk(app, outcome.pending)
            DetectionOutcome.Ignored -> Unit
        }
    }

    fun smsGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

    /** True when the user has given this app notification access in system settings. */
    fun notificationAccessGranted(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
        val me = ComponentName(context, PaymentNotificationListener::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    /**
     * Apps whose notifications are read for payments. Everything else (chats, mail, shopping updates) is never
     * looked at, so a friend typing "I paid Rs 500" cannot create a purchase.
     */
    val PAYMENT_APPS = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",
        "net.one97.paytm",
        "in.org.npci.upiapp", // BHIM
        "com.dreamplug.androidapp", // CRED
        "in.amazon.mShop.android.shopping", // Amazon Pay
        "com.mobikwik_new",
        "com.freecharge.android",
        "com.myairtelapp",
        "com.hdfcbank.android.now",
        "com.sbi.lotusintouch",
        "com.sbi.upi",
        "com.csam.icici.bank.imobile",
        "com.axis.mobile",
        "com.msf.kbank.mobile",
        "com.kotak.bank.mobile",
        "com.idfcfirstbank.optimus",
        "com.bankofbaroda.mconnect",
        "com.canarabank.mobility",
        "com.fedmobile",
        "money.jupiter",
        "money.super.payments",
        "com.naviapp",
        "com.fampay.in"
    )

    /** Common SMS apps, used only when this app has not been given SMS access itself. */
    val SMS_APPS = setOf(
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.android.messaging",
        "com.samsung.android.messaging",
        "com.oneplus.mms",
        "com.truecaller"
    )
}
