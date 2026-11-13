package com.spendingbuddy.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Synthetic representative examples for parser regression coverage. These are invented fixtures, not
 * messages collected from customers and not certification of any bank or wallet's current templates.
 */
class BankSmsCorpusTest {
    private data class Case(val label: String, val body: String, val paise: Long, val merchant: String, val kind: PaymentKind)

    private val completedPayments = listOf(
        Case("SBI UPI P2M", "SBI: A/c XX1234 debited by Rs 1.00 on 13-11-26 UPI/P2M/123456789012/TEA SHOP. Ref 987654321012", 100, "Tea Shop", PaymentKind.UPI_MERCHANT),
        Case("HDFC card", "HDFC Bank Card XX1234 spent INR 12,345.67 at BOOKMYSHOW on 13/11/26", 1_234_567, "Bookmyshow", PaymentKind.CARD),
        Case("ICICI UPI", "ICICI: Rs.89.50 debited from A/c XX1234 to ZOMATO on 13-Nov-26 UPI Ref 123456789012", 8_950, "Zomato", PaymentKind.UNKNOWN),
        Case("Axis UPI merchant", "INR 245.00 debited A/c XX1234 13-11-26 UPI/P2M/123456789012/BLINKIT Axis Bank", 24_500, "Blinkit", PaymentKind.UPI_MERCHANT),
        Case("Kotak merchant VPA", "Sent Rs.240.00 from Kotak Bank AC X1234 to swiggy@icici on 13-11-26 UPI Ref 123456789012", 24_000, "Swiggy", PaymentKind.UNKNOWN),
        Case("PNB card", "PNB: Rs 1,099.00 spent on card xx1234 at MYNTRA on 13-11-26", 109_900, "Myntra", PaymentKind.CARD),
        Case("Bank of Baroda UPI", "A/c XX1234 debited by INR 76.25 on 13-11-26 for UPI/P2M/123456789012/DMART - BOB", 7_625, "Dmart", PaymentKind.UPI_MERCHANT),
        Case("Canara UPI", "Canara Bank: Rs.500 debited from A/c xx1234 to UBER on 13-11-26 UPI Ref 123456789012", 50_000, "Uber", PaymentKind.UNKNOWN),
        Case("Union Bank card", "INR 2,499.99 spent on Union Bank Card XX1234 at CROMA", 249_999, "Croma", PaymentKind.CARD),
        Case("IDBI UPI", "IDBI Bank A/c XX1234 debited by 150.00 on 13/11/26 trf to METRO Refno 123456789012", 15_000, "Metro", PaymentKind.UNKNOWN),
        Case("IndusInd card", "Spent Rs.799.00 on IndusInd Bank Card 1234 at SPOTIFY on 13/11/26", 79_900, "Spotify", PaymentKind.CARD),
        Case("YES Bank UPI", "YES BANK: Rs 320.50 debited from a/c xx1234 to RAPIDO on 13-11-26 UPI Ref 123456789012", 32_050, "Rapido", PaymentKind.UNKNOWN),
        Case("Federal Bank UPI", "Federal Bank: A/c XX1234 debited by INR 60.00 on 13-11-26 UPI/P2M/123456789012/CHAI POINT", 6_000, "Chai Point", PaymentKind.UPI_MERCHANT),
        Case("IDFC FIRST card", "INR 4,000 spent on IDFC FIRST Bank Card ending 5678 at HOTEL TAJ on 13/11/26", 400_000, "Hotel Taj", PaymentKind.CARD),
        Case("AU Small Finance UPI", "AU Bank: Rs.35.75 debited from A/c XX1234 to ZEPTO on 13-Nov-26 UPI Ref 123456789012", 3_575, "Zepto", PaymentKind.UNKNOWN),
        Case("GPay notification", "Paid ₹240 to Swiggy", 24_000, "Swiggy", PaymentKind.UNKNOWN),
        Case("PhonePe notification", "You paid Rs. 1,250.00 to BigBasket", 125_000, "BigBasket", PaymentKind.UNKNOWN),
        Case("Paytm notification", "Paid INR 55.50 at DMRC METRO", 5_550, "Dmrc Metro", PaymentKind.UNKNOWN),
        Case("Amazon Pay notification", "Sent Rs 999.00 to AMAZON", 99_900, "Amazon", PaymentKind.UNKNOWN),
        Case("merchant field delimiter", "Rs.40.00 paid to Cafe Coffee Day on 13/11/26. UPI transaction successful", 4_000, "Cafe Coffee Day", PaymentKind.UNKNOWN),
        Case("ATM cash", "Rs.2,000.00 withdrawn from A/c XX1234 at ATM on 13-11-26. Available balance Rs 8,500", 200_000, "", PaymentKind.CASH),
        Case("person UPI", "Rs 750.00 sent to 9876543210@ybl on 13/11/26 UPI Ref 123456789012", 75_000, "", PaymentKind.UPI_PERSON)
    )

    @Test fun syntheticBankAndWalletCorpusPreservesExactAmountAndSafeMerchant() {
        completedPayments.forEach { sample ->
            val parsed = PaymentMessageParser.parse(sample.body)
            assertTrue("${sample.label} should parse", parsed != null)
            assertEquals(sample.label, sample.paise, parsed!!.amountPaise)
            assertEquals("${sample.label} merchant", sample.merchant, parsed.merchant)
            assertEquals("${sample.label} kind", sample.kind, parsed.kind)
        }
    }

