package com.crisanfitos.stitchmesh3d.ui.dashboard.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSageGreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold

data class YarnPaletteColor(
    val name: String,
    val color: Color
)

val DefaultCrochetPalette = listOf(
    YarnPaletteColor("Terracotta", StitchMeshTerracotta),
    YarnPaletteColor("Verde Salvia", StitchMeshSageGreen),
    YarnPaletteColor("Dorado Hilado", StitchMeshYarnGold),
    YarnPaletteColor("Rojo Coral", StitchMeshCoralRed),
    YarnPaletteColor("Crema Natural", Color(0xFFF5EBE0)),
    YarnPaletteColor("Carbón Técnico", Color(0xFF2C2F38)),
    YarnPaletteColor("Azul Nórdico", Color(0xFF4A7C9B)),
    YarnPaletteColor("Rosa Empolvado", Color(0xFFD48B96))
)

/**
 * Selector horizontal de muestras de color de hilado para la pieza inicial.
 */
@Composable
fun PaletteSwatchPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
    palette: List<YarnPaletteColor> = DefaultCrochetPalette
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val activeSwatchName = palette.firstOrNull { it.color == selectedColor }?.name ?: "Personalizado"

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Color de Hilado Inicial",
                style = MaterialTheme.typography.labelMedium,
                color = StitchMeshTextSecondary
            )
            Text(
                text = activeSwatchName,
                style = MaterialTheme.typography.labelSmall,
                color = StitchMeshTextPrimary
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            palette.forEach { swatch ->
                val isSelected = swatch.color == selectedColor
                ColorSwatchCircle(
                    color = swatch.color,
                    label = swatch.name,
                    isSelected = isSelected,
                    onClick = { onColorSelected(swatch.color) }
                )
            }
        }
    }
}

@Composable
private fun ColorSwatchCircle(
    color: Color,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) StitchMeshTerracotta else StitchMeshSurfaceBorder
    val borderWidth = if (isSelected) 3.dp else 1.dp

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .border(width = borderWidth, color = borderColor, shape = CircleShape)
            .clickable(onClick = onClick)
            .padding(4.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = label,
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}

@Preview(name = "PaletteSwatchPicker - Dark", showBackground = true)
@Composable
private fun PaletteSwatchPickerPreview() {
    StitchMesh3DTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            PaletteSwatchPicker(
                selectedColor = StitchMeshTerracotta,
                onColorSelected = {}
            )
        }
    }
}
