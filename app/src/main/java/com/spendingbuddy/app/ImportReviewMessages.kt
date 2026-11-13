package com.spendingbuddy.app

import android.content.Context
import java.time.LocalDate

/** Resource mapping at the Android boundary; pure validation remains usable by core and saved drafts. */
fun Context.importCandidateMessages(candidate: ImportCandidate, errors: ImportCandidateErrors = importCandidateErrors(candidate), today: LocalDate = todayIndia()): ImportCandidateErrors {
    val amount = amountToPaise(candidate.amountText)
    val date = runCatching { LocalDate.parse(candidate.date) }.getOrNull()
    return ImportCandidateErrors(
        amount = errors.amount?.let {
            if (amount != null && amount <= 0L) getString(R.string.import_error_positive)
            else getString(R.string.import_error_amount, candidate.merchant.ifBlank { getString(R.string.import_error_a_purchase) })
        },
        date = errors.date?.let {
            if (date != null && date > today) getString(R.string.import_error_future)
            else getString(R.string.import_error_date, candidate.merchant.ifBlank { getString(R.string.import_error_each_purchase) })
        },
        merchant = errors.merchant?.let { getString(R.string.import_error_details) },
        category = errors.category?.let { getString(R.string.import_error_details) },
        reason = errors.reason?.let { getString(R.string.import_error_details) }
    )
}

fun Context.importReviewMessage(candidates: List<ImportCandidate>, today: LocalDate = todayIndia()): String? {
    val selected = candidates.filter { it.selected }
    if (selected.isEmpty()) return getString(R.string.import_no_selection)
    return selected.firstNotNullOfOrNull { importCandidateMessages(it, importCandidateErrors(it, today), today).first }
}
