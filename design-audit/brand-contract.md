# Brand contract — Penny Circle

**SCOPED RATIFICATION: BATCH 1 ONLY** — Owner approved on 2026-11-13: “Yes approve the contract for batch 1 and go ahead. Leave the logo and scale decisions open for now.” Approval covers exact money parsing/formatting, preserving paise and valid zero limits, safe validation, consistent currency display and fitting the supported amount without clipping. Existing tokens, type families and adaptive behavior apply. Logo and scale decisions, other derived entries and batches 2–8 remain unratified. The original audit was completed against an unratified contract; its results remain historical.

**Expanded implementation authorization — 2026-11-14:** The owner subsequently requested “do all the batches if possible” with multiple subagents, then requested one consolidated validation suite at the end. Implement batches 2–8 against defined contract entries and the approved functional/accessibility requirements. This expands implementation scope without deciding the still-open logo, scale, icon-family or other explicitly undecided design choices. Record those blocked choices rather than inventing authority. Preserve the original audit and record subsequent verification separately. Intermediate source reviews are permitted; defer test/build/emulator validation until implementation is integrated.

## 1. Identity

| Item | Exact form | Status | Source |
|---|---|---|---|
| Product name | Penny Circle | defined | AGENTS.md:1-9; README.md:1-11; app/src/main/AndroidManifest.xml:15 |
| Short name | Penny Circle (no separate short name approved) | defined | README.md:11 |
| Feature names | Today, Activity, Circle; Auto purchase; Totals & status; Full details | defined | README.md:15-28; app/src/main/java/com/spendingbuddy/app/ui/AppNav.kt |
| Tagline | No approved single tagline; onboarding uses YOUR SHARED NOTEBOOK | undecided | app/src/main/java/com/spendingbuddy/app/ui/OnboardingFlow.kt |
| Legal entity/copyright | No approved entity/copyright line. Signing CN is not a public legal entity contract. | undecided | README.md; PRIVACY.md |

## 2. Logo and brand assets

| Asset | File/use | Min size/clear space | Status |
|---|---|---|---|
| Launcher mark | app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml and drawable/ic_launcher_foreground.xml | Adaptive-icon geometry is code-defined; no separate authored clear-space/minimum guide | derived: one launcher source family; no competing approved logo |
| Onboarding letter mark | app/src/main/java/com/spendingbuddy/app/ui/OnboardingFlow.kt | P tile; separate relationship to launcher mark not ratified | derived: one onboarding mark implementation |
| Notification glyph | app/src/main/res/drawable/ic_stat_buddy.xml | Android monochrome small-icon system sizing | derived: one small-icon family; identity coherence undecided |
| ₹ live tracker | app/src/main/java/com/spendingbuddy/app/LiveProgressAppearance.kt | 96px bitmap from32-unit original geometry; selected theme accent/onAccent | defined by latest user ₹/theme instruction and this named implementation |
| Theme-specific logo/reversal/social image | No complete authored family | Owner chooses canonical mark, reversals, minimums and clear space | undecided |

Do not assign S0 merely because multiple existing marks differ: a canonical approved logo has not been established. Record inconsistency/evidence and an owner decision.

## 3. Colour tokens

The named canonical values below are **defined** by app/src/main/java/com/spendingbuddy/app/ui/Theme.kt:130-169. All four source blocks are intentional theme definitions, not incidental raw colors. Category palettes at Theme.kt:53-107 are semantic category colors; danger is also semantic, not a second brand accent.

| Token | Light Horizon | Violet Night | Mint Night | Signal Blue | Role |
|---|---|---|---|---|---|
| bg | #FAFAFB | #0D0B14 | #08090A | #090B11 | App background |
| surface | #FFFFFF | #151220 | #101212 | #10131B | Panels |
| surface2 | #F1F1F5 | #1E1A2D | #181B1B | #171B26 | Raised/secondary panels |
| border | #E3E3EA | #34304D | #2E3434 | #2C3447 | Decorative hairlines; not automatically a compliant control boundary |
| text | #0B0B10 | #EFEDF8 | #F4F7F6 | #EEF2FA | Main content |
| muted | #6B6B7B | #8F89A6 | #8D9593 | #8993A8 | Secondary content |
| accent | #6A3DF5 | #9D6BFF | #00E599 | #4D9BFF | One selected brand accent |
| onAccent | #FFFFFF | #0D0B14 | #03120C | #04101F | Text/icons on accent |
| danger | #B91C1C | #FF6B6B | #FF6B6B | #FF6B6B | Impulse/over-budget/destructive facts, with text/icon |

