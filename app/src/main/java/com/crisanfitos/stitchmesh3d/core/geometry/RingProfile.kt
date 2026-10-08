package com.crisanfitos.stitchmesh3d.core.geometry

import kotlin.math.abs

/**
 * Perfil geométrico de una vuelta o anillo de crochet en el espacio tridimensional.
 *
 * @property roundIndex Índice de la vuelta (1..N).
 * @property vertices Lista de vértices perimetrales en orden azimutal continuo.
 * @property circumferenceMm Perímetro circunferencial total de la vuelta en milímetros.
 * @property meanRadiusMm Radio promedio del anillo en milímetros.
 * @property minZ Cota vertical mínima alcanzada en la vuelta.
 * @property maxZ Cota vertical máxima alcanzada en la vuelta.
 */
data class RingProfile(
    val roundIndex: Int,
    val vertices: List<RingVertex>,
    val circumferenceMm: Double,
    val meanRadiusMm: Double,
    val minZ: Double,
    val maxZ: Double
) {
    val vertexCount: Int get() = vertices.size
    val isPlanar: Boolean get() = abs(maxZ - minZ) < 1e-4

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
