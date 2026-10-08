package com.crisanfitos.stitchmesh3d.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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

        // 2. Si hay proyectos en la lista, pulsar el primero; si no, crearlo mediante el diálogo modal
        val projectCards = composeTestRule.onAllNodes(hasText("Amigurumi", substring = true))
        if (projectCards.fetchSemanticsNodes().isNotEmpty()) {
            projectCards[0].performClick()
        } else {
            composeTestRule.onNodeWithText("Crear Primer Proyecto").performClick()
            composeTestRule.onNode(hasText("Ej. Oso Amigurumi, Gorro Acanalado...")).performTextInput("Zorro Test")
            composeTestRule.onNodeWithText("Crear Proyecto").performClick()
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
