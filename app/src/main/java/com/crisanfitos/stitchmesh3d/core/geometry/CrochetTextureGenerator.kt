package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Cuadrantes indexados del Stitch Texture Atlas 2x2 (SM-074).
 *
 * Cada cuadrante ocupa una región disjunta `[uMin..uMax] x [vMin..vMax]` del atlas:
 * - [SINGLE_CROCHET] (`pb` / `sc`): Top-Left `[0.0, 0.0]..[0.5, 0.5]` — Lazadas gemelas en "V" con corona superior.
 * - [INCREASE] (`aum` / `inc`): Top-Right `[0.5, 0.0]..[1.0, 0.5]` — Dos postes divergiendo en abanico ("W") desde una misma base.
 * - [DECREASE] (`dism` / `dec` / `invdec`): Bottom-Left `[0.0, 0.5]..[0.5, 1.0]` — Dos postes inferiores convergiendo en "A" hacia una sola corona.
 * - [TALL_POST_AND_CHAIN] (`pa` / `pma` / `cad` / `pe`): Bottom-Right `[0.5, 0.5]..[1.0, 1.0]` — Poste vertical largo con torsión helicoidal y lazada transversal.
 */
enum class StitchAtlasQuadrant(
    val uMin: Float,
    val vMin: Float,
    val uMax: Float,
    val vMax: Float
) {
    SINGLE_CROCHET(0.0f, 0.0f, 0.5f, 0.5f),
    INCREASE(0.5f, 0.0f, 1.0f, 0.5f),
    DECREASE(0.0f, 0.5f, 0.5f, 1.0f),
    TALL_POST_AND_CHAIN(0.5f, 0.5f, 1.0f, 1.0f);

    /**
     * Proyecta coordenadas UV locales normalizadas `[0.0..1.0]` dentro de este cuadrante,
     * aplicando un margen interior (`inset = 0.03f`) para evitar sangrado bilinear entre cuadrantes vecinos.
     */
    fun mapLocalUv(localU: Float, localV: Float, inset: Float = 0.03f): FloatArray {
        val clampedU = localU.coerceIn(0.0f, 1.0f)
        val clampedV = localV.coerceIn(0.0f, 1.0f)
        val uSpan = (uMax - uMin) - 2f * inset
        val vSpan = (vMax - vMin) - 2f * inset
        val u = (uMin + inset) + clampedU * uSpan
        val v = (vMin + inset) + clampedV * vSpan
        return floatArrayOf(u, v)
    }

    /**
     * Comprueba si un par de coordenadas UV `(u, v)` pertenece a este cuadrante.
     */
    fun containsUv(u: Float, v: Float, epsilon: Float = 1e-4f): Boolean {
        return u >= (uMin - epsilon) && u <= (uMax + epsilon) &&
            v >= (vMin - epsilon) && v <= (vMax + epsilon)
    }
}

/**
 * Generador procedural de texturas de relieve y Stitch Texture Atlas 2x2 para puntadas de crochet (SM-074).
 *
 * Sintetiza un atlas unificado en escala de grises modulable con oclusión ambiental y microfibras que contiene
 * 4 glifos morfológicos diferenciados (`pb`, `aum`, `dism`, `pa`/`cad`).
 *
 * Conforme a glTF 2.0 PBR §3.6.2.3:
 * `fragmentColor = baseColorFactor * COLOR_0 * texture(baseColorTexture)`
 * Al multiplicar el color del hilado (monocromático o Jacquard por vértice) por este atlas,
 * las hebras, nudos y torsiones se tiñen de manera realista conservando el relieve y la oclusión ambiental.
 */
object CrochetTextureGenerator {

    /**
     * Resolución por defecto del Stitch Texture Atlas (256x256 px, 128x128 px por cuadrante).
     */
    const val DEFAULT_ATLAS_SIZE = 256

    /**
     * Búfer estático precacheado del Stitch Texture Atlas 2x2 en formato PNG RGBA.
     */
    val defaultStitchAtlasPng: ByteArray by lazy {
        generateStitchAtlasPng(width = DEFAULT_ATLAS_SIZE, height = DEFAULT_ATLAS_SIZE)
    }

