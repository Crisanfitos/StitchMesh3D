package com.crisanfitos.stitchmesh3d.ui.workspace.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.workspace.keyboard.CrochetTokenFormatter
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSageGreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextDisabled
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold
import kotlinx.coroutines.delay

/**
 * Modelo de UI que representa una vuelta en el editor de patrones reactivo.
 */
data class RoundItemUiModel(
    val id: String,
    val roundNumber: Int,
    val rawInstruction: String,
    val producedStitches: Int = 0,
    val consumedStitches: Int = 0,
    val declaredStitches: Int? = null,
    val isValid: Boolean = true,
    val errorMessage: String? = null,
    val isHighlighted: Boolean = false,
    val colorHex: String? = null
)

/**
 * Acción de edición originada fuera del campo de texto (teclado rápido de crochet).
 * La fila activa la aplica sobre su propio cursor para mantener una única fuente de
 * verdad de la selección entre teclado rápido y teclado nativo (SM-062).
 */
data class RoundExternalEdit(
    val roundId: String,
    val nonce: Long,
    val action: Action
) {
    sealed interface Action {
        data class InsertToken(val token: String) : Action
        data object Backspace : Action
    }
}

/**
 * Fila de edición de vuelta para el Workspace de StitchMesh 3D.
 *
 * Características principales:
 * - Rail visual lateral: SageGreen (válido), CoralRed (error sintáctico/invariante), SurfaceBorder (vacío).
 * - Identificador de vuelta monospace: V1..VN.
 * - Chip selector de color de vuelta asignado a la hilera (TRD §2, RF-4.3).
 * - Campo de texto de fórmula con tipografía monoespaciada (JetBrains Mono).
 * - Debounce configurable (por defecto 90 ms) para no sobrecargar el linter durante la escritura rápida.
 * - Chip reactivo de puntos computados vs declarados.
 */
