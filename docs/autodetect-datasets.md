# Public SMS datasets for Auto Detect

Research checked 13 November 2026. Goal: improve parser regression coverage for incoming Indian payment messages while testing false positives against ordinary SMS, spam, OTPs, refunds and failed/future transactions. This is a dataset review only: no datasets were downloaded or added to the app.

## Recommended sources

| Dataset | What it contains | License and access | Fit and caveats |
| --- | --- | --- | --- |
| [Indian Payment SMS Corpus](https://github.com/skyaara/indian-payment-sms-dataset) | 172 authored, synthetic/redacted Indian bank, UPI, wallet, ATM, transfer and card SMS cases. Each row has raw text plus expected `bookable`, debit/credit direction, currency, paise amount and related fields. Includes 67 hard-negative/future-event cases; numeric variants are augmented forms, not independent real observations. | CC BY 4.0 for corpus and docs. The [JSON file](https://raw.githubusercontent.com/skyaara/indian-payment-sms-dataset/main/data/indian-payment-sms-dataset.json) is directly available from GitHub without Kaggle account/API setup. Attribute as the repository requests. | Best first choice for deterministic parser regression tests: small, Indian-format and field-labeled, with explicitly synthetic/redacted provenance. It cannot establish performance on real bank messages. The repository says source grammars informed the rewrites and maintains a source ledger; do not copy upstream text/code under this dataset license. |
| [UCI SMS Spam Collection](https://archive.ics.uci.edu/dataset/228/sms%2Bspam%2Bcollection) (also mirrored on [Kaggle](https://www.kaggle.com/datasets/uciml/sms-spam-collection-dataset)) | 5,574 raw English messages with ham/spam labels (4,827 ham, 747 spam). It is general SMS, not payment-labelled data. UCI documents sources including public Grumbletext spam reports and a subset of the NUS SMS Corpus. | CC BY 4.0 according to UCI. UCI lists a 198.6 KB download and provides a public repository download / `ucimlrepo` access. Kaggle is a mirror; use UCI as the canonical license and provenance reference. | Best small broad-negative set for checking that conversational ham, old-style ads and spam do not become expenses. It is an older, English-only corpus from outside India, so it will not cover current Indian SMS/app formats. Some examples contain personal-looking identifiers; review/redact before committing fixture text. |

## Larger or less certain candidates

### FinEE (Hugging Face)

[Ranjit0034/finee-dataset](https://huggingface.co/datasets/Ranjit0034/finee-dataset) is the canonical dataset card; [Siddhu077/some](https://huggingface.co/datasets/Siddhu077/some/tree/main) is a mirror of it, not an independent corpus. The card lists 152,519 rows, Apache-2.0, and 220 MB total (the `data/train.jsonl` file alone is listed as 198 MB). It describes labels for amount, debit/credit, bank, merchant/beneficiary, P2M/P2P, category, and transaction status. The card claims 2,419 manually verified, anonymized ICICI SMS, 100,000 grammar-generated examples and 50,100 multilingual synthetic examples; it reports English, Hindi, Tamil, Telugu, Bengali and Kannada. The card also describes messages from SMS, email and notifications, so source/channel composition needs filtering before SMS-only evaluation.

This is the most field-rich candidate, but exceeds a small-download budget. Its Apache-2.0 statement and provenance are owner claims; the card does not give a detailed collection, consent, or de-identification method for the 2,419 claimed real messages. Do not add real-message rows to the repository without reviewing the actual records for personal data and establishing their redistribution basis. Given the size and provenance caveat, it is not needed for the first parser test expansion. The original HF card, not the mirror, should be checked before any future use.

### Indian Telecom SMS Spam Collection (GitHub/Kaggle)

The [repository owner page](https://github.com/junioralive/india-spam-sms-classification) describes over 2,000 raw SMS rows in `Msg` / `Label` form, binary spam/ham labels, and an MIT license; it links to a Kaggle listing and provides a [direct CSV URL](https://raw.githubusercontent.com/junioralive/india-spam-sms-classification/main/dataset/spam_ham_india.csv). This could provide more Indian-context negative examples than UCI, but it is not labelled for payment direction or amount. The page links a contribution form but does not document consent terms, collection method, PII scrubbing, or class counts. Keep it as an audit candidate rather than importing it automatically; inspect data and licensing provenance first.

## Suggested evaluation use

1. Start with the 172-row synthetic Indian corpus and map its expected labels to parser outcomes: book only completed, clear debits; do not book credits, refunds, reversals, failed/declined, future or non-financial messages. Verify exact paise amounts where applicable.
2. Add a small, reviewed subset of UCI ham/spam as negative regression cases. Do not use it as a proxy for Indian bank-message coverage.
3. Keep a separate consented, redacted real-device validation set out of Git. The public sources reviewed here do not provide a sufficiently documented, clearly licensed corpus of real Indian bank/payment SMS with field-level expected labels. Synthetic cases and a bank-format corpus run cannot certify parsing for a user's bank or payment app.
4. Preserve the current privacy boundary: test messages are local fixtures; do not upload inbox messages or screenshots. Avoid downloading large corpora unless a later task sets a size budget and checks provenance first.

## Sources checked

- Indian Payment SMS Corpus owner README and CC BY 4.0: <https://github.com/skyaara/indian-payment-sms-dataset>
- Its synthetic JSON and schema: <https://raw.githubusercontent.com/skyaara/indian-payment-sms-dataset/main/data/indian-payment-sms-dataset.json>
- UCI SMS Spam Collection description, 5,574 instances, source history and CC BY 4.0: <https://archive.ics.uci.edu/dataset/228/sms%2Bspam%2Bcollection>
- UCI dataset file page / listed archive size: <https://archive-beta.ics.uci.edu/dataset/228/sms%2Bspam%2Bcollection/files>
- Kaggle mirror: <https://www.kaggle.com/datasets/uciml/sms-spam-collection-dataset>
- FinEE original card (data sources, schema, splits, license, size): <https://huggingface.co/datasets/Ranjit0034/finee-dataset>
- Indian Telecom SMS Spam Collection owner README / license / direct raw CSV: <https://github.com/junioralive/india-spam-sms-classification>
