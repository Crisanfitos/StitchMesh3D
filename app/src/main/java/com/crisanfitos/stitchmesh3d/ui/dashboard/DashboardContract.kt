package com.crisanfitos.stitchmesh3d.ui.dashboard

import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import com.crisanfitos.stitchmesh3d.core.mvi.ViewEffect
import com.crisanfitos.stitchmesh3d.core.mvi.ViewIntent
import com.crisanfitos.stitchmesh3d.core.mvi.ViewState
import com.crisanfitos.stitchmesh3d.domain.model.ProjectStructureType
import com.crisanfitos.stitchmesh3d.ui.dashboard.components.ProjectCardUiModel

/**
 * Estado inmutable de la pantalla de biblioteca de proyectos (Dashboard).
 */
data class DashboardState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedFilter: String = "Todos",
    val filterOptions: List<String> = listOf("Todos", "Válidos", "Con errores", "Recientes"),
    val allProjects: List<ProjectCardUiModel> = emptyList(),
    val isCreateProjectModalVisible: Boolean = false,
    val errorMessage: String? = null
) : ViewState {

    /**
     * Proyectos filtrados dinámicamente según la búsqueda textual y el chip seleccionado.
     */
    val filteredProjects: List<ProjectCardUiModel>
        get() = allProjects.filter { project ->
            val matchesQuery = searchQuery.isBlank() ||
                project.title.contains(searchQuery, ignoreCase = true) ||
                project.yarnWeightLabel.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Válidos" -> project.isLinterValid
                "Con errores" -> !project.isLinterValid
                else -> true
            }

            matchesQuery && matchesFilter
        }
}

/**
 * Intenciones de usuario y eventos de la interfaz para el Dashboard.
 */
sealed interface DashboardIntent : ViewIntent {
    data class SearchQueryChanged(val query: String) : DashboardIntent
    data class FilterSelected(val filter: String) : DashboardIntent
    object OpenCreateProjectModal : DashboardIntent
    object DismissCreateProjectModal : DashboardIntent
    data class CreateProject(
        val title: String,
        val structureType: ProjectStructureType = ProjectStructureType.AMIGURUMI_3D,
        val pieceType: String = structureType.displayName,
        val yarnWeight: YarnWeightCategory,
        val hookSizeMm: Float,
        val primaryColorHex: String
    ) : DashboardIntent
    data class ProjectClicked(val projectId: String) : DashboardIntent
    data class DeleteProject(val projectId: String) : DashboardIntent
    object RefreshProjects : DashboardIntent
}

/**
 * Efectos secundarios de navegación y mensajes de un solo uso.
 */
sealed interface DashboardEffect : ViewEffect {
    data class NavigateToWorkspace(val projectId: String) : DashboardEffect
    data class ShowMessage(val message: String) : DashboardEffect
}
