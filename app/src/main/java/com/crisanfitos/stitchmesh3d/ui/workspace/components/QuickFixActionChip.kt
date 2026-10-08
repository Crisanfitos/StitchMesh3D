package com.crisanfitos.stitchmesh3d.ui.workspace.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.core.engine.validator.CorrectionSuggestion
import com.crisanfitos.stitchmesh3d.core.engine.validator.SuggestionActionType
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracottaDark

/**
 * Chip de acción rápida Quick-Fix para aplicar una sugerencia matemática en 1 clic.
 */
@Composable
fun QuickFixActionChip(
    suggestion: CorrectionSuggestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val backgroundColor = StitchMeshTerracotta.copy(alpha = 0.16f)
    val borderColor = StitchMeshTerracotta.copy(alpha = 0.45f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Sugerencia Quick-Fix",
            tint = StitchMeshTerracotta,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = suggestion.title,
            style = CrochetTypography.tokenBadge,
            color = StitchMeshTerracotta
        )
    }
}

@Preview(name = "QuickFixActionChip Preview", showBackground = true)
@Composable
private fun QuickFixActionChipPreview() {
    StitchMesh3DTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            QuickFixActionChip(
                suggestion = CorrectionSuggestion(
                    title = "Ajustar a 6 repeticiones",
                    explanation = "Consumirá exactamente los 12 puntos base",
                    actionType = SuggestionActionType.ADJUST_MULTIPLIER,
                    originalText = "[1 pb, 1 aum] * 5",
                    correctedText = "[1 pb, 1 aum] * 6 (18)"
                ),
                onClick = {}
            )
        }
    }
}
