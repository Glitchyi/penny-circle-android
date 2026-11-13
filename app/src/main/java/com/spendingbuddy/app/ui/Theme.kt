@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.spendingbuddy.app.ui

import android.app.Activity
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.spendingbuddy.app.R

/** Raw 0xRRGGBB values (no Compose types) so JVM unit tests can verify WCAG contrast. */
object PennyPalette {
    const val Background = 0xF8FAFC
    const val Surface = 0xFFFFFF
    const val SurfaceVariant = 0xF1F5F9
    const val Hairline = 0xE2E8F0      // decorative 1dp borders and dividers
    const val Outline = 0x64748B       // control borders (3:1+)
    const val Text = 0x0F172A
    const val TextMuted = 0x475569
    const val Primary = 0x4F46E5       // indigo
    const val OnPrimary = 0xFFFFFF
    const val PrimaryContainer = 0xE0E7FF
    const val OnPrimaryContainer = 0x312E81
    const val Secondary = 0x047857     // emerald
    const val OnSecondary = 0xFFFFFF
    const val SecondaryContainer = 0xD1FAE5
    const val OnSecondaryContainer = 0x064E3B
    const val Tertiary = 0xB45309      // warm amber highlight
    const val OnTertiary = 0xFFFFFF
    const val TertiaryContainer = 0xFEF3C7
    const val OnTertiaryContainer = 0x78350F
    const val Error = 0xB91C1C
    const val OnError = 0xFFFFFF
    const val ErrorContainer = 0xFEE2E2
    const val OnErrorContainer = 0x991B1B

    /** name -> (container, content, accent). content is text/icon on container; accent is a solid mark (stripe, dot) needing 3:1 on surface. */
    val categories: Map<String, Triple<Int, Int, Int>> = linkedMapOf(
        "Food" to Triple(0xFFEDD5, 0x9A3412, 0xEA580C),
        "Transport" to Triple(0xE0F2FE, 0x075985, 0x0284C7),
        "Shopping" to Triple(0xFCE7F3, 0x9D174D, 0xDB2777),
        "Bills" to Triple(0xEDE9FE, 0x5B21B6, 0x7C3AED),
        "Essential" to Triple(0xD1FAE5, 0x065F46, 0x059669),
        "Convenience" to Triple(0xCCFBF1, 0x115E59, 0x0D9488),
        "Social" to Triple(0xFEF9C3, 0x854D0E, 0xA16207),
        "Impulse" to Triple(0xFEE2E2, 0x991B1B, 0xDC2626)
    )
    val fallback = Triple(SurfaceVariant, TextMuted, Outline)
}

private fun c(rgb: Int) = Color(0xFF000000.toInt() or rgb)

/** Per-category accent palette. Always pair the colour with the category name as text. */
@Immutable
data class CategoryColor(val container: Color, val content: Color, val accent: Color)

object CategoryColors {
    val names: List<String> get() = PennyPalette.categories.keys.toList()
    fun forCategory(name: String, dark: Boolean = false): CategoryColor {
        val t = PennyPalette.categories.entries.firstOrNull { it.key.equals(name.trim(), ignoreCase = true) }?.value ?: PennyPalette.fallback
        if (dark) {
            val accent = c(t.third)
            return CategoryColor(
                container = accent.copy(alpha = 0.18f),
                content = lerp(accent, Color.White, 0.45f),
                accent = accent
            )
        }
        return CategoryColor(c(t.first), c(t.second), c(t.third))
    }
}

/** Small, consistent radii. */
object PennyShapes {
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(10.dp)
    val Large = RoundedCornerShape(12.dp)
}

/** Existing contract border roles; these do not define a new spacing or radius scale. */
object PennyBorders {
    val Hairline = 1.dp
    val SelectedChip = 1.5.dp
    val PermissionHighlight = 2.dp
}

/** The app's visual looks. `id` is the stored value in BuddyState.theme. */
enum class AppTheme(val id: String, val label: String, val mood: String, val isDark: Boolean) {
    LightHorizon("light_horizon", "Light Horizon", "White, hairlines, violet.", false),
    VioletNight("violet_night", "Violet Night", "Indigo-black, violet glow.", true),
    MintNight("mint_night", "Mint Night", "Black, mint horizon.", true),
    SignalBlue("signal_blue", "Signal Blue", "Graphite, electric blue.", true);