Accent2/accent3 exist in source but are not blanket permission to introduce extra decorative brand accents. Raw per-screen approximations of these canonical tokens are drift. Native notification system surfaces remain Android-controlled; app-owned accent and ₹ marker follow the user theme. Contrast must use actual composites/states, not just opaque base pairs.

## 4. Typography

Families: Geist for UI; Geist Mono for figures/mono labels. **Defined**: AGENTS.md:15; Theme.kt:176-223.

| Style | Size/line height sp | Weight | Tracking | Role/status |
|---|---|---|---|---|
| displayLarge | 56/62 |600|-.03em|Large headings; defined Theme.kt:204-216 |
| displayMedium |28/34|600|-.03em|Screen titles; defined |
| displaySmall |24/30|600|-.03em|Section display; defined |
| headlineSmall |22/28|600|default|Pushed-page headings; defined |
| titleLarge |20/26|600|default|Title; defined |
| titleMedium |18/24|600|default|Card title; defined |
| bodyLarge |16/22|400|default|Primary body; defined |
| bodyMedium |14/20|400|default|Secondary body; defined |
| labelLarge |14/20|500|default|Actions; defined |
| labelMedium |12/16|500|default|Mono metadata labels; defined |
| Hero amount |44–96sp adaptive; values in Responsive.kt|role-specific|role-specific|Defined explicit responsive exception, not a mandate for all text |

## 5. Scales

| Scale | Steps/policy | Status/source |
|---|---|---|
| Spacing | No central named spacing scale exists. Raw dp census (all literal dimensions, not solely gaps): 8: 91, 48: 66, 12: 56, 16: 53, 4: 45, 6: 40, 10: 33, 14: 32, 18: 30, 20: 30, 1: 28, 2: 27. Do not invent a complete approved spacing scale from these counts. | derived census in token-census.json; owner choice undecided |
| Radius | Named PennyShapes Small8,Medium10,Large12dp. Raw RoundedCornerShape counts: 24: 11, 20: 5, 18: 4, 16: 3, 12: 2, 10: 2, 28: 1, 2: 1, 13: 1, 8: 1, 14: 1, 3: 1. Card24dp and page-specific values compete with named shapes. | named shapes defined Theme.kt:112-116; other usage derived with counts |
| Border |1dp Rule/decorative hairline; selected-chip1.5dp and allowed-permission2dp have distinct state roles | defined shared implementations Components.kt:63-89; OnboardingFlow.kt |
| Shadows |Hard offset on pressable buttons; glass-like nav soft depth explicitly requested. No universal numeric shadow scale. | defined role policy AGENTS.md:15 plus latest user navigation direction; numbers undecided |
| Z layers |Main content < floating primary action/nav < modal sheets/dialogs < Android system surfaces | derived implementation ordering AppNav.kt; no numbered scale approved |
| Breakpoints/width |Max content640dp, compact height<780dp or fontScale>1.3; large-text branches vary by component | defined Responsive.kt:3-12; variant thresholds require review |
| Icon set |PennyIcons plus Material rounded/auto-mirrored icons; canonical family not resolved | derived two source families; size usage must fit role; owner choice undecided |
| Control heights |Shared primary/secondary actions min52dp; header back48dp. Platform default hit slop must be distinguished from drawn glyph size. | defined named controls AccentButton.kt:40-128; Components.kt:39-47 |
| Motion |PaneMotion named directional slides, FastOutSlowIn,300ms; PennyMotion spatial/effects springs; system reduced motion observed | defined NavigationMotion.kt; Theme.kt:192-196,229-244; no global ban on every non-navigation fade |

Additional raw sp counts (not an approved scale): 1: 7, 0.8: 6, 16: 4, 22: 4, 1.2: 4, 28: 3, 1.4: 3, 20: 3, 24: 2, 14: 2, 1.6: 1, 1.8: 1. Full counts and competing values remain in token-census.json.

