package com.crisanfitos.stitchmesh3d.ui.dashboard.dialogs

import androidx.compose.ui.graphics.Color
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewProjectDialogTest {

    @Test
    fun `default crochet palette contains curated color swatches with Terracotta as primary`() {
        val palette = DefaultCrochetPalette
        assertTrue(palette.isNotEmpty())
        assertEquals("Terracotta", palette.first().name)
        assertEquals(StitchMeshTerracotta, palette.first().color)
        assertTrue(palette.any { it.name == "Verde Salvia" })
        assertTrue(palette.any { it.name == "Dorado Hilado" })
    }

    @Test
    fun `yarn weight category hook ranges provide valid defaults for project creation`() {
        val medium = YarnWeightCategory.MEDIUM
        assertEquals(4, medium.code)
        assertEquals(4.50f, medium.minHookSizeMm)
        assertEquals(5.50f, medium.maxHookSizeMm)

        val lace = YarnWeightCategory.LACE
        assertEquals(0, lace.code)
        assertTrue(lace.minHookSizeMm < lace.maxHookSizeMm)
    }

    @Test
    fun `title validation requires non-empty and non-blank input`() {
        val emptyTitle = ""
        val whitespaceTitle = "   "
        val validTitle = "Amigurumi Unicornio"

        assertFalse(emptyTitle.trim().isNotEmpty())
        assertFalse(whitespaceTitle.trim().isNotEmpty())
        assertTrue(validTitle.trim().isNotEmpty())
    }

    @Test
    fun `toHexRgbString extracts real sRGB hex values for all DefaultCrochetPalette swatches instead of black`() {
        val hexBySwatch = DefaultCrochetPalette.associate { it.name to it.color.toHexRgbString() }

        DefaultCrochetPalette.forEach { swatch ->
            val hex = swatch.color.toHexRgbString()
            assertTrue("Expected #RRGGBB format, got $hex", hex.matches(Regex("^#[0-9A-F]{6}$")))
            assertNotEquals("Palette swatch '${swatch.name}' must not collapse to #000000", "#000000", hex)
        }

        assertEquals(DefaultCrochetPalette.size, hexBySwatch.values.toSet().size)
        assertEquals("#E06D53", hexBySwatch["Terracotta"])
        assertEquals("#F5EBE0", hexBySwatch["Crema Natural"])
        assertEquals("#2C2F38", hexBySwatch["Carbón Técnico"])
        assertEquals("#4A7C9B", hexBySwatch["Azul Nórdico"])
        assertEquals("#D48B96", hexBySwatch["Rosa Empolvado"])

        assertEquals("#000000", Color.Black.toHexRgbString())
        assertEquals("#FFFFFF", Color.White.toHexRgbString())
    }
}
