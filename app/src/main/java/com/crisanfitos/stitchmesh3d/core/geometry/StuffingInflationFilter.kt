package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import kotlin.math.sqrt

/**
 * Parámetros de simulación física del relleno de algodón sintético en piezas de amigurumi.
 *
 * @property inflationFactor Coeficiente de expansión hidrostática radial hacia afuera (0.0 = sin inflado, 0.08 = estándar, 0.15 = peluche muy relleno).
 * @property smoothingIterations Número de pasadas del suavizador laplaciano (1..3).
 * @property relaxationFactor Factor de relajación laplaciana (0.1..0.5).
 * @property preserveBloEdges Si es true, preserva la continuidad geométrica de aristas vivas en puntadas BLO sin abrir fisuras en la malla.
 */
data class StuffingInflationConfig(
    val inflationFactor: Float = 0.08f,
    val smoothingIterations: Int = 2,
    val relaxationFactor: Float = 0.35f,
    val preserveBloEdges: Boolean = true
)

/**
 * Filtro de simulación de inflado volumétrico por relleno de algodón y suavizado de curvatura para amigurumis.
 *
 * En la vida real, el algodón sintético ejerce una presión hacia afuera uniforme contra las paredes de la pieza de crochet,
 * redondeando las transiciones angulares entre vueltas y transformando poliedros facetados en superficies orgánicas y continuas.
 *
 * Implementa TRD §3.1, §3.2, PRD RF-3.2 y SM-067:
 * - Suavizado Laplaciano tangencial libre de contracción (zero shrinkage) para atenuar aristas poligonales duras.
 * - Expansión normal hidrostática posterior para topologías [PartTopologyType.CLOSED_FILLED] y [PartTopologyType.SEMI_CLOSED_TUBE].
 * - Exclusión estricta de piezas planas [PartTopologyType.FLAT_PANEL].
 * - Sincronización espacial de vértices duplicados en costuras BLO ([com.crisanfitos.stitchmesh3d.core.engine.model.TopologyFlag.BLO])
 *   para garantizar hermeticidad (watertight mesh) preservando el desacople de normales (arista viva).
 * - Recálculo de normales de superficie suaves y estrictamente unitarias (||n|| = 1.0 ± 0.001) para sombreado PBR en Filament.
 * - Rendimiento < 25 ms en mallas de alta densidad en Dispatchers.Default.
 */
object StuffingInflationFilter {

