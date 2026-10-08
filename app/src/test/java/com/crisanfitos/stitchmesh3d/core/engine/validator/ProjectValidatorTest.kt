package com.crisanfitos.stitchmesh3d.core.engine.validator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectValidatorTest {

    private val validHeadInstructions = listOf(
        "V1: AM 6 (6)",
        "V2: 6 aum (12)",
        "V3: [1 pb, 1 aum] * 6 (18)",
        "V4: 18 pb (18)",
        "V5: [1 pb, 1 dism] * 6 (12)",
        "V6: 6 dism (6)"
    )

    private val validBodyInstructions = listOf(
        "V1: AM 6 (6)",
        "V2: 6 aum (12)",
        "V3: 12 pb (12)",
        "V4: 6 dism (6)"
    )

    private val validEarInstructions = listOf(
        "V1: AM 4 (4)",
        "V2: [1 pb, 1 aum] * 2 (6)",
        "V3: 6 pb (6)"
    )

    @Test
    fun `validateProject aprueba proyecto amigurumi con 100 por ciento de partes validas`() {
        val parts = mapOf(
            "Cabeza" to validHeadInstructions,
            "Cuerpo" to validBodyInstructions,
            "Oreja Derecha" to validEarInstructions,
            "Oreja Izquierda" to validEarInstructions
        )

        val projectTree = ProjectValidator.validateProject(
            projectName = "Osito Amigurumi",
            parts = parts
        )

        assertTrue("El proyecto debe ser 100% válido", projectTree.isValid)
        assertEquals("Osito Amigurumi", projectTree.projectName)
        assertEquals(4, projectTree.totalParts)
        assertEquals(4, projectTree.validPartsCount)
        assertEquals(0, projectTree.invalidPartsCount)
        assertEquals(6 + 4 + 3 + 3, projectTree.totalRounds)
        assertEquals(16, projectTree.validRounds)
        assertEquals(0, projectTree.invalidRounds)
        assertTrue("No debe haber inconsistencias", projectTree.allInconsistencies.isEmpty())
    }

    @Test
    fun `validateProject invalida proyecto global si una sola parte contiene error de base`() {
        // En el cuerpo, la vuelta 3 solo teje 10 puntos en vez de los 12 disponibles
        val corruptedBodyInstructions = listOf(
            "V1: AM 6 (6)",
            "V2: 6 aum (12)",
            "V3: [1 pb, 1 aum] * 5 (15)", // Consume 10, esperaba 12
            "V4: 15 pb (15)"
        )

        val parts = mapOf(
            "Cabeza" to validHeadInstructions,
            "Cuerpo" to corruptedBodyInstructions
        )

        val projectTree = ProjectValidator.validateProject(
            projectName = "Conejo",
            parts = parts
        )

        assertFalse("El proyecto no puede ser válido si una parte tiene errores", projectTree.isValid)
        assertEquals(2, projectTree.totalParts)
        assertEquals(1, projectTree.validPartsCount)
        assertEquals(1, projectTree.invalidPartsCount)

        // Verificar validez por parte individual
        val cabezaResult = projectTree.parts.first { it.partName == "Cabeza" }
        val cuerpoResult = projectTree.parts.first { it.partName == "Cuerpo" }
        assertTrue("La cabeza es válida", cabezaResult.isValid)
        assertFalse("El cuerpo es inválido", cuerpoResult.isValid)

        // Verificar inconsistencias consolidadas del proyecto
        assertEquals(1, projectTree.allInconsistencies.size)
        val error = projectTree.allInconsistencies.first()
        assertEquals("Cuerpo", error.partName)
        assertEquals(3, error.roundIndex)
        assertEquals(3, error.roundNumber)
        assertEquals(InconsistencyType.BASE_MISMATCH, error.type)
        assertEquals(-2, error.difference)
        assertTrue(error.message.contains("faltaron 2 pts por tejer"))
    }

    @Test
    fun `validateProject invalida proyecto global si una parte contiene error de cierre`() {
        val corruptedHeadInstructions = listOf(
            "V1: AM 6 (6)",
            "V2: 6 aum (15)", // Produce 12, declaró 15
            "V3: 12 pb (12)"
        )

        val parts = mapOf(
            "Cabeza" to corruptedHeadInstructions,
            "Cuerpo" to validBodyInstructions
        )

        val projectTree = ProjectValidator.validateProject(
            projectName = "Gatito",
            parts = parts
        )

        assertFalse(projectTree.isValid)
        assertEquals(1, projectTree.allInconsistencies.size)
        val error = projectTree.allInconsistencies.first()
        assertEquals("Cabeza", error.partName)
        assertEquals(2, error.roundIndex)
        assertEquals(2, error.roundNumber)
        assertEquals(InconsistencyType.CLOSURE_MISMATCH, error.type)
        assertEquals(-3, error.difference)
    }

    @Test
    fun `validateProject consolida multiples inconsistencias de diferentes partes y tipos`() {
        val brokenHead = listOf(
            "V1: AM 6 (6)",
            "V2: [1 pb, aum" // Error sintáctico
        )
        val brokenBody = listOf(
            "V1: AM 6 (6)",
            "V2: 6 aum (12)",
            "V3: 10 pb (10)" // Error de base: consume 10, esperaba 12
        )
        val brokenEar = listOf(
            "V1: AM 4 (4)",
            "V2: 4 aum (10)" // Error de cierre: produce 8, declaró 10
        )

        val parts = mapOf(
            "Cabeza" to brokenHead,
            "Cuerpo" to brokenBody,
            "Oreja" to brokenEar
        )

        val projectTree = ProjectValidator.validateProject(
            projectName = "Proyecto Múltiples Errores",
            parts = parts
        )

        assertFalse(projectTree.isValid)
        assertEquals(0, projectTree.validPartsCount)
        assertEquals(3, projectTree.invalidPartsCount)
        assertEquals(3, projectTree.allInconsistencies.size)

        val errorTypes = projectTree.allInconsistencies.map { it.type }
        assertTrue(errorTypes.contains(InconsistencyType.SYNTAX_ERROR))
        assertTrue(errorTypes.contains(InconsistencyType.BASE_MISMATCH))
        assertTrue(errorTypes.contains(InconsistencyType.CLOSURE_MISMATCH))
    }

    @Test
    fun `validateProject marca como invalido un proyecto vacio o partes sin instrucciones`() {
        val emptyProject = ProjectValidator.validateProject(
            projectName = "Vacío",
            parts = emptyMap()
        )
        assertFalse("Un proyecto sin partes no es válido", emptyProject.isValid)

        val emptyPartProject = ProjectValidator.validateProject(
            projectName = "Parte Vacía",
            parts = mapOf("Sin Vueltas" to emptyList())
        )
        assertFalse("Un proyecto con parte vacía no es válido", emptyPartProject.isValid)
    }

    @Test
    fun `validatePart calcula metricas correctas para parte individual`() {
        val partResult = ProjectValidator.validatePart(
            partName = "Hocico",
            instructions = listOf(
                "V1: AM 6 (6)",
                "V2: 6 aum (12)",
                "V3: 10 pb (10)", // Error base
                "V4: 10 pb (10)"
            )
        )

        assertFalse(partResult.isValid)
        assertEquals("Hocico", partResult.partName)
        assertEquals(4, partResult.totalRounds)
        assertEquals(3, partResult.validRoundsCount)
        assertEquals(1, partResult.invalidRoundsCount)
        assertEquals(1, partResult.inconsistencies.size)
        assertEquals(3, partResult.inconsistencies.first().roundIndex)
    }
}
