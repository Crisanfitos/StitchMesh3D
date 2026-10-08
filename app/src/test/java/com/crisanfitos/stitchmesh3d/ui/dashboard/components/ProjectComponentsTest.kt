package com.crisanfitos.stitchmesh3d.ui.dashboard.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectComponentsTest {

    @Test
    fun `ProjectCardUiModel correctly initializes attributes and defaults`() {
        val model = ProjectCardUiModel(
            id = "proj-123",
            title = "Amigurumi Pulpo",
            lastModified = "Hace 10 min",
            yarnWeightLabel = "#4 Worsted",
            hookSizeMm = 3.5f,
            roundCount = 12,
            stitchCount = 72,
            isLinterValid = true
        )

        assertEquals("proj-123", model.id)
        assertEquals("Amigurumi Pulpo", model.title)
        assertEquals(3.5f, model.hookSizeMm)
        assertEquals(12, model.roundCount)
        assertEquals(72, model.stitchCount)
        assertTrue(model.isLinterValid)
        assertEquals(0, model.errorCount)
    }

    @Test
    fun `ProjectCardUiModel with errors captures errorCount and invalid state`() {
        val model = ProjectCardUiModel(
            id = "proj-456",
            title = "Esfera con fallo",
            lastModified = "Hace 1 hora",
            yarnWeightLabel = "#3 DK",
            hookSizeMm = 3.0f,
            roundCount = 6,
            stitchCount = 30,
            isLinterValid = false,
            errorCount = 2
        )

        assertFalse(model.isLinterValid)
        assertEquals(2, model.errorCount)
    }

    @Test
    fun `LinterValidationStatus enum contains expected validation states`() {
        val states = LinterValidationStatus.values()
        assertTrue(states.contains(LinterValidationStatus.VALID))
        assertTrue(states.contains(LinterValidationStatus.HAS_ERRORS))
        assertTrue(states.contains(LinterValidationStatus.WARNING))
    }
}
