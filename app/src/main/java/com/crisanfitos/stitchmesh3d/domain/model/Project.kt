package com.crisanfitos.stitchmesh3d.domain.model

import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory

/**
 * Modelo de dominio para un proyecto de crochet/amigurumi en StitchMesh 3D.
 */
data class Project(
    val id: String,
    val title: String,
    val description: String? = null,
    val yarnWeightCategory: YarnWeightCategory = YarnWeightCategory.MEDIUM,
    val hookSizeMm: Float = 3.50f,
    val customStitchWidthMm: Float? = null,
    val customStitchHeightMm: Float? = null,
    val isValid: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val parts: List<ProjectPart> = emptyList()
) {
    val totalRounds: Int
        get() = parts.sumOf { it.rounds.size }

    val totalStitches: Int
        get() = parts.sumOf { part -> part.rounds.sumOf { it.producedStitches } }
}
