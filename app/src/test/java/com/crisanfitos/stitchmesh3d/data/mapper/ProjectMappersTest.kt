package com.crisanfitos.stitchmesh3d.data.mapper

import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import com.crisanfitos.stitchmesh3d.data.local.entity.PartWithRounds
import com.crisanfitos.stitchmesh3d.data.local.entity.PatternRoundEntity
import com.crisanfitos.stitchmesh3d.data.local.entity.ProjectEntity
import com.crisanfitos.stitchmesh3d.data.local.entity.ProjectPartEntity
import com.crisanfitos.stitchmesh3d.data.local.entity.ProjectWithPartsAndRounds
import com.crisanfitos.stitchmesh3d.domain.model.PatternRound
import com.crisanfitos.stitchmesh3d.domain.model.Project
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectMappersTest {

    @Test
    fun `Project toEntity and toDomain round-trip preserves all attributes`() {
        val domainProject = Project(
            id = "proj-1",
            title = "Oso de Crochet",
            description = "Patrón paramétrico para oso",
            structureType = com.crisanfitos.stitchmesh3d.domain.model.ProjectStructureType.AMIGURUMI_3D,
            yarnWeightCategory = YarnWeightCategory.MEDIUM,
            hookSizeMm = 4.0f,
            customStitchWidthMm = 3.6f,
            customStitchHeightMm = 3.3f,
            isValid = true,
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val entity = domainProject.toEntity()
        assertEquals("proj-1", entity.id)
        assertEquals("Oso de Crochet", entity.title)
        assertEquals("amigurumi_3d", entity.structureType)
        assertEquals(4, entity.yarnWeightCategory)
        assertEquals(4.0f, entity.hookSizeMm)
        assertEquals(3.6f, entity.customStitchWidthMm)
        assertTrue(entity.isValid)

        val mappedBack = entity.toDomain()
        assertEquals(domainProject.id, mappedBack.id)
        assertEquals(domainProject.title, mappedBack.title)
        assertEquals(domainProject.structureType, mappedBack.structureType)
        assertEquals(domainProject.yarnWeightCategory, mappedBack.yarnWeightCategory)
        assertEquals(domainProject.hookSizeMm, mappedBack.hookSizeMm)
        assertEquals(domainProject.customStitchWidthMm, mappedBack.customStitchWidthMm)
        assertEquals(domainProject.isValid, mappedBack.isValid)
    }

    @Test
    fun `ProjectPart toEntity and toDomain preserves transform coordinates and rounds`() {
        val domainPart = ProjectPart(
            id = "part-1",
            projectId = "proj-1",
            name = "Cabeza",
            topologyType = com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType.CLOSED_FILLED,
            sortOrder = 0,
            transformPositionX = 10f,
            transformPositionY = 20f,
            transformPositionZ = 30f,
            transformRotationX = 0f,
            transformRotationY = 90f,
            transformRotationZ = 0f,
            isValid = true
        )

        val entity = domainPart.toEntity()
        assertEquals("part-1", entity.id)
        assertEquals("proj-1", entity.projectId)
        assertEquals("Cabeza", entity.name)
        assertEquals("closed_filled", entity.topologyType)
        assertEquals(10f, entity.transformPosX)
        assertEquals(90f, entity.transformRotY)

        val mappedBack = entity.toDomain()
        assertEquals(domainPart.id, mappedBack.id)
        assertEquals(domainPart.topologyType, mappedBack.topologyType)
        assertEquals(domainPart.transformPositionX, mappedBack.transformPositionX)
        assertEquals(domainPart.transformRotationY, mappedBack.transformRotationY)
    }

    @Test
    fun `PatternRound toEntity and toDomain maps linter statistics accurately`() {
        val round = PatternRound(
            id = "round-1",
            partId = "part-1",
            roundNumber = 3,
            rawInstruction = "[1 pb, 1 aum] * 6",
            colorHex = "#E06D53",
            consumedStitches = 12,
            producedStitches = 18,
            declaredStitches = 18,
            isValid = true,
            errorMessage = null
        )

        val entity = round.toEntity()
        assertEquals("round-1", entity.id)
        assertEquals(3, entity.roundNumber)
        assertEquals(12, entity.consumedStitches)
        assertEquals(18, entity.producedStitches)
        assertEquals(18, entity.declaredStitches)
        assertTrue(entity.isValid)

        val mappedBack = entity.toDomain()
        assertEquals(round, mappedBack)
    }

    @Test
    fun `ProjectWithPartsAndRounds aggregate maps nested hierarchy cleanly`() {
        val projectEntity = ProjectEntity(
            id = "proj-agg",
            title = "Muñeco Completo",
            yarnWeightCategory = 3,
            hookSizeMm = 3.0f
        )

        val partEntity = ProjectPartEntity(
            id = "part-agg",
            projectId = "proj-agg",
            name = "Cuerpo",
            sortOrder = 1
        )

        val roundEntity1 = PatternRoundEntity(
            id = "r-1",
            partId = "part-agg",
            roundNumber = 1,
            rawInstruction = "AM 6 pb",
            producedStitches = 6,
            isValid = true
        )

        val roundEntity2 = PatternRoundEntity(
            id = "r-2",
            partId = "part-agg",
            roundNumber = 2,
            rawInstruction = "6 aum",
            producedStitches = 12,
            isValid = true
        )

        val partWithRounds = PartWithRounds(
            part = partEntity,
            rounds = listOf(roundEntity2, roundEntity1) // Desordenados intencionalmente
        )

        val aggregate = ProjectWithPartsAndRounds(
            project = projectEntity,
            partsWithRounds = listOf(partWithRounds)
        )

        val domainProject = aggregate.toDomain()
        assertEquals(1, domainProject.parts.size)
        assertEquals("Cuerpo", domainProject.parts[0].name)
        // Vueltas deben ordenarse automáticamente por roundNumber
        assertEquals(2, domainProject.parts[0].rounds.size)
        assertEquals(1, domainProject.parts[0].rounds[0].roundNumber)
        assertEquals(2, domainProject.parts[0].rounds[1].roundNumber)

        // Verificamos propiedades computadas del dominio
        assertEquals(2, domainProject.totalRounds)
        assertEquals(18, domainProject.totalStitches)
    }
}
