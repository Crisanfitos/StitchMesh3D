package com.crisanfitos.stitchmesh3d.core.engine

import com.crisanfitos.stitchmesh3d.core.engine.validator.ArithmeticValidator
import com.crisanfitos.stitchmesh3d.core.engine.validator.CorrectionAssistant
import com.crisanfitos.stitchmesh3d.core.engine.validator.ProjectValidator
import com.crisanfitos.stitchmesh3d.core.engine.validator.RoundValidationResult
import com.crisanfitos.stitchmesh3d.core.engine.validator.ValidationLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Suite integral de pruebas del motor Linter con patrones reales de amigurumi
 * y verificación del requisito no funcional RND-2 (latencia < 30 ms para 200 vueltas).
 */
class LinterTest {

    // --- PATRÓN 1: Esfera amigurumi completa de 30 vueltas ---
    private val amigurumiSphere30Rounds = buildList {
        add("V1: AM 6 (6)")
        add("V2: 6 aum (12)")
        add("V3: [1 pb, 1 aum] * 6 (18)")
        add("V4: [2 pb, 1 aum] * 6 (24)")
        add("V5: [3 pb, 1 aum] * 6 (30)")
        add("V6: [4 pb, 1 aum] * 6 (36)")
        add("V7: [5 pb, 1 aum] * 6 (42)")
        add("V8: [6 pb, 1 aum] * 6 (48)")
        add("V9: [7 pb, 1 aum] * 6 (54)")
        add("V10: [8 pb, 1 aum] * 6 (60)")
        // Vueltas rectas intermedias
        for (v in 11..20) {
            add("V$v: 60 pb (60)")
        }
        // Vueltas de disminución
        add("V21: [8 pb, 1 dism] * 6 (54)")
        add("V22: [7 pb, 1 dism] * 6 (48)")
        add("V23: [6 pb, 1 dism] * 6 (42)")
        add("V24: [5 pb, 1 dism] * 6 (36)")
        add("V25: [4 pb, 1 dism] * 6 (30)")
        add("V26: [3 pb, 1 dism] * 6 (24)")
        add("V27: [2 pb, 1 dism] * 6 (18)")
        add("V28: [1 pb, 1 dism] * 6 (12)")
        add("V29: 6 dism (6)")
        add("V30: 6 pe (6)")
    }

    // --- PATRÓN 2: Transición de talón con alturas heterogéneas (pb, pma, pa) ---
    private val heelTransitionPattern = listOf(
        "V1: AM 6 (6)",
        "V2: 6 aum (12)",
        "V3: [1 pb, 1 aum] * 6 (18)",
        "V4: 6 pb, 6 pma, 6 pb (18)",
        "V5: 6 pb, 3 pa, 3 pa, 6 pb (18)",
        "V6: 4 pb, 5 dism, 4 pb (13)",
        "V7: 4 pb, 1 aum, 3 pb, 1 aum, 4 pb (15)",
        "V8: 15 pb (15)"
    )

    // --- PATRÓN 3: Proyecto amigurumi multipieza completo (5 componentes) ---
    private val multipartBearParts = mapOf(
        "Cabeza" to listOf(
            "V1: AM 6 (6)",
            "V2: 6 aum (12)",
            "V3: [1 pb, 1 aum] * 6 (18)",
            "V4: 18 pb (18)",
            "V5: [1 pb, 1 dism] * 6 (12)",
            "V6: 6 dism (6)"
        ),
        "Cuerpo" to listOf(
            "V1: AM 6 (6)",
            "V2: 6 aum (12)",
            "V3: [1 pb, 1 aum] * 6 (18)",
            "V4: 18 pb (18)",
            "V5: 18 pb (18)",
            "V6: [1 pb, 1 dism] * 6 (12)",
            "V7: 6 dism (6)"
        ),
        "Hocico" to listOf(
            "V1: AM 6 (6)",
            "V2: [1 pb, 1 aum] * 3 (9)",
            "V3: 9 pb (9)"
        ),
        "Brazo" to listOf(
            "V1: AM 5 (5)",
            "V2: 5 aum (10)",
            "V3: 10 pb (10)",
            "V4: 5 dism (5)"
        ),
        "Pierna" to listOf(
            "V1: AM 6 (6)",
            "V2: 6 aum (12)",
            "V3: 12 pb (12)",
            "V4: 3 dism, 6 pb (9)",
            "V5: 9 pb (9)"
        )
    )

    @Test
    fun `testAmigurumiSphere30Rounds valida exitosamente todos los invariantes formales`() {
        val results = ArithmeticValidator.validatePattern(amigurumiSphere30Rounds)

        assertEquals("Debe procesar las 30 vueltas", 30, results.size)
        assertTrue("Todas las vueltas de la esfera deben ser válidas", results.all { it.isValid })

        // Verificar puntos producidos en hitos clave
        assertEquals(6, results[0].totalProduced)   // V1
        assertEquals(12, results[1].totalProduced)  // V2
        assertEquals(60, results[9].totalProduced)  // V10
        assertEquals(60, results[19].totalProduced) // V20
        assertEquals(6, results[28].totalProduced)  // V29
        assertEquals(6, results[29].totalProduced)  // V30 (puntos enanos de cierre)
    }

