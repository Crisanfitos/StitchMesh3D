package com.crisanfitos.stitchmesh3d.data.repository

import androidx.room.withTransaction
import com.crisanfitos.stitchmesh3d.data.local.StitchMeshDatabase
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
 * Implementación del repositorio de proyectos respaldado por Room Database con soporte transaccional seguro.
 */
@Singleton
class ProjectRepositoryImpl(
    private val projectDao: ProjectDao,
    private val projectPartDao: ProjectPartDao,
    private val patternRoundDao: PatternRoundDao,
    private val database: StitchMeshDatabase?
) : ProjectRepository {

    @Inject
    constructor(
        database: StitchMeshDatabase,
        projectDao: ProjectDao,
        projectPartDao: ProjectPartDao,
        patternRoundDao: PatternRoundDao
    ) : this(projectDao, projectPartDao, patternRoundDao, database as StitchMeshDatabase?)

    constructor(
        projectDao: ProjectDao,
        projectPartDao: ProjectPartDao,
        patternRoundDao: PatternRoundDao
    ) : this(projectDao, projectPartDao, patternRoundDao, null)

    private suspend fun <R> runInTransaction(block: suspend () -> R): R {
        val db = database
        return if (db != null) {
            db.withTransaction { block() }
        } else {
            block()
        }
    }

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
        runInTransaction {
            projectDao.insertProject(project.toEntity())
            for (part in project.parts) {
                projectPartDao.insertPart(part.toEntity())
                if (part.rounds.isNotEmpty()) {
                    patternRoundDao.replaceRoundsForPart(part.id, part.rounds.map { it.toEntity() })
                }
            }
        }
    }

    override suspend fun deleteProjectById(projectId: String) {
        runInTransaction {
            projectDao.deleteProjectById(projectId)
        }
    }

    override suspend fun insertPart(part: ProjectPart) {
        runInTransaction {
            projectPartDao.insertPart(part.toEntity())
            if (part.rounds.isNotEmpty()) {
                patternRoundDao.replaceRoundsForPart(part.id, part.rounds.map { it.toEntity() })
            }
        }
    }

    override suspend fun deletePartById(partId: String) {
        runInTransaction {
            projectPartDao.deletePartById(partId)
        }
    }

    override suspend fun insertRound(round: PatternRound) {
        patternRoundDao.insertRound(round.toEntity())
    }

    override suspend fun deleteRoundById(roundId: String) {
        patternRoundDao.deleteRoundById(roundId)
    }

    override suspend fun replaceRoundsForPart(partId: String, rounds: List<PatternRound>) {
        runInTransaction {
            patternRoundDao.replaceRoundsForPart(partId, rounds.map { it.toEntity() })
        }
    }
}
