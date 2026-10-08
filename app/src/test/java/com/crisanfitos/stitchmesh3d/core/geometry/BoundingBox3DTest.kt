package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import com.crisanfitos.stitchmesh3d.ui.viewport.components.ViewportDimensionsUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundingBox3DTest {

    @Test
    fun `empty mesh geometry returns zero bounding box`() {
        val emptyMesh = MeshGeometry(
            vertexPositions = FloatArray(0),
            indices = ShortArray(0),
            vertexCount = 0,
            triangleCount = 0
        )

        val bbox = emptyMesh.computeBoundingBox()

        assertTrue(bbox.isEmpty)
        assertEquals(0f, bbox.widthMm, 0.001f)
        assertEquals(0f, bbox.heightMm, 0.001f)
        assertEquals(0f, bbox.depthMm, 0.001f)
        assertEquals(0f, bbox.maxDiameterMm, 0.001f)
    }

    @Test
    fun `known vertices compute precise bounding box`() {
        // Vértices definidos: (x, y, z)
        // P0: (-10, -5, 0)
        // P1: (20, 15, 30)
        // P2: (5, -12, 10)
        val positions = floatArrayOf(
            -10f, -5f, 0f,
            20f, 15f, 30f,
            5f, -12f, 10f
        )
        val mesh = MeshGeometry(
            vertexPositions = positions,
            indices = shortArrayOf(0, 1, 2),
            vertexCount = 3,
            triangleCount = 1
        )

        val bbox = mesh.computeBoundingBox()

        assertFalse(bbox.isEmpty)
        assertEquals(-10f, bbox.minX, 0.001f)
        assertEquals(20f, bbox.maxX, 0.001f)
        assertEquals(30f, bbox.widthMm, 0.001f) // 20 - (-10) = 30

        assertEquals(-12f, bbox.minY, 0.001f)
        assertEquals(15f, bbox.maxY, 0.001f)
        assertEquals(27f, bbox.depthMm, 0.001f) // 15 - (-12) = 27

        assertEquals(0f, bbox.minZ, 0.001f)
        assertEquals(30f, bbox.maxZ, 0.001f)
        assertEquals(30f, bbox.heightMm, 0.001f) // 30 - 0 = 30

        assertEquals(30f, bbox.maxDiameterMm, 0.001f) // max(30, 27) = 30
    }

    @Test
    fun `crochet mesh dimensions scale with hook size and tension (RF-2_1, RF-2_2)`() {
        val fineGauge = YarnGaugeStandard(
            id = 1,
            yarnWeightCategory = YarnWeightCategory.FINE,
            categoryName = "Sport",
            hookSizeMm = 2.5f,
            stitchWidthMm = 2.8f,
            stitchHeightMm = 3.2f,
            stitchThicknessMm = 1.4f
        )

        val bulkyGauge = YarnGaugeStandard(
            id = 5,
            yarnWeightCategory = YarnWeightCategory.BULKY,
            categoryName = "Bulky",
            hookSizeMm = 6.0f,
            stitchWidthMm = 6.6f,
            stitchHeightMm = 7.5f,
            stitchThicknessMm = 3.3f
        )

        val stitchesR1 = List(6) { StitchType.SingleCrochet }
        val stitchesR2 = List(6) { StitchType.Increase }
        val stitchesR3 = List(12) { StitchType.SingleCrochet }

        // Malla con hilado fino
        val fineR1 = RingProfileGenerator.generateRing(1, stitchesR1, fineGauge, null)
        val fineR2 = RingProfileGenerator.generateRing(2, stitchesR2, fineGauge, fineR1)
        val fineR3 = RingProfileGenerator.generateRing(3, stitchesR3, fineGauge, fineR2)
        val fineMesh = AdaptiveTessellator.tessellate(listOf(fineR1, fineR2, fineR3))
        val fineBbox = fineMesh.computeBoundingBox()

        // Malla con hilado grueso
        val bulkyR1 = RingProfileGenerator.generateRing(1, stitchesR1, bulkyGauge, null)
        val bulkyR2 = RingProfileGenerator.generateRing(2, stitchesR2, bulkyGauge, bulkyR1)
        val bulkyR3 = RingProfileGenerator.generateRing(3, stitchesR3, bulkyGauge, bulkyR1)
        val bulkyMesh = AdaptiveTessellator.tessellate(listOf(bulkyR1, bulkyR2, bulkyR3))
        val bulkyBbox = bulkyMesh.computeBoundingBox()

        // El modelo con aguja gruesa debe tener dimensiones métricas mayores
        assertTrue("La altura de la malla gruesa (${bulkyBbox.heightMm} mm) debe superar la fina (${fineBbox.heightMm} mm)",
            bulkyBbox.heightMm > fineBbox.heightMm)
        assertTrue("El diámetro de la malla gruesa (${bulkyBbox.maxDiameterMm} mm) debe superar la fina (${fineBbox.maxDiameterMm} mm)",
            bulkyBbox.maxDiameterMm > fineBbox.maxDiameterMm)
    }

    @Test
    fun `dimensions ui model formats millimeter and centimeter callouts properly`() {
        val model = ViewportDimensionsUiModel(
            widthMm = 124.8f,
            heightMm = 98.2f,
            depthMm = 110.0f
        )

        assertEquals("124 mm × 98 mm × 110 mm", model.formattedDimensions())
        assertEquals("12.5 cm × 9.8 cm × 11.0 cm", model.formattedDimensionsCm())
        assertEquals(124.8f, model.maxDiameterMm, 0.001f)
        assertEquals("Ø 124 mm · H 98 mm", model.formattedDiameterAndHeight())
    }
}