    companion object {
        fun fromId(id: String?): AppTheme = values().firstOrNull { it.id == id } ?: LightHorizon
    }
}

/** Semantic colour tokens for one theme. Read via LocalPennyTokens. */
@Immutable
class PennyTokens(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val rule: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val accent2: Color,
    val accent3: Color,
    val onAccent: Color,
    val danger: Color,
    val dark: Boolean
)

fun AppTheme.tokens(): PennyTokens = when (this) {
    AppTheme.LightHorizon -> PennyTokens(
        bg = c(0xFAFAFB), surface = c(0xFFFFFF), surface2 = c(0xF1F1F5), border = c(0xE3E3EA),
        rule = c(0x0B0B10), text = c(0x0B0B10), muted = c(0x6B6B7B), accent = c(0x6A3DF5),
        accent2 = c(0xFF6FB5), accent3 = c(0x4FC3FF), onAccent = c(0xFFFFFF), danger = c(0xB91C1C), dark = false
    )
    AppTheme.VioletNight -> PennyTokens(
        bg = c(0x0D0B14), surface = c(0x151220), surface2 = c(0x1E1A2D), border = c(0x34304D),
        rule = c(0xEFEDF8).copy(alpha = 0.30f), text = c(0xEFEDF8), muted = c(0x8F89A6), accent = c(0x9D6BFF),
        accent2 = c(0xFF5FA2), accent3 = c(0x4B7BFF), onAccent = c(0x0D0B14), danger = c(0xFF6B6B), dark = true
    )
    AppTheme.MintNight -> PennyTokens(
        bg = c(0x08090A), surface = c(0x101212), surface2 = c(0x181B1B), border = c(0x2E3434),
        rule = c(0xF4F7F6).copy(alpha = 0.28f), text = c(0xF4F7F6), muted = c(0x8D9593), accent = c(0x00E599),
        accent2 = c(0x22D3EE), accent3 = c(0xA3E635), onAccent = c(0x03120C), danger = c(0xFF6B6B), dark = true
    )
    AppTheme.SignalBlue -> PennyTokens(
        bg = c(0x090B11), surface = c(0x10131B), surface2 = c(0x171B26), border = c(0x2C3447),
        rule = c(0xEEF2FA).copy(alpha = 0.28f), text = c(0xEEF2FA), muted = c(0x8993A8), accent = c(0x4D9BFF),
        accent2 = c(0x22D3EE), accent3 = c(0x8B5CF6), onAccent = c(0x04101F), danger = c(0xFF6B6B), dark = true
    )
}

val LocalPennyTokens = compositionLocalOf { AppTheme.LightHorizon.tokens() }

/** True when reduced motion is on (system animator duration scale is 0). Provided by SpendingBuddyTheme. */
val LocalReducedMotion = compositionLocalOf { false }

/** Animate interactions inside a theme; never blend a prior theme into a new semantic pair. */
@Composable
internal fun pennyAnimatedColor(
    targetValue: Color,
    animationSpec: AnimationSpec<Color> = PennyMotion.effects(),
    label: String = "pennyColor"
): State<Color> {
    val tokens = LocalPennyTokens.current
    val reduced = LocalReducedMotion.current
    return key(tokens, reduced) {
        animateColorAsState(targetValue, if (reduced) snap() else animationSpec, label = label)
    }
}

private val Geist = FontFamily(
    Font(R.font.geist_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.geist_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.geist_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.geist_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

private val GeistMono = FontFamily(
    Font(R.font.geist_mono_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.geist_mono_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500)))
)

/**
 * Named motion specs, so springs are tuned in one place. Use the spatial ones for things that move or
 * resize, and [effects] (no overshoot) for colour and alpha.
 */
object PennyMotion {
    fun <T> spatial(): SpringSpec<T> = spring(dampingRatio = 0.7f, stiffness = 380f)
    fun <T> spatialSlow(): SpringSpec<T> = spring(dampingRatio = 0.85f, stiffness = 70f)
    fun <T> effects(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 380f)
}

/** Font families for direct use in components (for example amounts use Mono). */
object PennyType {
    val Mono: FontFamily = GeistMono
    val Sans: FontFamily = Geist
}

