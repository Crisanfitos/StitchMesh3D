package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.engine.model.TopologyFlag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Tests unitarios para el Stitch Texture Atlas 2x2 y modulación PBR de albedo por hilado (SM-074).
 */
class CrochetTextureGeneratorTest {

    @Test
    fun `quadrantForStitch maps pb, aum, dism, pa and cad to their dedicated 2x2 atlas quadrants`() {
        assertEquals(
            StitchAtlasQuadrant.SINGLE_CROCHET,
            CrochetTextureGenerator.quadrantForStitch(StitchType.SingleCrochet)
        )
        assertEquals(
            StitchAtlasQuadrant.SINGLE_CROCHET,
            CrochetTextureGenerator.quadrantForStitch(StitchType.SingleCrochet.withTopology(TopologyFlag.BLO))
        )

        assertEquals(
            StitchAtlasQuadrant.INCREASE,
            CrochetTextureGenerator.quadrantForStitch(StitchType.Increase)
        )
        assertEquals(
            StitchAtlasQuadrant.INCREASE,
            CrochetTextureGenerator.quadrantForStitch(StitchType.DcIncrease)
        )

        assertEquals(
            StitchAtlasQuadrant.DECREASE,
            CrochetTextureGenerator.quadrantForStitch(StitchType.Decrease)
        )
        assertEquals(
            StitchAtlasQuadrant.DECREASE,
            CrochetTextureGenerator.quadrantForStitch(StitchType.InvisibleDecrease)
        )

        assertEquals(
            StitchAtlasQuadrant.TALL_POST_AND_CHAIN,
            CrochetTextureGenerator.quadrantForStitch(StitchType.DoubleCrochet)
        )
        assertEquals(
            StitchAtlasQuadrant.TALL_POST_AND_CHAIN,
            CrochetTextureGenerator.quadrantForStitch(StitchType.HalfDoubleCrochet)
        )
        assertEquals(
            StitchAtlasQuadrant.TALL_POST_AND_CHAIN,
            CrochetTextureGenerator.quadrantForStitch(StitchType.Chain)
        )
    }

    @Test
    fun `computeAtlasUv restricts UV coordinates strictly inside each stitch quadrant with inset`() {
        for (stitch in listOf(
            StitchType.SingleCrochet,
            StitchType.Increase,
            StitchType.Decrease,
            StitchType.DoubleCrochet
        )) {
            val quadrant = CrochetTextureGenerator.quadrantForStitch(stitch)
            for (lu in listOf(0.0f, 0.5f, 1.0f)) {
                for (lv in listOf(0.0f, 0.5f, 1.0f)) {
                    val uv = CrochetTextureGenerator.computeAtlasUv(stitch, lu, lv)
                    assertTrue(
                        "UV (${uv[0]}, ${uv[1]}) para $stitch debe pertenecer a $quadrant",
                        quadrant.containsUv(uv[0], uv[1])
                    )
                    // Verificar margen interior anti-bleeding (inset = 0.03f)
                    assertTrue(uv[0] >= quadrant.uMin + 0.029f && uv[0] <= quadrant.uMax - 0.029f)
                    assertTrue(uv[1] >= quadrant.vMin + 0.029f && uv[1] <= quadrant.vMax - 0.029f)
                }
            }
        }
    }

    @Test
    fun `generateStitchAtlasRgba produces 4 visually distinct morphological glyphs in the 2x2 quadrants`() {
        val size = 128
        val half = size / 2
        val rgba = CrochetTextureGenerator.generateStitchAtlasRgba(width = size, height = size)
        assertEquals(size * size * 4, rgba.size)

        fun extractQuadrantLuma(startX: Int, startY: Int): IntArray {
            val patch = IntArray(half * half)
            var idx = 0
            for (y in startY until (startY + half)) {
                for (x in startX until (startX + half)) {
                    val p = (y * size + x) * 4
                    patch[idx++] = rgba[p].toInt() and 0xFF
                }
            }
            return patch
        }

        val qPb = extractQuadrantLuma(0, 0)
        val qAum = extractQuadrantLuma(half, 0)
        val qDism = extractQuadrantLuma(0, half)
        val qPa = extractQuadrantLuma(half, half)

        fun l1Distance(a: IntArray, b: IntArray): Long {
            var diff = 0L
            for (i in a.indices) {
                diff += abs(a[i] - b[i])
            }
            return diff
        }

        assertTrue("El glifo pb debe diferir morfológicamente de aum", l1Distance(qPb, qAum) > 5_000L)
        assertTrue("El glifo pb debe diferir morfológicamente de dism", l1Distance(qPb, qDism) > 5_000L)
        assertTrue("El glifo pb debe diferir morfológicamente de pa", l1Distance(qPb, qPa) > 5_000L)
        assertTrue("El glifo aum debe diferir morfológicamente de dism", l1Distance(qAum, qDism) > 5_000L)
        assertTrue("El glifo aum debe diferir morfológicamente de pa", l1Distance(qAum, qPa) > 5_000L)
        assertTrue("El glifo dism debe diferir morfológicamente de pa", l1Distance(qDism, qPa) > 5_000L)
    }

