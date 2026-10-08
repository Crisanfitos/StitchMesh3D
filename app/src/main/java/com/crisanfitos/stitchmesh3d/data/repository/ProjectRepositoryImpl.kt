package com.crisanfitos.stitchmesh3d.data.repository

import com.crisanfitos.stitchmesh3d.data.local.dao.PatternRoundDao
import com.crisanfitos.stitchmesh3d.data.local.dao.ProjectDao
import com.crisanfitos.stitchmesh3d.data.local.dao.ProjectPartDao
import com.crisanfitos.stitchmesh3d.data.mapper.toDomain
import com.crisanfitos.stitchmesh3d.data.mapper.toEntity
import com.crisanfitos.stitchmesh3d.domain.model.PatternRound
import com.crisanfitos.stitchmesh3d.domain.model.Project
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import com.crisanfitos.stitchmesh3d.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de proyectos respaldado por Room Database.
 */
@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val projectPartDao: ProjectPartDao,
    private val patternRoundDao: PatternRoundDao
) : ProjectRepository {

    override fun getAllProjectsFlow(): Flow<List<Project>> {
        return projectDao.getAllProjectsWithDetailsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getProjectById(projectId: String): Project? {
        return projectDao.getProjectWithDetails(projectId)?.toDomain()
    }

    override fun getProjectByIdFlow(projectId: String): Flow<Project?> {
        return projectDao.getProjectWithDetailsFlow(projectId).map { it?.toDomain() }
    }

    override suspend fun insertOrUpdateProject(project: Project) {
        projectDao.insertProject(project.toEntity())
        for (part in project.parts) {
            projectPartDao.insertPart(part.toEntity())
            if (part.rounds.isNotEmpty()) {
                patternRoundDao.replaceRoundsForPart(part.id, part.rounds.map { it.toEntity() })
            }
        }
    }

    override suspend fun deleteProjectById(projectId: String) {
        projectDao.deleteProjectById(projectId)
    }

    override suspend fun insertPart(part: ProjectPart) {
        projectPartDao.insertPart(part.toEntity())
        if (part.rounds.isNotEmpty()) {
            patternRoundDao.replaceRoundsForPart(part.id, part.rounds.map { it.toEntity() })
        }
    }

    override suspend fun deletePartById(partId: String) {
        projectPartDao.deletePartById(partId)
    }

    override suspend fun insertRound(round: PatternRound) {
        patternRoundDao.insertRound(round.toEntity())
    }

    override suspend fun deleteRoundById(roundId: String) {
        patternRoundDao.deleteRoundById(roundId)
    }

    override suspend fun replaceRoundsForPart(partId: String, rounds: List<PatternRound>) {
        patternRoundDao.replaceRoundsForPart(partId, rounds.map { it.toEntity() })
    }
}
