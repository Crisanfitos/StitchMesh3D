package com.crisanfitos.stitchmesh3d.ui.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshNeutralDark
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * Modelo de datos visual para representar un proyecto en la tarjeta de biblioteca.
 */
data class ProjectCardUiModel(
    val id: String,
    val title: String,
    val lastModified: String,
    val yarnWeightLabel: String,
    val hookSizeMm: Float,
    val roundCount: Int,
    val stitchCount: Int,
    val isLinterValid: Boolean,
    val errorCount: Int = 0
)

/**
 * Tarjeta de proyecto de crochet para la biblioteca y dashboard.
 * Ofrece vista previa técnica, metadatos de tensión, recuento de puntos y estado de validación formal.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectCard(
    project: ProjectCardUiModel,
    onClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = StitchMeshSurfaceBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = StitchMeshSurfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Contenedor de miniatura 3D / viewport CAD placeholder
            ThumbnailViewportPlaceholder(
                roundCount = project.roundCount,
                stitchCount = project.stitchCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            // Contenido descriptivo y metadatos
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cabecera: Título y menú de opciones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = project.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = StitchMeshTextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = project.lastModified,
                            style = MaterialTheme.typography.labelSmall,
                            color = StitchMeshTextSecondary
                        )
                    }

                    IconButton(
                        onClick = onMoreOptionsClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones de proyecto",
                            tint = StitchMeshTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Fila de chips de metadatos (Tensión, Linter, Recuento)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Estado del linter
                    val linterStatus = if (project.isLinterValid) {
                        LinterValidationStatus.VALID
                    } else {
                        LinterValidationStatus.HAS_ERRORS
                    }
                    val linterLabel = if (project.isLinterValid) {
                        "Válido"
                    } else {
                        if (project.errorCount > 0) "${project.errorCount} errores" else "Con errores"
                    }

                    StatusBadgeChip(
                        status = linterStatus,
                        customLabel = linterLabel
                    )

                    // Calibre y aguja de tensión
                    TensionBadgeChip(
                        yarnWeightLabel = project.yarnWeightLabel,
                        hookSizeMm = project.hookSizeMm
                    )

                    // Chip de recuento de vueltas y puntos
                    RoundCountBadgeChip(
                        roundCount = project.roundCount,
                        stitchCount = project.stitchCount
                    )
                }
            }
        }
    }
}

/**
 * Miniatura con efecto CAD paramétrico que dibuja una rejilla isométrica abstracta de crochet.
 */
@Composable
private fun ThumbnailViewportPlaceholder(
    roundCount: Int,
    stitchCount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(StitchMeshNeutralDark)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val gridColor = StitchMeshSurfaceBorder.copy(alpha = 0.6f)
            val accentLineColor = StitchMeshTerracotta.copy(alpha = 0.4f)
            val step = 20.dp.toPx()

            // Rejilla técnica de fondo
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
                x += step
            }

            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
                y += step
            }

            // Anillos concéntricos simulando malla 3D de crochet paramétrico
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = accentLineColor,
                radius = 35.dp.toPx(),
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
            drawCircle(
                color = accentLineColor.copy(alpha = 0.7f),
                radius = 20.dp.toPx(),
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
            drawCircle(
                color = StitchMeshTerracotta,
                radius = 4.dp.toPx(),
                center = center
            )
        }

        // Insignia técnica flotante sobre el viewport (3D CAD Preview)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(StitchMeshSurfaceHigh.copy(alpha = 0.85f))
                .border(width = 1.dp, color = StitchMeshSurfaceBorder, shape = RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "3D MESH",
                style = CrochetTypography.tokenBadge,
                color = StitchMeshTextSecondary
            )
        }
    }
}

/**
 * Chip para recuento de vueltas y puntos.
 */
@Composable
private fun RoundCountBadgeChip(
    roundCount: Int,
    stitchCount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(StitchMeshSurfaceHigh)
            .border(width = 1.dp, color = StitchMeshSurfaceBorder, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$roundCount vtas • $stitchCount pts",
            style = CrochetTypography.tokenBadge,
            color = StitchMeshTextSecondary
        )
    }
}

@Preview(name = "ProjectCard Valid - Dark", showBackground = true)
@Composable
private fun ProjectCardValidPreview() {
    StitchMesh3DTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ProjectCard(
                project = ProjectCardUiModel(
                    id = "proj-001",
                    title = "Oso Amigurumi - Cabeza",
                    lastModified = "Modificado hace 15m",
                    yarnWeightLabel = "#4 Worsted",
                    hookSizeMm = 3.5f,
                    roundCount = 18,
                    stitchCount = 108,
                    isLinterValid = true
                ),
                onClick = {},
                onMoreOptionsClick = {}
            )
        }
    }
}

@Preview(name = "ProjectCard With Errors - Dark", showBackground = true)
@Composable
private fun ProjectCardWithErrorsPreview() {
    StitchMesh3DTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ProjectCard(
                project = ProjectCardUiModel(
                    id = "proj-002",
                    title = "Dragón Articulado - Cuerpo",
                    lastModified = "Modificado ayer",
                    yarnWeightLabel = "#3 DK",
                    hookSizeMm = 3.0f,
                    roundCount = 42,
                    stitchCount = 280,
                    isLinterValid = false,
                    errorCount = 3
                ),
                onClick = {},
                onMoreOptionsClick = {}
            )
        }
    }
}
