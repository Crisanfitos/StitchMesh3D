package com.crisanfitos.stitchmesh3d.ui.export

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshOnAccent
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSageGreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * Constructor de intents de Android para guardar en dispositivo (Storage Access Framework)
 * y compartir ficheros generados (SM-058).
 */
object ExportSystemIntents {
    const val ACTION_CREATE_DOCUMENT = Intent.ACTION_CREATE_DOCUMENT
    const val ACTION_SEND = Intent.ACTION_SEND

    fun buildSaveDocumentAction(file: GeneratedExportFileUiModel): Pair<String, String> {
        return ACTION_CREATE_DOCUMENT to file.fileName
    }

    fun buildShareAction(files: List<GeneratedExportFileUiModel>): Pair<String, String> {
        val primaryMime = files.firstOrNull()?.mimeType ?: "*/*"
        return ACTION_SEND to primaryMime
    }
}

/**
 * Pantalla/tarjeta de finalización de exportación con listado de archivos generados,
 * acción de guardado SAF y compartición nativa (Stitch Screen a6516227, SM-058).
 */
@Composable
fun ExportSuccessResultScreen(
    projectTitle: String,
    generatedFiles: List<GeneratedExportFileUiModel>,
    onSaveToDevice: (List<GeneratedExportFileUiModel>) -> Unit,
    onShareFiles: (List<GeneratedExportFileUiModel>) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("export_success_result_screen"),
        shape = RoundedCornerShape(16.dp),
        color = StitchMeshSurfaceContainer,
        border = BorderStroke(1.dp, StitchMeshSurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(StitchMeshSageGreen.copy(alpha = 0.16f))
                    .border(1.dp, StitchMeshSageGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Exportación completada",
                    tint = StitchMeshSageGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Exportación Completada",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = StitchMeshTextPrimary
                )
                Text(
                    text = "$projectTitle · ${generatedFiles.size} archivo(s) listos",
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchMeshTextSecondary
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                generatedFiles.forEach { file ->
                    GeneratedFileCard(file = file)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onShareFiles(generatedFiles) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, StitchMeshSurfaceBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("export_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = StitchMeshTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Compartir",
                        color = StitchMeshTextPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = { onSaveToDevice(generatedFiles) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StitchMeshTerracotta),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(46.dp)
                        .testTag("export_save_device_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = null,
                        tint = StitchMeshOnAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Guardar en Dispositivo",
                        color = StitchMeshOnAccent,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            TextButton(
                onClick = onClose,
                modifier = Modifier.testTag("export_result_close_button")
            ) {
                Text(
                    text = "Volver al espacio de trabajo",
                    color = StitchMeshTextSecondary
                )
            }
        }
    }
}

@Preview(name = "ExportSuccessResultScreen Dark", showBackground = true)
@Composable
private fun ExportSuccessResultScreenPreview() {
    StitchMesh3DTheme {
        ExportSuccessResultScreen(
            projectTitle = "Zorro Amigurumi",
            generatedFiles = listOf(
                GeneratedExportFileUiModel("zorro_ficha.pdf", "application/pdf", 1_468_006L),
                GeneratedExportFileUiModel("zorro_modelo.glb", "model/gltf-binary", 3_984_588L)
            ),
            onSaveToDevice = {},
            onShareFiles = {},
            onClose = {}
        )
    }
}