private fun typography() = Typography(
    displayLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 56.sp, lineHeight = 62.sp, letterSpacing = (-0.03).em),
    displayMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.03).em),
    displaySmall = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.03).em),
    headlineSmall = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
).let { approved ->
    // Unspecified Material roles inherit an approved Geist role rather than platform defaults.
    approved.copy(
        bodySmall = approved.bodyMedium,
        labelSmall = approved.labelMedium,
        titleSmall = approved.titleMedium,
        headlineMedium = approved.displaySmall,
        headlineLarge = approved.displayMedium
    )
}

private val PennyTypography: Typography = typography()

private val PennyMaterialShapes = Shapes(
    extraSmall = PennyShapes.Small, small = PennyShapes.Small, medium = PennyShapes.Medium,
    large = PennyShapes.Large, extraLarge = PennyShapes.Large
)

/** True when the system animator duration scale is 0 (reduced motion / animations off). */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    val resolver = context.contentResolver
    fun read() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    var reduced by remember(context) { mutableStateOf(read()) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { reduced = read() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        reduced = read()
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}

/** Applies this theme's system-bar icon contrast to the current Compose dialog window only. */
@Composable
fun DialogSystemBars() {
    val view = LocalView.current
    val useDarkIcons = !LocalPennyTokens.current.dark
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = useDarkIcons
        controller.isAppearanceLightNavigationBars = useDarkIcons
    }
}

/** Switch semantic foreground/background pairs together so theme changes preserve contrast. */
@Composable
fun SpendingBuddyTheme(theme: AppTheme = AppTheme.LightHorizon, content: @Composable () -> Unit) {
    val t = remember(theme) { theme.tokens() }
    val reduceMotion = rememberReducedMotion()
    val a = t
    val colorScheme: ColorScheme = if (!a.dark) {
        lightColorScheme(
            primary = a.accent, onPrimary = a.onAccent,
            primaryContainer = a.surface2, onPrimaryContainer = a.text,
            secondary = a.accent, onSecondary = a.onAccent,
            secondaryContainer = a.surface2, onSecondaryContainer = a.text,
            tertiary = a.accent, onTertiary = a.onAccent,
            tertiaryContainer = a.surface2, onTertiaryContainer = a.text,
            background = a.bg, onBackground = a.text,
            surface = a.surface, onSurface = a.text,
            surfaceVariant = a.surface2, onSurfaceVariant = a.muted,
            surfaceTint = Color.Transparent,
            surfaceContainerLowest = a.surface, surfaceContainerLow = a.surface,
            surfaceContainer = a.surface, surfaceContainerHigh = a.surface2,
            surfaceContainerHighest = a.surface2,
            outline = a.muted, outlineVariant = a.border,
            error = a.danger, onError = a.bg,
            errorContainer = a.surface2, onErrorContainer = a.danger,
            inverseSurface = a.text, inverseOnSurface = a.bg
        )
    } else {
        darkColorScheme(
            primary = a.accent, onPrimary = a.onAccent,
            primaryContainer = a.surface2, onPrimaryContainer = a.text,
            secondary = a.accent, onSecondary = a.onAccent,
            secondaryContainer = a.surface2, onSecondaryContainer = a.text,
            tertiary = a.accent, onTertiary = a.onAccent,
            tertiaryContainer = a.surface2, onTertiaryContainer = a.text,
            background = a.bg, onBackground = a.text,
            surface = a.surface, onSurface = a.text,
            surfaceVariant = a.surface2, onSurfaceVariant = a.muted,
            surfaceTint = Color.Transparent,
            surfaceContainerLowest = a.surface, surfaceContainerLow = a.surface,
            surfaceContainer = a.surface, surfaceContainerHigh = a.surface2,
            surfaceContainerHighest = a.surface2,
            outline = a.muted, outlineVariant = a.border,
            error = a.danger, onError = a.bg,
            errorContainer = a.surface2, onErrorContainer = a.danger,
            inverseSurface = a.text, inverseOnSurface = a.bg
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            if (controller.isAppearanceLightStatusBars == t.dark) controller.isAppearanceLightStatusBars = !t.dark
            if (controller.isAppearanceLightNavigationBars == t.dark) controller.isAppearanceLightNavigationBars = !t.dark
        }
    }
    CompositionLocalProvider(LocalPennyTokens provides a, LocalReducedMotion provides reduceMotion) {
        MaterialTheme(colorScheme = colorScheme, typography = PennyTypography, shapes = PennyMaterialShapes, content = content)
    }
}
