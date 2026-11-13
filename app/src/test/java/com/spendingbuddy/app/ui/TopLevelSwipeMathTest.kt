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

    @Test fun neighborIsAvailableBeforeCommitThresholdAndNeverWraps() {
        assertEquals(2, topLevelSwipePreviewIndex(1, -1f, 1f))
        assertEquals(0, topLevelSwipePreviewIndex(1, 1f, 1f))
        assertEquals(2, topLevelSwipePreviewIndex(1, 1f, -1f))
        assertEquals(0, topLevelSwipePreviewIndex(1, -1f, -1f))
        assertNull(topLevelSwipePreviewIndex(1, 0f, 1f))
        assertNull(topLevelSwipePreviewIndex(0, 1f, 1f))
        assertNull(topLevelSwipePreviewIndex(2, -1f, 1f))
        assertNull(topLevelSwipePreviewIndex(0, -1f, -1f))
        assertNull(topLevelSwipePreviewIndex(2, 1f, -1f))
    }

    @Test fun sourceAndPreviewCoverTheViewportWithoutAGapInBothPhysicalDirections() {
        val width = 1_000f
        for (sourceOffset in listOf(-1f, -220f, -703f, -1_000f, 1f, 220f, 703f, 1_000f)) {
            val previewOffset = panePreviewOffset(sourceOffset, width)
            if (sourceOffset < 0f) {
                assertEquals(sourceOffset + width, previewOffset, 0.001f)
            } else {
                assertEquals(previewOffset + width, sourceOffset, 0.001f)
            }
        }
        assertEquals(0f, panePreviewOffset(-width, width), 0.001f)
        assertEquals(0f, panePreviewOffset(width, width), 0.001f)
    }
}
