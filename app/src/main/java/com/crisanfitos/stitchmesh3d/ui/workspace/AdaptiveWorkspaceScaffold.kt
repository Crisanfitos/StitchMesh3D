package com.crisanfitos.stitchmesh3d.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.crisanfitos.stitchmesh3d.ui.viewport.CrochetViewportScreen
import com.crisanfitos.stitchmesh3d.ui.viewport.ViewportIntent
import com.crisanfitos.stitchmesh3d.ui.viewport.ViewportViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crisanfitos.stitchmesh3d.core.engine.validator.CorrectionSuggestion
import com.crisanfitos.stitchmesh3d.ui.dashboard.components.LinterValidationStatus
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshNeutralDark
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshOnAccent
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.workspace.keyboard.CrochetQuickKeyboard
import com.crisanfitos.stitchmesh3d.ui.workspace.keyboard.CrochetTokenFormatter
import com.crisanfitos.stitchmesh3d.ui.viewport.components.PeelSliderBar
import com.crisanfitos.stitchmesh3d.ui.viewport.components.CameraPreset
import com.crisanfitos.stitchmesh3d.ui.viewport.components.ViewportDimensionsUiModel
import com.crisanfitos.stitchmesh3d.ui.viewport.components.ViewportHudOverlay
import com.crisanfitos.stitchmesh3d.ui.viewport.components.ViewportTelemetryUiModel
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextDisabled
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold
import com.crisanfitos.stitchmesh3d.ui.workspace.components.LinterInlineErrorCard
import com.crisanfitos.stitchmesh3d.ui.workspace.components.RoundItemRow
import com.crisanfitos.stitchmesh3d.ui.workspace.components.RoundItemUiModel

/**
 * Pestañas disponibles en el modo móvil compacto.
 */
enum class WorkspaceMobileTab {
    EDITOR,
    VIEWPORT_3D
}

/**
 * Scaffold maestro adaptativo del espacio de trabajo de StitchMesh 3D.
 *
 * Sigue la regla RND-3 y el Design Brief §2:
 * - En pantallas grandes (Tablet >= 840dp en horizontal): Dual-Pane simultáneo (40% editor / 60% visor 3D).
 * - En pantallas compactas (Móvil < 840dp): Alternancia por pestañas ("Editor" / "Visor 3D") con FAB de acceso directo.
 */
