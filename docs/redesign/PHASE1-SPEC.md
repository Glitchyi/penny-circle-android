# Phase 1: themes + fonts + onboarding theme picker (Penny Circle, Android / Jetpack Compose)

Repo: C:\Users\User\spending-buddy-android  (package com.spendingbuddy.app, Kotlin, Compose Material 3, minSdk 33)
App sources: app\src\main\java\com\spendingbuddy\app\   (UI in ...\ui\)
Design source of truth: the "Light Horizon" prototype (white/hairline/violet) and its three dark variants.

## Already done by the lead (do not redo)
- `BuddyState.theme: String` (default "light_horizon") in core Models.kt.
- `BuddyRepository.setTheme(id: String)` (throws for unknown ids) and `THEME_IDS`.
- Fonts copied to app\src\main\res\font\: `geist_variable.ttf` (R.font.geist_variable) and `geist_mono_variable.ttf` (R.font.geist_mono_variable). Both are variable fonts with a weight axis.

## The API contract (both packages rely on exactly this; names and signatures must match)
All in package `com.spendingbuddy.app.ui`, file `Theme.kt`:

```kotlin
enum class AppTheme(val id: String, val label: String, val mood: String, val isDark: Boolean) {
    LightHorizon("light_horizon", "Light Horizon", "White, hairlines, violet.", false),
    VioletNight("violet_night", "Violet Night", "Deep indigo-black with a violet glow.", true),
    MintNight("mint_night", "Mint Night", "Near-black with a mint horizon.", true),
    SignalBlue("signal_blue", "Signal Blue", "Cool graphite with an electric blue accent.", true);
    companion object { fun fromId(id: String?): AppTheme = values().firstOrNull { it.id == id } ?: LightHorizon }
}

@Immutable
class PennyTokens(
    val bg: Color, val surface: Color, val surface2: Color, val border: Color, val rule: Color,
    val text: Color, val muted: Color, val accent: Color, val onAccent: Color, val danger: Color, val dark: Boolean
)
fun AppTheme.tokens(): PennyTokens
val LocalPennyTokens = staticCompositionLocalOf { AppTheme.LightHorizon.tokens() }

@Composable fun SpendingBuddyTheme(theme: AppTheme = AppTheme.LightHorizon, content: @Composable () -> Unit)
```
Use `values()`, not `entries`.

## Token values (0xRRGGBB)
| token | LightHorizon | VioletNight | MintNight | SignalBlue |
|---|---|---|---|---|
| bg | FAFAFB | 0D0B14 | 08090A | 090B11 |
| surface | FFFFFF | 151220 | 101212 | 10131B |
| surface2 | F1F1F5 | 1E1A2D | 181B1B | 171B26 |
| border | E3E3EA | 2A2540 | 252929 | 232938 |
| rule | 0B0B10 | EFEDF8 at 30% alpha | F4F7F6 at 28% alpha | EEF2FA at 28% alpha |
| text | 0B0B10 | EFEDF8 | F4F7F6 | EEF2FA |
| muted | 6B6B7B | 8F89A6 | 8D9593 | 8993A8 |
| accent | 6A3DF5 | 9D6BFF | 00E599 | 4D9BFF |
| onAccent | FFFFFF | 0D0B14 | 03120C | 04101F |
| danger | B91C1C | FF6B6B | FF6B6B | FF6B6B |
(All colours opaque 0xFF alpha unless an alpha is stated. onAccent for VioletNight is near-black on purpose: it has better contrast on the violet than white does.)

---------------------------------------------------------------------------------------------
# PACKAGE T: theme system  (owner: agent T)
You may edit ONLY: ui\Theme.kt and ui\Components.kt (two call sites). Nothing else.

