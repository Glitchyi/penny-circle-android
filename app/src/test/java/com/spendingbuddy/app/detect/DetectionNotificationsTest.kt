package com.spendingbuddy.app.detect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.spendingbuddy.app.DEFAULT_PAYMENT_UPDATE_MINUTES
import com.spendingbuddy.app.PAYMENT_UPDATE_MINUTES
import com.spendingbuddy.app.paymentUpdateDurationMillis

class DetectionNotificationsTest {
    @Test fun criticalTextIsCompactAndStillTruthful() {
        val samples = listOf(
            Triple(24_000L, 50_000L, "48%"),
            Triple(50_000L, 50_000L, "100%"),
            Triple(51_000L, 50_000L, "Over"),
            Triple(0L, 0L, "₹0"),
            Triple(1L, 0L, "Over")
        )
        samples.forEach { (spent, limit, expected) ->
            val text = budgetCriticalText(spent, limit, "Over")
            assertEquals("spent=$spent limit=$limit", expected, text)
            assertTrue("critical text must fit comfortably in the status chip: $text", text.length <= 7)
        }
    }

    @Test fun budgetNotificationDurationIsTwoMinutes() {
        assertEquals(120_000L, DetectionNotifications.ADDED_NOTIFICATION_DURATION_MS)
        assertEquals(120_000L, paymentUpdateDurationMillis(DEFAULT_PAYMENT_UPDATE_MINUTES))
        assertEquals(listOf(60_000L, 120_000L, 300_000L), PAYMENT_UPDATE_MINUTES.map(::paymentUpdateDurationMillis))
    }

    @Test fun criticalTextUsesTheResourceLabelForBothOverLimitStates() {
        assertEquals("Above", budgetCriticalText(51_000L, 50_000L, "Above"))
        assertEquals("Above", budgetCriticalText(1L, 0L, "Above"))
        assertEquals("₹0", budgetCriticalText(0L, 0L, "Above"))
    }

}
