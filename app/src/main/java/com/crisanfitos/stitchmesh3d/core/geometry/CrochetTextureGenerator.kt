package com.crisanfitos.stitchmesh3d.core.geometry

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Generador procedural de texturas de relieve para puntadas de crochet (PBR Stitch Relief).
 *
 * Sintetiza un mapa de relieve y oclusión ambiental en escala de grises que representa la silueta
 * icónica en "V" del punto bajo (single crochet / pb), la lazada horizontal de la corona superior
 * y las grietas de oclusión inter-puntada.
 *
 * Conforme a glTF 2.0 PBR §3.6.2.3:
 * fragmentColor = baseColorFactor * COLOR_0 * texture(baseColorTexture)
 * Al multiplicar el color de la lana (o colores Jacquard por vértice) por este mapa,
 * los relieves y sombras se tiñen de manera fotorrealista sin coste extra en la GPU.
 *
 * Incluye un codificador PNG 100% nativo compatible con la JVM y Android (sin dependencias externas).
 */
object CrochetTextureGenerator {

    /**
     * Búfer estático precacheado de la textura de relieve en resolución estándar de 128x128 px (~3 KB).
     */
    val defaultStitchTexturePng: ByteArray by lazy {
        generateCrochetStitchPng(width = 128, height = 128)
    }

    /**
     * Genera un archivo binario PNG válido en memoria con la estampa del punto de crochet.
     */
    fun generateCrochetStitchPng(width: Int = 128, height: Int = 128): ByteArray {
        val rawPixels = ByteArray(width * height * 4)

        // Helper para distancia euclidiana de un punto a un segmento rectilíneo o curvo
        fun distToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
            val dx = x2 - x1
            val dy = y2 - y1
            val lenSq = dx * dx + dy * dy
            if (lenSq < 1e-6f) {
                val dpx = px - x1
                val dpy = py - y1
                return sqrt(dpx * dpx + dpy * dpy)
            }
            val t = ((px - x1) * dx + (py - y1) * dy) / lenSq
            val clampedT = t.coerceIn(0.0f, 1.0f)
            val projX = x1 + clampedT * dx
            val projY = y1 + clampedT * dy
            val dpx = px - projX
            val dpy = py - projY
            return sqrt(dpx * dpx + dpy * dpy)
        }

        // Parámetros de la morfología del punto bajo (en coordenadas normalizadas [0.0, 1.0])
        val strandRadius = 0.085f

        for (y in 0 until height) {
            val ny = y.toFloat() / height.toFloat() // 0.0 (arriba) a 1.0 (abajo)
            for (x in 0 until width) {
                val nx = x.toFloat() / width.toFloat() // 0.0 (izq) a 1.0 (der)

                // 1. Oclusión ambiental base: grietas profundas en las juntas entre puntadas
                val borderX = min(nx, 1.0f - nx)
                val borderY = min(ny, 1.0f - ny)
                val seamDist = min(borderX, borderY)
                val aoBase = (0.35f + 0.40f * (seamDist / 0.15f).coerceIn(0.0f, 1.0f))

                // 2. Lazada izquierda en "V": desde corona (0.22, 0.20) hasta nudo inferior (0.48, 0.85)
                val dLeft = distToSegment(nx, ny, 0.22f, 0.20f, 0.48f, 0.85f)

                // 3. Lazada derecha en "V": desde corona (0.78, 0.20) hasta nudo inferior (0.52, 0.85)
                val dRight = distToSegment(nx, ny, 0.78f, 0.20f, 0.52f, 0.85f)

                // 4. Corona superior horizontal: lazada entrelazada superior de (0.16, 0.18) a (0.84, 0.18)
                val dCrown = distToSegment(nx, ny, 0.16f, 0.18f, 0.84f, 0.18f)

                // 5. Nudo central inferior
                val dKnot = distToSegment(nx, ny, 0.45f, 0.82f, 0.55f, 0.82f)

                val minDistStrand = minOf(dLeft, dRight, dCrown, dKnot)

                // Relieve cilíndrico del cordón de lana
                var intensity = aoBase
                if (minDistStrand < strandRadius) {
                    val normalizedDist = minDistStrand / strandRadius
                    // Perfil semiesférico / toroidal del hilo
                    val cylindricalProfile = sqrt((1.0f - normalizedDist * normalizedDist).coerceAtLeast(0.0f))
                    // Microtrama de torsión de fibras (sinusoidal sutil)
                    val twist = 0.06f * sin((nx * 30.0f + ny * 20.0f).toDouble()).toFloat()
                    val strandLuma = 0.65f + 0.32f * cylindricalProfile + twist
                    intensity = max(intensity, strandLuma.coerceIn(0.0f, 1.0f))
                }

                // Sombra proyectada detrás de la corona superior sobre las patas
                if (ny in 0.20f..0.30f && (nx in 0.25f..0.75f)) {
                    intensity *= 0.82f
                }

                val byteVal = (intensity * 255.0f).toInt().coerceIn(0, 255).toByte()
                val pixelOffset = (y * width + x) * 4
                rawPixels[pixelOffset] = byteVal     // R
                rawPixels[pixelOffset + 1] = byteVal // G
                rawPixels[pixelOffset + 2] = byteVal // B
                rawPixels[pixelOffset + 3] = 255.toByte() // A (Opaco)
            }
        }

