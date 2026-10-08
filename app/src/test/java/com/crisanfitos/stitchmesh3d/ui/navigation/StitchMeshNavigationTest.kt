package com.crisanfitos.stitchmesh3d.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias del grafo y destinos tipados de navegación para StitchMesh 3D (SM-025).
 */
class StitchMeshNavigationTest {

    @Test
    fun `test destination routes are defined correctly`() {
        assertEquals("splash", StitchMeshDestination.Splash.route)
        assertEquals("dashboard", StitchMeshDestination.Dashboard.route)
        assertEquals("workspace/{projectId}", StitchMeshDestination.Workspace.route)
        assertEquals("tension_calibration", StitchMeshDestination.TensionCalibration.route)
    }

    @Test
    fun `test workspace createRoute generates correct parameterized route`() {
        val projectId = "project-uuid-456"
        val expected = "workspace/project-uuid-456"
        assertEquals(expected, StitchMeshDestination.Workspace.createRoute(projectId))
    }

    @Test
    fun `test destination argument key is projectId`() {
        assertEquals("projectId", StitchMeshDestination.Workspace.ARG_PROJECT_ID)
    }

    @Test
    fun `test all destination routes are distinct`() {
        val routes = listOf(
            StitchMeshDestination.Splash.route,
            StitchMeshDestination.Dashboard.route,
            StitchMeshDestination.Workspace.route,
            StitchMeshDestination.TensionCalibration.route
        )
        assertEquals(4, routes.toSet().size)
    }

    @Test
    fun `test workspace route contains parameter placeholder`() {
        assertTrue(
            StitchMeshDestination.Workspace.route.contains("{${StitchMeshDestination.Workspace.ARG_PROJECT_ID}}")
        )
    }
}
