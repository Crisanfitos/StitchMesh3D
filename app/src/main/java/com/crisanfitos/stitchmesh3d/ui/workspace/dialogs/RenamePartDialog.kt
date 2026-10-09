package com.crisanfitos.stitchmesh3d.ui.workspace.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.Typography

@Composable
fun RenamePartDialog(
    currentName: String,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nameText by remember { mutableStateOf(currentName) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validateAndSubmit() {
        val trimmed = nameText.trim()
        if (trimmed.isEmpty()) {
            errorMessage = "El nombre no puede estar vacío"
            return
        }
        if (existingNames.any { it.equals(trimmed, ignoreCase = true) && !it.equals(currentName, ignoreCase = true) }) {
            errorMessage = "Ya existe otra pieza con el nombre '$trimmed'"
            return
        }
        onConfirm(trimmed)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Renombrar Pieza",
                style = Typography.titleLarge,
                color = StitchMeshTextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        errorMessage = null
                    },
                    label = { Text("Nombre de la pieza") },
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
            }
        },
        confirmButton = {
            Button(
                onClick = { validateAndSubmit() },
                colors = ButtonDefaults.buttonColors(containerColor = StitchMeshTerracotta)
            ) {
                Text("Guardar")
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
