package com.spendingbuddy.app

import kotlin.math.PI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RotaryDialTest {
    @Test
    fun rotaryTargetsIncreaseByThirtyDegreesWithoutWrapDiscontinuity() {
        val targets = (1..9).map(::rotaryTargetRotation)
        val step = (PI / 6.0).toFloat()

        targets.zipWithNext().forEach { (previous, next) ->
            assertEquals(step, next - previous, 0.001f)
        }
        assertEquals(step, targets.first(), 0.001f)
        assertEquals(9f * step, targets.last(), 0.001f)
        assertTrue(targets.all { it > 0f && it < (2.0 * PI).toFloat() })
    }

    @Test
    fun signedDeltaCrossesAtan2SeamClockwise() {
        val from = (179.0 * PI / 180.0).toFloat()
        val to = (-179.0 * PI / 180.0).toFloat()
        assertEquals((2.0 * PI / 180.0).toFloat(), rotarySignedAngularDelta(from, to), 0.001f)
    }

    @Test
    fun keypadDigitsStopAtTwoFractionDigits() {
        assertEquals("12.34", appendRotaryDigit("12.3", 4))
        assertEquals("12.34", appendRotaryDigit("12.34", 5))
        assertEquals("7", appendRotaryDigit("0", 7))
        assertEquals("0.", appendRotaryDecimal(""))
    }
}
