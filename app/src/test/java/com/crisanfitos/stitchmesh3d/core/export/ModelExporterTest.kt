package com.crisanfitos.stitchmesh3d.core.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelExporterTest {

    private fun sampleTrianglePart(
        id: String,
        name: String,
        tx: Float = 0f,
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
            tensionScaleFactor = scale
        )
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
        assertTrue(bundle.objContent.contains("f 1/1/1 2/2/2 3/3/3"))
        assertTrue(bundle.objContent.contains("f 4/4/4 5/5/5 6/6/6"))
        assertTrue(bundle.mtlContent.contains("newmtl mat_Cabeza"))
        assertTrue(bundle.mtlContent.contains("newmtl mat_Cuerpo"))
    }

    @Test
    fun `exportToGltfJson generates valid glTF 2_0 asset with scene nodes and PBR materials`() {
        val head = sampleTrianglePart("p1", "Cabeza", tx = 12.5f)
        val json = ModelExporter.exportToGltfJson("Zorro Amigurumi", listOf(head))

        assertTrue(json.contains("\"version\": \"2.0\""))
        assertTrue(json.contains("\"projectTitle\": \"Zorro Amigurumi\""))
        assertTrue(json.contains("\"name\": \"Cabeza\""))
        assertTrue(json.contains("\"name\": \"mat_Cabeza\""))
        assertTrue(json.contains("\"translation\": [12.5000, 0.0000, 0.0000]"))
    }
}
