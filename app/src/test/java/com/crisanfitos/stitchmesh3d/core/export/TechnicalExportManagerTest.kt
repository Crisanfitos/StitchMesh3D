package com.crisanfitos.stitchmesh3d.core.export

import com.crisanfitos.stitchmesh3d.domain.model.PatternRound
import com.crisanfitos.stitchmesh3d.domain.model.Project
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import com.crisanfitos.stitchmesh3d.ui.export.components.ExportAssetFormat
import com.crisanfitos.stitchmesh3d.ui.export.components.ExportConfigurationState
import com.crisanfitos.stitchmesh3d.ui.export.components.ExportProgressState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TechnicalExportManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun sampleProject(): Project {
        return Project(
            id = "proj-bear",
            title = "Oso Amigurumi",
            hookSizeMm = 4.0f,
            isValid = true,
            parts = listOf(
                ProjectPart(
                    id = "part-1",
                    projectId = "proj-bear",
                    name = "Cabeza",
                    rounds = listOf(
                        PatternRound(
                            id = "r1",
                            partId = "part-1",
                            roundNumber = 1,
                            rawInstruction = "AM 6",
                            consumedStitches = 0,
                            producedStitches = 6,
                            isValid = true
                        )
                    )
                )
            )
        )
    }

    private fun sampleMeshParts(): List<ExportableMeshPart> {
        return listOf(
            ExportableMeshPart(
                partId = "part-1",
                name = "Cabeza",
                positions = floatArrayOf(0f, 0f, 0f, 10f, 0f, 0f, 0f, 10f, 0f),
                normals = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f),
                uvs = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f),
                indices = intArrayOf(0, 1, 2)
            )
        )
    }

    @Test
    fun `executeExportToDirectory generates all requested deliverables and reports progress`() = runBlocking {
        val project = sampleProject()
        val meshParts = sampleMeshParts()
        val outDir = tempFolder.newFolder("exports")

        val config = ExportConfigurationState(
            selectedFormats = setOf(
                ExportAssetFormat.PDF_SHEET,
                ExportAssetFormat.MARKDOWN_SHEET,
                ExportAssetFormat.OBJ_MESH,
                ExportAssetFormat.GLTF_MESH
            )
        )

        val progressReports = mutableListOf<ExportProgressState>()

        val results = TechnicalExportManager.executeExportToDirectory(
            project = project,
            meshParts = meshParts,
            config = config,
            outputDirectory = outDir,
            onProgress = { progressReports.add(it) }
        )

        // Verificamos que se hayan generado 6 ficheros: PDF, Markdown, OBJ, MTL, GLTF, GLB
        assertEquals(6, results.size)
        assertTrue(results.any { it.fileName.endsWith(".pdf") })
        assertTrue(results.any { it.fileName.endsWith(".md") })
        assertTrue(results.any { it.fileName.endsWith(".obj") })
        assertTrue(results.any { it.fileName.endsWith(".mtl") })
        assertTrue(results.any { it.fileName.endsWith(".gltf") })
        assertTrue(results.any { it.fileName.endsWith(".glb") })

        // Verificamos que los ficheros realmente existen físicamente en disco y son no vacíos
        for (f in results) {
            val diskFile = File(f.localPathOrUri)
            assertTrue("Fichero ${f.fileName} debe existir", diskFile.exists())
            assertTrue("Fichero ${f.fileName} no debe estar vacío", diskFile.length() > 0)
        }

        // Verificamos que el progreso comenzó en valor bajo y terminó al 100%
        assertTrue(progressReports.isNotEmpty())
        assertEquals(100, progressReports.last().progressPercent)
    }
}