    /**
     * Alias de compatibilidad hacia el atlas unificado de puntos para `GlbMeshBuilder`.
     */
    val defaultStitchTexturePng: ByteArray
        get() = defaultStitchAtlasPng

    /**
     * Determina el cuadrante del atlas correspondiente a cualquier [StitchType] del AST,
     * desenrollando recursivamente modificadores topológicos (`ModifiedStitch` como BLO/FLO).
     */
    fun quadrantForStitch(stitchType: StitchType): StitchAtlasQuadrant {
        val base = unwrapBaseStitch(stitchType)
        return when (base) {
            is StitchType.Increase,
            is StitchType.HdcIncrease,
            is StitchType.DcIncrease,
            is StitchType.TripleIncrease -> StitchAtlasQuadrant.INCREASE

            is StitchType.Decrease,
            is StitchType.InvisibleDecrease,
            is StitchType.HdcDecrease,
            is StitchType.DcDecrease,
            is StitchType.TripleDecrease -> StitchAtlasQuadrant.DECREASE

            is StitchType.HalfDoubleCrochet,
            is StitchType.DoubleCrochet,
            is StitchType.TrebleCrochet,
            is StitchType.DoubleTrebleCrochet,
            is StitchType.FrontPostHalfDoubleCrochet,
            is StitchType.FrontPostDoubleCrochet,
            is StitchType.BackPostHalfDoubleCrochet,
            is StitchType.BackPostDoubleCrochet,
            is StitchType.Chain,
            is StitchType.SlipStitch,
            is StitchType.BobbleStitch,
            is StitchType.PopcornStitch,
            is StitchType.PuffStitch -> StitchAtlasQuadrant.TALL_POST_AND_CHAIN

            else -> StitchAtlasQuadrant.SINGLE_CROCHET
        }
    }

    /**
     * Calcula las coordenadas UV en el atlas para un [StitchType] y coordenadas locales `[0.0..1.0]`.
     */
    fun computeAtlasUv(stitchType: StitchType, localU: Float, localV: Float): FloatArray {
        return quadrantForStitch(stitchType).mapLocalUv(localU, localV)
    }

    private tailrec fun unwrapBaseStitch(stitchType: StitchType): StitchType {
        return if (stitchType is StitchType.ModifiedStitch) {
            unwrapBaseStitch(stitchType.baseStitch)
        } else {
            stitchType
        }
    }

    /**
     * Genera los bytes RGBA crudos (4 bytes por píxel) del Stitch Texture Atlas 2x2.
     */
    fun generateStitchAtlasRgba(
        width: Int = DEFAULT_ATLAS_SIZE,
        height: Int = DEFAULT_ATLAS_SIZE
    ): ByteArray {
        val rawPixels = ByteArray(width * height * 4)
        val halfW = (width / 2).coerceAtLeast(1)
        val halfH = (height / 2).coerceAtLeast(1)

        for (y in 0 until height) {
            val isBottom = y >= halfH
            val localY = (if (isBottom) y - halfH else y).toFloat() / halfH.toFloat()

            for (x in 0 until width) {
                val isRight = x >= halfW
                val localX = (if (isRight) x - halfW else x).toFloat() / halfW.toFloat()

                val quadrant = when {
                    !isRight && !isBottom -> StitchAtlasQuadrant.SINGLE_CROCHET
                    isRight && !isBottom -> StitchAtlasQuadrant.INCREASE
                    !isRight && isBottom -> StitchAtlasQuadrant.DECREASE
                    else -> StitchAtlasQuadrant.TALL_POST_AND_CHAIN
                }

                val intensity = evaluateGlyphIntensity(quadrant, localX, localY)
                val byteVal = (intensity * 255.0f).toInt().coerceIn(0, 255).toByte()
                val pixelOffset = (y * width + x) * 4
                rawPixels[pixelOffset] = byteVal     // R
                rawPixels[pixelOffset + 1] = byteVal // G
                rawPixels[pixelOffset + 2] = byteVal // B
                rawPixels[pixelOffset + 3] = 255.toByte() // A
            }
        }
        return rawPixels
    }

