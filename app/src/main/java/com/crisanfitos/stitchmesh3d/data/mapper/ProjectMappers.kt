package com.crisanfitos.stitchmesh3d.data.mapper

import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import com.crisanfitos.stitchmesh3d.data.local.entity.PartWithRounds
import com.crisanfitos.stitchmesh3d.data.local.entity.PatternRoundEntity
import com.crisanfitos.stitchmesh3d.data.local.entity.ProjectEntity
import com.crisanfitos.stitchmesh3d.data.local.entity.ProjectPartEntity
import com.crisanfitos.stitchmesh3d.data.local.entity.ProjectWithPartsAndRounds
import com.crisanfitos.stitchmesh3d.domain.model.PatternRound
import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import com.crisanfitos.stitchmesh3d.domain.model.Project
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import com.crisanfitos.stitchmesh3d.domain.model.ProjectStructureType

fun ProjectEntity.toDomain(parts: List<ProjectPart> = emptyList()): Project {
    return Project(
        id = id,
        title = title,
        description = description,
        structureType = ProjectStructureType.fromCode(structureType),
        yarnWeightCategory = YarnWeightCategory.fromCode(yarnWeightCategory),
        hookSizeMm = hookSizeMm,
        customStitchWidthMm = customStitchWidthMm,
        customStitchHeightMm = customStitchHeightMm,
        isValid = isValid,
        createdAt = createdAt,
        updatedAt = updatedAt,
        parts = parts
    )
}

fun Project.toEntity(): ProjectEntity {
    return ProjectEntity(
        id = id,
        title = title,
        description = description,
        structureType = structureType.code,
        yarnWeightCategory = yarnWeightCategory.code,
        hookSizeMm = hookSizeMm,
        customStitchWidthMm = customStitchWidthMm,
        customStitchHeightMm = customStitchHeightMm,
        isValid = isValid,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ProjectPartEntity.toDomain(rounds: List<PatternRound> = emptyList()): ProjectPart {
    return ProjectPart(
        id = id,
        projectId = projectId,
        name = name,
        topologyType = PartTopologyType.fromCode(topologyType),
        sortOrder = sortOrder,
        transformPositionX = transformPosX,
        transformPositionY = transformPosY,
        transformPositionZ = transformPosZ,
        transformRotationX = transformRotX,
        transformRotationY = transformRotY,
        transformRotationZ = transformRotZ,
        isValid = isValid,
        createdAt = createdAt,
        rounds = rounds
    )
}

fun ProjectPart.toEntity(): ProjectPartEntity {
    return ProjectPartEntity(
        id = id,
        projectId = projectId,
        name = name,
        topologyType = topologyType.code,
        sortOrder = sortOrder,
        transformPosX = transformPositionX,
        transformPosY = transformPositionY,
        transformPosZ = transformPositionZ,
        transformRotX = transformRotationX,
        transformRotY = transformRotationY,
        transformRotZ = transformRotationZ,
        isValid = isValid,
        createdAt = createdAt
    )
}

fun PatternRoundEntity.toDomain(): PatternRound {
    return PatternRound(
        id = id,
        partId = partId,
        roundNumber = roundNumber,
        rawInstruction = rawInstruction,
        colorHex = colorHex,
        consumedStitches = consumedStitches,
        producedStitches = producedStitches,
        declaredStitches = declaredStitches,
        isValid = isValid,
        errorMessage = errorMessage,
        createdAt = createdAt
    )
}

fun PatternRound.toEntity(): PatternRoundEntity {
    return PatternRoundEntity(
        id = id,
        partId = partId,
        roundNumber = roundNumber,
        rawInstruction = rawInstruction,
        colorHex = colorHex,
        consumedStitches = consumedStitches,
        producedStitches = producedStitches,
        declaredStitches = declaredStitches,
        isValid = isValid,
        errorMessage = errorMessage,
        createdAt = createdAt
    )
}

fun PartWithRounds.toDomain(): ProjectPart {
    val domainRounds = rounds.sortedBy { it.roundNumber }.map { it.toDomain() }
    return part.toDomain(rounds = domainRounds)
}

fun ProjectWithPartsAndRounds.toDomain(): Project {
    val domainParts = partsWithRounds.sortedBy { it.part.sortOrder }.map { it.toDomain() }
    return project.toDomain(parts = domainParts)
}