@Composable
fun AdaptiveWorkspaceScaffold(
    projectTitle: String,
    hookSizeMm: Float,
    yarnWeightName: String,
    parts: List<ProjectPartUiModel>,
    selectedPartId: String,
    rounds: List<RoundItemUiModel>,
    validationStatus: LinterValidationStatus,
    onBackClick: () -> Unit,
    onPartSelected: (String) -> Unit,
    onInstructionChanged: (roundId: String, newInstruction: String) -> Unit,
    onAddRound: () -> Unit,
    modifier: Modifier = Modifier,
    selectedRoundId: String? = null,
    onRoundSelected: ((String) -> Unit)? = null,
    showQuickKeyboard: Boolean = true,
    onDeleteRound: ((roundId: String) -> Unit)? = null,
    onAddPart: (() -> Unit)? = null,
    onApplySuggestion: ((roundId: String, suggestion: CorrectionSuggestion) -> Unit)? = null,
    onCalibrateTensionClick: (() -> Unit)? = null,
    onExportClick: (() -> Unit)? = null,
    viewportContent: (@Composable () -> Unit)? = null
) {
    var internalSelectedRoundId by remember {
        mutableStateOf(selectedRoundId ?: rounds.lastOrNull()?.id)
    }
    val activeRoundId = selectedRoundId ?: internalSelectedRoundId
    var isKeyboardVisible by remember { mutableStateOf(showQuickKeyboard) }

    LaunchedEffect(selectedRoundId) {
        if (selectedRoundId != null) {
            internalSelectedRoundId = selectedRoundId
        }
    }

    var previousRoundCount by remember { mutableIntStateOf(rounds.size) }
    LaunchedEffect(rounds.size) {
        if (rounds.size > previousRoundCount) {
            rounds.lastOrNull()?.let { newlyAdded ->
                internalSelectedRoundId = newlyAdded.id
                onRoundSelected?.invoke(newlyAdded.id)
            }
        }
        previousRoundCount = rounds.size
    }

    val viewportViewModel = remember { ViewportViewModel() }
    val viewportState by viewportViewModel.state.collectAsState()
    var isFullscreenViewport by remember { mutableStateOf(false) }

    LaunchedEffect(rounds.size, hookSizeMm, yarnWeightName) {
        viewportViewModel.processIntent(
            ViewportIntent.UpdatePatternParameters(
                totalRounds = rounds.size,
                hookSizeMm = hookSizeMm,
                yarnWeightName = yarnWeightName
            )
        )
    }

    fun handleToken(token: String) {
        val target = rounds.firstOrNull { it.id == activeRoundId } ?: rounds.lastOrNull()
        if (target != null) {
            val updated = CrochetTokenFormatter.insertToken(target.rawInstruction, token)
            onInstructionChanged(target.id, updated)
        }
    }

    fun handleBackspace() {
        val target = rounds.firstOrNull { it.id == activeRoundId } ?: rounds.lastOrNull()
        if (target != null) {
            val updated = CrochetTokenFormatter.deleteLastChar(target.rawInstruction)
            onInstructionChanged(target.id, updated)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StitchMeshNeutralDark,
        topBar = {
            if (!isFullscreenViewport) {
                WorkspaceTopBar(
                    projectTitle = projectTitle,
                    hookSizeMm = hookSizeMm,
                    yarnWeightName = yarnWeightName,
                    validationStatus = validationStatus,
                    onBackClick = onBackClick,
                    onCalibrateTensionClick = onCalibrateTensionClick,
                    onExportClick = onExportClick
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isFullscreenViewport) {
                // Modo Visor 3D Pantalla Completa (Stitch screens 1d1f9e82 y 244f94d7)
                CrochetViewportScreen(
                    state = viewportState.copy(isFullscreen = true),
                    onIntent = { intent ->
                        if (intent is ViewportIntent.ToggleFullscreen) {
                            isFullscreenViewport = false
                        } else {
                            viewportViewModel.processIntent(intent)
                        }
                    },
                    onCloseFullscreen = { isFullscreenViewport = false },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val isTablet = maxWidth >= 840.dp

            if (isTablet) {
                // Modo Tablet Dual-Pane: 40% Editor (izquierda) / 60% Visor 3D (derecha)
                Row(modifier = Modifier.fillMaxSize()) {
                    // Panel de edición de patrón (40%)
                    Column(
                        modifier = Modifier
                            .weight(0.40f)
                            .fillMaxHeight()
                    ) {
                        PartSelectorTabBar(
                            parts = parts,
                            selectedPartId = selectedPartId,
                            onPartSelected = onPartSelected,
                            onAddPart = onAddPart
                        )

                        RoundEditorPane(
                            rounds = rounds,
                            activeRoundId = activeRoundId,
                            onRoundSelected = { id ->
                                internalSelectedRoundId = id
                                onRoundSelected?.invoke(id)
                            },
                            onInstructionChanged = onInstructionChanged,
                            onAddRound = onAddRound,
                            onDeleteRound = onDeleteRound,
                            onApplySuggestion = onApplySuggestion,
                            modifier = Modifier.weight(1f)
                        )

                        // Teclado virtual contextual de crochet
                        if (isKeyboardVisible) {
                            CrochetQuickKeyboard(
                                onTokenInserted = { handleToken(it) },
                                onBackspace = { handleBackspace() },
                                onEnter = onAddRound,
                                onClose = { isKeyboardVisible = false }
                            )
                        } else {
                            KeyboardOpenToggle(onClick = { isKeyboardVisible = true })
                        }
                    }

                    // Divisor vertical CAD de 1dp
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(StitchMeshSurfaceBorder)
                    )

                    // Panel de Visor 3D (60%)
                    Box(
                        modifier = Modifier
                            .weight(0.60f)
                            .fillMaxHeight()
                            .background(StitchMeshNeutralDark)
                    ) {
                        if (viewportContent != null) {
                            viewportContent()
                        } else {
                            CrochetViewportScreen(
                                state = viewportState.copy(isFullscreen = false),
                                onIntent = { intent ->
                                    if (intent is ViewportIntent.ToggleFullscreen) {
                                        isFullscreenViewport = true
                                    } else {
                                        viewportViewModel.processIntent(intent)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            } else {
                // Modo Móvil: Pestañas alternables "Editor" vs "Visor 3D" con FAB
                var selectedTab by remember { mutableStateOf(WorkspaceMobileTab.EDITOR) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = StitchMeshNeutralDark,
                    floatingActionButton = {
                        if (selectedTab == WorkspaceMobileTab.EDITOR) {
                            FloatingActionButton(
                                onClick = { selectedTab = WorkspaceMobileTab.VIEWPORT_3D },
                                containerColor = StitchMeshTerracotta,
                                contentColor = StitchMeshOnAccent,
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewInAr,
                                    contentDescription = "Ver modelo 3D"
                                )
                            }
                        }
                    }
                ) { mobilePadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(mobilePadding)
                    ) {
                        SecondaryTabRow(
                            selectedTabIndex = selectedTab.ordinal,
                            containerColor = StitchMeshSurfaceContainer,
                            contentColor = StitchMeshTextPrimary
                        ) {
                            Tab(
                                selected = selectedTab == WorkspaceMobileTab.EDITOR,
                                onClick = { selectedTab = WorkspaceMobileTab.EDITOR },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (selectedTab == WorkspaceMobileTab.EDITOR) StitchMeshTerracotta else StitchMeshTextSecondary
                                        )
                                        Text(
                                            text = "Editor (${rounds.size})",
                                            fontWeight = if (selectedTab == WorkspaceMobileTab.EDITOR) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedTab == WorkspaceMobileTab.EDITOR) StitchMeshTextPrimary else StitchMeshTextSecondary
                                        )
                                    }
                                }
                            )

                            Tab(
                                selected = selectedTab == WorkspaceMobileTab.VIEWPORT_3D,
                                onClick = { selectedTab = WorkspaceMobileTab.VIEWPORT_3D },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ViewInAr,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (selectedTab == WorkspaceMobileTab.VIEWPORT_3D) StitchMeshTerracotta else StitchMeshTextSecondary
                                        )
                                        Text(
                                            text = "Visor 3D",
                                            fontWeight = if (selectedTab == WorkspaceMobileTab.VIEWPORT_3D) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedTab == WorkspaceMobileTab.VIEWPORT_3D) StitchMeshTextPrimary else StitchMeshTextSecondary
                                        )
                                    }
                                }
                            )
                        }

                        when (selectedTab) {
                            WorkspaceMobileTab.EDITOR -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    PartSelectorTabBar(
                                        parts = parts,
                                        selectedPartId = selectedPartId,
                                        onPartSelected = onPartSelected,
                                        onAddPart = onAddPart
                                    )

                                    RoundEditorPane(
                                        rounds = rounds,
                                        activeRoundId = activeRoundId,
                                        onRoundSelected = { id ->
                                            internalSelectedRoundId = id
                                            onRoundSelected?.invoke(id)
                                        },
                                        onInstructionChanged = onInstructionChanged,
                                        onAddRound = onAddRound,
                                        onDeleteRound = onDeleteRound,
                                        onApplySuggestion = onApplySuggestion,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isKeyboardVisible) {
                                        CrochetQuickKeyboard(
                                            onTokenInserted = { handleToken(it) },
                                            onBackspace = { handleBackspace() },
                                            onEnter = onAddRound,
                                            onClose = { isKeyboardVisible = false }
                                        )
                                    } else {
                                        KeyboardOpenToggle(onClick = { isKeyboardVisible = true })
                                    }
                                }
                            }
                            WorkspaceMobileTab.VIEWPORT_3D -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (viewportContent != null) {
                                        viewportContent()
                                    } else {
                                        CrochetViewportScreen(
                                            state = viewportState.copy(isFullscreen = false),
                                            onIntent = { intent ->
                                                if (intent is ViewportIntent.ToggleFullscreen) {
                                                    isFullscreenViewport = true
                                                } else {
                                                    viewportViewModel.processIntent(intent)
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * Contenedor de la lista de vueltas y botón de agregar.
 */
@Composable
private fun RoundEditorPane(
    rounds: List<RoundItemUiModel>,
    activeRoundId: String?,
    onRoundSelected: (String) -> Unit,
    onInstructionChanged: (roundId: String, newInstruction: String) -> Unit,
    onAddRound: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteRound: ((roundId: String) -> Unit)? = null,
    onApplySuggestion: ((roundId: String, suggestion: CorrectionSuggestion) -> Unit)? = null
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = rounds,
            key = { it.id }
        ) { round ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RoundItemRow(
                    round = round.copy(isHighlighted = (round.id == activeRoundId)),
                    onInstructionChanged = { newText ->
                        onInstructionChanged(round.id, newText)
                    },
                    onRowClicked = {
                        onRoundSelected(round.id)
                    },
                    onDeleteRound = if (onDeleteRound != null) {
                        { onDeleteRound(round.id) }
                    } else null
                )

                // Tarjeta inline si la vuelta contiene error
                if (!round.isValid && !round.errorMessage.isNullOrBlank()) {
                    val sampleSuggestions = remember(round.rawInstruction) {
                        com.crisanfitos.stitchmesh3d.core.engine.validator.CorrectionAssistant.suggestCorrections(
                            rawLine = round.rawInstruction,
                            previousRoundStitches = round.consumedStitches
                        )
                    }

                    LinterInlineErrorCard(
                        errorMessage = round.errorMessage,
                        suggestions = sampleSuggestions,
                        onApplySuggestion = { suggestion ->
                            onApplySuggestion?.invoke(round.id, suggestion)
                        },
                        consumedStitches = round.consumedStitches,
                        expectedBaseStitches = round.consumedStitches + 2 // Demostración de discrepancia
                    )
                }
            }
        }

        item {
            Button(
                onClick = onAddRound,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StitchMeshSurfaceHigh,
                    contentColor = StitchMeshTerracotta
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Añadir Vuelta",
                    style = CrochetTypography.tokenBadge,
                    color = StitchMeshTerracotta
                )
            }
        }
    }
}

/**
 * Placeholder estilizado para el Visor 3D con integración del PeelSliderBar interactivo
 * y la capa HUD con cotas métricas (mm) y selector de cámara (SM-049).
 */
@Composable
private fun DefaultViewportPlaceholder(
    roundCount: Int = 3,
    activeRoundIndex: Int = 3,
    hookSizeMm: Float = 3.5f,
    yarnWeightName: String = "#4 Worsted",
    onRoundSelected: ((Int) -> Unit)? = null
) {
    var peelRound by remember(roundCount) { mutableIntStateOf(activeRoundIndex) }
    var isWireframe by remember { mutableStateOf(false) }
    var selectedCameraPreset by remember { mutableStateOf(CameraPreset.ISOMETRIC) }

    // Estimación geométrica de cotas proyectadas calibradas por tensión (RF-2.2, RF-3.3)
    val dimensions = remember(hookSizeMm, peelRound) {
        val baseRadiusMm = (peelRound * hookSizeMm * 2.2f).coerceAtLeast(30f)
        val heightMm = (peelRound * hookSizeMm * 3.1f).coerceAtLeast(25f)
        ViewportDimensionsUiModel(
            widthMm = baseRadiusMm * 2f,
            heightMm = heightMm,
            depthMm = baseRadiusMm * 2f
        )
    }

    // Telemetría gráfica técnica en tiempo real
    val telemetry = remember(yarnWeightName, hookSizeMm, peelRound) {
        val estimatedPolys = (peelRound * 180).coerceAtLeast(360)
        ViewportTelemetryUiModel(
            polygonCount = estimatedPolys,
            vertexCount = estimatedPolys / 2 + 32,
            fps = 60,
            tensionGaugeLabel = "$yarnWeightName · ${hookSizeMm} mm"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(StitchMeshSurfaceContainer)
            .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(StitchMeshSurfaceHigh)
                    .border(1.dp, StitchMeshTerracotta.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = "Visor 3D Paramétrico",
                    tint = StitchMeshTerracotta,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "VISOR 3D PARAMÉTRICO",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = StitchMeshTextPrimary
            )

            Text(
                text = if (isWireframe) "Modo Alambre CAD (Wireframe)" else "Motor Filament PBR (Malla de lana)",
                style = MaterialTheme.typography.bodySmall,
                color = StitchMeshTextSecondary
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(StitchMeshSurfaceHigh)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Capa activa: $peelRound / $roundCount · Vista: ${selectedCameraPreset.label}",
                    fontSize = 11.sp,
                    color = StitchMeshYarnGold
                )
            }
        }

        // Overlay HUD superior con cotas dimensionales métricas y presets de cámara
        ViewportHudOverlay(
            dimensions = dimensions,
            telemetry = telemetry,
            selectedCameraPreset = selectedCameraPreset,
            onCameraPresetSelected = { selectedCameraPreset = it }
        )

        // Overlay inferior: PeelSliderBar interactiva con switch de alambre
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        ) {
            PeelSliderBar(
                currentRound = peelRound,
                totalRounds = roundCount,
                onRoundSelected = {
                    peelRound = it
                    onRoundSelected?.invoke(it)
                },
                isWireframe = isWireframe,
                onWireframeChange = { isWireframe = it }
            )
        }
    }
}

/**
 * Barra inferior minimalista para abrir el teclado rápido cuando ha sido ocultado.
 */
@Composable
private fun KeyboardOpenToggle(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            .background(StitchMeshSurfaceContainer)
            .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Keyboard,
            contentDescription = "Abrir teclado de crochet",
            tint = StitchMeshTerracotta,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Abrir Teclado de Crochet",
            style = CrochetTypography.tokenBadge,
            color = StitchMeshTextPrimary
        )
    }
}

