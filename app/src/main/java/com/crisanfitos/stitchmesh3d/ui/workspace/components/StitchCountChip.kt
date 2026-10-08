package com.crisanfitos.stitchmesh3d.ui.workspace.components

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSageGreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold

/**
 * Chip indicador del conteo de puntos computados vs declarados de una vuelta.
 *
 * Muestra el recuento producido (ΣP) y, si difiere del declarado, resalta la discrepancia
 * en coral red con la diferencia aritmética (Δ).
 */
@Composable
fun StitchCountChip(
    producedCount: Int,
    declaredCount: Int? = null,
    isValid: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isMismatch = declaredCount != null && declaredCount != producedCount
    val effectiveValid = isValid && !isMismatch

    val accentColor = when {
        effectiveValid -> StitchMeshSageGreen
        isMismatch || !isValid -> StitchMeshCoralRed
        else -> StitchMeshYarnGold
    }

    val icon = when {
        effectiveValid -> Icons.Default.Check
        isMismatch || !isValid -> Icons.Default.Close
        else -> Icons.Default.Warning
    }

    val labelText = when {
        declaredCount != null && isMismatch -> "$producedCount/$declaredCount pt"
        declaredCount != null -> "$producedCount pt"
        else -> "$producedCount pt"
    }

    val backgroundColor = accentColor.copy(alpha = 0.15f)
    val borderColor = accentColor.copy(alpha = 0.40f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (effectiveValid) "Recuento válido" else "Inconsistencia de recuento",
                tint = Color.Black,
                modifier = Modifier.size(9.dp)
            )
        }
        Text(
            text = labelText,
            style = CrochetTypography.tokenBadge,
            color = accentColor
        )
    }
}

@Preview(name = "StitchCountChip Valid", showBackground = true)
@Composable
private fun StitchCountChipValidPreview() {
    StitchMesh3DTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            StitchCountChip(producedCount = 18, declaredCount = 18, isValid = true)
        }
    }
}

@Preview(name = "StitchCountChip Mismatch", showBackground = true)
@Composable
private fun StitchCountChipMismatchPreview() {
    StitchMesh3DTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            StitchCountChip(producedCount = 15, declaredCount = 18, isValid = false)
        }
    }
}