    @Test fun surroundingBalanceAndLimitValuesDoNotReplacePurchaseAmount() {
        val samples = listOf(
            "HDFC card spent Rs 99.99 at STORE. Avl Bal Rs 1,23,456.78",
            "Your A/c XX123 debited by INR 250.50 on 13-11-26. New Balance: INR 8,765.43",
            "Spent Rs 1,250.00 on Axis Bank Card at SHOP. Available limit INR 95,000.00"
        )
        val expected = listOf(9_999L, 25_050L, 125_000L)
        samples.forEachIndexed { i, body -> assertEquals("sample $i", expected[i], PaymentMessageParser.parse(body)?.amountPaise) }
    }

    @Test fun validIndianAndWesternGroupingAndDecimalPrecisionArePreserved() {
        val samples = listOf(
            "Rs 1,23,456.78 debited to SHOP" to 12_345_678L,
            "Rs 123,456.78 debited to SHOP" to 12_345_678L,
            "Rs 12.3 debited to SHOP" to 1_230L
        )
        samples.forEach { (body, expected) -> assertEquals(body, expected, PaymentMessageParser.parse(body)?.amountPaise) }
        assertEquals(24_000L, PaymentMessageParser.parse("Rs 240.00. Paid to Swiggy")?.amountPaise)
    }

    @Test fun malformedCurrencyTokensAreRejectedRatherThanPartiallyParsed() {
        val malformed = listOf("1.234", "1.2.3", "1,00,00", "12,3.45", "1,23,45.67", "1,234,56.78")
        malformed.forEach { amount ->
            val body = "Rs $amount debited to SHOP"
            assertNull("malformed full amount must be ignored: $body", PaymentMessageParser.parse(body))
        }
    }

    @Test fun nonPaymentsAndAmbiguousPaymentStatesAreRejected() {
        val falsePositives = listOf(
            "HDFC: OTP 123456 for Rs 450 payment to SHOP. Do not share OTP",
            "ICICI: Rs 450 credited to your account as refund for SHOP order",
            "SBI: Payment of Rs 450 to SHOP failed; amount will be reversed",
            "Axis: Rs 450 will be debited tomorrow for SHOP mandate",
            "Kotak: E-mandate registered for Rs 450 at SHOP",
            "PNB: Your Rs 450 payment to SHOP is pending",
            "SBI: Rs 450 paid to SHOP on 13/11/26; status is pending settlement",
            "Axis: Rs 450 to SHOP is not paid yet",
            "Kotak: Rs 450 transaction awaiting confirmation for SHOP",
            "ICICI: Payment of Rs 450 is still processing at SHOP",
            "BOB offer: spend Rs 4,500 and get Rs 450 cashback",
            "Canara: Bill due Rs 450; pay before 20-11-26",
            "Union Bank: RAHUL has requested Rs 450 from you",
            "PhonePe: collect request of Rs 450 from Rahul",
            "GPay: Cashback of Rs 25 received on your last purchase",
            "Paytm: Your payment of Rs 450 was cancelled",
            "Amazon Pay: Rs 450 refunded to your wallet",
            "Dinner? I can pay Rs 450 when we meet"
        )
        falsePositives.forEach { body -> assertNull("must ignore: $body", PaymentMessageParser.parse(body)) }
    }

    @Test fun otpCredentialPromptsAreIgnoredButGenericSecurityReminderDoesNotHidePayment() {
        val credentialPrompts = listOf(
            "Rs 450 debited to SHOP. OTP: 123456",
            "Rs 450 paid to SHOP. Use OTP 123456 to confirm transaction",
            "Rs 450 to SHOP. Enter the OTP to complete this payment",
            "Rs 450 paid to SHOP. Verification code 482913"
        )
        credentialPrompts.forEach { body -> assertNull("OTP credential is not a purchase: $body", PaymentMessageParser.parse(body)) }

        val completed = PaymentMessageParser.parse("Rs 240.00 spent on HDFC Bank Card x1234 at ZOMATO. Bank never asks for OTP. Not you? Call 18002586161")
        assertEquals(24_000L, completed?.amountPaise)
        assertEquals("Zomato", completed?.merchant)
    }

    @Test fun merchantFooterAndLongTailAreNotMistakenForAStore() {
        val bankFooter = PaymentMessageParser.parse("Rs 80 debited from A/c XX1234 to SHOP CAFE. Call customer care 1800123456 for help")
        assertEquals(8_000L, bankFooter?.amountPaise)
        assertEquals("Shop Cafe", bankFooter?.merchant)

        val clippedId = PaymentMessageParser.parse("Paid Rs 80 to VPA merchant123@bank UPI Ref 123456789012")
        assertEquals(8_000L, clippedId?.amountPaise)
        assertEquals("Merchant123", clippedId?.merchant)

        val pathTail = PaymentMessageParser.parse("A/c XX123 debited Rs 120.00 UPI/P2M/123456789012/SWIGGY Ref 987654321012. Call 1800123456")
        assertEquals(12_000L, pathTail?.amountPaise)
        assertEquals("Swiggy", pathTail?.merchant)

        val pathFooter = PaymentMessageParser.parse("A/c XX123 debited INR 90.00 UPI/P2M/123456789012/CAFE NOVA Bank Ref 987654321012")
        assertEquals(9_000L, pathFooter?.amountPaise)
        assertEquals("Cafe Nova", pathFooter?.merchant)

        val tooLong = PaymentMessageParser.parse("Paid Rs 80 to ${"X".repeat(75)} Store")
        assertEquals(8_000L, tooLong?.amountPaise)
        assertTrue(tooLong!!.merchant.length <= 60)
    }
}
