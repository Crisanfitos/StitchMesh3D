package com.crisanfitos.stitchmesh3d.ui.viewport.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshNeutralDark
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshOnAccent
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * Puntos de vista y orientaciones estándar de cámara CAD para el visor 3D.
 */
enum class CameraPreset(
    val label: String,
    val description: String,
    val icon: ImageVector
) {
    ISOMETRIC("Iso", "Vista isométrica CAD", Icons.Default.ViewInAr),
    FRONT("Frontal", "Vista frontal", Icons.Default.CropPortrait),
    SIDE("Lateral", "Vista lateral", Icons.Default.CropLandscape),
    TOP("Superior", "Vista cenital / superior", Icons.Default.VerticalAlignTop),
    RESET("Reset", "Centrar y reiniciar cámara", Icons.Default.RestartAlt)
}

/**
 * Barra de botones de píldora translúcida para cambiar rápidamente el punto de vista
 * de la cámara 3D sobre el modelo de amigurumi.
 */
@Composable
fun CameraOrientationPillRow(
    selectedPreset: CameraPreset,
    onPresetSelected: (CameraPreset) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(StitchMeshSurfaceContainer.copy(alpha = 0.90f))
            .border(
                width = 1.dp,
                color = StitchMeshSurfaceBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(3.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            CameraPreset.entries.forEach { preset ->
                val isSelected = (preset == selectedPreset)

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) StitchMeshTerracotta else StitchMeshSurfaceHigh.copy(alpha = 0.6f),
                    animationSpec = tween(180),
                    label = "pillBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) StitchMeshOnAccent else StitchMeshTextSecondary,
                    animationSpec = tween(180),
                    label = "pillContent"
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .clickable(
                            enabled = enabled,
                            role = Role.Button,
                            onClick = { onPresetSelected(preset) }
                        )
                        .padding(horizontal = 7.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = preset.icon,
                        contentDescription = preset.description,
                        tint = contentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = preset.label,
                        style = CrochetTypography.tokenBadge,
                        color = contentColor,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Preview(name = "CameraOrientationPillRow Preview", showBackground = true)
@Composable
private fun CameraOrientationPillRowPreview() {
    StitchMesh3DTheme {
        Box(
            modifier = Modifier
                .background(StitchMeshNeutralDark)
                .padding(16.dp)
        ) {
            CameraOrientationPillRow(
                selectedPreset = CameraPreset.ISOMETRIC,
                onPresetSelected = {}
            )
        }
    }
}
