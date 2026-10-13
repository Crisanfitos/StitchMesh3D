package com.crisanfitos.stitchmesh3d.ui.assembly.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas instrumentadas Compose UI para SceneGraphTree y PartTreeItemRow (SM-053).
 */
@RunWith(AndroidJUnit4::class)
class SceneGraphTreeComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testItems = listOf(
        SceneGraphItem(
            id = "part_head",
            name = "Cabeza Principal",
            roundCount = 18,
            stitchCount = 108,
            colorHex = "#E06D53",
            topologyType = PartTopologyType.CLOSED_FILLED,
            hasChildren = true,
            isExpanded = true
        ),
        SceneGraphItem(
            id = "part_ear_left",
            name = "Oreja Izquierda",
            parentId = "part_head",
            depth = 1,
            roundCount = 6,
            stitchCount = 24,
            colorHex = "#F2C94C",
            topologyType = PartTopologyType.FLAT_PANEL
        ),
        SceneGraphItem(
            id = "part_body",
            name = "Cuerpo Base",
            roundCount = 24,
            stitchCount = 144,
            colorHex = "#52A474",
            topologyType = PartTopologyType.CLOSED_FILLED
        )
    )

    @Test
    fun sceneGraphTree_displaysAllPartsAndMetrics() {
        composeTestRule.setContent {
            StitchMesh3DTheme {
                SceneGraphTree(
                    items = testItems,
                    selectedPartId = "part_head",
                    onPartSelected = {},
                    onToggleVisibility = {},
                    onToggleLock = {},
                    onColorChanged = { _, _ -> },
                    onAddPart = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Árbol de Escena").assertIsDisplayed()
        composeTestRule.onNodeWithText("3/3 visibles").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cabeza Principal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Oreja Izquierda").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cuerpo Base").assertIsDisplayed()
    }

    @Test
    fun sceneGraphTree_selectPartTriggersCallback() {
        var selectedId = ""

        composeTestRule.setContent {
            StitchMesh3DTheme {
                SceneGraphTree(
                    items = testItems,
                    selectedPartId = "part_head",
                    onPartSelected = { selectedId = it },
                    onToggleVisibility = {},
                    onToggleLock = {},
                    onColorChanged = { _, _ -> },
                    onAddPart = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Cuerpo Base").performClick()
        assertEquals("part_body", selectedId)
    }

    @Test
    fun sceneGraphTree_toggleVisibilityAndLockTriggerCallbacks() {
        var visibilityToggledId = ""
        var lockToggledId = ""

        composeTestRule.setContent {
            StitchMesh3DTheme {
                SceneGraphTree(
                    items = testItems,
                    selectedPartId = "part_head",
                    onPartSelected = {},
                    onToggleVisibility = { visibilityToggledId = it },
                    onToggleLock = { lockToggledId = it },
                    onColorChanged = { _, _ -> },
                    onAddPart = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("visibility_btn_part_head").performClick()
        assertEquals("part_head", visibilityToggledId)

        composeTestRule.onNodeWithTag("lock_btn_part_ear_left").performClick()
        assertEquals("part_ear_left", lockToggledId)
    }

    @Test
    fun sceneGraphTree_addPartButtonTriggersCallback() {
        var addPartClicked = false

        composeTestRule.setContent {
            StitchMesh3DTheme {
                SceneGraphTree(
                    items = testItems,
                    selectedPartId = "part_head",
                    onPartSelected = {},
                    onToggleVisibility = {},
                    onToggleLock = {},
                    onColorChanged = { _, _ -> },
                    onAddPart = { addPartClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("add_part_button").performClick()
        assertTrue(addPartClicked)
    }

    @Test
    fun sceneGraphTree_colorSwatchClickOpensDialogAndAppliesColor() {
        var recoloredPartId = ""
        var recoloredHex = ""

        composeTestRule.setContent {
            StitchMesh3DTheme {
                SceneGraphTree(
                    items = testItems,
                    selectedPartId = "part_head",
                    onPartSelected = {},
                    onToggleVisibility = {},
                    onToggleLock = {},
                    onColorChanged = { partId, newHex ->
                        recoloredPartId = partId
                        recoloredHex = newHex
                    },
                    onAddPart = {}
                )
            }
        }

        // Abrir diálogo tocando el swatch
        composeTestRule.onNodeWithTag("color_swatch_part_head").performClick()
        composeTestRule.onNodeWithText("Color de Hilado: Cabeza Principal").assertIsDisplayed()

        // Seleccionar color y aplicar
        composeTestRule.onNodeWithText("Aplicar Color").performClick()
        assertEquals("part_head", recoloredPartId)
        assertTrue(recoloredHex.isNotEmpty())
    }
}
