package com.crisanfitos.stitchmesh3d.ui.export.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSageGreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceLowest
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * Nivel semántico de una línea de telemetría durante la compilación de activos (SM-057).
 */
enum class TelemetryLogLevel(val prefix: String) {
    SUCCESS("[✓]"),
    ACTIVE("[⚡]"),
    INFO("[i]"),
    ERROR("[!]")
}

/**
 * Entrada individual de la consola de telemetría de exportación.
 */
data class TelemetryLogEntry(
    val message: String,
    val level: TelemetryLogLevel = TelemetryLogLevel.INFO,
    val timestampLabel: String? = null
) {
    val formattedLine: String
        get() = buildString {
            if (!timestampLabel.isNullOrBlank()) {
                append(timestampLabel).append(' ')
            }
            append(level.prefix).append(' ').append(message)
        }
}

/**
 * Consola terminal monoespaciada con autoscroll automático al recibir nuevas entradas (SM-057).
 */
@Composable
fun TelemetryLogConsole(
    logs: List<TelemetryLogEntry>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.lastIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(StitchMeshSurfaceLowest)
            .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
            .testTag("telemetry_log_console")
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(logs) { index, entry ->
                val textColor = when (entry.level) {
                    TelemetryLogLevel.SUCCESS -> StitchMeshSageGreen
                    TelemetryLogLevel.ACTIVE -> StitchMeshTerracotta
                    TelemetryLogLevel.INFO -> StitchMeshTextSecondary
                    TelemetryLogLevel.ERROR -> StitchMeshCoralRed
                }
                Text(
                    text = entry.formattedLine,
                    style = CrochetTypography.codeInstruction,
                    color = if (index == logs.lastIndex && entry.level == TelemetryLogLevel.ACTIVE) {
                        StitchMeshTextPrimary
                    } else {
                        textColor
                    }
                )
            }
        }
    }
}

@Preview(name = "TelemetryLogConsole Dark", showBackground = true)
@Composable
private fun TelemetryLogConsolePreview() {
    StitchMesh3DTheme {
        TelemetryLogConsole(
            logs = listOf(
                TelemetryLogEntry("Validador formal verificado (0 errores)", TelemetryLogLevel.SUCCESS),
                TelemetryLogEntry("Aplicando Muestra de tensión (3.2 x 3.0 mm)", TelemetryLogLevel.SUCCESS),
                TelemetryLogEntry("Generando geometría OBJ (14,820 caras)...", TelemetryLogLevel.ACTIVE)
            )
        )
    }
}
