package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.color.CrochetColorHelper
import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.engine.parser.StitchInstance
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Orquestador procedural de instanciación de bucles volumétricos tubulares de hebra 3D (SM-086).
 *
 * Implementa la Fase 2 de realismo textil:
 * - Extrae el marco de referencia ortonormal TBN de cada punto de la malla.
 * - Conmuta dinámicamente el nivel de detalle (LOD0 / LOD1) según el presupuesto de polígonos (RND-1: 50.000 tris).
 * - Aplica filtrado reactivo de vueltas para el PeelSliderBar (RF-3.4).
 * - Realiza la transformación y ensamblado por lotes en hilo secundario en < 50 ms para 2.000 puntos (RND-2).
 */
object ProceduralYarnLoopInstancer {

    /** Presupuesto poligonal máximo para renderizado en tiempo real a 60 FPS (RND-1). */
    const val MAX_POLYGON_BUDGET = 50_000

    /**
     * Extrae la lista de marcos ortonormales [StitchTbnFrame] a partir de una secuencia de [RingProfile].
     *
     * @param rings Perfiles de anillos axiales de la pieza.
     * @param gauge Parámetros calibrados de la muestra de tensión.
     * @param peelLimit Límite superior de vuelta visible (para PeelSliderBar).
     * @param defaultColorHex Color por defecto de la pieza.
     */
    fun extractFramesFromRings(
        rings: List<RingProfile>,
        gauge: YarnGaugeStandard,
        peelLimit: Int? = null,
        defaultColorHex: String = "#E06D53"
    ): List<StitchTbnFrame> {
        val validRings = rings.filter { it.vertexCount > 0 }
        if (validRings.isEmpty()) return emptyList()

        val maxRound = peelLimit ?: validRings.size
        val effectiveRings = validRings.filter { it.roundIndex <= maxRound }
        if (effectiveRings.size < 2) return emptyList()

        val frames = ArrayList<StitchTbnFrame>()
        val defaultWidth = gauge.stitchWidthMm.toFloat().coerceAtLeast(0.5f)
        val defaultHeight = gauge.stitchHeightMm.toFloat().coerceAtLeast(0.5f)
        val defaultThickness = gauge.stitchThicknessMm.toFloat().coerceAtLeast(0.5f)

        for (r in 0 until effectiveRings.size - 1) {
            val ring0 = effectiveRings[r]
            val ring1 = effectiveRings[r + 1]
            val n0 = ring0.vertexCount
            val n1 = ring1.vertexCount
            if (n0 == 0 || n1 == 0) continue

            for (j in 0 until n0) {
                val nextJ = (j + 1) % n0
                val v0 = ring0.vertices[j]
                val v1 = ring0.vertices[nextJ]

                val c0Idx = (j * n1 / n0).coerceIn(0, n1 - 1)
                val c1Idx = ((j + 1) * n1 / n0).coerceIn(0, n1 - 1)
                val c0 = ring1.vertices[c0Idx]
                val c1 = ring1.vertices[c1Idx]

                // 1. Centroide del punto en coordenadas cartesianas mm
                val ox = (v0.x + v1.x + c0.x + c1.x).toFloat() * 0.25f
                val oy = (v0.y + v1.y + c0.y + c1.y).toFloat() * 0.25f
                val oz = (v0.z + v1.z + c0.z + c1.z).toFloat() * 0.25f
                val origin = floatArrayOf(ox, oy, oz)

                // 2. Vector Tangente T a lo largo de la vuelta
                var tx = (v1.x - v0.x).toFloat()
                var ty = (v1.y - v0.y).toFloat()
                var tz = (v1.z - v0.z).toFloat()
                var lenT = sqrt(tx * tx + ty * ty + tz * tz)
                if (lenT < 1e-6f) {
                    tx = -sin(v0.theta).toFloat(); ty = 0f; tz = -cos(v0.theta).toFloat()
                    lenT = 1f
                }
                val tangent = floatArrayOf(tx / lenT, ty / lenT, tz / lenT)

                // 3. Vector Bitangente B vertical (hacia la vuelta siguiente)
                val baseMidX = (v0.x + v1.x) * 0.5
                val baseMidY = (v0.y + v1.y) * 0.5
                val baseMidZ = (v0.z + v1.z) * 0.5
                val topMidX = (c0.x + c1.x) * 0.5
                val topMidY = (c0.y + c1.y) * 0.5
                val topMidZ = (c0.z + c1.z) * 0.5

                var bx = (topMidX - baseMidX).toFloat()
                var by = (topMidY - baseMidY).toFloat()
                var bz = (topMidZ - baseMidZ).toFloat()
                var lenB = sqrt(bx * bx + by * by + bz * bz)
                if (lenB < 1e-6f) {
                    bx = 0f; by = 1f; bz = 0f; lenB = 1f
                }
                val rawBitangent = floatArrayOf(bx / lenB, by / lenB, bz / lenB)

                // 4. Vector Normal N ortogonal hacia afuera = Tangente x Bitangente
                var nx = tangent[1] * rawBitangent[2] - tangent[2] * rawBitangent[1]
                var ny = tangent[2] * rawBitangent[0] - tangent[0] * rawBitangent[2]
                var nz = tangent[0] * rawBitangent[1] - tangent[1] * rawBitangent[0]
                var lenN = sqrt(nx * nx + ny * ny + nz * nz)
                if (lenN < 1e-6f) {
                    nx = cos(v0.theta).toFloat(); ny = 0f; nz = -sin(v0.theta).toFloat()
                    lenN = 1f
                }
                val normal = floatArrayOf(nx / lenN, ny / lenN, nz / lenN)

                // Re-ortogonalizar Bitangente = Normal x Tangente para garantizar base diestra exacta
                val orthoBx = normal[1] * tangent[2] - normal[2] * tangent[1]
                val orthoBy = normal[2] * tangent[0] - normal[0] * tangent[2]
                val orthoBz = normal[0] * tangent[1] - normal[1] * tangent[0]
                val bitangent = floatArrayOf(orthoBx, orthoBy, orthoBz)

                val stitchWidth = (lenT).coerceIn(defaultWidth * 0.5f, defaultWidth * 2.5f)
                val stitchHeight = (lenB).coerceIn(defaultHeight * 0.5f, defaultHeight * 2.5f)
                val stitchThickness = defaultThickness

                val color = v0.colorHex ?: ring0.vertices.firstOrNull()?.colorHex ?: defaultColorHex

                frames.add(
                    StitchTbnFrame(
                        origin = origin,
                        tangent = tangent,
                        bitangent = bitangent,
                        normal = normal,
                        stitchType = v0.stitchType,
                        colorHex = color,
                        roundIndex = ring0.roundIndex,
                        stitchIndex = j,
                        widthMm = stitchWidth,
                        heightMm = stitchHeight,
                        thicknessMm = stitchThickness
                    )
                )
            }
        }

        return frames
    }

