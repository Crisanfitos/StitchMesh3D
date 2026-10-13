package com.crisanfitos.stitchmesh3d.ui.assembly.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.Typography

/**
 * Componente principal del árbol jerárquico SceneGraphTree para la pantalla de ensamblado 3D (Stitch screen 14f11294).
 * Gestiona la lista de piezas, visibilidad, bloqueo, color y acciones de jerarquía.
 */
@Composable
fun SceneGraphTree(
    items: List<SceneGraphItem>,
    selectedPartId: String,
    onPartSelected: (partId: String) -> Unit,
    onToggleVisibility: (partId: String) -> Unit,
    onToggleLock: (partId: String) -> Unit,
    onColorChanged: (partId: String, newColorHex: String) -> Unit,
    onAddPart: () -> Unit,
    modifier: Modifier = Modifier,
    onDuplicatePart: ((partId: String) -> Unit)? = null,
    onRenamePart: ((partId: String) -> Unit)? = null,
    onDeletePart: ((partId: String) -> Unit)? = null,
    onToggleExpand: ((partId: String) -> Unit)? = null
) {
    var colorPickerTargetPart by remember { mutableStateOf<SceneGraphItem?>(null) }

    val visibleCount = items.count { it.isVisible }
    val totalCount = items.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StitchMeshSurfaceContainer)
            .border(width = 1.dp, color = StitchMeshSurfaceBorder, shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("scene_graph_tree")
    ) {
        // Cabecera del Árbol de Escena
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = null,
                    tint = StitchMeshTerracotta,
                    modifier = Modifier.size(20.dp)
                )

                Column {
                    Text(
                        text = "Árbol de Escena",
                        style = Typography.titleMedium,
                        color = StitchMeshTextPrimary
                    )
                    Text(
                        text = "$visibleCount/$totalCount visibles",
                        style = CrochetTypography.tokenBadge,
                        color = StitchMeshTextSecondary
                    )
                }
            }

            // Botón añadir pieza
            Button(
                onClick = onAddPart,
                colors = ButtonDefaults.buttonColors(containerColor = StitchMeshTerracotta),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_part_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = "Pieza",
                    style = Typography.labelMedium
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(StitchMeshSurfaceBorder)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Lista jerárquica o estado vacío
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No hay piezas en el proyecto",
                        style = Typography.bodyMedium,
                        color = StitchMeshTextSecondary
                    )
                    Button(
                        onClick = onAddPart,
                        colors = ButtonDefaults.buttonColors(containerColor = StitchMeshSurfaceHigh),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Crear primera pieza", color = StitchMeshTerracotta)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = items,
                    key = { it.id }
                ) { item ->
                    PartTreeItemRow(
                        item = item,
                        isSelected = item.id == selectedPartId,
                        onPartSelected = onPartSelected,
                        onToggleVisibility = onToggleVisibility,
                        onToggleLock = onToggleLock,
                        onColorClick = {
                            colorPickerTargetPart = item
                        },
                        onToggleExpand = onToggleExpand,
                        onRenamePart = onRenamePart,
                        onDuplicatePart = onDuplicatePart,
                        onDeletePart = onDeletePart
                    )
                }
            }
        }
    }

    // Modal de selección de color de hilado
    if (colorPickerTargetPart != null) {
        val target = colorPickerTargetPart!!
        YarnColorPickerDialog(
            partName = target.name,
            initialColorHex = target.colorHex,
            onDismiss = { colorPickerTargetPart = null },
            onColorSelected = { newHex ->
                onColorChanged(target.id, newHex)
                colorPickerTargetPart = null
            }
        )
    }
}
