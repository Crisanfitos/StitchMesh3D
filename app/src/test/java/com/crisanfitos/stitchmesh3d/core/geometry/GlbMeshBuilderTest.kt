package com.crisanfitos.stitchmesh3d.core.geometry

import androidx.compose.ui.graphics.Color
import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.gauge.YarnGaugeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteOrder

/**
 * Pruebas unitarias para [GlbMeshBuilder], verificando la conformidad binaria con Khronos glTF 2.0.
 */
class GlbMeshBuilderTest {

    private val standard = YarnGaugeRegistry.DEFAULT_STANDARD

    @Test
    fun `buildGlb generates valid binary glTF 2_0 container with correct headers and chunks`() {
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

        val buffer = GlbMeshBuilder.buildGlb(
            mesh = mesh,
            yarnColor = Color(0xFFE06D53),
            roughness = 0.9f
        )

        assertTrue("El búfer debe ser directo", buffer.isDirect)
        assertTrue("La capacidad del buffer debe ser > 0", buffer.capacity() > 0)
        assertEquals("El orden debe ser Little-Endian", ByteOrder.LITTLE_ENDIAN, buffer.order())

        // 1. Header (12 bytes)
        val magic = buffer.getInt(0)
        val version = buffer.getInt(4)
        val totalLength = buffer.getInt(8)

        assertEquals("Magic debe ser 0x46546C67 ('glTF')", 0x46546C67, magic)
        assertEquals("Versión debe ser 2", 2, version)
        assertEquals("Total length debe coincidir con la capacidad del búfer", buffer.capacity(), totalLength)

        // 2. Chunk 0: JSON Chunk
        val jsonLength = buffer.getInt(12)
        val jsonChunkType = buffer.getInt(16)
        assertEquals("JSON chunk type debe ser 0x4E4F534A ('JSON')", 0x4E4F534A, jsonChunkType)
        assertEquals("La longitud del JSON debe ser múltiplo de 4", 0, jsonLength % 4)

        // Extraer texto JSON
        val jsonBytes = ByteArray(jsonLength)
        buffer.position(20)
        buffer.get(jsonBytes)
        val jsonStr = String(jsonBytes, Charsets.UTF_8)

        assertTrue("Debe contener especificación glTF 2.0", jsonStr.contains("\"version\": \"2.0\""))
        assertTrue("Debe declarar material Wool PBR", jsonStr.contains("WoolPbrMaterial"))
        assertTrue("Debe tener rugosidad 0.9", jsonStr.contains("\"roughnessFactor\": 0.9"))
        assertTrue("Debe ser dieléctrico no metálico (metallic 0.0)", jsonStr.contains("\"metallicFactor\": 0.0"))
        assertTrue("Debe declarar atributo POSITION", jsonStr.contains("\"POSITION\": 0"))
        assertTrue("Debe declarar atributo NORMAL", jsonStr.contains("\"NORMAL\": 1"))
        assertTrue("Debe declarar índices", jsonStr.contains("\"indices\": 2"))

        // 3. Chunk 1: BIN Chunk
        val binHeaderOffset = 20 + jsonLength
        val binLength = buffer.getInt(binHeaderOffset)
        val binChunkType = buffer.getInt(binHeaderOffset + 4)
        assertEquals("BIN chunk type debe ser 0x004E4942 ('BIN\\0')", 0x004E4942, binChunkType)
        assertEquals("La longitud del BIN debe ser múltiplo de 4", 0, binLength % 4)

        val expectedBinDataLength = (mesh.vertexCount * 3 * 4) + (mesh.vertexCount * 3 * 4) + (mesh.indices.size * 2)
        assertTrue(binLength >= expectedBinDataLength)
    }

    @Test
    fun `buildGlb handles empty mesh gracefully without crashing`() {
        val emptyMesh = MeshGeometry(
            vertexPositions = FloatArray(0),
            indices = ShortArray(0),
            vertexCount = 0,
            triangleCount = 0
        )

        val buffer = GlbMeshBuilder.buildGlb(emptyMesh)
        val magic = buffer.getInt(0)
        val version = buffer.getInt(4)
        assertEquals(0x46546C67, magic)
        assertEquals(2, version)
    }
}
