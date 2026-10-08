package com.crisanfitos.stitchmesh3d.data.repository

import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import com.crisanfitos.stitchmesh3d.data.local.dao.PatternRoundDao
import com.crisanfitos.stitchmesh3d.data.local.dao.ProjectDao
import com.crisanfitos.stitchmesh3d.data.local.dao.ProjectPartDao
import com.crisanfitos.stitchmesh3d.data.local.entity.PartWithRounds
import com.crisanfitos.stitchmesh3d.data.local.entity.PatternRoundEntity
import com.crisanfitos.stitchmesh3d.data.local.entity.ProjectEntity
import com.crisanfitos.stitchmesh3d.data.local.entity.ProjectPartEntity
import com.crisanfitos.stitchmesh3d.data.local.entity.ProjectWithPartsAndRounds
import com.crisanfitos.stitchmesh3d.domain.model.PatternRound
import com.crisanfitos.stitchmesh3d.domain.model.Project
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ProjectRepositoryTest {

    private lateinit var fakeProjectDao: FakeProjectDao
    private lateinit var fakeProjectPartDao: FakeProjectPartDao
    private lateinit var fakePatternRoundDao: FakePatternRoundDao
    private lateinit var repository: ProjectRepositoryImpl

    @Before
    fun setup() {
        fakeProjectDao = FakeProjectDao()
        fakeProjectPartDao = FakeProjectPartDao()
        fakePatternRoundDao = FakePatternRoundDao()
        fakeProjectDao.partsProvider = { projectId ->
            val parts = fakeProjectPartDao.parts.values.filter { it.projectId == projectId }
            parts.map { part ->
                val rounds = fakePatternRoundDao.rounds.values.filter { it.partId == part.id }
                PartWithRounds(part, rounds)
            }
        }
        repository = ProjectRepositoryImpl(
            projectDao = fakeProjectDao,
            projectPartDao = fakeProjectPartDao,
            patternRoundDao = fakePatternRoundDao
        )
    }

    @Test
    fun `insertOrUpdateProject stores project, parts and rounds and emits in flow`() = runBlocking {
        val project = Project(
            id = "proj-test",
            title = "Oso Polar",
            yarnWeightCategory = YarnWeightCategory.BULKY,
            hookSizeMm = 6.0f,
            parts = listOf(
                ProjectPart(
                    id = "part-1",
                    projectId = "proj-test",
                    name = "Cabeza",
                    rounds = listOf(
                        PatternRound(
                            id = "r-1",
                            partId = "part-1",
                            roundNumber = 1,
                            rawInstruction = "AM 6 pb",
                            producedStitches = 6,
                            isValid = true
                        )
                    )
                )
            )
        )

        repository.insertOrUpdateProject(project)

        val retrieved = repository.getProjectById("proj-test")
        assertNotNull(retrieved)
        assertEquals("Oso Polar", retrieved!!.title)
        assertEquals(YarnWeightCategory.BULKY, retrieved.yarnWeightCategory)
        assertEquals(1, retrieved.parts.size)
        assertEquals("Cabeza", retrieved.parts[0].name)
        assertEquals(1, retrieved.parts[0].rounds.size)
    }

    @Test
    fun `deleteProjectById removes project from database`() = runBlocking {
        val project = Project(
            id = "proj-del",
            title = "A borrar"
        )
        repository.insertOrUpdateProject(project)
        assertNotNull(repository.getProjectById("proj-del"))

        repository.deleteProjectById("proj-del")
        assertNull(repository.getProjectById("proj-del"))
    }

    @Test
    fun `getAllProjectsFlow reflects dynamic updates reactively`() = runBlocking {
        val initialList = repository.getAllProjectsFlow().first()
        assertEquals(0, initialList.size)

        repository.insertOrUpdateProject(Project(id = "p1", title = "P1"))
        repository.insertOrUpdateProject(Project(id = "p2", title = "P2"))

        val updatedList = repository.getAllProjectsFlow().first()
        assertEquals(2, updatedList.size)
    }
}

// Fakes para testing aislado de repositorio
private class FakeProjectDao : ProjectDao {
    private val projects = mutableMapOf<String, ProjectEntity>()
    private val flow = MutableStateFlow<List<ProjectWithPartsAndRounds>>(emptyList())
    var partsProvider: ((String) -> List<PartWithRounds>)? = null

    private fun updateFlow() {
        val list = projects.values.map { p ->
            val parts = partsProvider?.invoke(p.id) ?: emptyList()
            ProjectWithPartsAndRounds(p, parts)
        }
        flow.value = list
    }

    override fun getAllProjectsFlow(): Flow<List<ProjectEntity>> = MutableStateFlow(projects.values.toList())

    override fun getAllProjectsWithDetailsFlow(): Flow<List<ProjectWithPartsAndRounds>> = flow

    override fun getProjectWithDetailsFlow(id: String): Flow<ProjectWithPartsAndRounds?> {
        val p = projects[id] ?: return MutableStateFlow(null)
        val parts = partsProvider?.invoke(id) ?: emptyList()
        return MutableStateFlow(ProjectWithPartsAndRounds(p, parts))
    }

    override suspend fun getProjectWithDetails(id: String): ProjectWithPartsAndRounds? {
        val p = projects[id] ?: return null
        val parts = partsProvider?.invoke(id) ?: emptyList()
        return ProjectWithPartsAndRounds(p, parts)
    }

    override suspend fun insertProject(project: ProjectEntity) {
        projects[project.id] = project
        updateFlow()
    }

    override suspend fun deleteProjectById(id: String): Int {
        val removed = projects.remove(id) != null
        updateFlow()
        return if (removed) 1 else 0
    }

    override suspend fun deleteAllProjects(): Int {
        val count = projects.size
        projects.clear()
        updateFlow()
        return count
    }
}

private class FakeProjectPartDao : ProjectPartDao {
    val parts = mutableMapOf<String, ProjectPartEntity>()

    override suspend fun insertPart(part: ProjectPartEntity) {
        parts[part.id] = part
    }

    override suspend fun deletePartById(id: String): Int {
        return if (parts.remove(id) != null) 1 else 0
    }

    override fun getPartsForProjectFlow(projectId: String): Flow<List<ProjectPartEntity>> {
        return MutableStateFlow(parts.values.filter { it.projectId == projectId })
    }

    override suspend fun getPartById(id: String): ProjectPartEntity? = parts[id]
}

private class FakePatternRoundDao : PatternRoundDao() {
    val rounds = mutableMapOf<String, PatternRoundEntity>()

    override suspend fun insertRound(round: PatternRoundEntity) {
        rounds[round.id] = round
    }

    override suspend fun insertRounds(rounds: List<PatternRoundEntity>) {
        rounds.forEach { this.rounds[it.id] = it }
    }

    override suspend fun deleteRoundById(id: String): Int {
        return if (rounds.remove(id) != null) 1 else 0
    }

    override suspend fun deleteRoundsByPartId(partId: String): Int {
        val toRemove = rounds.values.filter { it.partId == partId }
        toRemove.forEach { rounds.remove(it.id) }
        return toRemove.size
    }

    override fun getRoundsForPartFlow(partId: String): Flow<List<PatternRoundEntity>> {
        return MutableStateFlow(rounds.values.filter { it.partId == partId }.sortedBy { it.roundNumber })
    }
}
