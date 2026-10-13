package com.crisanfitos.stitchmesh3d.ui.assembly.components

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextDisabled
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold
import com.crisanfitos.stitchmesh3d.ui.theme.Typography

/**
 * Fila individual del árbol Scene Graph representando una pieza de amigurumi con sus controles interactivos.
 */
@Composable
fun PartTreeItemRow(
    item: SceneGraphItem,
    isSelected: Boolean,
    onPartSelected: (partId: String) -> Unit,
    onToggleVisibility: (partId: String) -> Unit,
    onToggleLock: (partId: String) -> Unit,
    onColorClick: (partId: String) -> Unit,
    modifier: Modifier = Modifier,
    onToggleExpand: ((partId: String) -> Unit)? = null,
    onRenamePart: ((partId: String) -> Unit)? = null,
    onDuplicatePart: ((partId: String) -> Unit)? = null,
    onDeletePart: ((partId: String) -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    val partColor = try {
        Color(android.graphics.Color.parseColor(if (item.colorHex.startsWith("#")) item.colorHex else "#${item.colorHex}"))
    } catch (_: Exception) {
        StitchMeshTerracotta
    }

    val backgroundColor = if (isSelected) {
        StitchMeshTerracotta.copy(alpha = 0.16f)
    } else {
        StitchMeshSurfaceContainer
    }

    val borderColor = if (isSelected) {
        StitchMeshTerracotta
    } else {
        StitchMeshSurfaceBorder
    }

    val topologyBadgeText = when (item.topologyType) {
        PartTopologyType.CLOSED_FILLED -> "3D"
        PartTopologyType.SEMI_CLOSED_TUBE -> "TUBO"
        PartTopologyType.FLAT_PANEL -> "PLANA"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .clickable { onPartSelected(item.id) }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("part_row_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indentación jerárquica para nodos hijos
        if (item.depth > 0) {
            Spacer(modifier = Modifier.width((item.depth * 20).dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(StitchMeshSurfaceBorder)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Botón expandir/colapsar si tiene hijos
        if (item.hasChildren && onToggleExpand != null) {
            IconButton(
                onClick = { onToggleExpand(item.id) },
                modifier = Modifier
                    .size(28.dp)
                    .testTag("expand_btn_${item.id}")
            ) {
                Icon(
                    imageVector = if (item.isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (item.isExpanded) "Colapsar ${item.name}" else "Expandir ${item.name}",
                    tint = StitchMeshTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(4.dp))
        }

        // Swatch de color de hilado interactivo
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(partColor)
                .border(width = 1.dp, color = StitchMeshSurfaceBorder, shape = CircleShape)
                .clickable { onColorClick(item.id) }
                .testTag("color_swatch_${item.id}"),
            contentAlignment = Alignment.Center
        ) {
            if (!item.isVisible) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Nombre de la pieza y badges técnicos
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.name,
                    style = Typography.titleMedium,
                    color = if (item.isVisible) StitchMeshTextPrimary else StitchMeshTextDisabled,
                    maxLines = 1
                )

                // Badge de tipo topológico
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StitchMeshSurfaceHigh)
                        .border(0.5.dp, StitchMeshSurfaceBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = topologyBadgeText,
                        style = CrochetTypography.tokenBadge,
                        color = StitchMeshTextSecondary
                    )
                }
            }

            // Métrica de vueltas y puntos
            Text(
                text = "${item.roundCount} vtas" + if (item.stitchCount > 0) " · ${item.stitchCount} pts" else "",
                style = CrochetTypography.tokenBadge,
                color = StitchMeshTextSecondary
            )
        }

        // Toggle de Visibilidad (Ojo)
        IconButton(
            onClick = { onToggleVisibility(item.id) },
            modifier = Modifier
                .size(36.dp)
                .testTag("visibility_btn_${item.id}")
        ) {
            Icon(
                imageVector = if (item.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = if (item.isVisible) "Ocultar ${item.name}" else "Mostrar ${item.name}",
                tint = if (item.isVisible) StitchMeshTextPrimary else StitchMeshTextDisabled,
                modifier = Modifier.size(18.dp)
            )
        }

        // Toggle de Bloqueo (Candado)
        IconButton(
            onClick = { onToggleLock(item.id) },
            modifier = Modifier
                .size(36.dp)
                .testTag("lock_btn_${item.id}")
        ) {
            Icon(
                imageVector = if (item.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = if (item.isLocked) "Desbloquear ${item.name}" else "Bloquear ${item.name}",
                tint = if (item.isLocked) StitchMeshYarnGold else StitchMeshTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Menú de opciones (Renombrar, Duplicar, Eliminar)
        if (onRenamePart != null || onDuplicatePart != null || onDeletePart != null) {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("more_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones para ${item.name}",
                        tint = StitchMeshTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
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
                                onRenamePart(item.id)
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
                                onDuplicatePart(item.id)
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
                                onDeletePart(item.id)
                            }
                        )
                    }
                }
            }
        }
    }
}
