package com.crisanfitos.stitchmesh3d.ui.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Pantalla principal del Workspace CAD de StitchMesh 3D conectada a [WorkspaceViewModel].
 * Conforme a Documentación/implementation_plan.md (§1, §2 Fase 3),
 * design_brief_responsive_tablet_ux.md (§2) y SM-029.
 *
 * Expone la interfaz de usuario adaptativa para edición y renderizado 3D:
 * - En tablets horizontales (>= 840dp): Dual-Pane simultáneo (40% editor / 60% viewport 3D).
 * - En móviles y ventanas compactas (< 840dp): Pestañas "Editor" vs "Visor 3D" con FAB.
 */
@Composable
fun WorkspaceScreen(
    projectId: String? = null,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier,
    onCalibrateTension: (() -> Unit)? = null,
    viewModel: WorkspaceViewModel = viewModel()
) {
    LaunchedEffect(projectId) {
        if (projectId != null) {
            viewModel.processIntent(WorkspaceIntent.LoadProject(projectId))
        }
    }

    val state by viewModel.state.collectAsState()

    AdaptiveWorkspaceScaffold(
        projectTitle = state.projectTitle,
        hookSizeMm = state.hookSizeMm,
        yarnWeightName = state.yarnWeightName,
        parts = state.parts,
        selectedPartId = state.selectedPartId,
        rounds = state.rounds,
        validationStatus = state.validationStatus,
        selectedRoundId = state.selectedRoundId,
        showQuickKeyboard = state.showQuickKeyboard,
        onBackClick = onBackToDashboard,
        onCalibrateTensionClick = onCalibrateTension,
        onPartSelected = { viewModel.processIntent(WorkspaceIntent.SelectPart(it)) },
        onRoundSelected = { viewModel.processIntent(WorkspaceIntent.SelectRound(it)) },
        onInstructionChanged = { id, text -> viewModel.processIntent(WorkspaceIntent.EditRoundInstruction(id, text)) },
        onAddRound = { viewModel.processIntent(WorkspaceIntent.AddRound) },
        onDeleteRound = { viewModel.processIntent(WorkspaceIntent.DeleteRound(it)) },
        onAddPart = { viewModel.processIntent(WorkspaceIntent.AddPart) },
        onApplySuggestion = { id, sug -> viewModel.processIntent(WorkspaceIntent.ApplySuggestion(id, sug)) },
        modifier = modifier
    )
}
