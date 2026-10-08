package com.crisanfitos.stitchmesh3d.core.gauge

import com.crisanfitos.stitchmesh3d.core.gauge.model.TensionSwatchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwatchCalibratorTest {

    private val baseStandard = YarnGaugeRegistry.DEFAULT_STANDARD // w=3.2mm, h=3.0mm, t=2.5mm

    @Test
    fun standard10x10MatchesExpectedDimensions() {
        // 10 puntos miden 3.2 cm -> 3.2 mm/punto
        // 10 vueltas miden 3.0 cm -> 3.0 mm/vuelta
        val input = TensionSwatchInput(
            measuredWidthCm = 3.2f,
            measuredHeightCm = 3.0f,
            stitchesCount = 10,
            roundsCount = 10
        )

        val result = SwatchCalibrator.calibrate(input, baseStandard).getOrThrow()

        assertEquals(3.20f, result.customStandard.stitchWidthMm, 0.01f)
        assertEquals(3.00f, result.customStandard.stitchHeightMm, 0.01f)
        assertEquals(0.0f, result.widthDeviationPercent, 0.1f)
        assertEquals(0.0f, result.heightDeviationPercent, 0.1f)
        assertTrue(result.isWithinStandardTolerance)
    }

    @Test
    fun looseTensionDetectsPositiveDeviation() {
        // Muestra más holgada: 10 puntos miden 4.0 cm (4.0 mm/punto)
        // Desviación esperada: (4.0 - 3.2) / 3.2 = +25%
        val input = TensionSwatchInput(
            measuredWidthCm = 4.0f,
            measuredHeightCm = 3.6f,
            stitchesCount = 10,
            roundsCount = 10
        )

        val result = SwatchCalibrator.calibrate(input, baseStandard).getOrThrow()

        assertEquals(4.00f, result.customStandard.stitchWidthMm, 0.01f)
        assertEquals(3.60f, result.customStandard.stitchHeightMm, 0.01f)
        assertEquals(25.0f, result.widthDeviationPercent, 0.1f)
        assertEquals(20.0f, result.heightDeviationPercent, 0.1f)
        assertFalse(result.isWithinStandardTolerance) // > 15%
    }

    @Test
    fun calibrateFrom10x10CmSampleWindow() {
        // 20 puntos en 10 cm -> 100 mm / 20 = 5.0 mm por punto
        // 25 vueltas en 10 cm -> 100 mm / 25 = 4.0 mm por vuelta
        val result = SwatchCalibrator.calibrateFrom10x10Cm(20f, 25f, baseStandard).getOrThrow()

        assertEquals(5.00f, result.customStandard.stitchWidthMm, 0.01f)
        assertEquals(4.00f, result.customStandard.stitchHeightMm, 0.01f)
    }

    @Test
    fun rejectsDegenerateMeasurements() {
        // Ancho ridículamente diminuto (< 0.5 mm)
        val tiny = TensionSwatchInput(
            measuredWidthCm = 0.01f, // 0.1 mm para 10 puntos
            measuredHeightCm = 3.0f
        )
        val tinyResult = SwatchCalibrator.calibrate(tiny, baseStandard)
        assertTrue(tinyResult.isFailure)

        // Ancho gigantesco (> 30 mm)
        val huge = TensionSwatchInput(
            measuredWidthCm = 50.0f, // 50 mm por punto
            measuredHeightCm = 3.0f
        )
        val hugeResult = SwatchCalibrator.calibrate(huge, baseStandard)
        assertTrue(hugeResult.isFailure)
    }
}
