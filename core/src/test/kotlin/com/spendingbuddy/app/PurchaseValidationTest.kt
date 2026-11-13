package com.spendingbuddy.app

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PurchaseValidationTest {
    private val today = LocalDate.of(2026, 11, 14)

    @Test fun exactPositivePaiseAndPastDatesAreValid() {
        assertNull(purchaseFieldErrors("0.01", "Food", "Essential", "2026-11-13", today).first)
        assertNull(purchaseFieldErrors("99999999.99", "Food", "Essential", "2026-11-14", today).first)
    }

    @Test fun errorsIdentifyAmountCategoryReasonAndDateIndependently() {
        val errors = purchaseFieldErrors("1.001", "", "", "2026-11-15", today)
        assertNotNull(errors.amount)
        assertNotNull(errors.category)
        assertNotNull(errors.reason)
        assertNotNull(errors.date)
        assertNotNull(purchaseFieldErrors("0", "Food", "Essential", "2026-11-14", today).amount)
        assertNotNull(purchaseFieldErrors("-1", "Food", "Essential", "2026-11-14", today).amount)
        assertNotNull(purchaseFieldErrors("1", "Food", "Essential", "invalid", today).date)
    }
}
