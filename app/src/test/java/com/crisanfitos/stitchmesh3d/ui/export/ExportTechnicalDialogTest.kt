package com.crisanfitos.stitchmesh3d.ui.export

import com.crisanfitos.stitchmesh3d.ui.export.components.ExportAssetFormat
import com.crisanfitos.stitchmesh3d.ui.export.components.ExportConfigurationState
import com.crisanfitos.stitchmesh3d.ui.export.components.MeshResolutionLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de pruebas unitarias sobre el estado, modelos de configuración y contratos
 * del diálogo de exportación técnica (SM-088, SM-093).
 */
class ExportTechnicalDialogTest {

    @Test
    fun `initial configuration state has valid defaults`() {
        val config = ExportConfigurationState()

        assertTrue(config.canStartExport)
        assertEquals(2, config.selectedFormats.size)
        assertTrue(config.selectedFormats.contains(ExportAssetFormat.PDF_SHEET))
        assertTrue(config.selectedFormats.contains(ExportAssetFormat.OBJ_MESH))
        assertFalse(config.selectedFormats.contains(ExportAssetFormat.GLTF_MESH))
        assertEquals(MeshResolutionLevel.MEDIUM, config.meshResolution)
    }

    @Test
    fun `toggleFormat correctly toggles formats and validity flag`() {
        var config = ExportConfigurationState()

        // Deseleccionar ambos formatos por defecto
        config = config.toggleFormat(ExportAssetFormat.PDF_SHEET)
        config = config.toggleFormat(ExportAssetFormat.OBJ_MESH)

        // Ningún formato seleccionado -> no se puede exportar
        assertFalse(config.canStartExport)
        assertEquals(0, config.selectedFormats.size)

        // Seleccionar GLTF
        config = config.toggleFormat(ExportAssetFormat.GLTF_MESH)
        assertTrue(config.canStartExport)
        assertEquals(1, config.selectedFormats.size)
        assertTrue(config.selectedFormats.contains(ExportAssetFormat.GLTF_MESH))
    }

    @Test
    fun `setting resolution updates subdivision multiplier appropriately`() {
        var config = ExportConfigurationState()

        config = config.copy(meshResolution = MeshResolutionLevel.LOW)
        assertEquals(1, config.meshResolution.subdivisionMultiplier)

        config = config.copy(meshResolution = MeshResolutionLevel.HIGH_CAD)
        assertEquals(4, config.meshResolution.subdivisionMultiplier)
    }

    @Test
    fun `asset formats provide appropriate human readable labels and extensions`() {
        assertEquals("Ficha Técnica PDF", ExportAssetFormat.PDF_SHEET.labelEs)
        assertEquals(".pdf", ExportAssetFormat.PDF_SHEET.extension)

        assertEquals("Malla Wavefront OBJ + MTL", ExportAssetFormat.OBJ_MESH.labelEs)
        assertEquals(".obj", ExportAssetFormat.OBJ_MESH.extension)

        assertEquals("Escena 3D glTF 2.0", ExportAssetFormat.GLTF_MESH.labelEs)
        assertEquals(".gltf", ExportAssetFormat.GLTF_MESH.extension)
    }
}
