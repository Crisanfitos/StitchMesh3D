package com.crisanfitos.stitchmesh3d.core.engine.validator

import com.crisanfitos.stitchmesh3d.core.engine.parser.CrochetAstNode

/**
 * Asistente inteligente de corrección automática para patrones de crochet y amigurumi según RF-1.5.
 *
 * Analiza las infracciones formales detectadas por [ArithmeticValidator] y genera sugerencias
 * contextuales con texto reemplazante listo para ser aplicado por el usuario.
 */
object CorrectionAssistant {

    private val declaredRegex = Regex(
        """\s*(?:[\(\[]\s*(?:approx\.?|aprox\.?)?\s*(\d+)\s*(?:pts?|puntos?|sts?|stitches?)?\s*[\)\]])\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val multiplierRegex = Regex(
        """(?:\*\s*|x\s*)(\d+)""",
        RegexOption.IGNORE_CASE
    )

    private val simplePbRegex = Regex(
        """^(\s*(?:vueltas?|rounds?|vtas?|[vr])\s*\d+\s*[:.-]?\s*)?(\d+)\s*(?:pb|mp|sc)\s*(?:[\(\[]\s*\d+.*[\)\]])?\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val v1PbRegex = Regex(
        """^(\s*(?:vueltas?|rounds?|vtas?|[vr])\s*1\s*[:.-]?\s*)?(\d+)\s*(?:pb|mp|sc)\s*(?:[\(\[]\s*\d+.*[\)\]])?\s*$""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Genera la lista de sugerencias de corrección a partir de una instrucción y su base previa.
     */
    fun suggestCorrections(
        rawLine: String,
        previousRoundStitches: Int? = null,
        language: ValidationLanguage = ValidationLanguage.ES
    ): List<CorrectionSuggestion> {
        val validationResult = ArithmeticValidator.validateRound(
            rawLine = rawLine,
            previousRoundStitches = previousRoundStitches,
            language = language
        )
        return suggestCorrections(validationResult, language)
    }

    /**
     * Genera la lista de sugerencias de corrección a partir de un [RoundValidationResult].
     */
    fun suggestCorrections(
        roundResult: RoundValidationResult,
        language: ValidationLanguage = ValidationLanguage.ES
    ): List<CorrectionSuggestion> {
        if (roundResult.isValid || roundResult.ast == null) {
            return emptyList()
        }

        val suggestions = mutableListOf<CorrectionSuggestion>()

        when (roundResult) {
            is RoundValidationResult.ClosureMismatch -> {
                // Caso 1: La base es correcta, solo hay discrepancia con el conteo declarado
                suggestClosureFix(roundResult, language)?.let { suggestions.add(it) }
            }

            is RoundValidationResult.BaseMismatch -> {
                // Caso 2: Error en la base consumida frente a S_{k-1}
                suggestBaseFixes(
                    rawLine = roundResult.rawLine,
                    ast = roundResult.ast,
                    totalConsumed = roundResult.totalConsumed,
                    expectedBase = roundResult.expectedBaseStitches,
                    totalProduced = roundResult.totalProduced,
                    declaredCount = roundResult.declaredCount,
                    language = language,
                    outList = suggestions
                )
            }

            is RoundValidationResult.MultipleErrors -> {
                // Caso 3: Discrepancia simultánea de base y cierre
                suggestBaseFixes(
                    rawLine = roundResult.rawLine,
                    ast = roundResult.ast,
                    totalConsumed = roundResult.totalConsumed,
                    expectedBase = roundResult.expectedBaseStitches,
                    totalProduced = roundResult.totalProduced,
                    declaredCount = roundResult.declaredCount,
                    language = language,
                    outList = suggestions
                )
            }

            is RoundValidationResult.SyntaxError -> {
                // No se pueden inferir invariantes matemáticos con errores sintácticos
            }

            is RoundValidationResult.Valid -> {
                // Sin sugerencias requeridas
            }
        }

        return suggestions
    }

    private fun suggestClosureFix(
        mismatch: RoundValidationResult.ClosureMismatch,
        language: ValidationLanguage
    ): CorrectionSuggestion? {
        val raw = mismatch.rawLine
        val produced = mismatch.totalProduced
        val declared = mismatch.declaredCount

        val corrected = if (declaredRegex.containsMatchIn(raw)) {
            declaredRegex.replace(raw) { " ($produced)" }
        } else {
            "$raw ($produced)"
        }

        val title = when (language) {
            ValidationLanguage.ES -> "Actualizar recuento declarado a ($produced)"
            ValidationLanguage.EN -> "Update declared stitch count to ($produced)"
        }

        val explanation = when (language) {
            ValidationLanguage.ES ->
                "La secuencia de puntos produce $produced puntos reales. Se corrige la errata en el recuento declarado entre paréntesis ($declared)."
            ValidationLanguage.EN ->
                "The stitch sequence actually produces $produced stitches. Correcting typo in declared count ($declared)."
        }

        return CorrectionSuggestion(
            title = title,
            explanation = explanation,
            actionType = SuggestionActionType.UPDATE_DECLARED_COUNT,
            originalText = raw,
            correctedText = corrected
        )
    }

    private fun suggestBaseFixes(
        rawLine: String,
        ast: CrochetAstNode.RoundNode,
        totalConsumed: Int,
        expectedBase: Int,
        totalProduced: Int,
        declaredCount: Int?,
        language: ValidationLanguage,
        outList: MutableList<CorrectionSuggestion>
    ) {
        // Heurística A: Vuelta 1 que consumió puntos sin tener vuelta base ni AM
        if (expectedBase == 0) {
            val v1Match = v1PbRegex.find(rawLine)
            if (v1Match != null || rawLine.contains(Regex("""\bpb\b|\bsc\b""", RegexOption.IGNORE_CASE))) {
                val prefix = v1Match?.groupValues?.get(1) ?: "V1: "
                val count = if (totalConsumed > 0) totalConsumed else 6
                val corrected = when (language) {
                    ValidationLanguage.ES -> "${prefix}AM $count ($count)"
                    ValidationLanguage.EN -> "${prefix}MR $count ($count)"
                }
                outList.add(
                    CorrectionSuggestion(
                        title = when (language) {
                            ValidationLanguage.ES -> "Iniciar con Anillo Mágico (AM $count)"
                            ValidationLanguage.EN -> "Start with Magic Ring (MR $count)"
                        },
                        explanation = when (language) {
                            ValidationLanguage.ES ->
                                "La primera vuelta de una pieza de amigurumi cerrada debe iniciar con un Anillo Mágico polar en lugar de tejer sobre el vacío."
                            ValidationLanguage.EN ->
                                "The first round of a closed amigurumi piece must start with a polar Magic Ring instead of working into empty base."
                        },
                        actionType = SuggestionActionType.REPLACE_INSTRUCTION,
                        originalText = rawLine,
                        correctedText = corrected
                    )
                )
                return
            }
        }

        // Heurística B: Línea puramente de puntos bajos (ej. "V3: 10 pb (10)" cuando se esperaba 12)
        val simplePbMatch = simplePbRegex.find(rawLine)
        if (simplePbMatch != null) {
            val prefix = simplePbMatch.groupValues[1]
            val stitchWord = if (language == ValidationLanguage.EN) "sc" else "pb"
            val corrected = "$prefix$expectedBase $stitchWord ($expectedBase)"
            outList.add(
                CorrectionSuggestion(
                    title = when (language) {
                        ValidationLanguage.ES -> "Tejer toda la vuelta en punto bajo ($expectedBase $stitchWord)"
                        ValidationLanguage.EN -> "Work entire round in single crochet ($expectedBase $stitchWord)"
                    },
                    explanation = when (language) {
                        ValidationLanguage.ES ->
                            "Tejer $expectedBase $stitchWord consume exactamente los $expectedBase puntos disponibles de la vuelta anterior."
                        ValidationLanguage.EN ->
                            "Working $expectedBase $stitchWord consumes exactly the $expectedBase available base stitches."
                    },
                    actionType = SuggestionActionType.REPLACE_INSTRUCTION,
                    originalText = rawLine,
                    correctedText = corrected
                )
            )
        }

        // Heurística C: Bloque de repetición con multiplicador (* N)
        val repeatNode = ast.children.filterIsInstance<CrochetAstNode.RepeatNode>().firstOrNull()
        if (repeatNode != null) {
            // Calcular consumo y producción por iteración elemental del bloque
            val unitConsumed = repeatNode.children.sumOf { child ->
                when (child) {
                    is CrochetAstNode.StitchNode -> child.count * child.stitchType.consumedStitches
                    else -> 0
                }
            }
            val unitProduced = repeatNode.children.sumOf { child ->
                when (child) {
                    is CrochetAstNode.StitchNode -> child.count * child.stitchType.producedStitches
                    else -> 0
                }
            }

            if (unitConsumed > 0 && expectedBase % unitConsumed == 0) {
                val targetTimes = expectedBase / unitConsumed
                val newProduced = targetTimes * unitProduced

                // Sustituir el multiplicador en el texto original
                val correctedWithoutDeclared = multiplierRegex.replace(rawLine) { "* $targetTimes" }
                val finalCorrected = if (declaredRegex.containsMatchIn(correctedWithoutDeclared)) {
                    declaredRegex.replace(correctedWithoutDeclared) { " ($newProduced)" }
                } else {
                    "$correctedWithoutDeclared ($newProduced)"
                }

                outList.add(
                    CorrectionSuggestion(
                        title = when (language) {
                            ValidationLanguage.ES -> "Ajustar repetición a $targetTimes veces (* $targetTimes)"
                            ValidationLanguage.EN -> "Adjust repeat multiplier to $targetTimes times (* $targetTimes)"
                        },
                        explanation = when (language) {
                            ValidationLanguage.ES ->
                                "Cada bloque de repetición consume $unitConsumed puntos base. Repitiendo $targetTimes veces se consumen exactamente los $expectedBase puntos de la vuelta previa."
                            ValidationLanguage.EN ->
                                "Each repeat block consumes $unitConsumed base stitches. Repeating $targetTimes times works exactly the $expectedBase stitches of the previous round."
                        },
                        actionType = SuggestionActionType.ADJUST_MULTIPLIER,
                        originalText = rawLine,
                        correctedText = finalCorrected
                    )
                )
            }
        }

        // Heurística D: Añadir puntos de compensación al final si faltaron puntos base
        val delta = totalConsumed - expectedBase
        if (delta < 0) {
            val missing = -delta
            val stitchWord = if (language == ValidationLanguage.EN) "sc" else "pb"
            val newTotalProduced = totalProduced + missing

            // Remover el conteo declarado actual y anexar los puntos faltantes
            val lineWithoutDeclared = if (declaredRegex.containsMatchIn(rawLine)) {
                declaredRegex.replace(rawLine, "").trimEnd()
            } else {
                rawLine.trimEnd()
            }

            val corrected = "$lineWithoutDeclared, $missing $stitchWord ($newTotalProduced)"

            outList.add(
                CorrectionSuggestion(
                    title = when (language) {
                        ValidationLanguage.ES -> "Añadir $missing $stitchWord adicionales al final"
                        ValidationLanguage.EN -> "Add $missing $stitchWord at the end"
                    },
                    explanation = when (language) {
                        ValidationLanguage.ES ->
                            "Faltaron $missing puntos base por tejer de la vuelta anterior. Añadir $missing $stitchWord al final completa la vuelta ($expectedBase pts base consumidos)."
                        ValidationLanguage.EN ->
                            "Missing $missing base stitches from the previous round. Adding $missing $stitchWord at the end completes the round ($expectedBase base sts consumed)."
                    },
                    actionType = SuggestionActionType.APPEND_STITCHES,
                    originalText = rawLine,
                    correctedText = corrected
                )
            )
        }
    }
}
