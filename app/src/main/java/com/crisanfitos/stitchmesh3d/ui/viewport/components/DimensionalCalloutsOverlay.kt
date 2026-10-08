package com.crisanfitos.stitchmesh3d.ui.viewport.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold

/**
 * Capa de cotas dimensionales métricas estilo CAD sobre el visor 3D paramétrico (RF-2.2, RF-3.3).
 *
 * Muestra:
 * - Cota vertical (eje Z): altura total en mm / cm.
 * - Cota horizontal (eje X/Y): diámetro y anchura máxima en mm / cm.
 * - Indicador de sistema de referencia y escala métrica.
 */
@Composable
fun DimensionalCalloutsOverlay(
    dimensions: ViewportDimensionsUiModel,
    modifier: Modifier = Modifier,
    useCentimeters: Boolean = false,
    visible: Boolean = true,
    onToggleUnit: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val lineColor = StitchMeshTerracotta.copy(alpha = 0.65f)
            val tickColor = StitchMeshYarnGold.copy(alpha = 0.85f)

            // 1. Lienzo gráfico para las líneas y flechas de cota
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 36.dp, top = 64.dp, bottom = 48.dp)
            ) {
                val strokeWidthPx = 1.5.dp.toPx()
                val tickLengthPx = 12.dp.toPx()
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)

                val rightX = size.width - 8.dp.toPx()
                val topY = 16.dp.toPx()
                val bottomY = size.height - 32.dp.toPx()

                // --- COTA VERTICAL (ALTURA Z) ---
                // Línea principal vertical discontinua
                drawLine(
                    color = lineColor,
                    start = Offset(rightX, topY),
                    end = Offset(rightX, bottomY),
                    strokeWidth = strokeWidthPx,
                    pathEffect = dashEffect,
                    cap = StrokeCap.Round
                )
                // Delimitador superior
                drawLine(
                    color = tickColor,
                    start = Offset(rightX - tickLengthPx, topY),
                    end = Offset(rightX + 4.dp.toPx(), topY),
                    strokeWidth = strokeWidthPx * 1.5f,
                    cap = StrokeCap.Round
                )
                // Delimitador inferior
                drawLine(
                    color = tickColor,
                    start = Offset(rightX - tickLengthPx, bottomY),
                    end = Offset(rightX + 4.dp.toPx(), bottomY),
                    strokeWidth = strokeWidthPx * 1.5f,
                    cap = StrokeCap.Round
                )

                // --- COTA HORIZONTAL (DIÁMETRO / ANCHO X/Y) ---
                val leftX = 32.dp.toPx()
                val horizY = size.height - 8.dp.toPx()
                val rightHorizX = size.width - 32.dp.toPx()

                // Línea principal horizontal discontinua
                drawLine(
                    color = lineColor,
                    start = Offset(leftX, horizY),
                    end = Offset(rightHorizX, horizY),
                    strokeWidth = strokeWidthPx,
                    pathEffect = dashEffect,
                    cap = StrokeCap.Round
                )
                // Delimitador izquierdo
                drawLine(
                    color = tickColor,
                    start = Offset(leftX, horizY - tickLengthPx),
                    end = Offset(leftX, horizY + 4.dp.toPx()),
                    strokeWidth = strokeWidthPx * 1.5f,
                    cap = StrokeCap.Round
                )
                // Delimitador derecho
                drawLine(
                    color = tickColor,
                    start = Offset(rightHorizX, horizY - tickLengthPx),
                    end = Offset(rightHorizX, horizY + 4.dp.toPx()),
                    strokeWidth = strokeWidthPx * 1.5f,
                    cap = StrokeCap.Round
                )
            }

            // 2. Chip flotante de Cota Vertical (Altura Z) en el lateral derecho
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StitchMeshSurfaceContainer.copy(alpha = 0.92f))
                    .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(8.dp))
                    .then(if (onToggleUnit != null) Modifier.clickable(onClick = onToggleUnit) else Modifier)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "ALTO (Z)",
                        style = CrochetTypography.tokenBadge,
                        color = StitchMeshTextSecondary,
                        fontSize = 9.sp
                    )
                    Text(
                        text = if (useCentimeters) {
                            "%.1f cm".format(java.util.Locale.US, dimensions.heightMm / 10f)
                        } else {
                            "${dimensions.heightMm.toInt()} mm"
                        },
                        style = CrochetTypography.matrixValue,
                        color = StitchMeshTerracotta,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // 3. Chip flotante de Cota Horizontal (Diámetro / Ancho) en la parte inferior central
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StitchMeshSurfaceContainer.copy(alpha = 0.92f))
                    .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(8.dp))
                    .then(if (onToggleUnit != null) Modifier.clickable(onClick = onToggleUnit) else Modifier)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = "Cota de diámetro",
                        tint = StitchMeshYarnGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "DIÁM. (Ø / X-Y):",
                        style = CrochetTypography.tokenBadge,
                        color = StitchMeshTextSecondary,
                        fontSize = 9.sp
                    )
                    Text(
                        text = if (useCentimeters) {
                            "%.1f cm".format(java.util.Locale.US, dimensions.maxDiameterMm / 10f)
                        } else {
                            "${dimensions.maxDiameterMm.toInt()} mm"
                        },
                        style = CrochetTypography.matrixValue,
                        color = StitchMeshTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // 4. Indicador de sistema métrico y calibración (esquina inferior izquierda)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(StitchMeshSurfaceHigh.copy(alpha = 0.85f))
                    .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(6.dp))
                    .then(if (onToggleUnit != null) Modifier.clickable(onClick = onToggleUnit) else Modifier)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (useCentimeters) "CAD: CM (Toca para MM)" else "CAD: MM (Toca para CM)",
                    style = CrochetTypography.tokenBadge,
                    color = StitchMeshYarnGold,
                    fontSize = 9.sp
                )
            }
        }
    }
}
