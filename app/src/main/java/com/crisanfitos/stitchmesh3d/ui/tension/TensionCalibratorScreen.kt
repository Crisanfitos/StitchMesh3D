package com.crisanfitos.stitchmesh3d.ui.tension

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crisanfitos.stitchmesh3d.ui.tension.components.TensionMetricsCard
import com.crisanfitos.stitchmesh3d.ui.tension.components.TensionSampleForm
import com.crisanfitos.stitchmesh3d.ui.tension.components.TensionScaleComparisonCard
import com.crisanfitos.stitchmesh3d.ui.tension.components.TensionSwatchPreview
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshNeutralDark
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshOnAccent
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * Pantalla completa de Calibración de Tensión y Comparativa Paramétrica (SM-052).
 * Adaptativa con arquitectura Dual-Pane en tablets (>= 840dp) y vista en columna para teléfonos (< 840dp).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TensionCalibratorScreen(
    onBackClick: () -> Unit,
    viewModel: TensionViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TensionEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                TensionEffect.NavigateBack -> {
                    onBackClick()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Calibrador de Tensión CAD",
                            style = MaterialTheme.typography.titleMedium,
                            color = StitchMeshTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Muestra 10×10 cm & Comparativa Paramétrica",
                            style = MaterialTheme.typography.labelSmall,
                            color = StitchMeshTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = StitchMeshTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.processIntent(TensionIntent.ResetToStandard) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restablecer a valores estándar CYC",
                            tint = StitchMeshTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StitchMeshSurfaceContainer
                )
            )
        },
        containerColor = StitchMeshNeutralDark
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isTabletExpanded = maxWidth >= 840.dp

            if (isTabletExpanded) {
                // Modo Tablet Dual-Pane: Panel izquierdo (Formulario) + Panel derecho (Comparativa y Preview)
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Panel Izquierdo (48%): Especificación CYC, aguja nominal y formulario táctil
                    Column(
                        modifier = Modifier
                            .weight(0.48f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        TensionSampleForm(
                            selectedStandard = state.selectedStandard,
                            availableStandards = state.availableStandards,
                            stitchesCount = state.stitchesIn10Cm,
                            roundsCount = state.roundsIn10Cm,
                            onStandardSelected = { std -> viewModel.processIntent(TensionIntent.SelectStandard(std)) },
                            onIncrementStitches = { viewModel.processIntent(TensionIntent.IncrementStitches) },
                            onDecrementStitches = { viewModel.processIntent(TensionIntent.DecrementStitches) },
                            onIncrementRounds = { viewModel.processIntent(TensionIntent.IncrementRounds) },
                            onDecrementRounds = { viewModel.processIntent(TensionIntent.DecrementRounds) }
                        )

                        TensionMetricsCard(
                            calibrationResult = state.calibrationResult,
                            errorMessage = state.errorMessage
                        )
                    }

                    // Divisor vertical CAD
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(StitchMeshSurfaceBorder)
                    )

                    // Panel Derecho (52%): Comparativa paramétrica visual, cuadrícula y botón de aplicar
                    Column(
                        modifier = Modifier
                            .weight(0.52f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        val scaleComp = state.scaleComparison
                        if (scaleComp != null) {
                            TensionScaleComparisonCard(comparison = scaleComp)
                        }

                        val stitchW = state.calibrationResult?.customStandard?.stitchWidthMm ?: (100f / state.stitchesIn10Cm)
                        val stitchH = state.calibrationResult?.customStandard?.stitchHeightMm ?: (100f / state.roundsIn10Cm)

                        TensionSwatchPreview(
                            stitchesCount = state.stitchesIn10Cm,
                            roundsCount = state.roundsIn10Cm,
                            stitchWidthMm = stitchW,
                            stitchHeightMm = stitchH
                        )

                        ApplyTensionButton(
                            onClick = { viewModel.processIntent(TensionIntent.ApplyCalibration) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else {
                // Modo Compact (Móvil / Portrait): Columna vertical unificada con scroll
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    TensionSampleForm(
                        selectedStandard = state.selectedStandard,
                        availableStandards = state.availableStandards,
                        stitchesCount = state.stitchesIn10Cm,
                        roundsCount = state.roundsIn10Cm,
                        onStandardSelected = { std -> viewModel.processIntent(TensionIntent.SelectStandard(std)) },
                        onIncrementStitches = { viewModel.processIntent(TensionIntent.IncrementStitches) },
                        onDecrementStitches = { viewModel.processIntent(TensionIntent.DecrementStitches) },
                        onIncrementRounds = { viewModel.processIntent(TensionIntent.IncrementRounds) },
                        onDecrementRounds = { viewModel.processIntent(TensionIntent.DecrementRounds) }
                    )

                    TensionMetricsCard(
                        calibrationResult = state.calibrationResult,
                        errorMessage = state.errorMessage
                    )

                    val scaleComp = state.scaleComparison
                    if (scaleComp != null) {
                        TensionScaleComparisonCard(comparison = scaleComp)
                    }

                    val stitchW = state.calibrationResult?.customStandard?.stitchWidthMm ?: (100f / state.stitchesIn10Cm)
                    val stitchH = state.calibrationResult?.customStandard?.stitchHeightMm ?: (100f / state.roundsIn10Cm)

                    TensionSwatchPreview(
                        stitchesCount = state.stitchesIn10Cm,
                        roundsCount = state.roundsIn10Cm,
                        stitchWidthMm = stitchW,
                        stitchHeightMm = stitchH
                    )

                    ApplyTensionButton(
                        onClick = { viewModel.processIntent(TensionIntent.ApplyCalibration) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ApplyTensionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = StitchMeshTerracotta
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = StitchMeshOnAccent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "Aplicar Tensión al Proyecto",
            color = StitchMeshOnAccent,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
