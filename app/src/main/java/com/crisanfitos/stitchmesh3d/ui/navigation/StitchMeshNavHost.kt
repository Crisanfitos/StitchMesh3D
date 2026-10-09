package com.crisanfitos.stitchmesh3d.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.crisanfitos.stitchmesh3d.StitchMeshWorkbenchScreen
import com.crisanfitos.stitchmesh3d.ui.dashboard.DashboardScreen
import com.crisanfitos.stitchmesh3d.ui.dashboard.DashboardViewModel
import com.crisanfitos.stitchmesh3d.ui.splash.SplashScreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshNeutralDark
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshOnAccent
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary


/**
 * Grafo de navegación principal de StitchMesh 3D.
 * Conecta Splash -> Dashboard -> Workspace(projectId) -> Calibración de Tensión.
 *
 * Conforme a Documentación/app_flow_navigation.md (§1) y criterios de SM-025.
 */
@Composable
fun StitchMeshNavHost(
    dashboardViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = StitchMeshDestination.Splash.route,
    splashDurationMs: Long = 900L
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(280)) },
        exitTransition = { fadeOut(animationSpec = tween(280)) },
        popEnterTransition = { fadeIn(animationSpec = tween(280)) },
        popExitTransition = { fadeOut(animationSpec = tween(280)) }
    ) {
        // 1. Splash / Sesión inicial
        composable(route = StitchMeshDestination.Splash.route) {
            SplashScreen(
                splashDurationMs = splashDurationMs,
                onSplashCompleted = {
                    navController.navigate(StitchMeshDestination.Dashboard.route) {
                        popUpTo(StitchMeshDestination.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // 2. Dashboard de Proyectos (Biblioteca)
        composable(route = StitchMeshDestination.Dashboard.route) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToWorkspace = { projectId ->
                    navController.navigate(StitchMeshDestination.Workspace.createRoute(projectId))
                }
            )
        }

        // 3. Workspace CAD Adaptativo con ID de Proyecto
        composable(
            route = StitchMeshDestination.Workspace.route,
            arguments = listOf(
                navArgument(StitchMeshDestination.Workspace.ARG_PROJECT_ID) {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(StitchMeshDestination.Workspace.ARG_PROJECT_ID)
            com.crisanfitos.stitchmesh3d.ui.workspace.WorkspaceScreen(
                projectId = projectId,
                onBackToDashboard = {
                    navController.popBackStack()
                },
                onCalibrateTension = {
                    navController.navigate(StitchMeshDestination.TensionCalibration.route)
                }
            )
        }

        // 4. Calibración de Tensión (10x10 cm Swatch) & Comparativa Paramétrica
        composable(route = StitchMeshDestination.TensionCalibration.route) {
            com.crisanfitos.stitchmesh3d.ui.tension.TensionCalibratorScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
