package com.crisanfitos.stitchmesh3d.ui.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveWorkspaceTest {

    @Test
    fun `ProjectPartUiModel creates part with expected defaults`() {
        val part = ProjectPartUiModel(
            id = "part-cabeza",
            name = "Cabeza",
            roundCount = 14,
            colorHex = "#E06D53",
            isValid = true
        )

        assertEquals("part-cabeza", part.id)
        assertEquals("Cabeza", part.name)
        assertEquals(14, part.roundCount)
        assertEquals("#E06D53", part.colorHex)
        assertTrue(part.isValid)
    }

    @Test
    fun `WorkspaceMobileTab enum values include EDITOR and VIEWPORT_3D`() {
        val tabs = WorkspaceMobileTab.values()
        assertEquals(2, tabs.size)
        assertTrue(tabs.contains(WorkspaceMobileTab.EDITOR))
        assertTrue(tabs.contains(WorkspaceMobileTab.VIEWPORT_3D))
    }

    @Test
    fun `threshold logic separates tablet expanded layout from mobile compact`() {
        fun isTablet(widthDp: Int): Boolean = widthDp >= 840

        // Teléfono estándar (360dp - 412dp)
        assertFalse(isTablet(390))
        assertFalse(isTablet(412))

        // Tablet Portrait / Plegables medios (600dp - 839dp)
        assertFalse(isTablet(600))
        assertFalse(isTablet(800))

        // Tablet Landscape (>= 840dp, ej Pixel Tablet 1280dp)
        assertTrue(isTablet(840))
        assertTrue(isTablet(1024))
        assertTrue(isTablet(1280))
    }
}
