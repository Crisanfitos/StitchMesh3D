package com.crisanfitos.stitchmesh3d.core.engine.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumetricStitchTest {

    @Test
    fun `BLO modifier preserves C and P invariants while setting BLO flag`() {
        val sc = StitchType.SingleCrochet
        val bloSc = sc.withTopology(TopologyFlag.BLO)

        assertEquals(sc.consumedStitches, bloSc.consumedStitches)
        assertEquals(sc.producedStitches, bloSc.producedStitches)
        assertEquals(sc.deltaStitches, bloSc.deltaStitches)
        assertEquals(sc.hRel, bloSc.hRel, 0.001)
        assertEquals(sc.wRel, bloSc.wRel, 0.001)
        assertTrue(bloSc.topologyFlags.contains(TopologyFlag.BLO))
    }

    @Test
    fun `FLO modifier preserves C and P invariants on increases and decreases`() {
        val inc = StitchType.Increase
        val floInc = inc.withTopology(TopologyFlag.FLO)
        assertEquals(1, floInc.consumedStitches)
        assertEquals(2, floInc.producedStitches)
        assertEquals(1, floInc.deltaStitches)
        assertTrue(floInc.topologyFlags.contains(TopologyFlag.FLO))

        val dec = StitchType.Decrease
        val floDec = dec.withTopology(TopologyFlag.FLO)
        assertEquals(2, floDec.consumedStitches)
        assertEquals(1, floDec.producedStitches)
        assertEquals(-1, floDec.deltaStitches)
        assertTrue(floDec.topologyFlags.contains(TopologyFlag.FLO))
    }

    @Test
    fun `front post stitches have positive radial displacement +0_3 and FRONT_POST flag`() {
        val fpsc = StitchType.FrontPostSingleCrochet
        assertEquals(1, fpsc.consumedStitches)
        assertEquals(1, fpsc.producedStitches)
        assertEquals(0, fpsc.deltaStitches)
        assertEquals(0.3, fpsc.deltaRRel, 0.001)
        assertTrue(fpsc.topologyFlags.contains(TopologyFlag.FRONT_POST))

        val fphdc = StitchType.FrontPostHalfDoubleCrochet
        assertEquals(0.3, fphdc.deltaRRel, 0.001)
        assertTrue(fphdc.topologyFlags.contains(TopologyFlag.FRONT_POST))

        val fpdc = StitchType.FrontPostDoubleCrochet
        assertEquals(0.3, fpdc.deltaRRel, 0.001)
        assertTrue(fpdc.topologyFlags.contains(TopologyFlag.FRONT_POST))
    }

    @Test
    fun `back post stitches have negative radial displacement -0_3 and BACK_POST flag`() {
        val bpsc = StitchType.BackPostSingleCrochet
        assertEquals(1, bpsc.consumedStitches)
        assertEquals(1, bpsc.producedStitches)
        assertEquals(0, bpsc.deltaStitches)
        assertEquals(-0.3, bpsc.deltaRRel, 0.001)
        assertTrue(bpsc.topologyFlags.contains(TopologyFlag.BACK_POST))

        val bphdc = StitchType.BackPostHalfDoubleCrochet
        assertEquals(-0.3, bphdc.deltaRRel, 0.001)
        assertTrue(bphdc.topologyFlags.contains(TopologyFlag.BACK_POST))

        val bpdc = StitchType.BackPostDoubleCrochet
        assertEquals(-0.3, bpdc.deltaRRel, 0.001)
        assertTrue(bpdc.topologyFlags.contains(TopologyFlag.BACK_POST))
    }

    @Test
    fun `volumetric stitches consume 1 and produce 1 maintaining formal stitch count`() {
        // Bobble (garbanzo): C=1, P=1, Δn = +1.5
        val bobble = StitchType.BobbleStitch
        assertEquals(1, bobble.consumedStitches)
        assertEquals(1, bobble.producedStitches)
        assertEquals(0, bobble.deltaStitches)
        assertEquals(1.5, bobble.normalDisplacement, 0.001)

        // Popcorn (palomita): C=1, P=1, Δn = +2.2
        val popcorn = StitchType.PopcornStitch
        assertEquals(1, popcorn.consumedStitches)
        assertEquals(1, popcorn.producedStitches)
        assertEquals(0, popcorn.deltaStitches)
        assertEquals(2.2, popcorn.normalDisplacement, 0.001)

        // Puff (piña): C=1, P=1, Δn = +1.0
        val puff = StitchType.PuffStitch
        assertEquals(1, puff.consumedStitches)
        assertEquals(1, puff.producedStitches)
        assertEquals(0, puff.deltaStitches)
        assertEquals(1.0, puff.normalDisplacement, 0.001)

        // Reverse Single Crochet (cangrejo): C=1, P=1
        val crab = StitchType.ReverseSingleCrochet
        assertEquals(1, crab.consumedStitches)
        assertEquals(1, crab.producedStitches)
        assertEquals(0, crab.deltaStitches)
    }

    @Test
    fun `stitch registry resolves BLO and FLO prefixes and volumetric aliases`() {
        val bloPb = StitchRegistry.resolve("blo pb")
        assertNotNull(bloPb)
        assertEquals(1, bloPb?.consumedStitches)
        assertEquals(1, bloPb?.producedStitches)
        assertTrue(bloPb?.topologyFlags?.contains(TopologyFlag.BLO) == true)

        val floSc = StitchRegistry.resolve("FLO SC")
        assertNotNull(floSc)
        assertTrue(floSc?.topologyFlags?.contains(TopologyFlag.FLO) == true)

        assertEquals(StitchType.FrontPostSingleCrochet, StitchRegistry.resolve("fpsc"))
        assertEquals(StitchType.BackPostSingleCrochet, StitchRegistry.resolve("bpsc"))

        assertEquals(StitchType.BobbleStitch, StitchRegistry.resolve("bo"))
        assertEquals(StitchType.BobbleStitch, StitchRegistry.resolve("garbanzo"))

        assertEquals(StitchType.PopcornStitch, StitchRegistry.resolve("pop"))
        assertEquals(StitchType.PopcornStitch, StitchRegistry.resolve("palomita"))

        assertEquals(StitchType.PuffStitch, StitchRegistry.resolve("puff"))
        assertEquals(StitchType.PuffStitch, StitchRegistry.resolve("piña"))

        assertEquals(StitchType.ReverseSingleCrochet, StitchRegistry.resolve("cangrejo"))
        assertEquals(StitchType.ReverseSingleCrochet, StitchRegistry.resolve("crab"))
    }
}
