package com.crisanfitos.stitchmesh3d.ui.assembly.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas instrumentadas Compose UI para TransformInspectorPanel y NumericCoordinateRow (SM-054).
 */
@RunWith(AndroidJUnit4::class)
class TransformInspectorPanelComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testTransform = PartTransformState(
        partId = "part_ear_left",
        partName = "Oreja Izquierda",
        posX = 12.0f,
        posY = 18.5f,
        posZ = 0.0f,
        rotX = 0.0f,
        rotY = 15.0f,
        rotZ = -30.0f,
        isMirrored = false,
        isLocked = false
    )

    @Test
    fun transformInspectorPanel_displaysPartNameAndSections() {
        composeTestRule.setContent {
            StitchMesh3DTheme {
                TransformInspectorPanel(
                    transform = testTransform,
                    onPositionChange = { _, _, _ -> },
                    onRotationChange = { _, _, _ -> },
                    onMirrorToggled = {},
                    onResetOrigin = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Inspector CAD").assertIsDisplayed()
        composeTestRule.onNodeWithText("Oreja Izquierda").assertIsDisplayed()
        composeTestRule.onNodeWithText("Traslación Relativa (mm)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rotación Espacial (°)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Herramientas de Ensamblado").assertIsDisplayed()
    }

    @Test
    fun transformInspectorPanel_incrementXCallsPositionChange() {
        var updatedX = 0f

        composeTestRule.setContent {
            StitchMesh3DTheme {
                TransformInspectorPanel(
                    transform = testTransform,
                    onPositionChange = { x, _, _ -> updatedX = x },
                    onRotationChange = { _, _, _ -> },
                    onMirrorToggled = {},
                    onResetOrigin = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("increment_x").performClick()
        assertEquals(12.5f, updatedX, 0.001f)
    }

    @Test
    fun transformInspectorPanel_decrementPitchCallsRotationChange() {
        var updatedRotY = 0f

        composeTestRule.setContent {
            StitchMesh3DTheme {
                TransformInspectorPanel(
                    transform = testTransform,
                    onPositionChange = { _, _, _ -> },
                    onRotationChange = { _, y, _ -> updatedRotY = y },
                    onMirrorToggled = {},
                    onResetOrigin = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("decrement_pitch").performClick()
        assertEquals(10.0f, updatedRotY, 0.001f)
    }

    @Test
    fun transformInspectorPanel_mirrorAndResetButtonsTriggerCallbacks() {
        var mirrorClicked = false
        var resetClicked = false

        composeTestRule.setContent {
            StitchMesh3DTheme {
                TransformInspectorPanel(
                    transform = testTransform,
                    onPositionChange = { _, _, _ -> },
                    onRotationChange = { _, _, _ -> },
                    onMirrorToggled = { mirrorClicked = true },
                    onResetOrigin = { resetClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("mirror_toggle_button").performClick()
        assertTrue(mirrorClicked)

        composeTestRule.onNodeWithTag("reset_origin_button").performClick()
        assertTrue(resetClicked)
    }

    @Test
    fun transformInspectorPanel_showsEmptyStateWhenTransformIsNull() {
        composeTestRule.setContent {
            StitchMesh3DTheme {
                TransformInspectorPanel(
                    transform = null,
                    onPositionChange = { _, _, _ -> },
                    onRotationChange = { _, _, _ -> },
                    onMirrorToggled = {},
                    onResetOrigin = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Ninguna pieza seleccionada").assertIsDisplayed()
        composeTestRule.onNodeWithText("Selecciona una pieza en el árbol Scene Graph para inspeccionar y ajustar su posición.").assertIsDisplayed()
    }
}
