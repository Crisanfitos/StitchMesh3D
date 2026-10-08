package com.crisanfitos.stitchmesh3d.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSageGreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold

/**
 * Estado formal de validación del linter en la biblioteca de proyectos.
 */
enum class LinterValidationStatus {
    VALID,
    HAS_ERRORS,
    WARNING
}

private data class StatusBadgeConfig(
    val accentColor: Color,
    val icon: ImageVector,
    val defaultLabel: String
)

/**
 * Chip indicador de estado del linter aritmético.
 * Diseñado con alto contraste y semántica de color para el entorno CAD de StitchMesh.
 */
@Composable
fun StatusBadgeChip(
    status: LinterValidationStatus,
    modifier: Modifier = Modifier,
    customLabel: String? = null
) {
    val config = when (status) {
        LinterValidationStatus.VALID -> StatusBadgeConfig(
            accentColor = StitchMeshSageGreen,
            icon = Icons.Default.Check,
            defaultLabel = "Válido"
        )
        LinterValidationStatus.HAS_ERRORS -> StatusBadgeConfig(
            accentColor = StitchMeshCoralRed,
            icon = Icons.Default.Close,
            defaultLabel = "Con errores"
        )
        LinterValidationStatus.WARNING -> StatusBadgeConfig(
            accentColor = StitchMeshYarnGold,
            icon = Icons.Default.Warning,
            defaultLabel = "Revisión"
        )
    }

    val labelText = customLabel ?: config.defaultLabel
    val backgroundColor = config.accentColor.copy(alpha = 0.15f)
    val borderColor = config.accentColor.copy(alpha = 0.40f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(config.accentColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = config.icon,
                contentDescription = labelText,
                tint = Color.Black,
                modifier = Modifier.size(10.dp)
            )
        }
        Text(
            text = labelText,
            style = CrochetTypography.tokenBadge,
            color = config.accentColor
        )
    }
}

@Preview(name = "StatusBadgeChip Valid - Dark", showBackground = true)
@Composable
private fun StatusBadgeChipValidPreview() {
    StitchMesh3DTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            StatusBadgeChip(status = LinterValidationStatus.VALID)
        }
    }
}

@Preview(name = "StatusBadgeChip Errors - Dark", showBackground = true)
@Composable
private fun StatusBadgeChipErrorsPreview() {
    StitchMesh3DTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            StatusBadgeChip(
                status = LinterValidationStatus.HAS_ERRORS,
                customLabel = "2 errores"
            )
        }
    }
}

@Preview(name = "StatusBadgeChip Warning - Dark", showBackground = true)
@Composable
private fun StatusBadgeChipWarningPreview() {
    StitchMesh3DTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            StatusBadgeChip(status = LinterValidationStatus.WARNING)
        }
    }
}
