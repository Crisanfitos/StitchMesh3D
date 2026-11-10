package com.crisanfitos.stitchmesh3d.core.export

import com.crisanfitos.stitchmesh3d.domain.model.PatternRound
import com.crisanfitos.stitchmesh3d.domain.model.Project
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Generador de Ficha Técnica de proyecto en formatos Markdown y PDF paginado (SM-039, SM-088, RF-5.3).
 * Incluye especificaciones de materiales, Muestra de tensión 10x10 cm, consumo estimado de hilo en metros,
 * esquema visual de vueltas y tablas detalladas de vueltas por pieza.
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
     * Convierte texto con caracteres latinos/españoles a bytes WinAnsi compatibles con fuentes Type 1 PDF.
     */
    fun encodeWinAnsiString(text: String): ByteArray {
        val sanitized = text
            .replace("🧶", "[Stitch]")
            .replace("🧩", "[Part]")
            .replace("📐", "[CAD]")
            .replace("✓", "OK")
            .replace("—", "-")
            .replace("–", "-")
            .replace("«", "\"")
            .replace("»", "\"")

        val out = ByteArrayOutputStream()
        for (ch in sanitized) {
            when (ch) {
                '\\', '(', ')' -> {
                    out.write('\\'.code)
                    out.write(ch.code)
                }
                'á' -> out.write(0xE1)
                'é' -> out.write(0xE9)
                'í' -> out.write(0xED)
                'ó' -> out.write(0xF3)
                'ú' -> out.write(0xFA)
                'ñ' -> out.write(0xF1)
                'Á' -> out.write(0xC1)
                'É' -> out.write(0xC9)
                'Í' -> out.write(0xCD)
                'Ó' -> out.write(0xD3)
                'Ú' -> out.write(0xDA)
                'Ñ' -> out.write(0xD1)
                'ü' -> out.write(0xFC)
                'Ü' -> out.write(0xDC)
                '¿' -> out.write(0xBF)
                '¡' -> out.write(0xA1)
                '·' -> out.write(0xB7)
                '°' -> out.write(0xB0)
                in '\u0020'..'\u007E' -> out.write(ch.code)
                else -> {
                    // Caracteres fuera de BMP o desconocidos
                    if (ch.code in 32..126) {
                        out.write(ch.code)
                    } else {
                        out.write(' '.code)
                    }
                }
            }
        }
        return out.toByteArray()
    }

    /**
     * Genera un archivo PDF 1.4 totalmente válido y estándar, con fuentes Type 1, codificación WinAnsi,
     * tabla de referencias cruzadas (`xref`), ficha de Muestra de tensión (10x10 cm), esquema gráfico de vueltas
     * y tablas estructuradas de instrucciones por pieza (SM-039, SM-088, RF-5.3).
     */
    fun exportToPdfBytes(project: Project, maxLinesPerPage: Int = 42): ByteArray {
        val stitchWidthMm = project.customStitchWidthMm ?: (project.hookSizeMm * 0.92f)
        val stitchHeightMm = project.customStitchHeightMm ?: (project.hookSizeMm * 0.85f)
        val stitchesPer10Cm = (100f / stitchWidthMm).toInt().coerceAtLeast(1)
        val rowsPer10Cm = (100f / stitchHeightMm).toInt().coerceAtLeast(1)
        val estimatedMeters = estimateYarnConsumptionMeters(project)

        val allRounds: List<Triple<String, PatternRound, String>> = project.parts.flatMap { part ->
            part.rounds.map { round -> Triple(part.name, round, round.colorHex) }
        }

        // Paginación: Página 1 tiene carátula + Muestra de tensión + esquema radial de vueltas.
        val roundsPerPage1 = 12
        val roundsPerPageFollow = 26

        val roundChunks = mutableListOf<List<Triple<String, PatternRound, String>>>()
        if (allRounds.isEmpty()) {
            roundChunks.add(emptyList())
        } else {
            val firstChunk = allRounds.take(roundsPerPage1)
            roundChunks.add(firstChunk)
            var remaining = allRounds.drop(roundsPerPage1)
            while (remaining.isNotEmpty()) {
                val nextChunk = remaining.take(roundsPerPageFollow)
                roundChunks.add(nextChunk)
                remaining = remaining.drop(roundsPerPageFollow)
            }
        }

        val totalPages = roundChunks.size.coerceAtLeast(1)

        val out = ByteArrayOutputStream()
        val objectOffsets = mutableListOf<Long>()

        fun writeAscii(str: String) {
            out.write(str.toByteArray(StandardCharsets.US_ASCII))
        }

        fun startObject(objId: Int) {
            while (objectOffsets.size < objId) {
                objectOffsets.add(0L)
            }
            if (objectOffsets.size == objId) {
                objectOffsets.add(out.size().toLong())
            } else {
                objectOffsets[objId] = out.size().toLong()
            }
            writeAscii("$objId 0 obj\n")
        }

        fun endObject() {
            writeAscii("endobj\n")
        }

        // 1. PDF Header + binary comment
        writeAscii("%PDF-1.4\n")
        out.write(byteArrayOf('%'.code.toByte(), 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), '\n'.code.toByte()))

        // Objeto 1: Catalog
        startObject(1)
        writeAscii("<< /Type /Catalog /Pages 2 0 R >>\n")
        endObject()

        // Objeto 2: Pages
        val kids = (0 until totalPages).joinToString(" ") { "${5 + it * 2} 0 R" }
        startObject(2)
        writeAscii("<< /Type /Pages /Kids [$kids] /Count $totalPages >>\n")
        endObject()

        // Objeto 3: Font Regular
        startObject(3)
        writeAscii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\n")
        endObject()

        // Objeto 4: Font Bold
        startObject(4)
        writeAscii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\n")
        endObject()

        // Generar cada página y su stream de contenido
        for (pageIndex in 0 until totalPages) {
            val pageObjId = 5 + pageIndex * 2
            val contentObjId = pageObjId + 1

            val streamOut = ByteArrayOutputStream()

            fun streamAscii(s: String) {
                streamOut.write(s.toByteArray(StandardCharsets.US_ASCII))
            }

            fun streamText(text: String, fontRef: String, size: Float, x: Float, y: Float) {
                streamAscii("BT\n$fontRef $size Tf\n1 0 0 1 $x $y Tm\n(")
                streamOut.write(encodeWinAnsiString(text))
                streamAscii(") Tj\nET\n")
            }

            // Barra superior decorativa StitchMesh Terracotta
            streamAscii("q\n0.88 0.43 0.33 rg\n40 810 515 4 re f\nQ\n")

            // Encabezado de página
            streamText("${project.title} - Ficha Técnica", "/F2", 16f, 40f, 788f)
            streamText("StitchMesh 3D Parametric CAD & Formal Verification", "/F1", 9f, 40f, 774f)

            var cursorY: Float

            if (pageIndex == 0) {
                // Tarjeta 1: Muestra de tensión (10x10 cm) & Especificaciones
                streamAscii("q\n0.95 0.95 0.96 rg\n40 660 515 90 re f\n")
                streamAscii("0.85 0.85 0.87 RG\n1 w\n40 660 515 90 re S\nQ\n")

                streamText("MUESTRA DE TENSIÓN (10x10 cm) & CALIBRACIÓN DIMENSIONAL", "/F2", 10f, 52f, 734f)
                val line1 = String.format(
                    Locale.US,
                    "Estructura: %s   ·   Hilo CYC: #%s %s   ·   Aguja recomendada: %.2f mm",
                    project.structureType.displayName,
                    project.yarnWeightCategory.code,
                    project.yarnWeightCategory.displayName,
                    project.hookSizeMm
                )
                streamText(line1, "/F1", 9f, 52f, 718f)

                val line2 = String.format(
                    Locale.US,
                    "Muestra 10x10: %d pts x %d vtas (%.2f mm x %.2f mm / punto)   ·   Consumo est.: %.1f m",
                    stitchesPer10Cm,
                    rowsPer10Cm,
                    stitchWidthMm,
                    stitchHeightMm,
                    estimatedMeters
                )
                streamText(line2, "/F1", 9f, 52f, 702f)

                val line3 = "Total de piezas: ${project.parts.size}   ·   Total de puntos acumulados: ${project.totalStitches}"
                streamText(line3, "/F1", 9f, 52f, 686f)

                // Esquema Gráfico Visual de Vueltas (Crochet Radial Stitch Scheme)
                streamAscii("q\n")
                val cx = 110f
                val cy = 590f
                val maxRoundsToDraw = project.parts.firstOrNull()?.rounds?.size?.coerceIn(1, 6) ?: 3
                for (rIdx in 1..maxRoundsToDraw) {
                    val radius = rIdx * 8f
                    streamAscii("0.88 0.43 0.33 RG 0.8 w\n")
                    val k = radius * 0.55228475f
                    streamAscii("${cx + radius} $cy m\n")
                    streamAscii("${cx + radius} ${cy + k} ${cx + k} ${cy + radius} $cx ${cy + radius} c\n")
                    streamAscii("${cx - k} ${cy + radius} ${cx - radius} ${cy + k} ${cx - radius} $cy c\n")
                    streamAscii("${cx - radius} ${cy - k} ${cx - k} ${cy - radius} $cx ${cy - radius} c\n")
                    streamAscii("${cx + k} ${cy - radius} ${cx + radius} ${cy - k} ${cx + radius} $cy c S\n")
                }
                streamAscii("Q\n")

                streamText("Esquema Paramétrico Radial de Vueltas:", "/F2", 9f, 180f, 615f)
                streamText("Visualización de progresión anular de puntos (RF-5.3).", "/F1", 8.5f, 180f, 600f)
                streamText("Cotas milimétricas calibradas por la Muestra de tensión activa.", "/F1", 8.5f, 180f, 587f)

                cursorY = 520f
            } else {
                cursorY = 740f
            }

            // Tabla de Vueltas
            // Cabecera de la tabla
            streamAscii("q\n0.88 0.43 0.33 rg\n40 ${cursorY - 18f} 515 20 re f\nQ\n")
            streamText("Vta", "/F2", 9f, 48f, cursorY - 13f)
            streamText("Pieza", "/F2", 9f, 85f, cursorY - 13f)
            streamText("Instrucción del Patrón", "/F2", 9f, 175f, cursorY - 13f)
            streamText("Consumidos", "/F2", 9f, 340f, cursorY - 13f)
            streamText("Producidos", "/F2", 9f, 410f, cursorY - 13f)
            streamText("Color", "/F2", 9f, 475f, cursorY - 13f)
            streamText("Estado", "/F2", 9f, 518f, cursorY - 13f)

            cursorY -= 22f

            val pageRounds = roundChunks[pageIndex]
            for ((idx, item) in pageRounds.withIndex()) {
                val (partName, round, colorHex) = item
                val rowY = cursorY - 16f

                // Fila alterna
                if (idx % 2 == 1) {
                    streamAscii("q\n0.97 0.97 0.98 rg\n40 ${rowY - 4f} 515 16 re f\nQ\n")
                }

                streamText("V${round.roundNumber}", "/F2", 8.5f, 48f, rowY)
                streamText(partName.take(14), "/F1", 8.5f, 85f, rowY)
                streamText(round.rawInstruction.take(30), "/F1", 8.5f, 175f, rowY)
                streamText("${round.consumedStitches} pts", "/F1", 8.5f, 345f, rowY)
                streamText("${round.producedStitches} pts", "/F2", 8.5f, 415f, rowY)
                streamText(colorHex, "/F1", 8f, 475f, rowY)
                val statusText = if (round.isValid) "Válida" else "! Error"
                streamText(statusText, "/F2", 8.5f, 518f, rowY)

                cursorY -= 17f
            }

            // Pie de página
            streamAscii("q\n0.85 0.85 0.87 RG\n0.5 w\n40 45 515 0.5 re f\nQ\n")
            streamText("StitchMesh 3D CAD — Generado automáticamente conforme a RF-5.3", "/F1", 8f, 40f, 32f)
            streamText("Página ${pageIndex + 1} de $totalPages", "/F1", 8f, 490f, 32f)

            val streamBytes = streamOut.toByteArray()

            // Objeto Page
            startObject(pageObjId)
            writeAscii(
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents $contentObjId 0 R >>\n"
            )
            endObject()

            // Objeto Contents
            startObject(contentObjId)
            writeAscii("<< /Length ${streamBytes.size} >>\nstream\n")
            out.write(streamBytes)
            writeAscii("\nendstream\n")
            endObject()
        }

        // Tabla xref
        val xrefOffset = out.size().toLong()
        val totalObjects = 4 + totalPages * 2
        writeAscii("xref\n0 ${totalObjects + 1}\n")
        writeAscii("0000000000 65535 f \n")
        for (i in 1..totalObjects) {
            val off = if (i < objectOffsets.size) objectOffsets[i] else 0L
            writeAscii(String.format(Locale.US, "%010d 00000 n \n", off))
        }

        // Trailer
        writeAscii("trailer\n<< /Size ${totalObjects + 1} /Root 1 0 R >>\n")
        writeAscii("startxref\n$xrefOffset\n%%EOF\n")

        return out.toByteArray()
    }
}
