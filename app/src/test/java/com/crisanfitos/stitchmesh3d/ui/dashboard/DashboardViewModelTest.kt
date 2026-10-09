package com.crisanfitos.stitchmesh3d.ui.dashboard

import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import com.crisanfitos.stitchmesh3d.domain.model.PatternRound
import com.crisanfitos.stitchmesh3d.domain.model.Project
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import com.crisanfitos.stitchmesh3d.domain.model.ProjectStructureType
import com.crisanfitos.stitchmesh3d.domain.repository.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeProjectRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeProjectRepository()
        viewModel = DashboardViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads projects from repository and updates isLoading to false`() = runTest(testDispatcher) {
        val project1 = Project(
            id = "p-1",
            title = "Oso Amigurumi",
            yarnWeightCategory = YarnWeightCategory.MEDIUM,
            isValid = true
        )
        fakeRepository.emitProjects(listOf(project1))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(1, viewModel.state.value.allProjects.size)
        assertEquals("Oso Amigurumi", viewModel.state.value.allProjects[0].title)
        assertTrue(viewModel.state.value.allProjects[0].isLinterValid)
    }

    @Test
    fun `search query intent dynamically filters filteredProjects`() = runTest(testDispatcher) {
        val p1 = Project(id = "1", title = "Dragón Verde")
        val p2 = Project(id = "2", title = "Oso Marrón")
        fakeRepository.emitProjects(listOf(p1, p2))
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.filteredProjects.size)

        viewModel.processIntent(DashboardIntent.SearchQueryChanged("Dragón"))
        assertEquals("Dragón", viewModel.state.value.searchQuery)
        assertEquals(1, viewModel.state.value.filteredProjects.size)
        assertEquals("Dragón Verde", viewModel.state.value.filteredProjects[0].title)

        viewModel.processIntent(DashboardIntent.SearchQueryChanged(""))
        assertEquals(2, viewModel.state.value.filteredProjects.size)
    }

    @Test
    fun `filter selection filters valid and error projects`() = runTest(testDispatcher) {
        val pValid = Project(id = "1", title = "Proyecto OK", isValid = true)
        val pInvalid = Project(id = "2", title = "Proyecto Fallido", isValid = false)
        fakeRepository.emitProjects(listOf(pValid, pInvalid))
        advanceUntilIdle()

        viewModel.processIntent(DashboardIntent.FilterSelected("Válidos"))
        assertEquals(1, viewModel.state.value.filteredProjects.size)
        assertEquals("Proyecto OK", viewModel.state.value.filteredProjects[0].title)

        viewModel.processIntent(DashboardIntent.FilterSelected("Con errores"))
        assertEquals(1, viewModel.state.value.filteredProjects.size)
        assertEquals("Proyecto Fallido", viewModel.state.value.filteredProjects[0].title)
    }

    @Test
    fun `open and dismiss create modal intents update modal visibility`() = runTest(testDispatcher) {
        assertFalse(viewModel.state.value.isCreateProjectModalVisible)

        viewModel.processIntent(DashboardIntent.OpenCreateProjectModal)
        assertTrue(viewModel.state.value.isCreateProjectModalVisible)

        viewModel.processIntent(DashboardIntent.DismissCreateProjectModal)
        assertFalse(viewModel.state.value.isCreateProjectModalVisible)
    }

    @Test
    fun `create project intent persists project and emits NavigateToWorkspace effect`() = runTest(testDispatcher) {
        var emittedEffect: DashboardEffect? = null
        val job = launch {
            emittedEffect = viewModel.effect.first()
        }

        viewModel.processIntent(
            DashboardIntent.CreateProject(
                title = "Nuevo Panda",
                pieceType = "Cabeza",
                yarnWeight = YarnWeightCategory.BULKY,
                hookSizeMm = 6.0f,
                primaryColorHex = "#E06D53"
            )
        )
        advanceUntilIdle()

        assertNotNull(emittedEffect)
        assertTrue(emittedEffect is DashboardEffect.NavigateToWorkspace)
        val workspaceEffect = emittedEffect as DashboardEffect.NavigateToWorkspace
        assertNotNull(workspaceEffect.projectId)

        val stored = fakeRepository.getProjectById(workspaceEffect.projectId)
        assertNotNull(stored)
        assertEquals("Nuevo Panda", stored!!.title)
        assertEquals(ProjectStructureType.AMIGURUMI_3D, stored.structureType)
        assertEquals(YarnWeightCategory.BULKY, stored.yarnWeightCategory)
        assertEquals(1, stored.parts.size)
        assertEquals(PartTopologyType.CLOSED_FILLED, stored.parts[0].topologyType)
        assertEquals("Cabeza", stored.parts[0].name)
        assertEquals("AM 6 pb", stored.parts[0].rounds[0].rawInstruction)
        job.cancel()
    }

    @Test
    fun `create project with FLAT_GARMENT initializes FLAT_PANEL part and 10 cad round`() = runTest(testDispatcher) {
        var emittedEffect: DashboardEffect? = null
        val job = launch {
            emittedEffect = viewModel.effect.first()
        }

        viewModel.processIntent(
            DashboardIntent.CreateProject(
                title = "Bufanda Infinita",
                structureType = ProjectStructureType.FLAT_GARMENT,
                yarnWeight = YarnWeightCategory.MEDIUM,
                hookSizeMm = 4.5f,
                primaryColorHex = "#52A474"
            )
        )
        advanceUntilIdle()

        val navEffect = emittedEffect as DashboardEffect.NavigateToWorkspace
        val stored = fakeRepository.getProjectById(navEffect.projectId)
        assertNotNull(stored)
        assertEquals(ProjectStructureType.FLAT_GARMENT, stored!!.structureType)
        val initialPart = stored.parts.first()
        assertEquals(PartTopologyType.FLAT_PANEL, initialPart.topologyType)
        assertEquals("Panel Principal", initialPart.name)
        assertEquals("10 cad", initialPart.rounds.first().rawInstruction)
        assertEquals(10, initialPart.rounds.first().producedStitches)
        job.cancel()
    }

    @Test
    fun `create project with GRANNY_SQUARE initializes FLAT_PANEL part and AM 8 pb round`() = runTest(testDispatcher) {
        var emittedEffect: DashboardEffect? = null
        val job = launch {
            emittedEffect = viewModel.effect.first()
        }

        viewModel.processIntent(
            DashboardIntent.CreateProject(
                title = "Cuadrado Vintage",
                structureType = ProjectStructureType.GRANNY_SQUARE,
                yarnWeight = YarnWeightCategory.LIGHT,
                hookSizeMm = 3.5f,
                primaryColorHex = "#F2C94C"
            )
        )
        advanceUntilIdle()

        val navEffect = emittedEffect as DashboardEffect.NavigateToWorkspace
        val stored = fakeRepository.getProjectById(navEffect.projectId)
        assertNotNull(stored)
        assertEquals(ProjectStructureType.GRANNY_SQUARE, stored!!.structureType)
        val initialPart = stored.parts.first()
        assertEquals(PartTopologyType.FLAT_PANEL, initialPart.topologyType)
        assertEquals("Motivo 1", initialPart.name)
        assertEquals("AM 8 pb", initialPart.rounds.first().rawInstruction)
        assertEquals(8, initialPart.rounds.first().producedStitches)
        job.cancel()
    }

    @Test
    fun `create project with ACCESSORY initializes SEMI_CLOSED_TUBE part`() = runTest(testDispatcher) {
        var emittedEffect: DashboardEffect? = null
        val job = launch {
            emittedEffect = viewModel.effect.first()
        }

        viewModel.processIntent(
            DashboardIntent.CreateProject(
                title = "Gorro Beanie",
                structureType = ProjectStructureType.ACCESSORY,
                yarnWeight = YarnWeightCategory.BULKY,
                hookSizeMm = 5.0f,
                primaryColorHex = "#E06D53"
            )
        )
        advanceUntilIdle()

        val navEffect = emittedEffect as DashboardEffect.NavigateToWorkspace
        val stored = fakeRepository.getProjectById(navEffect.projectId)
        assertNotNull(stored)
        assertEquals(ProjectStructureType.ACCESSORY, stored!!.structureType)
        val initialPart = stored.parts.first()
        assertEquals(PartTopologyType.SEMI_CLOSED_TUBE, initialPart.topologyType)
        assertEquals("Base Tubular", initialPart.name)
        assertEquals("AM 6 pb", initialPart.rounds.first().rawInstruction)
        job.cancel()
    }

    @Test
    fun `project clicked intent emits NavigateToWorkspace with target id`() = runTest(testDispatcher) {
        var emittedEffect: DashboardEffect? = null
        val job = launch {
            emittedEffect = viewModel.effect.first()
        }

        viewModel.processIntent(DashboardIntent.ProjectClicked("p-target-123"))
        advanceUntilIdle()

        assertEquals(DashboardEffect.NavigateToWorkspace("p-target-123"), emittedEffect)
        job.cancel()
    }
}

private class FakeProjectRepository : ProjectRepository {
    private val projectsMap = mutableMapOf<String, Project>()
    private val flow = MutableStateFlow<List<Project>>(emptyList())

    fun emitProjects(list: List<Project>) {
        list.forEach { projectsMap[it.id] = it }
        flow.value = list
    }

    override fun getAllProjectsFlow(): Flow<List<Project>> = flow

    override suspend fun getProjectById(projectId: String): Project? = projectsMap[projectId]

    override fun getProjectByIdFlow(projectId: String): Flow<Project?> {
        return MutableStateFlow(projectsMap[projectId])
    }

    override suspend fun insertOrUpdateProject(project: Project) {
        projectsMap[project.id] = project
        flow.value = projectsMap.values.toList()
    }

    override suspend fun deleteProjectById(projectId: String) {
        projectsMap.remove(projectId)
        flow.value = projectsMap.values.toList()
    }

    override suspend fun insertPart(part: ProjectPart) {}
    override suspend fun deletePartById(partId: String) {}
    override suspend fun insertRound(round: PatternRound) {}
    override suspend fun deleteRoundById(roundId: String) {}
    override suspend fun replaceRoundsForPart(partId: String, rounds: List<PatternRound>) {}
}
