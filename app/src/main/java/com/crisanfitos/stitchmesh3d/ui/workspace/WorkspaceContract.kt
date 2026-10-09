package com.crisanfitos.stitchmesh3d.ui.workspace

import com.crisanfitos.stitchmesh3d.core.engine.validator.CorrectionSuggestion
import com.crisanfitos.stitchmesh3d.core.geometry.MeshGeometry
import com.crisanfitos.stitchmesh3d.ui.dashboard.components.LinterValidationStatus
import com.crisanfitos.stitchmesh3d.ui.workspace.components.RoundItemUiModel

/**
 * Estado inmutable de la pantalla de trabajo según el patrón MVI / UDF.
 * Conforme a Documentación/app_flow_navigation.md (§2) y TRD §1.
 */
data class WorkspaceState(
    val projectId: String = "proj-default",
    val projectTitle: String = "Zorro Amigurumi",
    val hookSizeMm: Float = 3.5f,
    val yarnWeightName: String = "#4 Worsted",
    val parts: List<ProjectPartUiModel> = listOf(
        ProjectPartUiModel("p1", "Cabeza", 3, "#E06D53", sortOrder = 0),
        ProjectPartUiModel("p2", "Cuerpo", 1, "#F2C94C", sortOrder = 1),
        ProjectPartUiModel("p3", "Orejas (x2)", 1, "#52A474", sortOrder = 2)
    ),
    val selectedPartId: String = "p1",
    val roundsByPartId: Map<String, List<RoundItemUiModel>> = mapOf(
        "p1" to listOf(
            RoundItemUiModel(
                id = "r1",
                roundNumber = 1,
                rawInstruction = "AM 6",
                producedStitches = 6,
                consumedStitches = 0,
                declaredStitches = null,
                isValid = true,
                colorHex = "#E06D53"
            ),
            RoundItemUiModel(
                id = "r2",
                roundNumber = 2,
                rawInstruction = "6 aum",
                producedStitches = 12,
                consumedStitches = 6,
                declaredStitches = null,
                isValid = true,
                colorHex = "#E06D53"
            ),
            RoundItemUiModel(
                id = "r3",
                roundNumber = 3,
                rawInstruction = "[1 pb, 1 aum] * 6",
                producedStitches = 18,
                consumedStitches = 12,
                declaredStitches = null,
                isValid = true,
                colorHex = "#E06D53"
            )
        ),
        "p2" to listOf(
            RoundItemUiModel(
                id = "p2_r1",
                roundNumber = 1,
                rawInstruction = "AM 6",
                producedStitches = 6,
                consumedStitches = 0,
                declaredStitches = null,
                isValid = true,
                colorHex = "#F2C94C"
            )
        ),
        "p3" to listOf(
            RoundItemUiModel(
                id = "p3_r1",
                roundNumber = 1,
                rawInstruction = "AM 6",
                producedStitches = 6,
                consumedStitches = 0,
                declaredStitches = null,
                isValid = true,
                colorHex = "#52A474"
            )
        )
    ),
    val rounds: List<RoundItemUiModel> = listOf(
        RoundItemUiModel(
            id = "r1",
            roundNumber = 1,
            rawInstruction = "AM 6",
            producedStitches = 6,
            consumedStitches = 0,
            declaredStitches = null,
            isValid = true,
            colorHex = "#E06D53"
        ),
        RoundItemUiModel(
            id = "r2",
            roundNumber = 2,
            rawInstruction = "6 aum",
            producedStitches = 12,
            consumedStitches = 6,
            declaredStitches = null,
            isValid = true,
            colorHex = "#E06D53"
        ),
        RoundItemUiModel(
            id = "r3",
            roundNumber = 3,
            rawInstruction = "[1 pb, 1 aum] * 6",
            producedStitches = 18,
            consumedStitches = 12,
            declaredStitches = null,
            isValid = true,
            colorHex = "#E06D53"
        )
    ),
    val selectedRoundId: String? = "r3",
    val validationStatus: LinterValidationStatus = LinterValidationStatus.VALID,
    val meshGeometry: MeshGeometry? = null,
    val lastValidMesh: MeshGeometry? = null,
    val isMeshFrozenDueToError: Boolean = false,
    val inconsistencyCount: Int = 0,
    val currentPeelRound: Int = 3,
    val isWireframe: Boolean = false,
    val isFullscreenViewport: Boolean = false,
    val showQuickKeyboard: Boolean = true,
    val showAddPartDialog: Boolean = false,
    val showRenamePartDialog: Boolean = false,
    val showDeletePartDialog: Boolean = false,
    val partActionTarget: ProjectPartUiModel? = null,
    val partActionError: String? = null
)

/**
 * Intenciones de usuario procesadas por [WorkspaceViewModel].
 */
sealed interface WorkspaceIntent {
    data class LoadProject(val projectId: String) : WorkspaceIntent
    data class EditRoundInstruction(val roundId: String, val newInstruction: String) : WorkspaceIntent
    data class ChangeRoundColor(val roundId: String, val colorHex: String) : WorkspaceIntent
    data class SelectRound(val roundId: String) : WorkspaceIntent
    data object AddRound : WorkspaceIntent
    data class DeleteRound(val roundId: String) : WorkspaceIntent
    data class ApplySuggestion(val roundId: String, val suggestion: CorrectionSuggestion) : WorkspaceIntent
    data class SelectPart(val partId: String) : WorkspaceIntent
    data object AddPart : WorkspaceIntent
    data object OpenAddPartDialog : WorkspaceIntent
    data object DismissPartDialogs : WorkspaceIntent
    data class CreatePart(val name: String, val colorHex: String = "#E06D53") : WorkspaceIntent
    data class OpenRenamePartDialog(val partId: String) : WorkspaceIntent
    data class ConfirmRenamePart(val partId: String, val newName: String) : WorkspaceIntent
    data class DuplicatePart(val partId: String) : WorkspaceIntent
    data class OpenDeletePartDialog(val partId: String) : WorkspaceIntent
    data class ConfirmDeletePart(val partId: String) : WorkspaceIntent
    data class ReorderPart(val partId: String, val toIndex: Int) : WorkspaceIntent
    data class SelectPeelRound(val roundNumber: Int) : WorkspaceIntent
    data class ToggleWireframe(val isWireframe: Boolean) : WorkspaceIntent
    data class ToggleFullscreen(val isFullscreen: Boolean) : WorkspaceIntent
    data class ToggleKeyboard(val show: Boolean) : WorkspaceIntent
    data class InsertTokenAtActiveRound(val token: String) : WorkspaceIntent
    data object DeleteCharFromActiveRound : WorkspaceIntent
}

/**
 * Efectos secundarios de navegación o feedback efímero.
 */
sealed interface WorkspaceEffect {
    data class ShowToast(val message: String) : WorkspaceEffect
    data class ScrollToRound(val roundId: String) : WorkspaceEffect
    data object TriggerHapticFeedback : WorkspaceEffect
}
