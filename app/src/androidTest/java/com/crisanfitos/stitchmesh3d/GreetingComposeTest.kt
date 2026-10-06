package com.crisanfitos.stitchmesh3d

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMesh3DTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test de componente Compose aislado para Greeting y temas de StitchMesh 3D.
 */
@RunWith(AndroidJUnit4::class)
class GreetingComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun greeting_displaysHeadlineText() {
        composeTestRule.setContent {
            StitchMesh3DTheme {
                Greeting(name = "Test")
            }
        }

        composeTestRule.onNodeWithText("StitchMesh 3D").assertIsDisplayed()
    }
}
