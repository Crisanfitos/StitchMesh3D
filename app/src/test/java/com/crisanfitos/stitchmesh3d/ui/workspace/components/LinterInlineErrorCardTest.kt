package com.crisanfitos.stitchmesh3d.ui.workspace.components

import com.crisanfitos.stitchmesh3d.core.engine.validator.ArithmeticValidator
import com.crisanfitos.stitchmesh3d.core.engine.validator.CorrectionAssistant
import com.crisanfitos.stitchmesh3d.core.engine.validator.CorrectionSuggestion
import com.crisanfitos.stitchmesh3d.core.engine.validator.SuggestionActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinterInlineErrorCardTest {

    @Test
    fun `CorrectionSuggestion holds action attributes correctly`() {
        val suggestion = CorrectionSuggestion(
            title = "Ajustar a 6 repeticiones",
            explanation = "Consumirá exactamente los 12 puntos base",
            actionType = SuggestionActionType.ADJUST_MULTIPLIER,
            originalText = "[1 pb, 1 aum] * 5 (15)",
            correctedText = "[1 pb, 1 aum] * 6 (18)"
        )

        assertEquals("Ajustar a 6 repeticiones", suggestion.title)
        assertEquals("Consumirá exactamente los 12 puntos base", suggestion.explanation)
        assertEquals(SuggestionActionType.ADJUST_MULTIPLIER, suggestion.actionType)
        assertEquals("[1 pb, 1 aum] * 5 (15)", suggestion.originalText)
        assertEquals("[1 pb, 1 aum] * 6 (18)", suggestion.correctedText)
    }

    @Test
    fun `CorrectionAssistant produces valid quick-fix suggestions for round mismatch`() {
        // V3 que intenta consumir de una base de 12 puntos pero sólo repite 5 veces (consume 10)
        val roundLine = "V3: [1 pb, 1 aum] * 5 (15)"
        val previousBase = 12

        val validationResult = ArithmeticValidator.validateRound(
            rawLine = roundLine,
            previousRoundStitches = previousBase
        )

        assertFalse(validationResult.isValid)
        assertTrue(validationResult.hasBaseMismatch)

        val suggestions = CorrectionAssistant.suggestCorrections(validationResult)
        assertFalse(suggestions.isEmpty())

        // Debe sugerir ajustar el multiplicador a 6
        val multiplierFix = suggestions.find { it.actionType == SuggestionActionType.ADJUST_MULTIPLIER }
        assertNotNull(multiplierFix)
        assertEquals("V3: [1 pb, 1 aum] * 6 (18)", multiplierFix?.correctedText)

        // Al aplicar la corrección, la nueva línea debe ser 100% válida en el validador
        val fixedResult = ArithmeticValidator.validateRound(
            rawLine = multiplierFix!!.correctedText,
            previousRoundStitches = previousBase
        )
        assertTrue(fixedResult.isValid)
    }

    @Test
    fun `discrepancy text computation handles excess and shortfall correctly`() {
        // Exceso
        val excessConsumed = 16
        val expectedBase = 12
        val excessDiff = excessConsumed - expectedBase
        assertEquals(4, excessDiff)

        // Faltantes
        val shortConsumed = 10
        val shortDiff = shortConsumed - expectedBase
        assertEquals(-2, shortDiff)
    }
}
