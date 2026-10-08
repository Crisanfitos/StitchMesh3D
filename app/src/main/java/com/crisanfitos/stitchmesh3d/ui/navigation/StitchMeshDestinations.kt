package com.crisanfitos.stitchmesh3d.ui.navigation

/**
 * Destinos y rutas del grafo de navegación de StitchMesh 3D.
 * Conforme a Documentación/app_flow_navigation.md (§1) y SM-025.
 */
sealed class StitchMeshDestination(val route: String) {

    /**
     * Pantalla de comprobación de sesión y Splash animado.
     */
    data object Splash : StitchMeshDestination("splash")

    /**
     * Biblioteca y explorador de proyectos.
     */
    data object Dashboard : StitchMeshDestination("dashboard")

    /**
     * Entorno CAD paramétrico y editor reactivo de vueltas.
     */
    data object Workspace : StitchMeshDestination("workspace/{projectId}") {
        const val ARG_PROJECT_ID = "projectId"

        fun createRoute(projectId: String): String = "workspace/$projectId"
    }

    /**
     * Calibrador dimensional de tensión y muestra 10x10 (CYC).
     */
    data object TensionCalibration : StitchMeshDestination("tension_calibration")
}
