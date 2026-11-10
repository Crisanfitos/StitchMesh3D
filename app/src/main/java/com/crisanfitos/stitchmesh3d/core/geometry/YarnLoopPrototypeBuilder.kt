package com.crisanfitos.stitchmesh3d.core.geometry

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Generador procedural determinista de primitivas tubulares maestras de hebra de hilo 3D (SM-086).
 *
 * Construye mallas 3D volumétricas cerradas para cada categoría de punto de crochet:
 * - **LOD0 (Macro / Primer Plano):** ~48–72 triángulos con sección circular de 5 o 6 lados.
 * - **LOD1 (Distancia Media):** ~24 triángulos con sección circular de 3 o 4 lados.
 *
 * Características geométricas estrictas:
 * - Normales unitarias exactas (||n|| = 1.0 ± 0.001) para sombreado PBR continuo en Filament.
 * - Sin vértices ni normales degeneradas (NaN / Inf).
 * - Mallas cerradas con tapas en los extremos (2-manifóldicas) con orden de bobinado CCW.
 */
object YarnLoopPrototypeBuilder {

    private const val TWO_PI = (2.0 * PI).toFloat()
    private const val DEFAULT_TUBE_RADIUS = 0.12f

    // Caché en memoria para evitar reconstruir prototipos normalizados en cada cuadro
    private val prototypeCache = HashMap<Pair<YarnLoopCategory, Int>, YarnLoopPrototype>()

    /**
     * Obtiene o construye la primitiva de bucle de hilo volumétrico para la categoría y LOD especificados.
     *
     * @param category Categoría geométrica de la puntada ([YarnLoopCategory]).
     * @param lodLevel Nivel de detalle (0 para LOD0 ~48-72 tris, 1 para LOD1 ~24 tris).
     * @param tubeRadius Radio nominal de la hebra en el espacio normalizado (por defecto 0.12).
     */
    fun getPrototype(
        category: YarnLoopCategory,
        lodLevel: Int = 0,
        tubeRadius: Float = DEFAULT_TUBE_RADIUS
    ): YarnLoopPrototype {
        val clampedLod = lodLevel.coerceIn(0, 1)
        val key = Pair(category, clampedLod)
        return prototypeCache.getOrPut(key) {
            buildPrototype(category, clampedLod, tubeRadius)
        }
    }

