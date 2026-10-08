package com.crisanfitos.stitchmesh3d.core.engine.validator

import com.crisanfitos.stitchmesh3d.core.engine.parser.CrochetAstNode
import com.crisanfitos.stitchmesh3d.core.engine.parser.CrochetParser

/**
 * Validador aritmético formal de invariantes de base y cierre para crochet y amigurumi.
 * Implementa los requisitos RF-1.2 y RF-1.3 del Core Engine y TRD §4.
 *
 * - Invariante 1 (Base Consumida): Σ C_i == S_{k-1}
 * - Invariante 2 (Puntos Producidos): Σ P_i == total declarado entre paréntesis
 */
object ArithmeticValidator {

    /**
     * Valida una instrucción textual de vuelta de crochet frente a la cantidad de puntos de la vuelta anterior.
     *
     * @param rawLine Línea de texto con la instrucción (ej: "V2: [1 pb, 1 aum] * 6 (18)").
     * @param previousRoundStitches Puntos producidos por la vuelta anterior S_{k-1} (null o 0 si es la vuelta inicial).
     * @param language Idioma para los mensajes explicativos de diagnóstico.
     * @return [RoundValidationResult] con el diagnóstico detallado.
     */
    fun validateRound(
        rawLine: String,
        previousRoundStitches: Int? = null,
        language: ValidationLanguage = ValidationLanguage.ES
    ): RoundValidationResult {
        val parseResult = CrochetParser.parse(rawLine)
        if (!parseResult.isSuccess || parseResult.ast == null) {
            val roundNumber = parseResult.ast?.header?.startRound
            return RoundValidationResult.SyntaxError(
                roundNumber = roundNumber,
                rawLine = rawLine,
                parseErrors = parseResult.errors
            )
        }

        return validateRound(
            ast = parseResult.ast,
            previousRoundStitches = previousRoundStitches,
            language = language
        )
    }

