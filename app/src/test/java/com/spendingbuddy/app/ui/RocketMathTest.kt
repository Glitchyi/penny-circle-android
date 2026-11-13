package com.spendingbuddy.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RocketMathTest {

    @Test
    fun rocketYStartsBelowBottomAndEndsAboveTop() {
        assertEquals(2060f, rocketY(0f, 2000f, 100f), 0.01f)
        assertEquals(-100f, rocketY(1f, 2000f, 100f), 0.01f)
    }

    @Test
    fun rocketYIsStrictlyDecreasing() {
        var prev = rocketY(0f, 2000f, 100f)
        for (k in 1..10) {
            val y = rocketY(k / 10f, 2000f, 100f)
            assertTrue("y at t=${k / 10f} should be below previous", y < prev)
            prev = y
        }
    }

    @Test
    fun rocketYClampsProgress() {
        assertEquals(rocketY(0f, 2000f, 100f), rocketY(-1f, 2000f, 100f), 0.0001f)
        assertEquals(rocketY(1f, 2000f, 100f), rocketY(2f, 2000f, 100f), 0.0001f)
    }

    @Test
    fun rocketXIsCentredAtStartAndEndAndWobbleDies() {
        assertEquals(500f, rocketX(0f, 1000f), 0.01f)
        assertTrue(Math.abs(rocketX(1f, 1000f) - 500f) < 0.01f)
    }

    @Test
    fun burstTimeIsInsideFlight() {
        assertTrue(0f < ROCKET_BURST_T)
        assertTrue(ROCKET_BURST_T < 1f)
    }
}
