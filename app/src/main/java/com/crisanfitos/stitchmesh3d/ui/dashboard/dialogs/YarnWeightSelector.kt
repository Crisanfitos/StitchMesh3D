package com.crisanfitos.stitchmesh3d.ui.dashboard.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextDisabled
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * Selector horizontal de categorías estándar CYC de grosor de hilado (#0 a #7).
 */
@Composable
fun YarnWeightSelector(
    selectedWeight: YarnWeightCategory,
    onSelectWeight: (YarnWeightCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Grosor de Hilado (CYC Standard)",
            style = MaterialTheme.typography.labelMedium,
            color = StitchMeshTextSecondary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            YarnWeightCategory.entries.forEach { category ->
                val isSelected = category == selectedWeight
                YarnWeightCard(
                    category = category,
                    isSelected = isSelected,
                    onClick = { onSelectWeight(category) }
                )
            }
        }
    }
}

@Composable
private fun YarnWeightCard(
    category: YarnWeightCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        StitchMeshTerracotta.copy(alpha = 0.15f)
    } else {
        StitchMeshSurfaceHigh
    }

    val borderColor = if (isSelected) {
        StitchMeshTerracotta
    } else {
        StitchMeshSurfaceBorder
    }

    val titleColor = if (isSelected) {
        StitchMeshTerracotta
    } else {
        StitchMeshTextPrimary
    }

    Box(
        modifier = modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${category.code}",
                    style = CrochetTypography.tokenBadge,
                    color = titleColor
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StitchMeshTerracotta)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ACTIVO",
                            style = CrochetTypography.tokenBadge,
                            color = StitchMeshTextPrimary
                        )
                    }
                }
            }

            Text(
                text = category.displayName.substringBefore(" /"),
                style = MaterialTheme.typography.titleSmall,
                color = StitchMeshTextPrimary,
                maxLines = 1
            )

            Text(
                text = "${category.minHookSizeMm} - ${category.maxHookSizeMm} mm",
                style = CrochetTypography.matrixValue,
                color = if (isSelected) StitchMeshTextSecondary else StitchMeshTextDisabled
            )
        }
    }
}

@Preview(name = "YarnWeightSelector - Dark", showBackground = true)
@Composable
private fun YarnWeightSelectorPreview() {
    StitchMesh3DTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            YarnWeightSelector(
                selectedWeight = YarnWeightCategory.MEDIUM,
                onSelectWeight = {}
            )
        }
    }
}
