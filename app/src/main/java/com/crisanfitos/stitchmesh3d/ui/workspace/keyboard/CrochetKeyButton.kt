package com.crisanfitos.stitchmesh3d.ui.workspace.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshOnAccent
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracottaDark
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary

/**
 * Tecla táctil individual del teclado rápido de crochet.
 *
 * Características:
 * - Tipografía monoespaciada estricta (JetBrains Mono).
 * - Respuesta háptica táctil al pulsar (LocalHapticFeedback).
 * - Altura táctil mínima de 38-42dp adecuada para pulgar o stylus.
 * - Estilo visual según modo normal o acento primario (Terracotta).
 */
@Composable
fun CrochetKeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = when {
        !enabled -> StitchMeshSurfaceHigh.copy(alpha = 0.5f)
        isPressed && isAccent -> StitchMeshTerracottaDark
        isPressed -> StitchMeshSurfaceBorder
        isAccent -> StitchMeshTerracotta
        else -> StitchMeshSurfaceHigh
    }

    val borderColor = if (isAccent) StitchMeshTerracotta else StitchMeshSurfaceBorder
    val contentColor = if (isAccent) StitchMeshOnAccent else StitchMeshTextPrimary

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 36.dp, minHeight = 40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(6.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = label,
                style = CrochetTypography.tokenBadge,
                color = contentColor,
                fontSize = 13.sp
            )
        }
    }
}

@Preview(name = "CrochetKeyButton Normal", showBackground = true)
@Composable
private fun CrochetKeyButtonNormalPreview() {
    StitchMesh3DTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CrochetKeyButton(label = "pb", onClick = {})
        }
    }
}

@Preview(name = "CrochetKeyButton Accent", showBackground = true)
@Composable
private fun CrochetKeyButtonAccentPreview() {
    StitchMesh3DTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CrochetKeyButton(label = "Intro", isAccent = true, onClick = {})
        }
    }
}