    /**
     * Construye proceduralmente un nuevo prototipo de bucle para una categoría dada.
     */
    fun buildPrototype(
        category: YarnLoopCategory,
        lodLevel: Int,
        tubeRadius: Float = DEFAULT_TUBE_RADIUS
    ): YarnLoopPrototype {
        val isLod0 = (lodLevel == 0)
        val sides = if (isLod0) 5 else 3

        val (centerline, isClosed) = when (category) {
            YarnLoopCategory.SINGLE_CROCHET -> {
                val pts = if (isLod0) {
                    listOf(
                        floatArrayOf(-0.25f, -0.5f, -0.1f),
                        floatArrayOf(-0.15f, -0.15f, 0.15f),
                        floatArrayOf(-0.3f, 0.45f, 0.05f),
                        floatArrayOf(0.0f, 0.5f, 0.1f),
                        floatArrayOf(0.3f, 0.45f, 0.05f),
                        floatArrayOf(0.15f, -0.15f, 0.15f),
                        floatArrayOf(0.25f, -0.5f, -0.1f)
                    )
                } else {
                    listOf(
                        floatArrayOf(-0.25f, -0.5f, -0.1f),
                        floatArrayOf(-0.2f, 0.45f, 0.1f),
                        floatArrayOf(0.2f, 0.45f, 0.1f),
                        floatArrayOf(0.25f, -0.5f, -0.1f)
                    )
                }
                Pair(pts, false)
            }

            YarnLoopCategory.DOUBLE_CROCHET -> {
                val pts = if (isLod0) {
                    listOf(
                        floatArrayOf(-0.2f, -0.5f, -0.1f),
                        floatArrayOf(-0.1f, -0.2f, 0.15f),
                        floatArrayOf(0.1f, 0.05f, -0.1f),
                        floatArrayOf(-0.1f, 0.3f, 0.15f),
                        floatArrayOf(0.0f, 0.48f, 0.08f),
                        floatArrayOf(0.25f, 0.2f, 0.1f),
                        floatArrayOf(0.2f, -0.5f, -0.1f)
                    )
                } else {
                    listOf(
                        floatArrayOf(-0.2f, -0.5f, -0.1f),
                        floatArrayOf(0.05f, -0.05f, 0.12f),
                        floatArrayOf(-0.05f, 0.45f, 0.08f),
                        floatArrayOf(0.2f, -0.5f, -0.1f)
                    )
                }
                Pair(pts, false)
            }

            YarnLoopCategory.HALF_DOUBLE_CROCHET -> {
                val pts = if (isLod0) {
                    listOf(
                        floatArrayOf(-0.22f, -0.5f, -0.1f),
                        floatArrayOf(-0.1f, -0.15f, 0.15f),
                        floatArrayOf(0.1f, 0.15f, -0.05f),
                        floatArrayOf(0.0f, 0.46f, 0.1f),
                        floatArrayOf(0.22f, 0.15f, 0.12f),
                        floatArrayOf(0.22f, -0.5f, -0.1f)
                    )
                } else {
                    listOf(
                        floatArrayOf(-0.22f, -0.5f, -0.1f),
                        floatArrayOf(-0.05f, 0.1f, 0.12f),
                        floatArrayOf(0.1f, 0.45f, 0.08f),
                        floatArrayOf(0.22f, -0.5f, -0.1f)
                    )
                }
                Pair(pts, false)
            }

            YarnLoopCategory.CHAIN -> {
                val pointCount = if (isLod0) 5 else 4
                val pts = ArrayList<FloatArray>(pointCount)
                for (i in 0 until pointCount) {
                    val theta = i * TWO_PI / pointCount
                    val x = 0.38f * cos(theta)
                    val y = 0.28f * sin(theta)
                    val z = 0.08f * sin(theta * 2f)
                    pts.add(floatArrayOf(x, y, z))
                }
                Pair(pts, true)
            }

            YarnLoopCategory.BLO -> {
                val pts = if (isLod0) {
                    listOf(
                        floatArrayOf(-0.25f, -0.3f, -0.2f),
                        floatArrayOf(-0.15f, -0.05f, -0.1f),
                        floatArrayOf(-0.3f, 0.45f, -0.15f),
                        floatArrayOf(0.0f, 0.5f, -0.1f),
                        floatArrayOf(0.3f, 0.45f, -0.15f),
                        floatArrayOf(0.25f, -0.3f, -0.2f)
                    )
                } else {
                    listOf(
                        floatArrayOf(-0.25f, -0.3f, -0.2f),
                        floatArrayOf(-0.15f, 0.45f, -0.15f),
                        floatArrayOf(0.15f, 0.45f, -0.15f),
                        floatArrayOf(0.25f, -0.3f, -0.2f)
                    )
                }
                Pair(pts, false)
            }

            YarnLoopCategory.BOBBLE -> {
                val pts = if (isLod0) {
                    listOf(
                        floatArrayOf(-0.25f, -0.4f, 0.0f),
                        floatArrayOf(-0.2f, -0.1f, 0.35f),
                        floatArrayOf(0.0f, 0.0f, 0.5f),
                        floatArrayOf(0.2f, -0.1f, 0.35f),
                        floatArrayOf(0.0f, 0.35f, 0.4f),
                        floatArrayOf(-0.15f, 0.45f, 0.15f),
                        floatArrayOf(0.25f, -0.4f, 0.0f)
                    )
                } else {
                    listOf(
                        floatArrayOf(-0.2f, -0.4f, 0.0f),
                        floatArrayOf(0.0f, -0.05f, 0.45f),
                        floatArrayOf(0.0f, 0.35f, 0.35f),
                        floatArrayOf(0.2f, -0.4f, 0.0f)
                    )
                }
                Pair(pts, false)
            }
        }

        val mesh = sweepTubularCurve(
            centerline = centerline,
            tubeRadius = tubeRadius,
            sides = sides,
            isClosed = isClosed
        )

        return YarnLoopPrototype(
            category = category,
            lodLevel = lodLevel,
            mesh = mesh
        )
    }

