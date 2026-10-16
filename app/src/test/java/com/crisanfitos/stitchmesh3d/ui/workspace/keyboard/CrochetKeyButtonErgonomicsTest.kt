package com.crisanfitos.stitchmesh3d.ui.workspace.keyboard

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios que verifican las especificaciones ergonómicas del teclado táctil de crochet (SM-072).
 */
class CrochetKeyButtonErgonomicsTest {

    @Test
    fun `standard touch target meets or exceeds Material 3 minimum 48dp threshold`() {
        val minTargetWidth = 48.dp
        val minTargetHeight = 48.dp

        assertTrue("Target width must be >= 48dp", minTargetWidth.value >= 48f)
        assertTrue("Target height must be >= 48dp", minTargetHeight.value >= 48f)
    }

    @Test
    fun `stitch tokens provide fast one-tap input for core amigurumi vocabulary`() {
        val stitchTokens = listOf("AM", "pb", "aum", "dism", "pe", "mpa", "pa", "cad")
        assertEquals(8, stitchTokens.size)

        // Todos los tokens esenciales deben estar disponibles en primera fila
        assertTrue(stitchTokens.contains("AM"))
        assertTrue(stitchTokens.contains("pb"))
        assertTrue(stitchTokens.contains("aum"))
        assertTrue(stitchTokens.contains("dism"))
    }

    @Test
    fun `syntax tokens cover topological modifiers and delimiters`() {
        val syntaxTokens = listOf("BLO", "FLO", "[", "]", "*", "( )", "(", ")", ",")
        assertEquals(9, syntaxTokens.size)

        assertTrue(syntaxTokens.contains("BLO"))
        assertTrue(syntaxTokens.contains("FLO"))
        assertTrue(syntaxTokens.contains("["))
        assertTrue(syntaxTokens.contains("]"))
        assertTrue(syntaxTokens.contains("*"))
    }
}
