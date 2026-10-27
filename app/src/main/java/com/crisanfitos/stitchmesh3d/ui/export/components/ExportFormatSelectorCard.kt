package com.crisanfitos.stitchmesh3d.ui.export.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshOnAccent
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * Formatos técnicos disponibles para exportación (SM-056, RF-5.3).
 */
enum class ExportAssetFormat(val extension: String, val labelEs: String) {
    PDF_SHEET(".pdf", "Ficha Técnica PDF"),
    MARKDOWN_SHEET(".md", "Patrón Markdown"),
    OBJ_MESH(".obj", "Malla Wavefront OBJ + MTL"),
    GLTF_MESH(".gltf", "Escena 3D glTF 2.0")
}

/**
 * Estado inmutable de configuración de exportación técnica (SM-056).
 */
data class ExportConfigurationState(
    val selectedFormats: Set<ExportAssetFormat> = setOf(
        ExportAssetFormat.PDF_SHEET,
        ExportAssetFormat.OBJ_MESH
    ),
    val includeStitchSummary: Boolean = true,
    val includeYarnConsumption: Boolean = true,
    val includeIsometricPreview: Boolean = true,
    val meshResolution: MeshResolutionLevel = MeshResolutionLevel.MEDIUM
) {
    val canStartExport: Boolean
        get() = selectedFormats.isNotEmpty()

    fun toggleFormat(format: ExportAssetFormat): ExportConfigurationState {
        val next = selectedFormats.toMutableSet().apply {
            if (!add(format)) remove(format)
        }
        return copy(selectedFormats = next)
    }
}

/**
 * Tarjeta seleccionable para un formato o bloque de exportación técnica (Stitch Screen 3aedf2f3).
 * Utiliza borde de acento Terracotta y Checkbox de Material 3 sin colores fijos fuera del tema.
 */
@Composable
fun ExportFormatSelectorCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "export_format_card"
) {
    val borderColor = if (isSelected) StitchMeshTerracotta else StitchMeshSurfaceBorder
    val containerColor = if (isSelected) StitchMeshSurfaceContainer else StitchMeshSurfaceHigh

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) StitchMeshTerracotta else StitchMeshTextSecondary,
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = StitchMeshTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = StitchMeshTextSecondary
            )
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = StitchMeshTerracotta,
                uncheckedColor = StitchMeshTextSecondary,
                checkmarkColor = StitchMeshOnAccent
            )
        )
    }
}

@Preview(name = "ExportFormatSelectorCard Dark", showBackground = true)
@Composable
private fun ExportFormatSelectorCardPreview() {
    StitchMesh3DTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExportFormatSelectorCard(
                title = "Ficha Técnica PDF / Markdown",
                subtitle = "Incluye tabla de vueltas, consumo de hilo y Muestra de tensión 10x10",
                icon = Icons.Default.Description,
                isSelected = true,
                onToggle = {}
            )
            ExportFormatSelectorCard(
                title = "Modelo 3D (.OBJ / .glTF)",
                subtitle = "Malla paramétrica con materiales PBR por pieza",
                icon = Icons.Default.ViewInAr,
                isSelected = false,
                onToggle = {}
            )
        }
    }
}