1. In ui\Theme.kt add `AppTheme`, `PennyTokens`, `AppTheme.tokens()`, `LocalPennyTokens` exactly as in the contract.
2. KEEP these existing things untouched because a unit test and other files use them: `object PennyPalette`, `object NotebookColors`, `object PennyShapes`, `CategoryColor`, `object CategoryColors` (you only extend it, see 5).
3. Typography: replace the Baloo/Mukta families with Geist (R.font.geist_variable) and Geist Mono (R.font.geist_mono_variable). For each weight you need (400, 500, 600, 700) add a `Font(R.font.geist_variable, FontWeight.X, variationSettings = FontVariation.Settings(FontVariation.weight(N)))` entry to one `FontFamily` named `Geist`; same for `GeistMono` with 400 and 500. Mapping: displayLarge/displayMedium/displaySmall/headlineSmall/titleLarge/titleMedium = Geist SemiBold (display sizes keep their current sp values, add `letterSpacing = (-0.03).em` for display styles); bodyLarge/bodyMedium = Geist Normal; labelLarge/labelMedium = Geist Medium. Also add `object PennyType { val Mono: FontFamily = GeistMono; val Sans: FontFamily = Geist }` (public) for later use. Delete the Baloo and Mukta FontFamily definitions; do NOT delete the font resource files.
4. `SpendingBuddyTheme(theme, content)`:
   - `val t = theme.tokens()`
   - Smooth theme switching: animate each colour you put into the scheme with `animateColorAsState(target, animationSpec = tween(450), label = ...)`. Write a small private helper `@Composable private fun anim(c: Color): Color = animateColorAsState(c, tween(450), label = "theme").value`.
   - Build `lightColorScheme(...)` when `!t.dark` else `darkColorScheme(...)` with this mapping: primary = accent, onPrimary = onAccent, primaryContainer = surface2, onPrimaryContainer = text, secondary = accent, onSecondary = onAccent, secondaryContainer = surface2, onSecondaryContainer = text, tertiary = accent, onTertiary = onAccent, tertiaryContainer = surface2, onTertiaryContainer = text, background = bg, onBackground = text, surface = surface, onSurface = text, surfaceVariant = surface2, onSurfaceVariant = muted, surfaceTint = Transparent, surfaceContainerLowest = surface, surfaceContainerLow = surface, surfaceContainer = surface, surfaceContainerHigh = surface2, surfaceContainerHighest = surface2, outline = muted, outlineVariant = border, error = danger, onError = bg, errorContainer = surface2, onErrorContainer = danger, inverseSurface = text, inverseOnSurface = bg.
   - `CompositionLocalProvider(LocalPennyTokens provides <animated tokens built from the same animated colours>) { MaterialTheme(colorScheme, typography(), shapes, content) }`. If building animated PennyTokens is awkward, provide the non-animated `t`.
   - System bars: in the existing `SideEffect`, set `controller.isAppearanceLightStatusBars = !t.dark` and `controller.isAppearanceLightNavigationBars = !t.dark`.
5. `CategoryColors.forCategory(name: String, dark: Boolean = false): CategoryColor`: when `dark` is true return `CategoryColor(container = accent.copy(alpha = 0.18f), content = <accent lightened 45% toward white, e.g. androidx.compose.ui.graphics.lerp(accent, Color.White, 0.45f)>, accent = accent)` where accent is the third value of the existing light entry; when false behave exactly as today.
6. In ui\Components.kt change the two calls `CategoryColors.forCategory(...)` (lines ~64 and ~81) to pass `LocalPennyTokens.current.dark` as the second argument. Add `import androidx.compose.runtime.CompositionLocal`-style imports only if needed (the import you need is just for LocalPennyTokens, which is in the same package, so none).

---------------------------------------------------------------------------------------------
# PACKAGE P: theme picker, onboarding step, settings entry  (owner: agent P)
You may edit ONLY: ui\ThemePicker.kt (NEW file), ui\SettingsScreens.kt, ui\AppNav.kt. Nothing else. Another agent is writing ui\Theme.kt at the same time to the contract above; write against the contract and never edit Theme.kt.

