package com.spendingbuddy.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TopLevelSwipeMathTest {
    @Test fun swipeRequiresThresholdAndMovesOneAdjacentTab() {
        assertNull(topLevelSwipeTargetIndex(1, 219f, 1_000f, 1f))
        assertEquals(0, topLevelSwipeTargetIndex(1, 220f, 1_000f, 1f))
        assertEquals(2, topLevelSwipeTargetIndex(1, -220f, 1_000f, 1f))
    }

    @Test fun directionMirrorsInRtlAndStopsAtEnds() {
        assertEquals(2, topLevelSwipeTargetIndex(1, 220f, 1_000f, -1f))
        assertEquals(0, topLevelSwipeTargetIndex(1, -220f, 1_000f, -1f))
        assertNull(topLevelSwipeTargetIndex(0, 220f, 1_000f, 1f))
        assertNull(topLevelSwipeTargetIndex(2, -220f, 1_000f, 1f))
    }

    @Test fun resistanceIsContinuousAndCapsAtThePaneEdge() {
        assertEquals(550f, resistedPaneOffset(550f, 1_000f), 0.01f)
        assertEquals(550.34f, resistedPaneOffset(551f, 1_000f), 0.01f)
        assertEquals(703f, resistedPaneOffset(1_000f, 1_000f), 0.01f)
        assertEquals(880f, resistedPaneOffset(5_000f, 1_000f), 0.01f)
        assertEquals(-703f, resistedPaneOffset(-1_000f, 1_000f), 0.01f)
    }
}
