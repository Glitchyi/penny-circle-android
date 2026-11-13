package com.spendingbuddy.app

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ImportReviewTest {
    private val today = LocalDate.of(2026, 11, 14)
    private fun candidate() = ImportCandidate(id = "one", amountText = "₹1,000.01", merchant = "Shop", date = "2026-11-13")

    @Test fun reviewConvertsExactPaiseOnlyAfterAllSelectedFieldsAreValid() {
        val draft = candidate()
        val expense = requireNotNull(reviewedImportExpenses(listOf(draft), today)).single()
        assertEquals(100001L, expense.amountPaise)
        assertEquals("2026-11-13", expense.date)
        assertEquals("expense", expense.kind)
        assertNull(importReviewError(listOf(draft), today))
    }

    @Test fun malformedPrecisionZeroNegativeAndBoundsNeverProduceExpenses() {
        listOf("", "abc", "1.001", "0", "-1", "100000000", "9223372036854775807").forEach { text ->
            val draft = candidate().copy(amountText = text, rawText = "sensitive original", warning = "Review extracted details")
            assertNotNull(importCandidateErrors(draft, today).amount)
            assertNull(reviewedImportExpenses(listOf(draft), today))
            assertEquals(text, draft.amountText)
            assertEquals("sensitive original", draft.rawText)
            assertEquals("Review extracted details", draft.warning)
            assertTrue(draft.selected)
        }
    }

    @Test fun dateValidationRejectsMissingMalformedAndFutureWithoutRewritingThem() {
        listOf("", "13/11/2026", "2026-02-30", "2026-11-15").forEach {
            val draft = candidate().copy(date = it)
            assertNotNull(importCandidateErrors(draft, today).date)
            assertNull(reviewedImportExpenses(listOf(draft), today))
            assertEquals(it, draft.date)
        }
        assertNull(importCandidateErrors(candidate().copy(date = "2024-02-29"), today).date)
    }

    @Test fun fieldErrorsIdentifyEveryMissingEditableDetail() {
        val errors = importCandidateErrors(candidate().copy(merchant = " ", category = "", reason = ""), today)
        assertNotNull(errors.merchant)
        assertNotNull(errors.category)
        assertNotNull(errors.reason)
    }

    @Test fun duplicatesAreNotRemovedAndInvalidDeselectedCandidatesAreNotSaved() {
        val first = candidate().copy(possibleDuplicate = true)
        val second = first.copy(id = "two")
        val ignored = candidate().copy(id = "ignored", amountText = "unreadable", selected = false)
        val expenses = requireNotNull(reviewedImportExpenses(listOf(first, second, ignored), today))
        assertEquals(2, expenses.size)
        assertEquals(listOf(100001L, 100001L), expenses.map { it.amountPaise })
        assertNotNull(importReviewError(listOf(ignored), today))
    }

    @Test fun reviewedIncomingCandidateRetainsRefundKind() {
        val draft = candidate().copy(warning = "$INCOMING_WARNING; review")
        assertEquals("refund", requireNotNull(reviewedImportExpenses(listOf(draft), today)).single().kind)
        assertEquals("refund", kindFor(draft.copy(merchant = "Corrected shop")))
    }

    @Test fun arrivingScreenshotPreservesUnfinishedFieldEditsAndSelection() {
        val original = candidate()
        val edited = original.copy(amountText = "unfinished.", date = "", merchant = "Corrected", selected = false)
        val added = candidate().copy(id = "new", amountText = "0.01")
        assertEquals(listOf(edited, added), mergeImportReviewDrafts(listOf(edited), listOf(original, added)))
    }

    @Test fun removingAnImageRemovesOnlyItsDraftAndKeepsOtherRawFields() {
        val retained = candidate().copy(amountText = "invalid", rawText = "on-device original")
        val removed = candidate().copy(id = "removed")
        assertEquals(listOf(retained), mergeImportReviewDrafts(listOf(retained, removed), listOf(candidate())))
    }
}