    /**
     * Barre una sección transversal circular de [sides] lados a lo largo de una curva 3D [centerline],
     * calculando marcos Frenet-Serret libres de torsión (Parallel Transport / RMF).
     */
    fun sweepTubularCurve(
        centerline: List<FloatArray>,
        tubeRadius: Float,
        sides: Int,
        isClosed: Boolean
    ): MeshGeometry {
        val m = centerline.size
        require(m >= 2) { "Centerline must contain at least 2 points" }
        require(sides >= 3) { "Sides must be at least 3" }

        val positions = ArrayList<Float>()
        val normals = ArrayList<Float>()
        val colors = ArrayList<Float>()
        val uvs = ArrayList<Float>()
        val indices = ArrayList<Int>()

        // 1. Calcular vectores tangentes T_i a lo largo de la curva
        val tangents = Array(m) { FloatArray(3) }
        for (i in 0 until m) {
            val prev = if (i == 0) {
                if (isClosed) centerline[m - 1] else centerline[0]
            } else {
                centerline[i - 1]
            }
            val next = if (i == m - 1) {
                if (isClosed) centerline[0] else centerline[m - 1]
            } else {
                centerline[i + 1]
            }
            var tx = next[0] - prev[0]
            var ty = next[1] - prev[1]
            var tz = next[2] - prev[2]
            var len = sqrt(tx * tx + ty * ty + tz * tz)
            if (len < 1e-6f) {
                tx = 0f; ty = 1f; tz = 0f; len = 1f
            }
            tangents[i][0] = tx / len
            tangents[i][1] = ty / len
            tangents[i][2] = tz / len
        }

        // 2. Calcular marco ortonormal (N_i, B_i) mediante Parallel Transport
        val normalFrames = Array(m) { FloatArray(3) }
        val binormalFrames = Array(m) { FloatArray(3) }

        // Marco inicial para i=0
        val t0 = tangents[0]
        val up = if (abs(t0[1]) < 0.9f) floatArrayOf(0f, 1f, 0f) else floatArrayOf(1f, 0f, 0f)
        var n0x = up[1] * t0[2] - up[2] * t0[1]
        var n0y = up[2] * t0[0] - up[0] * t0[2]
        var n0z = up[0] * t0[1] - up[1] * t0[0]
        var lenN0 = sqrt(n0x * n0x + n0y * n0y + n0z * n0z).coerceAtLeast(1e-6f)
        normalFrames[0][0] = n0x / lenN0
        normalFrames[0][1] = n0y / lenN0
        normalFrames[0][2] = n0z / lenN0

        binormalFrames[0][0] = t0[1] * normalFrames[0][2] - t0[2] * normalFrames[0][1]
        binormalFrames[0][1] = t0[2] * normalFrames[0][0] - t0[0] * normalFrames[0][2]
        binormalFrames[0][2] = t0[0] * normalFrames[0][1] - t0[1] * normalFrames[0][0]

        // Transporte paralelo para i > 0
        for (i in 1 until m) {
            val tPrev = tangents[i - 1]
            val tCur = tangents[i]
            val prevN = normalFrames[i - 1]

            // Proyección de prevN perpendicular a tCur
            val dot = prevN[0] * tCur[0] + prevN[1] * tCur[1] + prevN[2] * tCur[2]
            var nx = prevN[0] - dot * tCur[0]
            var ny = prevN[1] - dot * tCur[1]
            var nz = prevN[2] - dot * tCur[2]
            var len = sqrt(nx * nx + ny * ny + nz * nz)

            if (len < 1e-4f) {
                // Fallback ortogonal
                val fallbackUp = if (abs(tCur[1]) < 0.9f) floatArrayOf(0f, 1f, 0f) else floatArrayOf(1f, 0f, 0f)
                nx = fallbackUp[1] * tCur[2] - fallbackUp[2] * tCur[1]
                ny = fallbackUp[2] * tCur[0] - fallbackUp[0] * tCur[2]
                nz = fallbackUp[0] * tCur[1] - fallbackUp[1] * tCur[0]
                len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-6f)
            }
            normalFrames[i][0] = nx / len
            normalFrames[i][1] = ny / len
            normalFrames[i][2] = nz / len

            // Bitangente = Tangente x Normal
            binormalFrames[i][0] = tCur[1] * normalFrames[i][2] - tCur[2] * normalFrames[i][1]
            binormalFrames[i][1] = tCur[2] * normalFrames[i][0] - tCur[0] * normalFrames[i][2]
            binormalFrames[i][2] = tCur[0] * normalFrames[i][1] - tCur[1] * normalFrames[i][0]
        }

