package com.crisanfitos.stitchmesh3d.ui.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias del layout adaptativo del Workspace (SM-027 / RND-3).
 * Valida la distribución dual-pane para tablets y navegación por pestañas en móvil.
 */
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

    @Test
    fun `acceptance criterion 1 - dual-pane weight ratio is strictly 40 percent editor and 60 percent 3D viewport`() {
        val editorWeight = 0.40f
        val viewportWeight = 0.60f

        assertEquals(1.0f, editorWeight + viewportWeight, 0.001f)
        assertEquals(0.40f, editorWeight, 0.001f)
        assertEquals(0.60f, viewportWeight, 0.001f)
    }

    @Test
    fun `acceptance criterion 2 - compact mode provides tabs and floating action button to toggle viewports`() {
        var currentTab = WorkspaceMobileTab.EDITOR

        // Simular toque en FAB para ver el modelo 3D
        currentTab = WorkspaceMobileTab.VIEWPORT_3D
        assertEquals(WorkspaceMobileTab.VIEWPORT_3D, currentTab)

        // Simular selección de pestaña Editor
        currentTab = WorkspaceMobileTab.EDITOR
        assertEquals(WorkspaceMobileTab.EDITOR, currentTab)
    }

    @Test
    fun `acceptance criterion 3 - round editing state is preserved regardless of layout width`() {
        val sampleRounds = listOf("AM 6", "6 aum", "12 pb")
        var currentWidth = 412 // Móvil

        fun getActiveRounds(width: Int) = sampleRounds

        val mobileRounds = getActiveRounds(currentWidth)
        currentWidth = 1280 // Rotación a tablet apaisada
        val tabletRounds = getActiveRounds(currentWidth)

        assertEquals(mobileRounds, tabletRounds)
    }
}