@Composable
fun RoundItemRow(
    round: RoundItemUiModel,
    onInstructionChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRowClicked: (() -> Unit)? = null,
    onColorClick: (() -> Unit)? = null,
    onDeleteRound: (() -> Unit)? = null,
    debounceMs: Long = 90L,
    enabled: Boolean = true,
    requestInitialFocus: Boolean = false,
    onFocusRequested: (() -> Unit)? = null,
    externalEdit: RoundExternalEdit? = null
) {
    var fieldValue by remember(round.id) {
        mutableStateOf(TextFieldValue(round.rawInstruction, TextRange(round.rawInstruction.length)))
    }
    // Textos ya emitidos al ViewModel: sus ecos no deben pisar la edición en curso.
    val sentTexts = remember(round.id) { HashSet<String>() }
    var lastAppliedNonce by remember(round.id) { mutableStateOf(externalEdit?.nonce ?: 0L) }

    // Adoptar solo cambios externos reales (p. ej. Quick-Fix), nunca el eco del propio tecleo
    LaunchedEffect(round.rawInstruction) {
        val upstream = round.rawInstruction
        if (upstream != fieldValue.text && upstream !in sentTexts) {
            fieldValue = TextFieldValue(upstream, TextRange(upstream.length))
            sentTexts.clear()
        }
    }

    // Aplicar ediciones del teclado rápido sobre la posición real del cursor
    LaunchedEffect(externalEdit?.nonce) {
        val edit = externalEdit
        if (edit != null && edit.roundId == round.id && edit.nonce != lastAppliedNonce) {
            lastAppliedNonce = edit.nonce
            val sel = fieldValue.selection
            val (newText, newCursor) = when (val action = edit.action) {
                is RoundExternalEdit.Action.InsertToken ->
                    CrochetTokenFormatter.insertTokenReplacingSelection(
                        fieldValue.text, action.token, sel.start, sel.end
                    )
                RoundExternalEdit.Action.Backspace ->
                    CrochetTokenFormatter.backspaceAt(fieldValue.text, sel.start, sel.end)
            }
            fieldValue = TextFieldValue(newText, TextRange(newCursor))
        }
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            focusRequester.requestFocus()
            onFocusRequested?.invoke()
        }
    }

    LaunchedEffect(fieldValue.text) {
        if (fieldValue.text != round.rawInstruction) {
            delay(debounceMs)
            sentTexts.add(fieldValue.text)
            onInstructionChanged(fieldValue.text)
        }
    }

    val localText = fieldValue.text

    val railColor = when {
        localText.isBlank() -> StitchMeshSurfaceBorder
        round.isValid -> StitchMeshSageGreen
        else -> StitchMeshCoralRed
    }

    val containerBorderColor = if (round.isHighlighted) {
        StitchMeshTerracotta
    } else {
        StitchMeshSurfaceBorder
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(StitchMeshSurfaceContainer)
            .border(
                width = if (round.isHighlighted) 1.5.dp else 1.dp,
                color = containerBorderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .then(if (onRowClicked != null) Modifier.clickable(onClick = onRowClicked) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rail visual lateral indicador de validez formal (4dp)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(railColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Badge de número de vuelta
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(StitchMeshSurfaceHigh)
                        .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "V${round.roundNumber}",
                        style = CrochetTypography.tokenBadge,
                        color = StitchMeshYarnGold
                    )
                }

                // Chip selector de color de vuelta (TRD §2, RF-4.3)
                val swatchColor = round.colorHex?.let {
                    runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
                } ?: StitchMeshTerracotta

                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .border(1.dp, StitchMeshSurfaceBorder, CircleShape)
                        .then(if (onColorClick != null) Modifier.clickable(onClick = onColorClick) else Modifier)
                )

                // Editor de texto de fórmula con debounce y tipografía monoespaciada
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (localText.isEmpty()) {
                        Text(
                            text = "ej: 6 pb o [1 pb, 1 aum] * 6",
                            style = CrochetTypography.formulaInput,
                            color = StitchMeshTextDisabled
                        )
                    }
                    BasicTextField(
                        value = fieldValue,
                        onValueChange = { newValue ->
                            if (newValue.text.length == fieldValue.text.length + 1 && newValue.selection.collapsed) {
                                val (smartText, smartCursor) = CrochetTokenFormatter.autoCloseDelimiters(
                                    oldText = fieldValue.text,
                                    newText = newValue.text,
                                    newCursor = newValue.selection.start
                                )
                                fieldValue = TextFieldValue(smartText, TextRange(smartCursor))
                            } else if (fieldValue.selection.collapsed && newValue.selection.collapsed &&
                                newValue.text.length == fieldValue.text.length - 1 &&
                                newValue.selection.start == fieldValue.selection.start - 1
                            ) {
                                val (smartText, smartCursor) = CrochetTokenFormatter.smartDeleteBeforeCursor(
                                    currentText = fieldValue.text,
                                    cursorPosition = fieldValue.selection.start
                                )
                                fieldValue = TextFieldValue(smartText, TextRange(smartCursor))
                            } else {
                                fieldValue = newValue
                            }
                        },
                        enabled = enabled,
                        singleLine = true,
                        textStyle = CrochetTypography.formulaInput.copy(color = StitchMeshTextPrimary),
                        cursorBrush = SolidColor(StitchMeshTerracotta),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    onRowClicked?.invoke()
                                }
                            }
                    )
                }

                // Chip de recuento de puntos
                StitchCountChip(
                    producedCount = round.producedStitches,
                    declaredCount = round.declaredStitches,
                    isValid = round.isValid
                )

                // Botón opcional de eliminación
                if (onDeleteRound != null) {
                    IconButton(
                        onClick = onDeleteRound,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar vuelta ${round.roundNumber}",
                            tint = StitchMeshTextDisabled,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "RoundItemRow Valid - Dark", showBackground = true)
@Composable
private fun RoundItemRowValidPreview() {
    StitchMesh3DTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RoundItemRow(
                round = RoundItemUiModel(
                    id = "r-1",
                    roundNumber = 3,
                    rawInstruction = "[1 pb, 1 aum] * 6 (18)",
                    producedStitches = 18,
                    consumedStitches = 12,
                    declaredStitches = 18,
                    isValid = true
                ),
                onInstructionChanged = {}
            )
        }
    }
}

@Preview(name = "RoundItemRow Error - Dark", showBackground = true)
@Composable
private fun RoundItemRowErrorPreview() {
    StitchMesh3DTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RoundItemRow(
                round = RoundItemUiModel(
                    id = "r-2",
                    roundNumber = 3,
                    rawInstruction = "[1 pb, 1 aum] * 5 (15)",
                    producedStitches = 15,
                    consumedStitches = 10,
                    declaredStitches = 15,
                    isValid = false,
                    errorMessage = "Faltan 2 puntos base por consumir"
                ),
                onInstructionChanged = {}
            )
        }
    }
}