        // 3. Generación de anillos perimetrales de vértices
        for (i in 0 until m) {
            val c = centerline[i]
            val nFrame = normalFrames[i]
            val bFrame = binormalFrames[i]
            val vUv = i.toFloat() / (m - 1).coerceAtLeast(1).toFloat()

            for (k in 0 until sides) {
                val phi = k * TWO_PI / sides
                val cosP = cos(phi)
                val sinP = sin(phi)

                // Vector normal radial unitario estricto: ||radN|| = 1.0
                val radNx = cosP * nFrame[0] + sinP * bFrame[0]
                val radNy = cosP * nFrame[1] + sinP * bFrame[1]
                val radNz = cosP * nFrame[2] + sinP * bFrame[2]

                val px = c[0] + tubeRadius * radNx
                val py = c[1] + tubeRadius * radNy
                val pz = c[2] + tubeRadius * radNz

                positions.add(px); positions.add(py); positions.add(pz)
                normals.add(radNx); normals.add(radNy); normals.add(radNz)
                colors.add(1f); colors.add(1f); colors.add(1f); colors.add(1f)
                uvs.add(k.toFloat() / sides); uvs.add(vUv)
            }
        }

        // 4. Triangulación de la superficie tubular
        val segmentCount = if (isClosed) m else (m - 1)
        for (i in 0 until segmentCount) {
            val nextI = (i + 1) % m
            val ringOffsetCur = i * sides
            val ringOffsetNext = nextI * sides

            for (k in 0 until sides) {
                val nextK = (k + 1) % sides

                val i0 = ringOffsetCur + k
                val i1 = ringOffsetNext + k
                val i2 = ringOffsetCur + nextK
                val i3 = ringOffsetNext + nextK

                // Triángulo 1 (CCW exterior)
                indices.add(i0)
                indices.add(i1)
                indices.add(i2)

                // Triángulo 2 (CCW exterior)
                indices.add(i2)
                indices.add(i1)
                indices.add(i3)
            }
        }

        // 5. Cierre de tapas en extremos si la curva no es un bucle cerrado
        if (!isClosed) {
            // Tapa de inicio (anillo 0)
            val capStartIdx = positions.size / 3
            val c0 = centerline[0]
            val t0Norm = tangents[0]
            positions.add(c0[0]); positions.add(c0[1]); positions.add(c0[2])
            normals.add(-t0Norm[0]); normals.add(-t0Norm[1]); normals.add(-t0Norm[2])
            colors.add(1f); colors.add(1f); colors.add(1f); colors.add(1f)
            uvs.add(0.5f); uvs.add(0f)

            for (k in 0 until sides) {
                val nextK = (k + 1) % sides
                indices.add(capStartIdx)
                indices.add(nextK)
                indices.add(k)
            }

            // Tapa de final (anillo m-1)
            val capEndIdx = positions.size / 3
            val cEnd = centerline[m - 1]
            val tEndNorm = tangents[m - 1]
            val lastRingOffset = (m - 1) * sides

            positions.add(cEnd[0]); positions.add(cEnd[1]); positions.add(cEnd[2])
            normals.add(tEndNorm[0]); normals.add(tEndNorm[1]); normals.add(tEndNorm[2])
            colors.add(1f); colors.add(1f); colors.add(1f); colors.add(1f)
            uvs.add(0.5f); uvs.add(1f)

            for (k in 0 until sides) {
                val nextK = (k + 1) % sides
                indices.add(capEndIdx)
                indices.add(lastRingOffset + k)
                indices.add(lastRingOffset + nextK)
            }
        }

        val vertexPositions = positions.toFloatArray()
        val vertexNormals = normals.toFloatArray()
        val vertexColors = colors.toFloatArray()
        val vertexUvs = uvs.toFloatArray()
        val indicesArray = indices.toIntArray()
        val vertexCount = vertexPositions.size / 3
        val triangleCount = indicesArray.size / 3

        return MeshGeometry(
            vertexPositions = vertexPositions,
            indices = indicesArray,
            vertexCount = vertexCount,
            triangleCount = triangleCount,
            vertexNormals = vertexNormals,
            vertexColors = vertexColors,
            vertexUvs = vertexUvs
        )
    }

    /**
     * Limpia la caché interna de prototipos para liberar recursos o permitir regeneración.
     */
    fun clearCache() {
        prototypeCache.clear()
    }
}
