package com.spendingbuddy.app

import com.spendingbuddy.app.p2p.snapshotFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DetectionTest {
    private fun parse(text: String, title: String = "") = PaymentMessageParser.parse(text, title)
    private val on = BuddyState(autoDetect = true, onboardingDone = true)
    private val now = 1_800_000_000_000L

    // ---- Messages that are payments -------------------------------------------------------------------------

    @Test fun hdfcUpi() {
        val p = parse("Sent Rs.240.00 From HDFC Bank A/C *1234 To SWIGGY On 13/11/26 Ref 123456789012 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808")!!
        assertEquals(24000L, p.amountPaise); assertEquals("Swiggy", p.merchant); assertEquals("123456789012", p.ref)
    }

    @Test fun hdfcCard() {
        val p = parse("Spent Rs.1,250.00 On HDFC Bank Card 1234 At AMAZON PAY INDIA On 2026-11-13:14:22:31. Not You? To Block+Reissue Call 18002586161")!!
        assertEquals(125000L, p.amountPaise); assertEquals("Amazon Pay India", p.merchant); assertEquals(PaymentKind.CARD, p.kind)
    }

    @Test fun sbiUpiHasNoCurrencySymbol() {
        val p = parse("Dear UPI user A/C X1234 debited by 240.0 on date 13Nov26 trf to SWIGGY Refno 123456789012. If not u? call 1800111109. -SBI")!!
        assertEquals(24000L, p.amountPaise); assertEquals("Swiggy", p.merchant); assertEquals("123456789012", p.ref)
    }

    @Test fun sbiCard() {
        val p = parse("Rs.499.00 spent on your SBI Credit Card ending 1234 at NETFLIX on 13/11/26. Trxn. not done by you? Report at https://sbicard.com/Dispute")!!
        assertEquals(49900L, p.amountPaise); assertEquals("Netflix", p.merchant)
    }

    @Test fun iciciUpiNamesTheShopAsCredited() {
        val p = parse("ICICI Bank Acct XX123 debited for Rs 240.00 on 13-Nov-26; SWIGGY credited. UPI:123456789012. Call 18002662 for dispute. SMS BLOCK 123 to 9215676766.")!!
        assertEquals(24000L, p.amountPaise); assertEquals("Swiggy", p.merchant)
    }

    @Test fun iciciCardIgnoresTheAvailableLimit() {
        val p = parse("INR 1,250.00 spent on ICICI Bank Card XX1234 on 13-Nov-26 at AMAZON. Avl Lmt: INR 1,00,000.00. To dispute, call 18002662/SMS BLOCK 1234 to 9215676766")!!
        assertEquals(125000L, p.amountPaise); assertEquals("Amazon", p.merchant)
    }

    @Test fun axisUpiMerchantPath() {
        val p = parse("INR 240.00 debited\nA/c no. XX1234\n13-11-26, 14:22:31\nUPI/P2M/123456789012/SWIGGY\nNot you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank")!!
        assertEquals(24000L, p.amountPaise); assertEquals("Swiggy", p.merchant); assertEquals(PaymentKind.UPI_MERCHANT, p.kind)
    }

    @Test fun axisUpiPersonPath() {
        val p = parse("INR 500.00 debited\nA/c no. XX1234\n13-11-26, 14:22:31\nUPI/P2A/123456789012/RAHUL SHARMA\nNot you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank")!!
        assertEquals("Rahul Sharma", p.merchant); assertEquals(PaymentKind.UPI_PERSON, p.kind)
    }

    @Test fun axisCardHasTheShopOnItsOwnLine() {
        val p = parse("Spent INR 450\nAxis Bank Card no. XX1234\n13-11-26 14:22:31 IST\nSTARBUCKS\nAvl Limit: INR 98765.43\nNot you? SMS BLOCK 1234 to 919951860002")!!
        assertEquals(45000L, p.amountPaise); assertEquals("Starbucks", p.merchant); assertEquals(PaymentKind.CARD, p.kind)
    }

    @Test fun kotakUpiId() {
        val p = parse("Sent Rs.240.00 from Kotak Bank AC X1234 to swiggy@icici on 13-11-26.UPI Ref 123456789012. Not you, https://kotak.com/KBANKT/Fraud")!!
        assertEquals(24000L, p.amountPaise); assertEquals("Swiggy", p.merchant)
    }

    @Test fun phoneNumberUpiIdIsAPerson() {
        val p = parse("Sent Rs.300.00 from Kotak Bank AC X1234 to 9876543210@ybl on 13-11-26.UPI Ref 123456789012.")!!
        assertEquals(PaymentKind.UPI_PERSON, p.kind); assertEquals("", p.merchant)
    }

    @Test fun balanceIsNotTheAmount() {
        val p = parse("Your A/C XXXXX123 is debited by INR 240.00 on 13/11/26 14:22. New Bal :INR 5,000.00. Call 1800 for dispute")!!
        assertEquals(24000L, p.amountPaise); assertEquals("", p.merchant)
    }

    @Test fun debitedAndCreditedToAnAccountIsStillAPayment() {
        val p = parse("Your a/c no. XX1234 is debited for Rs.240.00 on 13-11-26 and credited to a/c no. XX5678 (UPI Ref no 123456789012)")!!
        assertEquals(24000L, p.amountPaise); assertEquals("", p.merchant)
    }

    @Test fun paymentAppNotificationTitle() {
        val p = parse("From HDFC Bank ••1234", title = "Paid ₹240 to Swiggy")!!
        assertEquals(24000L, p.amountPaise); assertEquals("Swiggy", p.merchant)
    }

    @Test fun atmWithdrawalIsCash() {
        val p = parse("Rs.2000 withdrawn from A/c XX1234 at ATM on 13-11-26. Avl Bal Rs.5,000.00")!!
        assertEquals(200000L, p.amountPaise); assertEquals(PaymentKind.CASH, p.kind)
    }

    // ---- Messages that are not payments ---------------------------------------------------------------------

    @Test fun otpIsIgnored() = assertNull(parse("123456 is the OTP for txn of INR 240.00 at SWIGGY on HDFC Bank card ending 1234. Valid for 5 mins. Do not share OTP with anyone"))
    @Test fun otpForAPaidOrderIsIgnored() = assertNull(parse("Use OTP 482913 to confirm. Rs 240 will be paid to SWIGGY."))
    @Test fun moneyReceivedIsIgnored() = assertNull(parse("Rs.500.00 credited to a/c XX1234 on 13-11-26 by a/c linked to VPA rahul@ybl (UPI Ref no 123456789012)"))
    @Test fun moneyReceivedThenSentByIsIgnored() = assertNull(parse("You have received Rs 500 in a/c XX1234, sent by RAHUL via UPI"))
    @Test fun refundIsIgnored() = assertNull(parse("Refund of Rs 240.00 for your order has been credited to your card ending 1234"))
    @Test fun failedPaymentIsIgnored() = assertNull(parse("Your payment of Rs 240 to SWIGGY failed. Any amount debited will be reversed."))
    @Test fun upcomingDebitIsIgnored() = assertNull(parse("Rs 499 will be debited from your a/c XX1234 on 15-11-26 for NETFLIX autopay"))
    @Test fun billDueIsIgnored() = assertNull(parse("Your credit card bill of Rs 5,400 is due on 20-11-26. Min amt due Rs 270. Paid already? Ignore."))
    @Test fun offerIsIgnored() = assertNull(parse("Offer: Get Rs 500 cashback when you have spent Rs 5,000 on your HDFC card this week"))
    @Test fun requestIsIgnored() = assertNull(parse("RAHUL has requested Rs 500 from you on Google Pay. Open the app to pay."))
    @Test fun plainChatIsIgnored() = assertNull(parse("Dinner tonight? I can bring Rs 500"))
    @Test fun debitSmsWithOtpWarningStillCounts() =
        assertNotNull(parse("Rs.240.00 spent on HDFC Bank Card x1234 at ZOMATO on 2026-11-13. Bank never asks for OTP. Not you? Call 18002586161"))

    @Test fun businessSenders() {
        assertTrue(looksLikeBusinessSender("AD-HDFCBK")); assertTrue(looksLikeBusinessSender("JK-SBIUPI-S")); assertTrue(looksLikeBusinessSender("ICICIB"))
        assertFalse(looksLikeBusinessSender("+919876543210")); assertFalse(looksLikeBusinessSender("98765 43210")); assertFalse(looksLikeBusinessSender(""))
    }

    // ---- What happens with a parsed payment -----------------------------------------------------------------

    @Test fun nothingHappensWhenSwitchedOff() {
        assertEquals(DetectionOutcome.Ignored, applyDetection(BuddyState(onboardingDone = true), ParsedPayment(24000, "Swiggy"), now))
    }

    @Test fun knownShopIsAddedAndHeld() {
        val out = applyDetection(on, ParsedPayment(24000, "Swiggy", "111111"), now) as DetectionOutcome.Added
        assertEquals("Food", out.expense.category); assertEquals("Convenience", out.expense.reason)
        assertEquals(SOURCE_AUTO, out.expense.source); assertEquals(now + AUTO_HOLD_MS, out.expense.heldUntil); assertFalse(out.expense.needsCategory)
        assertEquals(1, out.state.expenses.size)
    }

    @Test fun cardPaymentAtAnUnknownShopIsAddedAndFlagged() {
        val out = applyDetection(on, ParsedPayment(45000, "Blue Tokai", kind = PaymentKind.CARD), now) as DetectionOutcome.Added
        assertEquals("Other", out.expense.category); assertTrue(out.expense.needsCategory)
    }

    @Test fun unknownNameIsAskedFirstAndAutomaticAfterItWasAddedOnce() {
        val asked = applyDetection(on, ParsedPayment(5000, "Ramesh Stores", "1"), now) as DetectionOutcome.Asked
        assertTrue(asked.state.expenses.isEmpty()); assertEquals(1, asked.state.pendingDetections.size)
        val (added, expense) = addPendingDetection(asked.state, asked.pending.id, now)
        assertNotNull(expense); assertEquals(0L, expense!!.heldUntil); assertTrue(added.pendingDetections.isEmpty())
        val again = applyDetection(added, ParsedPayment(7000, "RAMESH STORES", "2"), now + 3_600_000)
        assertTrue(again is DetectionOutcome.Added)
    }

    @Test fun personCashNoNameAndLargeAmountsAreAsked() {
        assertTrue(applyDetection(on, ParsedPayment(50000, "Rahul Sharma", kind = PaymentKind.UPI_PERSON), now) is DetectionOutcome.Asked)
        assertTrue(applyDetection(on, ParsedPayment(200000, "", kind = PaymentKind.CASH), now) is DetectionOutcome.Asked)
        assertTrue(applyDetection(on, ParsedPayment(24000, ""), now) is DetectionOutcome.Asked)
        assertTrue(applyDetection(on, ParsedPayment(ASK_ABOVE_PAISE, "Amazon", kind = PaymentKind.CARD), now) is DetectionOutcome.Asked)
    }

    @Test fun skippingRemovesThePendingItem() {
        val asked = applyDetection(on, ParsedPayment(5000, "Ramesh Stores"), now) as DetectionOutcome.Asked
        assertTrue(skipPendingDetection(asked.state, asked.pending.id).pendingDetections.isEmpty())
    }

    @Test fun samePaymentSeenTwiceIsCountedOnce() {
        val first = applyDetection(on, ParsedPayment(24000, "Swiggy"), now) as DetectionOutcome.Added
        // The bank SMS for the same payment arrives a little later, this time with a reference number.
        assertEquals(DetectionOutcome.Ignored, applyDetection(first.state, ParsedPayment(24000, "Swiggy", "123456789012"), now + 20_000))
    }

    @Test fun twoPaymentsWithDifferentReferencesAreBothCounted() {
        val first = applyDetection(on, ParsedPayment(2000, "Metro", "111111"), now) as DetectionOutcome.Added
        assertTrue(applyDetection(first.state, ParsedPayment(2000, "Metro", "222222"), now + 20_000) is DetectionOutcome.Added)
    }

    @Test fun sameAmountLaterIsANewPayment() {
        val first = applyDetection(on, ParsedPayment(24000, "Swiggy"), now) as DetectionOutcome.Added
        assertTrue(applyDetection(first.state, ParsedPayment(24000, "Swiggy"), now + DUPLICATE_WINDOW_MS + 1) is DetectionOutcome.Added)
    }

    @Test fun aPurchaseJustTypedInByHandIsNotAddedAgain() {
        val today = java.time.Instant.ofEpochMilli(now).atZone(INDIA_ZONE).toLocalDate().toString()
        val manual = on.copy(expenses = listOf(Expense(date = today, amountPaise = 24000, merchant = "Swiggy", updatedAt = now - 60_000)))
        assertEquals(DetectionOutcome.Ignored, applyDetection(manual, ParsedPayment(24000, "Swiggy"), now))
    }

    @Test fun theUsersRuleWinsOverTheBuiltInList() {
        val ruled = upsertMerchantRule(on, "Swiggy", "Shopping", "Impulse")
        val out = applyDetection(ruled, ParsedPayment(24000, "SWIGGY"), now) as DetectionOutcome.Added
        assertEquals("Shopping", out.expense.category); assertEquals("Impulse", out.expense.reason)
        assertTrue(deleteMerchantRule(ruled, "swiggy").merchantRules.isEmpty())
    }

    @Test fun lastChoiceForAShopIsReused() {
        val history = on.copy(expenses = listOf(Expense(date = "2026-01-01", amountPaise = 100, merchant = "Blue Tokai", category = "Food", reason = "Social", updatedAt = 1)))
        assertEquals(CategoryGuess("Food", "Social", false), guessCategory(history, "BLUE TOKAI"))
    }

    @Test fun undoRemovesThePurchaseAndRestoresTheFinishedDay() {
        val today = java.time.Instant.ofEpochMilli(now).atZone(INDIA_ZONE).toLocalDate().toString()
        val finished = on.copy(checkIns = listOf(CheckIn(today, now - 1000, noSpend = true)))
        val out = applyDetection(finished, ParsedPayment(24000, "Swiggy"), now) as DetectionOutcome.Added
        assertTrue(out.state.checkIns.isEmpty())
        val undone = undoAutoExpense(out.state, out.expense.id, now + 1)
        assertTrue(undone.expenses.isEmpty()); assertEquals(1, undone.checkIns.size); assertTrue(undone.heldCheckIns.isEmpty())
    }

    @Test fun undoNeverRemovesAHandTypedPurchase() {
        val manual = on.copy(expenses = listOf(Expense(id = "m", amountPaise = 100)))
        assertEquals(manual, undoAutoExpense(manual, "m"))
    }

    @Test fun aKnownNameNeverOverridesAnExplicitPersonPayment() {
        val ruled = upsertMerchantRule(on, "Rahul Sharma", "Other", "Social")
        assertTrue(applyDetection(ruled, ParsedPayment(5000, "Rahul Sharma", kind = PaymentKind.UPI_PERSON), now) is DetectionOutcome.Asked)
    }

    @Test fun builtInNamesRequireAWholeMerchantWord() {
        for (name in listOf("NotSwiggy", "Rajio", "Starbucksfan")) {
            assertFalse(isKnownMerchant(on, name))
            assertTrue(applyDetection(on, ParsedPayment(5000, name), now) is DetectionOutcome.Asked)
        }
        assertTrue(isKnownMerchant(on, "SWIGGY INDIA"))
        assertTrue(isKnownMerchant(on, "AMAZON PAY INDIA"))
    }

    @Test fun bankTransfersAskEvenWhenTheRecipientMatchesAShopRule() {
        val payment = PaymentMessageParser.parse("Thanks for paying Rs.500.00 from A/c XXXX1111 to SWIGGY via HDFC Bank NetBanking.")!!
        assertEquals(PaymentKind.BANK_TRANSFER, payment.kind)
        assertTrue(applyDetection(on, payment, now) is DetectionOutcome.Asked)
    }

    @Test fun futureDebitsAndCardInstructionsDoNotBecomeCompletedPayments() {
        assertNull(parse("We will debit INR 500.00 from your account to SWIGGY tomorrow."))
        assertNull(parse("You can pay Rs.500 at SWIGGY using your debit card."))
        assertNull(parse("Use your HDFC Bank Debit Card for INR 749.00 towards SAMPLE MART."))
    }

    @Test fun negatedDebitsAndRewardEligibilityNeverBecomePurchases() {
        assertNull(parse("No debit INR 500.00 was made to SWIGGY."))
        assertNull(parse("Your account was not debited Rs.500.00 at SWIGGY."))
        assertNull(parse("You have spent Rs 240 at SWIGGY to qualify for cashback."))
    }

    @Test fun payingACardBillRequiresConfirmationInsteadOfCountingAsANewShopPurchase() {
        val payment = parse("Thank you for paying Rs 5,000.00 towards HDFC Credit Card Bill at PayNow Portal.")!!
        assertEquals(PaymentKind.BANK_TRANSFER, payment.kind)
        assertTrue(applyDetection(on, payment, now) is DetectionOutcome.Asked)
    }

    @Test fun aLongBalanceLabelCannotReplaceTheTransactionAmount() {
        val payment = parse("Debited after merchant settlement completed. Current available balance following authorization review is INR 5,000.00. Transaction amount was INR 240.00 at SWIGGY.")!!
        assertEquals(24000L, payment.amountPaise)
    }

    @Test fun undoIsAvailableOnlyDuringTheHold() {
        val out = applyDetection(on, ParsedPayment(24000, "Swiggy"), now) as DetectionOutcome.Added
        assertEquals(out.state, undoAutoExpense(out.state, out.expense.id, now + AUTO_HOLD_MS))
        val kept = keepAutoExpense(out.state, out.expense.id)
        assertEquals(kept, undoAutoExpense(kept, out.expense.id, now + 1))
    }

    @Test fun heldPurchasesDoNotRevealAReopenedDayOrChangeTheSharedStreak() {
        val today = java.time.Instant.ofEpochMilli(now).atZone(INDIA_ZONE).toLocalDate().toString()
        val original = CheckIn(today, now - 1000, noSpend = true)
        val finished = on.copy(checkIns = listOf(original))
        val out = applyDetection(finished, ParsedPayment(24000, "Swiggy"), now) as DetectionOutcome.Added
        assertTrue(out.state.checkIns.isEmpty())
        for (visibility in Visibility.entries) {
            val before = snapshotFor(finished, visibility, now)
            val during = snapshotFor(out.state, visibility, now + 1)
            assertEquals(before.checkIns, during.checkIns)
            assertEquals(before.streak, during.streak)
            assertEquals(0L, during.dailySpentPaise)
            assertTrue(snapshotFor(out.state, visibility, now + AUTO_HOLD_MS).checkIns.isEmpty())
            assertTrue(snapshotFor(keepAutoExpense(out.state, out.expense.id), visibility, now + 1).checkIns.isEmpty())
        }
    }

    @Test fun multipleHoldsRestoreTheDayOnlyAfterTheLastUndoInEitherOrder() {
        val today = java.time.Instant.ofEpochMilli(now).atZone(INDIA_ZONE).toLocalDate().toString()
        val original = CheckIn(today, now - 1000, noSpend = true)
        val first = applyDetection(on.copy(checkIns = listOf(original)), ParsedPayment(24000, "Swiggy"), now) as DetectionOutcome.Added
        val second = applyDetection(first.state, ParsedPayment(5000, "Uber"), now + 1) as DetectionOutcome.Added
        for (ids in listOf(listOf(first.expense.id, second.expense.id), listOf(second.expense.id, first.expense.id))) {
            val undoneOnce = undoAutoExpense(second.state, ids[0], now + 2)
            assertTrue(undoneOnce.checkIns.isEmpty())
            assertEquals(listOf(original), snapshotFor(undoneOnce, Visibility.FULL_DETAILS, now + 2).checkIns)
            val undoneBoth = undoAutoExpense(undoneOnce, ids[1], now + 3)
            assertEquals(listOf(original), undoneBoth.checkIns)
            assertTrue(undoneBoth.heldCheckIns.isEmpty())
        }
    }

    @Test fun keepingOrReleasingOneHoldPreventsAnotherUndoRestoringAStaleCheckIn() {
        val today = java.time.Instant.ofEpochMilli(now).atZone(INDIA_ZONE).toLocalDate().toString()
        val first = applyDetection(on.copy(checkIns = listOf(CheckIn(today, now - 1000, noSpend = true))), ParsedPayment(24000, "Swiggy"), now) as DetectionOutcome.Added
        val second = applyDetection(first.state, ParsedPayment(5000, "Uber"), now + 1000) as DetectionOutcome.Added
        val kept = undoAutoExpense(keepAutoExpense(second.state, first.expense.id), second.expense.id, now + 1001)
        assertTrue(kept.checkIns.isEmpty())
        val released = releaseExpiredHolds(second.state, now + AUTO_HOLD_MS)
        assertTrue(undoAutoExpense(released, second.expense.id, now + AUTO_HOLD_MS).checkIns.isEmpty())
        // Snapshot/Undo may run after expiry before Android schedules the release worker.
        assertTrue(undoAutoExpense(second.state, second.expense.id, now + AUTO_HOLD_MS).checkIns.isEmpty())
    }

    // ---- The circle only sees a purchase after its undo window ----------------------------------------------

    @Test fun heldPurchaseIsLeftOutOfTheSnapshotUntilTheHoldEnds() {
        val out = applyDetection(on, ParsedPayment(24000, "Swiggy"), now) as DetectionOutcome.Added
        val during = snapshotFor(out.state, Visibility.FULL_DETAILS, now + 60_000)
        assertTrue(during.expenses.isEmpty()); assertEquals(0L, during.dailySpentPaise)
        val after = snapshotFor(out.state, Visibility.FULL_DETAILS, now + AUTO_HOLD_MS)
        assertEquals(1, after.expenses.size); assertEquals(24000L, after.dailySpentPaise)
        // Local-only fields never leave the phone.
        assertEquals("", after.expenses[0].source); assertEquals(0L, after.expenses[0].heldUntil); assertFalse(after.expenses[0].needsCategory)
    }

    @Test fun keepSharesStraightAway() {
        val out = applyDetection(on, ParsedPayment(24000, "Swiggy"), now) as DetectionOutcome.Added
        val kept = keepAutoExpense(out.state, out.expense.id)
        assertEquals(1, snapshotFor(kept, Visibility.FULL_DETAILS, now + 1).expenses.size)
        assertTrue(heldExpenses(kept, now + 1).isEmpty())
    }

    @Test fun expiredHoldsAreReleased() {
        val out = applyDetection(on, ParsedPayment(24000, "Swiggy"), now) as DetectionOutcome.Added
        assertEquals(out.state, releaseExpiredHolds(out.state, now + 1))
        assertEquals(0L, releaseExpiredHolds(out.state, now + AUTO_HOLD_MS).expenses[0].heldUntil)
    }

    @Test fun backupLeavesOutDetectionWorkingData() {
        val asked = applyDetection(upsertMerchantRule(on, "Swiggy", "Food", "Social"), ParsedPayment(5000, "Ramesh Stores"), now) as DetectionOutcome.Asked
        val restored = StateCodec.decode(StateCodec.encodeBackup(asked.state))
        assertTrue(restored.pendingDetections.isEmpty()); assertTrue(restored.recentDetections.isEmpty()); assertEquals(1, restored.merchantRules.size)
    }

    // ---- Impulse habit --------------------------------------------------------------------------------------

    @Test fun impulseStatsCountTheWeekAndTheRunOfFreeDays() {
        val today = LocalDate.parse("2026-11-13") // a Friday
        val expenses = listOf(
            Expense(date = "2026-11-02", amountPaise = 100, reason = "Essential"),
            Expense(date = "2026-11-08", amountPaise = 900, reason = IMPULSE),   // last week (Sunday)
            Expense(date = "2026-11-10", amountPaise = 500, reason = IMPULSE),   // this week (Tuesday)
            Expense(date = "2026-11-12", amountPaise = 300, reason = "Social")
        )
        assertEquals(ImpulseStats(500, 1, 3), impulseStats(expenses, today))
    }

    @Test fun impulseFreeRunStartsAtTheFirstLoggedDay() {
        val today = LocalDate.parse("2026-11-13")
        assertEquals(ImpulseStats(0, 0, 0), impulseStats(emptyList(), today))
        assertEquals(ImpulseStats(0, 0, 3), impulseStats(listOf(Expense(date = "2026-11-11", amountPaise = 100)), today))
        assertEquals(0, impulseStats(listOf(Expense(date = "2026-11-13", amountPaise = 100, reason = IMPULSE)), today).freeDays)
    }

    @Test fun snapshotCarriesImpulseTotals() {
        val today = java.time.Instant.ofEpochMilli(now).atZone(INDIA_ZONE).toLocalDate().toString()
        val s = on.copy(expenses = listOf(Expense(date = today, amountPaise = 700, reason = IMPULSE)))
        val snap = snapshotFor(s, Visibility.TOTALS_STATUS, now)
        assertEquals(700L, snap.impulseWeekPaise); assertEquals(1, snap.impulseWeekCount); assertEquals(0, snap.impulseFreeDays)
    }
}