    /**
     * Valida un nodo AST [CrochetAstNode.RoundNode] frente a la cantidad de puntos de la vuelta anterior.
     *
     * @param ast Árbol AST de la vuelta analizada.
     * @param previousRoundStitches Puntos producidos por la vuelta anterior S_{k-1} (null o 0 si es la vuelta inicial).
     * @param language Idioma para los mensajes explicativos de diagnóstico.
     * @return [RoundValidationResult] con el diagnóstico detallado.
     */
    fun validateRound(
        ast: CrochetAstNode.RoundNode,
        previousRoundStitches: Int? = null,
        language: ValidationLanguage = ValidationLanguage.ES
    ): RoundValidationResult {
        val roundNumber = ast.header?.startRound
        val rawLine = ast.rawLine
        val totalConsumed = ast.totalConsumedStitches
        val totalProduced = ast.totalProducedStitches
        val declared = ast.declaredCount

        // 1. Invariante 1: Base Consumida (Σ C_i == S_{k-1})
        var baseMismatch = false
        var baseMessage: String? = null
        val expectedBase = previousRoundStitches ?: 0

        if (previousRoundStitches == null || previousRoundStitches == 0) {
            // Vuelta inicial (V1 / inicio de pieza)
            // Si consume 0 puntos base (ej. Anillo Mágico AM 6 o cadeneta de inicio), el invariante se cumple.
            if (totalConsumed != 0) {
                baseMismatch = true
                baseMessage = when (language) {
                    ValidationLanguage.ES ->
                        "La primera vuelta consumió $totalConsumed puntos base sin tener vuelta previa ni iniciar con Anillo Mágico o cadeneta."
                    ValidationLanguage.EN ->
                        "First round consumed $totalConsumed base stitches without a previous round or starting with a Magic Ring / chain."
                }
            }
        } else {
            // Vuelta subsiguiente con S_{k-1} puntos disponibles
            if (totalConsumed != previousRoundStitches) {
                baseMismatch = true
                val delta = totalConsumed - previousRoundStitches
                baseMessage = if (delta < 0) {
                    val missing = -delta
                    when (language) {
                        ValidationLanguage.ES ->
                            "Consumió $totalConsumed pts base, pero la vuelta anterior tenía $previousRoundStitches: faltaron $missing pts por tejer."
                        ValidationLanguage.EN ->
                            "Consumed $totalConsumed base sts, but previous round had $previousRoundStitches: missing $missing sts to work."
                    }
                } else {
                    val extra = delta
                    when (language) {
                        ValidationLanguage.ES ->
                            "Consumió $totalConsumed pts base, pero la vuelta anterior tenía $previousRoundStitches: sobraron $extra pts de más."
                        ValidationLanguage.EN ->
                            "Consumed $totalConsumed base sts, but previous round had $previousRoundStitches: exceeded by $extra sts."
                    }
                }
            }
        }

        // 2. Invariante 2: Puntos Producidos (Σ P_i == declarado)
        var closureMismatch = false
        var closureMessage: String? = null

        if (declared != null && totalProduced != declared) {
            closureMismatch = true
            val delta = totalProduced - declared
            closureMessage = if (delta < 0) {
                val missing = -delta
                when (language) {
                    ValidationLanguage.ES ->
                        "Puntos producidos ($totalProduced) no coinciden con el total declarado ($declared): faltaron $missing pts."
                    ValidationLanguage.EN ->
                        "Produced stitches ($totalProduced) do not match declared count ($declared): missing $missing sts."
                }
            } else {
                val extra = delta
                when (language) {
                    ValidationLanguage.ES ->
                        "Puntos producidos ($totalProduced) no coinciden con el total declarado ($declared): sobraron $extra pts."
                    ValidationLanguage.EN ->
                        "Produced stitches ($totalProduced) do not match declared count ($declared): exceeded by $extra sts."
                }
            }
        }

        // 3. Resolución del resultado
        return when {
            baseMismatch && closureMismatch -> {
                RoundValidationResult.MultipleErrors(
                    roundNumber = roundNumber,
                    rawLine = rawLine,
                    ast = ast,
                    totalConsumed = totalConsumed,
                    expectedBaseStitches = expectedBase,
                    totalProduced = totalProduced,
                    declaredCount = declared!!,
                    baseMessage = baseMessage!!,
                    closureMessage = closureMessage!!
                )
            }
            baseMismatch -> {
                RoundValidationResult.BaseMismatch(
                    roundNumber = roundNumber,
                    rawLine = rawLine,
                    ast = ast,
                    totalConsumed = totalConsumed,
                    expectedBaseStitches = expectedBase,
                    totalProduced = totalProduced,
                    declaredCount = declared,
                    message = baseMessage!!
                )
            }
            closureMismatch -> {
                RoundValidationResult.ClosureMismatch(
                    roundNumber = roundNumber,
                    rawLine = rawLine,
                    ast = ast,
                    totalConsumed = totalConsumed,
                    totalProduced = totalProduced,
                    declaredCount = declared!!,
                    message = closureMessage!!
                )
            }
            else -> {
                RoundValidationResult.Valid(
                    roundNumber = roundNumber,
                    rawLine = rawLine,
                    ast = ast,
                    totalConsumed = totalConsumed,
                    totalProduced = totalProduced,
                    declaredCount = declared
                )
            }
        }
    }

    /**
     * Valida secuencialmente una secuencia de instrucciones de vueltas que componen un patrón continuo.
     * Encadena los puntos producidos de cada vuelta como base para la siguiente.
     *
     * @param lines Lista de líneas de texto del patrón.
     * @param language Idioma para los mensajes diagnósticos.
     * @return Lista de [RoundValidationResult] correspondiente a cada línea analizada.
     */
    fun validatePattern(
        lines: List<String>,
        language: ValidationLanguage = ValidationLanguage.ES
    ): List<RoundValidationResult> {
        val results = mutableListOf<RoundValidationResult>()
        var currentBase: Int? = null

        for (line in lines) {
            if (line.isBlank()) continue

            val result = validateRound(
                rawLine = line,
                previousRoundStitches = currentBase,
                language = language
            )
            results.add(result)

            // Para la siguiente vuelta, tomamos los puntos producidos por la vuelta actual
            // incluso si hubo error de base o cierre, de modo que el validador pueda continuar
            // diagnosticando las vueltas posteriores.
            currentBase = when (result) {
                is RoundValidationResult.Valid -> result.totalProduced
                is RoundValidationResult.BaseMismatch -> result.totalProduced
                is RoundValidationResult.ClosureMismatch -> result.totalProduced
                is RoundValidationResult.MultipleErrors -> result.totalProduced
                is RoundValidationResult.SyntaxError -> currentBase
            }
        }

        return results
    }
}