## 6. Navigation policy

| Decision | Value | Status |
|---|---|---|
| Top-level places |Today,Activity,Circle, persistent bar; Today primary | defined README.md:15-20; latest user name direction supersedes older Buddy wording |
| Max menu depth |Two levels below top-level as rulebook default; deeper visible flows require clear back/title | derived rulebook default; no product-specific approval |
| Max ungrouped items |7 as rulebook default; labeled settings sections may exceed total7 | derived rulebook default |
| Ordering |Task/frequency for places; workflow order for forms; commands grouped by purpose; destructive separated | derived current shared navigation; owner-specific universal ordering undecided |
| Unavailable items |Explain disabled requirement when discoverability matters; hide truly role-inapplicable actions | derived existing role-gated Circle/permission flows; no blanket universal choice approved |
| Submenu indicators |Native forward/expand affordance for nested destinations; no hover-only dependency | derived Android platform adaptation; exact icon unresolved |
| Dialog actions |Direct verb/command labels, no global trailing-ellipsis convention currently established | undecided: avoid inventing ellipsis violation without a defined rule |
| Shortcuts |No keyboard shortcut vocabulary is claimed; all touch tasks need accessible alternatives | derived native phone product scope |
| Small screens |Persistent bottom navigation; sheets/forms scroll; system Back preserves unsaved input when defined | defined AGENTS.md:15-17 and current AppNav.kt |

## 7. Voice and tone

| Attribute | Do | Do not | Status |
|---|---|---|---|
| Friendly/factual |Review and log today's spending |You failed again | defined AGENTS.md:15-23 |
| Honest/private |Nothing is saved until you choose |Messages instantly shared | defined AGENTS.md:27-35 |
| Clear actions |Edit purchase; Save limits |Proceed; Submit when task unspecified | defined familiar action vocabulary in Components/Settings |
| Non-blocking support |Purchase removed with Undo |Require celebration dismissal before continuing | defined AGENTS.md:15 |
| Precise timing |10 PM reminder; next-day10 AM IST grace |Time is up implying logging blocked | defined README.md:17-20 |

Errors explain the issue and recovery. Success is concise and non-blocking. Deletion names consequence and Undo/confirmation. Onboarding explains choices/permissions without forcing optional tracking. Empty states give a next step.

## 8. Glossary

| Concept | Preferred | Avoid | Status |
|---|---|---|---|
| Product |Penny Circle|Spending Buddy as user-visible product name|defined AGENTS.md:1-9 |
| Shared group/place |Circle|Buddy as current primary destination; bank/account/backend|defined latest user instruction; README.md:26-30 |
| Record |Purchase for an individual entry; spending for aggregate|Unexplained transaction jargon|defined README.md:13-20; exact global noun convention remains provisional |
| Daily completion |Finish today / Finish day, with date when not today|Submit finish / hard deadline implying impossible late logging|defined AGENTS.md:13-23; final one-label form is undecided |
| Auto |Auto purchase, ten-minute Undo/share hold|Already shared during hold|defined AGENTS.md:31 |
| Public display period |Payment update duration|Undo deadline when describing1/2/5-minute card|defined SettingsScreens.kt:149-150 |
| Visibility |Totals & status / Full details|Ambiguous private/public switch|defined README.md:26 |
| Permission readiness |Allowed / Not set up; optional means optional|Ready implying guaranteed delivery/promotion|defined OnboardingFlow.kt; README.md:22 |
| Impulse |Impulse, text plus semantic red|Color-only red meaning|defined AGENTS.md:19-21 |

## 9. Copy mechanics

