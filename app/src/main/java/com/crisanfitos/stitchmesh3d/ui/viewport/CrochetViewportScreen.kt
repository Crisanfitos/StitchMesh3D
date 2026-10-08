package com.crisanfitos.stitchmesh3d.ui.viewport

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshNeutralDark
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshOnAccent
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSageGreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold
import com.crisanfitos.stitchmesh3d.ui.viewport.components.CameraPreset
import com.crisanfitos.stitchmesh3d.ui.viewport.components.PeelSliderBar
import com.crisanfitos.stitchmesh3d.ui.viewport.components.ViewportDimensionsUiModel
import com.crisanfitos.stitchmesh3d.ui.viewport.components.ViewportHudOverlay
import com.crisanfitos.stitchmesh3d.ui.viewport.components.ViewportTelemetryUiModel
import com.crisanfitos.stitchmesh3d.ui.viewport.components.CrochetViewport3D
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pantalla completa y contenedor interactivo del Visor 3D Paramétrico (Stitch screens 1d1f9e82 y 244f94d7).
 *
 * Ofrece:
 * - Renderizado e interactividad 3D con órbita 360°, paneo y zoom por pellizco (RF-3.3).
 * - Inspección de vueltas capa a capa mediante PeelSliderBar (RF-3.4).
 * - Conmutación entre malla sólida Filament PBR y estructura de alambre CAD (Wireframe).
 * - HUD con cotas métricas calibradas por tensión (mm) y telemetría de rendimiento a 60 FPS (RF-3.5).
 * - Botones de orientación rápida de cámara (Iso, Frontal, Lateral, Superior, Reset).
 * - Soporte para maximizar a pantalla completa o incrustar en el panel adaptativo de tablet.
 */
@Composable
fun CrochetViewportScreen(
    state: ViewportState,
    onIntent: (ViewportIntent) -> Unit,
    modifier: Modifier = Modifier,
    onCloseFullscreen: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StitchMeshNeutralDark)
    ) {
        if (state.meshGeometry != null) {
            // Renderizado nativo 3D con Google Filament y shader Wool PBR
            CrochetViewport3D(
                meshGeometry = state.meshGeometry,
                cameraPreset = state.selectedCameraPreset,
                isWireframe = state.isWireframe,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Lienzo CAD 2D preliminar si no hay malla triangulada
            CadInteractiveCanvas(
                state = state,
                onOrbitRotate = { dy, dp -> onIntent(ViewportIntent.UpdateOrbit(dy, dp)) },
                onZoomChange = { zoom -> onIntent(ViewportIntent.UpdateZoom(zoom)) },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay central informativo para estado sin malla
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(StitchMeshSurfaceHigh.copy(alpha = 0.85f))
                        .border(1.dp, StitchMeshTerracotta.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "Visor 3D Filament",
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
                    text = if (state.isWireframe) "Modo Alambre CAD (Wireframe)" else "Motor Filament PBR (Malla de lana)",
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchMeshTextSecondary
                )
            }
        }

        // Overlay HUD superior: cotas métricas (mm/cm), telemetría gráfica y presets de cámara
        ViewportHudOverlay(
            dimensions = state.dimensions,
            telemetry = state.telemetry,
            selectedCameraPreset = state.selectedCameraPreset,
            onCameraPresetSelected = { onIntent(ViewportIntent.SelectCameraPreset(it)) },
            isFullscreen = state.isFullscreen,
            showDimensionCallouts = state.showDimensionCallouts,
            useCentimeters = state.useCentimeters,
            onToggleDimensionCallouts = { onIntent(ViewportIntent.ToggleDimensionCallouts) },
            onToggleUnitSystem = { onIntent(ViewportIntent.ToggleUnitSystem) },
            onToggleFullscreen = { onIntent(ViewportIntent.ToggleFullscreen) },
            onBackClick = if (state.isFullscreen) onCloseFullscreen else null
        )

        // Overlay inferior: PeelSliderBar interactiva con toggle de wireframe
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            PeelSliderBar(
                currentRound = state.currentPeelRound,
                totalRounds = state.totalRounds,
                onRoundSelected = { onIntent(ViewportIntent.SelectPeelRound(it)) },
                isWireframe = state.isWireframe,
                onWireframeChange = { onIntent(ViewportIntent.ToggleWireframe(it)) }
            )
        }
    }
}

/**
 * Canvas de referencia CAD que responde a gestos táctiles de arrastre y zoom por pellizco.
 */
