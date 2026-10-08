package com.crisanfitos.stitchmesh3d.ui.viewport.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 * Modelo de datos para las cotas dimensionales métricas proyectadas (mm).
 */
data class ViewportDimensionsUiModel(
    val widthMm: Float,
    val heightMm: Float,
    val depthMm: Float
) {
    /**
     * Formato requerido: '124 mm × 98 mm × 110 mm'
     */
    fun formattedDimensions(): String {
        return "${widthMm.toInt()} mm × ${heightMm.toInt()} mm × ${depthMm.toInt()} mm"
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
    onToggleFullscreen: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Esquina superior izquierda: Cotas métricas y telemetría
        Column(
            modifier = Modifier.align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Ficha de cotas dimensionales
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(StitchMeshSurfaceContainer.copy(alpha = 0.90f))
                    .border(
                        width = 1.dp,
                        color = StitchMeshSurfaceBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
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
                        text = dimensions.formattedDimensions(),
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

        // Esquina superior derecha: Orientación de cámara y botón fullscreen
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CameraOrientationPillRow(
                selectedPreset = selectedCameraPreset,
                onPresetSelected = onCameraPresetSelected
            )

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
