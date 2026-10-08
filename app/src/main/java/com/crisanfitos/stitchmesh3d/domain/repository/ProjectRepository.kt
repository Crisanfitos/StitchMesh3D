package com.crisanfitos.stitchmesh3d.domain.repository

import com.crisanfitos.stitchmesh3d.domain.model.PatternRound
import com.crisanfitos.stitchmesh3d.domain.model.Project
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de repositorio reactivo para persistencia offline y sincronización de proyectos.
 */
interface ProjectRepository {
    fun getAllProjectsFlow(): Flow<List<Project>>
    suspend fun getProjectById(projectId: String): Project?
    fun getProjectByIdFlow(projectId: String): Flow<Project?>
    suspend fun insertOrUpdateProject(project: Project)
    suspend fun deleteProjectById(projectId: String)

    suspend fun insertPart(part: ProjectPart)
    suspend fun deletePartById(partId: String)

    suspend fun insertRound(round: PatternRound)
    suspend fun deleteRoundById(roundId: String)
    suspend fun replaceRoundsForPart(partId: String, rounds: List<PatternRound>)
}
