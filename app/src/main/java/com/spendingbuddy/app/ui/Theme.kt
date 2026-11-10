@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.spendingbuddy.app.ui

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
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
    fun forCategory(name: String): CategoryColor {
        val t = PennyPalette.categories.entries.firstOrNull { it.key.equals(name.trim(), ignoreCase = true) }?.value ?: PennyPalette.fallback
        return CategoryColor(c(t.first), c(t.second), c(t.third))
    }
}

/** Compatibility aliases for older call sites; prefer MaterialTheme.colorScheme and CategoryColors. */
object NotebookColors {
    val Paper = c(PennyPalette.Background)
    val PaperRaised = c(PennyPalette.Surface)
    val Rule = c(PennyPalette.Hairline)
    val Ink = c(PennyPalette.Primary)
    val InkDeep = c(PennyPalette.Text)
    val Muted = c(PennyPalette.TextMuted)
    val Marigold = c(PennyPalette.Tertiary)
    val Margin = c(PennyPalette.Error)
    val Leaf = c(PennyPalette.Secondary)
}

/** Small, consistent radii. */
object PennyShapes {
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(10.dp)
    val Large = RoundedCornerShape(12.dp)
}

private val Baloo = FontFamily(Font(R.font.baloo2_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))))
private val Mukta = FontFamily(
    Font(R.font.mukta_regular, FontWeight.Normal),
    Font(R.font.mukta_medium, FontWeight.Medium),
    Font(R.font.mukta_semibold, FontWeight.SemiBold),
    Font(R.font.mukta_bold, FontWeight.Bold)
)

private fun typography() = Typography(
    displayLarge = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.ExtraBold, fontSize = 56.sp, lineHeight = 62.sp),
    displayMedium = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    displaySmall = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = Baloo, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Mukta, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Mukta, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = Mukta, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Mukta, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Mukta, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Mukta, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
)

private val PennyColors = lightColorScheme(
    primary = c(PennyPalette.Primary), onPrimary = c(PennyPalette.OnPrimary),
    primaryContainer = c(PennyPalette.PrimaryContainer), onPrimaryContainer = c(PennyPalette.OnPrimaryContainer),
    secondary = c(PennyPalette.Secondary), onSecondary = c(PennyPalette.OnSecondary),
    secondaryContainer = c(PennyPalette.SecondaryContainer), onSecondaryContainer = c(PennyPalette.OnSecondaryContainer),
    tertiary = c(PennyPalette.Tertiary), onTertiary = c(PennyPalette.OnTertiary),
    tertiaryContainer = c(PennyPalette.TertiaryContainer), onTertiaryContainer = c(PennyPalette.OnTertiaryContainer),
    background = c(PennyPalette.Background), onBackground = c(PennyPalette.Text),
    surface = c(PennyPalette.Surface), onSurface = c(PennyPalette.Text),
    surfaceVariant = c(PennyPalette.SurfaceVariant), onSurfaceVariant = c(PennyPalette.TextMuted),
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = c(PennyPalette.Surface), surfaceContainerLow = c(PennyPalette.Surface),
    surfaceContainer = c(PennyPalette.Surface), surfaceContainerHigh = c(PennyPalette.SurfaceVariant),
    surfaceContainerHighest = c(PennyPalette.SurfaceVariant),
    outline = c(PennyPalette.Outline), outlineVariant = c(PennyPalette.Hairline),
    error = c(PennyPalette.Error), onError = c(PennyPalette.OnError),
    errorContainer = c(PennyPalette.ErrorContainer), onErrorContainer = c(PennyPalette.OnErrorContainer)
)

private val PennyMaterialShapes = Shapes(
    extraSmall = PennyShapes.Small, small = PennyShapes.Small, medium = PennyShapes.Medium,
    large = PennyShapes.Large, extraLarge = PennyShapes.Large
)

/** Penny Circle theme: light mode only, no dynamic colour. */
@Composable
fun SpendingBuddyTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }
    MaterialTheme(colorScheme = PennyColors, typography = typography(), shapes = PennyMaterialShapes, content = content)
}
