package com.crisanfitos.stitchmesh3d.core.engine.validator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArithmeticValidatorTest {

    @Test
    fun `validateRound valida exitosamente V1 con Anillo Magico`() {
        val result = ArithmeticValidator.validateRound(
            rawLine = "V1: AM 6 (6)",
            previousRoundStitches = null
        )

        assertTrue("El resultado debe ser válido", result.isValid)
        assertTrue(result is RoundValidationResult.Valid)
        val valid = result as RoundValidationResult.Valid
        assertEquals(1, valid.roundNumber)
        assertEquals(0, valid.totalConsumed)
        assertEquals(6, valid.totalProduced)
        assertEquals(6, valid.declaredCount)
        assertFalse(valid.hasBaseMismatch)
        assertFalse(valid.hasClosureMismatch)
        assertFalse(valid.hasSyntaxError)
    }

    @Test
    fun `validateRound valida exitosamente V1 con AM sin recuento declarado`() {
        val result = ArithmeticValidator.validateRound(
            rawLine = "V1: AM 8",
            previousRoundStitches = 0
        )

        assertTrue(result.isValid)
        assertTrue(result is RoundValidationResult.Valid)
        val valid = result as RoundValidationResult.Valid
        assertEquals(0, valid.totalConsumed)
        assertEquals(8, valid.totalProduced)
        assertNull(valid.declaredCount)
    }

    @Test
    fun `validateRound detecta error si V1 consume puntos base sin tener vuelta previa`() {
        val result = ArithmeticValidator.validateRound(
            rawLine = "V1: 6 pb (6)",
            previousRoundStitches = null,
            language = ValidationLanguage.ES
        )

        assertFalse(result.isValid)
        assertTrue(result.hasBaseMismatch)
        assertTrue(result is RoundValidationResult.BaseMismatch)
        val mismatch = result as RoundValidationResult.BaseMismatch
        assertEquals(6, mismatch.totalConsumed)
        assertEquals(0, mismatch.expectedBaseStitches)
        assertTrue(mismatch.message.contains("sin tener vuelta previa ni iniciar con Anillo Mágico"))
    }

    @Test
    fun `validateRound valida exitosamente vuelta 2 con aumentos`() {
        val result = ArithmeticValidator.validateRound(
            rawLine = "V2: 6 aum (12)",
            previousRoundStitches = 6
        )

        assertTrue(result.isValid)
        val valid = result as RoundValidationResult.Valid
        assertEquals(6, valid.totalConsumed)
        assertEquals(12, valid.totalProduced)
        assertEquals(12, valid.declaredCount)
    }

    @Test
    fun `validateRound valida exitosamente vuelta 3 con repeticion`() {
        val result = ArithmeticValidator.validateRound(
            rawLine = "V3: [1 pb, 1 aum] * 6 (18)",
            previousRoundStitches = 12
        )

        assertTrue(result.isValid)
        val valid = result as RoundValidationResult.Valid
        assertEquals(12, valid.totalConsumed)
        assertEquals(18, valid.totalProduced)
        assertEquals(18, valid.declaredCount)
    }

    @Test
    fun `validateRound detecta Invariante 1 BaseMismatch cuando faltan puntos por consumir`() {
        // La vuelta anterior tenía 12 puntos, pero [1 pb, 1 aum] * 5 solo consume 10 pts (faltan 2)
        val result = ArithmeticValidator.validateRound(
            rawLine = "V3: [1 pb, 1 aum] * 5 (15)",
            previousRoundStitches = 12,
            language = ValidationLanguage.ES
        )

        assertFalse(result.isValid)
        assertTrue(result.hasBaseMismatch)
        assertFalse(result.hasClosureMismatch)
        assertTrue(result is RoundValidationResult.BaseMismatch)
        val mismatch = result as RoundValidationResult.BaseMismatch
        assertEquals(10, mismatch.totalConsumed)
        assertEquals(12, mismatch.expectedBaseStitches)
        assertEquals(-2, mismatch.difference)
        assertTrue(mismatch.message.contains("faltaron 2 pts por tejer"))
    }

    @Test
    fun `validateRound detecta Invariante 1 BaseMismatch cuando sobran puntos consumidos`() {
        // La vuelta anterior tenía 12 puntos, pero [1 pb, 1 aum] * 7 consume 14 pts (sobran 2)
        val result = ArithmeticValidator.validateRound(
            rawLine = "V3: [1 pb, 1 aum] * 7 (21)",
            previousRoundStitches = 12,
            language = ValidationLanguage.ES
        )

        assertFalse(result.isValid)
        assertTrue(result.hasBaseMismatch)
        assertFalse(result.hasClosureMismatch)
        assertTrue(result is RoundValidationResult.BaseMismatch)
        val mismatch = result as RoundValidationResult.BaseMismatch
        assertEquals(14, mismatch.totalConsumed)
        assertEquals(12, mismatch.expectedBaseStitches)
        assertEquals(2, mismatch.difference)
        assertTrue(mismatch.message.contains("sobraron 2 pts de más"))
    }

    @Test
    fun `validateRound detecta Invariante 2 ClosureMismatch cuando produccion difiere de declarado`() {
        // Base es correcta: 6 aum consume 6 (la anterior tenía 6).
        // Produce 12, pero el patrón declara erróneamente (14).
        val result = ArithmeticValidator.validateRound(
            rawLine = "V2: 6 aum (14)",
            previousRoundStitches = 6,
            language = ValidationLanguage.ES
        )

        assertFalse(result.isValid)
        assertFalse(result.hasBaseMismatch)
        assertTrue(result.hasClosureMismatch)
        assertTrue(result is RoundValidationResult.ClosureMismatch)
        val mismatch = result as RoundValidationResult.ClosureMismatch
        assertEquals(6, mismatch.totalConsumed)
        assertEquals(12, mismatch.totalProduced)
        assertEquals(14, mismatch.declaredCount)
        assertEquals(-2, mismatch.difference)
        assertTrue(mismatch.message.contains("no coinciden con el total declarado (14)"))
        assertTrue(mismatch.message.contains("faltaron 2 pts"))
    }

    @Test
    fun `validateRound detecta MultipleErrors cuando fallan base y cierre simultaneamente`() {
        // Vuelta anterior tenía 6 puntos.
        // Texto: "V2: 5 aum (15)" -> consume 5 (esperaba 6), produce 10 (declarado 15)
        val result = ArithmeticValidator.validateRound(
            rawLine = "V2: 5 aum (15)",
            previousRoundStitches = 6,
            language = ValidationLanguage.ES
        )

        assertFalse(result.isValid)
        assertTrue(result.hasBaseMismatch)
        assertTrue(result.hasClosureMismatch)
        assertTrue(result is RoundValidationResult.MultipleErrors)
        val multiple = result as RoundValidationResult.MultipleErrors
        assertEquals(5, multiple.totalConsumed)
        assertEquals(6, multiple.expectedBaseStitches)
        assertEquals(-1, multiple.baseDifference)
        assertEquals(10, multiple.totalProduced)
        assertEquals(15, multiple.declaredCount)
        assertEquals(-5, multiple.closureDifference)
        assertTrue(multiple.baseMessage.contains("faltaron 1 pts por tejer"))
        assertTrue(multiple.closureMessage.contains("faltaron 5 pts"))
    }

    @Test
    fun `validateRound maneja errores sintacticos con SyntaxError`() {
        val result = ArithmeticValidator.validateRound(
            rawLine = "V2: [1 pb, aum",
            previousRoundStitches = 6
        )

        assertFalse(result.isValid)
        assertTrue(result.hasSyntaxError)
        assertTrue(result is RoundValidationResult.SyntaxError)
        val syntaxError = result as RoundValidationResult.SyntaxError
        assertTrue(syntaxError.parseErrors.isNotEmpty())
    }

    @Test
    fun `validateRound genera mensajes de error localizados en ingles`() {
        val baseResult = ArithmeticValidator.validateRound(
            rawLine = "V2: 5 aum (10)",
            previousRoundStitches = 6,
            language = ValidationLanguage.EN
        )
        assertTrue(baseResult is RoundValidationResult.BaseMismatch)
        val baseMismatch = baseResult as RoundValidationResult.BaseMismatch
        assertTrue(baseMismatch.message.contains("missing 1 sts to work"))

        val closureResult = ArithmeticValidator.validateRound(
            rawLine = "V2: 6 aum (14)",
            previousRoundStitches = 6,
            language = ValidationLanguage.EN
        )
        assertTrue(closureResult is RoundValidationResult.ClosureMismatch)
        val closureMismatch = closureResult as RoundValidationResult.ClosureMismatch
        assertTrue(closureMismatch.message.contains("missing 2 sts"))
    }

    @Test
    fun `validatePattern valida patron completo de esfera de amigurumi de 8 vueltas`() {
        val spherePattern = listOf(
            "V1: AM 6 (6)",
            "V2: 6 aum (12)",
            "V3: [1 pb, 1 aum] * 6 (18)",
            "V4: [2 pb, 1 aum] * 6 (24)",
            "V5: 24 pb (24)",
            "V6: [2 pb, 1 dism] * 6 (18)",
            "V7: [1 pb, 1 dism] * 6 (12)",
            "V8: 6 dism (6)"
        )

        val results = ArithmeticValidator.validatePattern(spherePattern)

        assertEquals(8, results.size)
        assertTrue(results.all { it.isValid })

        // Verificar flujo de puntos producidos en cada vuelta
        val producedPerRound = results.map { it.totalProduced }
        assertEquals(listOf(6, 12, 18, 24, 24, 18, 12, 6), producedPerRound)
    }

    @Test
    fun `validatePattern valida patron con base de cadeneta lineal`() {
        val chainPattern = listOf(
            "V1: 10 cad (10)",
            "V2: 10 pb (10)"
        )

        val results = ArithmeticValidator.validatePattern(chainPattern)

        assertEquals(2, results.size)
        assertTrue(results.all { it.isValid })
        assertEquals(0, results[0].totalConsumed)
        assertEquals(10, results[0].totalProduced)
        assertEquals(10, results[1].totalConsumed)
        assertEquals(10, results[1].totalProduced)
    }
}
