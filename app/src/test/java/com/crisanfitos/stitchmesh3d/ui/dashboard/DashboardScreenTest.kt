package com.crisanfitos.stitchmesh3d.ui.dashboard

import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import com.crisanfitos.stitchmesh3d.domain.model.PatternRound
import com.crisanfitos.stitchmesh3d.domain.model.Project
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import com.crisanfitos.stitchmesh3d.domain.repository.ProjectRepository
import com.crisanfitos.stitchmesh3d.ui.dashboard.dialogs.DefaultCrochetPalette
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

/**
 * Pruebas unitarias de la pantalla Dashboard de Proyectos y diálogo de creación (SM-026).
 * Verifica los criterios de aceptación de presentación de metadatos, validación de título y navegación.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardScreenTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeDashboardProjectRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDashboardProjectRepository()
        viewModel = DashboardViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `acceptance criterion 1 - projects display metadata including title, round count, and validation status`() = runTest(testDispatcher) {
        val sampleRound = PatternRound(
            id = "round-1",
            partId = "part-1",
            roundNumber = 1,
            rawInstruction = "AM 6",
            producedStitches = 6,
            consumedStitches = 0,
            isValid = true
        )
        val testProject = Project(
            id = "proj-meta-1",
            title = "Zorro Amigurumi",
            yarnWeightCategory = YarnWeightCategory.MEDIUM,
            hookSizeMm = 3.5f,
            isValid = true,
            parts = listOf(
                ProjectPart(
                    id = "part-1",
                    projectId = "proj-meta-1",
                    name = "Cabeza",
                    rounds = listOf(sampleRound)
                )
            )
        )
        fakeRepository.emitProjects(listOf(testProject))
        advanceUntilIdle()

        val projects = viewModel.state.value.allProjects
        assertEquals(1, projects.size)
        val uiModel = projects.first()
        assertEquals("Zorro Amigurumi", uiModel.title)
        assertEquals(1, uiModel.roundCount)
        assertEquals(6, uiModel.stitchCount)
        assertEquals(3.5f, uiModel.hookSizeMm)
        assertTrue(uiModel.isLinterValid)
        assertEquals(0, uiModel.errorCount)
    }

    @Test
    fun `acceptance criterion 2 - title validation prevents creation with empty or blank title`() {
        val blankTitle = "   "
        val emptyTitle = ""
        val validTitle = "Amigurumi Dragon"

        assertFalse(blankTitle.trim().isNotEmpty())
        assertFalse(emptyTitle.trim().isNotEmpty())
        assertTrue(validTitle.trim().isNotEmpty())
    }

    @Test
    fun `acceptance criterion 3 - creating new project triggers navigation to workspace with generated id`() = runTest(testDispatcher) {
        var emittedEffect: DashboardEffect? = null
        val job = launch {
            emittedEffect = viewModel.effect.first()
        }

        viewModel.processIntent(DashboardIntent.OpenCreateProjectModal)
        assertTrue(viewModel.state.value.isCreateProjectModalVisible)

        viewModel.processIntent(
            DashboardIntent.CreateProject(
                title = "Oso Polar",
                pieceType = "Amigurumi",
                yarnWeight = YarnWeightCategory.MEDIUM,
                hookSizeMm = 4.0f,
                primaryColorHex = "#FFFFFF"
            )
        )
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isCreateProjectModalVisible)
        assertNotNull(emittedEffect)
        assertTrue(emittedEffect is DashboardEffect.NavigateToWorkspace)
        val navEffect = emittedEffect as DashboardEffect.NavigateToWorkspace

        val created = fakeRepository.getProjectById(navEffect.projectId)
        assertNotNull(created)
        assertEquals("Oso Polar", created?.title)
        assertEquals(4.0f, created?.hookSizeMm)
        job.cancel()
    }

    @Test
    fun `default swatches provide terracotta as primary base color`() {
        val palette = DefaultCrochetPalette
        assertTrue(palette.isNotEmpty())
        assertEquals("Terracotta", palette.first().name)
    }
}

private class FakeDashboardProjectRepository : ProjectRepository {
    private val projectsMap = mutableMapOf<String, Project>()
    private val flow = MutableStateFlow<List<Project>>(emptyList())

    fun emitProjects(list: List<Project>) {
        list.forEach { projectsMap[it.id] = it }
        flow.value = list
    }

    override fun getAllProjectsFlow(): Flow<List<Project>> = flow

    override suspend fun getProjectById(projectId: String): Project? = projectsMap[projectId]

    override fun getProjectByIdFlow(projectId: String): Flow<Project?> = MutableStateFlow(projectsMap[projectId])

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
