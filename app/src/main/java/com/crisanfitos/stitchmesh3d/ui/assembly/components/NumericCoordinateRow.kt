package com.crisanfitos.stitchmesh3d.ui.assembly.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSageGreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextDisabled
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold
import java.util.Locale

/**
 * Renglón individual de coordenada o ángulo con stepper táctil y campo numérico de alta precisión.
 */
@Composable
fun NumericCoordinateRow(
    axisLabel: String,
    value: Float,
    unit: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    step: Float = 0.5f,
    enabled: Boolean = true
) {
    var isEditing by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf(String.format(Locale.US, "%.1f", value)) }

    val axisBadgeColor = when (axisLabel.uppercase()) {
        "X" -> StitchMeshCoralRed
        "Y" -> StitchMeshSageGreen
        "Z" -> StitchMeshYarnGold
        "ROLL" -> StitchMeshCoralRed
        "PITCH" -> StitchMeshSageGreen
        "YAW" -> StitchMeshYarnGold
        else -> StitchMeshTerracotta
    }

    val formattedDisplay = String.format(Locale.US, "%+.1f %s", value, unit)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(StitchMeshSurfaceHigh)
            .border(
                width = 1.dp,
                color = if (isEditing) StitchMeshTerracotta else StitchMeshSurfaceBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indicador del Eje
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(axisBadgeColor.copy(alpha = if (enabled) 0.18f else 0.08f))
                .border(
                    width = 1.dp,
                    color = axisBadgeColor.copy(alpha = if (enabled) 0.8f else 0.3f),
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = axisLabel,
                style = CrochetTypography.tokenBadge,
                color = if (enabled) axisBadgeColor else StitchMeshTextDisabled
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Botón decremento (-)
        IconButton(
            onClick = {
                val next = Math.round((value - step) * 10f) / 10f
                onValueChange(next)
            },
            enabled = enabled,
            modifier = Modifier
                .size(36.dp)
                .testTag("decrement_${axisLabel.lowercase()}")
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrementar $axisLabel",
                tint = if (enabled) StitchMeshTextPrimary else StitchMeshTextDisabled,
                modifier = Modifier.size(16.dp)
            )
        }

        // Valor Numérico Central (Visualización o Edición Directa)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(StitchMeshSurfaceContainer)
                .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(6.dp))
                .clickable(enabled = enabled) {
                    inputText = String.format(Locale.US, "%.1f", value)
                    isEditing = true
                }
                .padding(horizontal = 8.dp)
                .testTag("value_box_${axisLabel.lowercase()}"),
            contentAlignment = Alignment.Center
        ) {
            if (isEditing && enabled) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { newText ->
                        inputText = newText
                    },
                    singleLine = true,
                    textStyle = CrochetTypography.matrixValue.copy(
                        color = StitchMeshTerracotta,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(StitchMeshTerracotta),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val parsed = inputText.toFloatOrNull()
                            if (parsed != null) {
                                onValueChange(parsed)
                            }
                            isEditing = false
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_${axisLabel.lowercase()}")
                )
            } else {
                Text(
                    text = formattedDisplay,
                    style = CrochetTypography.matrixValue,
                    color = if (enabled) StitchMeshTextPrimary else StitchMeshTextDisabled,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Botón incremento (+)
        IconButton(
            onClick = {
                val next = Math.round((value + step) * 10f) / 10f
                onValueChange(next)
            },
            enabled = enabled,
            modifier = Modifier
                .size(36.dp)
                .testTag("increment_${axisLabel.lowercase()}")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Incrementar $axisLabel",
                tint = if (enabled) StitchMeshTextPrimary else StitchMeshTextDisabled,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
