package com.crisanfitos.stitchmesh3d.ui.assembly.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold
import com.crisanfitos.stitchmesh3d.ui.theme.Typography

/**
 * Panel lateral inspector para control numérico milimétrico de posición (X, Y, Z)
 * y rotación (Roll, Pitch, Yaw) de la pieza seleccionada (Stitch screen 14f11294, SM-054).
 */
@Composable
fun TransformInspectorPanel(
    transform: PartTransformState?,
    onPositionChange: (x: Float, y: Float, z: Float) -> Unit,
    onRotationChange: (rotX: Float, rotY: Float, rotZ: Float) -> Unit,
    onMirrorToggled: () -> Unit,
    onResetOrigin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StitchMeshSurfaceContainer)
            .border(width = 1.dp, color = StitchMeshSurfaceBorder, shape = RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("transform_inspector_panel")
    ) {
        // Cabecera del Inspector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = StitchMeshTerracotta,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Inspector CAD",
                        style = Typography.titleMedium,
                        color = StitchMeshTextPrimary
                    )
                    Text(
                        text = if (transform != null) transform.partName else "Ninguna pieza seleccionada",
                        style = CrochetTypography.tokenBadge,
                        color = if (transform != null) StitchMeshTerracotta else StitchMeshTextSecondary
                    )
                }
            }

            if (transform != null && transform.isLocked) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(StitchMeshYarnGold.copy(alpha = 0.15f))
                        .border(1.dp, StitchMeshYarnGold, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = StitchMeshYarnGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "BLOQUEADA",
                            style = CrochetTypography.tokenBadge,
                            color = StitchMeshYarnGold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(StitchMeshSurfaceBorder)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (transform == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Selecciona una pieza en el árbol Scene Graph para inspeccionar y ajustar su posición.",
                    style = Typography.bodyMedium,
                    color = StitchMeshTextSecondary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Sección Traslación Relativa
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Traslación Relativa (mm)",
                        style = CrochetTypography.tokenBadge,
                        color = StitchMeshTextSecondary
                    )

                    NumericCoordinateRow(
                        axisLabel = "X",
                        value = transform.posX,
                        unit = "mm",
                        step = 0.5f,
                        enabled = !transform.isLocked,
                        onValueChange = { newX ->
                            onPositionChange(newX, transform.posY, transform.posZ)
                        }
                    )

                    NumericCoordinateRow(
                        axisLabel = "Y",
                        value = transform.posY,
                        unit = "mm",
                        step = 0.5f,
                        enabled = !transform.isLocked,
                        onValueChange = { newY ->
                            onPositionChange(transform.posX, newY, transform.posZ)
                        }
                    )

                    NumericCoordinateRow(
                        axisLabel = "Z",
                        value = transform.posZ,
                        unit = "mm",
                        step = 0.5f,
                        enabled = !transform.isLocked,
                        onValueChange = { newZ ->
                            onPositionChange(transform.posX, transform.posY, newZ)
                        }
                    )
                }

                // Sección Rotación Espacial
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Rotación Espacial (°)",
                        style = CrochetTypography.tokenBadge,
                        color = StitchMeshTextSecondary
                    )

                    NumericCoordinateRow(
                        axisLabel = "Roll",
                        value = transform.rotX,
                        unit = "°",
                        step = 5.0f,
                        enabled = !transform.isLocked,
                        onValueChange = { newRotX ->
                            onRotationChange(newRotX, transform.rotY, transform.rotZ)
                        }
                    )

                    NumericCoordinateRow(
                        axisLabel = "Pitch",
                        value = transform.rotY,
                        unit = "°",
                        step = 5.0f,
                        enabled = !transform.isLocked,
                        onValueChange = { newRotY ->
                            onRotationChange(transform.rotX, newRotY, transform.rotZ)
                        }
                    )

                    NumericCoordinateRow(
                        axisLabel = "Yaw",
                        value = transform.rotZ,
                        unit = "°",
                        step = 5.0f,
                        enabled = !transform.isLocked,
                        onValueChange = { newRotZ ->
                            onRotationChange(transform.rotX, transform.rotY, newRotZ)
                        }
                    )
                }

                // Sección Acciones y Simetría
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Herramientas de Ensamblado",
                        style = CrochetTypography.tokenBadge,
                        color = StitchMeshTextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Botón Simetría / Espejo
                        OutlinedButton(
                            onClick = onMirrorToggled,
                            enabled = !transform.isLocked,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mirror_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = if (transform.isMirrored) StitchMeshTerracotta else StitchMeshTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (transform.isMirrored) "Espejo (Activo)" else "Espejo Sagital",
                                style = Typography.labelMedium,
                                color = if (transform.isMirrored) StitchMeshTerracotta else StitchMeshTextPrimary
                            )
                        }

                        // Botón Restablecer Origen
                        Button(
                            onClick = onResetOrigin,
                            enabled = !transform.isLocked,
                            colors = ButtonDefaults.buttonColors(containerColor = StitchMeshSurfaceHigh),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reset_origin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = StitchMeshTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reset (0,0,0)",
                                style = Typography.labelMedium,
                                color = StitchMeshTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
