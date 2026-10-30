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

    @Test
    fun `tokenizes literal magic ring with stitch suffixes`() {
        val r1 = CrochetLexer.tokenize("V1: AM 6 pb (6)")
        val s1 = r1.tokens.filterIsInstance<CrochetToken.StitchToken>()
        assertEquals(1, s1.size)
        assertTrue(s1[0].stitchType is StitchType.MagicRing)
        assertEquals(6, (s1[0].stitchType as StitchType.MagicRing).stitchCount)

        val r2 = CrochetLexer.tokenize("V1: anillo mágico 6 pb (6)")
        val s2 = r2.tokens.filterIsInstance<CrochetToken.StitchToken>()
        assertEquals(1, s2.size)
        assertTrue(s2[0].stitchType is StitchType.MagicRing)
        assertEquals(6, (s2[0].stitchType as StitchType.MagicRing).stitchCount)

        val r3 = CrochetLexer.tokenize("V1: MR 8 sc (8)")
        val s3 = r3.tokens.filterIsInstance<CrochetToken.StitchToken>()
        assertEquals(1, s3.size)
        assertTrue(s3[0].stitchType is StitchType.MagicRing)
        assertEquals(8, (s3[0].stitchType as StitchType.MagicRing).stitchCount)
    }

    @Test
    fun `parses foundation chain variants in CrochetParser`() {
        val p1 = com.crisanfitos.stitchmesh3d.core.engine.parser.CrochetParser.parse("V1: 10 cad (10)")
        assertTrue(p1.isSuccess)
        val s1 = p1.ast?.children?.filterIsInstance<com.crisanfitos.stitchmesh3d.core.engine.parser.CrochetAstNode.StitchNode>()
        assertEquals(1, s1?.size)
        assertEquals(StitchType.Chain, s1?.get(0)?.stitchType)
        assertEquals(10, s1?.get(0)?.count)

        val p2 = com.crisanfitos.stitchmesh3d.core.engine.parser.CrochetParser.parse("V1: cad 10 (10)")
        assertTrue(p2.isSuccess)
        val s2 = p2.ast?.children?.filterIsInstance<com.crisanfitos.stitchmesh3d.core.engine.parser.CrochetAstNode.StitchNode>()
        assertEquals(1, s2?.size)
        assertEquals(StitchType.Chain, s2?.get(0)?.stitchType)
        assertEquals(10, s2?.get(0)?.count)
    }

    @Test(timeout = 1000L)
    fun `tokenizes benign punctuation and unicode multiplier without hanging`() {
        val parsedPeriod = com.crisanfitos.stitchmesh3d.core.engine.parser.CrochetParser.parse("V1: 6 pb. (6)")
        assertTrue(parsedPeriod.isSuccess)
        assertEquals(6, parsedPeriod.ast?.totalProducedStitches)

        val parsedSemicolon = com.crisanfitos.stitchmesh3d.core.engine.parser.CrochetParser.parse("V2: 3 pb; 1 aum; 2 pb (7)")
        assertTrue(parsedSemicolon.isSuccess)
        assertEquals(7, parsedSemicolon.ast?.totalProducedStitches)

        val parsedUnicodeMul = com.crisanfitos.stitchmesh3d.core.engine.parser.CrochetParser.parse("V3: [1 pb, 1 aum] × 6 (18)")
        assertTrue(parsedUnicodeMul.isSuccess)
        assertEquals(18, parsedUnicodeMul.ast?.totalProducedStitches)

        val strayColon = CrochetLexer.tokenize("Base: 6 pb")
        assertEquals(1, strayColon.unknownTokens.size)
        assertEquals("Base", strayColon.unknownTokens.first().raw)
        assertEquals(1, strayColon.tokens.filterIsInstance<CrochetToken.StitchToken>().size)
    }

    @Test(timeout = 1000L)
    fun `never hangs on unrecognized symbols malformed hashes or surrogate emojis`() {
        val inputs = listOf(
            "V1: 3 pb + 3 pb / rematar = 6 @ ! ? \" '",
            "#",
            "# ",
            "#12 6 pb",
            "V1: - 6 pb",
            "V1: 6 pb 🧶✨"
        )

        for (input in inputs) {
            val result = CrochetLexer.tokenize(input)
            assertTrue("Expected at least one unknown token for: $input", result.hasUnknownTokens)
            for (unknown in result.unknownTokens) {
                assertTrue("Unknown token must never be empty for: $input", unknown.raw.isNotEmpty())
                assertTrue(
                    "Unknown token indices must advance strictly for: $input",
                    unknown.endIndex > unknown.startIndex
                )
            }
        }

        val emojiResult = CrochetLexer.tokenize("6 pb 🧶")
        assertEquals("🧶", emojiResult.unknownTokens.single().raw)
        assertEquals(2, emojiResult.unknownTokens.single().endIndex - emojiResult.unknownTokens.single().startIndex)

        val invalidHashNumber = CrochetLexer.tokenize("#12 6 pb")
        assertEquals("#12", invalidHashNumber.unknownTokens.single().raw)
    }

    @Test
    fun `meets RND-2 latency under 30ms for 200 rounds with mixed punctuation`() {
        val pattern200 = List(200) { idx ->
            val r = idx + 1
            if (r == 1) "V1: AM [6]. (6)"
            else "V$r: [1 pb; 1 aum] × 3, 6 pb. (15)"
        }

        // Warm-up JVM JIT
        repeat(5) {
            pattern200.forEach { com.crisanfitos.stitchmesh3d.core.engine.parser.CrochetParser.parse(it) }
        }

        val startNanos = System.nanoTime()
        val parsed = pattern200.map { com.crisanfitos.stitchmesh3d.core.engine.parser.CrochetParser.parse(it) }
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000.0

        assertTrue(parsed.all { it.isSuccess })
        assertTrue("Expected < 30 ms for 200 rounds (RND-2), got ${elapsedMs}ms", elapsedMs < 30.0)
    }
}

