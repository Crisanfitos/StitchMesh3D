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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
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
import com.crisanfitos.stitchmesh3d.core.gauge.model.TensionCalibrationResult
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshNeutralDark
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSageGreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextDisabled
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold
import java.util.Locale

/**
 * Tarjeta de telemetría dimensional que muestra los milímetros reales por punto y vuelta (RF-2.3),
 * así como la comparación con el estándar teórico CYC y el estado de tolerancia.
 */
@Composable
fun TensionMetricsCard(
    calibrationResult: TensionCalibrationResult?,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = StitchMeshSurfaceHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Telemetría Dimensional de Tensión",
                    style = MaterialTheme.typography.titleMedium,
                    color = StitchMeshTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                if (calibrationResult != null) {
                    val isTolerant = calibrationResult.isWithinStandardTolerance
                    val badgeBg = if (isTolerant) StitchMeshSageGreen.copy(alpha = 0.15f) else StitchMeshYarnGold.copy(alpha = 0.15f)
                    val badgeBorder = if (isTolerant) StitchMeshSageGreen else StitchMeshYarnGold
                    val badgeText = if (isTolerant) "Tolerancia Óptima" else "Tensión Ajustada"

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(badgeBg)
                            .border(1.dp, badgeBorder, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isTolerant) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = badgeBorder,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeBorder,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (errorMessage != null) {
                // Alerta de error
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchMeshCoralRed.copy(alpha = 0.15f))
                        .border(1.dp, StitchMeshCoralRed, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = StitchMeshCoralRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchMeshCoralRed
                    )
                }
            } else if (calibrationResult != null) {
                val std = calibrationResult.customStandard

                // Grid de métricas en 3 columnas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        title = "ANCHO / PUNTO",
                        symbol = "w",
                        valueMm = std.stitchWidthMm,
                        deviationPercent = calibrationResult.widthDeviationPercent,
                        modifier = Modifier.weight(1f)
                    )

                    MetricBox(
                        title = "ALTO / VUELTA",
                        symbol = "h",
                        valueMm = std.stitchHeightMm,
                        deviationPercent = calibrationResult.heightDeviationPercent,
                        modifier = Modifier.weight(1f)
                    )

                    MetricBox(
                        title = "GROSOR ESTIMADO",
                        symbol = "t",
                        valueMm = std.stitchThicknessMm,
                        deviationPercent = null,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Leyenda explicativa de tolerancia
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchMeshNeutralDark)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = StitchMeshTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val descText = if (calibrationResult.isWithinStandardTolerance) {
                        "La muestra coincide con el estándar CYC dentro de la tolerancia de ±15%."
                    } else {
                        val devW = calibrationResult.widthDeviationPercent
                        if (devW > 0) {
                            "Muestra más suelta (+${String.format(Locale.US, "%.1f", devW)}% ancho). El modelo 3D será ligeramente mayor al estándar."
                        } else {
                            "Muestra más tensa/apretada (${String.format(Locale.US, "%.1f", devW)}% ancho). El amigurumi final será más compacto."
                        }
                    }
                    Text(
                        text = descText,
                        style = MaterialTheme.typography.bodySmall,
                        color = StitchMeshTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    symbol: String,
    valueMm: Float,
    deviationPercent: Float?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(StitchMeshSurfaceContainer)
            .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = StitchMeshTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = String.format(Locale.US, "%.2f", valueMm),
                    style = CrochetTypography.matrixValue,
                    color = StitchMeshTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "mm",
                    style = MaterialTheme.typography.labelSmall,
                    color = StitchMeshTextDisabled,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            if (deviationPercent != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val isPositive = deviationPercent >= 0f
                val sign = if (isPositive) "+" else ""
                val devColor = if (kotlin.math.abs(deviationPercent) <= 15f) {
                    StitchMeshSageGreen
                } else {
                    StitchMeshYarnGold
                }

                Text(
                    text = "$sign${String.format(Locale.US, "%.1f", deviationPercent)}% vs CYC",
                    style = MaterialTheme.typography.labelSmall,
                    color = devColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
