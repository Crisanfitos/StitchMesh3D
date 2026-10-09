package com.crisanfitos.stitchmesh3d.ui.tension.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
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
 * Formulario interactivo de muestra de tensión 10x10 cm (RF-2.3, Regla 13).
 * Proporciona selección de categoría CYC de hilado y steppers táctiles de alta precisión
 * para ingresar la cantidad de puntos y vueltas contados en una muestra de 10 cm.
 */
@Composable
fun TensionSampleForm(
    selectedStandard: YarnGaugeStandard,
    availableStandards: List<YarnGaugeStandard>,
    stitchesCount: Int,
    roundsCount: Int,
    onStandardSelected: (YarnGaugeStandard) -> Unit,
    onIncrementStitches: () -> Unit,
    onDecrementStitches: () -> Unit,
    onIncrementRounds: () -> Unit,
    onDecrementRounds: () -> Unit,
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
            modifier = Modifier.padding(16.dp)
        ) {
            // Header del Formulario
            Text(
                text = "Medición de Muestra de Tensión (10×10 cm)",
                style = MaterialTheme.typography.titleMedium,
                color = StitchMeshTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Teje un cuadrado de muestra, mide una ventana de 10 cm e introduce los puntos y vueltas.",
                style = MaterialTheme.typography.bodySmall,
                color = StitchMeshTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Selector de Estándar Base CYC
            Text(
                text = "ESTÁNDAR CYC DE REFERENCIA",
                style = MaterialTheme.typography.labelSmall,
                color = StitchMeshTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableStandards.forEach { std ->
                    val isSelected = std.id == selectedStandard.id
                    val shortName = std.categoryName.substringBefore(" /")

                    FilterChip(
                        selected = isSelected,
                        onClick = { onStandardSelected(std) },
                        label = {
                            Text(
                                text = "#${std.yarnWeightCategory.code} $shortName (${std.hookSizeMm}mm)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StitchMeshTerracotta,
                            selectedLabelColor = StitchMeshOnAccent,
                            containerColor = StitchMeshSurfaceContainer,
                            labelColor = StitchMeshTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = StitchMeshSurfaceBorder,
                            selectedBorderColor = StitchMeshTerracotta
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Steppers táctiles para Puntos y Vueltas en 10 cm
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Stepper Puntos
                StepperModule(
                    title = "PUNTOS EN 10 CM",
                    unitLabel = "puntos",
                    value = stitchesCount,
                    onIncrement = onIncrementStitches,
                    onDecrement = onDecrementStitches,
                    modifier = Modifier.weight(1f)
                )

                // Stepper Vueltas
                StepperModule(
                    title = "VUELTAS EN 10 CM",
                    unitLabel = "vueltas",
                    value = roundsCount,
                    onIncrement = onIncrementRounds,
                    onDecrement = onDecrementRounds,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Componente modular de stepper táctil con target de toque de 48dp (RND-3).
 */
@Composable
private fun StepperModule(
    title: String,
    unitLabel: String,
    value: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(StitchMeshSurfaceContainer)
            .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = StitchMeshTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Botón Decrementar (-)
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(StitchMeshNeutralDark)
                        .border(1.dp, StitchMeshSurfaceBorder, CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = StitchMeshTextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Disminuir $title",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Valor numérico centrado
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = value.toString(),
                        color = StitchMeshTextPrimary,
                        fontSize = 26.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = unitLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = StitchMeshTextDisabled,
                        fontSize = 10.sp
                    )
                }

                // Botón Incrementar (+)
                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(StitchMeshNeutralDark)
                        .border(1.dp, StitchMeshSurfaceBorder, CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = StitchMeshTerracotta
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Aumentar $title",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
