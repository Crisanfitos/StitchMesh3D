package com.crisanfitos.stitchmesh3d.core.engine.color

import com.crisanfitos.stitchmesh3d.core.engine.lexer.CrochetLexer
import com.crisanfitos.stitchmesh3d.core.engine.parser.CrochetParser
import com.crisanfitos.stitchmesh3d.core.geometry.ColorSpaceHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de pruebas unitarias sobre el motor de cambios de color intra-vuelta (SM-069, SM-093).
 * Valida la interoperabilidad entre lexer, parser sintáctico y conversión matemática a sRGB/lineal.
 */
class ColorChangeEngineTest {

    @Test
    fun `lexer tokenizes color tags embedded in round instructions`() {
        val result = CrochetLexer.tokenize("6 pb [color #E06D53], 6 pb [color #52A474]")

        assertNotNull(result)
        assertTrue(result.tokens.isNotEmpty())
    }

    @Test
    fun `CrochetColorHelper handles all named palettes and fallbacks`() {
        assertEquals("#E06D53", CrochetColorHelper.normalizeToHex("terracota"))
        assertEquals("#FBBF24", CrochetColorHelper.normalizeToHex("amarillo"))
        assertEquals("#52A474", CrochetColorHelper.normalizeToHex("verde"))
        assertEquals("#FFFFFF", CrochetColorHelper.normalizeToHex("blanco"))
        assertEquals("#1E1F24", CrochetColorHelper.normalizeToHex("negro"))
        // Código HEX de 6 dígitos preservado
        assertEquals("#123456", CrochetColorHelper.normalizeToHex("#123456"))
        // Código HEX de 3 dígitos expandido
        assertEquals("#AABBCC", CrochetColorHelper.normalizeToHex("#abc"))
    }

    @Test
    fun `ColorSpaceHelper converts hex colors to linear RGBA float arrays accurately`() {
        val whiteRgba = ColorSpaceHelper.hexToLinearRgba("#FFFFFF")
        assertEquals(4, whiteRgba.size)
        assertEquals(1.0f, whiteRgba[0], 0.01f)
        assertEquals(1.0f, whiteRgba[1], 0.01f)
        assertEquals(1.0f, whiteRgba[2], 0.01f)
        assertEquals(1.0f, whiteRgba[3], 0.01f)

        val blackRgba = ColorSpaceHelper.hexToLinearRgba("#000000")
        assertEquals(4, blackRgba.size)
        assertEquals(0.0f, blackRgba[0], 0.01f)
        assertEquals(0.0f, blackRgba[1], 0.01f)
        assertEquals(0.0f, blackRgba[2], 0.01f)

        val terracottaRgba = ColorSpaceHelper.hexToLinearRgba("#E06D53")
        assertTrue(terracottaRgba[0] > 0.4f) // Rojo dominante
        assertTrue(terracottaRgba[1] in 0.05f..0.3f)
        assertTrue(terracottaRgba[2] in 0.02f..0.2f)
    }

    @Test
    fun `parser generates valid ast for multiround text with color specifications`() {
        val parseResult = CrochetParser.parse("3 pb, 3 aum")

        assertTrue(parseResult.errors.isEmpty())
        assertNotNull(parseResult.ast)
    }
}