    /**
     * Extrae la lista de marcos ortonormales [StitchTbnFrame] a partir de hileras planas ([StitchInstance]).
     */
    fun extractFramesFromPlanarRows(
        rows: List<List<StitchInstance>>,
        gauge: YarnGaugeStandard,
        peelLimit: Int? = null,
        defaultColorHex: String = "#E06D53"
    ): List<StitchTbnFrame> {
        val validRows = rows.filter { it.isNotEmpty() }
        if (validRows.isEmpty()) return emptyList()

        val maxRow = peelLimit ?: validRows.size
        val effectiveRows = validRows.take(maxRow)
        val frames = ArrayList<StitchTbnFrame>()

        val wStitch = gauge.stitchWidthMm.toFloat().coerceAtLeast(0.5f)
        val hStitch = gauge.stitchHeightMm.toFloat().coerceAtLeast(0.5f)
        val tStitch = gauge.stitchThicknessMm.toFloat().coerceAtLeast(0.5f)

        for ((rIdx, row) in effectiveRows.withIndex()) {
            val yPos = (rIdx + 0.5f) * hStitch
            val rowWidth = row.size * wStitch
            val startX = -rowWidth / 2.0f

            for ((sIdx, stitch) in row.withIndex()) {
                val xPos = startX + (sIdx + 0.5f) * wStitch
                val origin = floatArrayOf(xPos, yPos, 0f)

                // Marco estándar para panel plano: T = +X, B = +Y, N = +Z
                val tangent = floatArrayOf(1f, 0f, 0f)
                val bitangent = floatArrayOf(0f, 1f, 0f)
                val normal = floatArrayOf(0f, 0f, 1f)

                frames.add(
                    StitchTbnFrame(
                        origin = origin,
                        tangent = tangent,
                        bitangent = bitangent,
                        normal = normal,
                        stitchType = stitch.stitchType,
                        colorHex = stitch.colorHex ?: defaultColorHex,
                        roundIndex = rIdx + 1,
                        stitchIndex = sIdx,
                        widthMm = wStitch,
                        heightMm = hStitch,
                        thicknessMm = tStitch
                    )
                )
            }
        }

        return frames
    }

