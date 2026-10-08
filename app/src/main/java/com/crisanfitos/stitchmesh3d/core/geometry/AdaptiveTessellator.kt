package com.crisanfitos.stitchmesh3d.core.geometry

import kotlin.math.abs

/**
 * Algoritmo de teselación adaptativa entre anillos de crochet y generación de búfer de índices.
 *
 * Implementa TRD §3.2:
 * - Triangulación de cremallera angular mínima entre anillos de cardinalidad dispar.
 * - Topologías divergentes para aumentos (aum) y convergentes para disminuciones (dism).
 * - Cierre de casquete polar en la base (Anillo Mágico) mediante abanico de triángulos (Triangle Fan).
 * - Genera mallas 2-variedad cerradas con winding order consistente CCW (normales hacia afuera).
 */
object AdaptiveTessellator {

    /**
     * Tesela una secuencia completa de perfiles de anillo en una malla 3D indexada continua.
     *
     * @param rings Lista de perfiles de anillo ordenados por vuelta (V1..VN).
     * @param includePolarCap Si es true, añade un vértice central en el origen para cerrar la base del Anillo Mágico.
     * @return [MeshGeometry] conteniendo el búfer de posiciones de vértices (FloatArray) y el búfer de índices (ShortArray).
     */
    fun tessellate(
        rings: List<RingProfile>,
        includePolarCap: Boolean = true
    ): MeshGeometry {
        val validRings = rings.filter { it.vertexCount > 0 }
        if (validRings.isEmpty()) {
            return MeshGeometry(
                vertexPositions = FloatArray(0),
                indices = ShortArray(0),
                vertexCount = 0,
                triangleCount = 0
            )
        }

        val totalRingVertices = validRings.sumOf { it.vertexCount }
        val hasPole = includePolarCap && validRings.isNotEmpty()
        val totalVertices = if (hasPole) totalRingVertices + 1 else totalRingVertices

        // Construir búfer continuo de posiciones de vértices [X, Y, Z, ...]
        val positions = FloatArray(totalVertices * 3)
        var posIndex = 0

        val ringBaseOffsets = IntArray(validRings.size)

        // 1. Vértice polar (índice 0 si está activo)
        var currentOffset = 0
        if (hasPole) {
            val firstRing = validRings.first()
            val poleZ = (firstRing.minZ - (firstRing.meanRadiusMm * 0.25)).coerceAtLeast(0.0).toFloat()
            positions[posIndex++] = 0.0f
            positions[posIndex++] = 0.0f
            positions[posIndex++] = poleZ
            currentOffset = 1
        }

        // 2. Vértices de cada anillo
        for (r in validRings.indices) {
            ringBaseOffsets[r] = currentOffset
            val ring = validRings[r]
            for (v in ring.vertices) {
                positions[posIndex++] = v.x.toFloat()
                positions[posIndex++] = v.y.toFloat()
                positions[posIndex++] = v.z.toFloat()
            }
            currentOffset += ring.vertexCount
        }

        // 3. Generación del búfer de índices triangulados
        val indexList = ArrayList<Short>()

        // 3a. Casquete polar (Triangle Fan para cerrar el fondo del Anillo Mágico)
        if (hasPole && validRings.isNotEmpty()) {
            val firstRing = validRings[0]
            val n = firstRing.vertexCount
            val baseOffset = ringBaseOffsets[0]
            for (j in 0 until n) {
                val nextJ = (j + 1) % n
                // Winding order para normal orientada hacia el exterior del fondo
                indexList.add(0.toShort())
                indexList.add((baseOffset + nextJ).toShort())
                indexList.add((baseOffset + j).toShort())
            }
        }

        // 3b. Teselación adaptativa entre anillos adyacentes
        for (r in 0 until validRings.size - 1) {
            val ring0 = validRings[r]
            val ring1 = validRings[r + 1]
            val offset0 = ringBaseOffsets[r]
            val offset1 = ringBaseOffsets[r + 1]

            tessellateRingPair(ring0, ring1, offset0, offset1, indexList)
        }

        val indicesArray = ShortArray(indexList.size) { indexList[it] }

        return MeshGeometry(
            vertexPositions = positions,
            indices = indicesArray,
            vertexCount = totalVertices,
            triangleCount = indicesArray.size / 3
        )
    }

    /**
     * Tesela la banda cilíndrica/cónica entre dos anillos adyacentes mediante avance angular adaptativo.
     */
    private fun tessellateRingPair(
        ring0: RingProfile,
        ring1: RingProfile,
        offset0: Int,
        offset1: Int,
        outIndices: ArrayList<Short>
    ) {
        val n0 = ring0.vertexCount
        val n1 = ring1.vertexCount
        if (n0 == 0 || n1 == 0) return

        // Caso isomorfo 1:1 rápido
        if (n0 == n1) {
            for (j in 0 until n0) {
                val nextJ = (j + 1) % n0
                val p0 = (offset0 + j).toShort()
                val p1 = (offset0 + nextJ).toShort()
                val c0 = (offset1 + j).toShort()
                val c1 = (offset1 + nextJ).toShort()

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
                val p = (offset0 + (i % n0)).toShort()
                val cCurrent = (offset1 + (j % n1)).toShort()
                val cNext = (offset1 + ((j + 1) % n1)).toShort()
                outIndices.add(p)
                outIndices.add(cNext)
                outIndices.add(cCurrent)
                j++
            } else if (j == n1) {
                // Completar vuelta inferior
                val pCurrent = (offset0 + (i % n0)).toShort()
                val pNext = (offset0 + ((i + 1) % n0)).toShort()
                val c = (offset1 + (j % n1)).toShort()
                outIndices.add(pCurrent)
                outIndices.add(pNext)
                outIndices.add(c)
                i++
            } else {
                val nextAngle0 = (i + 1).toDouble() / n0
                val nextAngle1 = (j + 1).toDouble() / n1

                if (nextAngle0 <= nextAngle1) {
                    // Avanzar en el anillo inferior (generando triángulo convergente si n0 > n1)
                    val pCurrent = (offset0 + (i % n0)).toShort()
                    val pNext = (offset0 + ((i + 1) % n0)).toShort()
                    val c = (offset1 + (j % n1)).toShort()
                    outIndices.add(pCurrent)
                    outIndices.add(pNext)
                    outIndices.add(c)
                    i++
                } else {
                    // Avanzar en el anillo superior (generando triángulo divergente si n1 > n0)
                    val p = (offset0 + (i % n0)).toShort()
                    val cCurrent = (offset1 + (j % n1)).toShort()
                    val cNext = (offset1 + ((j + 1) % n1)).toShort()
                    outIndices.add(p)
                    outIndices.add(cNext)
                    outIndices.add(cCurrent)
                    j++
                }
            }
        }
    }
}
