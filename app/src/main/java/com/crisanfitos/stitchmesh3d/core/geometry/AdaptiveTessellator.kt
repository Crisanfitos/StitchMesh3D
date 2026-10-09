package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.color.CrochetColorHelper
import com.crisanfitos.stitchmesh3d.core.engine.model.TopologyFlag
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Algoritmo de teselación adaptativa entre anillos de crochet y generación de geometría 3D indexada.
 *
 * Implementa TRD §3.2 y RF-3.2:
 * - Triangulación de cremallera angular mínima entre anillos de cardinalidad dispar.
 * - Topologías divergentes para aumentos (aum) y convergentes para disminuciones (dism).
 * - Cierre de casquete polar en la base (Anillo Mágico) mediante abanico de triángulos (Triangle Fan).
 * - Duplicación de vértices en puntadas con [TopologyFlag.BLO] para desacoplar el sombreado (Normal Split a 90°).
 * - Cálculo de normales de superficie por cara acumuladas con ponderación de área y estrictamente normalizadas (||n|| = 1.0).
 * - Genera mallas 2-variedad cerradas con winding order consistente CCW (normales radiales hacia afuera).
 */
object AdaptiveTessellator {

    /**
     * Tesela una secuencia completa de perfiles de anillo en una malla 3D indexada continua con normales.
     *
     * @param rings Lista de perfiles de anillo ordenados por vuelta (V1..VN).
     * @param includePolarCap Si es true, añade un vértice central en el origen para cerrar la base del Anillo Mágico.
     * @return [MeshGeometry] conteniendo posiciones, normales de vértice e índices triangulados.
     */
    fun tessellate(
        rings: List<RingProfile>,
        includePolarCap: Boolean = true,
        defaultColorHex: String = "#E06D53"
    ): MeshGeometry {
        val validRings = rings.filter { it.vertexCount > 0 }
        if (validRings.isEmpty()) {
            return MeshGeometry(
                vertexPositions = FloatArray(0),
                indices = ShortArray(0),
                vertexCount = 0,
                triangleCount = 0,
                vertexNormals = FloatArray(0),
                vertexColors = FloatArray(0)
            )
        }

        val hasPole = includePolarCap && validRings.isNotEmpty()

        // Asignación de índices: cada puntada no-BLO usa un único vértice (in == out).
        // Las puntadas con TopologyFlag.BLO duplican su vértice: inIndex para la cara inferior, outIndex para la superior.
        var currentOffset = 0
        val inIndices = Array(validRings.size) { ShortArray(validRings[it].vertexCount) }
        val outIndices = Array(validRings.size) { ShortArray(validRings[it].vertexCount) }

        val positionsList = ArrayList<Float>()
        val colorsList = ArrayList<Float>()

        // 1. Vértice polar (índice 0 si está activo)
        if (hasPole) {
            val firstRing = validRings.first()
            val poleZ = (firstRing.minZ - (firstRing.meanRadiusMm * 0.25)).coerceAtLeast(0.0).toFloat()
            positionsList.add(0.0f)
            positionsList.add(0.0f)
            positionsList.add(poleZ)

            val poleColor = firstRing.vertices.firstOrNull()?.colorHex ?: defaultColorHex
            val poleRgba = CrochetColorHelper.toRgbaFloats(poleColor)
            colorsList.add(poleRgba[0])
            colorsList.add(poleRgba[1])
            colorsList.add(poleRgba[2])
            colorsList.add(poleRgba[3])
            currentOffset = 1
        }

        // 2. Vértices de cada anillo (con soporte de duplicación BLO)
        for (r in validRings.indices) {
            val ring = validRings[r]
            for (j in ring.vertices.indices) {
                val v = ring.vertices[j]
                val isBlo = v.stitchType.topologyFlags.contains(TopologyFlag.BLO)

                val inIdx = currentOffset.toShort()
                inIndices[r][j] = inIdx
                positionsList.add(v.x.toFloat())
                positionsList.add(v.y.toFloat())
                positionsList.add(v.z.toFloat())

                val vColor = v.colorHex ?: defaultColorHex
                val rgba = CrochetColorHelper.toRgbaFloats(vColor)
                colorsList.add(rgba[0])
                colorsList.add(rgba[1])
                colorsList.add(rgba[2])
                colorsList.add(rgba[3])
                currentOffset++

                if (isBlo) {
                    val outIdx = currentOffset.toShort()
                    outIndices[r][j] = outIdx
                    // Vértice duplicado en la misma posición espacial exacta para preservar contigüidad
                    positionsList.add(v.x.toFloat())
                    positionsList.add(v.y.toFloat())
                    positionsList.add(v.z.toFloat())

                    colorsList.add(rgba[0])
                    colorsList.add(rgba[1])
                    colorsList.add(rgba[2])
                    colorsList.add(rgba[3])
                    currentOffset++
                } else {
                    outIndices[r][j] = inIdx
                }
            }
        }

        val totalVertices = currentOffset
        val positions = FloatArray(positionsList.size) { positionsList[it] }
        val hasCustomColors = validRings.any { r -> r.vertices.any { it.colorHex != null } }
        val colors = if (hasCustomColors) {
            FloatArray(colorsList.size) { colorsList[it] }
        } else {
            FloatArray(0)
        }

        // 3. Generación del búfer de índices triangulados
        val indexList = ArrayList<Short>()

        // 3a. Casquete polar (Triangle Fan para cerrar el fondo del Anillo Mágico)
        if (hasPole && validRings.isNotEmpty()) {
            val n = validRings[0].vertexCount
            val r0In = inIndices[0]
            for (j in 0 until n) {
                val nextJ = (j + 1) % n
                // Winding order CCW para normal orientada hacia el exterior del fondo
                indexList.add(0.toShort())
                indexList.add(r0In[nextJ])
                indexList.add(r0In[j])
            }
        }

        // 3b. Teselación adaptativa entre anillos adyacentes
        for (r in 0 until validRings.size - 1) {
            val ring0 = validRings[r]
            val ring1 = validRings[r + 1]
            val r0Out = outIndices[r]
            val r1In = inIndices[r + 1]

            tessellateRingPair(ring0, ring1, r0Out, r1In, indexList)
        }

        val indicesArray = ShortArray(indexList.size) { indexList[it] }
        val triangleCount = indicesArray.size / 3

        // 4. Cálculo ponderado de normales por área de caras adyacentes y suavizado analítico de revolución (RF-3.2)
        val normals = calculateSmoothAndAreaWeightedNormals(
            positions = positions,
            indices = indicesArray,
            vertexCount = totalVertices,
            triangleCount = triangleCount,
            validRings = validRings,
            hasPole = hasPole,
            inIndices = inIndices,
            outIndices = outIndices
        )

        return MeshGeometry(
            vertexPositions = positions,
            indices = indicesArray,
            vertexCount = totalVertices,
            triangleCount = triangleCount,
            vertexNormals = normals,
            vertexColors = colors
        )
    }