    /**
     * Genera la geometría 3D completa de hebras volumétricas instanciadas a partir de los marcos TBN.
     *
     * @param frames Lista de marcos ortonormales de los puntos a instanciar.
     * @param polygonBudget Presupuesto máximo de triángulos antes de forzar LOD1 o fallback.
     * @return [MeshGeometry] combinada lista para renderizado PBR en Filament, o null si excede el presupuesto total.
     */
    fun buildVolumetricMesh(
        frames: List<StitchTbnFrame>,
        polygonBudget: Int = MAX_POLYGON_BUDGET
    ): MeshGeometry? {
        if (frames.isEmpty()) return null

        val stitchCount = frames.size

        // 1. Selección automática de LOD según el presupuesto
        // LOD0 genera ~60 triángulos/punto, LOD1 genera ~24 triángulos/punto
        val lod0Est = stitchCount * 60
        val lod1Est = stitchCount * 24

        val selectedLod = when {
            lod0Est <= polygonBudget -> 0
            lod1Est <= polygonBudget -> 1
            else -> return null // Fallback a la malla PBR de Fase 1
        }

        // 2. Prealocación de prototipos maestros por categoría
        val categories = YarnLoopCategory.entries
        val prototypes = HashMap<YarnLoopCategory, YarnLoopPrototype>(categories.size)
        for (cat in categories) {
            prototypes[cat] = YarnLoopPrototypeBuilder.getPrototype(cat, lodLevel = selectedLod)
        }

        // 3. Cálculo de dimensiones de búfer pre-alocadas
        var totalVertices = 0
        var totalIndices = 0
        for (frame in frames) {
            val cat = YarnLoopCategory.fromStitchType(frame.stitchType)
            val proto = prototypes[cat] ?: continue
            totalVertices += proto.vertexCount
            totalIndices += proto.mesh.indices.size
        }

        if (totalVertices == 0 || totalIndices == 0) return null

        val positions = FloatArray(totalVertices * 3)
        val normals = FloatArray(totalVertices * 3)
        val colors = FloatArray(totalVertices * 4)
        val uvs = FloatArray(totalVertices * 2)
        val indices = IntArray(totalIndices)

        var vertexOffset = 0
        var indexOffset = 0

        // 4. Ensamblado directo por lotes de las instancias
        for (frame in frames) {
            val cat = YarnLoopCategory.fromStitchType(frame.stitchType)
            val proto = prototypes[cat] ?: continue
            val protoMesh = proto.mesh
            val protoVertCount = protoMesh.vertexCount
            val protoIndices = protoMesh.indices

            val matrix = frame.toTransformMatrix()
            val rgba = CrochetColorHelper.toRgbaFloats(frame.colorHex)

            val baseVertexIndex = vertexOffset

            // Transformar vértices del prototipo
            for (v in 0 until protoVertCount) {
                val px = protoMesh.vertexPositions[v * 3]
                val py = protoMesh.vertexPositions[v * 3 + 1]
                val pz = protoMesh.vertexPositions[v * 3 + 2]

                val nx = protoMesh.vertexNormals[v * 3]
                val ny = protoMesh.vertexNormals[v * 3 + 1]
                val nz = protoMesh.vertexNormals[v * 3 + 2]

                val tp = matrix.transformPoint(px, py, pz)
                val tn = matrix.transformNormal(nx, ny, nz)

                val vIdx = (vertexOffset + v)
                positions[vIdx * 3] = tp[0]
                positions[vIdx * 3 + 1] = tp[1]
                positions[vIdx * 3 + 2] = tp[2]

                normals[vIdx * 3] = tn[0]
                normals[vIdx * 3 + 1] = tn[1]
                normals[vIdx * 3 + 2] = tn[2]

                colors[vIdx * 4] = rgba[0]
                colors[vIdx * 4 + 1] = rgba[1]
                colors[vIdx * 4 + 2] = rgba[2]
                colors[vIdx * 4 + 3] = rgba[3]

                if (protoMesh.vertexUvs.isNotEmpty() && (v * 2 + 1) < protoMesh.vertexUvs.size) {
                    uvs[vIdx * 2] = protoMesh.vertexUvs[v * 2]
                    uvs[vIdx * 2 + 1] = protoMesh.vertexUvs[v * 2 + 1]
                }
            }

            // Copiar índices con desplazamiento base
            for (idx in protoIndices) {
                indices[indexOffset++] = baseVertexIndex + idx
            }

            vertexOffset += protoVertCount
        }

        return MeshGeometry(
            vertexPositions = positions,
            indices = indices,
            vertexCount = totalVertices,
            triangleCount = totalIndices / 3,
            vertexNormals = normals,
            vertexColors = colors,
            vertexUvs = uvs
        )
    }
}
