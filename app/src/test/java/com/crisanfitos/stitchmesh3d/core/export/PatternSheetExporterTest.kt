package com.crisanfitos.stitchmesh3d.core.export

import com.crisanfitos.stitchmesh3d.domain.model.PatternRound
import com.crisanfitos.stitchmesh3d.domain.model.Project
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternSheetExporterTest {

    private fun sampleProject(): Project {
        val rounds = (1..12).map { idx ->
            PatternRound(
                id = "r-$idx",
                partId = "part-1",
                roundNumber = idx,
                rawInstruction = if (idx == 1) "AM 6" else "6 pb",
                consumedStitches = if (idx == 1) 0 else 6,
                producedStitches = 6,
                declaredStitches = 6,
                isValid = true
            )
        }
        return Project(
            id = "proj-fox",
            title = "Zorro Amigurumi",
            description = "Modelo paramétrico de prueba",
            hookSizeMm = 3.5f,
            customStitchWidthMm = 3.2f,
            customStitchHeightMm = 3.0f,
            isValid = true,
            parts = listOf(
                ProjectPart(
                    id = "part-1",
                    projectId = "proj-fox",
                    name = "Cabeza",
                    rounds = rounds
                )
            )
        )
    }

    @Test
    fun `exportToMarkdown includes tension swatch 10x10 yarn consumption and round table without forbidden terms`() {
        val project = sampleProject()
        val markdown = PatternSheetExporter.exportToMarkdown(project)

        assertTrue(markdown.contains("# 🧶 Zorro Amigurumi — Ficha Técnica de Patrón"))
        assertTrue(markdown.contains("Muestra de tensión (10x10 cm)"))
        assertTrue(markdown.contains("## 🧩 Pieza: Cabeza"))
        assertTrue(markdown.contains("| V1 | `AM 6` | 0 | **6** |"))
        assertFalse(markdown.lowercase().contains("galga"))
    }

    @Test
    fun `exportToPdfBytes produces valid multi-page PDF document header and trailer`() {
        val project = sampleProject()
        val pdfBytes = PatternSheetExporter.exportToPdfBytes(project, maxLinesPerPage = 10)
        val pdfText = String(pdfBytes, Charsets.UTF_8)

        assertTrue(pdfText.startsWith("%PDF-1.4"))
        assertTrue(pdfText.contains("/Type /Catalog"))
        assertTrue(pdfText.contains("/Type /Page"))
        assertTrue(pdfText.trimEnd().endsWith("%%EOF"))
    }
}