@Composable
private fun CadInteractiveCanvas(
    state: ViewportState,
    onOrbitRotate: (deltaYaw: Float, deltaPitch: Float) -> Unit,
    onZoomChange: (scaleMultiplier: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridBorderColor = StitchMeshSurfaceBorder
    val terracottaColor = StitchMeshTerracotta
    val yarnGoldColor = StitchMeshYarnGold
    val wireframeColor = StitchMeshSageGreen

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (zoom != 1f) {
                        onZoomChange(zoom)
                    }
                    if (pan != Offset.Zero) {
                        onOrbitRotate(pan.x * 0.35f, -pan.y * 0.35f)
                    }
                }
            }
    ) {
        val centerX = size.width / 2f + state.panOffset.x
        val centerY = size.height / 2f + state.panOffset.y
        val scale = state.zoomScale

        // Dibujar cuadrícula isométrica de fondo
        val gridSize = 40.dp.toPx() * scale
        val gridLines = 14
        val halfW = (gridLines * gridSize) / 2f

        for (i in -gridLines..gridLines) {
            val offset = i * gridSize
            // Líneas horizontales
            drawLine(
                color = gridBorderColor.copy(alpha = 0.25f),
                start = Offset(centerX - halfW, centerY + offset),
                end = Offset(centerX + halfW, centerY + offset),
                strokeWidth = 1f
            )
            // Líneas verticales
            drawLine(
                color = gridBorderColor.copy(alpha = 0.25f),
                start = Offset(centerX + offset, centerY - halfW),
                end = Offset(centerX + offset, centerY + halfW),
                strokeWidth = 1f
            )
        }

        // Representación paramétrica de anillos de vueltas peladas (Peel slider)
        val activeRounds = state.currentPeelRound
        for (round in 1..activeRounds) {
            val ringRadius = (round * 28.dp.toPx() * scale).coerceAtLeast(10f)
            val ringColor = if (state.isWireframe) {
                wireframeColor.copy(alpha = (round.toFloat() / activeRounds.toFloat()).coerceIn(0.3f, 0.9f))
            } else {
                if (round == activeRounds) terracottaColor else yarnGoldColor.copy(alpha = 0.5f)
            }

            drawCircle(
                color = ringColor,
                radius = ringRadius,
                center = Offset(centerX, centerY),
                style = Stroke(
                    width = if (round == activeRounds) 3.dp.toPx() else 1.5.dp.toPx()
                )
            )

            // Si está en wireframe, dibuja subdivisiones radiales para simular teselación de puntos
            if (state.isWireframe) {
                val segments = (round * 6).coerceIn(6, 36)
                for (s in 0 until segments) {
                    val angle = (s.toFloat() / segments) * 2f * Math.PI.toFloat()
                    val pX = centerX + ringRadius * cos(angle)
                    val pY = centerY + ringRadius * sin(angle)
                    drawLine(
                        color = wireframeColor.copy(alpha = 0.35f),
                        start = Offset(centerX, centerY),
                        end = Offset(pX, pY),
                        strokeWidth = 1f
                    )
                }
            }
        }
    }
}

@Preview(name = "CrochetViewportScreen Dual-Pane Preview", showBackground = true, widthDp = 800, heightDp = 600)
@Composable
private fun CrochetViewportScreenPreview() {
    StitchMesh3DTheme {
        CrochetViewportScreen(
            state = ViewportState(
                currentPeelRound = 3,
                totalRounds = 4,
                isWireframe = false,
                dimensions = ViewportDimensionsUiModel(68f, 42f, 68f),
                telemetry = ViewportTelemetryUiModel(
                    polygonCount = 720,
                    vertexCount = 392,
                    fps = 60,
                    tensionGaugeLabel = "#4 Worsted · 3.5 mm"
                )
            ),
            onIntent = {}
        )
    }
}

@Preview(name = "CrochetViewportScreen Fullscreen Wireframe Preview", showBackground = true, widthDp = 1000, heightDp = 700)
@Composable
private fun CrochetViewportScreenFullscreenPreview() {
    StitchMesh3DTheme {
        CrochetViewportScreen(
            state = ViewportState(
                isFullscreen = true,
                currentPeelRound = 2,
                totalRounds = 5,
                isWireframe = true,
                selectedCameraPreset = CameraPreset.FRONT,
                dimensions = ViewportDimensionsUiModel(54f, 30f, 54f),
                telemetry = ViewportTelemetryUiModel(
                    polygonCount = 360,
                    vertexCount = 196,
                    fps = 60,
                    tensionGaugeLabel = "#4 Worsted · 3.5 mm"
                )
            ),
            onIntent = {},
            onCloseFullscreen = {}
        )
    }
}
