package com.crisanfitos.stitchmesh3d.ui.dashboard.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextDisabled
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary

/**
 * Barra interactiva de búsqueda y filtrado horizontal para la biblioteca de proyectos.
 */
@Composable
fun SearchAndFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    filterOptions: List<String>,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Buscar proyectos de crochet..."
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Campo de búsqueda con esquinas redondeadas
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholderText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StitchMeshTextDisabled
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = StitchMeshTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpiar búsqueda",
                            tint = StitchMeshTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = StitchMeshSurfaceHigh,
                unfocusedContainerColor = StitchMeshSurfaceHigh,
                focusedBorderColor = StitchMeshTerracotta,
                unfocusedBorderColor = StitchMeshSurfaceBorder,
                focusedTextColor = StitchMeshTextPrimary,
                unfocusedTextColor = StitchMeshTextPrimary,
                cursorColor = StitchMeshTerracotta
            )
        )

        // Fila horizontal con scroll para chips de filtrado rápido
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterOptions.forEach { option ->
                val isSelected = option.equals(selectedFilter, ignoreCase = true)
                DashboardFilterChip(
                    label = option,
                    isSelected = isSelected,
                    onClick = { onFilterSelected(option) }
                )
            }
        }
    }
}

@Composable
private fun DashboardFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        StitchMeshTerracotta
    } else {
        StitchMeshSurfaceHigh
    }

    val contentColor = if (isSelected) {
        Color.White
    } else {
        StitchMeshTextSecondary
    }

    val borderColor = if (isSelected) {
        StitchMeshTerracotta
    } else {
        StitchMeshSurfaceBorder
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}

@Preview(name = "SearchAndFilterBar - Dark", showBackground = true)
@Composable
private fun SearchAndFilterBarPreview() {
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todos") }

    StitchMesh3DTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            SearchAndFilterBar(
                query = query,
                onQueryChange = { query = it },
                selectedFilter = selectedFilter,
                filterOptions = listOf("Todos", "Válidos", "Con errores", "Recientes"),
                onFilterSelected = { selectedFilter = it }
            )
        }
    }
}
