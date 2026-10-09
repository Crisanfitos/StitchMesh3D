package com.crisanfitos.stitchmesh3d.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.crisanfitos.stitchmesh3d.ui.dashboard.components.ProjectCard
import com.crisanfitos.stitchmesh3d.ui.dashboard.components.ProjectCardUiModel
import com.crisanfitos.stitchmesh3d.ui.dashboard.components.SearchAndFilterBar
import com.crisanfitos.stitchmesh3d.ui.dashboard.dialogs.NewProjectModalSheet
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshNeutralDark
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextDisabled
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import kotlinx.coroutines.flow.collectLatest

/**
 * Pantalla principal de la Biblioteca de Proyectos (Dashboard) conectada a ViewModel con UDF.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToWorkspace: (projectId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is DashboardEffect.NavigateToWorkspace -> {
                    onNavigateToWorkspace(effect.projectId)
                }
                is DashboardEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    DashboardContent(
        state = state,
        onIntent = viewModel::processIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    state: DashboardState,
    onIntent: (DashboardIntent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StitchMeshNeutralDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DashboardTopBar()
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onIntent(DashboardIntent.OpenCreateProjectModal) },
                containerColor = StitchMeshTerracotta,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nuevo Proyecto de Crochet",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Barra de búsqueda y chips de filtrado
            SearchAndFilterBar(
                query = state.searchQuery,
                onQueryChange = { onIntent(DashboardIntent.SearchQueryChanged(it)) },
                selectedFilter = state.selectedFilter,
                filterOptions = state.filterOptions,
                onFilterSelected = { onIntent(DashboardIntent.FilterSelected(it)) }
            )

            // Contenido condicional: cargando, vacío o rejilla de proyectos
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = StitchMeshTerracotta)
                    }
                }

                state.filteredProjects.isEmpty() -> {
                    DashboardEmptyState(
                        searchQuery = state.searchQuery,
                        onCreateProjectClick = { onIntent(DashboardIntent.OpenCreateProjectModal) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 320.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = state.filteredProjects,
                            key = { it.id }
                        ) { project ->
                            var isMenuExpanded by remember { mutableStateOf(false) }

                            Box {
                                ProjectCard(
                                    project = project,
                                    onClick = { onIntent(DashboardIntent.ProjectClicked(project.id)) },
                                    onMoreOptionsClick = { isMenuExpanded = true }
                                )

                                DropdownMenu(
                                    expanded = isMenuExpanded,
                                    onDismissRequest = { isMenuExpanded = false },
                                    modifier = Modifier.background(StitchMeshSurfaceHigh)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Eliminar", color = StitchMeshCoralRed) },
                                        onClick = {
                                            isMenuExpanded = false
                                            onIntent(DashboardIntent.DeleteProject(project.id))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modal BottomSheet de creación de proyecto paramétrico
        if (state.isCreateProjectModalVisible) {
            NewProjectModalSheet(
                onDismiss = { onIntent(DashboardIntent.DismissCreateProjectModal) },
                onCreateProject = { title, structureType, yarnWeight, hookSizeMm, primaryColor ->
                    val colorHex = String.format("#%06X", 0xFFFFFF and primaryColor.toArgb())
                    onIntent(
                        DashboardIntent.CreateProject(
                            title = title,
                            structureType = structureType,
                            yarnWeight = yarnWeight,
                            hookSizeMm = hookSizeMm,
                            primaryColorHex = colorHex
                        )
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar() {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchMeshTerracotta),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "StitchMesh Logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "StitchMesh 3D",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StitchMeshTextPrimary
                    )
                    Text(
                        text = "Biblioteca de Patrones y Verificación CAD",
                        style = CrochetTypography.tokenBadge,
                        color = StitchMeshTextSecondary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = StitchMeshNeutralDark
        ),
        actions = {
            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StitchMeshSurfaceHigh)
                    .border(width = 1.dp, color = StitchMeshSurfaceBorder, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "CAD v1.0",
                    style = CrochetTypography.tokenBadge,
                    color = StitchMeshTerracotta
                )
            }
        }
    )
}

@Composable
private fun DashboardEmptyState(
    searchQuery: String,
    onCreateProjectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(StitchMeshSurfaceContainer)
                .border(width = 1.dp, color = StitchMeshSurfaceBorder, shape = RoundedCornerShape(20.dp))
                .padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(StitchMeshSurfaceHigh)
                    .border(width = 1.dp, color = StitchMeshSurfaceBorder, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = StitchMeshTerracotta,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = if (searchQuery.isNotEmpty()) "Sin resultados para \"$searchQuery\"" else "No hay proyectos guardados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = StitchMeshTextPrimary
            )

            Text(
                text = if (searchQuery.isNotEmpty())
                    "Prueba a buscar con otro término o limpia los filtros."
                else
                    "Comienza a tejer creando tu primer amigurumi con verificación paramétrica formal.",
                style = MaterialTheme.typography.bodyMedium,
                color = StitchMeshTextSecondary
            )

            if (searchQuery.isEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(StitchMeshTerracotta)
                        .clickable(onClick = onCreateProjectClick)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Crear Primer Proyecto",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Preview(name = "DashboardScreen - Dark", showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    StitchMesh3DTheme {
        DashboardContent(
            state = DashboardState(
                allProjects = listOf(
                    ProjectCardUiModel(
                        id = "p1",
                        title = "Oso Amigurumi - Cabeza",
                        lastModified = "Hace 15m",
                        yarnWeightLabel = "#4 Worsted",
                        hookSizeMm = 3.5f,
                        roundCount = 18,
                        stitchCount = 108,
                        isLinterValid = true
                    ),
                    ProjectCardUiModel(
                        id = "p2",
                        title = "Conejo Orejas Largas",
                        lastModified = "Ayer",
                        yarnWeightLabel = "#3 DK",
                        hookSizeMm = 3.0f,
                        roundCount = 24,
                        stitchCount = 144,
                        isLinterValid = false,
                        errorCount = 1
                    )
                )
            ),
            onIntent = {}
        )
    }
}
