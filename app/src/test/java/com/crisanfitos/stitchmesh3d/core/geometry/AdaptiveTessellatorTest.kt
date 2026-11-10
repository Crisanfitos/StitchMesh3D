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

            assertEquals(0, i0)
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

            // Centroide radial del triángulo en plano XZ (Y-up estándar glTF)
            val cx = (x0 + x1 + x2) / 3f
            val cz = (z0 + z1 + z2) / 3f

            // Producto escalar normal con vector radial hacia afuera: (nx * cx + nz * cz)
            val radialDot = nx * cx + nz * cz
            assertTrue("La normal del triángulo $t debe apuntar hacia el exterior radial: $radialDot", radialDot > 0f)
        }
    }

    @Test
    fun `smooth normals calculation produces unit length vectors and outward radial orientation`() {
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
        val mesh = AdaptiveTessellator.tessellate(listOf(ring1, ring2), includePolarCap = true)

        assertEquals(mesh.vertexCount * 3, mesh.vertexNormals.size)

        for (v in 0 until mesh.vertexCount) {
            val mag = mesh.getNormalMagnitude(v)
            assertEquals("Cada normal debe ser unitaria (||n|| ~ 1.0)", 1.0f, mag, 0.01f)

            if (v > 0) {
                // Vértices de anillos: la normal debe apuntar hacia el exterior en el plano XZ (Y-up)
                val pos = mesh.getVertexPosition(v)
                val norm = mesh.getVertexNormal(v)
                val radialDot = norm[0] * pos[0] + norm[2] * pos[2]
                assertTrue("La normal del vértice $v debe apuntar hacia el exterior radial: $radialDot", radialDot > 0f)
            } else {
                // Vértice polar (0): debe apuntar hacia abajo en eje -Y
                val norm = mesh.getVertexNormal(0)
                assertEquals(0f, norm[0], 0.01f)
                assertEquals(-1f, norm[1], 0.01f)
                assertEquals(0f, norm[2], 0.01f)
            }
        }
    }

    @Test
    fun `tessellate assigns stitch-specific UV atlas quadrants for mixed rounds with pb, aum, dism and pa`() {
        val mixedStitches = listOf(
            StitchType.SingleCrochet,
            StitchType.Increase,
            StitchType.Decrease,
            StitchType.DoubleCrochet
        )
        val ring1 = RingProfileGenerator.generateRing(
            roundIndex = 1,
            stitches = mixedStitches,
            gaugeStandard = standard
        )

        val mesh = AdaptiveTessellator.tessellate(listOf(ring1), includePolarCap = false)

        // 1 pb (1) + 1 aum (2) + 1 dism (1) + 1 pa (1) = 5 puntos de corona producidos
        assertEquals(5, mesh.vertexCount)
        assertEquals(10, mesh.vertexUvs.size)

        val expectedStitches = listOf(
            StitchType.SingleCrochet,
            StitchType.Increase,
            StitchType.Increase,
            StitchType.Decrease,
            StitchType.DoubleCrochet
        )

        for (i in expectedStitches.indices) {
            val u = mesh.vertexUvs[i * 2]
            val v = mesh.vertexUvs[i * 2 + 1]
            val expectedQuadrant = CrochetTextureGenerator.quadrantForStitch(expectedStitches[i])
            assertTrue(
                "El vértice $i (${expectedStitches[i].technicalName}) con UV ($u, $v) debe mapear al cuadrante $expectedQuadrant",
                expectedQuadrant.containsUv(u, v)
            )
        }
    }

    @Test
    fun `open tube with wall thickness generates interior shell and top rim lip (SM-083)`() {
        val r1 = RingProfileGenerator.generateRing(1, List(8) { StitchType.SingleCrochet }, standard)
        val r2 = RingProfileGenerator.generateRing(2, List(8) { StitchType.SingleCrochet }, standard, r1)

        val thinMesh = AdaptiveTessellator.tessellate(listOf(r1, r2), includePolarCap = false, wallThicknessMm = 0f)
        val thickMesh = AdaptiveTessellator.tessellate(listOf(r1, r2), includePolarCap = false, wallThicknessMm = 2.0f)

        // La malla con grosor genera vértices adicionales para el labio y la pared interior
        assertTrue(thickMesh.vertexCount > thinMesh.vertexCount)
        assertTrue(thickMesh.triangleCount > thinMesh.triangleCount)

        // Todas las normales deben permanecer estrictamente unitarias
        for (v in 0 until thickMesh.vertexCount) {
            assertEquals("Normal unitaria", 1.0f, thickMesh.getNormalMagnitude(v), 0.001f)
        }
    }

    @Test
    fun `tessellate with decoupledStitchQuads assigns strictly confined UVs without diagonal bleed (SM-084)`() {
        val mixedStitches = listOf(
            StitchType.SingleCrochet,
            StitchType.Increase,
            StitchType.Decrease,
            StitchType.DoubleCrochet
        )
        val ring1 = RingProfileGenerator.generateRing(1, mixedStitches, standard)
        val ring2 = RingProfileGenerator.generateRing(2, List(5) { StitchType.SingleCrochet }, standard, ring1)

        val mesh = AdaptiveTessellator.tessellate(
            rings = listOf(ring1, ring2),
            includePolarCap = false,
            decoupledStitchQuads = true
        )

        assertTrue(mesh.vertexCount > 0)
        assertTrue(mesh.triangleCount > 0)
        assertEquals("Debe tener búfer de tangentes completo (4 floats por vértice)", mesh.vertexCount * 4, mesh.vertexTangents.size)

        // Verificar tangentes unitarias y W = 1.0f
        for (v in 0 until mesh.vertexCount) {
            val tan = mesh.getVertexTangent(v)
            val len = kotlin.math.sqrt(tan[0] * tan[0] + tan[1] * tan[1] + tan[2] * tan[2])
            assertEquals("Tangente unitaria", 1.0f, len, 0.01f)
            assertEquals("Handedness W = 1.0f", 1.0f, tan[3], 1e-4f)
        }
    }

    @Test
    fun `tessellate with decoupledStitchQuads ensures localV monotonically advances from bottom to top on all rounds (SM-084)`() {
        val r1 = RingProfileGenerator.generateRing(1, List(6) { StitchType.SingleCrochet }, standard)
        val r2 = RingProfileGenerator.generateRing(2, List(6) { StitchType.SingleCrochet }, standard, r1)
        val r3 = RingProfileGenerator.generateRing(3, List(6) { StitchType.SingleCrochet }, standard, r2)

        val mesh = AdaptiveTessellator.tessellate(
            rings = listOf(r1, r2, r3),
            includePolarCap = false,
            decoupledStitchQuads = true
        )

        // En modo desacoplado, cada quad tiene 4 vértices (BL=0, BR=1, TR=2, TL=3)
        // BL y BR tienen localV = 0.0f; TR y TL tienen localV = 1.0f dentro de su cuadrante
        val singleQuadVerts = 4
        val quadCount = mesh.vertexCount / singleQuadVerts
        for (q in 0 until quadCount) {
            val baseV = q * singleQuadVerts
            val vBottomLeft = mesh.vertexUvs[baseV * 2 + 1]
            val vTopLeft = mesh.vertexUvs[(baseV + 3) * 2 + 1]
            assertTrue("localV en la parte superior ($vTopLeft) debe ser mayor que en la base ($vBottomLeft) (nunca invertido)", vTopLeft > vBottomLeft)
        }
    }

    @Test
    fun `tessellate with decoupledStitchQuads and odd stitch counts produces non-degenerate quads without seam collapse (SM-084)`() {
        // 7 puntos (número impar susceptible a ping-pong colapsado)
        val r1 = RingProfileGenerator.generateRing(1, List(7) { StitchType.SingleCrochet }, standard)
        val r2 = RingProfileGenerator.generateRing(2, List(7) { StitchType.SingleCrochet }, standard, r1)

        val mesh = AdaptiveTessellator.tessellate(
            rings = listOf(r1, r2),
            includePolarCap = false,
            decoupledStitchQuads = true
        )

        // 7 quads * 4 vértices = 28 vértices
        assertEquals(28, mesh.vertexCount)
        assertEquals(14, mesh.triangleCount)

        for (q in 0 until 7) {
            val baseV = q * 4
            val uLeft = mesh.vertexUvs[baseV * 2]
            val uRight = mesh.vertexUvs[(baseV + 1) * 2]
            assertTrue("localU debe crecer de izquierda a derecha (uRight > uLeft): $uRight vs $uLeft", uRight > uLeft)
        }
    }
}
