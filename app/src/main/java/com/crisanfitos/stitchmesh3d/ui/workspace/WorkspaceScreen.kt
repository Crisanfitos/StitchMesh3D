package com.crisanfitos.stitchmesh3d.ui.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crisanfitos.stitchmesh3d.ui.workspace.dialogs.AddPartDialog
import com.crisanfitos.stitchmesh3d.ui.workspace.dialogs.DeletePartConfirmDialog
import com.crisanfitos.stitchmesh3d.ui.workspace.dialogs.RenamePartDialog

/**
 * Pantalla principal del Workspace CAD de StitchMesh 3D conectada a [WorkspaceViewModel].
 * Conforme a Documentación/implementation_plan.md (§1, §2 Fase 3),
 * design_brief_responsive_tablet_ux.md (§2) y SM-029 / SM-031.
 *
 * Expone la interfaz de usuario adaptativa para edición y renderizado 3D:
 * - En tablets horizontales (>= 840dp): Dual-Pane simultáneo (40% editor / 60% viewport 3D).
 * - En móviles y ventanas compactas (< 840dp): Pestañas "Editor" vs "Visor 3D" con FAB.
 * - Gestión completa de partes compuestas (CRUD de piezas en proyecto) (RF-4.1).
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
        onAddPart = { viewModel.processIntent(WorkspaceIntent.OpenAddPartDialog) },
        onRenamePart = { viewModel.processIntent(WorkspaceIntent.OpenRenamePartDialog(it)) },
        onDuplicatePart = { viewModel.processIntent(WorkspaceIntent.DuplicatePart(it)) },
        onDeletePart = { viewModel.processIntent(WorkspaceIntent.OpenDeletePartDialog(it)) },
        onApplySuggestion = { id, sug -> viewModel.processIntent(WorkspaceIntent.ApplySuggestion(id, sug)) },
        newlyCreatedRoundId = state.newlyCreatedRoundId,
        onConsumeFocus = { viewModel.processIntent(WorkspaceIntent.ConsumeInitialFocus) },
        modifier = modifier
    )

    if (state.showAddPartDialog) {
        AddPartDialog(
            existingNames = state.parts.map { it.name },
            onDismiss = { viewModel.processIntent(WorkspaceIntent.DismissPartDialogs) },
            onConfirm = { name, colorHex, topologyType ->
                viewModel.processIntent(WorkspaceIntent.CreatePart(name, colorHex, topologyType))
            }
        )
    }

    if (state.showRenamePartDialog && state.partActionTarget != null) {
        RenamePartDialog(
            currentName = state.partActionTarget!!.name,
            existingNames = state.parts.map { it.name },
            onDismiss = { viewModel.processIntent(WorkspaceIntent.DismissPartDialogs) },
            onConfirm = { newName ->
                viewModel.processIntent(WorkspaceIntent.ConfirmRenamePart(state.partActionTarget!!.id, newName))
            }
        )
    }

    if (state.showDeletePartDialog && state.partActionTarget != null) {
        DeletePartConfirmDialog(
            partName = state.partActionTarget!!.name,
            onDismiss = { viewModel.processIntent(WorkspaceIntent.DismissPartDialogs) },
            onConfirm = {
                viewModel.processIntent(WorkspaceIntent.ConfirmDeletePart(state.partActionTarget!!.id))
            }
        )
    }
}
