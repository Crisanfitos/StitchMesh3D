package com.crisanfitos.stitchmesh3d.ui.tension.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crisanfitos.stitchmesh3d.ui.tension.TensionDiagnosis
import com.crisanfitos.stitchmesh3d.ui.tension.TensionScaleComparison
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshNeutralDark
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSageGreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextDisabled
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold

/**
 * Tarjeta de comparativa visual paramétrica del tejido (SM-052).
 * Muestra el impacto porcentual de la tensión física en la silueta, volumen y cotas dimensionales.
 */
@Composable
fun TensionScaleComparisonCard(
    comparison: TensionScaleComparison,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StitchMeshSurfaceHigh),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StitchMeshSurfaceBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Cabecera con título y badge de diagnóstico
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Comparativa de Escala Paramétrica",
                        style = MaterialTheme.typography.titleMedium,
                        color = StitchMeshTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Silueta real vs estándar CYC teórico",
                        style = MaterialTheme.typography.labelSmall,
                        color = StitchMeshTextSecondary,
                        fontSize = 11.sp
                    )
                }

                DiagnosisBadge(diagnosis = comparison.diagnosis)
            }

            // 2. Gráfico de silueta comparativa
            TensionScaleComparisonCanvas(comparison = comparison)

            // Leyenda de silueta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp, 3.dp)
                            .background(StitchMeshTextDisabled, shape = RoundedCornerShape(1.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Estándar CYC (Teórico)",
                        style = MaterialTheme.typography.labelSmall,
                        color = StitchMeshTextSecondary,
                        fontSize = 11.sp
                    )
                }

                val activeColor = when (comparison.diagnosis) {
                    TensionDiagnosis.BALANCED -> StitchMeshSageGreen
                    TensionDiagnosis.LOOSE -> StitchMeshTerracotta
                    TensionDiagnosis.TIGHT -> StitchMeshYarnGold
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp, 4.dp)
                            .background(activeColor, shape = RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tu Muestra (Calibrada)",
                        style = MaterialTheme.typography.labelSmall,
                        color = StitchMeshTextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }

            // 3. Grid de métricas de desviación porcentual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricDeviationBox(
                    label = "Escala Ancho",
                    percent = comparison.widthDeviationPercent,
                    modifier = Modifier.weight(1f)
                )
                MetricDeviationBox(
                    label = "Escala Alto",
                    percent = comparison.heightDeviationPercent,
                    modifier = Modifier.weight(1f)
                )
                MetricDeviationBox(
                    label = "Volumen Total",
                    percent = comparison.volumeDeviationPercent,
                    modifier = Modifier.weight(1.15f),
                    isVolume = true
                )
            }

            // 4. Cotas estimadas para pieza de referencia (30p x 30v)
            val theoW = "%.0f".format(comparison.theoreticalDimensionsMm.first)
            val theoH = "%.0f".format(comparison.theoreticalDimensionsMm.second)
            val calW = "%.0f".format(comparison.calibratedDimensionsMm.first)
            val calH = "%.0f".format(comparison.calibratedDimensionsMm.second)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StitchMeshSurfaceContainer, shape = RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pieza de referencia (30p × 30v)",
                            style = MaterialTheme.typography.labelSmall,
                            color = StitchMeshTextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Teórico: $theoW × $theoH mm",
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchMeshTextDisabled,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "➔ $calW × $calH mm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StitchMeshTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // 5. Diagnóstico de impacto en amigurumi y sugerencia de aguja
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StitchMeshNeutralDark.copy(alpha = 0.6f), shape = RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = StitchMeshTerracotta,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = comparison.diagnosisSummary,
                        style = MaterialTheme.typography.labelMedium,
                        color = StitchMeshTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (comparison.hookSuggestion != null) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = StitchMeshSageGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = comparison.hookSuggestion,
                            style = MaterialTheme.typography.labelSmall,
                            color = StitchMeshSageGreen,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosisBadge(diagnosis: TensionDiagnosis) {
    val (label, bg, fg) = when (diagnosis) {
        TensionDiagnosis.BALANCED -> Triple(
            "Tensión Equilibrada",
            StitchMeshSageGreen.copy(alpha = 0.16f),
            StitchMeshSageGreen
        )
        TensionDiagnosis.LOOSE -> Triple(
            "Tensión Holgada",
            StitchMeshTerracotta.copy(alpha = 0.16f),
            StitchMeshTerracotta
        )
        TensionDiagnosis.TIGHT -> Triple(
            "Tensión Apretada",
            StitchMeshYarnGold.copy(alpha = 0.16f),
            StitchMeshYarnGold
        )
    }

    Box(
        modifier = Modifier
            .background(bg, shape = RoundedCornerShape(8.dp))
            .border(1.dp, fg.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun MetricDeviationBox(
    label: String,
    percent: Float,
    modifier: Modifier = Modifier,
    isVolume: Boolean = false
) {
    val sign = if (percent >= 0f) "+" else ""
    val formatted = "$sign${"%.1f".format(percent)}%"

    val textColor = when {
        isVolume && percent > 5f -> StitchMeshTerracotta
        isVolume && percent < -5f -> StitchMeshYarnGold
        isVolume -> StitchMeshSageGreen
        percent > 0f -> StitchMeshTerracotta
        percent < 0f -> StitchMeshYarnGold
        else -> StitchMeshTextPrimary
    }

    Box(
        modifier = modifier
            .background(StitchMeshSurfaceContainer, shape = RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = StitchMeshTextSecondary,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = formatted,
                style = MaterialTheme.typography.titleSmall,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = if (isVolume) 14.sp else 13.sp
            )
        }
    }
}