    /**
     * Calcula normales de superficie combinando el perfil analítico de revolución (para eliminar
     * el efecto de facetado angular y de papel arrugado) con las normales de caras triangulares acumuladas.
     * Garantiza magnitud unitaria ||n|| = 1.0 +/- 0.001 en todo el búfer.
     */
    private fun calculateSmoothAndAreaWeightedNormals(
        positions: FloatArray,
        indices: ShortArray,
        vertexCount: Int,
        triangleCount: Int,
        validRings: List<RingProfile>,
        hasPole: Boolean,
        inIndices: Array<ShortArray>,
        outIndices: Array<ShortArray>
    ): FloatArray {
        val faceNormalAccum = FloatArray(vertexCount * 3)

        for (t in 0 until triangleCount) {
            val i0 = (indices[t * 3].toInt() and 0xFFFF)
            val i1 = (indices[t * 3 + 1].toInt() and 0xFFFF)
            val i2 = (indices[t * 3 + 2].toInt() and 0xFFFF)

            val v0x = positions[i0 * 3]
            val v0y = positions[i0 * 3 + 1]
            val v0z = positions[i0 * 3 + 2]

            val v1x = positions[i1 * 3]
            val v1y = positions[i1 * 3 + 1]
            val v1z = positions[i1 * 3 + 2]

            val v2x = positions[i2 * 3]
            val v2y = positions[i2 * 3 + 1]
            val v2z = positions[i2 * 3 + 2]

            val e1x = v1x - v0x
            val e1y = v1y - v0y
            val e1z = v1z - v0z

            val e2x = v2x - v0x
            val e2y = v2y - v0y
            val e2z = v2z - v0z

            // Producto vectorial
            val nx = e1y * e2z - e1z * e2y
            val ny = e1z * e2x - e1x * e2z
            val nz = e1x * e2y - e1y * e2x

            faceNormalAccum[i0 * 3] += nx
            faceNormalAccum[i0 * 3 + 1] += ny
            faceNormalAccum[i0 * 3 + 2] += nz

            faceNormalAccum[i1 * 3] += nx
            faceNormalAccum[i1 * 3 + 1] += ny
            faceNormalAccum[i1 * 3 + 2] += nz

            faceNormalAccum[i2 * 3] += nx
            faceNormalAccum[i2 * 3 + 1] += ny
            faceNormalAccum[i2 * 3 + 2] += nz
        }

        val normals = FloatArray(vertexCount * 3)

        // 1. Normal del vértice polar (apunta hacia el fondo exterior del casquete)
        if (hasPole && vertexCount > 0) {
            normals[0] = 0.0f
            normals[1] = 0.0f
            normals[2] = -1.0f
        }

        // 2. Cálculo para cada anillo
        for (r in validRings.indices) {
            val ring = validRings[r]
            val prevRing = if (r > 0) validRings[r - 1] else null
            val nextRing = if (r < validRings.size - 1) validRings[r + 1] else null

            val rPrev = prevRing?.meanRadiusMm ?: 0.0
            val rNext = nextRing?.meanRadiusMm ?: ring.meanRadiusMm
            val zPrev = prevRing?.minZ ?: (validRings[0].minZ - validRings[0].meanRadiusMm * 0.25).coerceAtLeast(0.0)
            val zNext = nextRing?.maxZ ?: ring.maxZ

            val dr = rNext - rPrev
            val dz = (zNext - zPrev).coerceAtLeast(0.05)
            val pLen = sqrt(dr * dr + dz * dz).coerceAtLeast(1e-4)
            val profCos = (dz / pLen).toFloat()
            val profSin = (-dr / pLen).toFloat()

            for (j in ring.vertices.indices) {
                val v = ring.vertices[j]
                val cosT = cos(v.theta).toFloat()
                val sinT = sin(v.theta).toFloat()

                val revNx = profCos * cosT
                val revNy = profCos * sinT
                val revNz = profSin

                val inIdx = inIndices[r][j].toInt()
                val outIdx = outIndices[r][j].toInt()
                val isBlo = v.stitchType.topologyFlags.contains(TopologyFlag.BLO)

                // Extraer normal acumulada de caras para inIdx
                val fnx = faceNormalAccum[inIdx * 3]
                val fny = faceNormalAccum[inIdx * 3 + 1]
                val fnz = faceNormalAccum[inIdx * 3 + 2]
                val flen = sqrt(fnx * fnx + fny * fny + fnz * fnz)

                val faceNx = if (flen > 1e-6f) fnx / flen else revNx
                val faceNy = if (flen > 1e-6f) fny / flen else revNy
                val faceNz = if (flen > 1e-6f) fnz / flen else revNz

                val blendNx: Float
                val blendNy: Float
                val blendNz: Float

                if (isBlo) {
                    // BLO: Conservar la normal de cara nítida para enfatizar la arista viva
                    blendNx = faceNx
                    blendNy = faceNy
                    blendNz = faceNz
                } else if (v.normalBumpMm > 0) {
                    // Relieve volumétrico (bobble/popcorn): 40% superficie suave + 60% relieve
                    blendNx = 0.40f * revNx + 0.60f * faceNx
                    blendNy = 0.40f * revNy + 0.60f * faceNy
                    blendNz = 0.40f * revNz + 0.60f * faceNz
                } else {
                    // Puntadas estándar: 80% superficie analítica suave + 20% caras adyacentes
                    blendNx = 0.80f * revNx + 0.20f * faceNx
                    blendNy = 0.80f * revNy + 0.20f * faceNy
                    blendNz = 0.80f * revNz + 0.20f * faceNz
                }

                val blen = sqrt(blendNx * blendNx + blendNy * blendNy + blendNz * blendNz)
                if (blen > 1e-6f) {
                    normals[inIdx * 3] = blendNx / blen
                    normals[inIdx * 3 + 1] = blendNy / blen
                    normals[inIdx * 3 + 2] = blendNz / blen
                } else {
                    normals[inIdx * 3] = revNx
                    normals[inIdx * 3 + 1] = revNy
                    normals[inIdx * 3 + 2] = revNz
                }

                // Vértice duplicado para BLO
                if (isBlo && outIdx != inIdx) {
                    val ofnx = faceNormalAccum[outIdx * 3]
                    val ofny = faceNormalAccum[outIdx * 3 + 1]
                    val ofnz = faceNormalAccum[outIdx * 3 + 2]
                    val oflen = sqrt(ofnx * ofnx + ofny * ofny + ofnz * ofnz)
                    if (oflen > 1e-6f) {
                        normals[outIdx * 3] = ofnx / oflen
                        normals[outIdx * 3 + 1] = ofny / oflen
                        normals[outIdx * 3 + 2] = ofnz / oflen
                    } else {
                        normals[outIdx * 3] = normals[inIdx * 3]
                        normals[outIdx * 3 + 1] = normals[inIdx * 3 + 1]
                        normals[outIdx * 3 + 2] = normals[inIdx * 3 + 2]
                    }
                }
            }
        }

        // Si hubiera algún vértice no indexado por los anillos, asegurar normal unitaria
        for (v in 0 until vertexCount) {
            val nx = normals[v * 3]
            val ny = normals[v * 3 + 1]
            val nz = normals[v * 3 + 2]
            val len = sqrt(nx * nx + ny * ny + nz * nz)
            if (len < 1e-6f) {
                normals[v * 3] = 0f
                normals[v * 3 + 1] = 0f
                normals[v * 3 + 2] = 1f
            }
        }

        return normals
    }