    /**
     * Aplica la simulación de inflado y suavizado de curvatura a una geometría indexada.
     *
     * @param mesh Malla 3D de entrada con posiciones, índices y normales.
     * @param topologyType Tipología estructural de la pieza (rellena, tubular o plana).
     * @param config Configuración del filtro (factor de inflado, iteraciones laplacianas).
     * @return Nueva [MeshGeometry] inflada y suavizada, o la original si no requiere inflado.
     */
    fun applyInflation(
        mesh: MeshGeometry,
        topologyType: PartTopologyType,
        config: StuffingInflationConfig = StuffingInflationConfig()
    ): MeshGeometry {
        // Piezas planas no llevan relleno de algodón; mallas vacías o degeneradas se retornan intactas.
        if (topologyType == PartTopologyType.FLAT_PANEL ||
            mesh.vertexCount <= 3 ||
            mesh.indices.isEmpty() ||
            (config.inflationFactor <= 0f && config.smoothingIterations <= 0)
        ) {
            return mesh
        }

        val vertexCount = mesh.vertexCount
        val curPos = mesh.vertexPositions.copyOf()
        val indices = mesh.indices

        // 1. Detección de vértices coincidentes (ej. duplicados de TopologyFlag.BLO para hermeticidad de malla)
        val coincidentPairs = if (config.preserveBloEdges) {
            findCoincidentVertexPairs(curPos, vertexCount)
        } else {
            emptyList()
        }

        // 2. Construcción de grafo de adyacencia de vértices a partir de los triángulos
        val neighbors = buildAdjacencyList(indices, vertexCount)

        // 3. Suavizado Laplaciano tangencial libre de contracción para atenuar cantos y facetas poligonales
        if (config.smoothingIterations > 0 && config.relaxationFactor > 0f) {
            val tempPos = FloatArray(vertexCount * 3)

            for (iter in 0 until config.smoothingIterations) {
                for (i in 0 until vertexCount) {
                    val nbrs = neighbors[i]
                    val idx = i * 3

                    if (nbrs.isEmpty()) {
                        tempPos[idx] = curPos[idx]
                        tempPos[idx + 1] = curPos[idx + 1]
                        tempPos[idx + 2] = curPos[idx + 2]
                        continue
                    }

                    var avgX = 0f
                    var avgY = 0f
                    var avgZ = 0f
                    for (n in nbrs) {
                        val nIdx = n * 3
                        avgX += curPos[nIdx]
                        avgY += curPos[nIdx + 1]
                        avgZ += curPos[nIdx + 2]
                    }
                    val invCount = 1f / nbrs.size
                    avgX *= invCount
                    avgY *= invCount
                    avgZ *= invCount

                    val lapX = avgX - curPos[idx]
                    val lapY = avgY - curPos[idx + 1]
                    val lapZ = avgZ - curPos[idx + 2]

                    // Proyección tangencial: eliminar el componente normal hacia adentro para evitar encogimiento
                    val nx = mesh.vertexNormals.getOrElse(idx) { 0f }
                    val ny = mesh.vertexNormals.getOrElse(idx + 1) { 0f }
                    val nz = mesh.vertexNormals.getOrElse(idx + 2) { 0f }
                    val nLen = sqrt(nx * nx + ny * ny + nz * nz)

                    val tLapX: Float
                    val tLapY: Float
                    val tLapZ: Float

                    if (nLen > 1e-4f) {
                        val unx = nx / nLen
                        val uny = ny / nLen
                        val unz = nz / nLen
                        val normalDot = lapX * unx + lapY * uny + lapZ * unz
                        // Si el vector laplaciano apunta hacia el interior (contracción), suprimir el colapso normal
                        val inwardPull = if (normalDot < 0f) normalDot else 0f
                        tLapX = lapX - inwardPull * unx
                        tLapY = lapY - inwardPull * uny
                        tLapZ = lapZ - inwardPull * unz
                    } else {
                        tLapX = lapX
                        tLapY = lapY
                        tLapZ = lapZ
                    }

                    tempPos[idx] = curPos[idx] + config.relaxationFactor * tLapX
                    tempPos[idx + 1] = curPos[idx + 1] + config.relaxationFactor * tLapY
                    tempPos[idx + 2] = curPos[idx + 2] + config.relaxationFactor * tLapZ
                }

                // Sincronizar vértices coincidentes en cada iteración para evitar fisuras o aberturas
                synchronizeCoincidentVertices(tempPos, coincidentPairs)

                // Copiar el estado suavizado a curPos
                System.arraycopy(tempPos, 0, curPos, 0, tempPos.size)
            }
        }

        // 4. Normales de superficie de la malla suavizada
        val smoothedNormals = computeSmoothNormals(curPos, indices, vertexCount)

        // 5. Cálculo del centro axial en XY y desplazamiento radial por presión de inflado
        if (config.inflationFactor > 0f) {
            var sumX = 0.0
            var sumY = 0.0
            for (i in 0 until vertexCount) {
                sumX += curPos[i * 3]
                sumY += curPos[i * 3 + 1]
            }
            val axisCenterX = (sumX / vertexCount).toFloat()
            val axisCenterY = (sumY / vertexCount).toFloat()

            val inflationScale = if (topologyType == PartTopologyType.CLOSED_FILLED) {
                config.inflationFactor
            } else {
                config.inflationFactor * 0.70f // Tubos semi-abiertos tienen menor retención de presión
            }

            for (i in 0 until vertexCount) {
                val idx = i * 3
                val vx = curPos[idx]
                val vy = curPos[idx + 1]

                val dx = vx - axisCenterX
                val dy = vy - axisCenterY
                val localRadius = sqrt(dx * dx + dy * dy).coerceAtLeast(0.5f)

                // La presión de empuje actúa normal a la superficie suavizada
                val pushDistance = inflationScale * localRadius

                val nx = smoothedNormals[idx]
                val ny = smoothedNormals[idx + 1]
                val nz = smoothedNormals[idx + 2]

                curPos[idx] += nx * pushDistance
                curPos[idx + 1] += ny * pushDistance
                curPos[idx + 2] += nz * pushDistance
            }

            // Sincronizar vértices coincidentes tras el inflado
            synchronizeCoincidentVertices(curPos, coincidentPairs)
        }

        // 6. Recálculo final de normales de superficie suaves y estrictamente unitarias (||n|| = 1.0)
        val finalNormals = computeSmoothNormals(curPos, indices, vertexCount)

        return MeshGeometry(
            vertexPositions = curPos,
            indices = indices,
            vertexCount = vertexCount,
            triangleCount = mesh.triangleCount,
            vertexNormals = finalNormals
        )
    }

    /**
     * Identifica pares de índices de vértices que comparten idéntica posición espacial inicial (aristas duplicadas BLO).
     */
    private fun findCoincidentVertexPairs(positions: FloatArray, vertexCount: Int): List<Pair<Int, Int>> {
        val pairs = ArrayList<Pair<Int, Int>>()
        // Tabla hash espacial basada en coordenadas cuantizadas a 0.05 mm
        val spatialMap = HashMap<Long, ArrayList<Int>>(vertexCount)

        for (i in 0 until vertexCount) {
            val idx = i * 3
            val qx = (positions[idx] * 20f).toInt().toLong()
            val qy = (positions[idx + 1] * 20f).toInt().toLong()
            val qz = (positions[idx + 2] * 20f).toInt().toLong()

            val key = (qx * 73856093L) xor (qy * 19349663L) xor (qz * 83492791L)
            val bucket = spatialMap.getOrPut(key) { ArrayList(2) }

            for (other in bucket) {
                val oIdx = other * 3
                val dx = positions[idx] - positions[oIdx]
                val dy = positions[idx + 1] - positions[oIdx + 1]
                val dz = positions[idx + 2] - positions[oIdx + 2]
                val distSq = dx * dx + dy * dy + dz * dz
                if (distSq < 1e-4f) { // Coincidentes dentro de 0.01 mm
                    pairs.add(Pair(other, i))
                }
            }
            bucket.add(i)
        }
        return pairs
    }

