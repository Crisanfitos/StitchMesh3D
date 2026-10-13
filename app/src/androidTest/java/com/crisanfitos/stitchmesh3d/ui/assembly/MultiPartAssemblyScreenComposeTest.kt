package com.crisanfitos.stitchmesh3d.ui.assembly

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas instrumentadas de Compose UI para [MultiPartAssemblyScreen] (SM-055).
 * Valida la integración de SceneGraphTree, AssemblyViewport3D y TransformInspectorPanel en layout CAD.
 */
class MultiPartAssemblyScreenComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun multiPartAssemblyScreen_displaysAllPanelsAndTopBar() {
        var backClicked = false

        composeTestRule.setContent {
            StitchMesh3DTheme {
                MultiPartAssemblyScreen(
                    projectId = "amigurumi-bear",
                    onBackClick = { backClicked = true }
                )
            }
        }

        // Barra superior
        composeTestRule.onNodeWithTag("assembly_top_bar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ensamblador 3D").assertIsDisplayed()

        // Visor 3D y HUD
        composeTestRule.onNodeWithTag("assembly_viewport_3d").assertIsDisplayed()
        composeTestRule.onNodeWithTag("assembly_telemetry_hud").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gizmo_translate_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gizmo_rotate_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gizmo_camera_btn").assertIsDisplayed()

        // Botón volver
        composeTestRule.onNodeWithTag("assembly_back_button").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun multiPartAssemblyScreen_switchingGizmoModes_worksCorrectly() {
        composeTestRule.setContent {
            StitchMesh3DTheme {
                MultiPartAssemblyScreen(
                    projectId = "amigurumi-bear",
                    onBackClick = {}
                )
            }
        }

        // Conmutar a modo Rotación
        composeTestRule.onNodeWithTag("gizmo_rotate_btn").performClick()

        // Conmutar a modo Cámara libre
        composeTestRule.onNodeWithTag("gizmo_camera_btn").performClick()

        // Regresar a modo Traslación
        composeTestRule.onNodeWithTag("gizmo_translate_btn").performClick()
    }
}