@Preview(name = "AdaptiveWorkspace Tablet Preview", widthDp = 1024, heightDp = 600, showBackground = true)
@Composable
private fun AdaptiveWorkspaceTabletPreview() {
    StitchMesh3DTheme {
        AdaptiveWorkspaceScaffold(
            projectTitle = "Zorro Amigurumi",
            hookSizeMm = 3.5f,
            yarnWeightName = "#4 Worsted",
            parts = listOf(
                ProjectPartUiModel("p1", "Cabeza", 3, "#E06D53"),
                ProjectPartUiModel("p2", "Cuerpo", 0, "#F2C94C")
            ),
            selectedPartId = "p1",
            rounds = listOf(
                RoundItemUiModel("r1", 1, "AM 6", 6, 0, null, true),
                RoundItemUiModel("r2", 2, "6 aum", 12, 6, null, true),
                RoundItemUiModel("r3", 3, "[1 pb, 1 aum] * 5", 15, 10, null, false, "Faltan 2 puntos base")
            ),
            validationStatus = LinterValidationStatus.HAS_ERRORS,
            onBackClick = {},
            onPartSelected = {},
            onInstructionChanged = { _, _ -> },
            onAddRound = {}
        )
    }
}

@Preview(name = "AdaptiveWorkspace Phone Preview", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun AdaptiveWorkspacePhonePreview() {
    StitchMesh3DTheme {
        AdaptiveWorkspaceScaffold(
            projectTitle = "Zorro Amigurumi",
            hookSizeMm = 3.5f,
            yarnWeightName = "#4 Worsted",
            parts = listOf(
                ProjectPartUiModel("p1", "Cabeza", 3, "#E06D53")
            ),
            selectedPartId = "p1",
            rounds = listOf(
                RoundItemUiModel("r1", 1, "AM 6", 6, 0, null, true),
                RoundItemUiModel("r2", 2, "6 aum", 12, 6, null, true)
            ),
            validationStatus = LinterValidationStatus.VALID,
            onBackClick = {},
            onPartSelected = {},
            onInstructionChanged = { _, _ -> },
            onAddRound = {}
        )
    }
}
