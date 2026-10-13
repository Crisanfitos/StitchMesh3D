package com.crisanfitos.stitchmesh3d.core.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.DataInputStream

/**
 * Pruebas unitarias para [CrochetTextureGenerator], validando la conformidad binaria con el estándar PNG
 * (ISO/IEC 15948:2004) y la correcta síntesis de la textura de relieve de puntadas de crochet.
 */
class CrochetTextureGeneratorTest {

    @Test
    fun `generateCrochetStitchPng produces valid PNG magic header and chunk structure`() {
        val pngBytes = CrochetTextureGenerator.generateCrochetStitchPng(width = 64, height = 64)
        assertNotNull(pngBytes)
        assertTrue("El archivo PNG debe tener más de 100 bytes", pngBytes.size > 100)

        // 1. Validar firma PNG (8 bytes: 89 50 4E 47 0D 0A 1A 0A)
        val expectedMagic = byteArrayOf(
            0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
            0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte()
        )
        for (i in 0 until 8) {
            assertEquals("Byte $i del encabezado PNG incorrecto", expectedMagic[i], pngBytes[i])
        }

        // 2. Analizar chunks con DataInputStream
        val dis = DataInputStream(ByteArrayInputStream(pngBytes, 8, pngBytes.size - 8))

        // Primer chunk: IHDR (13 bytes)
        val ihdrLen = dis.readInt()
        assertEquals("IHDR payload length debe ser 13", 13, ihdrLen)
        val ihdrType = String(dis.readNBytes(4), Charsets.US_ASCII)
        assertEquals("Primer chunk debe ser IHDR", "IHDR", ihdrType)

        val width = dis.readInt()
        val height = dis.readInt()
        val bitDepth = dis.readByte().toInt()
        val colorType = dis.readByte().toInt()
        val compression = dis.readByte().toInt()
        val filter = dis.readByte().toInt()
        val interlace = dis.readByte().toInt()
        val ihdrCrc = dis.readInt()

        assertEquals("Ancho debe ser 64", 64, width)
        assertEquals("Alto debe ser 64", 64, height)
        assertEquals("Bit depth debe ser 8 bits", 8, bitDepth)
        assertEquals("Color type debe ser 6 (RGBA)", 6, colorType)
        assertEquals("Compresión debe ser 0", 0, compression)
        assertEquals("Filtro debe ser 0", 0, filter)
        assertEquals("Entrelazado debe ser 0", 0, interlace)

        // Segundo chunk: IDAT
        val idatLen = dis.readInt()
        assertTrue("IDAT payload debe ser > 0", idatLen > 0)
        val idatType = String(dis.readNBytes(4), Charsets.US_ASCII)
        assertEquals("Segundo chunk debe ser IDAT", "IDAT", idatType)
        dis.skipBytes(idatLen) // saltar datos comprimidos
        val idatCrc = dis.readInt()

        // Tercer chunk: IEND (0 bytes)
        val iendLen = dis.readInt()
        assertEquals("IEND payload length debe ser 0", 0, iendLen)
        val iendType = String(dis.readNBytes(4), Charsets.US_ASCII)
        assertEquals("Tercer chunk debe ser IEND", "IEND", iendType)
        val iendCrc = dis.readInt()

        assertEquals("No debe haber bytes remanentes después de IEND", 0, dis.available())
    }

    @Test
    fun `defaultStitchTexturePng lazy singleton produces 128x128 valid texture within budget`() {
        val png = CrochetTextureGenerator.defaultStitchTexturePng
        assertNotNull(png)
        assertTrue("El PNG debe pesar menos de 20 KB para optimización de GPU", png.size < 20_000)
        assertTrue("El PNG debe ser mayor a 500 bytes", png.size > 500)

        // Verificar dimensiones en encabezado IHDR
        val dis = DataInputStream(ByteArrayInputStream(png, 8, 24))
        val ihdrLen = dis.readInt()
        val ihdrType = String(dis.readNBytes(4), Charsets.US_ASCII)
        val width = dis.readInt()
        val height = dis.readInt()

        assertEquals("IHDR", ihdrType)
        assertEquals(128, width)
        assertEquals(128, height)
    }

    @Test
    fun `AdaptiveTessellator and PlanarMeshGenerator produce vertexUvs matching vertexCount`() {
        // 1. Tubular mesh
        val ring = RingProfileGenerator.generateRing(
            roundIndex = 1,
            stitches = List(6) { com.crisanfitos.stitchmesh3d.core.engine.model.StitchType.SingleCrochet },
            gaugeStandard = com.crisanfitos.stitchmesh3d.core.gauge.YarnGaugeRegistry.DEFAULT_STANDARD
        )
        val amigurumiMesh = AdaptiveTessellator.tessellate(listOf(ring), includePolarCap = true)

        assertEquals("vertexUvs size debe ser exactamente vertexCount * 2",
            amigurumiMesh.vertexCount * 2, amigurumiMesh.vertexUvs.size)

        // Polar vertex UV
        val polarUv = amigurumiMesh.getVertexUv(0)
        assertEquals(0.5f, polarUv[0], 0.001f)
        assertEquals(0.0f, polarUv[1], 0.001f)

        // Ring vertex UVs
        for (i in 1 until amigurumiMesh.vertexCount) {
            val uv = amigurumiMesh.getVertexUv(i)
            assertTrue("U debe ser >= 0", uv[0] >= 0f)
            assertTrue("V debe ser >= 1", uv[1] >= 1f)
        }

        // Direct UV buffer
        val uvBuffer = amigurumiMesh.toDirectUvBuffer()
        assertNotNull(uvBuffer)
        assertEquals(amigurumiMesh.vertexCount * 2, uvBuffer.capacity())

        // 2. Planar row-by-row mesh
        val planarRowMesh = PlanarMeshGenerator.generateRowByRowMesh(
            rows = listOf(List(4) {
                com.crisanfitos.stitchmesh3d.core.engine.parser.StitchInstance(
                    stitchType = com.crisanfitos.stitchmesh3d.core.engine.model.StitchType.SingleCrochet,
                    indexInRound = it
                )
            }),
            gauge = com.crisanfitos.stitchmesh3d.core.gauge.YarnGaugeRegistry.DEFAULT_STANDARD
        )
        assertEquals(planarRowMesh.vertexCount * 2, planarRowMesh.vertexUvs.size)

        // 3. Planar concentric granny square
        val planarConcentricMesh = PlanarMeshGenerator.generateConcentricMesh(
            rounds = listOf(List(6) {
                com.crisanfitos.stitchmesh3d.core.engine.parser.StitchInstance(
                    stitchType = com.crisanfitos.stitchmesh3d.core.engine.model.StitchType.SingleCrochet,
                    indexInRound = it
                )
            }),
            gauge = com.crisanfitos.stitchmesh3d.core.gauge.YarnGaugeRegistry.DEFAULT_STANDARD
        )
        assertEquals(planarConcentricMesh.vertexCount * 2, planarConcentricMesh.vertexUvs.size)
    }
}
