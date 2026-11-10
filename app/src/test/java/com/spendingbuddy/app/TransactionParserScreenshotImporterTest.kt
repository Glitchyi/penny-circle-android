package com.spendingbuddy.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TransactionParserScreenshotImporterTest {
    @Test
    fun parsesRupeeRsAndInrAmountsWithBoundedDates() {
        val candidates = TransactionParser.parse(
            """
            12 Mar 2025
            Paid to Cafe One
            ₹1,234.50
            Yesterday
            Sent to Cafe Two
            Rs. 250
            Today
            Paid to Cafe Three
            INR 75.25
            """.trimIndent(),
            "image-amounts"
        )

        assertEquals(3, candidates.size)
        assertEquals("₹1,234.50", candidates[0].amountText)
        assertEquals("Cafe One", candidates[0].merchant)
        assertEquals("2025-03-12", candidates[0].date)
        assertEquals("₹250", candidates[1].amountText)
        assertEquals("Cafe Two", candidates[1].merchant)
        assertEquals(LocalDate.now(INDIA_ZONE).minusDays(1).toString(), candidates[1].date)
        assertEquals("₹75.25", candidates[2].amountText)
        assertEquals("Cafe Three", candidates[2].merchant)
    }

    @Test
    fun parsesUngroupedAndDecimalAmountsWithoutTruncating() {
        val candidates = TransactionParser.parse(
            """
            Paid to Grocery Store
            ₹2500
            Paid to Travel Store
            ₹123456.78
            """.trimIndent(),
            "image-large-amounts"
        )

        assertEquals(listOf("₹2500", "₹123456.78"), candidates.map { it.amountText })
        assertEquals(250000L, amountToPaise(candidates[0].amountText))
        assertEquals(12345678L, amountToPaise(candidates[1].amountText))
    }

    @Test
    fun rejectsAmountsWithMoreThanTwoDecimalPlacesInsteadOfTruncating() {
        val candidates = TransactionParser.parse("Paid to Grocery Store\n₹1.234", "image-invalid-decimal")

        assertTrue(candidates.isEmpty())
        assertNull(amountToPaise("₹1.234"))
    }

    @Test
    fun acceptsGooglePayMerchantRowsButNotHeadingsOrAdjacentRows() {
        val candidates = TransactionParser.parse(
            """
            Today
            Corner Cafe
            ₹2500
            Balance
            ₹100
            """.trimIndent(),
            "image-google-pay"
        )

        assertEquals(1, candidates.size)
        assertEquals("Corner Cafe", candidates.single().merchant)
        assertEquals("₹2500", candidates.single().amountText)
    }

    @Test
    fun doesNotMergeSameAmountMerchantsFromOverlappingScreenshotRows() {
        val candidates = TransactionParser.parse(
            """
            Today
            Paid to Alpha Cafe
            ₹100
            Paid to Beta Cafe
            ₹100
            """.trimIndent(),
            "image-duplicates"
        )

        assertEquals(2, candidates.size)
        assertEquals(listOf("Alpha Cafe", "Beta Cafe"), candidates.map { it.merchant })
        assertEquals(listOf("₹100", "₹100"), candidates.map { it.amountText })
    }

    @Test
    fun keepsIncomingCandidateForReviewButDoesNotSelectIt() {
        val candidates = TransactionParser.parse(
            """
            08/03/2025
            Received from Friend
            INR 500
            """.trimIndent(),
            "image-incoming"
        )

        assertEquals(1, candidates.size)
        assertEquals("Friend", candidates.single().merchant)
        assertFalse(candidates.single().selected)
        assertTrue(candidates.single().warning.contains("Incoming/refund"))
        assertEquals("2025-03-08", candidates.single().date)
    }

    @Test
    fun skipsFailedOrCancelledAmounts() {
        val candidates = TransactionParser.parse(
            """
            08 Mar 2025
            Payment failed to Closed Store
            ₹900
            08 Mar 2025
            Payment cancelled to Cancelled Store
            ₹800
            """.trimIndent(),
            "image-status"
        )

        assertTrue(candidates.isEmpty())
    }

    @Test
    fun leavesMissingDateBlankAndWarnsForPartialYear() {
        val noDate = TransactionParser.parse("Paid to Unknown Shop\n₹45", "image-no-date").single()
        assertEquals("", noDate.date)
        assertTrue(noDate.warning.contains("Date not detected"))

        val partial = TransactionParser.parse("08 Mar\nPaid to Old Shop\n₹55", "image-partial-date").single()
        assertEquals("08 Mar", partial.date)
        assertTrue(partial.warning.contains("year is uncertain"))
    }

    @Test
    fun paidFromBankIsNotIncoming() {
        val c = TransactionParser.parse("Paid from HDFC\nPaid to Cafe One\n₹120", "img").single()
        assertTrue(c.selected)
        assertFalse(c.isIncoming())
        assertEquals("expense", kindFor(c))
    }

    @Test
    fun trailingStatusWordsAreRead() {
        assertTrue(TransactionParser.parse("Paid to Shop A\n₹300\nFailed", "img").isEmpty())
        val received = TransactionParser.parse("Paid to Shop B\n₹300\nReceived", "img").single()
        assertTrue(received.isIncoming())
        assertFalse(received.selected)
    }

    @Test
    fun nextRowStatusIsNotAttachedToPreviousAmount() {
        val out = TransactionParser.parse("Paid to Shop A\n₹100\nReceived from Bob\n₹200", "img")
        assertEquals(2, out.size)
        assertFalse(out[0].isIncoming())
        assertTrue(out[1].isIncoming())
    }

    @Test
    fun incomingCandidatesSaveAsRefund() {
        val c = TransactionParser.parse("Received from Friend\nINR 500", "img").single()
        assertEquals("refund", kindFor(c))
    }

    @Test
    fun interruptedAndLegacyErrorImportsBecomeFailed() {
        val out = recoverInterruptedImports(listOf(
            ImportImage(status = "processing"), ImportImage(status = "error", error = "x"), ImportImage(status = "pending")
        ))
        assertEquals(listOf("failed", "failed", "pending"), out.map { it.status })
    }
}
