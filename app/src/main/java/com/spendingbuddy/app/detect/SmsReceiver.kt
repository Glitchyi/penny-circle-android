package com.spendingbuddy.app.detect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.spendingbuddy.app.looksLikeBusinessSender

/**
 * Receives each incoming SMS as it arrives. Messages from ordinary phone numbers are dropped unread; bank and
 * payment alerts are parsed on the phone. The inbox is never scanned and no message text is stored.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = runCatching { Telephony.Sms.Intents.getMessagesFromIntent(intent) }.getOrNull()?.filterNotNull().orEmpty()
        if (parts.isEmpty()) return
        val sender = parts.first().displayOriginatingAddress.orEmpty()
        if (!looksLikeBusinessSender(sender)) return
        val body = parts.joinToString("") { it.displayMessageBody.orEmpty() }
        val pending = goAsync()
        PurchaseDetector.worker.execute {
            try { PurchaseDetector.onMessage(context.applicationContext, body) } catch (_: Exception) { } finally { pending.finish() }
        }
    }
}
