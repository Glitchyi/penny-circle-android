# Approved implementation — all eight batches

Owner expanded scope on 14 November 2026 to all batches where possible, with parallel subagents and one consolidated validation pass at the end. Logo/scales and other explicit owner decisions remain open. F-Droid is deferred at the owner's request.

| Batch | State | Owner / boundary |
|---|---|---|
|1 Exact money|Implemented; initial and consolidated checks passed|Eleven records in fixed.jsonl; final suite included these changes again|
|2 Notification routes|Implemented; consolidated checks and scoped emulator/source rechecks passed|Destination identities, warm routes, granted notification Review and consumed launch state|
|3 OCR and save recovery|Implemented; consolidated checks and scoped emulator/source rechecks passed|Editable local OCR review, retries, drafts, suspend error outcomes, protected unreadable-file recovery and shared recovery ownership|
|4 Accessibility and controls|Implemented; consolidated checks and scoped emulator/source rechecks passed|Shared contrast/focus/disabled states, field/selection/status semantics, wrapped/scrollable forms and required hints|
|5 Circle actions|Implemented; consolidated checks and scoped emulator/source rechecks passed|Generation-gated cancellation, parent drafts, confirmations/keyed progress/errors, member retry and honest delivery|
|6 Visual roles/tokens|Actionable changes implemented; owner-dependent choices deferred|Single accent, flat panels, Geist roles, named borders, dynamic motion, chart/amount fitting; see DEFERRED.md|
|7 Copy/status terms|Implemented; consolidated checks and scoped emulator/source rechecks passed|Android catalogs/plurals and safe message boundaries; user data, protocol identifiers and third-party notices preserved|
|8 Consolidated verification|Completed for available automated/emulator checks; real-device matrix remains open|One core/app test, lint, build and emulator validation pass; report real-device/TalkBack/FPS limitations honestly|

Implementation was integrated before one consolidated validation stage; failures and scoped final corrections were rechecked during that stage. The owner subsequently requested release 0.3.1. Application source is committed; the existing production key signs the new distribution. Original v0.3.0 assets remain unchanged.
