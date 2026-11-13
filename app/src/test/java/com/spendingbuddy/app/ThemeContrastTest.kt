package com.spendingbuddy.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.spendingbuddy.app.ui.AppTheme
import com.spendingbuddy.app.ui.tokens
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    private fun ratio(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble()
        val l2 = b.luminance().toDouble()
        val hi = maxOf(l1, l2)
        val lo = minOf(l1, l2)
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun check(theme: AppTheme, name: String, fg: Color, bg: Color, min: Double) {
        val r = ratio(fg, bg)
        assertTrue("${theme.id} $name ${"%.2f".format(r)}:1 < $min", r >= min)
    }

    @Test fun everyThemeMeetsContrastTargets() {
        for (theme in AppTheme.values()) {
            val t = theme.tokens()
            check(theme, "onAccent/accent", t.onAccent, t.accent, 4.5)
            check(theme, "text/bg", t.text, t.bg, 7.0)
            check(theme, "text/surface", t.text, t.surface, 7.0)
            check(theme, "muted/bg", t.muted, t.bg, 4.5)
            check(theme, "muted/surface2", t.muted, t.surface2, 4.5)
            check(theme, "accent/bg", t.accent, t.bg, 3.0)
            check(theme, "danger/bg", t.danger, t.bg, 4.5)
            check(theme, "muted/surface", t.muted, t.surface, 4.5)
            check(theme, "danger/surface", t.danger, t.surface, 4.5)
        }
    }
}
