package com.crisanfitos.stitchmesh3d

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test instrumentado de la UI principal de StitchMesh 3D.
 * Verifica el inicio exitoso de MainActivity y la presencia del título de la aplicación.
 */
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivity_launchesAndDisplaysAppTitle() {
        // Verifica que la pantalla principal se renderiza y muestra el título de la aplicación
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("StitchMesh", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
