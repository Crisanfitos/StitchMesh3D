package com.crisanfitos.stitchmesh3d.ui.export.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshOnAccent
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * Niveles de resolución de teselación de malla para exportación 3D (SM-056, RF-5.3).
 */
enum class MeshResolutionLevel(
    val labelEs: String,
    val subtitleEs: String,
    val subdivisionMultiplier: Int
) {
    LOW("Baja", "Ligera (~5k caras)", 1),
    MEDIUM("Media", "Estándar (~15k caras)", 2),
    HIGH_CAD("Alta / CAD", "Detalle máximo (~45k caras)", 4)
}

/**
 * Selector segmentado de resolución de malla 3D para exportación técnica (Stitch Screen 3aedf2f3).
 */
@Composable
fun MeshResolutionSelector(
    selectedResolution: MeshResolutionLevel,
    onResolutionSelected: (MeshResolutionLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Resolución de Malla 3D",
            style = MaterialTheme.typography.labelMedium,
            color = StitchMeshTextSecondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MeshResolutionLevel.entries.forEach { level ->
                val isSelected = level == selectedResolution
                val bgColor = if (isSelected) StitchMeshTerracotta else StitchMeshSurfaceHigh
                val borderColor = if (isSelected) StitchMeshTerracotta else StitchMeshSurfaceBorder
                val titleColor = if (isSelected) StitchMeshOnAccent else StitchMeshTextPrimary
                val subColor = if (isSelected) StitchMeshOnAccent else StitchMeshTextSecondary

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                        .clickable { onResolutionSelected(level) }
                        .padding(vertical = 10.dp, horizontal = 8.dp)
                        .testTag("mesh_resolution_${level.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = level.labelEs,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                    Text(
                        text = level.subtitleEs,
                        style = MaterialTheme.typography.labelSmall,
                        color = subColor
                    )
                }
            }
        }
    }
}

@Preview(name = "MeshResolutionSelector Dark", showBackground = true)
@Composable
private fun MeshResolutionSelectorPreview() {
    StitchMesh3DTheme {
        MeshResolutionSelector(
            selectedResolution = MeshResolutionLevel.MEDIUM,
            onResolutionSelected = {}
        )
    }
}
