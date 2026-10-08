package com.crisanfitos.stitchmesh3d.ui.viewport.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
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
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextDisabled
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * Conmutador táctil de dos estados para alternar entre superficie sólida (Lana PBR)
 * y modo alambre técnico (Wireframe CAD).
 */
@Composable
fun WireframeToggleSwitch(
    isWireframe: Boolean,
    onWireframeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(StitchMeshSurfaceHigh)
            .border(
                width = 1.dp,
                color = StitchMeshSurfaceBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Opción: Sólido PBR
            val solidBg by animateColorAsState(
                targetValue = if (!isWireframe) StitchMeshTerracotta else StitchMeshSurfaceHigh,
                animationSpec = tween(180),
                label = "solidBg"
            )
            val solidTextColor by animateColorAsState(
                targetValue = if (!isWireframe) StitchMeshOnAccent else StitchMeshTextSecondary,
                animationSpec = tween(180),
                label = "solidTextColor"
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(solidBg)
                    .clickable(
                        enabled = enabled && isWireframe,
                        role = Role.RadioButton,
                        onClick = { onWireframeChange(false) }
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = solidTextColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Sólido",
                    style = CrochetTypography.tokenBadge,
                    color = solidTextColor,
                    fontSize = 11.sp
                )
            }

            // Opción: Wireframe CAD
            val wireframeBg by animateColorAsState(
                targetValue = if (isWireframe) StitchMeshTerracotta else StitchMeshSurfaceHigh,
                animationSpec = tween(180),
                label = "wireframeBg"
            )
            val wireframeTextColor by animateColorAsState(
                targetValue = if (isWireframe) StitchMeshOnAccent else StitchMeshTextSecondary,
                animationSpec = tween(180),
                label = "wireframeTextColor"
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(wireframeBg)
                    .clickable(
                        enabled = enabled && !isWireframe,
                        role = Role.RadioButton,
                        onClick = { onWireframeChange(true) }
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = null,
                    tint = wireframeTextColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Alambre",
                    style = CrochetTypography.tokenBadge,
                    color = wireframeTextColor,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Preview(name = "WireframeToggleSwitch - Solid", showBackground = true)
@Composable
private fun WireframeToggleSwitchSolidPreview() {
    StitchMesh3DTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            WireframeToggleSwitch(
                isWireframe = false,
                onWireframeChange = {}
            )
        }
    }
}

@Preview(name = "WireframeToggleSwitch - Wireframe", showBackground = true)
@Composable
private fun WireframeToggleSwitchWireframePreview() {
    StitchMesh3DTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            WireframeToggleSwitch(
                isWireframe = true,
                onWireframeChange = {}
            )
        }
    }
}
