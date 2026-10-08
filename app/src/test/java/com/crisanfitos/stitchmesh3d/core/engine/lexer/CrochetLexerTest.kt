package com.crisanfitos.stitchmesh3d.core.engine.lexer

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.engine.model.TopologyFlag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrochetLexerTest {

    @Test
    fun `tokenizes magic ring round with header and declared count`() {
        val result = CrochetLexer.tokenize("V1: AM [6] (6)")

        assertNotNull(result.roundHeader)
        assertEquals(1, result.roundHeader?.startRound)
        assertEquals(1, result.roundHeader?.endRound)
        assertFalse(result.roundHeader?.isRange == true)

        assertEquals(6, result.declaredCount)

        val stitchTokens = result.tokens.filterIsInstance<CrochetToken.StitchToken>()
        assertEquals(1, stitchTokens.size)
        assertTrue(stitchTokens[0].stitchType is StitchType.MagicRing)
        assertEquals(6, (stitchTokens[0].stitchType as StitchType.MagicRing).stitchCount)
    }

    @Test
    fun `tokenizes complex round with modifiers and volumetric stitches`() {
        val line = "V6: 3 pb, 1 bo, BLO 2 pb (10)"
        val result = CrochetLexer.tokenize(line)

        assertNotNull(result.roundHeader)
        assertEquals(6, result.roundHeader?.startRound)
        assertEquals(10, result.declaredCount)

        val nonDelimiters = result.tokens.filter { it !is CrochetToken.Comma && it !is CrochetToken.DeclaredCount }
        // Expected: [Number(3), StitchToken(pb), Number(1), StitchToken(bo), Modifier(BLO), Number(2), StitchToken(pb)]
        assertEquals(7, nonDelimiters.size)

        assertTrue(nonDelimiters[0] is CrochetToken.Number)
        assertEquals(3, (nonDelimiters[0] as CrochetToken.Number).value)

        assertTrue(nonDelimiters[1] is CrochetToken.StitchToken)
        assertEquals(StitchType.SingleCrochet, (nonDelimiters[1] as CrochetToken.StitchToken).stitchType)

        assertTrue(nonDelimiters[2] is CrochetToken.Number)
        assertEquals(1, (nonDelimiters[2] as CrochetToken.Number).value)

        assertTrue(nonDelimiters[3] is CrochetToken.StitchToken)
        assertEquals(StitchType.BobbleStitch, (nonDelimiters[3] as CrochetToken.StitchToken).stitchType)

        assertTrue(nonDelimiters[4] is CrochetToken.ModifierToken)
        assertEquals(TopologyFlag.BLO, (nonDelimiters[4] as CrochetToken.ModifierToken).flag)

        assertTrue(nonDelimiters[5] is CrochetToken.Number)
        assertEquals(2, (nonDelimiters[5] as CrochetToken.Number).value)

        assertTrue(nonDelimiters[6] is CrochetToken.StitchToken)
        assertEquals(StitchType.SingleCrochet, (nonDelimiters[6] as CrochetToken.StitchToken).stitchType)
    }

    @Test
    fun `tokenizes round range correctly`() {
        val result = CrochetLexer.tokenize("V3-V8: 12 pb (12)")

        assertNotNull(result.roundHeader)
        assertEquals(3, result.roundHeader?.startRound)
        assertEquals(8, result.roundHeader?.endRound)
        assertTrue(result.roundHeader?.isRange == true)
        assertEquals(12, result.declaredCount)

        val stitches = result.tokens.filterIsInstance<CrochetToken.StitchToken>()
        assertEquals(1, stitches.size)
        assertEquals(StitchType.SingleCrochet, stitches[0].stitchType)
    }

    @Test
    fun `tokenizes English patterns with bracket groups and multipliers`() {
        val result = CrochetLexer.tokenize("R2: [1 sc, 1 inc] * 6 (18 sts)")

        assertNotNull(result.roundHeader)
        assertEquals(2, result.roundHeader?.startRound)
        assertEquals(18, result.declaredCount)

        val hasBracketOpen = result.tokens.any { it is CrochetToken.BracketOpen }
        val hasBracketClose = result.tokens.any { it is CrochetToken.BracketClose }
        val hasMultiply = result.tokens.any { it is CrochetToken.Multiply }

        assertTrue(hasBracketOpen)
        assertTrue(hasBracketClose)
        assertTrue(hasMultiply)

        val stitchTokens = result.tokens.filterIsInstance<CrochetToken.StitchToken>()
        assertEquals(2, stitchTokens.size)
        assertEquals(StitchType.SingleCrochet, stitchTokens[0].stitchType)
        assertEquals(StitchType.Increase, stitchTokens[1].stitchType)
    }

    @Test
    fun `tokenizes multi-word stitches like sl st and hdc inc`() {
        val result = CrochetLexer.tokenize("1 sl st, 2 hdc inc (5)")
        assertEquals(5, result.declaredCount)

        val stitches = result.tokens.filterIsInstance<CrochetToken.StitchToken>()
        assertEquals(2, stitches.size)
        assertEquals(StitchType.SlipStitch, stitches[0].stitchType)
        assertEquals(StitchType.HdcIncrease, stitches[1].stitchType)
    }

    @Test
    fun `handles header variations in Spanish and English`() {
        val h1 = CrochetLexer.tokenize("Vuelta 4: 6 pb").roundHeader
        assertEquals(4, h1?.startRound)

        val h2 = CrochetLexer.tokenize("Round 7: 6 sc").roundHeader
        assertEquals(7, h2?.startRound)

        val h3 = CrochetLexer.tokenize("Rounds 3-5: 12 sc (12)").roundHeader
        assertEquals(3, h3?.startRound)
        assertEquals(5, h3?.endRound)

        val h4 = CrochetLexer.tokenize("vta 10: 10 pb").roundHeader
        assertEquals(10, h4?.startRound)
    }

    @Test
    fun `handles lines without header or without declared count`() {
        val noHeader = CrochetLexer.tokenize("6 pb, 1 aum (8)")
        assertNull(noHeader.roundHeader)
        assertEquals(8, noHeader.declaredCount)

        val noDeclared = CrochetLexer.tokenize("V1: 6 pb")
        assertEquals(1, noDeclared.roundHeader?.startRound)
        assertNull(noDeclared.declaredCount)

        val plain = CrochetLexer.tokenize("6 pb")
        assertNull(plain.roundHeader)
        assertNull(plain.declaredCount)
        assertEquals(2, plain.tokens.size)
    }

    @Test
    fun `handles unknown tokens without crashing`() {
        val result = CrochetLexer.tokenize("V1: 3 pb, xyz123, 1 aum (5)")
        assertTrue(result.hasUnknownTokens)
        assertEquals(1, result.unknownTokens.size)
        assertEquals("xyz123", result.unknownTokens[0].raw)
    }
}