    @Test
    fun `defaultStitchAtlasPng generates valid PNG binary with IHDR, IDAT and IEND chunks`() {
        val png = CrochetTextureGenerator.defaultStitchAtlasPng
        assertTrue("El PNG del atlas debe tener contenido", png.size > 256)

        // Verificar firma PNG (89 50 4E 47 0D 0A 1A 0A)
        assertEquals(0x89.toByte(), png[0])
        assertEquals(0x50.toByte(), png[1])
        assertEquals(0x4E.toByte(), png[2])
        assertEquals(0x47.toByte(), png[3])
    }

    @Test
    fun `modulateYarnAlbedo tints stitch relief and ambient occlusion without dropping to black`() {
        val terracotta = floatArrayOf(0.878f, 0.427f, 0.325f)
        val seamColor = CrochetTextureGenerator.modulateYarnAlbedo(
            stitchType = StitchType.SingleCrochet,
            yarnColorRgb = terracotta,
            localU = 0.01f,
            localV = 0.01f
        )
        val crownColor = CrochetTextureGenerator.modulateYarnAlbedo(
            stitchType = StitchType.SingleCrochet,
            yarnColorRgb = terracotta,
            localU = 0.50f,
            localV = 0.16f
        )

        // La corona iluminada del hilo debe ser más brillante que la junta de oclusión ambiental
        assertTrue(crownColor[0] > seamColor[0])
        // Incluso en la junta de oclusión, el color nunca cae a negro (< 35% del color base)
        assertTrue(seamColor[0] >= terracotta[0] * 0.35f)
        assertNotEquals(seamColor[0], crownColor[0], 0.01f)
    }

    @Test
    fun `evaluateGlyphNormal produces normalized unit tangent space vectors with positive Nz`() {
        for (quadrant in StitchAtlasQuadrant.values()) {
            for (lu in listOf(0.05f, 0.25f, 0.5f, 0.75f, 0.95f)) {
                for (lv in listOf(0.05f, 0.25f, 0.5f, 0.75f, 0.95f)) {
                    val normal = CrochetTextureGenerator.evaluateGlyphNormal(quadrant, lu, lv)
                    assertEquals(3, normal.size)
                    val len = kotlin.math.sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2])
                    assertEquals("El vector normal en espacio tangente debe ser unitario", 1.0f, len, 0.01f)
                    assertTrue("Nz debe ser estrictamente positivo para relieve saliente: ${normal[2]}", normal[2] > 0.0f)
                }
            }
        }
    }

    @Test
    fun `generateNormalMapRgba encodes tangent space normals into valid RGB channels`() {
        val size = 64
        val raw = CrochetTextureGenerator.generateNormalMapRgba(size, size)
        assertEquals(size * size * 4, raw.size)

        for (i in 0 until (size * size)) {
            val r = raw[i * 4].toInt() and 0xFF
            val g = raw[i * 4 + 1].toInt() and 0xFF
            val b = raw[i * 4 + 2].toInt() and 0xFF
            val a = raw[i * 4 + 3].toInt() and 0xFF

            assertEquals("Alpha debe ser 255", 255, a)
            // Canal B decodifica Nz = (b/255)*2 - 1. Como Nz > 0, b debe ser >= 127
            assertTrue("Canal B de normal map debe ser >= 127 para Nz > 0: $b", b >= 127)
        }

        val png = CrochetTextureGenerator.defaultNormalMapPng
        assertTrue("El PNG del normal map debe ser válido", png.size > 200)
        assertEquals(0x89.toByte(), png[0])
    }

    @Test
    fun `generateOcclusionRoughnessRgba encodes ambient occlusion and matte wool roughness`() {
        val size = 64
        val raw = CrochetTextureGenerator.generateOcclusionRoughnessRgba(size, size)
        assertEquals(size * size * 4, raw.size)

        for (i in 0 until (size * size)) {
            val r = raw[i * 4].toInt() and 0xFF // Oclusión ambiental
            val g = raw[i * 4 + 1].toInt() and 0xFF // Rugosidad
            val b = raw[i * 4 + 2].toInt() and 0xFF // Metálico (0.0 para lana)

            assertTrue("Oclusión ambiental R debe estar en rango [80..255]: $r", r in 80..255)
            assertTrue("Rugosidad textil G debe ser mate alta [200..255]: $g", g in 200..255)
            assertEquals("Metálico B debe ser 0 para hilados textiles", 0, b)
        }

        val png = CrochetTextureGenerator.defaultOcclusionRoughnessPng
        assertTrue("El PNG ORM debe ser válido", png.size > 200)
        assertEquals(0x89.toByte(), png[0])
    }
}
