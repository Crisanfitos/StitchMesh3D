package com.crisanfitos.stitchmesh3d.ui.workspace.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas unitarias para las reglas sintácticas del teclado rápido y nativo (SM-063):
 * - Unión de dígitos contiguos ("23 pb").
 * - Auto-cierre de corchetes y paréntesis con cursor en el interior.
 */
class CrochetTokenFormatterRulesTest {

    @Test
    fun insertConsecutiveDigits_joinsDigitsWithoutSpace() {
        val (step1Text, step1Cursor) = CrochetTokenFormatter.insertTokenAtCursor("", "2", 0)
        assertEquals("2", step1Text)
        assertEquals(1, step1Cursor)

        val (step2Text, step2Cursor) = CrochetTokenFormatter.insertTokenAtCursor(step1Text, "3", step1Cursor)
        assertEquals("23", step2Text)
        assertEquals(2, step2Cursor)

        val (step3Text, step3Cursor) = CrochetTokenFormatter.insertTokenAtCursor(step2Text, "4", step2Cursor)
        assertEquals("234", step3Text)
        assertEquals(3, step3Cursor)
    }

    @Test
    fun insertStitchAfterDigits_addsSpaceCorrectly() {
        val (text, cursor) = CrochetTokenFormatter.insertTokenAtCursor("23", "pb", 2)
        assertEquals("23 pb", text)
        assertEquals(5, cursor)
    }

    @Test
    fun insertOpenBracket_autoClosesWithCursorInside() {
        val (text, cursor) = CrochetTokenFormatter.insertTokenAtCursor("", "[", 0)
        assertEquals("[]", text)
        assertEquals(1, cursor)
    }

    @Test
    fun insertOpenBracketAfterContent_addsSpaceAndPlacesCursorInside() {
        val (text, cursor) = CrochetTokenFormatter.insertTokenAtCursor("6", "[", 1)
        assertEquals("6 []", text)
        assertEquals(3, cursor)
    }

    @Test
    fun insertOpenParenthesis_autoClosesWithCursorInside() {
        val (text, cursor) = CrochetTokenFormatter.insertTokenAtCursor("", "(", 0)
        assertEquals("()", text)
        assertEquals(1, cursor)
    }

    @Test
    fun insertParenthesisToken_autoClosesWithCursorInside() {
        val (text, cursor) = CrochetTokenFormatter.insertTokenAtCursor("", "( )", 0)
        assertEquals("()", text)
        assertEquals(1, cursor)
    }

    @Test
    fun insertTokenConvenience_joinsDigits() {
        assertEquals("23", CrochetTokenFormatter.insertToken("2", "3"))
        assertEquals("23 pb", CrochetTokenFormatter.insertToken("23", "pb"))
        assertEquals("[", CrochetTokenFormatter.insertToken("", "["))
        assertEquals("()", CrochetTokenFormatter.insertToken("", "( )"))
    }

    @Test
    fun autoCloseDelimiters_forNativeBracket_insertsClosingBracket() {
        val (text, cursor) = CrochetTokenFormatter.autoCloseDelimiters(
            oldText = "",
            newText = "[",
            newCursor = 1
        )
        assertEquals("[]", text)
        assertEquals(1, cursor)
    }

    @Test
    fun autoCloseDelimiters_forNativeParenthesis_insertsClosingParenthesis() {
        val (text, cursor) = CrochetTokenFormatter.autoCloseDelimiters(
            oldText = "6 pb, ",
            newText = "6 pb, (",
            newCursor = 7
        )
        assertEquals("6 pb, ()", text)
        assertEquals(7, cursor)
    }

    @Test
    fun autoCloseDelimiters_skipsDuplicateClosingBracket() {
        val (text, cursor) = CrochetTokenFormatter.autoCloseDelimiters(
            oldText = "6 pb, [1 aum]",
            newText = "6 pb, [1 aum]]",
            newCursor = 13
        )
        assertEquals("6 pb, [1 aum]", text)
        assertEquals(13, cursor)
    }

    @Test
    fun insertTokenAtCursor_skipsClosingBracketWhenAlreadyPresent() {
        val (text, cursor) = CrochetTokenFormatter.insertTokenAtCursor(
            currentText = "[1 pb]",
            token = "]",
            cursorPosition = 5
        )
        assertEquals("[1 pb]", text)
        assertEquals(6, cursor)
    }

    @Test
    fun autoCloseDelimiters_forRegularCharacters_leavesTextUnchanged() {
        val (textDigit, cursorDigit) = CrochetTokenFormatter.autoCloseDelimiters(
            oldText = "2",
            newText = "23",
            newCursor = 2
        )
        assertEquals("23", textDigit)
        assertEquals(2, cursorDigit)

        val (textChar, cursorChar) = CrochetTokenFormatter.autoCloseDelimiters(
            oldText = "p",
            newText = "pb",
            newCursor = 2
        )
        assertEquals("pb", textChar)
        assertEquals(2, cursorChar)
    }

    @Test
    fun autoCloseDelimiters_onDeletion_leavesTextUnchanged() {
        val (text, cursor) = CrochetTokenFormatter.autoCloseDelimiters(
            oldText = "[]",
            newText = "[",
            newCursor = 1
        )
        assertEquals("[", text)
        assertEquals(1, cursor)
    }
}