    /**
     * Tesela la banda cilíndrica/cónica entre dos anillos adyacentes mediante avance angular adaptativo.
     */
    private fun tessellateRingPair(
        ring0: RingProfile,
        ring1: RingProfile,
        ring0Indices: ShortArray,
        ring1Indices: ShortArray,
        outIndices: ArrayList<Short>
    ) {
        val n0 = ring0.vertexCount
        val n1 = ring1.vertexCount
        if (n0 == 0 || n1 == 0) return

        // Caso isomorfo 1:1 rápido
        if (n0 == n1) {
            for (j in 0 until n0) {
                val nextJ = (j + 1) % n0
                val p0 = ring0Indices[j]
                val p1 = ring0Indices[nextJ]
                val c0 = ring1Indices[j]
                val c1 = ring1Indices[nextJ]

                // Triángulo inferior-superior (CCW outward normal)
                outIndices.add(p0)
                outIndices.add(p1)
                outIndices.add(c0)

                // Triángulo superior-inferior (CCW outward normal)
                outIndices.add(p1)
                outIndices.add(c1)
                outIndices.add(c0)
            }
            return
        }

        // Caso asimétrico adaptativo general (aumentos / disminuciones)
        var i = 0
        var j = 0

        while (i < n0 || j < n1) {
            if (i == n0) {
                // Completar vuelta superior
                val p = ring0Indices[i % n0]
                val cCurrent = ring1Indices[j % n1]
                val cNext = ring1Indices[(j + 1) % n1]
                outIndices.add(p)
                outIndices.add(cNext)
                outIndices.add(cCurrent)
                j++
            } else if (j == n1) {
                // Completar vuelta inferior
                val pCurrent = ring0Indices[i % n0]
                val pNext = ring0Indices[(i + 1) % n0]
                val c = ring1Indices[j % n1]
                outIndices.add(pCurrent)
                outIndices.add(pNext)
                outIndices.add(c)
                i++
            } else {
                val nextAngle0 = (i + 1).toDouble() / n0
                val nextAngle1 = (j + 1).toDouble() / n1

                if (nextAngle0 <= nextAngle1) {
                    // Avanzar en el anillo inferior (generando triángulo convergente si n0 > n1)
                    val pCurrent = ring0Indices[i % n0]
                    val pNext = ring0Indices[(i + 1) % n0]
                    val c = ring1Indices[j % n1]
                    outIndices.add(pCurrent)
                    outIndices.add(pNext)
                    outIndices.add(c)
                    i++
                } else {
                    // Avanzar en el anillo superior (generando triángulo divergente si n1 > n0)
                    val p = ring0Indices[i % n0]
                    val cCurrent = ring1Indices[j % n1]
                    val cNext = ring1Indices[(j + 1) % n1]
                    outIndices.add(p)
                    outIndices.add(cNext)
                    outIndices.add(cCurrent)
                    j++
                }
            }
        }
    }
}

