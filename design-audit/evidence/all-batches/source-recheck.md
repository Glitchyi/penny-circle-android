# Scoped fix regression and candidate-ledger review

Reviewed the existing batch 2–7 finding causes against the expanded implementation authorization in `brand-contract.md`. This review preserves the historical audit and its blocked checks. It does not declare the application or the complete accessibility matrix clean.

`proposed-fixed.jsonl` contains **115 candidate source-resolved findings** from **121** finding IDs in those batches. `uncovered-findings.json` lists the remaining **six owner-deferred findings** with their reasons. The final `design-audit/fixed.jsonl` was not changed.

## Brand and accessibility source recheck

- Primary controls use the selected accent/onAccent pair, provide the approved action text style, retain focus indication and render disabled state. The unnamed sheen and extra decorative accent are removed. `contrast.json` records passing four-theme primary text pairs and selected subtitle/keypad focus samples.
- Selected onboarding subtitles use the normal text role. Theme semantic pairs switch atomically, and child color animation state resets on theme/reduced-motion changes. Startup titles expose headings.
- Import selection is a single named checkbox row. OCR completion/failure retain polite status semantics. Preset default names contain their visible commands. Form errors have input/group placement and shared polite error semantics; source implementation does not establish actual TalkBack announcement timing.
- Required-field conventions cover Circle create/join/suggestion, onboarding profile/limits, Settings limits, purchase amount/details, shop-rule and preset forms. Existing optional merchant/note labels remain explicit. The last three shared-hint insertions were at **2026-11-13 19:41:29 UTC**.
- App-owned labels, status/error templates and quantities are catalog-backed at their UI boundaries. Complete-day and unreadable-local-state domain outcomes now map to catalog resources. Internal protocol/discriminator values, user data and third-party notices retain their appropriate representation.
- Circle copy names the current destination, accurately includes held Auto purchases in full-details sharing, states the same-identity rejoin restriction and separates readiness to share from actual peer receipt. Recovery paths preserve pending/local data and display actionable next steps.
- Settings arrival/grouping, no-results OCR recovery, empty transfer guidance, notice titles/retry copy, specific command labels and preset-cap explanation match the scoped findings.

The source review identified and the authorized follow-up corrected the remaining TodayParts raw-white fill/content overrides, complete-day/load-reason catalog boundaries and three missing required-field hints. Reopening the current Settings metadata code confirmed the local letter-spacing override was already absent; no additional typography scale was introduced.

## Files inspected

Shared UI and accessibility: `AccentButton.kt`, `AddSheetParts.kt`, `Components.kt`, `GlassSurface.kt`, `Theme.kt`, `ThemePicker.kt`, `Motion.kt`, `StartupSurface.kt`, `RotaryDial.kt`, `FinishStamp.kt`, `RocketCelebration.kt`, `ActivityPager.kt`, `TodayHero.kt`, `TodayParts.kt`.

Forms/navigation/state: `AppNav.kt`, `Screens.kt`, `ImportCandidateEditor.kt`, `PurchaseDetailsSheet.kt`, `AutoDetectScreen.kt`, `PresetsScreen.kt`, `SettingsScreens.kt`, `OnboardingFlow.kt`, `PrivacyLicensesSheet.kt`, `PresetCopy.kt`, `CompletionCopy.kt`, `LocalOperation.kt`, `CircleSetup.kt`, `CircleScreen.kt`, `CircleSettings.kt`, `CircleCommon.kt`.

Boundaries/domain and resources: `MainActivity.kt`, `BuddyRepository.kt`, `NotificationRouting.kt`, `ReminderWorker.kt`, `DetectionNotifications.kt`, `FinishCountdownNotifications.kt`, `BuddyLinkService.kt`, `RecoveryOperation.kt`, `StateStorage.kt`, `ScreenshotImporter.kt`, `ImportReviewMessages.kt`, `PaymentCopy.kt`, `PeerSyncManager.kt`, `JoinAttemptGate.kt`, `CircleRuntime.kt`, `CircleErrors.kt`, core `StateCodec.kt`, `ImportReview.kt`, `PurchaseValidation.kt`, `SyncProtocol.kt`, `Circle.kt`, and the affected Android string/plural catalogs. Candidate records list the exact repository paths associated with each finding.

## Automated evidence and practical limits

The consolidated baseline recorded in `unit-results.json` is **264 passed, one optional online test skipped**, with no failures/errors. The reported release lint result is **zero errors, 69 warnings and two hints**; debug and unsigned release assemblies passed (`gradle-resume.log`). The final focused rerun after the small authorized source/catalog corrections is tracked separately by the build owner. These baseline results alone do not attest to the later APK hashes.

The read-only APK inspection in `artifacts.json` belongs to its recorded hashes. It confirmed packaging, alignment, notices and the requested MainActivity DEX patch on those APKs. Re-run it with the final build owner's explicit `--expected-debug-sha256` to associate evidence with a newer correction APK.

Physical-device TalkBack, haptic quality, OEM payment delivery/Doze, physical frame-rate behavior, exhaustive keyboard traversal and the complete four-theme/viewport/state matrix remain evidence-limited. Existing historical blocked cells are not converted to passes by this source recheck. Runtime workers' actual observations belong in their separate evidence.

## Owner decisions retained

- `visual-1-002`, `visual-2-002`: approved hard-offset shadow dimensions/depth remain open.
- `visual-1-005`, `visual-2-005`: canonical role radii remain open.
- `visual-1-008`, `visual-2-007`: broad amount/keypad/stamp typography scale remains open; fitting and existing-role improvements do not wholly resolve these findings.

Logo family, global spacing/shadow/radius/type scales and the canonical icon family retain their undecided contract status. No new choice is inferred from this review.
