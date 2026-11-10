package com.crisanfitos.stitchmesh3d.core.export

import android.content.Context
import com.crisanfitos.stitchmesh3d.domain.model.Project
import com.crisanfitos.stitchmesh3d.ui.export.GeneratedExportFileUiModel
import com.crisanfitos.stitchmesh3d.ui.export.components.ExportAssetFormat
import com.crisanfitos.stitchmesh3d.ui.export.components.ExportConfigurationState
import com.crisanfitos.stitchmesh3d.ui.export.components.ExportProgressState
import com.crisanfitos.stitchmesh3d.ui.export.components.TelemetryLogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Gestor de orquestación técnica de compilación de entregables (SM-057, SM-058, SM-088, RF-5.3).
 * Ejecuta la compilación de Fichas Técnicas PDF, especificaciones Markdown, mallas OBJ y glTF 2.0
 * de forma asíncrona en [Dispatchers.IO] emitiendo telemetría porcentual en tiempo real.
 */
object TechnicalExportManager {

    suspend fun executeExport(
        context: Context,
        project: Project,
        meshParts: List<ExportableMeshPart>,
        config: ExportConfigurationState,
        onProgress: (ExportProgressState) -> Unit
    ): List<GeneratedExportFileUiModel> {
        return executeExportToDirectory(
            project = project,
            meshParts = meshParts,
            config = config,
            outputDirectory = File(context.cacheDir, "exports"),
            onProgress = onProgress
        )
    }

    suspend fun executeExportToDirectory(
        project: Project,
        meshParts: List<ExportableMeshPart>,
        config: ExportConfigurationState,
        outputDirectory: File,
        onProgress: (ExportProgressState) -> Unit
    ): List<GeneratedExportFileUiModel> = withContext(Dispatchers.IO) {
        var progressState = ExportProgressState(
            progressFraction = 0.05f,
            currentStageTitle = "Iniciando compilador técnico..."
        ).appendLog("Inicializando compilador técnico StitchMesh 3D", TelemetryLogLevel.INFO, 0.05f)
        onProgress(progressState)

        val exportDir = outputDirectory.apply { mkdirs() }
        val safeBaseName = project.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "StitchMesh_Project" }
        val generatedFiles = mutableListOf<GeneratedExportFileUiModel>()

        // 1. PDF Ficha Técnica (Pantalla 13 -> 14)
        if (ExportAssetFormat.PDF_SHEET in config.selectedFormats) {
            progressState = progressState.appendLog(
                "Compilando Ficha Técnica PDF con Muestra de Tensión 10x10 cm...",
                TelemetryLogLevel.ACTIVE,
                0.25f
            )
            onProgress(progressState)

            val pdfBytes = PatternSheetExporter.exportToPdfBytes(project)
            val pdfFile = File(exportDir, "${safeBaseName}_Ficha_Tecnica.pdf")
            pdfFile.writeBytes(pdfBytes)

            generatedFiles.add(
                GeneratedExportFileUiModel(
                    fileName = pdfFile.name,
                    mimeType = "application/pdf",
                    sizeBytes = pdfFile.length(),
                    localPathOrUri = pdfFile.absolutePath
                )
            )
            progressState = progressState.appendLog(
                "PDF generado con éxito (${(pdfFile.length() / 1024L).coerceAtLeast(1L)} KB)",
                TelemetryLogLevel.SUCCESS,
                0.45f
            )
            onProgress(progressState)
        }

        // 2. Patrón Markdown
        if (ExportAssetFormat.MARKDOWN_SHEET in config.selectedFormats) {
            progressState = progressState.appendLog(
                "Exportando tabla de instrucciones y especificaciones a Markdown...",
                TelemetryLogLevel.ACTIVE,
                0.55f
            )
            onProgress(progressState)

            val mdText = PatternSheetExporter.exportToMarkdown(project)
            val mdFile = File(exportDir, "${safeBaseName}_Patron.md")
            mdFile.writeText(mdText)

            generatedFiles.add(
                GeneratedExportFileUiModel(
                    fileName = mdFile.name,
                    mimeType = "text/markdown",
                    sizeBytes = mdFile.length(),
                    localPathOrUri = mdFile.absolutePath
                )
            )
            progressState = progressState.appendLog(
                "Fichero Markdown generado con éxito",
                TelemetryLogLevel.SUCCESS,
                0.65f
            )
            onProgress(progressState)
        }

