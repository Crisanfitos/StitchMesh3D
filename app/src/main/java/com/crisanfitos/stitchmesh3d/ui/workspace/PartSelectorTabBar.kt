package com.crisanfitos.stitchmesh3d.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshOnAccent
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * Modelo ligero de UI para una parte o extremidad del modelo amigurumi.
 */
data class ProjectPartUiModel(
    val id: String,
    val name: String,
    val roundCount: Int,
    val colorHex: String = "#E06D53",
    val isValid: Boolean = true,
    val sortOrder: Int = 0
)

/**
 * Selector horizontal de partes del proyecto amigurumi (Cabeza, Cuerpo, Orejas, etc.).
 * Soporta selección, añadir, renombrar, duplicar y eliminar piezas (RF-4.1).
 */
@Composable
fun PartSelectorTabBar(
    parts: List<ProjectPartUiModel>,
    selectedPartId: String,
    onPartSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    onAddPart: (() -> Unit)? = null,
    onRenamePart: ((String) -> Unit)? = null,
    onDuplicatePart: ((String) -> Unit)? = null,
    onDeletePart: ((String) -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(StitchMeshSurfaceContainer)
            .border(width = 1.dp, color = StitchMeshSurfaceBorder)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        parts.forEach { part ->
            val isSelected = part.id == selectedPartId
            var showMenu by remember { mutableStateOf(false) }

            val partColor = try {
                Color(android.graphics.Color.parseColor(part.colorHex))
            } catch (e: Exception) {
                StitchMeshTerracotta
            }

            val backgroundColor = if (isSelected) StitchMeshTerracotta else StitchMeshSurfaceHigh
            val borderColor = if (isSelected) StitchMeshTerracotta else StitchMeshSurfaceBorder
            val textColor = if (isSelected) StitchMeshOnAccent else StitchMeshTextPrimary

            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(backgroundColor)
                        .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
                        .clickable { onPartSelected(part.id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Muestra de color de lana de la parte
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(partColor)
                    )

                    Text(
                        text = "${part.name} (${part.roundCount} vtas)",
                        style = CrochetTypography.tokenBadge,
                        color = textColor
                    )

                    if (isSelected && (onRenamePart != null || onDuplicatePart != null || onDeletePart != null)) {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opciones de la parte ${part.name}",
                                tint = textColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (onRenamePart != null) {
                        DropdownMenuItem(
                            text = { Text("Renombrar") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = StitchMeshTerracotta
                                )
                            },
                            onClick = {
                                showMenu = false
                                onRenamePart(part.id)
                            }
                        )
                    }

                    if (onDuplicatePart != null) {
                        DropdownMenuItem(
                            text = { Text("Duplicar") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = StitchMeshTextSecondary
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDuplicatePart(part.id)
                            }
                        )
                    }

                    if (onDeletePart != null) {
                        DropdownMenuItem(
                            text = { Text("Eliminar", color = StitchMeshCoralRed) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = StitchMeshCoralRed
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDeletePart(part.id)
                            }
                        )
                    }
                }
            }
        }

        if (onAddPart != null) {
            IconButton(
                onClick = onAddPart,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StitchMeshSurfaceHigh)
                    .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Añadir nueva parte",
                    tint = StitchMeshTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview(name = "PartSelectorTabBar Preview", showBackground = true)
@Composable
private fun PartSelectorTabBarPreview() {
    StitchMesh3DTheme {
        PartSelectorTabBar(
            parts = listOf(
                ProjectPartUiModel("p1", "Cabeza", 18, "#E06D53"),
                ProjectPartUiModel("p2", "Cuerpo", 24, "#F2C94C"),
                ProjectPartUiModel("p3", "Orejas (x2)", 8, "#52A474")
            ),
            selectedPartId = "p1",
            onPartSelected = {},
            onAddPart = {},
            onRenamePart = {},
            onDuplicatePart = {},
            onDeletePart = {}
        )
    }
}
