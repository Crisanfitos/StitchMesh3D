package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.engine.parser.StitchInstance
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import com.crisanfitos.stitchmesh3d.domain.model.ProjectStructureType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PlanarMeshGeneratorTest {

    private val testGauge = YarnGaugeStandard(
        id = 1,
        yarnWeightCategory = YarnWeightCategory.MEDIUM,
        categoryName = "Worsted #4",
        hookSizeMm = 4.0f,
        stitchWidthMm = 4.4f,
        stitchHeightMm = 5.0f,
        stitchThicknessMm = 2.2f
    )

    @Test
    fun `generateRowByRowMesh produces valid 2-manifold closed slab with yarn thickness`() {
        // 4 hileras de 10 puntos bajos
        val rows = List(4) {
            List(10) { idx ->
                StitchInstance(stitchType = StitchType.SingleCrochet, indexInRound = idx)
            }
        }

        val mesh = PlanarMeshGenerator.generateRowByRowMesh(
            rows = rows,
            gauge = testGauge,
            defaultColorHex = "#E06D53"
        )

        assertNotNull(mesh)
        assertTrue(mesh.vertexCount > 0)
        assertTrue(mesh.triangleCount > 0)
        assertEquals(mesh.triangleCount * 3, mesh.indices.size)
        assertEquals(mesh.vertexCount * 3, mesh.vertexPositions.size)
        assertEquals(mesh.vertexCount * 3, mesh.vertexNormals.size)

        // Verificar espesor físico de hilado en Z (debe ser exactamente stitchThicknessMm = 2.2f)
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE
        for (i in 0 until mesh.vertexCount) {
            val z = mesh.vertexPositions[i * 3 + 2]
            if (z < minZ) minZ = z
            if (z > maxZ) maxZ = z
        }

        val expectedHalfT = testGauge.stitchThicknessMm / 2.0f
        assertEquals(-expectedHalfT, minZ, 0.01f)
        assertEquals(expectedHalfT, maxZ, 0.01f)
        assertEquals(testGauge.stitchThicknessMm, maxZ - minZ, 0.02f)

        // Verificar que hay normales apuntando hacia +Z (tapa superior) y -Z (tapa inferior)
        var hasTopNormals = false
        var hasBottomNormals = false
        var hasSideNormals = false

        for (i in 0 until mesh.vertexCount) {
            val normal = mesh.getVertexNormal(i)
            if (normal[2] > 0.9f) hasTopNormals = true
            if (normal[2] < -0.9f) hasBottomNormals = true
            if (abs(normal[2]) < 0.1f && (abs(normal[0]) > 0.9f || abs(normal[1]) > 0.9f)) hasSideNormals = true
        }

        assertTrue("Debe contener normales hacia arriba en la cara superior", hasTopNormals)
        assertTrue("Debe contener normales hacia abajo en la cara inferior", hasBottomNormals)
        assertTrue("Debe contener normales laterales en el borde perimetral", hasSideNormals)
    }

    @Test
    fun `generateConcentricMesh produces valid granny square mesh with thickness and center fan`() {
        // 3 vueltas concéntricas (6, 12, 18 puntos)
        val rounds = listOf(
            List(6) { StitchInstance(StitchType.SingleCrochet, it) },
            List(12) { StitchInstance(StitchType.SingleCrochet, it) },
            List(18) { StitchInstance(StitchType.SingleCrochet, it) }
        )

        val mesh = PlanarMeshGenerator.generateConcentricMesh(
            rounds = rounds,
            gauge = testGauge,
            isSquare = true,
            defaultColorHex = "#52A474"
        )

        assertNotNull(mesh)
        assertTrue(mesh.vertexCount > 0)
        assertTrue(mesh.triangleCount > 0)

        // Verificar que el centro está cerrado en origen (0, 0, +/- halfT)
        val centerTop = mesh.getVertexPosition(0)
        assertEquals(0f, centerTop[0], 0.001f)
        assertEquals(0f, centerTop[1], 0.001f)
        assertEquals(testGauge.stitchThicknessMm / 2f, centerTop[2], 0.01f)

        val centerBot = mesh.getVertexPosition(1)
        assertEquals(0f, centerBot[0], 0.001f)
        assertEquals(0f, centerBot[1], 0.001f)
        assertEquals(-testGauge.stitchThicknessMm / 2f, centerBot[2], 0.01f)
    }

    @Test
    fun `generatePlanarMesh preserves vertex colors for multicolored tapestry rows`() {
        // Hilera con alternancia de colores (#FFFFFF y #000000)
        val row = List(6) { idx ->
            StitchInstance(
                stitchType = StitchType.SingleCrochet,
                indexInRound = idx,
                colorHex = if (idx % 2 == 0) "#FFFFFF" else "#000000"
            )
        }

        val mesh = PlanarMeshGenerator.generatePlanarMesh(
            rounds = listOf(row),
            gauge = testGauge,
            structureType = ProjectStructureType.FLAT_GARMENT
        )

        assertTrue(mesh.vertexColors.isNotEmpty())
        assertEquals(mesh.vertexCount * 4, mesh.vertexColors.size)

        var foundWhite = false
        var foundBlack = false
        for (i in 0 until mesh.vertexCount) {
            val r = mesh.vertexColors[i * 4]
            val g = mesh.vertexColors[i * 4 + 1]
            val b = mesh.vertexColors[i * 4 + 2]
            if (r > 0.9f && g > 0.9f && b > 0.9f) foundWhite = true
            if (r < 0.1f && g < 0.1f && b < 0.1f) foundBlack = true
        }

        assertTrue("Debe registrar vértices blancos", foundWhite)
        assertTrue("Debe registrar vértices negros", foundBlack)
    }

    @Test
    fun `generatePlanarMesh handles empty or degenerate input gracefully`() {
        val mesh = PlanarMeshGenerator.generatePlanarMesh(
            rounds = emptyList(),
            gauge = testGauge
        )

        assertEquals(0, mesh.vertexCount)
        assertEquals(0, mesh.triangleCount)
        assertEquals(0, mesh.vertexPositions.size)
    }

    @Test
    fun `benchmark planar mesh generation runs in under 15ms`() {
        val largeRows = List(50) {
            List(20) { idx ->
                StitchInstance(StitchType.SingleCrochet, idx)
            }
        }

        // Calentamiento JIT
        repeat(3) {
            PlanarMeshGenerator.generatePlanarMesh(largeRows, testGauge)
        }

        val start = System.nanoTime()
        val mesh = PlanarMeshGenerator.generatePlanarMesh(largeRows, testGauge)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0

        assertTrue(mesh.vertexCount > 0)
        assertTrue("La generación de 50 hileras debe tomar < 15ms (tomó ${elapsedMs}ms)", elapsedMs < 15.0)
    }
}
