# Public release audit — 2026-11-13

Read-only review of the current repository and eight reachable commits through `02a94f2`, before the publication commit. No signing directory, password file, device, inbox, ignored capture, or ignored raw SMS corpus was accessed. No build or runtime test was run by this audit.

## Findings

- Pattern scan of 148 tracked and untracked nonignored paths, and text tracked in all eight reachable commits, found no obvious private-key PEM block, AWS access-key identifier, GitHub token, Google API key, or quoted secret assignment. The sole current generic assignment match was the `numberToken` parser expression in `Detection.kt`, a false positive. No tracked/history signing-key, database, runtime-log or APK filename was found. This bounded heuristic review is not a guarantee that all secrets are absent.
- `.gitignore` now excludes `*.p12`, `*.pfx`, and `*.key` as well as keystores, builds, captures and release outputs.
- Commit metadata contains historical author identities/email addresses. Publishing history exposes those existing identities; no history rewrite was performed.
- The vendored Indian Payment SMS Corpus has the documented SHA-256 `c66af286a002f0d38da97fb990862484616a3f06483f0b0609a4acf17328b570`. The test-only README supplies attribution, pinned provenance, a CC BY 4.0 link and synthetic-data limitations. Upstream [pinned license](https://github.com/skyaara/indian-payment-sms-dataset/blob/ef81c756ddd8c8f5ba09f1b00820700108f9c84c/LICENSE) confirms CC BY 4.0. Raw UCI corpus is ignored and was not inspected.
- Existing screenshot documentation contains app spending totals. The inspected Activity image shows generic categories, with no payment account identifiers. Visual appearance alone cannot establish that every historical screenshot is synthetic. Preserve only intentionally public documentation screenshots; captures remain ignored.
- No Android `Log`, `println`, `printStackTrace` or Timber usage was found in runtime source by the bounded text search.

## Distribution notices

The initial missing font/dependency notice finding has been addressed in source assets: `app/src/main/assets/licenses/index.txt` indexes complete font OFL, Apache, iroh MIT/Apache, JNA Apache/libffi, ZXing notices and Google SDK/ML Kit/API terms. The bounded release package inventory contains 119 external Maven coordinates and 51 original archive license/notice entries. See [third-party-notices.md](third-party-notices.md) for exact provenance, licensing distinctions and limitations. The bundled font metadata identifies copyright 2024; the packaged OFL comes from the official 1.8.0 release rather than current main's newer copyright. No Penny Circle source license was selected.

The release owner verified the assets reader on the debug build and all 15 assets in the signed APK. This work did not independently reconstruct all native transitive source licenses and is not a legal certification. Public privacy wording now distinguishes on-device ML Kit inputs/outputs from Google performance/utilization telemetry and possible update/compatibility requests. Those later implementation checks were performed by the release owner, not this bounded audit.

## Public claims

Public documentation describes opt-in local parsing, the separate ten-minute undo/sync hold, approximate background delivery and early-beta/security-review limitations. The budget notification defaults to two minutes and can be set to one, two or five minutes; changing that display preference does not change the hold. Timer preferences remain local. The finish-reminder timer can start 15, 30 or 60 minutes before the fixed 10 PM IST reminder; the next-day 10 AM grace is separate.

The release owner has verified actual Live Update promotion on an API 36.1 emulator. That result does not establish promotion or delivery on Xiaomi/OnePlus devices or compliance with Google Play promotion policy; those remain unresolved. Final validation counts, signing evidence and release limitations belong in BUILD-EVIDENCE.md and README. No claim of live-bank template certification or independent security audit should be added. This update records the owner's later checks without extending the original eight-commit secret-scan scope.

## Publication follow-up

The release owner additionally scanned 154 current nonbinary text paths for private-key blocks and common credential identifiers, finding no matches; no tracked key/password/database/capture/release-output paths were present. This is another bounded heuristic check, not a security certification.

Source/tag `v0.3.0` identifies `46b4ffe5b9cb696f9aaf3a580a5a9a5ead3e4b01`. The public repository is now [Glitchyi/penny-circle-android](https://github.com/Glitchyi/penny-circle-android), and its [0.3.0 release](https://github.com/Glitchyi/penny-circle-android/releases/tag/v0.3.0) contains only the signed ARM64 APK, SHA-256 file and public certificate/build metadata. Private signing files and captures remain excluded. The final APK contains all 15 bundled notice assets; its signature, four ARM64 ELF libraries and ZIP alignment passed validation. An unauthenticated download matched the local APK's SHA-256. See `BUILD-EVIDENCE.md` for exact artifact identity and test limitations.
