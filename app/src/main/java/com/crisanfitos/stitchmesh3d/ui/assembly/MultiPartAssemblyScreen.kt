package com.crisanfitos.stitchmesh3d.ui.assembly

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crisanfitos.stitchmesh3d.ui.assembly.components.AssemblyViewport3D
import com.crisanfitos.stitchmesh3d.ui.assembly.components.SceneGraphTree
import com.crisanfitos.stitchmesh3d.ui.assembly.components.TransformInspectorPanel
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshNeutralDark
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshOnAccent
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.Typography
import com.crisanfitos.stitchmesh3d.ui.workspace.dialogs.AddPartDialog
import com.crisanfitos.stitchmesh3d.ui.workspace.dialogs.DeletePartConfirmDialog
import com.crisanfitos.stitchmesh3d.ui.workspace.dialogs.RenamePartDialog

/**
 * Pantalla completa de Ensamblado 3D Multipieza (Stitch screen 14f11294, SM-055).
 * Integra el árbol jerárquico SceneGraphTree a la izquierda, el visor interactivo CAD 3D
 * en el centro y el panel TransformInspectorPanel a la derecha bajo un flujo MVI reactivo.
 *
 * Adaptable a tablets (3 paneles simultáneos en >= 900dp) y móviles (visor superior y pestañas inferiores en < 900dp).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiPartAssemblyScreen(
    projectId: String? = null,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AssemblyViewModel = viewModel()
) {
    LaunchedEffect(projectId) {
        if (!projectId.isNullOrBlank()) {
            viewModel.processIntent(AssemblyIntent.LoadProject(projectId))
        }
    }

    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AssemblyEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is AssemblyEffect.NavigateBack -> {
                    onBackClick()
                }
            }
        }
    }

    var selectedMobileTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier
                    .border(width = 1.dp, color = StitchMeshSurfaceBorder)
                    .testTag("assembly_top_bar"),
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Ensamblador 3D",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = StitchMeshTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(StitchMeshTerracotta)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "CAD MULTI-PIEZA",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchMeshOnAccent,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        Text(
                            text = "${state.projectTitle} · ${state.parts.size} piezas",
                            style = MaterialTheme.typography.labelSmall,
                            color = StitchMeshTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("assembly_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al espacio de trabajo",
                            tint = StitchMeshTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.processIntent(AssemblyIntent.ResetCamera) },
                        modifier = Modifier.testTag("assembly_top_reset_camera")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Restablecer cámara",
                            tint = StitchMeshTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StitchMeshSurfaceContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = StitchMeshNeutralDark
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isTablet = maxWidth >= 900.dp

            if (isTablet) {
                // Layout 3 Columnas Tablet: Árbol (320dp) | Visor 3D (Fluido) | Inspector (320dp)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Columna Izquierda: Árbol Scene Graph
                    Box(
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight()
                    ) {
                        SceneGraphTree(
                            items = state.parts,
                            selectedPartId = state.selectedPartId,
                            onPartSelected = { viewModel.processIntent(AssemblyIntent.SelectPart(it)) },
                            onToggleVisibility = { viewModel.processIntent(AssemblyIntent.ToggleVisibility(it)) },
                            onToggleLock = { viewModel.processIntent(AssemblyIntent.ToggleLock(it)) },
                            onColorChanged = { id, hex -> viewModel.processIntent(AssemblyIntent.UpdateColor(id, hex)) },
                            onAddPart = { viewModel.processIntent(AssemblyIntent.OpenAddPartDialog) },
                            onDuplicatePart = { viewModel.processIntent(AssemblyIntent.DuplicatePart(it)) },
                            onRenamePart = { viewModel.processIntent(AssemblyIntent.OpenRenamePartDialog(it)) },
                            onDeletePart = { viewModel.processIntent(AssemblyIntent.OpenDeletePartDialog(it)) }
                        )
                    }

                    // Columna Central: Visor 3D y Gizmo de Manipulación
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        AssemblyViewport3D(
                            parts = state.parts,
                            selectedPartId = state.selectedPartId,
                            transformsByPartId = state.transformsByPartId,
                            gizmoMode = state.activeGizmoMode,
                            isWireframe = state.isWireframe,
                            showGrid = state.showCoordinateGrid,
                            orbitYaw = state.orbitYaw,
                            orbitPitch = state.orbitPitch,
                            zoomScale = state.zoomScale,
                            panOffset = state.panOffset,
                            onPartSelected = { viewModel.processIntent(AssemblyIntent.SelectPart(it)) },
                            onPositionChange = { id, x, y, z ->
                                viewModel.processIntent(AssemblyIntent.UpdatePosition(id, x, y, z))
                            },
                            onRotationChange = { id, rx, ry, rz ->
                                viewModel.processIntent(AssemblyIntent.UpdateRotation(id, rx, ry, rz))
                            },
                            onGizmoModeChange = { viewModel.processIntent(AssemblyIntent.SetGizmoMode(it)) },
                            onToggleWireframe = { viewModel.processIntent(AssemblyIntent.ToggleWireframe) },
                            onToggleGrid = { viewModel.processIntent(AssemblyIntent.ToggleCoordinateGrid) },
                            onOrbitRotate = { dy, dp -> viewModel.processIntent(AssemblyIntent.UpdateOrbit(dy, dp)) },
                            onZoomChange = { zoom -> viewModel.processIntent(AssemblyIntent.UpdateZoom(zoom)) },
                            onPanChange = { pan -> viewModel.processIntent(AssemblyIntent.UpdatePan(pan)) },
                            onResetCamera = { viewModel.processIntent(AssemblyIntent.ResetCamera) }
                        )
                    }

                    // Columna Derecha: Inspector Numérico CAD
                    Box(
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight()
                    ) {
                        TransformInspectorPanel(
                            transform = state.selectedTransform,
                            onPositionChange = { x, y, z ->
                                state.selectedPart?.let {
                                    viewModel.processIntent(AssemblyIntent.UpdatePosition(it.id, x, y, z))
                                }
                            },
                            onRotationChange = { rx, ry, rz ->
                                state.selectedPart?.let {
                                    viewModel.processIntent(AssemblyIntent.UpdateRotation(it.id, rx, ry, rz))
                                }
                            },
                            onMirrorToggled = {
                                state.selectedPart?.let {
                                    viewModel.processIntent(AssemblyIntent.ToggleMirror(it.id))
                                }
                            },
                            onResetOrigin = {
                                state.selectedPart?.let {
                                    viewModel.processIntent(AssemblyIntent.ResetOrigin(it.id))
                                }
                            }
                        )
                    }
                }
            } else {
                // Layout Móvil: Visor 3D arriba (55%) y Pestañas abajo (45%)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.55f)
                    ) {
                        AssemblyViewport3D(
                            parts = state.parts,
                            selectedPartId = state.selectedPartId,
                            transformsByPartId = state.transformsByPartId,
                            gizmoMode = state.activeGizmoMode,
                            isWireframe = state.isWireframe,
                            showGrid = state.showCoordinateGrid,
                            orbitYaw = state.orbitYaw,
                            orbitPitch = state.orbitPitch,
                            zoomScale = state.zoomScale,
                            panOffset = state.panOffset,
                            onPartSelected = { viewModel.processIntent(AssemblyIntent.SelectPart(it)) },
                            onPositionChange = { id, x, y, z ->
                                viewModel.processIntent(AssemblyIntent.UpdatePosition(id, x, y, z))
                            },
                            onRotationChange = { id, rx, ry, rz ->
                                viewModel.processIntent(AssemblyIntent.UpdateRotation(id, rx, ry, rz))
                            },
                            onGizmoModeChange = { viewModel.processIntent(AssemblyIntent.SetGizmoMode(it)) },
                            onToggleWireframe = { viewModel.processIntent(AssemblyIntent.ToggleWireframe) },
                            onToggleGrid = { viewModel.processIntent(AssemblyIntent.ToggleCoordinateGrid) },
                            onOrbitRotate = { dy, dp -> viewModel.processIntent(AssemblyIntent.UpdateOrbit(dy, dp)) },
                            onZoomChange = { zoom -> viewModel.processIntent(AssemblyIntent.UpdateZoom(zoom)) },
                            onPanChange = { pan -> viewModel.processIntent(AssemblyIntent.UpdatePan(pan)) },
                            onResetCamera = { viewModel.processIntent(AssemblyIntent.ResetCamera) }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.45f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StitchMeshSurfaceContainer)
                            .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(12.dp))
                    ) {
                        PrimaryTabRow(
                            selectedTabIndex = selectedMobileTab,
                            containerColor = StitchMeshSurfaceContainer,
                            contentColor = StitchMeshTerracotta
                        ) {
                            Tab(
                                selected = selectedMobileTab == 0,
                                onClick = { selectedMobileTab = 0 },
                                text = {
                                    Text(
                                        text = "Piezas (${state.parts.size})",
                                        style = Typography.labelLarge,
                                        color = if (selectedMobileTab == 0) StitchMeshTerracotta else StitchMeshTextSecondary
                                    )
                                },
                                modifier = Modifier.testTag("tab_scene_graph")
                            )
                            Tab(
                                selected = selectedMobileTab == 1,
                                onClick = { selectedMobileTab = 1 },
                                text = {
                                    Text(
                                        text = "Inspector CAD",
                                        style = Typography.labelLarge,
                                        color = if (selectedMobileTab == 1) StitchMeshTerracotta else StitchMeshTextSecondary
                                    )
                                },
                                modifier = Modifier.testTag("tab_inspector")
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            if (selectedMobileTab == 0) {
                                SceneGraphTree(
                                    items = state.parts,
                                    selectedPartId = state.selectedPartId,
                                    onPartSelected = { viewModel.processIntent(AssemblyIntent.SelectPart(it)) },
                                    onToggleVisibility = { viewModel.processIntent(AssemblyIntent.ToggleVisibility(it)) },
                                    onToggleLock = { viewModel.processIntent(AssemblyIntent.ToggleLock(it)) },
                                    onColorChanged = { id, hex -> viewModel.processIntent(AssemblyIntent.UpdateColor(id, hex)) },
                                    onAddPart = { viewModel.processIntent(AssemblyIntent.OpenAddPartDialog) },
                                    onDuplicatePart = { viewModel.processIntent(AssemblyIntent.DuplicatePart(it)) },
                                    onRenamePart = { viewModel.processIntent(AssemblyIntent.OpenRenamePartDialog(it)) },
                                    onDeletePart = { viewModel.processIntent(AssemblyIntent.OpenDeletePartDialog(it)) }
                                )
                            } else {
                                TransformInspectorPanel(
                                    transform = state.selectedTransform,
                                    onPositionChange = { x, y, z ->
                                        state.selectedPart?.let {
                                            viewModel.processIntent(AssemblyIntent.UpdatePosition(it.id, x, y, z))
                                        }
                                    },
                                    onRotationChange = { rx, ry, rz ->
                                        state.selectedPart?.let {
                                            viewModel.processIntent(AssemblyIntent.UpdateRotation(it.id, rx, ry, rz))
                                        }
                                    },
                                    onMirrorToggled = {
                                        state.selectedPart?.let {
                                            viewModel.processIntent(AssemblyIntent.ToggleMirror(it.id))
                                        }
                                    },
                                    onResetOrigin = {
                                        state.selectedPart?.let {
                                            viewModel.processIntent(AssemblyIntent.ResetOrigin(it.id))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Diálogo Crear Pieza
    if (state.showAddPartDialog) {
        AddPartDialog(
            existingNames = state.parts.map { it.name },
            onDismiss = { viewModel.processIntent(AssemblyIntent.DismissDialogs) },
            onConfirm = { name, colorHex, topology ->
                viewModel.processIntent(AssemblyIntent.CreatePart(name, colorHex, topology))
            }
        )
    }

    // Modal Diálogo Renombrar Pieza
    if (state.showRenamePartDialog && state.partActionTarget != null) {
        RenamePartDialog(
            currentName = state.partActionTarget!!.name,
            existingNames = state.parts.map { it.name },
            onDismiss = { viewModel.processIntent(AssemblyIntent.DismissDialogs) },
            onConfirm = { newName ->
                viewModel.processIntent(AssemblyIntent.RenamePart(state.partActionTarget!!.id, newName))
            }
        )
    }

    // Modal Diálogo Confirmar Eliminación
    if (state.showDeletePartDialog && state.partActionTarget != null) {
        DeletePartConfirmDialog(
            partName = state.partActionTarget!!.name,
            onDismiss = { viewModel.processIntent(AssemblyIntent.DismissDialogs) },
            onConfirm = {
                viewModel.processIntent(AssemblyIntent.DeletePart(state.partActionTarget!!.id))
            }
        )
    }
}