    @Test
    fun `testHeelTransitionPattern valida transicion asimetrica con variacion de alturas`() {
        val results = ArithmeticValidator.validatePattern(heelTransitionPattern)

        assertEquals(8, results.size)
        assertTrue("Todas las vueltas del talón deben ser válidas", results.all { it.isValid })
        assertEquals(listOf(6, 12, 18, 18, 18, 13, 15, 15), results.map { it.totalProduced })
    }

    @Test
    fun `testMultipartBearProject valida ensamblado multipieza completo`() {
        val projectTree = ProjectValidator.validateProject(
            projectName = "Osito Amigurumi Completo",
            parts = multipartBearParts
        )

        assertTrue("El proyecto multipieza debe ser 100% válido", projectTree.isValid)
        assertEquals(5, projectTree.totalParts)
        assertEquals(5, projectTree.validPartsCount)
        assertEquals(0, projectTree.invalidPartsCount)
        assertTrue("No debe haber inconsistencias", projectTree.allInconsistencies.isEmpty())
    }

    @Test
    fun `testBrokenPatternsCatalog detecta y sugiere correcciones aplicables para cada error`() {
        val brokenCases = listOf(
            // Error de cierre
            Triple("V2: 6 aum (14)", 6, "V2: 6 aum (12)"),
            // Error de base (faltaron repeticiones)
            Triple("V3: [1 pb, 1 aum] * 5 (15)", 12, "V3: [1 pb, 1 aum] * 6 (18)"),
            // V1 sin AM
            Triple("V1: 6 pb (6)", 0, "V1: AM 6 (6)")
        )

        for ((raw, base, expectedFix) in brokenCases) {
            val result = ArithmeticValidator.validateRound(raw, base)
            assertFalse("Debe fallar: $raw", result.isValid)

            val suggestions = CorrectionAssistant.suggestCorrections(result)
            assertTrue("Debe existir sugerencia para: $raw", suggestions.isNotEmpty())

            val match = suggestions.find { it.correctedText == expectedFix }
            assertTrue(
                "Debe contener la corrección esperada '$expectedFix', sugerencias: ${suggestions.map { it.correctedText }}",
                match != null
            )

            // Verificar que al aplicar la corrección la vuelta pasa a ser VÁLIDA
            val revalidation = ArithmeticValidator.validateRound(expectedFix, base)
            assertTrue("La corrección aplicada '$expectedFix' debe ser válida", revalidation.isValid)
        }
    }

    @Test
    fun `testRND2_performanceBenchmark_200RoundsUnder30ms`() = runBlocking {
        // 1. Generar patrón sintético de 200 vueltas válidas representativas de amigurumi
        val pattern200 = buildList {
            add("V1: AM 6 (6)")
            // V2..V25: aumentos progresivos (de 6 a 150 puntos)
            var currentStitches = 6
            for (round in 2..25) {
                // Agregar 6 puntos por vuelta
                val pbBefore = round - 2
                val line = if (pbBefore == 0) {
                    "V$round: 6 aum (${currentStitches + 6})"
                } else {
                    "V$round: [$pbBefore pb, 1 aum] * 6 (${currentStitches + 6})"
                }
                currentStitches += 6
                add(line)
            }
            // V26..V175: 150 vueltas rectas de 150 puntos
            for (round in 26..175) {
                add("V$round: $currentStitches pb ($currentStitches)")
            }
            // V176..V199: disminuciones progresivas (de 150 a 6 puntos)
            for (round in 176..199) {
                val pbBefore = 200 - round - 1
                val line = if (pbBefore == 0) {
                    "V$round: 6 dism (${currentStitches - 6})"
                } else {
                    "V$round: [$pbBefore pb, 1 dism] * 6 (${currentStitches - 6})"
                }
                currentStitches -= 6
                add(line)
            }
            // V200: vuelta final de remate
            add("V200: 6 pe (6)")
        }

        assertEquals("El patrón de benchmark debe tener exactamente 200 vueltas", 200, pattern200.size)

        // 2. Warmup de JIT previo a la medición
        repeat(3) {
            ArithmeticValidator.validatePattern(pattern200)
        }

        // 3. Ejecución y medición en Dispatchers.Default según RND-2
        val (results, elapsedMs) = withContext(Dispatchers.Default) {
            var res: List<RoundValidationResult> = emptyList()
            val time = measureTimeMillis {
                res = ArithmeticValidator.validatePattern(pattern200)
            }
            Pair(res, time)
        }

        // 4. Aserciones de exactitud y latencia RND-2
        assertEquals(200, results.size)
        assertTrue("Todas las 200 vueltas deben ser válidas", results.all { it.isValid })

        println("⚡ [Benchmark RND-2] Validación de 200 vueltas completada en ${elapsedMs} ms en Dispatchers.Default")

        assertTrue(
            "RND-2 Violado: Latencia ($elapsedMs ms) debe ser estrictamente menor que 30 ms",
            elapsedMs < 30
        )
    }
}
