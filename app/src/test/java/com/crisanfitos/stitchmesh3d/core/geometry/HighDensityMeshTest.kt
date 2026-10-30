package com.crisanfitos.stitchmesh3d.core.geometry

import androidx.compose.ui.graphics.Color
import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pruebas unitarias y de estrés para mallas 3D de alta densidad (SM-081):
 * - Soporte de índices de 32 bits (>= 32.768 y > 65.535 vértices).
 * - Ausencia de desbordamientos aritméticos (Short signed overflow) y ArrayIndexOutOfBoundsException.
 * - Validación de generación glTF 2.0 con UNSIGNED_INT (componentType 5125).
 */
class HighDensityMeshTest {

    private fun generateHighDensityRings(totalVerticesTarget: Int): List<RingProfile> {
        val rings = ArrayList<RingProfile>()
        val ringCount = 100
        val verticesPerRing = (totalVerticesTarget / ringCount).coerceAtLeast(10)
        for (r in 1..ringCount) {
            val z = r * 1.5
            val radius = 30.0
            val vertices = List(verticesPerRing) { j ->
                val theta = (2 * Math.PI * j) / verticesPerRing
                RingVertex(
                    index = j,
                    theta = theta,
                    radiusMm = radius,
                    x = radius * cos(theta),
                    y = radius * sin(theta),
                    z = z,
                    stitchType = StitchType.SingleCrochet
                )
            }
            rings.add(
                RingProfile(
                    roundIndex = r,
                    vertices = vertices,
                    circumferenceMm = 2 * Math.PI * radius,
                    meanRadiusMm = radius,
                    minZ = z,
                    maxZ = z
                )
            )
        }
        return rings
    }

    @Test
    fun `mesh with over 35000 vertices does not overflow short indices and computes unit normals`() {
        // En ShortArray previo, el vértice 32.768 se convertía en -32.768 provocando ArrayIndexOutOfBoundsException
        val rings = generateHighDensityRings(36_000)
        val mesh = AdaptiveTessellator.tessellate(rings, includePolarCap = true)

        assertTrue("La malla debe superar los 32.768 vértices", mesh.vertexCount >= 35_000)
        assertTrue("La cantidad de triángulos debe ser proporcional", mesh.triangleCount >= 35_000)

        // Verificar que ningún índice sea negativo
        for (idx in mesh.indices) {
            assertTrue("Ningún índice puede ser negativo (desbordamiento de Short)", idx >= 0)
            assertTrue("El índice debe estar dentro de la cota de vértices", idx < mesh.vertexCount)
        }

        // Verificar que las normales se calcularon correctamente y tienen magnitud unitaria
        val normal0Mag = mesh.getNormalMagnitude(0)
        assertEquals(1.0f, normal0Mag, 0.05f)

        val normalHalfMag = mesh.getNormalMagnitude(33_000)
        assertEquals(1.0f, normalHalfMag, 0.05f)

        // Verificación de suavizado por StuffingInflationFilter sin crasheos
        val inflated = StuffingInflationFilter.applyInflation(mesh)
        assertNotNull(inflated)
        assertEquals(mesh.vertexCount, inflated.vertexCount)
    }

    @Test
    fun `mesh with over 65535 vertices uses 32-bit unsigned int glTF accessor componentType 5125`() {
        val rings = generateHighDensityRings(68_000)
        val mesh = AdaptiveTessellator.tessellate(rings, includePolarCap = false)

        assertTrue("La malla debe superar los 65.535 vértices", mesh.vertexCount > 65_535)

        val glbBuffer = GlbMeshBuilder.buildGlb(
            mesh = mesh,
            yarnColor = Color(0xFFE06D53),
            roughness = 0.9f,
            isWireframe = false
        )

        assertNotNull(glbBuffer)
        assertTrue("El buffer GLB debe tener contenido", glbBuffer.capacity() > 0)

        // Leer el JSON chunk del GLB para verificar que se emitió "componentType": 5125
        glbBuffer.rewind()
        val magic = glbBuffer.getInt()
        assertEquals(0x46546C67, magic) // "glTF"
        val version = glbBuffer.getInt()
        assertEquals(2, version)
        val totalLength = glbBuffer.getInt()
        assertEquals(glbBuffer.capacity(), totalLength)

        val jsonChunkLength = glbBuffer.getInt()
        val jsonChunkType = glbBuffer.getInt()
        assertEquals(0x4E4F534A, jsonChunkType) // "JSON"

        val jsonBytes = ByteArray(jsonChunkLength)
        glbBuffer.get(jsonBytes)
        val jsonString = String(jsonBytes, Charsets.UTF_8)

        assertTrue(
            "El accessor de índices debe usar componentType 5125 (UNSIGNED_INT) para > 65535 vértices",
            jsonString.contains("\"componentType\": 5125")
        )
        assertFalse(
            "No debe usar 5123 (UNSIGNED_SHORT) para más de 65535 vértices",
            jsonString.contains("\"componentType\": 5123")
        )
    }

    @Test
    fun `standard mesh with under 65535 vertices uses 16-bit unsigned short glTF accessor componentType 5123`() {
        val rings = generateHighDensityRings(1_000)
        val mesh = AdaptiveTessellator.tessellate(rings, includePolarCap = false)

        assertTrue("La malla estándar debe tener <= 65.535 vértices", mesh.vertexCount <= 65_535)

        val glbBuffer = GlbMeshBuilder.buildGlb(
            mesh = mesh,
            yarnColor = Color(0xFF52A474),
            roughness = 0.8f,
            isWireframe = false
        )

        glbBuffer.rewind()
        glbBuffer.getInt() // magic
        glbBuffer.getInt() // version
        glbBuffer.getInt() // totalLength
        val jsonChunkLength = glbBuffer.getInt()
        glbBuffer.getInt() // jsonChunkType
        val jsonBytes = ByteArray(jsonChunkLength)
        glbBuffer.get(jsonBytes)
        val jsonString = String(jsonBytes, Charsets.UTF_8)

        assertTrue(
            "Mallas estándar deben usar componentType 5123 (UNSIGNED_SHORT) para optimizar memoria GPU",
            jsonString.contains("\"componentType\": 5123")
        )
    }
}
