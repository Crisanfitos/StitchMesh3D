package com.crisanfitos.stitchmesh3d.core.gauge

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DimensionalModulatorTest {

    private val standard = YarnGaugeRegistry.DEFAULT_STANDARD // w=3.2mm, h=3.0mm, t=2.5mm

    @Test
    fun singleCrochetProducesBaseDimensions() {
        val scDims = StitchDimensionalModulator.calculate(StitchType.SingleCrochet, standard)
        assertEquals(3.20, scDims.widthMm, 0.001)
        assertEquals(3.00, scDims.heightMm, 0.001)
        assertEquals(0.0, scDims.radialOffsetMm, 0.001)
        assertEquals(0.0, scDims.normalDisplacementMm, 0.001)
    }

    @Test
    fun doubleCrochetProducesDoubleHeight() {
        val scDims = StitchDimensionalModulator.calculate(StitchType.SingleCrochet, standard)
        val dcDims = StitchDimensionalModulator.calculate(StitchType.DoubleCrochet, standard)

        assertEquals(scDims.heightMm * 2.0, dcDims.heightMm, 0.001)
        assertEquals(6.00, dcDims.heightMm, 0.001)
        assertEquals(3.20 * 1.2, dcDims.widthMm, 0.001)
    }

    @Test
    fun halfDoubleCrochetProduces1Point4Height() {
        val hdcDims = StitchDimensionalModulator.calculate(StitchType.HalfDoubleCrochet, standard)
        assertEquals(3.00 * 1.4, hdcDims.heightMm, 0.001)
    }

    @Test
    fun volumetricStitchesProducePositiveNormalDisplacement() {
        val bobble = StitchDimensionalModulator.calculate(StitchType.BobbleStitch, standard)
        val popcorn = StitchDimensionalModulator.calculate(StitchType.PopcornStitch, standard)
        val puff = StitchDimensionalModulator.calculate(StitchType.PuffStitch, standard)

        assertEquals(2.50 * 1.5, bobble.normalDisplacementMm, 0.001)
        assertEquals(2.50 * 2.2, popcorn.normalDisplacementMm, 0.001)
        assertEquals(2.50 * 1.0, puff.normalDisplacementMm, 0.001)
        assertTrue(popcorn.normalDisplacementMm > bobble.normalDisplacementMm)
    }

    @Test
    fun frontAndBackPostStitchesProduceRadialOffsets() {
        val fp = StitchDimensionalModulator.calculate(StitchType.FrontPostSingleCrochet, standard)
        val bp = StitchDimensionalModulator.calculate(StitchType.BackPostSingleCrochet, standard)

        assertEquals(2.50 * 0.3, fp.radialOffsetMm, 0.001)
        assertEquals(2.50 * -0.3, bp.radialOffsetMm, 0.001)
    }

    @Test
    fun roundDimensionsAggregateMetricsCorrectly() {
        val stitches = listOf(
            StitchType.SingleCrochet,
            StitchType.SingleCrochet,
            StitchType.Increase, // wRel = 1.8
            StitchType.DoubleCrochet // hRel = 2.0
        )

        val round = StitchDimensionalModulator.calculateRoundDimensions(stitches, standard)
        assertTrue(round.totalCircumferenceMm > 0.0)
        assertEquals(6.00, round.maxHeightMm, 0.001) // DC max height
        assertTrue(round.meanHeightMm > 3.00) // Influenciado al alza por DC
    }
}
