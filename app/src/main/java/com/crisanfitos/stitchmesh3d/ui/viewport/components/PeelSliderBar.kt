package com.crisanfitos.stitchmesh3d.ui.viewport.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextDisabled
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold
import kotlin.math.roundToInt

/**
 * Barra de control interactiva PeelSliderBar para el visor 3D de StitchMesh.
 *
 * Permite pelar/desocultar las capas vuelta por vuelta (1..totalRounds) para
 * examinar la geometría interna y el relleno del amigurumi (RF-3.4).
 * Integra además el switch para alternar superficie sólida y modo wireframe.
 */
@Composable
fun PeelSliderBar(
    currentRound: Int,
    totalRounds: Int,
    onRoundSelected: (Int) -> Unit,
    isWireframe: Boolean,
    onWireframeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    currentRoundStitches: Int? = null,
    enabled: Boolean = true
) {
    val effectiveTotal = maxOf(1, totalRounds)
    val clampedRound = currentRound.coerceIn(1, effectiveTotal)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StitchMeshSurfaceContainer.copy(alpha = 0.95f))
            .border(
                width = 1.dp,
                color = StitchMeshSurfaceBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Fila superior: Información de capa y switch de wireframe
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StitchMeshSurfaceHigh)
                            .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PEEL SLIDER",
                            style = CrochetTypography.tokenBadge,
                            color = StitchMeshYarnGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Vuelta $clampedRound de $effectiveTotal",
                        style = CrochetTypography.matrixValue,
                        color = StitchMeshTextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    if (currentRoundStitches != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(StitchMeshTerracotta.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$currentRoundStitches pts",
                                style = CrochetTypography.tokenBadge,
                                color = StitchMeshTerracotta,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Switch Solid vs Wireframe
                WireframeToggleSwitch(
                    isWireframe = isWireframe,
                    onWireframeChange = onWireframeChange,
                    enabled = enabled
                )
            }

            // Fila inferior: Botón [-1], Slider interactivo, Botón [+1] y Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Botón paso atrás (-1 vuelta)
                IconButton(
                    onClick = {
                        if (clampedRound > 1) {
                            onRoundSelected(clampedRound - 1)
                        }
                    },
                    enabled = enabled && clampedRound > 1,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Vuelta anterior",
                        tint = if (enabled && clampedRound > 1) StitchMeshTextPrimary else StitchMeshTextDisabled,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Slider continuo con snap entero
                Slider(
                    value = clampedRound.toFloat(),
                    onValueChange = { newVal ->
                        onRoundSelected(newVal.roundToInt())
                    },
                    valueRange = 1f..effectiveTotal.toFloat(),
                    steps = if (effectiveTotal > 2) effectiveTotal - 2 else 0,
                    enabled = enabled && effectiveTotal > 1,
                    colors = SliderDefaults.colors(
                        thumbColor = StitchMeshTerracotta,
                        activeTrackColor = StitchMeshTerracotta,
                        inactiveTrackColor = StitchMeshSurfaceHigh,
                        activeTickColor = StitchMeshOnAccent,
                        inactiveTickColor = StitchMeshSurfaceBorder
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Botón paso adelante (+1 vuelta)
                IconButton(
                    onClick = {
                        if (clampedRound < effectiveTotal) {
                            onRoundSelected(clampedRound + 1)
                        }
                    },
                    enabled = enabled && clampedRound < effectiveTotal,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Vuelta siguiente",
                        tint = if (enabled && clampedRound < effectiveTotal) StitchMeshTextPrimary else StitchMeshTextDisabled,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Botón mostrar todas las capas (Reset al total)
                IconButton(
                    onClick = { onRoundSelected(effectiveTotal) },
                    enabled = enabled && clampedRound < effectiveTotal,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Mostrar todas las vueltas",
                        tint = if (enabled && clampedRound < effectiveTotal) StitchMeshTerracotta else StitchMeshTextDisabled,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Preview(name = "PeelSliderBar Preview", showBackground = true)
@Composable
private fun PeelSliderBarPreview() {
    StitchMesh3DTheme {
        Box(
            modifier = Modifier
                .background(StitchMeshNeutralDark)
                .padding(16.dp)
        ) {
            PeelSliderBar(
                currentRound = 8,
                totalRounds = 18,
                currentRoundStitches = 48,
                onRoundSelected = {},
                isWireframe = false,
                onWireframeChange = {}
            )
        }
    }
}
