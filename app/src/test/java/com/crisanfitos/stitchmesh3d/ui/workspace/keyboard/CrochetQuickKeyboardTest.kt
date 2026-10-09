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
        val syntaxTokens = listOf("BLO", "FLO", "[", "]", "*", "( )", "(", ")", ",")
        assertEquals(9, syntaxTokens.size)
        assertTrue(syntaxTokens.contains("BLO"))
        assertTrue(syntaxTokens.contains("FLO"))
        assertTrue(syntaxTokens.contains("["))
        assertTrue(syntaxTokens.contains("]"))
        assertTrue(syntaxTokens.contains("*"))
        assertTrue(syntaxTokens.contains("( )"))
    }

    @Test
    fun `insertTokenAtCursor places token at specific index and returns updated cursor`() {
        val original = "6 pb"
        // Insertar ", 1 aum" al final
        val (text1, cursor1) = CrochetTokenFormatter.insertTokenAtCursor(original, ",", 4)
        assertEquals("6 pb,", text1)
        assertEquals(5, cursor1)

        val (text2, cursor2) = CrochetTokenFormatter.insertTokenAtCursor(text1, "aum", 5)
        assertEquals("6 pb, aum", text2)
        assertEquals(9, cursor2)
    }

    @Test
    fun `insertToken handles automatic parentheses pair`() {
        val formatted = CrochetTokenFormatter.insertToken("6 pb", "( )")
        assertEquals("6 pb ()", formatted)

        val (textAtCursor, newCursor) = CrochetTokenFormatter.insertTokenAtCursor("6 pb", "( )", 4)
        assertEquals("6 pb ()", textAtCursor)
        assertEquals(6, newCursor) // Cursor posicionado dentro de los paréntesis
    }

    @Test
    fun `deleteCharBeforeCursor removes character before cursor position`() {
        val (text, newCursor) = CrochetTokenFormatter.deleteCharBeforeCursor("6 pb", 4)
        assertEquals("6 p", text)
        assertEquals(3, newCursor)
    }
}