    /**
     * Genera el archivo binario PNG válido en memoria del Stitch Texture Atlas 2x2.
     */
    fun generateStitchAtlasPng(
        width: Int = DEFAULT_ATLAS_SIZE,
        height: Int = DEFAULT_ATLAS_SIZE
    ): ByteArray {
        val rgba = generateStitchAtlasRgba(width, height)
        return encodeRgbaToPng(width, height, rgba)
    }

    /**
     * Mantiene el método clásico de estampa individual o atlas para compatibilidad retroactiva.
     */
    fun generateCrochetStitchPng(width: Int = 128, height: Int = 128): ByteArray {
        return generateStitchAtlasPng(width = width, height = height)
    }

    /**
     * Evalúa la luminancia PBR en escala de grises `[0.0..1.0]` para una coordenada local `(nx, ny)`
     * dentro de un cuadrante específico del atlas, sintetizando oclusión ambiental del nudo,
     * perfil cilíndrico de las hebras y microtextura de torsión de lana.
     */
    internal fun evaluateGlyphIntensity(
        quadrant: StitchAtlasQuadrant,
        nx: Float,
        ny: Float
    ): Float {
        // 1. Oclusión ambiental base: junta suave en el perímetro de la celda (nunca negra gracias a aoMin = 0.45f)
        val borderX = min(nx, 1.0f - nx)
        val borderY = min(ny, 1.0f - ny)
        val seamDist = min(borderX, borderY)
        val aoBase = 0.45f + 0.30f * (seamDist / 0.14f).coerceIn(0.0f, 1.0f)

        val strandRadius: Float
        val minDistStrand: Float
        val twistPhase: Float

        when (quadrant) {
            StitchAtlasQuadrant.SINGLE_CROCHET -> {
                // Glifo 1 (pb): Lazadas gemelas en "V" simétrica + corona superior horizontal
                strandRadius = 0.090f
                val dLeft = distToSegment(nx, ny, 0.22f, 0.22f, 0.47f, 0.84f)
                val dRight = distToSegment(nx, ny, 0.78f, 0.22f, 0.53f, 0.84f)
                val dCrown = distToSegment(nx, ny, 0.18f, 0.16f, 0.82f, 0.16f)
                val dBaseKnot = distToSegment(nx, ny, 0.44f, 0.84f, 0.56f, 0.84f)
                minDistStrand = minOf(dLeft, dRight, dCrown, dBaseKnot)
                twistPhase = sin((nx * 32.0f + ny * 22.0f).toDouble()).toFloat()
            }

            StitchAtlasQuadrant.INCREASE -> {
                // Glifo 2 (aum): Abanico doble en "W" (dos "V" completas divergiendo desde el mismo bucle inferior)
                strandRadius = 0.072f
                // Primera "V" izquierda saliendo de la base común (0.50, 0.88)
                val dLeftOuter = distToSegment(nx, ny, 0.12f, 0.22f, 0.46f, 0.86f)
                val dLeftInner = distToSegment(nx, ny, 0.46f, 0.24f, 0.49f, 0.86f)
                // Segunda "V" derecha saliendo de la base común (0.50, 0.88)
                val dRightInner = distToSegment(nx, ny, 0.54f, 0.24f, 0.51f, 0.86f)
                val dRightOuter = distToSegment(nx, ny, 0.88f, 0.22f, 0.54f, 0.86f)
                // Doble corona superior y anillo de inserción compartido en la base
                val dCrownLeft = distToSegment(nx, ny, 0.12f, 0.15f, 0.48f, 0.15f)
                val dCrownRight = distToSegment(nx, ny, 0.52f, 0.15f, 0.88f, 0.15f)
                val dSharedRing = distToSegment(nx, ny, 0.38f, 0.88f, 0.62f, 0.88f)
                minDistStrand = minOf(
                    dLeftOuter, dLeftInner, dRightInner, dRightOuter,
                    dCrownLeft, dCrownRight, dSharedRing
                )
                twistPhase = sin(((nx - 0.5f) * 40.0f + ny * 26.0f).toDouble()).toFloat()
            }

            StitchAtlasQuadrant.DECREASE -> {
                // Glifo 3 (dism / invdec): Dos postes inferiores separados convergiendo en una sola corona superior ("A" / chevron convergente)
                strandRadius = 0.082f
                val dBaseLeftLeg = distToSegment(nx, ny, 0.16f, 0.86f, 0.45f, 0.24f)
                val dBaseRightLeg = distToSegment(nx, ny, 0.84f, 0.86f, 0.55f, 0.24f)
                val dInnerMergeLeft = distToSegment(nx, ny, 0.34f, 0.86f, 0.49f, 0.26f)
                val dInnerMergeRight = distToSegment(nx, ny, 0.66f, 0.86f, 0.51f, 0.26f)
                val dSingleCrown = distToSegment(nx, ny, 0.30f, 0.15f, 0.70f, 0.15f)
                val dBridge = distToSegment(nx, ny, 0.22f, 0.58f, 0.78f, 0.58f)
                minDistStrand = minOf(
                    dBaseLeftLeg, dBaseRightLeg, dInnerMergeLeft,
                    dInnerMergeRight, dSingleCrown, dBridge
                )
                twistPhase = sin((abs(nx - 0.5f) * 38.0f - ny * 28.0f).toDouble()).toFloat()
            }

            StitchAtlasQuadrant.TALL_POST_AND_CHAIN -> {
                // Glifo 4 (pa / pma / cad): Poste vertical esbelto con lazadas helicoidales transversales
                strandRadius = 0.078f
                val dPostLeft = distToSegment(nx, ny, 0.41f, 0.16f, 0.43f, 0.88f)
                val dPostRight = distToSegment(nx, ny, 0.59f, 0.16f, 0.57f, 0.88f)
                // Lazadas diagonales de entorchado del punto alto (yarn-over wraps)
                val dWrap1 = distToSegment(nx, ny, 0.32f, 0.36f, 0.68f, 0.44f)
                val dWrap2 = distToSegment(nx, ny, 0.32f, 0.58f, 0.68f, 0.66f)
                // Eslabón de corona / cadeneta superior
                val dChainTop = distToSegment(nx, ny, 0.24f, 0.14f, 0.76f, 0.14f)
                minDistStrand = minOf(dPostLeft, dPostRight, dWrap1, dWrap2, dChainTop)
                twistPhase = sin((nx * 24.0f + ny * 48.0f).toDouble()).toFloat()
            }
        }

        var intensity = aoBase
        if (minDistStrand < strandRadius) {
            val normalizedDist = minDistStrand / strandRadius
            val cylindricalProfile = sqrt((1.0f - normalizedDist * normalizedDist).coerceAtLeast(0.0f))
            val microFuzz = 0.05f * twistPhase
            val strandLuma = 0.68f + 0.30f * cylindricalProfile + microFuzz
            intensity = max(intensity, strandLuma.coerceIn(0.0f, 1.0f))
        }

        // Sombra suave bajo la corona superior para dar volumen 3D al nudo
        if (ny in 0.19f..0.27f && nx in 0.22f..0.78f) {
            intensity *= 0.88f
        }

        return intensity.coerceIn(0.35f, 1.0f)
    }

    /**
     * Modula un color de hilado RGB `[0.0..1.0]` con la luminancia del glifo del punto en `(localU, localV)`
     * conforme a la ecuación PBR de glTF 2.0: `fragColor = baseColor * glyphIntensity`.
     */
    fun modulateYarnAlbedo(
        stitchType: StitchType,
        yarnColorRgb: FloatArray,
        localU: Float = 0.5f,
        localV: Float = 0.5f
    ): FloatArray {
        val quadrant = quadrantForStitch(stitchType)
        val luma = evaluateGlyphIntensity(
            quadrant,
            localU.coerceIn(0f, 1f),
            localV.coerceIn(0f, 1f)
        )
        val r = (yarnColorRgb.getOrElse(0) { 1f } * luma).coerceIn(0f, 1f)
        val g = (yarnColorRgb.getOrElse(1) { 1f } * luma).coerceIn(0f, 1f)
        val b = (yarnColorRgb.getOrElse(2) { 1f } * luma).coerceIn(0f, 1f)
        return floatArrayOf(r, g, b)
    }

    private fun distToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
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
