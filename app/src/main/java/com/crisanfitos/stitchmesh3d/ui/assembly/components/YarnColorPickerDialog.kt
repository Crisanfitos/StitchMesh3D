package com.crisanfitos.stitchmesh3d.ui.assembly.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.dashboard.dialogs.DefaultCrochetPalette
import com.crisanfitos.stitchmesh3d.ui.dashboard.dialogs.toHexRgbString
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.Typography

/**
 * Diálogo modal para seleccionar o personalizar el color de hilado de una pieza del Scene Graph.
 */
@Composable
fun YarnColorPickerDialog(
    partName: String,
    initialColorHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (newColorHex: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentColorHex by remember {
        mutableStateOf(if (initialColorHex.startsWith("#")) initialColorHex else "#$initialColorHex")
    }
    var hexInputText by remember { mutableStateOf(currentColorHex) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun parseColor(hex: String): Color? {
        val clean = if (hex.startsWith("#")) hex else "#$hex"
        return try {
            Color(android.graphics.Color.parseColor(clean))
        } catch (_: Exception) {
            null
        }
    }

    val previewColor = parseColor(currentColorHex) ?: StitchMeshTerracotta

    fun handleConfirm() {
        val validated = parseColor(hexInputText)
        if (validated == null) {
            errorMessage = "Código hexadecimal inválido (ej. #E06D53)"
            return
        }
        val clean = if (hexInputText.startsWith("#")) hexInputText.uppercase() else "#${hexInputText.uppercase()}"
        onColorSelected(clean)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Color de Hilado: $partName",
                style = Typography.titleLarge,
                color = StitchMeshTextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Selecciona un color de la paleta estándar o introduce un código hexadecimal.",
                    style = Typography.bodyMedium,
                    color = StitchMeshTextSecondary
                )

                // Muestra de color actual
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchMeshSurfaceHigh)
                        .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(previewColor)
                            .border(1.dp, StitchMeshSurfaceBorder, CircleShape)
                    )
                    Column {
                        Text(
                            text = "Color Activo",
                            style = Typography.labelMedium,
                            color = StitchMeshTextSecondary
                        )
                        Text(
                            text = currentColorHex.uppercase(),
                            style = CrochetTypography.formulaInput,
                            color = StitchMeshTextPrimary
                        )
                    }
                }

                // Paleta de colores predefinida
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Catálogo de lanas",
                        style = CrochetTypography.tokenBadge,
                        color = StitchMeshTextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DefaultCrochetPalette.forEach { item ->
                            val hex = item.color.toHexRgbString()
                            val isSelected = currentColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(item.color)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) StitchMeshTextPrimary else StitchMeshSurfaceBorder,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        currentColorHex = hex
                                        hexInputText = hex
                                        errorMessage = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = item.name,
                                        tint = if (item.color == Color(0xFFF5EBE0)) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Campo de entrada Hexadecimal personalizado
                OutlinedTextField(
                    value = hexInputText,
                    onValueChange = { input ->
                        hexInputText = input
                        val parsed = parseColor(input)
                        if (parsed != null) {
                            currentColorHex = input
                            errorMessage = null
                        } else {
                            errorMessage = "Formato no válido"
                        }
                    },
                    label = { Text("Código Hexadecimal") },
                    placeholder = { Text("#E06D53") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(text = errorMessage!!, color = StitchMeshCoralRed)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = CrochetTypography.formulaInput,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = StitchMeshSurfaceHigh,
                        unfocusedContainerColor = StitchMeshSurfaceHigh,
                        focusedTextColor = StitchMeshTextPrimary,
                        unfocusedTextColor = StitchMeshTextPrimary,
                        focusedIndicatorColor = StitchMeshTerracotta,
                        unfocusedIndicatorColor = StitchMeshSurfaceBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { handleConfirm() },
                colors = ButtonDefaults.buttonColors(containerColor = StitchMeshTerracotta)
            ) {
                Text("Aplicar Color")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = StitchMeshTextSecondary)
            }
        },
        containerColor = StitchMeshSurfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    )
}
