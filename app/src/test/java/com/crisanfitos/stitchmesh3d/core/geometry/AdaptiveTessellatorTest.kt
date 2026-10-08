package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.gauge.YarnGaugeRegistry
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveTessellatorTest {

    private val standard: YarnGaugeStandard = YarnGaugeRegistry.DEFAULT_STANDARD

    @Test
    fun `polar cap generates closed triangle fan with valid indices for initial ring`() {
        val ring1 = RingProfileGenerator.generateRing(
            roundIndex = 1,
            stitches = List(6) { StitchType.SingleCrochet },
            gaugeStandard = standard
        )

        val mesh = AdaptiveTessellator.tessellate(listOf(ring1), includePolarCap = true)

        // 1 vértice polar + 6 vértices de anillo = 7 vértices
        assertEquals(7, mesh.vertexCount)
        // 6 triángulos cerrando el fondo polar
        assertEquals(6, mesh.triangleCount)
        assertEquals(18, mesh.indices.size)

        // Comprobar que todos los índices están en el rango válido [0, vertexCount - 1]
        for (idx in mesh.indices) {
            assertTrue("Índice debe ser >= 0", idx >= 0)
            assertTrue("Índice debe ser < ${mesh.vertexCount}", idx < mesh.vertexCount)
        }

        // Cada triángulo del abanico conecta el polo (0) con dos vértices consecutivos del anillo
        for (t in 0 until mesh.triangleCount) {
            val i0 = mesh.indices[t * 3]
            val i1 = mesh.indices[t * 3 + 1]
            val i2 = mesh.indices[t * 3 + 2]

            assertEquals(0.toShort(), i0)
            assertNotEquals(i1, i2)
            assertNotEquals(i0, i1)
        }
    }

    @Test
    fun `isomorphic transition between equal rings generates exactly 2N quads without gaps`() {
        val ring1 = RingProfileGenerator.generateRing(
            roundIndex = 1,
            stitches = List(6) { StitchType.SingleCrochet },
            gaugeStandard = standard
        )
        val ring2 = RingProfileGenerator.generateRing(
            roundIndex = 2,
            stitches = List(6) { StitchType.SingleCrochet },
            gaugeStandard = standard,
            previousRing = ring1
        )

        val mesh = AdaptiveTessellator.tessellate(listOf(ring1, ring2), includePolarCap = false)

        assertEquals(12, mesh.vertexCount)
        // 6 quads * 2 triángulos = 12 triángulos
        assertEquals(12, mesh.triangleCount)
        assertEquals(36, mesh.indices.size)

        for (idx in mesh.indices) {
            assertTrue(idx in 0 until mesh.vertexCount)
        }

        // No debe haber triángulos degenerados
        for (t in 0 until mesh.triangleCount) {
            val i0 = mesh.indices[t * 3]
            val i1 = mesh.indices[t * 3 + 1]
            val i2 = mesh.indices[t * 3 + 2]

            assertNotEquals(i0, i1)
            assertNotEquals(i1, i2)
            assertNotEquals(i0, i2)
        }
    }

    @Test
    fun `expansion transition with increases produces divergent triangles spanning both rings`() {
        // V1: 6 puntos, V2: 12 puntos (6 aum)
        val ring1 = RingProfileGenerator.generateRing(
            roundIndex = 1,
            stitches = List(6) { StitchType.SingleCrochet },
            gaugeStandard = standard
        )
        val ring2 = RingProfileGenerator.generateRing(
            roundIndex = 2,
            stitches = List(12) { StitchType.SingleCrochet },
            gaugeStandard = standard,
            previousRing = ring1
        )

        val mesh = AdaptiveTessellator.tessellate(listOf(ring1, ring2), includePolarCap = false)

        assertEquals(18, mesh.vertexCount)
        // La cinta adaptativa entre 6 y 12 puntos genera N0 + N1 = 18 triángulos
        assertEquals(18, mesh.triangleCount)

        // Validar rango y no degeneración
        for (t in 0 until mesh.triangleCount) {
            val i0 = mesh.indices[t * 3]
            val i1 = mesh.indices[t * 3 + 1]
            val i2 = mesh.indices[t * 3 + 2]

            assertTrue(i0 in 0 until mesh.vertexCount)
            assertTrue(i1 in 0 until mesh.vertexCount)
            assertTrue(i2 in 0 until mesh.vertexCount)

            assertNotEquals(i0, i1)
            assertNotEquals(i1, i2)
            assertNotEquals(i0, i2)
        }
    }

    @Test
    fun `reduction transition with decreases produces convergent triangles`() {
        // V1: 12 puntos, V2: 6 puntos (6 dism)
        val ring1 = RingProfileGenerator.generateRing(
            roundIndex = 1,
            stitches = List(12) { StitchType.SingleCrochet },
            gaugeStandard = standard
        )
        val ring2 = RingProfileGenerator.generateRing(
            roundIndex = 2,
            stitches = List(6) { StitchType.SingleCrochet },
            gaugeStandard = standard,
            previousRing = ring1
        )

        val mesh = AdaptiveTessellator.tessellate(listOf(ring1, ring2), includePolarCap = false)

        assertEquals(18, mesh.vertexCount)
        assertEquals(18, mesh.triangleCount)

        for (t in 0 until mesh.triangleCount) {
            val i0 = mesh.indices[t * 3]
            val i1 = mesh.indices[t * 3 + 1]
            val i2 = mesh.indices[t * 3 + 2]

            assertTrue(i0 in 0 until mesh.vertexCount)
            assertTrue(i1 in 0 until mesh.vertexCount)
            assertTrue(i2 in 0 until mesh.vertexCount)

            assertNotEquals(i0, i1)
            assertNotEquals(i1, i2)
            assertNotEquals(i0, i2)
        }
    }

    @Test
    fun `winding order produces outward facing normals on cylinder mesh`() {
        val ring1 = RingProfileGenerator.generateRing(
            roundIndex = 1,
            stitches = List(8) { StitchType.SingleCrochet },
            gaugeStandard = standard
        )
        val ring2 = RingProfileGenerator.generateRing(
            roundIndex = 2,
            stitches = List(8) { StitchType.SingleCrochet },
            gaugeStandard = standard,
            previousRing = ring1
        )

        val mesh = AdaptiveTessellator.tessellate(listOf(ring1, ring2), includePolarCap = false)

        // Comprobar que las normales de los triángulos apuntan radialmente hacia fuera (dot product > 0)
        for (t in 0 until mesh.triangleCount) {
            val i0 = mesh.indices[t * 3].toInt()
            val i1 = mesh.indices[t * 3 + 1].toInt()
            val i2 = mesh.indices[t * 3 + 2].toInt()

            val x0 = mesh.vertexPositions[i0 * 3]
            val y0 = mesh.vertexPositions[i0 * 3 + 1]
            val z0 = mesh.vertexPositions[i0 * 3 + 2]

            val x1 = mesh.vertexPositions[i1 * 3]
            val y1 = mesh.vertexPositions[i1 * 3 + 1]
            val z1 = mesh.vertexPositions[i1 * 3 + 2]

            val x2 = mesh.vertexPositions[i2 * 3]
            val y2 = mesh.vertexPositions[i2 * 3 + 1]
            val z2 = mesh.vertexPositions[i2 * 3 + 2]

            // Vectores borde A = V1 - V0, B = V2 - V0
            val ax = x1 - x0
            val ay = y1 - y0
            val az = z1 - z0

            val bx = x2 - x0
            val by = y2 - y0
            val bz = z2 - z0

            // Normal = A x B
            val nx = ay * bz - az * by
            val ny = az * bx - ax * bz
            val nz = ax * by - ay * bx

            // Centroide radial del triángulo
            val cx = (x0 + x1 + x2) / 3f
            val cy = (y0 + y1 + y2) / 3f

            // Producto escalar normal con vector radial hacia afuera: (nx * cx + ny * cy)
            val radialDot = nx * cx + ny * cy
            assertTrue("La normal del triángulo $t debe apuntar hacia el exterior radial: $radialDot", radialDot > 0f)
        }
    }
}
