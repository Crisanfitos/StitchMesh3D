package com.crisanfitos.stitchmesh3d.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crisanfitos.stitchmesh3d.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas instrumentadas de integración del grafo de navegación (SM-025).
 * Valida el ciclo: Splash -> Dashboard -> Workspace(projectId) -> Back -> Dashboard.
 */
@RunWith(AndroidJUnit4::class)
class AppNavigationInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testNavigation_SplashToDashboardToWorkspaceAndBack() {
        // 1. Esperar a que el Splash transicione al Dashboard
        composeTestRule.waitUntil(timeoutMillis = 7000) {
            composeTestRule.onAllNodes(hasText("Biblioteca de Patrones", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }

        // 2. Si hay botón de estado vacío, crear proyecto; si ya hay tarjetas, pulsar la primera
        val emptyStateBtn = composeTestRule.onAllNodes(hasText("Crear Primer Proyecto"))
        if (emptyStateBtn.fetchSemanticsNodes().isNotEmpty()) {
            emptyStateBtn[0].performClick()
            val titleFieldMatcher = hasSetTextAction() and hasText("Ej. Oso Amigurumi", substring = true)
            composeTestRule.waitUntil(timeoutMillis = 4000) {
                composeTestRule.onAllNodes(titleFieldMatcher).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNode(titleFieldMatcher).performTextInput("Zorro Test")
            Espresso.closeSoftKeyboard()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Crear Proyecto").performScrollTo().performClick()
        } else {
            val cardMatcher = hasClickAction() and (hasText("vtas", substring = true) or hasAnyDescendant(hasText("vtas", substring = true)))
            composeTestRule.onAllNodes(cardMatcher)[0].performClick()
        }

        // 3. Verificar que nos encontramos en el Workspace (aparece el badge CAD WORKBENCH)
        composeTestRule.waitUntil(timeoutMillis = 7000) {
            composeTestRule.onAllNodes(hasText("CAD WORKBENCH", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }

        // 4. Presionar el botón de volver al Dashboard
        val backButton = composeTestRule.onNodeWithContentDescription("Volver a la Biblioteca")
        backButton.assertIsDisplayed()
        backButton.performClick()

        // 5. Verificar que regresamos al Dashboard
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(hasText("Biblioteca de Patrones", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
