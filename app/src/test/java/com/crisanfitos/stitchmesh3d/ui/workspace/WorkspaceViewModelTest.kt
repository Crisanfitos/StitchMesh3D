package com.crisanfitos.stitchmesh3d.ui.workspace

import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import com.crisanfitos.stitchmesh3d.domain.model.PatternRound
import com.crisanfitos.stitchmesh3d.domain.model.Project
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import com.crisanfitos.stitchmesh3d.domain.repository.ProjectRepository
import com.crisanfitos.stitchmesh3d.ui.dashboard.components.LinterValidationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has valid default rounds and valid validation status`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Zorro Amigurumi", state.projectTitle)
        assertEquals(3, state.rounds.size)
        assertEquals(LinterValidationStatus.VALID, state.validationStatus)
        assertEquals(0, state.inconsistencyCount)
        assertFalse(state.isMeshFrozenDueToError)
        assertNotNull("La malla 3D debe estar generada para el patrón inicial válido", state.meshGeometry)
    }

    @Test
    fun `editing round with arithmetic error freezes mesh and sets error status`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        val initialMesh = viewModel.state.value.meshGeometry
        assertNotNull(initialMesh)

        // Introducir una inconsistencia: V3 consume 10 pts pero la base disponible es 12
        viewModel.processIntent(WorkspaceIntent.EditRoundInstruction("r3", "[1 pb, 1 aum] * 5"))
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(LinterValidationStatus.HAS_ERRORS, state.validationStatus)
        assertTrue(state.isMeshFrozenDueToError)
        assertEquals(1, state.inconsistencyCount)
        // La malla 3D previa se preserva congelada sin crashear el renderizado (SM-029)
        assertEquals(initialMesh, state.meshGeometry)
        assertNotNull(state.lastValidMesh)
    }

    @Test
    fun `correcting arithmetic error regenerates 3D mesh and restores valid status`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        // Introducir error
        viewModel.processIntent(WorkspaceIntent.EditRoundInstruction("r3", "[1 pb, 1 aum] * 5"))
        testScheduler.advanceUntilIdle()
        assertEquals(LinterValidationStatus.HAS_ERRORS, viewModel.state.value.validationStatus)

        // Corregir error
        viewModel.processIntent(WorkspaceIntent.EditRoundInstruction("r3", "[1 pb, 1 aum] * 6"))
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(LinterValidationStatus.VALID, state.validationStatus)
        assertFalse(state.isMeshFrozenDueToError)
        assertEquals(0, state.inconsistencyCount)
        assertNotNull(state.meshGeometry)
    }

    @Test
    fun `adding round creates instruction without parentheses and updates active selection`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        assertEquals(3, viewModel.state.value.rounds.size)

        viewModel.processIntent(WorkspaceIntent.AddRound)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(4, state.rounds.size)
        val newRound = state.rounds.last()
        assertEquals("r4", newRound.id)
        assertEquals(4, newRound.roundNumber)
        // La nueva vuelta no incluye (18) entre paréntesis en su texto crudo
        assertEquals("18 pb", newRound.rawInstruction)
        assertFalse("No debe contener paréntesis con recuento declarado", newRound.rawInstruction.contains("("))
        assertEquals(newRound.id, state.selectedRoundId)
    }

    @Test
    fun `selecting round updates selectedRoundId`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        viewModel.processIntent(WorkspaceIntent.SelectRound("r2"))
        testScheduler.advanceUntilIdle()

        assertEquals("r2", viewModel.state.value.selectedRoundId)
    }

    @Test
    fun `inserting token via quick keyboard targets active round`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        // Seleccionar vuelta 2 ("6 aum")
        viewModel.processIntent(WorkspaceIntent.SelectRound("r2"))
        testScheduler.advanceUntilIdle()

        viewModel.processIntent(WorkspaceIntent.InsertTokenAtActiveRound("pe"))
        testScheduler.advanceUntilIdle()

        val r2 = viewModel.state.value.rounds.first { it.id == "r2" }
        assertEquals("6 aum pe", r2.rawInstruction)
    }

    @Test
    fun `loadProject fetches from ProjectRepository and updates workspace state`() = runTest(testDispatcher) {
        val testProject = Project(
            id = "proj-repo-1",
            title = "Repo Amigurumi",
            yarnWeightCategory = YarnWeightCategory.MEDIUM,
            hookSizeMm = 3.5f,
            parts = listOf(
                ProjectPart(
                    id = "p-custom",
                    projectId = "proj-repo-1",
                    name = "Cuerpo",
                    rounds = listOf(
                        PatternRound(
                            id = "r-c1",
                            partId = "p-custom",
                            roundNumber = 1,
                            rawInstruction = "AM 6 pb",
                            producedStitches = 6,
                            isValid = true
                        )
                    )
                )
            )
        )
        val fakeRepo = FakeTestProjectRepository(mutableMapOf("proj-repo-1" to testProject))
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher, projectRepository = fakeRepo)
        testScheduler.advanceUntilIdle()

        viewModel.processIntent(WorkspaceIntent.LoadProject("proj-repo-1"))
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("proj-repo-1", state.projectId)
        assertEquals("Repo Amigurumi", state.projectTitle)
        assertEquals(3.5f, state.hookSizeMm)
        assertEquals(1, state.parts.size)
        assertEquals("p-custom", state.selectedPartId)
        assertEquals(1, state.rounds.size)
        assertEquals("AM 6 pb", state.rounds[0].rawInstruction)
    }

    @Test
    fun `modifying rounds persists updated state to ProjectRepository`() = runTest(testDispatcher) {
        val testProject = Project(
            id = "proj-persist",
            title = "Test Persist",
            yarnWeightCategory = YarnWeightCategory.MEDIUM,
            hookSizeMm = 4.0f,
            parts = listOf(
                ProjectPart(
                    id = "p1",
                    projectId = "proj-persist",
                    name = "Cabeza",
                    rounds = listOf(
                        PatternRound(
                            id = "r1",
                            partId = "p1",
                            roundNumber = 1,
                            rawInstruction = "AM 6",
                            producedStitches = 6,
                            isValid = true
                        )
                    )
                )
            )
        )
        val fakeRepo = FakeTestProjectRepository(mutableMapOf("proj-persist" to testProject))
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher, projectRepository = fakeRepo)
        testScheduler.advanceUntilIdle()

        viewModel.processIntent(WorkspaceIntent.LoadProject("proj-persist"))
        testScheduler.advanceUntilIdle()

        viewModel.processIntent(WorkspaceIntent.AddRound)
        testScheduler.advanceUntilIdle()

        val persisted = fakeRepo.getProjectById("proj-persist")
        assertNotNull(persisted)
        val part = persisted!!.parts.firstOrNull { it.id == "p1" }
        assertNotNull(part)
        assertEquals(2, part!!.rounds.size)
    }

    @Test
    fun `expanding range V3-V6 produces 4 individual rounds and leaves next round as V7`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        // Estado inicial: 3 vueltas (r1, r2, r3)
        assertEquals(3, viewModel.state.value.rounds.size)

        // Editar vuelta 3 con un rango V3-V6: 18 pb
        viewModel.processIntent(WorkspaceIntent.EditRoundInstruction("r3", "V3-V6: 18 pb"))
        testScheduler.advanceUntilIdle()

        val expandedRounds = viewModel.state.value.rounds
        assertEquals(6, expandedRounds.size)
        assertEquals("V3: 18 pb", expandedRounds[2].rawInstruction)
        assertEquals(3, expandedRounds[2].roundNumber)
        assertEquals("V4: 18 pb", expandedRounds[3].rawInstruction)
        assertEquals(4, expandedRounds[3].roundNumber)
        assertEquals("V5: 18 pb", expandedRounds[4].rawInstruction)
        assertEquals(5, expandedRounds[4].roundNumber)
        assertEquals("V6: 18 pb", expandedRounds[5].rawInstruction)
        assertEquals(6, expandedRounds[5].roundNumber)

        // Agregar una vuelta más debe generar automáticamente V7
        viewModel.processIntent(WorkspaceIntent.AddRound)
        testScheduler.advanceUntilIdle()

        val afterAddRounds = viewModel.state.value.rounds
        assertEquals(7, afterAddRounds.size)
        assertEquals(7, afterAddRounds.last().roundNumber)
        assertEquals("r7", afterAddRounds.last().id)
    }

    @Test
    fun `linter assigns correct expectedBaseStitches to all rounds`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        val rounds = viewModel.state.value.rounds
        assertEquals(0, rounds[0].expectedBaseStitches) // V1
        assertEquals(6, rounds[1].expectedBaseStitches) // V2 espera 6 pb de V1
        assertEquals(12, rounds[2].expectedBaseStitches) // V3 espera 12 pb de V2
    }
}

private class FakeTestProjectRepository(
    private val projects: MutableMap<String, Project> = mutableMapOf()
) : ProjectRepository {

    override fun getAllProjectsFlow(): Flow<List<Project>> = MutableStateFlow(projects.values.toList())

    override suspend fun getProjectById(projectId: String): Project? = projects[projectId]

    override fun getProjectByIdFlow(projectId: String): Flow<Project?> = MutableStateFlow(projects[projectId])

    override suspend fun insertOrUpdateProject(project: Project) {
        projects[project.id] = project
    }

    override suspend fun deleteProjectById(projectId: String) {
        projects.remove(projectId)
    }

    override suspend fun insertPart(part: ProjectPart) {
        val proj = projects[part.projectId] ?: return
        val updatedParts = proj.parts.filter { it.id != part.id } + part
        projects[proj.id] = proj.copy(parts = updatedParts)
    }

    override suspend fun deletePartById(partId: String) {
        projects.forEach { (id, proj) ->
            projects[id] = proj.copy(parts = proj.parts.filter { it.id != partId })
        }
    }

    override suspend fun insertRound(round: PatternRound) {}

    override suspend fun deleteRoundById(roundId: String) {}

    override suspend fun replaceRoundsForPart(partId: String, rounds: List<PatternRound>) {
        projects.forEach { (id, proj) ->
            val updatedParts = proj.parts.map { part ->
                if (part.id == partId) part.copy(rounds = rounds) else part
            }
            projects[id] = proj.copy(parts = updatedParts)
        }
    }
}
