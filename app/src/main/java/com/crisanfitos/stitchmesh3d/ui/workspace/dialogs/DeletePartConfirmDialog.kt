package com.crisanfitos.stitchmesh3d.ui.workspace.dialogs

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.Typography

@Composable
fun DeletePartConfirmDialog(
    partName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = StitchMeshCoralRed
            )
        },
        title = {
            Text(
                text = "¿Eliminar pieza '$partName'?",
                style = Typography.titleLarge,
                color = StitchMeshTextPrimary
            )
        },
        text = {
            Text(
                text = "Se eliminarán permanentemente todas las vueltas y la geometría 3D generadas para esta pieza. Esta acción no se puede deshacer.",
                style = Typography.bodyMedium,
                color = StitchMeshTextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = StitchMeshCoralRed)
            ) {
                Text("Eliminar Pieza")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", color = StitchMeshTextSecondary)
            }
        },
        containerColor = StitchMeshSurfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    )
}