1. New file ui\ThemePicker.kt: 
```kotlin
@Composable fun ThemePicker(selectedId: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier, haptics: Boolean = true)
```
   - A 2-column grid (two `Row`s inside a `Column`, each card `Modifier.weight(1f)`, `Arrangement.spacedBy(10.dp)`) over `AppTheme.values()`.
   - Each card: `Modifier.selectable(selected = ..., role = Role.RadioButton, onClick = ...)`, min height 48.dp (in practice about 150.dp), a `ThemePreview(theme)` at the top, then `Text(theme.label)` (style titleMedium-ish: MaterialTheme.typography.labelLarge, FontWeight.SemiBold) and `Text(theme.mood)` (bodyMedium, onSurfaceVariant). Card shape `RoundedCornerShape(14.dp)`, border 1.dp `MaterialTheme.colorScheme.outlineVariant` when not selected; when selected the border is 2.dp `MaterialTheme.colorScheme.primary` (animate the colour and width with `animateColorAsState` and `animateDpAsState`) and a small `Icons.Rounded.Check` (androidx.compose.material.icons.rounded.Check) badge appears at the top right of the preview. Card background `MaterialTheme.colorScheme.surface`.
   - A spring scale animation on selection: `animateFloatAsState(if (selected) 1f else 0.97f, spring(dampingRatio = 0.55f, stiffness = 400f))` applied with `Modifier.graphicsLayer { scaleX = s; scaleY = s }`.
   - Accessibility: `Modifier.semantics { contentDescription = "${theme.label}. ${theme.mood}"; }` (selectable already exposes the selected state).
   - Haptic tick on a new selection: `LocalView.current.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)` only when `haptics` is true and the tapped card is not already selected.
   - `ThemePreview(theme: AppTheme)` is a private composable that draws a miniature of that theme using `theme.tokens()` (NOT the current theme): a `Box` 96.dp tall, `RoundedCornerShape(10.dp)`, background `t.bg`, 1.dp border `t.border`, containing: top-left `Text("₹250")` 22.sp SemiBold in `t.text` using `PennyType.Mono`; a 3.dp-high accent bar (width 45%) in `t.accent` below it; at the bottom a "horizon": a `Box` full width, 16.dp high, `RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp)`, 1.dp border `t.accent`, background `t.accent.copy(alpha = 0.12f)`; and to the right of the number two small rounded rectangles (28.dp x 6.dp) in `t.surface2` with `t.border` borders to hint at list rows.
2. ui\SettingsScreens.kt, `OnboardingScreen`: it is currently 3 steps (0 name, 1 limits, else reminders). Make it 4 steps: 0 name, 1 NEW theme step, 2 limits, 3 reminders.
   - Add parameters `theme: String` and `onTheme: (String) -> Unit` (place them right after `step`).
   - The label becomes `"Step ${step + 1} of 4"`.
   - New step 1: `Text("Pick a look", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 38.dp))`, then `Text("You can change it any time in Settings.", style = bodyMedium, color = onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))`, then `ThemePicker(selectedId = theme, onSelect = onTheme, modifier = Modifier.padding(top = 20.dp))`. Make the step content scrollable: wrap the whole top `Column` content area in `Modifier.verticalScroll(rememberScrollState())` if it is not already, so the picker never overflows small screens (keep the bottom buttons outside the scroll area).
   - The existing limits step moves from `1 ->` to `2 ->`; the final reminders step is the `else ->` branch. The button logic `step == 2` becomes `step == 3` in both places (button action and both labels).
3. ui\SettingsScreens.kt, the main Settings screen (the one that already has an "Amount entry" section with the Keypad / Rotary dial choice, around line 80): add a new section titled "Appearance" ABOVE "Amount entry", styled exactly like the other section headings, containing `ThemePicker(selectedId = state.theme, onSelect = onTheme, haptics = state.haptics)`. Add a parameter `onTheme: (String) -> Unit` to that Settings composable and pass it from its call site in ui\AppNav.kt as `onTheme = { repo.setTheme(it) }`. (`state.theme` is a String.)
4. ui\AppNav.kt:
   - `SpendingBuddyTheme {` (around line 151) becomes `SpendingBuddyTheme(AppTheme.fromId(state.theme)) {`. `state` is already in scope there.
   - In the `composable(ONBOARDING)` block: pass `theme = state.theme` and `onTheme = { repo.setTheme(it) }` to `OnboardingScreen`, and change `onNext = { if (onboardingStep < 2) onboardingStep += 1 }` to `< 3`.
   - Also pass `onTheme` to the Settings composable's call site (see 3).

## Rules for both agents
- Do not run Gradle, adb or git. Do not create files other than the one named. Do not add dependencies or touch build files or tests.
- Read the exact lines before each edit. Add every import you use. If something does not match this spec, skip that item and report the mismatch instead of guessing or inventing an API.
- Reply in under 15 lines: one line per numbered item (done / skipped with reason) and the list of files you changed.
