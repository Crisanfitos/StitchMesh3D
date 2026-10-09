package com.crisanfitos.stitchmesh3d.ui.workspace.keyboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold

/**
 * Teclado virtual rápido de crochet acoplado a la parte inferior de la pantalla.
 *
 * Facilita la redacción ágil de instrucciones técnicas de amigurumi mediante
 * botones dedicados para abreviaturas frecuentes, modificadores, sintaxis y números.
 */
@Composable
fun CrochetQuickKeyboard(
    onTokenInserted: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true
) {
    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
        exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(StitchMeshSurfaceContainer)
                .border(
                    width = 1.dp,
                    color = StitchMeshSurfaceBorder,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Barra superior de herramientas del teclado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(StitchMeshTerracotta)
                    )
                    Text(
                        text = "TECLADO RÁPIDO DE CROCHET",
                        style = CrochetTypography.tokenBadge,
                        color = StitchMeshYarnGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardHide,
                        contentDescription = "Ocultar teclado",
                        tint = StitchMeshTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Fila 1: Puntos frecuentes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val stitchTokens = listOf("AM", "pb", "aum", "dism", "pe", "mpa", "pa", "cad")
                stitchTokens.forEach { token ->
                    CrochetKeyButton(
                        label = token,
                        onClick = { onTokenInserted(token) }
                    )
                }
            }

            // Fila 2: Modificadores y sintaxis matemática
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val syntaxTokens = listOf("BLO", "FLO", "[", "]", "*", "( )", "(", ")", ",")
                syntaxTokens.forEach { token ->
                    CrochetKeyButton(
                        label = token,
                        onClick = { onTokenInserted(token) }
                    )
                }
            }

            // Fila 3: Fila numérica 0-9
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                numbers.forEach { num ->
                    CrochetKeyButton(
                        label = num,
                        onClick = { onTokenInserted(num) }
                    )
                }
            }

            // Fila 4: Acciones (Espacio, Borrar, Intro)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Espacio
                CrochetKeyButton(
                    label = "Espacio",
                    icon = Icons.Default.SpaceBar,
                    onClick = { onTokenInserted(" ") },
                    modifier = Modifier.weight(1f)
                )

                // Borrar / Backspace
                CrochetKeyButton(
                    label = "Borrar",
                    icon = Icons.AutoMirrored.Filled.Backspace,
                    onClick = onBackspace,
                    modifier = Modifier.width(60.dp)
                )

                // Intro / Nueva vuelta
                CrochetKeyButton(
                    label = "Intro",
                    icon = Icons.AutoMirrored.Filled.KeyboardReturn,
                    isAccent = true,
                    onClick = onEnter,
                    modifier = Modifier.width(68.dp)
                )
            }
        }
    }
}

@Preview(name = "CrochetQuickKeyboard Preview", showBackground = true)
@Composable
private fun CrochetQuickKeyboardPreview() {
    StitchMesh3DTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CrochetQuickKeyboard(
                onTokenInserted = {},
                onBackspace = {},
                onEnter = {},
                onClose = {}
            )
        }
    }
}
