package com.crisanfitos.stitchmesh3d.core.gauge

import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GaugeMatrixTest {

    @Test
    fun defaultStandardIsWorstedMedium() {
        val defaultStandard = YarnGaugeRegistry.DEFAULT_STANDARD
        assertEquals(YarnWeightCategory.MEDIUM, defaultStandard.yarnWeightCategory)
        assertEquals(3.50f, defaultStandard.hookSizeMm, 0.01f)
        assertEquals(3.20f, defaultStandard.stitchWidthMm, 0.01f)
        assertEquals(3.00f, defaultStandard.stitchHeightMm, 0.01f)
        assertTrue(defaultStandard.aspectRatio > 1.0f)
    }

    @Test
    fun allStandardsCoverCycRange() {
        val standards = YarnGaugeRegistry.getAllStandards()
        assertTrue(standards.size >= 8)

        // Verificar presencia de extremos y medianos
        assertNotNull(standards.find { it.yarnWeightCategory == YarnWeightCategory.LACE })
        assertNotNull(standards.find { it.yarnWeightCategory == YarnWeightCategory.MEDIUM })
        assertNotNull(standards.find { it.yarnWeightCategory == YarnWeightCategory.JUMBO })

        // Dimensiones crecientes con el grosor del hilado
        val lace = YarnGaugeRegistry.findStandard(YarnWeightCategory.LACE)
        val worsted = YarnGaugeRegistry.findStandard(YarnWeightCategory.MEDIUM)
        val jumbo = YarnGaugeRegistry.findStandard(YarnWeightCategory.JUMBO)

        assertTrue(worsted.stitchWidthMm > lace.stitchWidthMm)
        assertTrue(jumbo.stitchWidthMm > worsted.stitchWidthMm)
        assertTrue(worsted.stitchHeightMm > lace.stitchHeightMm)
        assertTrue(jumbo.stitchHeightMm > worsted.stitchHeightMm)
    }

    @Test
    fun findStandardReturnsClosestHookSize() {
        val standard = YarnGaugeRegistry.findStandard(YarnWeightCategory.MEDIUM, 3.85f)
        assertEquals(4.00f, standard.hookSizeMm, 0.01f)

        val standard35 = YarnGaugeRegistry.findStandard(YarnWeightCategory.MEDIUM, 3.4f)
        assertEquals(3.50f, standard35.hookSizeMm, 0.01f)
    }

    @Test
    fun interpolateStandardCalculatesScaledDimensions() {
        val interpolated = YarnGaugeRegistry.interpolateStandard(YarnWeightCategory.MEDIUM, 5.0f)
        assertEquals(5.0f, interpolated.hookSizeMm, 0.01f)
        // La aguja de 5.0mm es mayor que la de 4.0mm, las dimensiones deben ser proporcionalmente mayores
        val base4 = YarnGaugeRegistry.findStandard(YarnWeightCategory.MEDIUM, 4.0f)
        assertTrue(interpolated.stitchWidthMm > base4.stitchWidthMm)
        assertTrue(interpolated.stitchHeightMm > base4.stitchHeightMm)
    }
}
