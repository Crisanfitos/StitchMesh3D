package com.crisanfitos.stitchmesh3d.core.geometry

import kotlin.math.abs

/**
 * Perfil geométrico de una vuelta o anillo de crochet en el espacio tridimensional.
 *
 * @property roundIndex Índice de la vuelta (1..N).
 * @property vertices Lista de vértices perimetrales en orden azimutal continuo.
 * @property circumferenceMm Perímetro circunferencial total de la vuelta en milímetros.
 * @property meanRadiusMm Radio promedio del anillo en milímetros.
 * @property minY Cota vertical mínima alcanzada en la vuelta (Y-up glTF standard).
 * @property maxY Cota vertical máxima alcanzada en la vuelta (Y-up glTF standard).
 * @property minZ Alias de compatibilidad retroactiva para minY.
 * @property maxZ Alias de compatibilidad retroactiva para maxY.
 */
data class RingProfile(
    val roundIndex: Int,
    val vertices: List<RingVertex>,
    val circumferenceMm: Double,
    val meanRadiusMm: Double,
    val minY: Double = 0.0,
    val maxY: Double = 0.0,
    val minZ: Double = minY,
    val maxZ: Double = maxY
) {
    /** Constructor de compatibilidad para constructores con minZ y maxZ como argumentos posicionales o nombrados */
    constructor(
        roundIndex: Int,
        vertices: List<RingVertex>,
        circumferenceMm: Double,
        meanRadiusMm: Double,
        minZ: Double,
        maxZ: Double
    ) : this(
        roundIndex = roundIndex,
        vertices = vertices,
        circumferenceMm = circumferenceMm,
        meanRadiusMm = meanRadiusMm,
        minY = minZ,
        maxY = maxZ,
        minZ = minZ,
        maxZ = maxZ
    )

    val vertexCount: Int get() = vertices.size
    val isPlanar: Boolean get() = abs(maxY - minY) < 1e-4

    /**
     * Encuentra el vértice del anillo más próximo a un ángulo azimutal dado en radianes.
     */
    fun findClosestVertex(targetTheta: Double): RingVertex? {
        if (vertices.isEmpty()) return null
        return vertices.minByOrNull { v ->
            val diff = abs(v.theta - targetTheta)
            minOf(diff, (2 * Math.PI) - diff)
        }
    }
}
