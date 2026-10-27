package com.crisanfitos.stitchmesh3d.ui.export

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportSuccessResultTest {

    @Test
    fun `GeneratedExportFileUiModel formats size in MB and detects 3D model extensions`() {
        val pdfFile = GeneratedExportFileUiModel(
            fileName = "ficha_tecnica.pdf",
            mimeType = "application/pdf",
            sizeBytes = 1_468_006L
        )
        val glbFile = GeneratedExportFileUiModel(
            fileName = "amigurumi.glb",
            mimeType = "model/gltf-binary",
            sizeBytes = 3_984_588L
        )

        assertEquals("1.4 MB", pdfFile.formattedSizeLabel)
        assertFalse(pdfFile.is3dModel)

        assertEquals("3.8 MB", glbFile.formattedSizeLabel)
        assertTrue(glbFile.is3dModel)
    }

    @Test
    fun `ExportSystemIntents builds expected SAF and Share action pairs`() {
        val pdfFile = GeneratedExportFileUiModel("patron.pdf", "application/pdf", 1024L * 512L)

        val saveAction = ExportSystemIntents.buildSaveDocumentAction(pdfFile)
        val shareAction = ExportSystemIntents.buildShareAction(listOf(pdfFile))

        assertEquals(Intent.ACTION_CREATE_DOCUMENT, saveAction.first)
        assertEquals("patron.pdf", saveAction.second)
        assertEquals(Intent.ACTION_SEND, shareAction.first)
        assertEquals("application/pdf", shareAction.second)
    }
}
