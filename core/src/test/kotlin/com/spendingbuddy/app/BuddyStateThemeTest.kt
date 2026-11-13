package com.spendingbuddy.app

import org.junit.Assert.assertEquals
import org.junit.Test

class BuddyStateThemeTest {
    @Test fun oldStateWithoutThemeDefaultsToLightHorizon() {
        val v2NoTheme = """{"schemaVersion":2,"name":"Asha","onboardingDone":true,"dailyLimitPaise":40000}"""
        val s = StateCodec.decode(v2NoTheme)
        assertEquals("light_horizon", s.theme)
    }

    @Test fun themeSurvivesEncodeDecodeRoundTrip() {
        val s = BuddyState(name = "Asha", onboardingDone = true, theme = "violet_night")
        assertEquals("violet_night", StateCodec.decode(StateCodec.encode(s)).theme)
    }
}
