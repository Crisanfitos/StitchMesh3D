package com.crisanfitos.stitchmesh3d.ui.assembly

import androidx.compose.ui.geometry.Offset
import com.crisanfitos.stitchmesh3d.core.mvi.ViewEffect
import com.crisanfitos.stitchmesh3d.core.mvi.ViewIntent
import com.crisanfitos.stitchmesh3d.core.mvi.ViewState
import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import com.crisanfitos.stitchmesh3d.ui.assembly.components.PartTransformState
import com.crisanfitos.stitchmesh3d.ui.assembly.components.SceneGraphItem

/**
 * Modo de manipulación interactiva directa en el visor CAD 3D.
 */
enum class GizmoMode {
    NONE,
    TRANSLATE,
    ROTATE
}

/**
 * Estado inmutable de la pantalla de ensamblado multipieza 3D (MVI).
 * Conforme a Stitch Screen 14f11294, PRD RF-4.1..4.3 y SM-055.
 */
data class AssemblyState(
    val projectId: String = "",
    val projectTitle: String = "Proyecto Amigurumi",
    val parts: List<SceneGraphItem> = emptyList(),
    val selectedPartId: String = "",
    val transformsByPartId: Map<String, PartTransformState> = emptyMap(),
    val activeGizmoMode: GizmoMode = GizmoMode.TRANSLATE,
    val isWireframe: Boolean = false,
    val showCoordinateGrid: Boolean = true,
    val orbitYaw: Float = 45f,
    val orbitPitch: Float = 30f,
    val zoomScale: Float = 1.0f,
    val panOffset: Offset = Offset.Zero,
    val showAddPartDialog: Boolean = false,
    val partActionTarget: SceneGraphItem? = null,
    val showRenamePartDialog: Boolean = false,
    val showDeletePartDialog: Boolean = false
) : ViewState {
    val selectedPart: SceneGraphItem?
        get() = parts.find { it.id == selectedPartId }

    val selectedTransform: PartTransformState?
        get() = transformsByPartId[selectedPartId]
}

/**
 * Intenciones de usuario en el entorno de ensamblado multipieza.
 */
sealed interface AssemblyIntent : ViewIntent {
    data class LoadProject(val projectId: String) : AssemblyIntent
    data class SelectPart(val partId: String) : AssemblyIntent
    data class ToggleVisibility(val partId: String) : AssemblyIntent
    data class ToggleLock(val partId: String) : AssemblyIntent
    data class UpdateColor(val partId: String, val newColorHex: String) : AssemblyIntent
    data class UpdatePosition(val partId: String, val x: Float, val y: Float, val z: Float) : AssemblyIntent
    data class UpdateRotation(val partId: String, val rotX: Float, val rotY: Float, val rotZ: Float) : AssemblyIntent
    data class ToggleMirror(val partId: String) : AssemblyIntent
    data class ResetOrigin(val partId: String) : AssemblyIntent
    data class SetGizmoMode(val mode: GizmoMode) : AssemblyIntent
    data object ToggleWireframe : AssemblyIntent
    data object ToggleCoordinateGrid : AssemblyIntent
    data class UpdateOrbit(val deltaYaw: Float, val deltaPitch: Float) : AssemblyIntent
    data class UpdateZoom(val scaleMultiplier: Float) : AssemblyIntent
    data class UpdatePan(val deltaOffset: Offset) : AssemblyIntent
    data object ResetCamera : AssemblyIntent
    data object OpenAddPartDialog : AssemblyIntent
    data class OpenRenamePartDialog(val partId: String) : AssemblyIntent
    data class OpenDeletePartDialog(val partId: String) : AssemblyIntent
    data object DismissDialogs : AssemblyIntent
    data class CreatePart(
        val name: String,
        val colorHex: String,
        val topologyType: PartTopologyType = PartTopologyType.CLOSED_FILLED
    ) : AssemblyIntent
    data class RenamePart(val partId: String, val newName: String) : AssemblyIntent
    data class DuplicatePart(val partId: String) : AssemblyIntent
    data class DeletePart(val partId: String) : AssemblyIntent
}

/**
 * Efectos secundarios de un solo uso para retroalimentación táctil y navegación.
 */
sealed interface AssemblyEffect : ViewEffect {
    data class ShowSnackbar(val message: String) : AssemblyEffect
    data object NavigateBack : AssemblyEffect
}
