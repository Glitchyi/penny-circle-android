package com.spendingbuddy.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.spendingbuddy.app.ui.AppTheme
import com.spendingbuddy.app.ui.CategoryColors
import com.spendingbuddy.app.ui.tokens
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryContrastTest {
    private fun ratio(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble()
        val l2 = b.luminance().toDouble()
        val hi = maxOf(l1, l2)
        val lo = minOf(l1, l2)
        return (hi + 0.05) / (lo + 0.05)
    }

    /** Composite a translucent colour over an opaque background, per channel. */
    private fun composite(fg: Color, bg: Color): Color {
        val a = fg.alpha
        return Color(
            red = fg.red * a + bg.red * (1f - a),
            green = fg.green * a + bg.green * (1f - a),
            blue = fg.blue * a + bg.blue * (1f - a),
            alpha = 1f
        )
    }

    /** Returns "name (ratio:1)" for every category whose check fails. */
    private fun failures(
        names: List<String>,
        contentOn: (String) -> Pair<Color, Color>,
        accentOn: (String) -> Pair<Color, Color>
    ): List<String> {
        val out = mutableListOf<String>()
        for (name in names) {
            val (content, bg) = contentOn(name)
            val contentRatio = ratio(content, bg)
            if (contentRatio < 4.5) out += "$name content ${"%.2f".format(contentRatio)}:1 < 4.5"
            val (accent, surface) = accentOn(name)
            val accentRatio = ratio(accent, surface)
            if (accentRatio < 3.0) out += "$name accent ${"%.2f".format(accentRatio)}:1 < 3.0"
        }
        return out
    }

    @Test fun darkThemeCategoriesMeetContrastOnCompositedContainer() {
        for (theme in AppTheme.values().filter { it.isDark }) {
            val surface = theme.tokens().surface
            val fails = failures(
                names = CategoryColors.names,
                contentOn = { name ->
                    val cc = CategoryColors.forCategory(name, true)
                    Pair(cc.content, composite(cc.container, surface))
                },
                accentOn = { name ->
                    Pair(CategoryColors.forCategory(name, true).accent, surface)
                }
            )
            assertTrue("${theme.id} dark category contrast failures: $fails", fails.isEmpty())
        }
    }

    @Test fun lightThemeCategoriesMeetContrast() {
        for (theme in AppTheme.values().filter { !it.isDark }) {
            val surface = theme.tokens().surface
            val fails = failures(
                names = CategoryColors.names,
                contentOn = { name ->
                    val cc = CategoryColors.forCategory(name, false)
                    Pair(cc.content, cc.container)
                },
                accentOn = { name ->
                    Pair(CategoryColors.forCategory(name, false).accent, surface)
                }
            )
            assertTrue("${theme.id} light category contrast failures: $fails", fails.isEmpty())
        }
    }
}
