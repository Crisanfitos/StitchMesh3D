package com.crisanfitos.stitchmesh3d.core.engine.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StitchTypeTest {

    @Test
    fun `single crochet has exact standard amigurumi constants`() {
        val sc = StitchType.SingleCrochet
        assertEquals(1, sc.consumedStitches)
        assertEquals(1, sc.producedStitches)
        assertEquals(0, sc.deltaStitches)
        assertEquals(1.0, sc.hRel, 0.001)
        assertEquals(1.0, sc.wRel, 0.001)
        assertEquals(0.0, sc.deltaRRel, 0.001)
    }

    @Test
    fun `increase consumes 1 and produces 2 with delta plus 1`() {
        val inc = StitchType.Increase
        assertEquals(1, inc.consumedStitches)
        assertEquals(2, inc.producedStitches)
        assertEquals(1, inc.deltaStitches)
        assertEquals(1.0, inc.hRel, 0.001)
        assertEquals(1.8, inc.wRel, 0.001)
        assertEquals(0.05, inc.deltaRRel, 0.001)
    }

    @Test
    fun `decrease consumes 2 and produces 1 with delta minus 1`() {
        val dec = StitchType.Decrease
        assertEquals(2, dec.consumedStitches)
        assertEquals(1, dec.producedStitches)
        assertEquals(-1, dec.deltaStitches)
        assertEquals(1.0, dec.hRel, 0.001)
        assertEquals(0.9, dec.wRel, 0.001)
        assertEquals(-0.05, dec.deltaRRel, 0.001)
    }

    @Test
    fun `magic ring produces N stitches without consuming prior round stitches`() {
        val mr6 = StitchType.MagicRing(6)
        assertEquals(0, mr6.consumedStitches)
        assertEquals(6, mr6.producedStitches)
        assertEquals(6, mr6.deltaStitches)
        assertEquals(0.5, mr6.hRel, 0.001)
        assertEquals(0.0, mr6.deltaRRel, 0.001)

        val mr12 = StitchType.MagicRing(12)
        assertEquals(12, mr12.producedStitches)
        assertEquals(12, mr12.deltaStitches)
    }

    @Test
    fun `all TRD section 2_2 standard stitches have valid delta and non-negative parameters`() {
        val allStitches = StitchRegistry.allStandardStitches()
        assertEquals(17, allStitches.size)

        for (stitch in allStitches) {
            assertTrue("Consumed stitches must be >= 0", stitch.consumedStitches >= 0)
            assertTrue("Produced stitches must be >= 0", stitch.producedStitches >= 0)
            assertTrue("hRel must be >= 0", stitch.hRel >= 0.0)
            assertTrue("wRel must be >= 0", stitch.wRel >= 0.0)
            assertEquals(
                "Delta must be P - C",
                stitch.producedStitches - stitch.consumedStitches,
                stitch.deltaStitches
            )
        }
    }

    @Test
    fun `stitch registry resolves Spanish and US abbreviations case insensitively`() {
        assertEquals(StitchType.SingleCrochet, StitchRegistry.resolve("pb"))
        assertEquals(StitchType.SingleCrochet, StitchRegistry.resolve("PB"))
        assertEquals(StitchType.SingleCrochet, StitchRegistry.resolve("sc"))
        assertEquals(StitchType.SingleCrochet, StitchRegistry.resolve("SC"))
        assertEquals(StitchType.SingleCrochet, StitchRegistry.resolve("mp"))

        assertEquals(StitchType.Increase, StitchRegistry.resolve("aum"))
        assertEquals(StitchType.Increase, StitchRegistry.resolve("AUM"))
        assertEquals(StitchType.Increase, StitchRegistry.resolve("inc"))

        assertEquals(StitchType.Decrease, StitchRegistry.resolve("dism"))
        assertEquals(StitchType.Decrease, StitchRegistry.resolve("DISM"))
        assertEquals(StitchType.Decrease, StitchRegistry.resolve("dec"))
        assertEquals(StitchType.Decrease, StitchRegistry.resolve("sc2tog"))

        assertEquals(StitchType.DoubleCrochet, StitchRegistry.resolve("pa"))
        assertEquals(StitchType.DoubleCrochet, StitchRegistry.resolve("dc"))
        assertEquals(StitchType.HalfDoubleCrochet, StitchRegistry.resolve("pma"))
        assertEquals(StitchType.HalfDoubleCrochet, StitchRegistry.resolve("hdc"))

        assertEquals(StitchType.SlipStitch, StitchRegistry.resolve("pe"))
        assertEquals(StitchType.SlipStitch, StitchRegistry.resolve("sl st"))
        assertEquals(StitchType.Chain, StitchRegistry.resolve("cad"))
        assertEquals(StitchType.Chain, StitchRegistry.resolve("ch"))

        assertEquals(StitchType.InvisibleDecrease, StitchRegistry.resolve("invdec"))
        assertEquals(StitchType.TripleIncrease, StitchRegistry.resolve("aum3"))
        assertEquals(StitchType.TripleDecrease, StitchRegistry.resolve("dism3"))
        assertEquals(StitchType.Skip, StitchRegistry.resolve("saltar"))
        assertEquals(StitchType.Skip, StitchRegistry.resolve("sk"))
    }

    @Test
    fun `stitch registry resolves parameterized magic ring variants`() {
        val am6 = StitchRegistry.resolve("AM [6]")
        assertNotNull(am6)
        assertTrue(am6 is StitchType.MagicRing)
        assertEquals(6, (am6 as StitchType.MagicRing).stitchCount)

        val am8 = StitchRegistry.resolve("am 8")
        assertNotNull(am8)
        assertEquals(8, (am8 as StitchType.MagicRing).stitchCount)

        val mr10 = StitchRegistry.resolve("MR [10]")
        assertNotNull(mr10)
        assertEquals(10, (mr10 as StitchType.MagicRing).stitchCount)
    }

    @Test
    fun `stitch registry returns null for invalid symbols`() {
        assertNull(StitchRegistry.resolve("xyz123"))
        assertNull(StitchRegistry.resolve(""))
        assertNull(StitchRegistry.resolve("   "))
    }
}
