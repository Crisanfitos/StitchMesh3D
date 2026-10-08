package com.crisanfitos.stitchmesh3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crisanfitos.stitchmesh3d.core.engine.model.StitchRegistry
import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshNeutralDark
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshOnAccent
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSageGreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StitchMesh3DTheme {
                StitchMeshWorkbenchScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StitchMeshWorkbenchScreen() {
    var selectedStitch by remember { mutableStateOf<StitchType>(StitchType.SingleCrochet) }
    var formulaText by remember { mutableStateOf("6 pb, 1 aum, 2 pb") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = StitchMeshNeutralDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "StitchMesh 3D",
                                style = MaterialTheme.typography.titleLarge,
                                color = StitchMeshTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(StitchMeshTerracotta)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "CAD ENGINE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchMeshOnAccent
                                )
                            }
                        }
                        Text(
                            text = "Amigurumi & Crochet Parametric Linter",
                            style = MaterialTheme.typography.labelSmall,
                            color = StitchMeshTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StitchMeshSurfaceContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección 1: Explorador del Catálogo Maestro de Puntos
            item {
                Text(
                    text = "CATÁLOGO DE PUNTADAS (6-TUPLA FORMAL)",
                    style = MaterialTheme.typography.labelLarge,
                    color = StitchMeshYarnGold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sampleStitches = listOf(
                        StitchType.SingleCrochet,
                        StitchType.Increase,
                        StitchType.Decrease,
                        StitchType.MagicRing(6),
                        StitchType.HalfDoubleCrochet,
                        StitchType.DoubleCrochet,
                        StitchType.SlipStitch,
                        StitchType.Chain,
                        StitchType.TripleIncrease,
                        StitchType.TripleDecrease,
                        StitchType.InvisibleDecrease
                    )

                    sampleStitches.forEach { stitch ->
                        val isSelected = selectedStitch::class == stitch::class
                        val label = stitch.spanishAbbreviations.firstOrNull() ?: stitch.technicalName
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) StitchMeshTerracotta else StitchMeshSurfaceHigh)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) StitchMeshTerracotta else StitchMeshSurfaceBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedStitch = stitch }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = CrochetTypography.tokenBadge,
                                color = if (isSelected) StitchMeshOnAccent else StitchMeshTextPrimary
                            )
                        }
                    }
                }
            }

            // Sección 2: Ficha Técnica de la Puntada Seleccionada
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StitchMeshSurfaceContainer),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StitchMeshSurfaceBorder))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedStitch.technicalName,
                                style = MaterialTheme.typography.titleMedium,
                                color = StitchMeshTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            val delta = selectedStitch.deltaStitches
                            val deltaColor = when {
                                delta > 0 -> StitchMeshSageGreen
                                delta < 0 -> StitchMeshCoralRed
                                else -> StitchMeshTextSecondary
                            }
                            Text(
                                text = if (delta >= 0) "+$delta pt" else "$delta pt",
                                style = CrochetTypography.matrixValue,
                                color = deltaColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Cuadrícula 2x3 de parámetros 6-tupla
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ParameterBadge("C (Consumido)", "${selectedStitch.consumedStitches}")
                            ParameterBadge("P (Producido)", "${selectedStitch.producedStitches}")
                            ParameterBadge("Δ (Variación)", "${selectedStitch.deltaStitches}")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ParameterBadge("h_rel (Alto)", "${selectedStitch.hRel}x")
                            ParameterBadge("w_rel (Ancho)", "${selectedStitch.wRel}x")
                            ParameterBadge("Δr (Radial)", "${selectedStitch.deltaRRel}")
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Abreviaturas ES: ${selectedStitch.spanishAbbreviations.joinToString()} | US: ${selectedStitch.englishAbbreviations.joinToString()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StitchMeshTextSecondary
                        )
                    }
                }
            }

            // Sección 3: Calculador de Vuelta Reactivo
            item {
                Text(
                    text = "CALCULADORA DE VUELTA (PREVIEW LINTER)",
                    style = MaterialTheme.typography.labelLarge,
                    color = StitchMeshYarnGold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = formulaText,
                    onValueChange = { formulaText = it },
                    label = { Text("Fórmula de crochet (ej: 6 pb, 1 aum, 2 pb)") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = CrochetTypography.formulaInput,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = StitchMeshSurfaceHigh,
                        unfocusedContainerColor = StitchMeshSurfaceContainer,
                        focusedBorderColor = StitchMeshTerracotta,
                        unfocusedBorderColor = StitchMeshSurfaceBorder,
                        focusedTextColor = StitchMeshTextPrimary,
                        unfocusedTextColor = StitchMeshTextPrimary,
                        focusedLabelColor = StitchMeshTerracotta,
                        unfocusedLabelColor = StitchMeshTextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Evaluación en vivo
                val tokens = formulaText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                var totalC = 0
                var totalP = 0
                for (token in tokens) {
                    val parts = token.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        val count = parts[0].toIntOrNull() ?: 1
                        val symbol = parts.drop(1).joinToString(" ")
                        val stitch = StitchRegistry.resolve(symbol)
                        if (stitch != null) {
                            totalC += stitch.consumedStitches * count
                            totalP += stitch.producedStitches * count
                        }
                    } else if (parts.size == 1) {
                        val stitch = StitchRegistry.resolve(parts[0])
                        if (stitch != null) {
                            totalC += stitch.consumedStitches
                            totalP += stitch.producedStitches
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = StitchMeshSurfaceHigh)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Base consumida: $totalC pts | Producidos: $totalP pts",
                                style = CrochetTypography.matrixValue,
                                color = StitchMeshTextPrimary
                            )
                            Text(
                                text = "Variación perimetral: ${if (totalP - totalC >= 0) "+${totalP - totalC}" else "${totalP - totalC}"} pts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StitchMeshTextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (totalP > 0) StitchMeshSageGreen else StitchMeshCoralRed)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (totalP > 0) "VÁLIDO" else "PENDIENTE",
                                style = CrochetTypography.tokenBadge,
                                color = StitchMeshOnAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParameterBadge(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(StitchMeshSurfaceHigh)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = StitchMeshTextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = CrochetTypography.matrixValue,
            color = StitchMeshTextPrimary
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "StitchMesh 3D",
        style = MaterialTheme.typography.headlineMedium
    )
}