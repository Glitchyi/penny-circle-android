package com.spendingbuddy.app

import java.time.LocalDate

data class PurchaseFieldErrors(val amount: String? = null, val category: String? = null, val reason: String? = null, val date: String? = null) {
    val first: String? get() = amount ?: category ?: reason ?: date
}

fun purchaseFieldErrors(amount: String, category: String, reason: String, date: String, today: LocalDate = todayIndia()): PurchaseFieldErrors {
    val parsedAmount = amountToPaise(amount)
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
    return PurchaseFieldErrors(
        amount = "Enter an amount above ₹0".takeIf { parsedAmount == null || parsedAmount <= 0L },
        category = "Choose a category and a reason".takeIf { category.isBlank() },
        reason = "Choose a reason".takeIf { reason.isBlank() },
        date = "Choose today or an earlier date".takeIf { parsedDate == null || parsedDate > today }
    )
}
