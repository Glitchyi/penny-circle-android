# Public SMS parser evaluation data

This directory is test-only. The copied JSON fixture is not packaged into the Android app, scanned from an inbox,
uploaded, or used as runtime training data.

## Indian Payment SMS Corpus

- Dataset: [Indian Payment SMS Corpus](https://github.com/skyaara/indian-payment-sms-dataset), version 0.2.0
- Author attribution: SplitEasy contributors
- License: [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)
- Pinned upstream commit: `ef81c756ddd8c8f5ba09f1b00820700108f9c84c`
- Upstream Git blob SHA-1: `3992f71e4984ddadbc113ff082b84f47392ab10c`
- Vendored file SHA-256: `c66af286a002f0d38da97fb990862484616a3f06483f0b0609a4acf17328b570`
- File: `indian-payment-sms-dataset.json`
- Contents: 172 SMS rows: 58 seed fixtures and 114 numeric variants, including 78 completed debits and 94 non-bookable cases.

The upstream README and `SOURCES.md` state that each body is an invented, redacted rewrite; no private inbox data or
live account identifiers are included. Examples are not certified or verified templates from any bank or wallet.
Variants are synthetic augmentation, not independent observations. The test normalizes the corpus's literal `\\n`
sequences into line breaks before parsing. It checks the source's explicit `expected.bookable` and
`transaction_amount_paise` labels against `PaymentMessageParser` and `applyDetection`. A completed debit can correctly
be parsed but still require confirmation (cash, person, blank merchant, or large amount); only an `Added` result for
a `bookable=false` row is reported as an unsafe false auto-add.

Attribution: “Indian Payment SMS Corpus” by SplitEasy contributors, CC BY 4.0, copied from the pinned commit above.
The row bodies are authored synthetic rewrites; attribution to this dataset does not imply affiliation with any bank.

## Generic SMS Spam Collection (opt-in local evaluation)

The UCI [SMS Spam Collection](https://archive.ics.uci.edu/dataset/228/sms+spam+collection), DOI
[10.24432/C5CC84](https://doi.org/10.24432/C5CC84), is licensed CC BY 4.0. UCI describes 5,574 volunteer-contributed
ham/spam messages from several sources and says contributors were made aware their contributions would be public.
It is a general spam dataset, not an expense dataset: ham cannot be assumed to be a valid parser negative, and a spam
label does not imply the message is non-payment.

Raw UCI SMS bodies are never copied into tracked source, runtime assets or test output. For an explicitly requested local run, download
the official archive into the ignored `.gradle` cache and evaluate aggregates only:

```powershell
$datasetDir = Join-Path (Get-Location) '.gradle/public-sms-datasets'
New-Item -ItemType Directory -Force -Path $datasetDir | Out-Null
$zipPath = Join-Path $datasetDir 'sms-spam-collection.zip'
Invoke-WebRequest -Uri 'https://archive.ics.uci.edu/static/public/228/sms%2Bspam%2Bcollection.zip' -OutFile $zipPath
Get-FileHash -LiteralPath $zipPath -Algorithm SHA256
Expand-Archive -LiteralPath $zipPath -DestinationPath (Join-Path $datasetDir 'extracted') -Force
Get-FileHash -LiteralPath (Join-Path $datasetDir 'extracted/SMSSpamCollection') -Algorithm SHA256
```

Expected SHA-256 values are `1587ea43e58e82b14ff1f5425c88e17f8496bfcdb67a583dbff9eefaf9963ce3` for the ZIP and
`7d039a24a6083ed9ef0f806ebad56bbb976e3aeb8de05669173bfdc4996c239d` for `SMSSpamCollection`. The extracted file has
5,574 rows: 4,827 `ham` and 747 `spam`.

After verifying the file hash, run the optional JUnit evaluator (it is skipped by default unless the project property
is supplied):

```powershell
.\gradlew.bat :core:test --tests com.spendingbuddy.app.UciSmsSpamEvaluationTest `
  -PpublicSmsUciPath="$((Resolve-Path (Join-Path $datasetDir 'extracted/SMSSpamCollection')).Path)" --console=plain -q
```

The report contains only aggregate message counts, parser-candidate counts, auto-add candidates, and confirmation
candidates grouped by `ham`/`spam`; it prints no SMS text or row IDs. These are candidate counts, not accuracy or
false-positive rates, because UCI does not label messages as payments/non-payments. Kaggle's `uciml` mirror does not
supply a clear license in its listing; UCI is the authoritative licensed source used here.
