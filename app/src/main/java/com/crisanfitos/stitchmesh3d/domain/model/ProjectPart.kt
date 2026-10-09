package com.crisanfitos.stitchmesh3d.domain.model

/**
 * Modelo de dominio para una pieza independiente dentro de un proyecto.
 */
data class ProjectPart(
    val id: String,
    val projectId: String,
    val name: String,
    val topologyType: PartTopologyType = PartTopologyType.CLOSED_FILLED,
    val sortOrder: Int = 0,
    val transformPositionX: Float = 0f,
    val transformPositionY: Float = 0f,
    val transformPositionZ: Float = 0f,
    val transformRotationX: Float = 0f,
    val transformRotationY: Float = 0f,
    val transformRotationZ: Float = 0f,
    val isValid: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val rounds: List<PatternRound> = emptyList()
)
