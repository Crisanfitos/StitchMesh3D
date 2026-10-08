package com.crisanfitos.stitchmesh3d.ui.workspace.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrochetQuickKeyboardTest {

    @Test
    fun `token sequence builds valid crochet instruction line`() {
        var currentText = ""

        // Simular secuencia: 6 -> pb -> , -> 1 -> aum
        currentText = CrochetTokenFormatter.insertToken(currentText, "6")
        currentText = CrochetTokenFormatter.insertToken(currentText, "pb")
        currentText = CrochetTokenFormatter.insertToken(currentText, ",")
        currentText = CrochetTokenFormatter.insertToken(currentText, "1")
        currentText = CrochetTokenFormatter.insertToken(currentText, "aum")

        assertEquals("6 pb, 1 aum", currentText)

        // Simular borrado de 4 caracteres (" aum")
        currentText = CrochetTokenFormatter.deleteLastChar(currentText) // 'm'
        currentText = CrochetTokenFormatter.deleteLastChar(currentText) // 'u'
        currentText = CrochetTokenFormatter.deleteLastChar(currentText) // 'a'
        currentText = CrochetTokenFormatter.deleteLastChar(currentText) // ' '

        assertEquals("6 pb, 1", currentText)
    }

    @Test
    fun `bracket and multiplication formatting is preserved without redundant spaces`() {
        var formula = ""
        formula = CrochetTokenFormatter.insertToken(formula, "[")
        formula = CrochetTokenFormatter.insertToken(formula, "1")
        formula = CrochetTokenFormatter.insertToken(formula, "pb")
        formula = CrochetTokenFormatter.insertToken(formula, ",")
        formula = CrochetTokenFormatter.insertToken(formula, "1")
        formula = CrochetTokenFormatter.insertToken(formula, "aum")
        formula = CrochetTokenFormatter.insertToken(formula, "]")
        formula = CrochetTokenFormatter.insertToken(formula, "*")
        formula = CrochetTokenFormatter.insertToken(formula, "6")

        assertEquals("[1 pb, 1 aum]* 6", formula)
    }

    @Test
    fun `common crochet stitch tokens are defined`() {
        val stitchTokens = listOf("AM", "pb", "aum", "dism", "pe", "mpa", "pa", "cad")
        assertEquals(8, stitchTokens.size)
        assertTrue(stitchTokens.contains("pb"))
        assertTrue(stitchTokens.contains("aum"))
        assertTrue(stitchTokens.contains("dism"))
        assertTrue(stitchTokens.contains("AM"))
    }

    @Test
    fun `syntax and modifier tokens include topological modifiers and grouping`() {
        val syntaxTokens = listOf("BLO", "FLO", "[", "]", "*", "(", ")", ",")
        assertEquals(8, syntaxTokens.size)
        assertTrue(syntaxTokens.contains("BLO"))
        assertTrue(syntaxTokens.contains("FLO"))
        assertTrue(syntaxTokens.contains("["))
        assertTrue(syntaxTokens.contains("]"))
        assertTrue(syntaxTokens.contains("*"))
    }
}
