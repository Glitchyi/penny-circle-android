# All-batch design-fix verification — 14 November 2026

Branch: `codex/design-fix-batch-1`. All eight batches authorized where possible. Application source is committed as `a528e1e0b7fd0b96bb3ebac5c05c044add2542a5`; the owner subsequently authorized release 0.3.1/code 4. Publication status is recorded in the release handoff. Original design-audit findings/verdicts are historical; this is a scoped implementation check, not a fresh whole-app audit. Six broad scale-dependent finding IDs and physical-device limitations remain explicitly open in `../../DEFERRED.md`.

## Automated consolidated suite

JDK21, Gradle max-workers1, Xmx1536m; core/app tests, release lint, debug and unsigned release assembly were run together after integration, with the ignored UCI SMS corpus path configured. Core175 cases:174passed and one optional online iroh skip. App90cases passed. Total264 passed/one skip /zero failures or errors. No separate Android instrumentation suite is configured.

The first suite found15 Circle string-format lint errors (numeric arguments passed to `%s`). They were corrected to `%d` without suppression, then lint/build tasks resumed successfully. Release lint:zero errors, 69 warnings,two hints at that checkpoint. Warnings remain and are not claimed fixed. See gradle-first.log/json, lint-first-errors.json, gradle-resume.log/json and unit-results.json.

Final UI/source reviewers found a native purchase-sheet Back callback issue, a remaining Today stat-panel white tint, and small required-field/catalog boundary gaps. These were corrected within the audited scope; focused app test/lint/debug/release tasks passed in gradle-corrections.log/json. The versioned production pipeline then reran required tests/lint and release assembly before signing (release-0.3.1.log/json). Final counts remain264 passed/one skip and lint zero errors/69 warnings/two hints. The corrected native Back emulator retest passed, including IME-first handling, draft close/reopen and one visible keyboard focus sample.

## Contrast, artifacts and source review

Official design-grill contrast checker passed all sampled shared theme pairs: Light primary5.84:1, Violet5.56, Mint11.56, Signal6.78; focused keypad text/surface2>=14.62. Light selected choice subtitle16.75. See contrast.json. These are actual token-pair checks, not exhaustive physical focus/TalkBack results.

Independent artifact verification confirmed canonical package/name/version/min33/target36; nondebuggable ARM64-only signed 0.3.1/code 4 release; all four native ELF load alignments>=16KB and ZIP16KB alignment; unchanged15 notice assets; no matching sensitive/private-key filenames or PEM assets. Debug is signed with Android Debug certificate and includes ARM64+x86_64. APK DEX verifies consumed destination removal and restored share-key handling. Debug x86_64 native alignment limitations remain separate from production ARM64 checks. Production v3 signing verified the existing certificate `84e2559af0440e8a514d154904b9a2a203b617a204c8c46968cb3d94782d98fd`. Final signed APK: 77,203,821 bytes, SHA256 `7e02fabd51a36084ca9997658ec4feb2f5ab9d5b22ebc4bf0ab4364f11d25c19`. Corrected UI debug:123,261,452bytes, SHA256 `4dd85b7004e7d745a2039017b802b9add7493ccfb2af6923e2c3abe97204ed7c` (version0.3.0; release business source matches, with version-only0.3.1 bump and final required-field hints). Final checks are in artifacts.json; preceding unsigned artifacts remain in artifacts-pre-release.json. Source identity is external metadata, not an embedded revision asset.

Scoped source regression reviews covered Circle cancellation/generation/action safeguards, recovery ownership and protected atomic persistence, import validation/drafts, shared theme/accessibility roles and catalog boundaries. Findings fixed are recorded separately in ../../fixed.jsonl. No complete runtime assistive-tech/viewport/OEM matrix was established.

## Synthetic emulator checks

Two separate API36/API36.1 emulators run temporary synthetic QA profiles. Their original paired synthetic profiles were snapshotted on-device before testing (hashes in original-snapshots.json). No physical phone, private purchases, old SMS inbox or signing secrets were read.

Admin checks passed exact limits/draft Back-reopen, controlled local write-failure retention/retry, protected recovery Close/reopen, fresh-start cancellation/confirmation, malformed/newer backup rejection, valid restore, preserved original corrupt bytes and warm Activity/Add routing. See admin-ui/results.md and captures. No slow/cloud document-provider race was claimed as a UI test; operation ownership is covered by tests/source review.

Member checks cover onboarding field errors, permission Review, payment demos and large-font amounts; final report is in member-ui/results.md. Real temporary Circle create/invite/join, pending reopen/Check again, Cancel/rejoin/Approve, safe confirmation cancels and bidirectional basic snapshots passed. An initial peer connection error recovered through Retry; see circle-results.md. This is bounded evidence, not an exhaustive sync matrix.

Both original profile files were restored byte-for-byte before any restart, with matching original hashes; display1080x2400/density420, font1.0 and animator null restored. See restoration.json. Both emulators were then closed gracefully. Original on-device snapshots remain intact. Corrected Back/IME/draft and keyboard focus checks passed in member-ui/corrected-back-results.md.

The final ledger contains126 source-resolved finding records; six owner-dependent broad findings remain in DEFERRED.md. Local PNG/XML captures and raw logs remain working evidence rather than public release assets.

## Remaining decisions and unavailable evidence

Logo/radius/shadow/figure-scale choices remain open. F-Droid deferred. Physical TalkBack/haptic quality, real OEM/payment delivery, Doze and measured frame timing/FPS are not proven by emulator screenshots. The original audit's3062blocked checks remain historical blocked verdicts; this pass does not automatically convert them into passes.
