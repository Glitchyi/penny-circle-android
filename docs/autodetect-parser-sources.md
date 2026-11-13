# Public sources for payment-message parser coverage

Reviewed 2026-11-13. This note records what official Indian bank/provider sources publicly document about SMS alerts. It does not claim that the examples below represent the exact live SMS payloads delivered to customers. Fixtures are synthetic and redact or replace identifiers and values.

## Published examples and documented structures

1. **SBI e-mandate due alert (published template; scheduled debit, not a completed purchase).** SBI shows a template with a customer greeting, merchant, mandate reference, rupee amount, due date, and debit-card suffix. Synthetic fixture: `Dear Customer, Shop e-mandate (SI1234567890) payment for Rs. 125.00 is due on 01/01/2027 and will be processed through SBI Debit Card ending 1234.` This should not be interpreted as evidence that a purchase has already completed. [SBI e-mandate FAQ](https://sbi.co.in/web/personal-banking/faq-on-e-mandate-on-debit-cards)

2. **SBI unauthorized-debit reporting suffix.** An SBI security booklet says transaction SMS messages carry an additional fixed fraud-reporting suffix directing a customer who did not make the transaction to forward the SMS. This is useful as a suffix/noise case, but the booklet does not publish the core debit-message body. Synthetic fixture: `Rs. 125.00 debited at SHOP from A/c XX1234. If not done by you, forward this SMS ...` [SBI security booklet (PDF)](https://sbi.co.in/documents/136/1364568/12122022_Hive%2BBooklet%2BSep%2B2022.pdf/ac3c1e9d-31f1-2423-ac8c-d8ac3539ac7d?t=1670848428091)

3. **Axis account-status SMS (published example; not a payment).** Axis publishes an account-closure notification example containing an account suffix, effective date, customer-request context, and bank signature. It is a useful negative example for distinguishing account status alerts from purchase alerts. Synthetic fixture: `A/c XX1234 closed effective 01-01-2027 on your request. - Axis Bank` [Axis Bank support FAQ](https://uat.axisbank.com/support/citi-account-operation)

4. **HDFC credit-card transaction-alert scope (no body template).** HDFC states these alerts cover approved/successful domestic and international card transactions and says declined transactions do not get these alerts. This supports a success-vs-decline test requirement, but does not supply wording, field order, or sender IDs. [HDFC transaction alerts FAQ/PDF](https://www.hdfcbank.com/content/bbp/repositories/723fb80a-2dde-42a3-9793-7ae1be57c87f/?path=%2FPersonal%2FPay%2FCards%2FCredit+Card%2FCredit+Card+Landing+Page%2FTransactionAlerts.pdf)

5. **ICICI debit-card alerts (no body template).** ICICI says debit-card transactions generate SMS and email alerts, and its FAQ notes a transit-card auto-recharge SMS is sent after the linked bank account is debited. No message body template is published on this page. [ICICI debit-card FAQs](https://www.icicibank.com/personal-banking/faq/card/debit-cards-faqs)

6. **Canara Bank card-alert scope (no body template).** Canara's debit-card policy says customers can register for SMS transaction alerts, but does not publish sample message wording. [Canara Bank debit-card issuance policy (PDF)](https://www.canarabank.com/UploadedFiles/Pdf/DEBIT_CARD_ISSUANCE_POLICY_FY_2023-24_with_MITC.pdf)

7. **PNB IMPS confirmation behavior (no body template).** PNB says the remitting bank sends confirmation to the remitter and the beneficiary bank sends a credit confirmation to the beneficiary, according to their SMS-alert policies. It does not specify a common body template. [PNB IMPS FAQ](https://www.pnbindia.in/imps.html)

## Coverage limits

The official sources reviewed do **not** provide enough exact successful-payment SMS bodies to substantiate a multi-bank parser corpus. In particular, official HDFC, ICICI, Canara, and PNB references here describe alert behavior or scope rather than field-level templates. The available SBI template is for a future mandate debit, while the available Axis sample is account closure; neither is a completed shop payment. Do not label locally authored examples for these banks as bank-sourced samples. Real received SMS and payment-app notification payloads still need user-provided, redacted samples or controlled device testing before claiming format coverage.

The SBI scheduled-mandate and Axis closure fixtures should remain negative cases for automatic purchase creation. The SBI fraud suffix can be used to test parser tolerance, but the synthetic core debit wording is not bank-authentic.
