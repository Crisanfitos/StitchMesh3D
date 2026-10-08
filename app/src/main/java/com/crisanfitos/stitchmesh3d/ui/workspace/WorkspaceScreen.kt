package com.crisanfitos.stitchmesh3d.ui.workspace

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crisanfitos.stitchmesh3d.StitchMeshWorkbenchScreen

/**
 * Pantalla principal del Workspace CAD de StitchMesh 3D.
 * Conforme a Documentación/implementation_plan.md (§1, §2 Fase 3),
 * design_brief_responsive_tablet_ux.md (§2) y SM-027.
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
    onCalibrateTension: (() -> Unit)? = null
) {
    StitchMeshWorkbenchScreen(
        projectId = projectId,
        onBackToDashboard = onBackToDashboard,
        onCalibrateTension = onCalibrateTension
    )
}
