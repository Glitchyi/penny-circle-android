package com.spendingbuddy.app.detect

import android.app.Notification
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.spendingbuddy.app.BuddyRepository
import com.spendingbuddy.app.looksLikeBusinessSender

/**
 * Reads payment alerts from payment and bank apps as they are posted. Only the apps in
 * [PurchaseDetector.PAYMENT_APPS] are looked at. SMS alerts are read from the SMS app's notification only when
 * this app has no SMS access of its own (otherwise [SmsReceiver] already has the full message).
 */
class PaymentNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return
        val notification = sbn.notification ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (!BuddyRepository.get(applicationContext).state.value.autoDetect) return

        val fromPaymentApp = pkg in PurchaseDetector.PAYMENT_APPS
        val fromSmsApp = !fromPaymentApp && (pkg in PurchaseDetector.SMS_APPS || pkg == runCatching { Telephony.Sms.getDefaultSmsPackage(this) }.getOrNull())
        if (!fromPaymentApp && !fromSmsApp) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (text.isBlank() && title.isBlank()) return

        if (fromSmsApp) {
            // In an SMS app the title is the sender. Bank alerts come from named senders, never a phone number.
            if (PurchaseDetector.smsGranted(this) || !looksLikeBusinessSender(title)) return
            PurchaseDetector.worker.execute { runCatching { PurchaseDetector.onMessage(applicationContext, text) } }
        } else {
            PurchaseDetector.worker.execute { runCatching { PurchaseDetector.onMessage(applicationContext, text, title) } }
        }
    }
}
