package com.crisanfitos.stitchmesh3d.ui.tension.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshNeutralDark
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextDisabled
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * Representación visual esquemática de la muestra física de 10×10 cm.
 * Muestra las cotas perimetrales (100 mm) y dibuja una cuadrícula proporcional
 * a los puntos y vueltas contados para dar intuición gráfica inmediata sobre la densidad.
 */
@Composable
fun TensionSwatchPreview(
    stitchesCount: Int,
    roundsCount: Int,
    stitchWidthMm: Float,
    stitchHeightMm: Float,
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
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Esquema Geométrico de Muestra",
                    style = MaterialTheme.typography.titleMedium,
                    color = StitchMeshTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "${stitchesCount * roundsCount} puntos / 100 cm²",
                    style = MaterialTheme.typography.labelSmall,
                    color = StitchMeshTerracotta,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Lienzo de la Muestra con Cuadrícula (acotado para ergonomía en tablets y móviles)
            Box(
                modifier = Modifier
                    .sizeIn(maxWidth = 260.dp, maxHeight = 260.dp)
                    .fillMaxWidth(0.6f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StitchMeshNeutralDark)
                    .border(1.5.dp, StitchMeshTerracotta.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                    val w = size.width
                    val h = size.height

                    // Dibuja el marco interior
                    drawRect(
                        color = Color(0xFF1E1F24),
                        topLeft = Offset.Zero,
                        size = size
                    )

                    // Líneas verticales (puntos)
                    val visibleCols = stitchesCount.coerceIn(2, 40)
                    val colStep = w / visibleCols.toFloat()
                    for (i in 1 until visibleCols) {
                        val x = i * colStep
                        drawLine(
                            color = StitchMeshSurfaceBorder.copy(alpha = 0.5f),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1f
                        )
                    }

                    // Líneas horizontales (vueltas)
                    val visibleRows = roundsCount.coerceIn(2, 40)
                    val rowStep = h / visibleRows.toFloat()
                    for (j in 1 until visibleRows) {
                        val y = j * rowStep
                        drawLine(
                            color = StitchMeshSurfaceBorder.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    // Borde exterior
                    drawRect(
                        color = StitchMeshTerracotta.copy(alpha = 0.7f),
                        topLeft = Offset.Zero,
                        size = size,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                }

                // Cota de Ancho en la parte superior
                Text(
                    text = "100 mm (10 cm)",
                    color = StitchMeshTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                )

                // Cota de Alto en el lateral
                Text(
                    text = "100 mm",
                    color = StitchMeshTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Leyenda de densidad
            Text(
                text = "Densidad: ${stitchesCount} pt × ${roundsCount} vtas en ventana de 10×10 cm",
                style = MaterialTheme.typography.bodySmall,
                color = StitchMeshTextDisabled,
                fontSize = 12.sp
            )
        }
    }
}
