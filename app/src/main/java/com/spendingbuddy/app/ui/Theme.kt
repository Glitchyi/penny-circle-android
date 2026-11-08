@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.spendingbuddy.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
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

object NotebookColors {
    val Paper = Color(0xFFF5F7FC)
    val PaperRaised = Color(0xFFFFFFFF)
    val Rule = Color(0xFFD8DEF0)
    val Ink = Color(0xFF2340A0)
    val InkDeep = Color(0xFF1A2350)
    val Muted = Color(0xFF5A6488)
    val Marigold = Color(0xFFF2A516)
    val Margin = Color(0xFFC7363A)
    val Leaf = Color(0xFF17744B)
    val DarkPaper = Color(0xFF0E1430)
    val DarkRaised = Color(0xFF182045)
    val DarkRule = Color(0xFF2A3463)
    val DarkInk = Color(0xFFA9B8FF)
    val DarkText = Color(0xFFE8ECFA)
    val DarkMuted = Color(0xFFA3ABCF)
    val DarkGold = Color(0xFFFFB833)
    val DarkMargin = Color(0xFFFF7A7A)
    val DarkLeaf = Color(0xFF5BD49B)
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

@Composable
fun SpendingBuddyTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) darkColorScheme(
        background = NotebookColors.DarkPaper,
        surface = NotebookColors.DarkRaised,
        surfaceVariant = NotebookColors.DarkRule,
        primary = NotebookColors.DarkInk,
        onPrimary = NotebookColors.DarkPaper,
        onBackground = NotebookColors.DarkText,
        onSurface = NotebookColors.DarkText,
        onSurfaceVariant = NotebookColors.DarkMuted,
        error = NotebookColors.DarkMargin,
        outline = NotebookColors.DarkRule
    ) else lightColorScheme(
        background = NotebookColors.Paper,
        surface = NotebookColors.PaperRaised,
        surfaceVariant = NotebookColors.Rule,
        primary = NotebookColors.Ink,
        onPrimary = Color.White,
        onBackground = NotebookColors.InkDeep,
        onSurface = NotebookColors.InkDeep,
        onSurfaceVariant = NotebookColors.Muted,
        error = NotebookColors.Margin,
        outline = NotebookColors.Rule
    )
    MaterialTheme(colorScheme = colors, typography = typography(), content = content)
}
