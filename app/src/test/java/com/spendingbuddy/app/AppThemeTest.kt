package com.spendingbuddy.app

import com.spendingbuddy.app.ui.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeTest {
    @Test fun fromIdRoundTripsEveryTheme() {
        for (theme in AppTheme.values()) {
            assertEquals(theme, AppTheme.fromId(theme.id))
        }
    }

    @Test fun unknownOrMissingIdFallsBackToLightHorizon() {
        assertEquals(AppTheme.LightHorizon, AppTheme.fromId(null))
        assertEquals(AppTheme.LightHorizon, AppTheme.fromId(""))
        assertEquals(AppTheme.LightHorizon, AppTheme.fromId("bogus"))
    }

    @Test fun themeIdsMatchEnumInOrder() {
        assertEquals(AppTheme.values().map { it.id }, THEME_IDS)
    }

    @Test fun themeIdsAreUnique() {
        assertEquals(AppTheme.values().size, AppTheme.values().map { it.id }.toSet().size)
        assertEquals(THEME_IDS.size, THEME_IDS.toSet().size)
    }

    @Test fun exactlyOneLightThemeAndTheRestDark() {
        val light = AppTheme.values().filter { !it.isDark }
        val dark = AppTheme.values().filter { it.isDark }
        assertEquals(1, light.size)
        assertEquals(AppTheme.LightHorizon, light.single())
        assertEquals(3, dark.size)
        assertTrue(dark.all { it.isDark })
    }
}
