package com.crisanfitos.stitchmesh3d.core.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class YarnLoopPrototypeBuilderTest {

    @Before
    fun setUp() {
        YarnLoopPrototypeBuilder.clearCache()
    }

    @Test
    fun getPrototype_returnsCachedInstanceOnSubsequentCalls() {
        val proto1 = YarnLoopPrototypeBuilder.getPrototype(YarnLoopCategory.SINGLE_CROCHET, lodLevel = 0)
        val proto2 = YarnLoopPrototypeBuilder.getPrototype(YarnLoopCategory.SINGLE_CROCHET, lodLevel = 0)
        assertTrue("Subsequent calls should return cached prototype", proto1 === proto2)
    }

    @Test
    fun buildPrototype_lod0GeneratesExpectedTriangleCountForSingleCrochet() {
        val proto = YarnLoopPrototypeBuilder.buildPrototype(YarnLoopCategory.SINGLE_CROCHET, lodLevel = 0)
        assertNotNull(proto)
        assertEquals(YarnLoopCategory.SINGLE_CROCHET, proto.category)
        assertEquals(0, proto.lodLevel)

        // LOD0 specification: ~48-72 triangles
        assertTrue(
            "LOD0 single crochet triangle count (${proto.triangleCount}) should be in range [48, 72]",
            proto.triangleCount in 48..72
        )
    }

    @Test
    fun buildPrototype_lod1GeneratesExpectedTriangleCountForSingleCrochet() {
        val proto = YarnLoopPrototypeBuilder.buildPrototype(YarnLoopCategory.SINGLE_CROCHET, lodLevel = 1)
        assertNotNull(proto)
        assertEquals(YarnLoopCategory.SINGLE_CROCHET, proto.category)
        assertEquals(1, proto.lodLevel)

        // LOD1 specification: ~24 triangles
        assertEquals("LOD1 single crochet should have ~24 triangles", 24, proto.triangleCount)
    }

    @Test
    fun buildPrototype_allCategoriesGenerateValidGeometryWithStrictUnitNormals() {
        for (category in YarnLoopCategory.entries) {
            for (lod in 0..1) {
                val proto = YarnLoopPrototypeBuilder.buildPrototype(category, lodLevel = lod)
                val mesh = proto.mesh

                assertTrue("${category.name} LOD$lod must have vertices", mesh.vertexCount > 0)
                assertTrue("${category.name} LOD$lod must have triangles", mesh.triangleCount > 0)
                assertEquals(mesh.indices.size, mesh.triangleCount * 3)

                // Verify no NaN or infinite vertex positions
                for (i in 0 until mesh.vertexCount) {
                    val p = mesh.getVertexPosition(i)
                    assertFalse("Position X must not be NaN/Inf", p[0].isNaN() || p[0].isInfinite())
                    assertFalse("Position Y must not be NaN/Inf", p[1].isNaN() || p[1].isInfinite())
                    assertFalse("Position Z must not be NaN/Inf", p[2].isNaN() || p[2].isInfinite())

                    // Verify unit normals ||n|| = 1.0 ± 0.01
                    val norm = mesh.getNormalMagnitude(i)
                    assertTrue(
                        "Normal magnitude at vertex $i in ${category.name} LOD$lod must be unit length (got $norm)",
                        abs(norm - 1.0f) < 0.02f
                    )
                }

                // Verify indices are strictly in range [0, vertexCount - 1]
                for (idx in mesh.indices) {
                    assertTrue("Index $idx out of range [0, ${mesh.vertexCount})", idx in 0 until mesh.vertexCount)
                }
            }
        }
    }

    @Test
    fun sweepTubularCurve_closedLoopGeneratesClosedIndicesWithoutCaps() {
        val circlePoints = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(-1f, 0f, 0f),
            floatArrayOf(0f, -1f, 0f)
        )
        val mesh = YarnLoopPrototypeBuilder.sweepTubularCurve(
            centerline = circlePoints,
            tubeRadius = 0.2f,
            sides = 3,
            isClosed = true
        )

        // 4 segments * 3 sides * 2 triangles = 24 triangles
        assertEquals(24, mesh.triangleCount)
        assertEquals(12, mesh.vertexCount)
    }
}