        return encodeRgbaToPng(width, height, rawPixels)
    }

    /**
     * Codificador PNG nativo estándar de RGBA de 8 bits sin dependencias de Android Bitmap.
     * Garantiza compatibilidad universal JVM en pruebas unitarias y en tiempo de ejecución.
     */
    private fun encodeRgbaToPng(width: Int, height: Int, rgbaBytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()

        // 1. PNG Signature (8 bytes)
        out.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))

        // 2. IHDR Chunk (13 bytes payload)
        val ihdrPayload = ByteArrayOutputStream(13)
        val ihdrData = DataOutputStream(ihdrPayload)
        ihdrData.writeInt(width)
        ihdrData.writeInt(height)
        ihdrData.writeByte(8) // Bit depth: 8 bits por canal
        ihdrData.writeByte(6) // Color type: 6 (RGBA)
        ihdrData.writeByte(0) // Compression: Deflate
        ihdrData.writeByte(0) // Filter: None
        ihdrData.writeByte(0) // Interlace: None
        writeChunk(out, "IHDR", ihdrPayload.toByteArray())

        // 3. IDAT Chunk: Datos de imagen con filtro None (0x00) al inicio de cada scanline
        val scanlineLength = width * 4
        val rawScanlines = ByteArray(height * (1 + scanlineLength))
        var rawIdx = 0
        var rgbaIdx = 0
        for (y in 0 until height) {
            rawScanlines[rawIdx++] = 0.toByte() // Filter 0 (None)
            System.arraycopy(rgbaBytes, rgbaIdx, rawScanlines, rawIdx, scanlineLength)
            rawIdx += scanlineLength
            rgbaIdx += scanlineLength
        }

        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION)
        deflater.setInput(rawScanlines)
        deflater.finish()
        val compressedBuffer = ByteArray(rawScanlines.size + 1024)
        val compressedLength = deflater.deflate(compressedBuffer)
        deflater.end()

        val idatBytes = compressedBuffer.copyOf(compressedLength)
        writeChunk(out, "IDAT", idatBytes)

        // 4. IEND Chunk (0 bytes payload)
        writeChunk(out, "IEND", ByteArray(0))

        return out.toByteArray()
    }

    private fun writeChunk(out: ByteArrayOutputStream, type: String, payload: ByteArray) {
        val dataOut = DataOutputStream(out)
        dataOut.writeInt(payload.size)

        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        dataOut.write(typeBytes)
        dataOut.write(payload)

        val crc = CRC32()
        crc.update(typeBytes)
        crc.update(payload)
        dataOut.writeInt(crc.value.toInt())
    }
}
