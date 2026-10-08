package com.crisanfitos.stitchmesh3d.ui.dashboard.dialogs

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextDisabled
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * ModalBottomSheet para la creación paramétrica de un nuevo proyecto de crochet.
 * Captura nombre, tipo de pieza, tensión inicial (CYC + aguja) y paleta cromática.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectModalSheet(
    onDismiss: () -> Unit,
    onCreateProject: (
        title: String,
        pieceType: String,
        yarnWeight: YarnWeightCategory,
        hookSizeMm: Float,
        primaryColor: Color
    ) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StitchMeshSurfaceContainer,
        modifier = modifier
    ) {
        NewProjectFormContent(
            onDismiss = onDismiss,
            onCreateProject = onCreateProject,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun NewProjectFormContent(
    onDismiss: () -> Unit,
    onCreateProject: (
        title: String,
        pieceType: String,
        yarnWeight: YarnWeightCategory,
        hookSizeMm: Float,
        primaryColor: Color
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var titleTouched by remember { mutableStateOf(false) }
    var selectedPieceType by remember { mutableStateOf("Amigurumi (3D)") }
    var selectedWeight by remember { mutableStateOf(YarnWeightCategory.MEDIUM) }
    var hookSizeMm by remember { mutableFloatStateOf(3.50f) }
    var selectedColor by remember { mutableStateOf(StitchMeshTerracotta) }

    val isTitleValid = title.trim().isNotEmpty()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera del diálogo modal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nuevo Proyecto de Crochet",
                style = MaterialTheme.typography.titleLarge,
                color = StitchMeshTextPrimary
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar modal",
                    tint = StitchMeshTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Campo de entrada: Título del proyecto
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Nombre del Proyecto",
                style = MaterialTheme.typography.labelMedium,
                color = StitchMeshTextSecondary
            )
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleTouched = true
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Ej. Oso Amigurumi, Gorro Acanalado...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StitchMeshTextDisabled
                    )
                },
                singleLine = true,
                isError = titleTouched && !isTitleValid,
                supportingText = {
                    if (titleTouched && !isTitleValid) {
                        Text(
                            text = "El nombre del proyecto no puede estar vacío",
                            style = MaterialTheme.typography.labelSmall,
                            color = StitchMeshCoralRed
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = StitchMeshSurfaceHigh,
                    unfocusedContainerColor = StitchMeshSurfaceHigh,
                    focusedBorderColor = StitchMeshTerracotta,
                    unfocusedBorderColor = StitchMeshSurfaceBorder,
                    errorBorderColor = StitchMeshCoralRed,
                    focusedTextColor = StitchMeshTextPrimary,
                    unfocusedTextColor = StitchMeshTextPrimary,
                    cursorColor = StitchMeshTerracotta
                )
            )
        }

        // Selector de tipo de proyecto / estructura
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Tipo de Estructura",
                style = MaterialTheme.typography.labelMedium,
                color = StitchMeshTextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Amigurumi (3D)", "Prenda Plana", "Accesorio").forEach { pieceType ->
                    val isSelected = pieceType == selectedPieceType
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) StitchMeshTerracotta else StitchMeshSurfaceHigh)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) StitchMeshTerracotta else StitchMeshSurfaceBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedPieceType = pieceType }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pieceType,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color.White else StitchMeshTextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Selector CYC de grosor de hilado
        YarnWeightSelector(
            selectedWeight = selectedWeight,
            onSelectWeight = { weight ->
                selectedWeight = weight
                hookSizeMm = weight.minHookSizeMm
            }
        )

        // Calibre de aguja en mm
        HookSizePicker(
            hookSizeMm = hookSizeMm,
            onHookSizeChange = { hookSizeMm = it }
        )

        // Selector de muestra de color inicial
        PaletteSwatchPicker(
            selectedColor = selectedColor,
            onColorSelected = { selectedColor = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Botones de acción
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StitchMeshSurfaceBorder)
            ) {
                Text(
                    text = "Cancelar",
                    style = MaterialTheme.typography.labelLarge,
                    color = StitchMeshTextSecondary
                )
            }

            Button(
                onClick = {
                    if (isTitleValid) {
                        onCreateProject(
                            title.trim(),
                            selectedPieceType,
                            selectedWeight,
                            hookSizeMm,
                            selectedColor
                        )
                    }
                },
                enabled = isTitleValid,
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StitchMeshTerracotta,
                    disabledContainerColor = StitchMeshSurfaceHigh
                )
            ) {
                Text(
                    text = "Crear Proyecto",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isTitleValid) Color.White else StitchMeshTextDisabled
                )
            }
        }
    }
}

@Preview(name = "NewProjectForm - Dark", showBackground = true)
@Composable
private fun NewProjectFormPreview() {
    StitchMesh3DTheme {
        Box(
            modifier = Modifier
                .background(StitchMeshSurfaceContainer)
                .padding(16.dp)
        ) {
            NewProjectFormContent(
                onDismiss = {},
                onCreateProject = { _, _, _, _, _ -> }
            )
        }
    }
}
