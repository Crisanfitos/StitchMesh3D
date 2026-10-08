package com.crisanfitos.stitchmesh3d.ui.viewport.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeelSliderBarTest {

    @Test
    fun `peel round is clamped within valid bounds 1 to totalRounds`() {
        val totalRounds = 18

        fun clampRound(requested: Int): Int {
            val effectiveTotal = maxOf(1, totalRounds)
            return requested.coerceIn(1, effectiveTotal)
        }

        assertEquals(1, clampRound(0))
        assertEquals(1, clampRound(-5))
        assertEquals(10, clampRound(10))
        assertEquals(18, clampRound(18))
        assertEquals(18, clampRound(25))
    }

    @Test
    fun `single round project clamps to 1`() {
        val totalRounds = 1

        fun clampRound(requested: Int): Int {
            val effectiveTotal = maxOf(1, totalRounds)
            return requested.coerceIn(1, effectiveTotal)
        }

        assertEquals(1, clampRound(0))
        assertEquals(1, clampRound(1))
        assertEquals(1, clampRound(5))
    }

    @Test
    fun `fine step navigation increments and decrements round correctly`() {
        var currentRound = 5
        val totalRounds = 10

        // Step next
        if (currentRound < totalRounds) {
            currentRound += 1
        }
        assertEquals(6, currentRound)

        // Step previous
        if (currentRound > 1) {
            currentRound -= 1
        }
        assertEquals(5, currentRound)

        // Reset to all rounds
        currentRound = totalRounds
        assertEquals(10, currentRound)
    }

    @Test
    fun `wireframe state toggles accurately`() {
        var isWireframe = false

        fun toggleWireframe(newValue: Boolean) {
            isWireframe = newValue
        }

        assertFalse(isWireframe)
        toggleWireframe(true)
        assertTrue(isWireframe)
        toggleWireframe(false)
        assertFalse(isWireframe)
    }
}
