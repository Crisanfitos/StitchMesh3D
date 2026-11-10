package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.engine.parser.StitchInstance
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class ProceduralYarnLoopInstancerTest {

    private val testGauge = YarnGaugeStandard(
        id = 1,
        yarnWeightCategory = YarnWeightCategory.MEDIUM,
        categoryName = "Worsted",
        hookSizeMm = 4.0f,
        stitchWidthMm = 4.4f,
        stitchHeightMm = 5.0f,
        stitchThicknessMm = 2.2f
    )

    private fun createSampleRings(): List<RingProfile> {
        val r1Vertices = (0 until 6).map { idx ->
            val theta = idx * (2.0 * Math.PI) / 6.0
            RingVertex(
                index = idx,
                theta = theta,
                radiusMm = 10.0,
                x = 10.0 * Math.cos(theta),
                y = 10.0 * Math.sin(theta),
                z = 0.0,
                stitchType = StitchType.SingleCrochet,
                colorHex = "#E06D53"
            )
        }
        val r2Vertices = (0 until 12).map { idx ->
            val theta = idx * (2.0 * Math.PI) / 12.0
            RingVertex(
                index = idx,
                theta = theta,
                radiusMm = 20.0,
                x = 20.0 * Math.cos(theta),
                y = 20.0 * Math.sin(theta),
                z = 5.0,
                stitchType = StitchType.SingleCrochet,
                colorHex = "#E06D53"
            )
        }
        val r3Vertices = (0 until 18).map { idx ->
            val theta = idx * (2.0 * Math.PI) / 18.0
            RingVertex(
                index = idx,
                theta = theta,
                radiusMm = 30.0,
                x = 30.0 * Math.cos(theta),
                y = 30.0 * Math.sin(theta),
                z = 10.0,
                stitchType = StitchType.SingleCrochet,
                colorHex = "#E06D53"
            )
        }

        return listOf(
            RingProfile(1, r1Vertices, 60.0, 10.0, 0.0, 0.0),
            RingProfile(2, r2Vertices, 120.0, 20.0, 5.0, 5.0),
            RingProfile(3, r3Vertices, 180.0, 30.0, 10.0, 10.0)
        )
    }

    @Test
    fun extractFramesFromRings_extractsExpectedNumberOfFrames() {
        val rings = createSampleRings()
        val frames = ProceduralYarnLoopInstancer.extractFramesFromRings(rings, testGauge)

        // Ring 1 (6 vertices) + Ring 2 (12 vertices) = 18 stitch cells
        assertEquals(18, frames.size)
        assertTrue("Frames should have valid unit normals", frames.all { it.isOrthonormal(0.2f) })
    }

    @Test
    fun extractFramesFromRings_respectsPeelLimit() {
        val rings = createSampleRings()
        val framesPeel2 = ProceduralYarnLoopInstancer.extractFramesFromRings(rings, testGauge, peelLimit = 2)

        // Only transitions within round <= 2 (Ring 1 -> Ring 2) = 6 frames
        assertEquals(6, framesPeel2.size)
    }

    @Test
    fun extractFramesFromPlanarRows_extractsCorrectFrames() {
        val rows = listOf(
            listOf(
                StitchInstance(StitchType.Chain, 0, colorHex = "#E06D53"),
                StitchInstance(StitchType.Chain, 1, colorHex = "#E06D53")
            ),
            listOf(
                StitchInstance(StitchType.SingleCrochet, 0, colorHex = "#E06D53"),
                StitchInstance(StitchType.SingleCrochet, 1, colorHex = "#E06D53")
            )
        )
        val frames = ProceduralYarnLoopInstancer.extractFramesFromPlanarRows(rows, testGauge)
        assertEquals(4, frames.size)
        assertTrue(frames.all { it.isOrthonormal() })
    }

    @Test
    fun buildVolumetricMesh_generatesValidMeshUnderBudget() {
        val rings = createSampleRings()
        val frames = ProceduralYarnLoopInstancer.extractFramesFromRings(rings, testGauge)
        val mesh = ProceduralYarnLoopInstancer.buildVolumetricMesh(frames)

        assertNotNull("Mesh should be generated within budget", mesh)
        assertTrue("Vertex count must be positive", mesh!!.vertexCount > 0)
        assertTrue("Triangle count must be positive", mesh.triangleCount > 0)
        assertEquals(mesh.indices.size, mesh.triangleCount * 3)
    }

    @Test
    fun buildVolumetricMesh_switchesToLod1WhenPointsIncrease() {
        // Create 1,000 frames: 1,000 * 60 = 60,000 > 50,000 (LOD0 budget exceeded)
        // 1,000 * 24 = 24,000 <= 50,000 -> Should switch to LOD1!
        val dummyFrame = StitchTbnFrame(
            origin = floatArrayOf(0f, 0f, 0f),
            tangent = floatArrayOf(1f, 0f, 0f),
            bitangent = floatArrayOf(0f, 1f, 0f),
            normal = floatArrayOf(0f, 0f, 1f),
            stitchType = StitchType.SingleCrochet
        )
        val frames = List(1_000) { dummyFrame }

        val mesh = ProceduralYarnLoopInstancer.buildVolumetricMesh(frames, polygonBudget = 50_000)
        assertNotNull(mesh)
        // LOD1 has 24 triangles per single crochet prototype
        assertEquals(1_000 * 24, mesh!!.triangleCount)
    }

    @Test
    fun buildVolumetricMesh_returnsNullWhenExceedingTotalBudget() {
        // Create 3,000 frames: 3,000 * 24 = 72,000 > 50,000 budget -> Should fallback (return null)
        val dummyFrame = StitchTbnFrame(
            origin = floatArrayOf(0f, 0f, 0f),
            tangent = floatArrayOf(1f, 0f, 0f),
            bitangent = floatArrayOf(0f, 1f, 0f),
            normal = floatArrayOf(0f, 0f, 1f),
            stitchType = StitchType.SingleCrochet
        )
        val frames = List(3_000) { dummyFrame }

        val mesh = ProceduralYarnLoopInstancer.buildVolumetricMesh(frames, polygonBudget = 50_000)
        assertNull("Should fallback to null when budget is exceeded", mesh)
    }

    @Test
    fun benchmark_2000PointsAssembleInUnder50Ms() {
        val dummyFrame = StitchTbnFrame(
            origin = floatArrayOf(5f, 10f, 15f),
            tangent = floatArrayOf(1f, 0f, 0f),
            bitangent = floatArrayOf(0f, 1f, 0f),
            normal = floatArrayOf(0f, 0f, 1f),
            stitchType = StitchType.SingleCrochet,
            colorHex = "#E06D53"
        )
        val frames = List(2_000) { dummyFrame }

        // Warm up JIT
        ProceduralYarnLoopInstancer.buildVolumetricMesh(frames, polygonBudget = 100_000)

        // Benchmark
        val elapsedMs = measureTimeMillis {
            val mesh = ProceduralYarnLoopInstancer.buildVolumetricMesh(frames, polygonBudget = 100_000)
            assertNotNull(mesh)
        }

        println("Benchmark SM-086: 2,000 stitch instances assembled in $elapsedMs ms")
        assertTrue("Assembling 2,000 points must complete in < 50 ms (got $elapsedMs ms)", elapsedMs < 50)
    }
}
