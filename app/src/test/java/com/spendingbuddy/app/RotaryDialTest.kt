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

    @Test
    fun integerDigitsAreCappedAtEight() {
        assertEquals("12345678", appendRotaryDigit("12345678", 9))
        assertEquals("1234567", appendRotaryDigit("123456", 7))
        assertEquals("12345678.9", appendRotaryDigit("12345678.", 9))
    }

    @Test
    fun decimalHelperHandlesEmptyExistingAndPrefix() {
        assertEquals("5.", appendRotaryDecimal("5"))
        assertEquals("5.", appendRotaryDecimal("5."))
        assertEquals("0.", appendRotaryDecimal("₹"))
    }

    @Test
    fun zeroHandling() {
        assertEquals("0", appendRotaryDigit("", 0))
        assertEquals("0", appendRotaryDigit("0", 0))
        assertEquals("", appendRotaryDigit("", 10))
    }

    @Test
    fun removeLastCharacter() {
        assertEquals("12", removeRotaryLastCharacter("123"))
        assertEquals("", removeRotaryLastCharacter("5"))
        assertEquals("", removeRotaryLastCharacter(""))
        assertEquals("1", removeRotaryLastCharacter("1."))
    }

    @Test
    fun normalizeAngleWraps() {
        val twoPi = (2.0 * PI).toFloat()
        assertEquals(0.5f, normalizeRotaryAngle(0.5f + twoPi), 0.001f)
        assertEquals(twoPi - 0.5f, normalizeRotaryAngle(-0.5f), 0.001f)
        assertEquals(0f, normalizeRotaryAngle(0f), 0.0001f)
    }

    @Test
    fun clockwiseDistanceWrapsForward() {
        assertEquals(1f, rotaryClockwiseDistance(1f, 2f), 0.001f)
        val twoPi = (2.0 * PI).toFloat()
        assertEquals(twoPi - 1f, rotaryClockwiseDistance(2f, 1f), 0.001f)
    }
}
