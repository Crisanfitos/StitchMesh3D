package com.crisanfitos.stitchmesh3d.ui.export

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSageGreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import java.util.Locale

/**
 * Modelo de UI para un archivo generado tras la exportación técnica (SM-058).
 */
data class GeneratedExportFileUiModel(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localPathOrUri: String = ""
) {
    /**
     * Formatea el tamaño en MB (o KB si es menor de 100 KB) con 1 decimal en punto (`.`).
     */
    val formattedSizeLabel: String
        get() {
            val mb = sizeBytes.toDouble() / (1024.0 * 1024.0)
            return if (mb >= 0.1) {
                String.format(Locale.US, "%.1f MB", mb)
            } else {
                val kb = (sizeBytes.toDouble() / 1024.0).coerceAtLeast(0.1)
                String.format(Locale.US, "%.1f KB", kb)
            }
        }

    val is3dModel: Boolean
        get() = fileName.endsWith(".obj", ignoreCase = true) ||
            fileName.endsWith(".gltf", ignoreCase = true) ||
            fileName.endsWith(".glb", ignoreCase = true)
}

/**
 * Tarjeta descriptiva de un fichero exportado mostrando nombre, icono y tamaño en MB (SM-058).
 */
@Composable
fun GeneratedFileCard(
    file: GeneratedExportFileUiModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StitchMeshSurfaceHigh)
            .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("generated_file_card_${file.fileName}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (file.is3dModel) Icons.Default.ViewInAr else Icons.Default.Description,
            contentDescription = null,
            tint = if (file.is3dModel) StitchMeshTerracotta else StitchMeshSageGreen,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = StitchMeshTextPrimary
            )
            Text(
                text = file.mimeType,
                style = MaterialTheme.typography.labelSmall,
                color = StitchMeshTextSecondary
            )
        }
        Text(
            text = file.formattedSizeLabel,
            style = CrochetTypography.tokenBadge,
            color = StitchMeshSageGreen,
            modifier = Modifier.testTag("generated_file_size_${file.fileName}")
        )
    }
}

@Preview(name = "GeneratedFileCard Dark", showBackground = true)
@Composable
private fun GeneratedFileCardPreview() {
    StitchMesh3DTheme {
        GeneratedFileCard(
            file = GeneratedExportFileUiModel(
                fileName = "zorro_amigurumi_ficha.pdf",
                mimeType = "application/pdf",
                sizeBytes = 1_468_006L
            )
        )
    }
}
