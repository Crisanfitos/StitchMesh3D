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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextDisabled
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * Selector numérico y visual de calibre de aguja en milímetros (mm).
 * Cuenta con botones de incremento/decremento y accesos rápidos a calibres comunes.
 */
@Composable
fun HookSizePicker(
    hookSizeMm: Float,
    onHookSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    minMm: Float = 1.50f,
    maxMm: Float = 15.00f,
    stepMm: Float = 0.25f
) {
    val quickSizes = listOf(2.5f, 3.0f, 3.5f, 4.0f, 4.5f, 5.0f, 6.0f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Calibre de Aguja (mm)",
            style = MaterialTheme.typography.labelMedium,
            color = StitchMeshTextSecondary
        )

        // Controles de ajuste fino y visor digital del calibre
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(StitchMeshSurfaceHigh)
                .border(width = 1.dp, color = StitchMeshSurfaceBorder, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    val next = (hookSizeMm - stepMm).coerceAtLeast(minMm)
                    onHookSizeChange(Math.round(next * 100f) / 100f)
                },
                enabled = hookSizeMm > minMm,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (hookSizeMm > minMm) StitchMeshSurfaceBorder else StitchMeshSurfaceHigh)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Disminuir aguja",
                    tint = if (hookSizeMm > minMm) StitchMeshTextPrimary else StitchMeshTextDisabled
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = String.format("%.2f mm", hookSizeMm),
                    style = CrochetTypography.matrixValue,
                    color = StitchMeshTerracotta
                )
                Text(
                    text = "Grosor exacto",
                    style = MaterialTheme.typography.labelSmall,
                    color = StitchMeshTextSecondary
                )
            }

            IconButton(
                onClick = {
                    val next = (hookSizeMm + stepMm).coerceAtMost(maxMm)
                    onHookSizeChange(Math.round(next * 100f) / 100f)
                },
                enabled = hookSizeMm < maxMm,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (hookSizeMm < maxMm) StitchMeshSurfaceBorder else StitchMeshSurfaceHigh)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Aumentar aguja",
                    tint = if (hookSizeMm < maxMm) StitchMeshTextPrimary else StitchMeshTextDisabled
                )
            }
        }

        // Fila de accesos rápidos a calibres populares
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickSizes.forEach { size ->
                val isSelected = Math.abs(size - hookSizeMm) < 0.05f
                QuickSizeChip(
                    sizeMm = size,
                    isSelected = isSelected,
                    onClick = { onHookSizeChange(size) }
                )
            }
        }
    }
}

@Composable
private fun QuickSizeChip(
    sizeMm: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) StitchMeshTerracotta else StitchMeshSurfaceHigh
    val textColor = if (isSelected) StitchMeshTextPrimary else StitchMeshTextSecondary
    val borderColor = if (isSelected) StitchMeshTerracotta else StitchMeshSurfaceBorder

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = String.format("%.1f", sizeMm),
            style = CrochetTypography.tokenBadge,
            color = textColor
        )
    }
}

@Preview(name = "HookSizePicker - Dark", showBackground = true)
@Composable
private fun HookSizePickerPreview() {
    StitchMesh3DTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            HookSizePicker(
                hookSizeMm = 3.50f,
                onHookSizeChange = {}
            )
        }
    }
}
