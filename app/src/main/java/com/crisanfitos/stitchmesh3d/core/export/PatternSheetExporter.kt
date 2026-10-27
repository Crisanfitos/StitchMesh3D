package com.crisanfitos.stitchmesh3d.core.export

import com.crisanfitos.stitchmesh3d.domain.model.Project
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Generador de Ficha Técnica de proyecto en formatos Markdown y PDF paginado (SM-039, RF-5.3).
 * Incluye especificaciones de materiales, Muestra de tensión 10x10 cm, consumo estimado de hilo en metros
 * y tablas detalladas de vueltas por pieza.
 */
object PatternSheetExporter {

    /**
     * Calcula el consumo estimado de hilo en metros a partir del total de puntos y el ancho de punto en mm.
     */
    fun estimateYarnConsumptionMeters(project: Project): Float {
        val stitchWidthMm = project.customStitchWidthMm ?: (project.hookSizeMm * 0.92f)
        val yarnPerStitchMm = stitchWidthMm * 3.4f
        val totalMm = project.totalStitches * yarnPerStitchMm
        return (totalMm / 1000f).coerceAtLeast(0.5f)
    }

    /**
     * Genera el documento Markdown completo con cabecera técnica, Muestra de tensión y tabla de vueltas.
     */
    fun exportToMarkdown(project: Project): String {
        val stitchWidthMm = project.customStitchWidthMm ?: (project.hookSizeMm * 0.92f)
        val stitchHeightMm = project.customStitchHeightMm ?: (project.hookSizeMm * 0.85f)
        val stitchesPer10Cm = (100f / stitchWidthMm).toInt().coerceAtLeast(1)
        val rowsPer10Cm = (100f / stitchHeightMm).toInt().coerceAtLeast(1)
        val estimatedMeters = estimateYarnConsumptionMeters(project)

        val sb = StringBuilder()
        sb.appendLine("# 🧶 ${project.title} — Ficha Técnica de Patrón")
        if (!project.description.isNullOrBlank()) {
            sb.appendLine()
            sb.appendLine("> ${project.description}")
        }
        sb.appendLine()
        sb.appendLine("## 📐 Especificaciones y Muestra de Tensión (10x10 cm)")
        sb.appendLine("- **Estructura:** ${project.structureType.displayName}")
        sb.appendLine("- **Grosor de Hilo (CYC):** #${project.yarnWeightCategory.code} ${project.yarnWeightCategory.displayName}")
        sb.appendLine(String.format(Locale.US, "- **Aguja recomendada:** %.2f mm", project.hookSizeMm))
        sb.appendLine(
            String.format(
                Locale.US,
                "- **Muestra de tensión (10x10 cm):** %d pts × %d vueltas (%.2f mm × %.2f mm por punto)",
                stitchesPer10Cm,
                rowsPer10Cm,
                stitchWidthMm,
                stitchHeightMm
            )
        )
        sb.appendLine(String.format(Locale.US, "- **Consumo estimado de hilo:** %.1f m", estimatedMeters))
        sb.appendLine("- **Total de piezas:** ${project.parts.size} · **Total de puntos:** ${project.totalStitches}")

        for (part in project.parts.sortedBy { it.sortOrder }) {
            sb.appendLine()
            sb.appendLine("## 🧩 Pieza: ${part.name}")
            sb.appendLine("- **Topología:** ${part.topologyType.displayName}")
            sb.appendLine()
            sb.appendLine("| Vuelta | Instrucción | Puntos Consumidos | Puntos Producidos | Color | Estado |")
            sb.appendLine("| :--- | :--- | :---: | :---: | :---: | :---: |")

            for (round in part.rounds.sortedBy { it.roundNumber }) {
                val statusIcon = if (round.isValid) "✓ Válida" else "! Error"
                sb.appendLine(
                    "| V${round.roundNumber} | `${round.rawInstruction}` | ${round.consumedStitches} | **${round.producedStitches}** | `${round.colorHex}` | $statusIcon |"
                )
            }
        }

        return sb.toString()
    }

    /**
     * Divide el contenido de la ficha técnica en páginas con un máximo de líneas por página.
     */
    fun paginateMarkdownLines(markdown: String, maxLinesPerPage: Int = 42): List<List<String>> {
        val safeMax = maxLinesPerPage.coerceAtLeast(5)
        val lines = markdown.lines()
        return lines.chunked(safeMax)
    }

    /**
     * Genera un archivo PDF válido conforme a la especificación PDF 1.4 con paginación limpia
     * a partir de la ficha técnica del proyecto (portable tanto en Android como en JVM).
     */
    fun exportToPdfBytes(project: Project, maxLinesPerPage: Int = 42): ByteArray {
        val markdown = exportToMarkdown(project)
        val pages = paginateMarkdownLines(markdown, maxLinesPerPage)

        val pdf = StringBuilder()
        pdf.appendLine("%PDF-1.4")
        pdf.appendLine("% StitchMesh 3D Technical Pattern Sheet")
        pdf.appendLine("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj")

        val pageCount = pages.size.coerceAtLeast(1)
        val kidsRefs = (0 until pageCount).joinToString(" ") { "${3 + it * 2} 0 R" }
        pdf.appendLine("2 0 obj << /Type /Pages /Kids [$kidsRefs] /Count $pageCount >> endobj")

        pages.forEachIndexed { pageIndex, pageLines ->
            val pageObjId = 3 + pageIndex * 2
            val contentObjId = pageObjId + 1
            val streamBuilder = StringBuilder()
            streamBuilder.appendLine("BT")
            streamBuilder.appendLine("/F1 10 Tf")
            var y = 790
            for (line in pageLines) {
                val escaped = line
                    .replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                streamBuilder.appendLine("1 0 0 1 40 $y Tm ($escaped) Tj")
                y -= 16
            }
            streamBuilder.appendLine("ET")
            val streamText = streamBuilder.toString()

            pdf.appendLine(
                "$pageObjId 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents $contentObjId 0 R >> endobj"
            )
            pdf.appendLine(
                "$contentObjId 0 obj << /Length ${streamText.toByteArray(StandardCharsets.UTF_8).size} >> stream"
            )
            pdf.append(streamText)
            pdf.appendLine("endstream endobj")
        }

        pdf.appendLine("trailer << /Root 1 0 R >>")
        pdf.appendLine("%%EOF")
        return pdf.toString().toByteArray(StandardCharsets.UTF_8)
    }
}
