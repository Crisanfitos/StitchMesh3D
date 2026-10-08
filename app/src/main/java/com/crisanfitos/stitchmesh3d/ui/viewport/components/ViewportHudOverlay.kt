package com.crisanfitos.stitchmesh3d.ui.viewport.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshNeutralDark
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold

/**
 * Modelo de datos para las cotas dimensionales métricas proyectadas (mm y cm).
 */
data class ViewportDimensionsUiModel(
    val widthMm: Float,
    val heightMm: Float,
    val depthMm: Float
) {
    val maxDiameterMm: Float get() = maxOf(widthMm, depthMm)

    /**
     * Formato requerido en milímetros: '124 mm × 98 mm × 110 mm'
     */
    fun formattedDimensions(): String {
        return "${widthMm.toInt()} mm × ${heightMm.toInt()} mm × ${depthMm.toInt()} mm"
    }

    /**
     * Formato en centímetros: '12.4 cm × 9.8 cm × 11.0 cm'
     */
    fun formattedDimensionsCm(): String {
        return "%.1f cm × %.1f cm × %.1f cm".format(
            java.util.Locale.US,
            widthMm / 10f,
            heightMm / 10f,
            depthMm / 10f
        )
    }

    /**
     * Formato simplificado de cota: 'Ø 124 mm · H 98 mm'
     */
    fun formattedDiameterAndHeight(): String {
        return "Ø ${maxDiameterMm.toInt()} mm · H ${heightMm.toInt()} mm"
    }
}

/**
 * Telemetría técnica de la malla 3D en tiempo real.
 */
data class ViewportTelemetryUiModel(
    val polygonCount: Int = 1240,
    val vertexCount: Int = 642,
    val fps: Int = 60,
    val tensionGaugeLabel: String = "#4 Worsted · 3.5 mm"
)

/**
 * Capa HUD superpuesta flotante para el visor 3D paramétrico.
 *
 * Muestra cotas dimensionales calibradas por tensión (RF-2.2, RF-3.3),
 * telemetría gráfica en tiempo real y selector de orientación de cámara.
 */
@Composable
fun ViewportHudOverlay(
    dimensions: ViewportDimensionsUiModel,
    telemetry: ViewportTelemetryUiModel,
    selectedCameraPreset: CameraPreset,
    onCameraPresetSelected: (CameraPreset) -> Unit,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    showDimensionCallouts: Boolean = true,
    useCentimeters: Boolean = false,
    onToggleDimensionCallouts: (() -> Unit)? = null,
    onToggleUnitSystem: (() -> Unit)? = null,
    onToggleFullscreen: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Líneas de cota proyectadas sobre el canvas 3D (CAD Callouts)
        DimensionalCalloutsOverlay(
            dimensions = dimensions,
            useCentimeters = useCentimeters,
            visible = showDimensionCallouts,
            onToggleUnit = onToggleUnitSystem
        )

        // Esquina superior izquierda: Cotas métricas y telemetría
        Column(
            modifier = Modifier.align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Botón de retorno al editor si está activo (modo fullscreen)
            if (onBackClick != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchMeshSurfaceContainer.copy(alpha = 0.90f))
                        .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(8.dp))
                        .clickable(onClick = onBackClick)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al editor",
                            tint = StitchMeshTerracotta,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Volver al Editor",
                            style = CrochetTypography.tokenBadge,
                            color = StitchMeshTextPrimary
                        )
                    }
                }
            }

            // Ficha de cotas dimensionales
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(StitchMeshSurfaceContainer.copy(alpha = 0.90f))
                    .border(
                        width = 1.dp,
                        color = if (showDimensionCallouts) StitchMeshTerracotta.copy(alpha = 0.5f) else StitchMeshSurfaceBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .then(if (onToggleUnitSystem != null) Modifier.clickable(onClick = onToggleUnitSystem) else Modifier)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SquareFoot,
                        contentDescription = "Cotas métricas",
                        tint = StitchMeshTerracotta,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (useCentimeters) dimensions.formattedDimensionsCm() else dimensions.formattedDimensions(),
                        style = CrochetTypography.matrixValue,
                        color = StitchMeshTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Badge de telemetría de malla
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(StitchMeshSurfaceHigh.copy(alpha = 0.85f))
                    .border(
                        width = 1.dp,
                        color = StitchMeshSurfaceBorder,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${telemetry.polygonCount} tris · ${telemetry.vertexCount} verts · ${telemetry.fps} FPS",
                    style = CrochetTypography.tokenBadge,
                    color = StitchMeshYarnGold,
                    fontSize = 10.sp
                )
            }
        }

        // Esquina superior derecha: Orientación de cámara, botón cotas y botón fullscreen
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CameraOrientationPillRow(
                selectedPreset = selectedCameraPreset,
                onPresetSelected = onCameraPresetSelected
            )

            // Conmutador de cotas CAD
            if (onToggleDimensionCallouts != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (showDimensionCallouts) {
                                StitchMeshTerracotta.copy(alpha = 0.20f)
                            } else {
                                StitchMeshSurfaceContainer.copy(alpha = 0.90f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (showDimensionCallouts) StitchMeshTerracotta else StitchMeshSurfaceBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    IconButton(
                        onClick = onToggleDimensionCallouts,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SquareFoot,
                            contentDescription = if (showDimensionCallouts) "Ocultar cotas" else "Mostrar cotas",
                            tint = if (showDimensionCallouts) StitchMeshTerracotta else StitchMeshTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (onToggleFullscreen != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(StitchMeshSurfaceContainer.copy(alpha = 0.90f))
                        .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(10.dp))
                ) {
                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullscreen) "Salir de pantalla completa" else "Pantalla completa",
                            tint = StitchMeshTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "ViewportHudOverlay Preview", showBackground = true)
@Composable
private fun ViewportHudOverlayPreview() {
    StitchMesh3DTheme {
        Box(
            modifier = Modifier
                .background(StitchMeshNeutralDark)
                .size(600.dp, 400.dp)
        ) {
            ViewportHudOverlay(
                dimensions = ViewportDimensionsUiModel(
                    widthMm = 124f,
                    heightMm = 98f,
                    depthMm = 110f
                ),
                telemetry = ViewportTelemetryUiModel(
                    polygonCount = 1240,
                    vertexCount = 642,
                    fps = 60,
                    tensionGaugeLabel = "#4 Worsted · 3.5 mm"
                ),
                selectedCameraPreset = CameraPreset.ISOMETRIC,
                onCameraPresetSelected = {},
                isFullscreen = false,
                onToggleFullscreen = {}
            )
        }
    }
}