        // 3. Malla Wavefront OBJ + MTL
        if (ExportAssetFormat.OBJ_MESH in config.selectedFormats) {
            progressState = progressState.appendLog(
                "Calculando geometría Wavefront OBJ y materiales MTL...",
                TelemetryLogLevel.ACTIVE,
                0.72f
            )
            onProgress(progressState)

            val effectiveParts = if (meshParts.isNotEmpty()) meshParts else listOf(
                ExportableMeshPart(
                    partId = "p-fallback",
                    name = project.parts.firstOrNull()?.name ?: "Pieza",
                    positions = floatArrayOf(0f, 0f, 0f, 10f, 0f, 0f, 0f, 10f, 0f),
                    normals = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f),
                    indices = intArrayOf(0, 1, 2)
                )
            )

            val mtlFileName = "${safeBaseName}_materials.mtl"
            val bundle = ModelExporter.exportToWavefrontObj(project.title, effectiveParts, mtlFileName)
            val objFile = File(exportDir, "${safeBaseName}.obj")
            objFile.writeText(bundle.objContent)
            val mtlFile = File(exportDir, mtlFileName)
            mtlFile.writeText(bundle.mtlContent)

            generatedFiles.add(
                GeneratedExportFileUiModel(
                    fileName = objFile.name,
                    mimeType = "model/obj",
                    sizeBytes = objFile.length(),
                    localPathOrUri = objFile.absolutePath
                )
            )
            generatedFiles.add(
                GeneratedExportFileUiModel(
                    fileName = mtlFile.name,
                    mimeType = "text/plain",
                    sizeBytes = mtlFile.length(),
                    localPathOrUri = mtlFile.absolutePath
                )
            )
            progressState = progressState.appendLog(
                "Malla OBJ exportada: ${bundle.totalVertices} vértices, ${bundle.totalFaces} caras",
                TelemetryLogLevel.SUCCESS,
                0.82f
            )
            onProgress(progressState)
        }

        // 4. Escena 3D glTF 2.0 / GLB
        if (ExportAssetFormat.GLTF_MESH in config.selectedFormats) {
            progressState = progressState.appendLog(
                "Serializando buffers binarios glTF 2.0 y GLB...",
                TelemetryLogLevel.ACTIVE,
                0.88f
            )
            onProgress(progressState)

            val effectiveParts = if (meshParts.isNotEmpty()) meshParts else listOf(
                ExportableMeshPart(
                    partId = "p-fallback",
                    name = project.parts.firstOrNull()?.name ?: "Pieza",
                    positions = floatArrayOf(0f, 0f, 0f, 10f, 0f, 0f, 0f, 10f, 0f),
                    normals = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f),
                    indices = intArrayOf(0, 1, 2)
                )
            )

            val gltfJson = ModelExporter.exportToGltfJson(project.title, effectiveParts)
            val gltfFile = File(exportDir, "${safeBaseName}.gltf")
            gltfFile.writeText(gltfJson)

            val glbBytes = ModelExporter.exportToGlb(project.title, effectiveParts)
            val glbFile = File(exportDir, "${safeBaseName}.glb")
            glbFile.writeBytes(glbBytes)

            generatedFiles.add(
                GeneratedExportFileUiModel(
                    fileName = gltfFile.name,
                    mimeType = "model/gltf+json",
                    sizeBytes = gltfFile.length(),
                    localPathOrUri = gltfFile.absolutePath
                )
            )
            generatedFiles.add(
                GeneratedExportFileUiModel(
                    fileName = glbFile.name,
                    mimeType = "model/gltf-binary",
                    sizeBytes = glbFile.length(),
                    localPathOrUri = glbFile.absolutePath
                )
            )
            progressState = progressState.appendLog(
                "glTF 2.0 y contenedor GLB binario completados",
                TelemetryLogLevel.SUCCESS,
                0.96f
            )
            onProgress(progressState)
        }

        progressState = progressState.appendLog(
            "Compilación técnica finalizada con ${generatedFiles.size} entregables",
            TelemetryLogLevel.SUCCESS,
            1.0f
        )
        onProgress(progressState)

        generatedFiles
    }
}
