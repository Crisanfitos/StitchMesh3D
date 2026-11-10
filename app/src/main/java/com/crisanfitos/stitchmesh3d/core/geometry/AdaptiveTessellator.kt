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
        defaultColorHex: String = "#E06D53",
        wallThicknessMm: Float = 0f,
        decoupledStitchQuads: Boolean = false
    ): MeshGeometry {
        val validRings = rings.filter { it.vertexCount > 0 }
        if (validRings.isEmpty()) {
            return MeshGeometry(
                vertexPositions = FloatArray(0),
                indices = IntArray(0),
                vertexCount = 0,
                triangleCount = 0,
                vertexNormals = FloatArray(0),
                vertexColors = FloatArray(0),
                vertexUvs = FloatArray(0),
                vertexTangents = FloatArray(0)
            )
        }

        if (decoupledStitchQuads) {
            return tessellateDecoupled(
                validRings = validRings,
                includePolarCap = includePolarCap,
                defaultColorHex = defaultColorHex,
                wallThicknessMm = wallThicknessMm
            )
        }

        val hasPole = includePolarCap && validRings.isNotEmpty()

        // Asignación de índices: cada puntada no-BLO usa un único vértice (in == out).
        // Las puntadas con TopologyFlag.BLO duplican su vértice: inIndex para la cara inferior, outIndex para la superior.
        var currentOffset = 0
        val inIndices = Array(validRings.size) { IntArray(validRings[it].vertexCount) }
        val outIndices = Array(validRings.size) { IntArray(validRings[it].vertexCount) }

        val positionsList = ArrayList<Float>()
        val colorsList = ArrayList<Float>()
        val uvsList = ArrayList<Float>()
        val tangentsList = ArrayList<Float>()

        // 1. Vértice polar (índice 0 si está activo) en sistema Y-up estándar
        if (hasPole) {
            val firstRing = validRings.first()
            val poleY = (firstRing.minY - (firstRing.meanRadiusMm * 0.25)).coerceAtLeast(0.0).toFloat()
            positionsList.add(0.0f)
            positionsList.add(poleY)
            positionsList.add(0.0f)

            val poleColor = firstRing.vertices.firstOrNull()?.colorHex ?: defaultColorHex
            val poleRgba = CrochetColorHelper.toRgbaFloats(poleColor)
            colorsList.add(poleRgba[0])
            colorsList.add(poleRgba[1])
            colorsList.add(poleRgba[2])
            colorsList.add(poleRgba[3])

            val poleStitch = firstRing.vertices.firstOrNull()?.stitchType
            val poleUv = if (poleStitch != null) {
                CrochetTextureGenerator.computeAtlasUv(poleStitch, 0.5f, 0.0f)
            } else {
                floatArrayOf(0.25f, 0.03f)
            }
            uvsList.add(poleUv[0])
            uvsList.add(poleUv[1])

            tangentsList.add(1.0f)
            tangentsList.add(0.0f)
            tangentsList.add(0.0f)
            tangentsList.add(1.0f)
            currentOffset = 1
        }

        // 2. Vértices de cada anillo (con soporte de duplicación BLO)
        for (r in validRings.indices) {
            val ring = validRings[r]
            for (j in ring.vertices.indices) {
                val v = ring.vertices[j]
                val isBlo = v.stitchType.topologyFlags.contains(TopologyFlag.BLO)

                val inIdx = currentOffset
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

                val localU = if (j % 2 == 0) 0.0f else 1.0f
                val localV = if (r % 2 == 0) 0.0f else 1.0f
                val atlasUv = CrochetTextureGenerator.computeAtlasUv(v.stitchType, localU, localV)
                val u = atlasUv[0]
                val vCoord = atlasUv[1]
                uvsList.add(u)
                uvsList.add(vCoord)

                val tx = -sin(v.theta).toFloat()
                val ty = 0.0f
                val tz = -cos(v.theta).toFloat()
                tangentsList.add(tx)
                tangentsList.add(ty)
                tangentsList.add(tz)
                tangentsList.add(1.0f)
                currentOffset++

                val nextRing = validRings.getOrNull(r + 1)
                val closestNext = nextRing?.findClosestVertex(v.theta)
                val nextColor = closestNext?.colorHex
                    ?: nextRing?.vertices?.firstOrNull()?.colorHex
                    ?: vColor
                val hasColorChange = nextRing != null && nextColor != vColor

                if (isBlo || hasColorChange) {
                    val outIdx = currentOffset
                    outIndices[r][j] = outIdx
                    // Vértice duplicado en la misma posición espacial exacta para preservar contigüidad
                    positionsList.add(v.x.toFloat())
                    positionsList.add(v.y.toFloat())
                    positionsList.add(v.z.toFloat())

                    // En frontera cromática, el vértice saliente outIdx adopta el color del siguiente tramo
                    val outColorRgba = if (hasColorChange) {
                        CrochetColorHelper.toRgbaFloats(nextColor)
                    } else {
                        rgba
                    }

                    colorsList.add(outColorRgba[0])
                    colorsList.add(outColorRgba[1])
                    colorsList.add(outColorRgba[2])
                    colorsList.add(outColorRgba[3])

                    uvsList.add(u)
                    uvsList.add(vCoord)

                    tangentsList.add(tx)
                    tangentsList.add(ty)
                    tangentsList.add(tz)
                    tangentsList.add(1.0f)
                    currentOffset++
                } else {
                    outIndices[r][j] = inIdx
                }
            }
        }

        // 3. Vértices de espesor de pared interior para piezas con grosor de tensión (SM-083)
        val hasInteriorWall = wallThicknessMm > 0f && validRings.isNotEmpty()
        val innerIndices = if (hasInteriorWall) {
            Array(validRings.size) { r -> IntArray(validRings[r].vertexCount) }
        } else {
            emptyArray()
        }

        if (hasInteriorWall) {
            for (r in validRings.indices) {
                val ring = validRings[r]
                for (j in ring.vertices.indices) {
                    val v = ring.vertices[j]
                    val inIdx = currentOffset++
                    innerIndices[r][j] = inIdx

                    val rad = sqrt(v.x * v.x + v.z * v.z).toFloat()
                    val inwardRatio = if (rad > wallThicknessMm) {
                        (rad - wallThicknessMm) / rad
                    } else {
                        0.25f
                    }
                    val inX = (v.x * inwardRatio).toFloat()
                    val inY = v.y.toFloat()
                    val inZ = (v.z * inwardRatio).toFloat()

                    positionsList.add(inX)
                    positionsList.add(inY)
                    positionsList.add(inZ)

                    val vColor = v.colorHex ?: defaultColorHex
                    val rgba = CrochetColorHelper.toRgbaFloats(vColor)
                    colorsList.add(rgba[0] * 0.88f)
                    colorsList.add(rgba[1] * 0.88f)
                    colorsList.add(rgba[2] * 0.88f)
                    colorsList.add(rgba[3])

                    val localU = if (j % 2 == 0) 0.0f else 1.0f
                    val localV = if (r % 2 == 0) 0.0f else 1.0f
                    val atlasUv = CrochetTextureGenerator.computeAtlasUv(v.stitchType, localU, localV)
                    uvsList.add(atlasUv[0])
                    uvsList.add(atlasUv[1])

                    val tx = -sin(v.theta).toFloat()
                    val ty = 0.0f
                    val tz = -cos(v.theta).toFloat()
                    tangentsList.add(tx)
                    tangentsList.add(ty)
                    tangentsList.add(tz)
                    tangentsList.add(1.0f)
                }
            }
        }

        val totalVertices = currentOffset
        val positions = FloatArray(positionsList.size) { positionsList[it] }
        val hasCustomColors = validRings.any { r ->
            r.vertices.any { it.colorHex != null }
        } || (defaultColorHex.isNotEmpty() && defaultColorHex != "#E06D53")
        val colors = if (hasCustomColors) {
            FloatArray(colorsList.size) { colorsList[it] }
        } else {
            FloatArray(0)
        }
        val uvs = FloatArray(uvsList.size) { uvsList[it] }
        val tangents = FloatArray(tangentsList.size) { tangentsList[it] }

        // 3. Generación del búfer de índices triangulados
        val indexList = ArrayList<Int>()

        // 3a. Casquete polar (Triangle Fan para cerrar el fondo del Anillo Mágico)
        if (hasPole && validRings.isNotEmpty()) {
            val n = validRings[0].vertexCount
            val r0In = inIndices[0]
            for (j in 0 until n) {
                val nextJ = (j + 1) % n
                // Winding order CCW para normal orientada hacia el exterior del fondo
                indexList.add(0)
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

        // 3c. Labio superior y pared interior si hasInteriorWall está activo (SM-083)
        if (hasInteriorWall) {
            val topIdx = validRings.size - 1
            val topOuter = outIndices[topIdx]
            val topInner = innerIndices[topIdx]
            val nTop = validRings[topIdx].vertexCount
            for (j in 0 until nTop) {
                val nextJ = (j + 1) % nTop
                val o0 = topOuter[j]
                val o1 = topOuter[nextJ]
                val i0 = topInner[j]
                val i1 = topInner[nextJ]

                // Normal hacia +Y en el labio superior
                indexList.add(o0)
                indexList.add(o1)
                indexList.add(i1)

                indexList.add(o0)
                indexList.add(i1)
                indexList.add(i0)
            }

            // Teselación de bandas de la pared interior (con normales hacia la cavidad interior)
            for (r in 0 until validRings.size - 1) {
                val ringLower = validRings[r]
                val ringUpper = validRings[r + 1]
                val lowerIn = innerIndices[r]
                val upperIn = innerIndices[r + 1]
                tessellateInteriorRingPair(ringUpper, ringLower, upperIn, lowerIn, indexList)
            }
        }

        val indicesArray = IntArray(indexList.size) { indexList[it] }
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
            vertexColors = colors,
            vertexUvs = uvs,
            vertexTangents = tangents
        )
    }

    /**
     * Tesela la malla emitiendo quads desacoplados por punto con UVs canónicas estrictamente confinadas
     * al tile del atlas [0.0..1.0] x [0.0..1.0], eliminando el ping-pong UV, el sangrado diagonal
     * y los artefactos de costura en números impares de puntos (SM-084).
     */
    private fun tessellateDecoupled(
        validRings: List<RingProfile>,
        includePolarCap: Boolean,
        defaultColorHex: String,
        wallThicknessMm: Float
    ): MeshGeometry {
        val positionsList = ArrayList<Float>()
        val normalsList = ArrayList<Float>()
        val colorsList = ArrayList<Float>()
        val uvsList = ArrayList<Float>()
        val tangentsList = ArrayList<Float>()
        val indexList = ArrayList<Int>()
        var vertexOffset = 0

        val hasPole = includePolarCap && validRings.isNotEmpty()

        // 1. Casquete polar con quads/triángulos desacoplados por puntada
        if (hasPole) {
            val ring0 = validRings.first()
            val poleY = (ring0.minY - (ring0.meanRadiusMm * 0.25)).coerceAtLeast(0.0).toFloat()
            val n0 = ring0.vertexCount

            for (j in 0 until n0) {
                val nextJ = (j + 1) % n0
                val v0 = ring0.vertices[j]
                val v1 = ring0.vertices[nextJ]
                val stitch = v0.stitchType
                val col0 = CrochetColorHelper.toRgbaFloats(v0.colorHex ?: defaultColorHex)
                val col1 = CrochetColorHelper.toRgbaFloats(v1.colorHex ?: defaultColorHex)

                // Vértice 0: Polo
                val idxPole = vertexOffset++
                positionsList.add(0.0f); positionsList.add(poleY); positionsList.add(0.0f)
                normalsList.add(0.0f); normalsList.add(-1.0f); normalsList.add(0.0f)
                colorsList.add(col0[0]); colorsList.add(col0[1]); colorsList.add(col0[2]); colorsList.add(col0[3])
                val uvPole = CrochetTextureGenerator.computeAtlasUv(stitch, 0.5f, 0.0f)
                uvsList.add(uvPole[0]); uvsList.add(uvPole[1])
                tangentsList.add(1.0f); tangentsList.add(0.0f); tangentsList.add(0.0f); tangentsList.add(1.0f)

                // Vértice 1: Siguiente perimetral (nextJ)
                val idxNext = vertexOffset++
                positionsList.add(v1.x.toFloat()); positionsList.add(v1.y.toFloat()); positionsList.add(v1.z.toFloat())
                val cosT1 = cos(v1.theta).toFloat()
                val sinT1 = -sin(v1.theta).toFloat()
                normalsList.add(cosT1 * 0.707f); normalsList.add(-0.707f); normalsList.add(sinT1 * 0.707f)
                colorsList.add(col1[0]); colorsList.add(col1[1]); colorsList.add(col1[2]); colorsList.add(col1[3])
                val uvNext = CrochetTextureGenerator.computeAtlasUv(stitch, 1.0f, 1.0f)
                uvsList.add(uvNext[0]); uvsList.add(uvNext[1])
                tangentsList.add(sinT1); tangentsList.add(0.0f); tangentsList.add(-cosT1); tangentsList.add(1.0f)

                // Vértice 2: Actual perimetral (j)
                val idxCur = vertexOffset++
                positionsList.add(v0.x.toFloat()); positionsList.add(v0.y.toFloat()); positionsList.add(v0.z.toFloat())
                val cosT0 = cos(v0.theta).toFloat()
                val sinT0 = -sin(v0.theta).toFloat()
                normalsList.add(cosT0 * 0.707f); normalsList.add(-0.707f); normalsList.add(sinT0 * 0.707f)
                colorsList.add(col0[0]); colorsList.add(col0[1]); colorsList.add(col0[2]); colorsList.add(col0[3])
                val uvCur = CrochetTextureGenerator.computeAtlasUv(stitch, 0.0f, 1.0f)
                uvsList.add(uvCur[0]); uvsList.add(uvCur[1])
                tangentsList.add(sinT0); tangentsList.add(0.0f); tangentsList.add(-cosT0); tangentsList.add(1.0f)

                indexList.add(idxPole)
                indexList.add(idxNext)
                indexList.add(idxCur)
            }
        }

        // Helper para normal analítica
        fun computeAnalyticalNormal(r: Int, v: RingVertex): FloatArray {
            val ring = validRings[r]
            val prevRing = if (r > 0) validRings[r - 1] else null
            val nextRing = if (r < validRings.size - 1) validRings[r + 1] else null
            val rPrev = prevRing?.meanRadiusMm ?: 0.0
            val rNext = nextRing?.meanRadiusMm ?: ring.meanRadiusMm
            val yPrev = prevRing?.minY ?: (validRings[0].minY - validRings[0].meanRadiusMm * 0.25).coerceAtLeast(0.0)
            val yNext = nextRing?.maxY ?: ring.maxY
            val dr = rNext - rPrev
            val dy = (yNext - yPrev).coerceAtLeast(0.05)
            val pLen = sqrt(dr * dr + dy * dy).coerceAtLeast(1e-4)
            val profCos = (dy / pLen).toFloat()
            val profSin = (-dr / pLen).toFloat()
            val cosT = cos(v.theta).toFloat()
            val sinT = -sin(v.theta).toFloat()
            return floatArrayOf(profCos * cosT, profSin, profCos * sinT)
        }

        // 2. Bandas entre anillos consecutivos con Quads desacoplados por punto
        for (r in 0 until validRings.size - 1) {
            val ring0 = validRings[r]
            val ring1 = validRings[r + 1]
            val n0 = ring0.vertexCount
            val n1 = ring1.vertexCount
            if (n0 == 0 || n1 == 0) continue

            if (n0 == n1) {
                // Caso 1:1 isomorfo
                for (j in 0 until n0) {
                    val nextJ = (j + 1) % n0
                    val v0 = ring0.vertices[j]
                    val v1 = ring0.vertices[nextJ]
                    val c0 = ring1.vertices[j]
                    val c1 = ring1.vertices[nextJ]
                    val stitch = v0.stitchType

                    val col0 = CrochetColorHelper.toRgbaFloats(v0.colorHex ?: defaultColorHex)
                    val col1 = CrochetColorHelper.toRgbaFloats(v1.colorHex ?: defaultColorHex)
                    val colC0 = CrochetColorHelper.toRgbaFloats(c0.colorHex ?: defaultColorHex)
                    val colC1 = CrochetColorHelper.toRgbaFloats(c1.colorHex ?: defaultColorHex)

                    val normV0 = computeAnalyticalNormal(r, v0)
                    val normV1 = computeAnalyticalNormal(r, v1)
                    val normC0 = computeAnalyticalNormal(r + 1, c0)
                    val normC1 = computeAnalyticalNormal(r + 1, c1)

                    // BL: (v0) localU = 0.0, localV = 0.0
                    val idxBL = vertexOffset++
                    positionsList.add(v0.x.toFloat()); positionsList.add(v0.y.toFloat()); positionsList.add(v0.z.toFloat())
                    normalsList.add(normV0[0]); normalsList.add(normV0[1]); normalsList.add(normV0[2])
                    colorsList.add(col0[0]); colorsList.add(col0[1]); colorsList.add(col0[2]); colorsList.add(col0[3])
                    val uvBL = CrochetTextureGenerator.computeAtlasUv(stitch, 0.0f, 0.0f)
                    uvsList.add(uvBL[0]); uvsList.add(uvBL[1])
                    tangentsList.add(-sin(v0.theta).toFloat()); tangentsList.add(0.0f); tangentsList.add(-cos(v0.theta).toFloat()); tangentsList.add(1.0f)

                    // BR: (v1) localU = 1.0, localV = 0.0
                    val idxBR = vertexOffset++
                    positionsList.add(v1.x.toFloat()); positionsList.add(v1.y.toFloat()); positionsList.add(v1.z.toFloat())
                    normalsList.add(normV1[0]); normalsList.add(normV1[1]); normalsList.add(normV1[2])
                    colorsList.add(col1[0]); colorsList.add(col1[1]); colorsList.add(col1[2]); colorsList.add(col1[3])
                    val uvBR = CrochetTextureGenerator.computeAtlasUv(stitch, 1.0f, 0.0f)
                    uvsList.add(uvBR[0]); uvsList.add(uvBR[1])
                    tangentsList.add(-sin(v1.theta).toFloat()); tangentsList.add(0.0f); tangentsList.add(-cos(v1.theta).toFloat()); tangentsList.add(1.0f)

                    // TR: (c1) localU = 1.0, localV = 1.0
                    val idxTR = vertexOffset++
                    positionsList.add(c1.x.toFloat()); positionsList.add(c1.y.toFloat()); positionsList.add(c1.z.toFloat())
                    normalsList.add(normC1[0]); normalsList.add(normC1[1]); normalsList.add(normC1[2])
                    colorsList.add(colC1[0]); colorsList.add(colC1[1]); colorsList.add(colC1[2]); colorsList.add(colC1[3])
                    val uvTR = CrochetTextureGenerator.computeAtlasUv(stitch, 1.0f, 1.0f)
                    uvsList.add(uvTR[0]); uvsList.add(uvTR[1])
                    tangentsList.add(-sin(c1.theta).toFloat()); tangentsList.add(0.0f); tangentsList.add(-cos(c1.theta).toFloat()); tangentsList.add(1.0f)

                    // TL: (c0) localU = 0.0, localV = 1.0
                    val idxTL = vertexOffset++
                    positionsList.add(c0.x.toFloat()); positionsList.add(c0.y.toFloat()); positionsList.add(c0.z.toFloat())
                    normalsList.add(normC0[0]); normalsList.add(normC0[1]); normalsList.add(normC0[2])
                    colorsList.add(colC0[0]); colorsList.add(colC0[1]); colorsList.add(colC0[2]); colorsList.add(colC0[3])
                    val uvTL = CrochetTextureGenerator.computeAtlasUv(stitch, 0.0f, 1.0f)
                    uvsList.add(uvTL[0]); uvsList.add(uvTL[1])
                    tangentsList.add(-sin(c0.theta).toFloat()); tangentsList.add(0.0f); tangentsList.add(-cos(c0.theta).toFloat()); tangentsList.add(1.0f)

                    // Triángulo 1 (BL -> BR -> TL)
                    indexList.add(idxBL); indexList.add(idxBR); indexList.add(idxTL)
                    // Triángulo 2 (BR -> TR -> TL)
                    indexList.add(idxBR); indexList.add(idxTR); indexList.add(idxTL)
                }
            } else {
                // Caso asimétrico adaptativo general (aumentos / disminuciones)
                var i = 0
                var j = 0
                while (i < n0 || j < n1) {
                    if (i == n0) {
                        val p = ring0.vertices[i % n0]
                        val cCurrent = ring1.vertices[j % n1]
                        val cNext = ring1.vertices[(j + 1) % n1]
                        val stitch = cCurrent.stitchType
                        val colP = CrochetColorHelper.toRgbaFloats(p.colorHex ?: defaultColorHex)
                        val colC = CrochetColorHelper.toRgbaFloats(cCurrent.colorHex ?: defaultColorHex)
                        val colCN = CrochetColorHelper.toRgbaFloats(cNext.colorHex ?: defaultColorHex)
                        val normP = computeAnalyticalNormal(r, p)
                        val normC = computeAnalyticalNormal(r + 1, cCurrent)
                        val normCN = computeAnalyticalNormal(r + 1, cNext)

                        val iP = vertexOffset++
                        positionsList.add(p.x.toFloat()); positionsList.add(p.y.toFloat()); positionsList.add(p.z.toFloat())
                        normalsList.add(normP[0]); normalsList.add(normP[1]); normalsList.add(normP[2])
                        colorsList.add(colP[0]); colorsList.add(colP[1]); colorsList.add(colP[2]); colorsList.add(colP[3])
                        val uvP = CrochetTextureGenerator.computeAtlasUv(stitch, 0.5f, 0.0f)
                        uvsList.add(uvP[0]); uvsList.add(uvP[1])
                        tangentsList.add(-sin(p.theta).toFloat()); tangentsList.add(0f); tangentsList.add(-cos(p.theta).toFloat()); tangentsList.add(1f)

                        val iCN = vertexOffset++
                        positionsList.add(cNext.x.toFloat()); positionsList.add(cNext.y.toFloat()); positionsList.add(cNext.z.toFloat())
                        normalsList.add(normCN[0]); normalsList.add(normCN[1]); normalsList.add(normCN[2])
                        colorsList.add(colCN[0]); colorsList.add(colCN[1]); colorsList.add(colCN[2]); colorsList.add(colCN[3])
                        val uvCN = CrochetTextureGenerator.computeAtlasUv(stitch, 1.0f, 1.0f)
                        uvsList.add(uvCN[0]); uvsList.add(uvCN[1])
                        tangentsList.add(-sin(cNext.theta).toFloat()); tangentsList.add(0f); tangentsList.add(-cos(cNext.theta).toFloat()); tangentsList.add(1f)

                        val iC = vertexOffset++
                        positionsList.add(cCurrent.x.toFloat()); positionsList.add(cCurrent.y.toFloat()); positionsList.add(cCurrent.z.toFloat())
                        normalsList.add(normC[0]); normalsList.add(normC[1]); normalsList.add(normC[2])
                        colorsList.add(colC[0]); colorsList.add(colC[1]); colorsList.add(colC[2]); colorsList.add(colC[3])
                        val uvC = CrochetTextureGenerator.computeAtlasUv(stitch, 0.0f, 1.0f)
                        uvsList.add(uvC[0]); uvsList.add(uvC[1])
                        tangentsList.add(-sin(cCurrent.theta).toFloat()); tangentsList.add(0f); tangentsList.add(-cos(cCurrent.theta).toFloat()); tangentsList.add(1f)

                        indexList.add(iP); indexList.add(iCN); indexList.add(iC)
                        j++
                    } else if (j == n1) {
                        val pCurrent = ring0.vertices[i % n0]
                        val pNext = ring0.vertices[(i + 1) % n0]
                        val c = ring1.vertices[j % n1]
                        val stitch = pCurrent.stitchType
                        val colP = CrochetColorHelper.toRgbaFloats(pCurrent.colorHex ?: defaultColorHex)
                        val colPN = CrochetColorHelper.toRgbaFloats(pNext.colorHex ?: defaultColorHex)
                        val colC = CrochetColorHelper.toRgbaFloats(c.colorHex ?: defaultColorHex)
                        val normP = computeAnalyticalNormal(r, pCurrent)
                        val normPN = computeAnalyticalNormal(r, pNext)
                        val normC = computeAnalyticalNormal(r + 1, c)

                        val iP = vertexOffset++
                        positionsList.add(pCurrent.x.toFloat()); positionsList.add(pCurrent.y.toFloat()); positionsList.add(pCurrent.z.toFloat())
                        normalsList.add(normP[0]); normalsList.add(normP[1]); normalsList.add(normP[2])
                        colorsList.add(colP[0]); colorsList.add(colP[1]); colorsList.add(colP[2]); colorsList.add(colP[3])
                        val uvP = CrochetTextureGenerator.computeAtlasUv(stitch, 0.0f, 0.0f)
                        uvsList.add(uvP[0]); uvsList.add(uvP[1])
                        tangentsList.add(-sin(pCurrent.theta).toFloat()); tangentsList.add(0f); tangentsList.add(-cos(pCurrent.theta).toFloat()); tangentsList.add(1f)

                        val iPN = vertexOffset++
                        positionsList.add(pNext.x.toFloat()); positionsList.add(pNext.y.toFloat()); positionsList.add(pNext.z.toFloat())
                        normalsList.add(normPN[0]); normalsList.add(normPN[1]); normalsList.add(normPN[2])
                        colorsList.add(colPN[0]); colorsList.add(colPN[1]); colorsList.add(colPN[2]); colorsList.add(colPN[3])
                        val uvPN = CrochetTextureGenerator.computeAtlasUv(stitch, 1.0f, 0.0f)
                        uvsList.add(uvPN[0]); uvsList.add(uvPN[1])
                        tangentsList.add(-sin(pNext.theta).toFloat()); tangentsList.add(0f); tangentsList.add(-cos(pNext.theta).toFloat()); tangentsList.add(1f)

                        val iC = vertexOffset++
                        positionsList.add(c.x.toFloat()); positionsList.add(c.y.toFloat()); positionsList.add(c.z.toFloat())
                        normalsList.add(normC[0]); normalsList.add(normC[1]); normalsList.add(normC[2])
                        colorsList.add(colC[0]); colorsList.add(colC[1]); colorsList.add(colC[2]); colorsList.add(colC[3])
                        val uvC = CrochetTextureGenerator.computeAtlasUv(stitch, 0.5f, 1.0f)
                        uvsList.add(uvC[0]); uvsList.add(uvC[1])
                        tangentsList.add(-sin(c.theta).toFloat()); tangentsList.add(0f); tangentsList.add(-cos(c.theta).toFloat()); tangentsList.add(1f)

                        indexList.add(iP); indexList.add(iPN); indexList.add(iC)
                        i++
                    } else {
                        val nextAngle0 = (i + 1).toDouble() / n0
                        val nextAngle1 = (j + 1).toDouble() / n1
                        if (nextAngle0 <= nextAngle1) {
                            val pCurrent = ring0.vertices[i % n0]
                            val pNext = ring0.vertices[(i + 1) % n0]
                            val c = ring1.vertices[j % n1]
                            val stitch = pCurrent.stitchType
                            val colP = CrochetColorHelper.toRgbaFloats(pCurrent.colorHex ?: defaultColorHex)
                            val colPN = CrochetColorHelper.toRgbaFloats(pNext.colorHex ?: defaultColorHex)
                            val colC = CrochetColorHelper.toRgbaFloats(c.colorHex ?: defaultColorHex)
                            val normP = computeAnalyticalNormal(r, pCurrent)
                            val normPN = computeAnalyticalNormal(r, pNext)
                            val normC = computeAnalyticalNormal(r + 1, c)

                            val iP = vertexOffset++
                            positionsList.add(pCurrent.x.toFloat()); positionsList.add(pCurrent.y.toFloat()); positionsList.add(pCurrent.z.toFloat())
                            normalsList.add(normP[0]); normalsList.add(normP[1]); normalsList.add(normP[2])
                            colorsList.add(colP[0]); colorsList.add(colP[1]); colorsList.add(colP[2]); colorsList.add(colP[3])
                            val uvP = CrochetTextureGenerator.computeAtlasUv(stitch, 0.0f, 0.0f)
                            uvsList.add(uvP[0]); uvsList.add(uvP[1])
                            tangentsList.add(-sin(pCurrent.theta).toFloat()); tangentsList.add(0f); tangentsList.add(-cos(pCurrent.theta).toFloat()); tangentsList.add(1f)

                            val iPN = vertexOffset++
                            positionsList.add(pNext.x.toFloat()); positionsList.add(pNext.y.toFloat()); positionsList.add(pNext.z.toFloat())
                            normalsList.add(normPN[0]); normalsList.add(normPN[1]); normalsList.add(normPN[2])
                            colorsList.add(colPN[0]); colorsList.add(colPN[1]); colorsList.add(colPN[2]); colorsList.add(colPN[3])
                            val uvPN = CrochetTextureGenerator.computeAtlasUv(stitch, 1.0f, 0.0f)
                            uvsList.add(uvPN[0]); uvsList.add(uvPN[1])
                            tangentsList.add(-sin(pNext.theta).toFloat()); tangentsList.add(0f); tangentsList.add(-cos(pNext.theta).toFloat()); tangentsList.add(1f)

                            val iC = vertexOffset++
                            positionsList.add(c.x.toFloat()); positionsList.add(c.y.toFloat()); positionsList.add(c.z.toFloat())
                            normalsList.add(normC[0]); normalsList.add(normC[1]); normalsList.add(normC[2])
                            colorsList.add(colC[0]); colorsList.add(colC[1]); colorsList.add(colC[2]); colorsList.add(colC[3])
                            val uvC = CrochetTextureGenerator.computeAtlasUv(stitch, 0.5f, 1.0f)
                            uvsList.add(uvC[0]); uvsList.add(uvC[1])
                            tangentsList.add(-sin(c.theta).toFloat()); tangentsList.add(0f); tangentsList.add(-cos(c.theta).toFloat()); tangentsList.add(1f)

                            indexList.add(iP); indexList.add(iPN); indexList.add(iC)
                            i++
                        } else {
                            val p = ring0.vertices[i % n0]
                            val cCurrent = ring1.vertices[j % n1]
                            val cNext = ring1.vertices[(j + 1) % n1]
                            val stitch = cCurrent.stitchType
                            val colP = CrochetColorHelper.toRgbaFloats(p.colorHex ?: defaultColorHex)
                            val colC = CrochetColorHelper.toRgbaFloats(cCurrent.colorHex ?: defaultColorHex)
                            val colCN = CrochetColorHelper.toRgbaFloats(cNext.colorHex ?: defaultColorHex)
                            val normP = computeAnalyticalNormal(r, p)
                            val normC = computeAnalyticalNormal(r + 1, cCurrent)
                            val normCN = computeAnalyticalNormal(r + 1, cNext)

                            val iP = vertexOffset++
                            positionsList.add(p.x.toFloat()); positionsList.add(p.y.toFloat()); positionsList.add(p.z.toFloat())
                            normalsList.add(normP[0]); normalsList.add(normP[1]); normalsList.add(normP[2])
                            colorsList.add(colP[0]); colorsList.add(colP[1]); colorsList.add(colP[2]); colorsList.add(colP[3])
                            val uvP = CrochetTextureGenerator.computeAtlasUv(stitch, 0.5f, 0.0f)
                            uvsList.add(uvP[0]); uvsList.add(uvP[1])
                            tangentsList.add(-sin(p.theta).toFloat()); tangentsList.add(0f); tangentsList.add(-cos(p.theta).toFloat()); tangentsList.add(1f)

                            val iCN = vertexOffset++
                            positionsList.add(cNext.x.toFloat()); positionsList.add(cNext.y.toFloat()); positionsList.add(cNext.z.toFloat())
                            normalsList.add(normCN[0]); normalsList.add(normCN[1]); normalsList.add(normCN[2])
                            colorsList.add(colCN[0]); colorsList.add(colCN[1]); colorsList.add(colCN[2]); colorsList.add(colCN[3])
                            val uvCN = CrochetTextureGenerator.computeAtlasUv(stitch, 1.0f, 1.0f)
                            uvsList.add(uvCN[0]); uvsList.add(uvCN[1])
                            tangentsList.add(-sin(cNext.theta).toFloat()); tangentsList.add(0f); tangentsList.add(-cos(cNext.theta).toFloat()); tangentsList.add(1f)

                            val iC = vertexOffset++
                            positionsList.add(cCurrent.x.toFloat()); positionsList.add(cCurrent.y.toFloat()); positionsList.add(cCurrent.z.toFloat())
                            normalsList.add(normC[0]); normalsList.add(normC[1]); normalsList.add(normC[2])
                            colorsList.add(colC[0]); colorsList.add(colC[1]); colorsList.add(colC[2]); colorsList.add(colC[3])
                            val uvC = CrochetTextureGenerator.computeAtlasUv(stitch, 0.0f, 1.0f)
                            uvsList.add(uvC[0]); uvsList.add(uvC[1])
                            tangentsList.add(-sin(cCurrent.theta).toFloat()); tangentsList.add(0f); tangentsList.add(-cos(cCurrent.theta).toFloat()); tangentsList.add(1f)

                            indexList.add(iP); indexList.add(iCN); indexList.add(iC)
                            j++
                        }
                    }
                }
            }
        }

        val totalVertices = vertexOffset
        val indicesArray = IntArray(indexList.size) { indexList[it] }
        val triangleCount = indicesArray.size / 3

        val positions = FloatArray(positionsList.size) { positionsList[it] }
        val normals = FloatArray(normalsList.size) { normalsList[it] }
        val colors = FloatArray(colorsList.size) { colorsList[it] }
        val uvs = FloatArray(uvsList.size) { uvsList[it] }
        val tangents = FloatArray(tangentsList.size) { tangentsList[it] }

        return MeshGeometry(
            vertexPositions = positions,
            indices = indicesArray,
            vertexCount = totalVertices,
            triangleCount = triangleCount,
            vertexNormals = normals,
            vertexColors = colors,
            vertexUvs = uvs,
            vertexTangents = tangents
        )
    }

    /**
     * Calcula normales de superficie combinando el perfil analítico de revolución (para eliminar
     * el efecto de facetado angular y de papel arrugado) con las normales de caras triangulares acumuladas.
     * Garantiza magnitud unitaria ||n|| = 1.0 +/- 0.001 en todo el búfer.
     */
    private fun calculateSmoothAndAreaWeightedNormals(
        positions: FloatArray,
        indices: IntArray,
        vertexCount: Int,
        triangleCount: Int,
        validRings: List<RingProfile>,
        hasPole: Boolean,
        inIndices: Array<IntArray>,
        outIndices: Array<IntArray>
    ): FloatArray {
        val faceNormalAccum = FloatArray(vertexCount * 3)

        for (t in 0 until triangleCount) {
            val i0 = indices[t * 3]
            val i1 = indices[t * 3 + 1]
            val i2 = indices[t * 3 + 2]

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

        // 1. Normal del vértice polar (apunta hacia el fondo exterior del casquete en -Y)
        if (hasPole && vertexCount > 0) {
            normals[0] = 0.0f
            normals[1] = -1.0f
            normals[2] = 0.0f
        }

        // 2. Cálculo para cada anillo
        for (r in validRings.indices) {
            val ring = validRings[r]
            val prevRing = if (r > 0) validRings[r - 1] else null
            val nextRing = if (r < validRings.size - 1) validRings[r + 1] else null

            val rPrev = prevRing?.meanRadiusMm ?: 0.0
            val rNext = nextRing?.meanRadiusMm ?: ring.meanRadiusMm
            val yPrev = prevRing?.minY ?: (validRings[0].minY - validRings[0].meanRadiusMm * 0.25).coerceAtLeast(0.0)
            val yNext = nextRing?.maxY ?: ring.maxY

            val dr = rNext - rPrev
            val dy = (yNext - yPrev).coerceAtLeast(0.05)
            val pLen = sqrt(dr * dr + dy * dy).coerceAtLeast(1e-4)
            val profCos = (dy / pLen).toFloat()
            val profSin = (-dr / pLen).toFloat()

            for (j in ring.vertices.indices) {
                val v = ring.vertices[j]
                val cosT = cos(v.theta).toFloat()
                val sinT = -sin(v.theta).toFloat()

                val revNx = profCos * cosT
                val revNy = profSin
                val revNz = profCos * sinT

                val inIdx = inIndices[r][j]
                val outIdx = outIndices[r][j]
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

                // Vértice duplicado para BLO o cambio cromático
                if (outIdx != inIdx) {
                    if (isBlo) {
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
                    } else {
                        // Frontera cromática: normal suave continua sin arista viva
                        normals[outIdx * 3] = normals[inIdx * 3]
                        normals[outIdx * 3 + 1] = normals[inIdx * 3 + 1]
                        normals[outIdx * 3 + 2] = normals[inIdx * 3 + 2]
                    }
                }
            }
        }

        // Si hubiera algún vértice no indexado por los anillos exteriores (ej. pared interior o labio),
        // normalizar directamente la normal acumulada de caras o asignar unitaria
        for (v in 0 until vertexCount) {
            val idx = v * 3
            val nx = normals[idx]
            val ny = normals[idx + 1]
            val nz = normals[idx + 2]
            val len = sqrt(nx * nx + ny * ny + nz * nz)
            if (len < 1e-6f) {
                val fnx = faceNormalAccum[idx]
                val fny = faceNormalAccum[idx + 1]
                val fnz = faceNormalAccum[idx + 2]
                val flen = sqrt(fnx * fnx + fny * fny + fnz * fnz)
                if (flen > 1e-6f) {
                    normals[idx] = fnx / flen
                    normals[idx + 1] = fny / flen
                    normals[idx + 2] = fnz / flen
                } else {
                    normals[idx] = 0f
                    normals[idx + 1] = 1f
                    normals[idx + 2] = 0f
                }
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
        ring0Indices: IntArray,
        ring1Indices: IntArray,
        outIndices: ArrayList<Int>
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

    /**
     * Tesela la pared interior cilíndrica entre dos anillos adyacentes con normales orientadas hacia la cavidad interior.
     */
    private fun tessellateInteriorRingPair(
        ringUpper: RingProfile,
        ringLower: RingProfile,
        ringUpperIndices: IntArray,
        ringLowerIndices: IntArray,
        outIndices: ArrayList<Int>
    ) {
        val nUpper = ringUpper.vertexCount
        val nLower = ringLower.vertexCount
        if (nUpper == 0 || nLower == 0) return

        if (nUpper == nLower) {
            for (j in 0 until nUpper) {
                val nextJ = (j + 1) % nUpper
                val u0 = ringUpperIndices[j]
                val u1 = ringUpperIndices[nextJ]
                val l0 = ringLowerIndices[j]
                val l1 = ringLowerIndices[nextJ]

                // Winding CCW para que la normal apunte hacia la cavidad interior
                outIndices.add(u0)
                outIndices.add(l0)
                outIndices.add(u1)

                outIndices.add(u1)
                outIndices.add(l0)
                outIndices.add(l1)
            }
            return
        }

        var i = 0
        var j = 0

        while (i < nUpper || j < nLower) {
            if (i == nUpper) {
                val u = ringUpperIndices[i % nUpper]
                val lCurrent = ringLowerIndices[j % nLower]
                val lNext = ringLowerIndices[(j + 1) % nLower]
                outIndices.add(u)
                outIndices.add(lCurrent)
                outIndices.add(lNext)
                j++
            } else if (j == nLower) {
                val uCurrent = ringUpperIndices[i % nUpper]
                val uNext = ringUpperIndices[(i + 1) % nUpper]
                val l = ringLowerIndices[j % nLower]
                outIndices.add(uCurrent)
                outIndices.add(l)
                outIndices.add(uNext)
                i++
            } else {
                val nextAngle0 = (i + 1).toDouble() / nUpper
                val nextAngle1 = (j + 1).toDouble() / nLower

                if (nextAngle0 <= nextAngle1) {
                    val uCurrent = ringUpperIndices[i % nUpper]
                    val uNext = ringUpperIndices[(i + 1) % nUpper]
                    val l = ringLowerIndices[j % nLower]
                    outIndices.add(uCurrent)
                    outIndices.add(l)
                    outIndices.add(uNext)
                    i++
                } else {
                    val u = ringUpperIndices[i % nUpper]
                    val lCurrent = ringLowerIndices[j % nLower]
                    val lNext = ringLowerIndices[(j + 1) % nLower]
                    outIndices.add(u)
                    outIndices.add(lCurrent)
                    outIndices.add(lNext)
                    j++
                }
            }
        }
    }
}

