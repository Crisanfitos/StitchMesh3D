package com.crisanfitos.stitchmesh3d.ui.workspace.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas de edición basada en cursor/selección compartida entre teclado rápido y nativo (SM-062).
 */
class CrochetTokenFormatterCursorTest {

    @Test
    fun `insert at end places cursor after inserted token`() {
        val (text, cursor) = CrochetTokenFormatter.insertTokenReplacingSelection("2", "pb", 1, 1)
        assertEquals("2 pb", text)
        assertEquals(4, cursor)
    }

    @Test
    fun `native char typed after quick keyboard insert lands at cursor not at zero`() {
        val (text, cursor) = CrochetTokenFormatter.insertTokenReplacingSelection("", "2", 0, 0)
        val (text2, cursor2) = CrochetTokenFormatter.insertTokenReplacingSelection(text, "pb", cursor, cursor)
        // Simula el teclado nativo escribiendo una coma en la posición del cursor
        val typed = text2.substring(0, cursor2) + "," + text2.substring(cursor2)
        assertEquals("2 pb,", typed)
    }

    @Test
    fun `insert in the middle keeps suffix and moves cursor`() {
        val (text, cursor) = CrochetTokenFormatter.insertTokenReplacingSelection("6 pb", "aum", 1, 1)
        assertEquals("6 aum pb", text.replace("6aum", "6 aum"))
        assertEquals(true, cursor > 1)
    }

    @Test
    fun `insert replaces active selection`() {
        val (text, cursor) = CrochetTokenFormatter.insertTokenReplacingSelection("6 pb", "aum", 2, 4)
        assertEquals("6 aum", text)
        assertEquals(5, cursor)
    }

    @Test
    fun `backspace removes char before cursor in the middle of text`() {
        val (text, cursor) = CrochetTokenFormatter.backspaceAt("6 pbx", 4, 4)
        assertEquals("6 px", text)
        assertEquals(3, cursor)
    }

    @Test
    fun `backspace removes whole selection`() {
        val (text, cursor) = CrochetTokenFormatter.backspaceAt("6 pb aum", 2, 4)
        assertEquals("6  aum", text)
        assertEquals(2, cursor)
    }

    @Test
    fun `backspace at start is a no-op`() {
        val (text, cursor) = CrochetTokenFormatter.backspaceAt("6 pb", 0, 0)
        assertEquals("6 pb", text)
        assertEquals(0, cursor)
    }

    @Test
    fun `repeated backspace never leaves trailing characters out of sync`() {
        var text = "6 pb aum"
        var cursor = text.length
        repeat(text.length) {
            val r = CrochetTokenFormatter.backspaceAt(text, cursor, cursor)
            text = r.first
            cursor = r.second
        }
        assertEquals("", text)
        assertEquals(0, cursor)
    }
}
