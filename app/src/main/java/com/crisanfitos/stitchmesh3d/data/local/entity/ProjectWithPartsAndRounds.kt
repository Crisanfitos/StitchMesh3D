package com.crisanfitos.stitchmesh3d.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Agregado relacional que vincula una pieza de proyecto con todas sus vueltas asociadas.
 */
data class PartWithRounds(
    @Embedded
    val part: ProjectPartEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "part_id"
    )
    val rounds: List<PatternRoundEntity>
)

/**
 * Agregado relacional completo que vincula un proyecto con sus partes y respectivas vueltas.
 */
data class ProjectWithPartsAndRounds(
    @Embedded
    val project: ProjectEntity,

    @Relation(
        entity = ProjectPartEntity::class,
        parentColumn = "id",
        entityColumn = "project_id"
    )
    val partsWithRounds: List<PartWithRounds>
)
