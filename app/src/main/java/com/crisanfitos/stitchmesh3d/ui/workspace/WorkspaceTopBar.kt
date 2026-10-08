package com.crisanfitos.stitchmesh3d.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crisanfitos.stitchmesh3d.ui.dashboard.components.LinterValidationStatus
import com.crisanfitos.stitchmesh3d.ui.dashboard.components.StatusBadgeChip
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshOnAccent
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextDisabled
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * Barra superior del espacio de trabajo técnico CAD de StitchMesh 3D.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceTopBar(
    projectTitle: String,
    hookSizeMm: Float,
    yarnWeightName: String,
    validationStatus: LinterValidationStatus,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCalibrateTensionClick: (() -> Unit)? = null,
    onExportClick: (() -> Unit)? = null
) {
    TopAppBar(
        modifier = modifier.border(width = 1.dp, color = StitchMeshSurfaceBorder),
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = projectTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StitchMeshTextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StitchMeshTerracotta)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CAD WORKBENCH",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchMeshOnAccent
                        )
                    }
                }
                Text(
                    text = "Gancho $hookSizeMm mm · $yarnWeightName",
                    style = MaterialTheme.typography.labelSmall,
                    color = StitchMeshTextSecondary
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver a la Biblioteca",
                    tint = StitchMeshTextPrimary
                )
            }
        },
        actions = {
            StatusBadgeChip(status = validationStatus)

            Spacer(modifier = Modifier.width(6.dp))

            if (onCalibrateTensionClick != null) {
                IconButton(onClick = onCalibrateTensionClick) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = "Muestra de tensión",
                        tint = StitchMeshTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (onExportClick != null) {
                IconButton(onClick = onExportClick) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Exportar modelo y ficha técnica",
                        tint = StitchMeshTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = StitchMeshSurfaceContainer
        )
    )
}

@Preview(name = "WorkspaceTopBar Preview", showBackground = true)
@Composable
private fun WorkspaceTopBarPreview() {
    StitchMesh3DTheme {
        WorkspaceTopBar(
            projectTitle = "Zorro Amigurumi",
            hookSizeMm = 3.5f,
            yarnWeightName = "#4 Worsted",
            validationStatus = LinterValidationStatus.VALID,
            onBackClick = {}
        )
    }
}
