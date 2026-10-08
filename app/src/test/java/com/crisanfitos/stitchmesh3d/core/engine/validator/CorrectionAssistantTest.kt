package com.crisanfitos.stitchmesh3d.core.engine.validator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectionAssistantTest {

    @Test
    fun `suggestCorrections para error de cierre propone actualizar el recuento declarado`() {
        val raw = "V2: 6 aum (14)"
        val suggestions = CorrectionAssistant.suggestCorrections(
            rawLine = raw,
            previousRoundStitches = 6,
            language = ValidationLanguage.ES
        )

        assertEquals(1, suggestions.size)
        val suggestion = suggestions.first()
        assertEquals(SuggestionActionType.UPDATE_DECLARED_COUNT, suggestion.actionType)
        assertEquals("V2: 6 aum (12)", suggestion.correctedText)
        assertTrue(suggestion.title.contains("Actualizar recuento declarado a (12)"))
        assertTrue(suggestion.explanation.contains("produce 12 puntos reales"))
    }

    @Test
    fun `suggestCorrections para repeticion incompleta propone ajustar multiplicador y anexar puntos`() {
        // [1 pb, 1 aum] consume 2 pts y produce 3.
        // Con * 5 consume 10 pts cuando la anterior tenía 12.
        val raw = "V3: [1 pb, 1 aum] * 5 (15)"
        val suggestions = CorrectionAssistant.suggestCorrections(
            rawLine = raw,
            previousRoundStitches = 12,
            language = ValidationLanguage.ES
        )

        assertTrue(suggestions.isNotEmpty())

        val adjustMultiplier = suggestions.find { it.actionType == SuggestionActionType.ADJUST_MULTIPLIER }
        assertNotNull("Debe existir sugerencia de ajuste de multiplicador", adjustMultiplier)
        assertEquals("V3: [1 pb, 1 aum] * 6 (18)", adjustMultiplier!!.correctedText)
        assertTrue(adjustMultiplier.title.contains("Ajustar repetición a 6 veces"))

        val appendStitches = suggestions.find { it.actionType == SuggestionActionType.APPEND_STITCHES }
        assertNotNull("Debe existir sugerencia de añadir puntos al final", appendStitches)
        assertEquals("V3: [1 pb, 1 aum] * 5, 2 pb (17)", appendStitches!!.correctedText)
    }

    @Test
    fun `suggestCorrections para repeticion excesiva propone reducir el multiplicador`() {
        val raw = "V3: [1 pb, 1 aum] * 7 (21)"
        val suggestions = CorrectionAssistant.suggestCorrections(
            rawLine = raw,
            previousRoundStitches = 12,
            language = ValidationLanguage.ES
        )

        val adjustMultiplier = suggestions.find { it.actionType == SuggestionActionType.ADJUST_MULTIPLIER }
        assertNotNull(adjustMultiplier)
        assertEquals("V3: [1 pb, 1 aum] * 6 (18)", adjustMultiplier!!.correctedText)
        assertTrue(adjustMultiplier.title.contains("Ajustar repetición a 6 veces"))
    }

    @Test
    fun `suggestCorrections para V1 que teje sobre el vacio propone iniciar con Anillo Magico`() {
        val raw = "V1: 6 pb (6)"
        val suggestions = CorrectionAssistant.suggestCorrections(
            rawLine = raw,
            previousRoundStitches = null,
            language = ValidationLanguage.ES
        )

        assertEquals(1, suggestions.size)
        val suggestion = suggestions.first()
        assertEquals(SuggestionActionType.REPLACE_INSTRUCTION, suggestion.actionType)
        assertEquals("V1: AM 6 (6)", suggestion.correctedText)
        assertTrue(suggestion.title.contains("Iniciar con Anillo Mágico (AM 6)"))
    }

    @Test
    fun `suggestCorrections para vuelta plana con puntos faltantes propone igualar o anexar`() {
        val raw = "V4: 10 pb (10)"
        val suggestions = CorrectionAssistant.suggestCorrections(
            rawLine = raw,
            previousRoundStitches = 12,
            language = ValidationLanguage.ES
        )

        val replace = suggestions.find { it.actionType == SuggestionActionType.REPLACE_INSTRUCTION }
        assertNotNull(replace)
        assertEquals("V4: 12 pb (12)", replace!!.correctedText)

        val append = suggestions.find { it.actionType == SuggestionActionType.APPEND_STITCHES }
        assertNotNull(append)
        assertEquals("V4: 10 pb, 2 pb (12)", append!!.correctedText)
    }

    @Test
    fun `suggestCorrections para vuelta valida retorna lista vacia`() {
        val raw = "V2: 6 aum (12)"
        val suggestions = CorrectionAssistant.suggestCorrections(
            rawLine = raw,
            previousRoundStitches = 6
        )

        assertTrue("Vuelta válida no debe generar sugerencias", suggestions.isEmpty())
    }

    @Test
    fun `suggestCorrections genera sugerencias localizadas en ingles`() {
        val raw = "V2: 6 inc (14)"
        val suggestions = CorrectionAssistant.suggestCorrections(
            rawLine = raw,
            previousRoundStitches = 6,
            language = ValidationLanguage.EN
        )

        assertEquals(1, suggestions.size)
        val suggestion = suggestions.first()
        assertTrue(suggestion.title.contains("Update declared stitch count to (12)"))
        assertTrue(suggestion.explanation.contains("The stitch sequence actually produces 12 stitches"))
        assertEquals("V2: 6 inc (12)", suggestion.correctedText)
    }
}
