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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * Chip de información técnica sobre la tensión configurada del proyecto.
 * Muestra el calibre de aguja y la clasificación estándar CYC del hilado.
 */
@Composable
fun TensionBadgeChip(
    yarnWeightLabel: String,
    hookSizeMm: Float,
    modifier: Modifier = Modifier
) {
    val formattedHook = if (hookSizeMm % 1.0f == 0.0f) {
        String.format("%.0f mm", hookSizeMm)
    } else {
        String.format("%.1f mm", hookSizeMm)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(StitchMeshSurfaceHigh)
            .border(
                width = 1.dp,
                color = StitchMeshSurfaceBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Indicador circular de acento de hilado
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(StitchMeshTerracotta)
        )
        Text(
            text = "$yarnWeightLabel • $formattedHook",
            style = CrochetTypography.tokenBadge,
            color = StitchMeshTextPrimary
        )
    }
}

@Preview(name = "TensionBadgeChip - Dark", showBackground = true)
@Composable
private fun TensionBadgeChipPreview() {
    StitchMesh3DTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TensionBadgeChip(
                yarnWeightLabel = "#4 Worsted",
                hookSizeMm = 3.5f
            )
        }
    }
}
