package com.crisanfitos.stitchmesh3d.ui.export

import com.crisanfitos.stitchmesh3d.ui.export.components.ExportAssetFormat
import com.crisanfitos.stitchmesh3d.ui.export.components.ExportConfigurationState
import com.crisanfitos.stitchmesh3d.ui.export.components.MeshResolutionLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportFormatConfigTest {

    @Test
    fun `default ExportConfigurationState selects PDF and OBJ with MEDIUM resolution`() {
        val state = ExportConfigurationState()

        assertTrue(state.canStartExport)
        assertTrue(ExportAssetFormat.PDF_SHEET in state.selectedFormats)
        assertTrue(ExportAssetFormat.OBJ_MESH in state.selectedFormats)
        assertEquals(MeshResolutionLevel.MEDIUM, state.meshResolution)
    }

    @Test
    fun `toggleFormat adds and removes formats and updates canStartExport`() {
        var state = ExportConfigurationState(selectedFormats = setOf(ExportAssetFormat.PDF_SHEET))

        state = state.toggleFormat(ExportAssetFormat.GLTF_MESH)
        assertEquals(2, state.selectedFormats.size)

        state = state.toggleFormat(ExportAssetFormat.PDF_SHEET)
        state = state.toggleFormat(ExportAssetFormat.GLTF_MESH)
        assertFalse(state.canStartExport)
    }

    @Test
    fun `mesh resolution levels define ascending subdivision multipliers`() {
        assertTrue(
            MeshResolutionLevel.LOW.subdivisionMultiplier <
                MeshResolutionLevel.MEDIUM.subdivisionMultiplier
        )
        assertTrue(
            MeshResolutionLevel.MEDIUM.subdivisionMultiplier <
                MeshResolutionLevel.HIGH_CAD.subdivisionMultiplier
        )
    }
}
