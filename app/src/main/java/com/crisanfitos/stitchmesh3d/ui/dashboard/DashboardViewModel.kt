package com.crisanfitos.stitchmesh3d.ui.dashboard

import androidx.lifecycle.viewModelScope
import com.crisanfitos.stitchmesh3d.core.mvi.BaseViewModel
import com.crisanfitos.stitchmesh3d.data.sync.SyncEngine
import com.crisanfitos.stitchmesh3d.domain.model.AuthState
import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import com.crisanfitos.stitchmesh3d.domain.model.PatternRound
import com.crisanfitos.stitchmesh3d.domain.model.Project
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import com.crisanfitos.stitchmesh3d.domain.model.ProjectStructureType
import com.crisanfitos.stitchmesh3d.domain.repository.AuthRepository
import com.crisanfitos.stitchmesh3d.domain.repository.ProjectRepository
import com.crisanfitos.stitchmesh3d.ui.dashboard.components.ProjectCardUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel que gestiona la lógica de presentación del Dashboard de proyectos siguiendo arquitectura MVI (SM-036, SM-089).
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository? = null,
    private val syncEngine: SyncEngine? = null
) : BaseViewModel<DashboardState, DashboardIntent, DashboardEffect>(DashboardState(isLoading = true)) {

    constructor(projectRepository: ProjectRepository) : this(projectRepository, null, null)

    init {
        observeProjects()
        observeAuthAndSync()
    }

    private fun observeAuthAndSync() {
        authRepository?.let { repo ->
            viewModelScope.launch {
                repo.authState.collect { auth ->
                    setState { copy(authState = auth) }
                }
            }
        }
        syncEngine?.let { engine ->
            viewModelScope.launch {
                engine.syncStatus.collect { sync ->
                    setState { copy(syncStatus = sync) }
                }
            }
        }
    }

    private fun observeProjects() {
        viewModelScope.launch {
            projectRepository.getAllProjectsFlow()
                .catch { error ->
                    setState { copy(isLoading = false, errorMessage = error.localizedMessage) }
                }
                .collect { projects ->
                    val uiModels = projects.map { project ->
                        val yarnLabel = "#${project.yarnWeightCategory.code} ${project.yarnWeightCategory.displayName.substringBefore(" /")}"
                        val invalidRoundsCount = project.parts.sumOf { part ->
                            part.rounds.count { !it.isValid }
                        }

                        ProjectCardUiModel(
                            id = project.id,
                            title = project.title,
                            lastModified = formatTimestamp(project.updatedAt),
                            yarnWeightLabel = yarnLabel,
                            hookSizeMm = project.hookSizeMm,
                            roundCount = project.totalRounds,
                            stitchCount = project.totalStitches,
                            isLinterValid = project.isValid,
                            errorCount = invalidRoundsCount
                        )
                    }

                    setState {
                        copy(
                            isLoading = false,
                            allProjects = uiModels,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    override fun processIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.SearchQueryChanged -> {
                setState { copy(searchQuery = intent.query) }
            }

            is DashboardIntent.FilterSelected -> {
                setState { copy(selectedFilter = intent.filter) }
            }

            DashboardIntent.OpenCreateProjectModal -> {
                setState { copy(isCreateProjectModalVisible = true) }
            }

            DashboardIntent.DismissCreateProjectModal -> {
                setState { copy(isCreateProjectModalVisible = false) }
            }

            is DashboardIntent.CreateProject -> {
                createNewProject(intent)
            }

            is DashboardIntent.ProjectClicked -> {
                sendEffect(DashboardEffect.NavigateToWorkspace(intent.projectId))
            }

            is DashboardIntent.DeleteProject -> {
                deleteProject(intent.projectId)
            }

            DashboardIntent.RefreshProjects -> {
                observeProjects()
            }

            DashboardIntent.OpenCloudAccountModal -> {
                setState { copy(isCloudAccountDialogVisible = true) }
            }

            DashboardIntent.DismissCloudAccountModal -> {
                setState { copy(isCloudAccountDialogVisible = false) }
            }

            is DashboardIntent.SignIn -> {
                viewModelScope.launch {
                    val repo = authRepository ?: return@launch
                    val res = repo.signInWithEmail(intent.email, intent.pass)
                    res.onSuccess {
                        sendEffect(DashboardEffect.ShowMessage("Sesión iniciada con éxito"))
                        setState { copy(isCloudAccountDialogVisible = false) }
                        syncEngine?.syncAllProjects()
                    }.onFailure { error ->
                        sendEffect(DashboardEffect.ShowMessage("Error al iniciar sesión: ${error.localizedMessage}"))
                    }
                }
            }

            is DashboardIntent.SignUp -> {
                viewModelScope.launch {
                    val repo = authRepository ?: return@launch
                    val res = repo.signUpWithEmail(intent.email, intent.pass, intent.username, intent.displayName)
                    res.onSuccess {
                        sendEffect(DashboardEffect.ShowMessage("Cuenta creada con éxito"))
                        setState { copy(isCloudAccountDialogVisible = false) }
                        syncEngine?.syncAllProjects()
                    }.onFailure { error ->
                        sendEffect(DashboardEffect.ShowMessage("Error al registrarse: ${error.localizedMessage}"))
                    }
                }
            }

            is DashboardIntent.UpdateProfile -> {
                viewModelScope.launch {
                    val repo = authRepository ?: return@launch
                    val res = repo.updateProfile(intent.username, intent.displayName)
                    res.onSuccess {
                        sendEffect(DashboardEffect.ShowMessage("Perfil actualizado"))
                    }.onFailure { error ->
                        sendEffect(DashboardEffect.ShowMessage("Error al actualizar perfil: ${error.localizedMessage}"))
                    }
                }
            }

            DashboardIntent.SignOut -> {
                viewModelScope.launch {
                    authRepository?.signOut()
                    setState { copy(isCloudAccountDialogVisible = false) }
                    sendEffect(DashboardEffect.ShowMessage("Sesión cerrada"))
                }
            }

            DashboardIntent.TriggerSync -> {
                viewModelScope.launch {
                    syncEngine?.syncAllProjects()
                }
            }
        }
    }

    private fun createNewProject(intent: DashboardIntent.CreateProject) {
        viewModelScope.launch {
            val projectId = UUID.randomUUID().toString()
            val partId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val (partName, topologyType, rawInstruction, initialStitches) = when (intent.structureType) {
                ProjectStructureType.AMIGURUMI_3D -> {
                    val name = if (intent.pieceType.isNotBlank() && intent.pieceType != "Amigurumi (3D)") intent.pieceType else "Cuerpo Principal"
                    Quadruple(name, PartTopologyType.CLOSED_FILLED, "AM 6 pb", 6)
                }
                ProjectStructureType.FLAT_GARMENT -> {
                    val name = if (intent.pieceType.isNotBlank() && intent.pieceType != "Prenda Plana") intent.pieceType else "Panel Principal"
                    Quadruple(name, PartTopologyType.FLAT_PANEL, "10 cad", 10)
                }
                ProjectStructureType.GRANNY_SQUARE -> {
                    val name = if (intent.pieceType.isNotBlank() && intent.pieceType != "Granny Square") intent.pieceType else "Motivo 1"
                    Quadruple(name, PartTopologyType.FLAT_PANEL, "AM 8 pb", 8)
                }
                ProjectStructureType.ACCESSORY -> {
                    val name = if (intent.pieceType.isNotBlank() && intent.pieceType != "Accesorio") intent.pieceType else "Base Tubular"
                    Quadruple(name, PartTopologyType.SEMI_CLOSED_TUBE, "AM 6 pb", 6)
                }
            }

            val initialRound = PatternRound(
                id = UUID.randomUUID().toString(),
                partId = partId,
                roundNumber = 1,
                rawInstruction = rawInstruction,
                colorHex = intent.primaryColorHex,
                consumedStitches = 0,
                producedStitches = initialStitches,
                declaredStitches = initialStitches,
                isValid = true,
                createdAt = now
            )

            val initialPart = ProjectPart(
                id = partId,
                projectId = projectId,
                name = partName,
                topologyType = topologyType,
                sortOrder = 0,
                isValid = true,
                createdAt = now,
                rounds = listOf(initialRound)
            )

            val newProject = Project(
                id = projectId,
                title = intent.title,
                structureType = intent.structureType,
                yarnWeightCategory = intent.yarnWeight,
                hookSizeMm = intent.hookSizeMm,
                isValid = true,
                createdAt = now,
                updatedAt = now,
                parts = listOf(initialPart)
            )

            projectRepository.insertOrUpdateProject(newProject)

            setState { copy(isCreateProjectModalVisible = false) }
            sendEffect(DashboardEffect.NavigateToWorkspace(projectId))
        }
    }

    private fun deleteProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.deleteProjectById(projectId)
            sendEffect(DashboardEffect.ShowMessage("Proyecto eliminado"))
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val diffMinutes = (System.currentTimeMillis() - timestamp) / (1000 * 60)
        return when {
            diffMinutes < 1 -> "Modificado hace un momento"
            diffMinutes < 60 -> "Modificado hace ${diffMinutes}m"
            diffMinutes < 1440 -> "Modificado hace ${diffMinutes / 60}h"
            else -> "Modificado hace ${diffMinutes / 1440}d"
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
