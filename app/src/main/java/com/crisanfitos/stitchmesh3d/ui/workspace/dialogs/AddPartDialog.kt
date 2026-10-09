package com.crisanfitos.stitchmesh3d.ui.workspace.dialogs

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
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.Typography

@Composable
fun AddPartDialog(
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nameText by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validateAndSubmit() {
        val trimmed = nameText.trim()
        if (trimmed.isEmpty()) {
            errorMessage = "El nombre de la pieza no puede estar vacío"
            return
        }
        if (existingNames.any { it.equals(trimmed, ignoreCase = true) }) {
            errorMessage = "Ya existe una pieza con el nombre '$trimmed'"
            return
        }
        val palette = DefaultCrochetPalette[selectedColorIndex]
        val hex = String.format("#%06X", (0xFFFFFF and palette.color.value.toLong().toInt()))
        onConfirm(trimmed, hex)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nueva Pieza Amigurumi",
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
                    text = "Añade un nuevo componente independiente al patrón (ej. Orejas, Patas, Cola).",
                    style = Typography.bodyMedium,
                    color = StitchMeshTextSecondary
                )

                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        errorMessage = null
                    },
                    label = { Text("Nombre de la pieza") },
                    placeholder = { Text("ej. Brazo Izquierdo") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(text = errorMessage!!, color = StitchMeshCoralRed)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = StitchMeshSurfaceHigh,
                        unfocusedContainerColor = StitchMeshSurfaceHigh,
                        focusedTextColor = StitchMeshTextPrimary,
                        unfocusedTextColor = StitchMeshTextPrimary,
                        focusedIndicatorColor = StitchMeshTerracotta,
                        unfocusedIndicatorColor = StitchMeshSurfaceBorder
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Color base de lana",
                        style = CrochetTypography.tokenBadge,
                        color = StitchMeshTextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DefaultCrochetPalette.take(6).forEachIndexed { index, item ->
                            val isSelected = selectedColorIndex == index
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
                                    .clickable { selectedColorIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (item.color == Color(0xFFF5EBE0)) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { validateAndSubmit() },
                colors = ButtonDefaults.buttonColors(containerColor = StitchMeshTerracotta)
            ) {
                Text("Crear Pieza")
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
