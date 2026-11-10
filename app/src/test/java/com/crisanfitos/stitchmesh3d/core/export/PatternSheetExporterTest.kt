package com.crisanfitos.stitchmesh3d.core.export

import com.crisanfitos.stitchmesh3d.domain.model.PatternRound
import com.crisanfitos.stitchmesh3d.domain.model.Project
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternSheetExporterTest {

    private fun sampleProject(roundCount: Int = 12): Project {
        val rounds = (1..roundCount).map { idx ->
            PatternRound(
                id = "r-$idx",
                partId = "part-1",
                roundNumber = idx,
                rawInstruction = if (idx == 1) "AM 6" else "6 pb en círculo",
                consumedStitches = if (idx == 1) 0 else 6,
                producedStitches = 6,
                declaredStitches = 6,
                isValid = true
            )
        }
        return Project(
            id = "proj-fox",
            title = "Zorro Ágil y Pequeño",
            description = "Modelo paramétrico de prueba con acentos españoles",
            hookSizeMm = 3.5f,
            customStitchWidthMm = 3.2f,
            customStitchHeightMm = 3.0f,
            isValid = true,
            parts = listOf(
                ProjectPart(
                    id = "part-1",
                    projectId = "proj-fox",
                    name = "Cabeza Añil",
                    rounds = rounds
                )
            )
        )
    }

    @Test
    fun `exportToMarkdown includes tension swatch 10x10 yarn consumption and round table without forbidden terms`() {
        val project = sampleProject()
        val markdown = PatternSheetExporter.exportToMarkdown(project)

        assertTrue(markdown.contains("# 🧶 Zorro Ágil y Pequeño — Ficha Técnica de Patrón"))
        assertTrue(markdown.contains("Muestra de tensión (10x10 cm)"))
        assertTrue(markdown.contains("## 🧩 Pieza: Cabeza Añil"))
        assertTrue(markdown.contains("| V1 | `AM 6` | 0 | **6** |"))
        assertFalse(markdown.lowercase().contains("galga"))
    }

    @Test
    fun `exportToPdfBytes produces valid multi-page PDF document header, trailer, xref and fonts`() {
        val project = sampleProject(roundCount = 20)
        val pdfBytes = PatternSheetExporter.exportToPdfBytes(project)
        val pdfText = String(pdfBytes, Charsets.ISO_8859_1)

        assertTrue(pdfText.startsWith("%PDF-1.4"))
        assertTrue(pdfText.contains("/Type /Catalog"))
        assertTrue(pdfText.contains("/Type /Pages"))
        assertTrue(pdfText.contains("/Type /Page"))
        assertTrue(pdfText.contains("/BaseFont /Helvetica"))
        assertTrue(pdfText.contains("/Encoding /WinAnsiEncoding"))
        assertTrue(pdfText.contains("xref\n0 "))
        assertTrue(pdfText.contains("startxref\n"))
        assertTrue(pdfText.trimEnd().endsWith("%%EOF"))
        assertFalse(pdfText.lowercase().contains("galga"))
    }

    @Test
    fun `encodeWinAnsiString correctly converts Spanish accents and special punctuation`() {
        val original = "Zorro Ágil y Pequeño con piña, ¿verdad? ¡Sí!"
        val encodedBytes = PatternSheetExporter.encodeWinAnsiString(original)

        // Verificamos que las tildes se codifiquen en los bytes WinAnsi correspondientes
        // Á -> 0xC1 (193), í -> 0xED (237), e -> 0xE9, ñ -> 0xF1 (241), ¿ -> 0xBF (191), ¡ -> 0xA1 (161)
        val byteList = encodedBytes.map { it.toInt() and 0xFF }

        assertTrue(byteList.contains(0xC1)) // Á
        assertTrue(byteList.contains(0xED)) // í
        assertTrue(byteList.contains(0xF1)) // ñ
        assertTrue(byteList.contains(0xBF)) // ¿
        assertTrue(byteList.contains(0xA1)) // ¡
    }

    @Test
    fun `exportToPdfBytes generates multiple pages when project has many rounds`() {
        val longProject = sampleProject(roundCount = 35)
        val pdfBytes = PatternSheetExporter.exportToPdfBytes(longProject)
        val pdfText = String(pdfBytes, Charsets.ISO_8859_1)

        assertTrue(pdfText.contains("/Count 2") || pdfText.contains("/Count 3"))
        assertTrue(pdfText.contains("Página 1 de"))
        assertTrue(pdfText.contains("Página 2 de"))
    }
}
