package com.spendingbuddy.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationMotionTest {
    @Test fun topLevelTabsFollowTheirDisplayedOrder() {
        assertEquals(2, PaneMotion.direction("today", "activity"))
        assertEquals(2, PaneMotion.direction("today", "circle"))
        assertEquals(-2, PaneMotion.direction("circle", "activity"))
        assertEquals(0, PaneMotion.direction("activity", "activity"))
    }

    @Test fun settingsAndCircleDetailsPushAndPop() {
        assertEquals(1, PaneMotion.direction("today", "settings"))
        assertEquals(1, PaneMotion.direction("settings", "limits"))
        assertEquals(-1, PaneMotion.direction("limits", "settings"))
        assertEquals(1, PaneMotion.direction("circle", "circle/invite"))
        assertEquals(-1, PaneMotion.direction("circle/settings", "circle"))
        assertEquals(1, PaneMotion.direction("onboarding", "today"))
    }

    @Test fun missingOrUnknownRoutesDoNotAnimate() {
        assertEquals(0, PaneMotion.direction(null, "today"))
        assertEquals(0, PaneMotion.direction("unknown", "today"))
        assertEquals(0, PaneMotion.direction("today", "unknown"))
    }
}
