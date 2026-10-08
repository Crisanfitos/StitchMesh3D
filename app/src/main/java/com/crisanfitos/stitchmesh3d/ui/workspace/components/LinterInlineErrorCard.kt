package com.crisanfitos.stitchmesh3d.ui.workspace.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crisanfitos.stitchmesh3d.core.engine.validator.CorrectionSuggestion
import com.crisanfitos.stitchmesh3d.core.engine.validator.SuggestionActionType
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshOnAccent
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold

/**
 * Tarjeta expandible de error inline del Linter formal para una vuelta.
 *
 * Muestra el diagnóstico del fallo aritmético o sintáctico de la vuelta y
 * ofrece chips de acción rápida Quick-Fix para solventarlo con 1 sola pulsación.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LinterInlineErrorCard(
    errorMessage: String,
    suggestions: List<CorrectionSuggestion>,
    onApplySuggestion: (CorrectionSuggestion) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    consumedStitches: Int? = null,
    expectedBaseStitches: Int? = null,
    producedStitches: Int? = null,
    declaredStitches: Int? = null
) {
    AnimatedVisibility(
        visible = isVisible && errorMessage.isNotBlank(),
        enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
        exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180)),
        modifier = modifier
    ) {
        val discrepancyBadgeText: String? = when {
            consumedStitches != null && expectedBaseStitches != null && consumedStitches != expectedBaseStitches -> {
                val diff = consumedStitches - expectedBaseStitches
                if (diff > 0) "+$diff pb de exceso" else "$diff pb faltantes"
            }
            producedStitches != null && declaredStitches != null && producedStitches != declaredStitches -> {
                val diff = producedStitches - declaredStitches
                if (diff > 0) "+$diff pt declarados" else "$diff pt declarados"
            }
            else -> null
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(StitchMeshSurfaceHigh)
                .border(
                    width = 1.dp,
                    color = StitchMeshCoralRed.copy(alpha = 0.50f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cabecera de error con badge de discrepancia
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Inconsistencia del linter",
                            tint = StitchMeshCoralRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "INCONSISTENCIA ARITMÉTICA",
                            style = CrochetTypography.tokenBadge,
                            color = StitchMeshCoralRed,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (discrepancyBadgeText != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StitchMeshCoralRed.copy(alpha = 0.20f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = discrepancyBadgeText,
                                style = CrochetTypography.tokenBadge,
                                color = StitchMeshCoralRed,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Detalle explicativo de la discrepancia
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = StitchMeshTextSecondary
                )

                // Sección de sugerencias Quick-Fix
                if (suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Sugerencias Quick-Fix (1 clic):",
                        style = MaterialTheme.typography.labelSmall,
                        color = StitchMeshYarnGold,
                        fontWeight = FontWeight.SemiBold
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        suggestions.forEach { suggestion ->
                            QuickFixActionChip(
                                suggestion = suggestion,
                                onClick = { onApplySuggestion(suggestion) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "LinterInlineErrorCard Preview", showBackground = true)
@Composable
private fun LinterInlineErrorCardPreview() {
    StitchMesh3DTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            LinterInlineErrorCard(
                errorMessage = "La sumatoria de puntos consumidos (10) no coincide con los puntos base disponibles (12). Faltan 2 puntos.",
                consumedStitches = 10,
                expectedBaseStitches = 12,
                suggestions = listOf(
                    CorrectionSuggestion(
                        title = "Ajustar repeticiones a * 6",
                        explanation = "Ajusta el multiplicador para consumir exactamente 12 puntos",
                        actionType = SuggestionActionType.ADJUST_MULTIPLIER,
                        originalText = "[1 pb, 1 aum] * 5 (15)",
                        correctedText = "[1 pb, 1 aum] * 6 (18)"
                    ),
                    CorrectionSuggestion(
                        title = "Añadir 2 pb al final",
                        explanation = "Completa los 2 puntos base restantes",
                        actionType = SuggestionActionType.APPEND_STITCHES,
                        originalText = "[1 pb, 1 aum] * 5 (15)",
                        correctedText = "[1 pb, 1 aum] * 5, 2 pb (17)"
                    )
                ),
                onApplySuggestion = {}
            )
        }
    }
}