| Element | Rule | Status |
|---|---|---|
| Menus/buttons |Sentence case verb-first commands; noun places | derived shared labels; exact literal counts in token-census.json; human-readable dominant convention, not all Text literals are action labels |
| Headings |Sentence case content headings; uppercase mono numbered section labels are an intentional distinct style | defined AGENTS.md:15 and shared Components/Onboarding |
| Terminal periods |Complete explanatory sentences may end in periods; compact action labels do not | derived shared controls; blanket rule not otherwise ratified |
| Punctuation |Use clear sentence mechanics; labels like Totals & status keep approved ampersand. No decided universal ellipsis/serial comma/quote/exclamation policy | undecided where not explicit |
| Dates/times |Asia/Kolkata governs accountability. Display must distinguish10PM reminder from next-day10AM grace. Format census: h:mm a: 3, EEE d MMM: 1, EEE, d MMM yyyy: 1, EEEE, d MMMM: 1, d MMM: 1 | defined time semantics Models.kt; formatting alternatives derived, universal locale style undecided |
| Currency |₹, exact paise, Indian grouping for app amounts, no silent fractional truncation in input | defined Models.kt rupees/amountToPaise; OnboardingValidation.kt |
| Units |Readable min/minutes where context clear; options1/2/5 and15/30/60 show selected/default states | defined SettingsScreens.kt:146-165,192-209 |

## 10. Feedback policy

| Class | Channel/placement | Duration/dismissal | Status |
|---|---|---|---|
| Field validation |Inline at relevant input |Until corrected; preserve entered values | defined onboarding/add/limits forms |
| Success |Snackbar or inline state; rocket acknowledgement for finish |Non-blocking; reduced-motion still state | defined AGENTS.md:13-15; AppNav.kt |
| Recoverable error |Inline/sheet/snackbar with actionable retry/review |Keep input; retry without restart where implemented | defined startup/import design; unresolved consistency judged from actual code |
| Blocking init/load error |Startup retry surface; unreadable state recovery notice |Explicit safe recovery; never silently replace data | defined StartupSurface.kt and AppNav.kt |
| Destructive action |Separated destructive action, confirmation or Undo appropriate to reversibility |Delete reopens check-in and exposes Undo; membership actions state consequences | defined PurchaseDetailsSheet.kt; CircleSettings.kt |
| System/account notice |Native notification/permission surface; app-owned status copy |System-controls promotion. Payment1/2/5min; Undo/share10min; countdown ends22:00 | defined native notification implementations |

Dialog button order: native Material confirm/dismiss conventions when applicable; no extra owner-approved universal ordering. Web-only Esc/focus checks use Android Back/focus restoration equivalents; physical keyboard/TalkBack runtime remains blocked unless actually exercised.

## 11. Scope

| Item | Value |
|---|---|
| Accessibility |WCAG2.2AA audit target, native platform equivalents; no HTML/ARIA mandate |
| Platforms |Android13+ distribution; Android16/API36 and36.1 emulator runtime; Xiaomi/OnePlus real-device checks unavailable |
| Viewports |360dp small,411dp regular, font1.0/2.0; max640dp container statically; reduced motion; selected RTL geometry where supported |
| Themes |Light Horizon,Violet Night,Mint Night,Signal Blue |
| Locales |English UI; en-IN money semantics, device-localized dates where implemented; no translated UI claim; RTL layout support audited without promising translations |

## 12. Exceptions register

| Id | Deviation | Where/why | Authority |
|---|---|---|---|
| EX01 |Internal package/applicationId remains com.spendingbuddy.app |Compatibility; not user-facing brand drift | AGENTS.md:5 |
| EX02 |Clear opt-in shop payments auto-save without pre-review |Separate10-minute private Undo/share hold; screenshot/OCR stays review-first | AGENTS.md:31 |
| EX03 |Glass-like custom bottom bar replaces earlier stock styling; pane slide replaces fades |Explicit later user nav/haptics instruction; familiar native interactions still apply | README.md:20; docs/ui-navigation-refresh.md |
| EX04 |Week rings/bold totals replace old horizon/gauge hero |Latest design overrides legacy guide | AGENTS.md:13 |
| EX05 |System notification chrome follows Android; accent/₹ head follow app theme |Do not demand app-colored notification system background | LiveProgressAppearance.kt; docs/android-live-update-validation.md |
| EX06 |Semantic danger/category colors coexist with one brand accent |Color must retain text/icon meaning | AGENTS.md:19-21; Theme.kt:53-107 |

No other deviation is approved. Mark unresolved owner choices as decisions, not invented authority. Contract findings against derived rules must retain the UNRATIFIED caveat in the report.