    /**
     * Sincroniza la posición de vértices coincidentes promediando sus coordenadas para mantener la malla hermética (watertight).
     */
    private fun synchronizeCoincidentVertices(positions: FloatArray, pairs: List<Pair<Int, Int>>) {
        for (pair in pairs) {
            val idxA = pair.first * 3
            val idxB = pair.second * 3

            val avgX = (positions[idxA] + positions[idxB]) * 0.5f
            val avgY = (positions[idxA + 1] + positions[idxB + 1]) * 0.5f
            val avgZ = (positions[idxA + 2] + positions[idxB + 2]) * 0.5f

            positions[idxA] = avgX
            positions[idxB] = avgX
            positions[idxA + 1] = avgY
            positions[idxB + 1] = avgY
            positions[idxA + 2] = avgZ
            positions[idxB + 2] = avgZ
        }
    }

    /**
     * Construye una lista de vecindad para cada vértice a partir del búfer de índices.
     */
    private fun buildAdjacencyList(indices: ShortArray, vertexCount: Int): Array<IntArray> {
        val sets = Array(vertexCount) { HashSet<Int>(6) }
        val triangleCount = indices.size / 3

        for (t in 0 until triangleCount) {
            val i0 = indices[t * 3].toInt() and 0xFFFF
            val i1 = indices[t * 3 + 1].toInt() and 0xFFFF
            val i2 = indices[t * 3 + 2].toInt() and 0xFFFF

            if (i0 < vertexCount && i1 < vertexCount && i2 < vertexCount) {
                sets[i0].add(i1)
                sets[i0].add(i2)
                sets[i1].add(i0)
                sets[i1].add(i2)
                sets[i2].add(i0)
                sets[i2].add(i1)
            }
        }

        return Array(vertexCount) { idx -> sets[idx].toIntArray() }
    }

    /**
     * Recalcula las normales de vértice por acumulación de normales de cara ponderadas por área.
     */
    private fun computeSmoothNormals(
        positions: FloatArray,
        indices: ShortArray,
        vertexCount: Int
    ): FloatArray {
        val normalAccum = FloatArray(vertexCount * 3)
        val triangleCount = indices.size / 3

        for (t in 0 until triangleCount) {
            val i0 = indices[t * 3].toInt() and 0xFFFF
            val i1 = indices[t * 3 + 1].toInt() and 0xFFFF
            val i2 = indices[t * 3 + 2].toInt() and 0xFFFF

            if (i0 >= vertexCount || i1 >= vertexCount || i2 >= vertexCount) continue

            val idx0 = i0 * 3
            val idx1 = i1 * 3
            val idx2 = i2 * 3

            val x0 = positions[idx0]
            val y0 = positions[idx0 + 1]
            val z0 = positions[idx0 + 2]

            val uX = positions[idx1] - x0
            val uY = positions[idx1 + 1] - y0
            val uZ = positions[idx1 + 2] - z0

            val vX = positions[idx2] - x0
            val vY = positions[idx2 + 1] - y0
            val vZ = positions[idx2 + 2] - z0

            // Producto vectorial u × v
            val fnX = uY * vZ - uZ * vY
            val fnY = uZ * vX - uX * vZ
            val fnZ = uX * vY - uY * vX

            // Acumular normal de cara en los 3 vértices
            normalAccum[idx0] += fnX
            normalAccum[idx0 + 1] += fnY
            normalAccum[idx0 + 2] += fnZ

            normalAccum[idx1] += fnX
            normalAccum[idx1 + 1] += fnY
            normalAccum[idx1 + 2] += fnZ

            normalAccum[idx2] += fnX
            normalAccum[idx2 + 1] += fnY
            normalAccum[idx2 + 2] += fnZ
        }

        val resultNormals = FloatArray(vertexCount * 3)
        for (i in 0 until vertexCount) {
            val idx = i * 3
            val nx = normalAccum[idx]
            val ny = normalAccum[idx + 1]
            val nz = normalAccum[idx + 2]
            val len = sqrt(nx * nx + ny * ny + nz * nz)

            if (len > 1e-6f) {
                val invLen = 1f / len
                resultNormals[idx] = nx * invLen
                resultNormals[idx + 1] = ny * invLen
                resultNormals[idx + 2] = nz * invLen
            } else {
                resultNormals[idx] = 0f
                resultNormals[idx + 1] = 0f
                resultNormals[idx + 2] = 1f
            }
        }

        return resultNormals
    }
}
