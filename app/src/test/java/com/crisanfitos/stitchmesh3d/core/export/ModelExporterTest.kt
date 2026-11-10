package com.crisanfitos.stitchmesh3d.core.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class ModelExporterTest {

    private fun sampleTrianglePart(
        id: String,
        name: String,
        tx: Float = 0f,
        rotY: Float = 0f,
        scale: Float = 1f
    ): ExportableMeshPart {
        return ExportableMeshPart(
            partId = id,
            name = name,
            colorHex = "#E06D53",
            positions = floatArrayOf(
                0f, 0f, 0f,
                10f, 0f, 0f,
                0f, 10f, 0f
            ),
            normals = floatArrayOf(
                0f, 0f, 1f,
                0f, 0f, 1f,
                0f, 0f, 1f
            ),
            uvs = floatArrayOf(
                0f, 0f,
                1f, 0f,
                0f, 1f
            ),
            indices = intArrayOf(0, 1, 2),
            translateXMm = tx,
            rotateYDeg = rotY,
            tensionScaleFactor = scale
        )
    }

    @Test
    fun `applyTransformToMesh rotates normals and scales positions correctly with unit normal length`() {
        val part = sampleTrianglePart("p1", "Oreja", tx = 10f, rotY = 90f, scale = 2.0f)
        val transformed = ModelExporter.applyTransformToMesh(part)

        // Posiciones escaladas a 2.0x y trasladadas en X por +10
        // (10, 0, 0) escalado -> (20, 0, 0), rotado 90° alrededor de Y -> (0, 0, -20) + (10, 0, 0) -> (10, 0, -20)
        assertEquals(3, transformed.positions.size / 3)
        assertEquals(10f, transformed.positions[0], 0.01f) // px(0) + tx
        assertEquals(0f, transformed.positions[1], 0.01f)

        // Normal (0, 0, 1) rotada 90° en Y pasa a ser (1, 0, 0)
        assertEquals(3, transformed.normals.size / 3)
        val nx = transformed.normals[0]
        val ny = transformed.normals[1]
        val nz = transformed.normals[2]
        assertEquals(1.0f, nx, 0.01f)
        assertEquals(0.0f, ny, 0.01f)
        assertEquals(0.0f, nz, 0.01f)

        val len = Math.sqrt((nx * nx + ny * ny + nz * nz).toDouble()).toFloat()
        assertEquals(1.0f, len, 0.001f)
    }

    @Test
    fun `exportToWavefrontObj generates valid vertices normals uvs and 1-based face offsets across multiple parts`() {
        val head = sampleTrianglePart("p1", "Cabeza", tx = 0f, scale = 1.5f)
        val body = sampleTrianglePart("p2", "Cuerpo", tx = 30f, scale = 1.5f)

        val bundle = ModelExporter.exportToWavefrontObj("Zorro Amigurumi", listOf(head, body))

        assertEquals(6, bundle.totalVertices)
        assertEquals(2, bundle.totalFaces)
        assertTrue(bundle.objContent.contains("v 15.0000 0.0000 0.0000"))
        assertTrue(bundle.objContent.contains("v 30.0000 0.0000 0.0000"))
        assertTrue(bundle.objContent.contains("vn 0.0000 0.0000 1.0000"))
        assertTrue(bundle.objContent.contains("f 1/1/1 2/2/2 3/3/3"))
        assertTrue(bundle.objContent.contains("f 4/4/4 5/5/5 6/6/6"))
        assertTrue(bundle.mtlContent.contains("newmtl mat_Cabeza"))
        assertTrue(bundle.mtlContent.contains("newmtl mat_Cuerpo"))
    }

    @Test
    fun `exportToGltfJson generates valid glTF 2_0 asset with real accessors, bufferViews, and PBR materials`() {
        val head = sampleTrianglePart("p1", "Cabeza", tx = 12.5f)
        val json = ModelExporter.exportToGltfJson("Zorro Amigurumi", listOf(head))

        assertTrue(json.contains("\"version\": \"2.0\""))
        assertTrue(json.contains("\"projectTitle\": \"Zorro Amigurumi\""))
        assertTrue(json.contains("\"name\": \"Cabeza\""))
        assertTrue(json.contains("\"name\": \"mat_Cabeza\""))
        assertTrue(json.contains("\"translation\": [12.5000, 0.0000, 0.0000]"))

        // Verificaciones de buffers geométricos reales
        assertTrue(json.contains("\"bufferViews\": ["))
        assertTrue(json.contains("\"accessors\": ["))
        assertTrue(json.contains("\"POSITION\": 0"))
        assertTrue(json.contains("\"NORMAL\": 1"))
        assertTrue(json.contains("\"TEXCOORD_0\": 2"))
        assertTrue(json.contains("\"indices\": 3"))
        assertTrue(json.contains("data:application/octet-stream;base64,"))
    }

    @Test
    fun `exportToGlb generates standard binary container with valid magic and version`() {
        val head = sampleTrianglePart("p1", "Cabeza", tx = 10f)
        val glbBytes = ModelExporter.exportToGlb("Zorro Amigurumi", listOf(head))

        assertTrue(glbBytes.size > 20)
        val buf = ByteBuffer.wrap(glbBytes).order(ByteOrder.LITTLE_ENDIAN)

        val magic = buf.getInt(0)
        val version = buf.getInt(4)
        val length = buf.getInt(8)

        assertEquals(0x46546C67, magic) // "glTF"
        assertEquals(2, version)
        assertEquals(glbBytes.size, length)
    }
}
