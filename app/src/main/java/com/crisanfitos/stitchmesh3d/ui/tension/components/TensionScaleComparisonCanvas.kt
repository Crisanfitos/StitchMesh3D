package com.crisanfitos.stitchmesh3d.ui.tension.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.tension.TensionDiagnosis
import com.crisanfitos.stitchmesh3d.ui.tension.TensionScaleComparison
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSageGreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold

/**
 * Canvas de visualización comparativa paramétrica entre el estándar teórico CYC
 * y la escala real calculada a partir de la muestra de tensión del usuario.
 */
@Composable
fun TensionScaleComparisonCanvas(
    comparison: TensionScaleComparison,
    modifier: Modifier = Modifier
) {
    val activeColor = when (comparison.diagnosis) {
        TensionDiagnosis.BALANCED -> StitchMeshSageGreen
        TensionDiagnosis.LOOSE -> StitchMeshTerracotta
        TensionDiagnosis.TIGHT -> StitchMeshYarnGold
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(StitchMeshSurfaceContainer, shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(156.dp)) {
            val canvasW = size.width
            val canvasH = size.height
            val centerX = canvasW / 2f
            val centerY = canvasH / 2f

            // Dimensiones base teóricas del contorno de referencia (70% del espacio útil)
            val baseBoxW = canvasW * 0.45f
            val baseBoxH = canvasH * 0.55f

            // Dimensiones calibradas escaladas por la tensión real del usuario
            val clampedWidthRatio = comparison.widthScaleRatio.coerceIn(0.6f, 1.4f)
            val clampedHeightRatio = comparison.heightScaleRatio.coerceIn(0.6f, 1.4f)
            val calBoxW = baseBoxW * clampedWidthRatio
            val calBoxH = baseBoxH * clampedHeightRatio

            val cornerRadius = CornerRadius(16f, 16f)

            // 1. Contorno Teórico Estándar CYC (Línea discontinua)
            val theoreticalTopLeft = Offset(centerX - baseBoxW / 2f, centerY - baseBoxH / 2f)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

            drawRoundRect(
                color = StitchMeshSurfaceBorder,
                topLeft = theoreticalTopLeft,
                size = Size(baseBoxW, baseBoxH),
                cornerRadius = cornerRadius,
                style = Stroke(width = 2.5f, pathEffect = dashEffect)
            )

            // 2. Silueta Calibrada Real (Relleno translúcido y borde continuo)
            val calTopLeft = Offset(centerX - calBoxW / 2f, centerY - calBoxH / 2f)

            drawRoundRect(
                color = activeColor.copy(alpha = 0.14f),
                topLeft = calTopLeft,
                size = Size(calBoxW, calBoxH),
                cornerRadius = cornerRadius
            )

            drawRoundRect(
                color = activeColor,
                topLeft = calTopLeft,
                size = Size(calBoxW, calBoxH),
                cornerRadius = cornerRadius,
                style = Stroke(width = 3.5f)
            )

            // 3. Ejes de simetría centrales tenues
            drawLine(
                color = StitchMeshSurfaceBorder.copy(alpha = 0.4f),
                start = Offset(centerX, centerY - canvasH * 0.42f),
                end = Offset(centerX, centerY + canvasH * 0.42f),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
            drawLine(
                color = StitchMeshSurfaceBorder.copy(alpha = 0.4f),
                start = Offset(centerX - canvasW * 0.38f, centerY),
                end = Offset(centerX + canvasW * 0.38f, centerY),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }
    }
}
