package com.crisanfitos.stitchmesh3d.ui.export

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import com.crisanfitos.stitchmesh3d.ui.workspace.dialogs.ExportTechnicalDialog
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas instrumentadas de Compose UI para el flujo completo de exportación técnica (Pantallas 13–15, SM-088).
 */
class TechnicalExportFlowComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun exportTechnicalDialog_displaysFormatSelectorsAndToggles() {
        var dismissClicked = false

        composeTestRule.setContent {
            StitchMesh3DTheme {
                ExportTechnicalDialog(
                    projectTitle = "Zorro Amigurumi",
                    onDismiss = { dismissClicked = true }
                )
            }
        }

        // Pantalla 13: Verificación de diálogo modal y opciones
        composeTestRule.onNodeWithTag("export_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("export_option_pdf").assertIsDisplayed()
        composeTestRule.onNodeWithTag("export_option_3d").assertIsDisplayed()
        composeTestRule.onNodeWithTag("export_confirm_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("export_cancel_btn").assertIsDisplayed()

        // Conmutar opción 3D
        composeTestRule.onNodeWithTag("export_option_3d").performClick()

        // Cancelar diálogo
        composeTestRule.onNodeWithTag("export_cancel_btn").performClick()
        assertTrue(dismissClicked)
    }

    @Test
    fun exportSuccessResultScreen_displaysAllDeliverablesAndActions() {
        var saved = false
        var shared = false
        var closed = false

        val testFiles = listOf(
            GeneratedExportFileUiModel(
                fileName = "Zorro_Ficha_Tecnica.pdf",
                mimeType = "application/pdf",
                sizeBytes = 245_000L
            ),
            GeneratedExportFileUiModel(
                fileName = "Zorro.obj",
                mimeType = "model/obj",
                sizeBytes = 1_850_000L
            )
        )

        composeTestRule.setContent {
            StitchMesh3DTheme {
                ExportSuccessResultScreen(
                    projectTitle = "Zorro Amigurumi",
                    generatedFiles = testFiles,
                    onSaveToDevice = { saved = true },
                    onShareFiles = { shared = true },
                    onClose = { closed = true }
                )
            }
        }

        // Pantalla 15: Comprobación de componentes de entrega técnica
        composeTestRule.onNodeWithTag("export_success_result_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("save_to_device_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("share_files_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("save_to_device_button").performClick()
        assertTrue(saved)

        composeTestRule.onNodeWithTag("share_files_button").performClick()
        assertTrue(shared)
    }
}
