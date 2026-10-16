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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSageGreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextDisabled
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * Diálogo modal para la exportación técnica del proyecto (SM-038, SM-039, SM-071).
 * Permite seleccionar formatos de ficha técnica PDF, modelo 3D y especificaciones de tensión.
 */
@Composable
fun ExportTechnicalDialog(
    projectTitle: String,
    onDismiss: () -> Unit,
    onExportConfirmed: (exportPdf: Boolean, exportModel3d: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var exportPdf by remember { mutableStateOf(true) }
    var exportModel3d by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StitchMeshSurfaceContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, StitchMeshSurfaceBorder),
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .semantics { testTagsAsResourceId = true }
                .testTag("export_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Exportación Técnica",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = StitchMeshTextPrimary
                        )
                        Text(
                            text = projectTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchMeshTextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("export_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar diálogo",
                            tint = StitchMeshTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = "Selecciona los entregables para generar:",
                    style = MaterialTheme.typography.labelMedium,
                    color = StitchMeshTextSecondary
                )

                // Opción 1: Ficha Técnica PDF
                ExportOptionCard(
                    title = "Ficha Técnica PDF",
                    subtitle = "Patrón detallado, vueltas, cotas en mm y muestra 10x10",
                    icon = Icons.Default.Description,
                    isSelected = exportPdf,
                    testTag = "export_option_pdf",
                    onClick = { exportPdf = !exportPdf }
                )

                // Opción 2: Modelo 3D Paramétrico OBJ / GLTF
                ExportOptionCard(
                    title = "Modelo 3D (.OBJ / .GLTF)",
                    subtitle = "Malla poligonal indexada, coordenadas UV y colores",
                    icon = Icons.Default.ViewInAr,
                    isSelected = exportModel3d,
                    testTag = "export_option_3d",
                    onClick = { exportModel3d = !exportModel3d }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StitchMeshSurfaceBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("export_cancel_btn")
                    ) {
                        Text(
                            text = "Cancelar",
                            color = StitchMeshTextSecondary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = {
                            if (exportPdf || exportModel3d) {
                                onExportConfirmed(exportPdf, exportModel3d)
                            }
                        },
                        enabled = exportPdf || exportModel3d,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StitchMeshTerracotta,
                            disabledContainerColor = StitchMeshSurfaceHigh
                        ),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(44.dp)
                            .testTag("export_confirm_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Exportar",
                            color = if (exportPdf || exportModel3d) Color.White else StitchMeshTextDisabled,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) StitchMeshTerracotta else StitchMeshSurfaceBorder
    val backgroundColor = if (isSelected) StitchMeshTerracotta.copy(alpha = 0.08f) else StitchMeshSurfaceHigh

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) StitchMeshTerracotta else StitchMeshTextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = StitchMeshTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = StitchMeshTextSecondary
            )
        }
        Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = if (isSelected) "Seleccionado" else "No seleccionado",
            tint = if (isSelected) StitchMeshTerracotta else StitchMeshTextDisabled,
            modifier = Modifier.size(20.dp)
        )
    }
}
