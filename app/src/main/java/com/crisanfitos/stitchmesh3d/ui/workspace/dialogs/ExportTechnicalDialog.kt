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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.crisanfitos.stitchmesh3d.core.export.ExportableMeshPart
import com.crisanfitos.stitchmesh3d.core.export.TechnicalExportManager
import com.crisanfitos.stitchmesh3d.domain.model.Project
import com.crisanfitos.stitchmesh3d.ui.export.ExportSuccessResultScreen
import com.crisanfitos.stitchmesh3d.ui.export.GeneratedExportFileUiModel
import com.crisanfitos.stitchmesh3d.ui.export.TechnicalExportSharer
import com.crisanfitos.stitchmesh3d.ui.export.components.ExportAssetFormat
import com.crisanfitos.stitchmesh3d.ui.export.components.ExportConfigurationState
import com.crisanfitos.stitchmesh3d.ui.export.components.ExportFormatSelectorCard
import com.crisanfitos.stitchmesh3d.ui.export.components.ExportProgressDialog
import com.crisanfitos.stitchmesh3d.ui.export.components.ExportProgressState
import com.crisanfitos.stitchmesh3d.ui.export.components.MeshResolutionSelector
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSageGreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextDisabled
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import kotlinx.coroutines.launch

/**
 * Etapas del flujo unificado de exportación técnica CAD (Pantallas 13, 14 y 15, SM-088, RF-5.3).
 */
enum class ExportFlowStage {
    CONFIGURATION,  // Pantalla 13: Configuración y formatos
    PROGRESS,       // Pantalla 14: Diálogo de compilación asíncrona
    SUCCESS         // Pantalla 15: Pantalla de resultado exitoso, guardado SAF y compartición
}

/**
 * Diálogo modal para la exportación técnica del proyecto (SM-038, SM-039, SM-056, SM-057, SM-058, SM-088).
 * Orquesta de extremo a extremo las Pantallas 13 (selector de formato), 14 (progreso) y 15 (resultados SAF/Share).
 */
@Composable
fun ExportTechnicalDialog(
    projectTitle: String,
    onDismiss: () -> Unit,
    onExportConfirmed: ((exportPdf: Boolean, exportModel3d: Boolean) -> Unit)? = null,
    project: Project? = null,
    meshParts: List<ExportableMeshPart> = emptyList(),
    modifier: Modifier = Modifier
) {
    var stage by remember { mutableStateOf(ExportFlowStage.CONFIGURATION) }
    var configState by remember { mutableStateOf(ExportConfigurationState()) }
    var progressState by remember { mutableStateOf(ExportProgressState()) }
    var generatedFiles by remember { mutableStateOf<List<GeneratedExportFileUiModel>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val effectiveProject = remember(project, projectTitle) {
        project ?: Project(
            id = "proj-active",
            title = projectTitle,
            isValid = true
        )
    }

    when (stage) {
        ExportFlowStage.CONFIGURATION -> {
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Cabecera
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Exportación Técnica CAD",
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
                            text = "Selecciona los entregables para compilar:",
                            style = MaterialTheme.typography.labelMedium,
                            color = StitchMeshTextSecondary
                        )

                        // Opción 1: Ficha Técnica PDF (Pantalla 13)
                        val isPdfSelected = ExportAssetFormat.PDF_SHEET in configState.selectedFormats
                        ExportOptionCard(
                            title = "Ficha Técnica PDF",
                            subtitle = "Patrón detallado, vueltas, cotas en mm y muestra de tensión 10x10",
                            icon = Icons.Default.Description,
                            isSelected = isPdfSelected,
                            testTag = "export_option_pdf",
                            onClick = { configState = configState.toggleFormat(ExportAssetFormat.PDF_SHEET) }
                        )

                        // Opción 2: Modelo 3D Wavefront OBJ + MTL
                        val isObjSelected = ExportAssetFormat.OBJ_MESH in configState.selectedFormats
                        ExportOptionCard(
                            title = "Malla 3D Wavefront (.OBJ + .MTL)",
                            subtitle = "Malla poligonal indexada, normales rotadas y materiales de hilado",
                            icon = Icons.Default.ViewInAr,
                            isSelected = isObjSelected,
                            testTag = "export_option_3d",
                            onClick = { configState = configState.toggleFormat(ExportAssetFormat.OBJ_MESH) }
                        )

                        // Opción 3: Escena 3D glTF 2.0 / GLB
                        val isGltfSelected = ExportAssetFormat.GLTF_MESH in configState.selectedFormats
                        ExportOptionCard(
                            title = "Escena 3D glTF 2.0 (.GLTF / .GLB)",
                            subtitle = "Búferes binarios PBR con jerarquía de nodos de piezas",
                            icon = Icons.Default.ViewInAr,
                            isSelected = isGltfSelected,
                            testTag = "export_option_gltf",
                            onClick = { configState = configState.toggleFormat(ExportAssetFormat.GLTF_MESH) }
                        )

                        // Selector de resolución de malla (Pantalla 13)
                        MeshResolutionSelector(
                            selectedResolution = configState.meshResolution,
                            onResolutionSelected = { configState = configState.copy(meshResolution = it) }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

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
                                    if (configState.canStartExport) {
                                        onExportConfirmed?.invoke(
                                            ExportAssetFormat.PDF_SHEET in configState.selectedFormats,
                                            ExportAssetFormat.OBJ_MESH in configState.selectedFormats ||
                                                ExportAssetFormat.GLTF_MESH in configState.selectedFormats
                                        )
                                        stage = ExportFlowStage.PROGRESS
                                        coroutineScope.launch {
                                            val files = TechnicalExportManager.executeExport(
                                                context = context,
                                                project = effectiveProject,
                                                meshParts = meshParts,
                                                config = configState
                                            ) { p -> progressState = p }
                                            generatedFiles = files
                                            stage = ExportFlowStage.SUCCESS
                                        }
                                    }
                                },
                                enabled = configState.canStartExport,
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
                                    color = if (configState.canStartExport) Color.White else StitchMeshTextDisabled,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        ExportFlowStage.PROGRESS -> {
            // Pantalla 14: Diálogo de compilación técnica con telemetría en tiempo real
            ExportProgressDialog(
                state = progressState,
                onCancelExport = { stage = ExportFlowStage.CONFIGURATION }
            )
        }

        ExportFlowStage.SUCCESS -> {
            // Pantalla 15: Pantalla de resultado exitoso con guardado SAF y compartición
            Dialog(onDismissRequest = onDismiss) {
                ExportSuccessResultScreen(
                    projectTitle = projectTitle,
                    generatedFiles = generatedFiles,
                    onSaveToDevice = { files ->
                        TechnicalExportSharer.saveFilesFeedback(context, files)
                    },
                    onShareFiles = { files ->
                        TechnicalExportSharer.shareFiles(context, files)
                    },
                    onClose = onDismiss
                )
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
