package com.crisanfitos.stitchmesh3d.ui.export.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceLowest
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import kotlin.math.roundToInt

/**
 * Estado del job de compilación y exportación técnica con porcentaje y telemetría (SM-057).
 */
data class ExportProgressState(
    val progressFraction: Float = 0f,
    val currentStageTitle: String = "Preparando compilación…",
    val logs: List<TelemetryLogEntry> = emptyList(),
    val isCancelled: Boolean = false
) {
    val clampedProgress: Float
        get() = progressFraction.coerceIn(0f, 1f)

    val progressPercent: Int
        get() = (clampedProgress * 100f).roundToInt()

    fun appendLog(message: String, level: TelemetryLogLevel, newProgress: Float = clampedProgress): ExportProgressState {
        return copy(
            progressFraction = newProgress.coerceIn(0f, 1f),
            currentStageTitle = message,
            logs = logs + TelemetryLogEntry(message = message, level = level)
        )
    }
}

/**
 * Diálogo interactivo de progreso de compilación técnica con barra porcentual animada
 * y consola de telemetría en tiempo real (Stitch Screen ae5d26df, SM-057).
 */
@Composable
fun ExportProgressDialog(
    state: ExportProgressState,
    onCancelExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.clampedProgress,
        label = "exportProgressAnimation"
    )

    Dialog(onDismissRequest = onCancelExport) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .testTag("export_progress_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = StitchMeshSurfaceContainer,
            border = BorderStroke(1.dp, StitchMeshSurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Compilando Entregables Técnicos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StitchMeshTextPrimary
                        )
                        Text(
                            text = state.currentStageTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchMeshTextSecondary
                        )
                    }
                    Text(
                        text = "${state.progressPercent}%",
                        style = CrochetTypography.hudDimensionValue,
                        color = StitchMeshTerracotta,
                        modifier = Modifier.testTag("export_progress_percent_text")
                    )
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("export_progress_bar"),
                    color = StitchMeshTerracotta,
                    trackColor = StitchMeshSurfaceLowest
                )

                TelemetryLogConsole(logs = state.logs)

                OutlinedButton(
                    onClick = onCancelExport,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, StitchMeshSurfaceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("export_progress_cancel_button")
                ) {
                    Text(
                        text = "Cancelar",
                        color = StitchMeshTextSecondary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Preview(name = "ExportProgressDialog Dark", showBackground = true)
@Composable
private fun ExportProgressDialogPreview() {
    StitchMesh3DTheme {
        ExportProgressDialog(
            state = ExportProgressState(
                progressFraction = 0.68f,
                currentStageTitle = "Generando geometría OBJ...",
                logs = listOf(
                    TelemetryLogEntry("Validador formal verificado", TelemetryLogLevel.SUCCESS),
                    TelemetryLogEntry("Generando geometría OBJ...", TelemetryLogLevel.ACTIVE)
                )
            ),
            onCancelExport = {}
        )
    }
}
