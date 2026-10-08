package com.crisanfitos.stitchmesh3d.core.engine.validator

import java.util.UUID

/**
 * Clasificación del tipo de inconsistencia detectada en el árbol de validez del proyecto.
 */
enum class InconsistencyType {
    /** Invariante 1: Puntos base consumidos no coinciden con la vuelta anterior. */
    BASE_MISMATCH,

    /** Invariante 2: Puntos producidos no coinciden con el total declarado. */
    CLOSURE_MISMATCH,

    /** Fallo simultáneo de base y cierre. */
    MULTIPLE_ERRORS,

    /** Error léxico o sintáctico en la instrucción. */
    SYNTAX_ERROR
}

/**
 * Representa una inconsistencia concreta localizada en una vuelta y parte del proyecto.
 *
 * @property partId Identificador único de la pieza / parte.
 * @property partName Nombre legible de la pieza (ej. "Cabeza", "Cuerpo").
 * @property roundIndex Índice 1-based de la instrucción dentro de la parte.
 * @property roundNumber Número formal de la vuelta si fue especificado en el patrón (ej. 3 para "V3:").
 * @property rawLine Texto plano original de la instrucción.
 * @property type Clasificación de la inconsistencia.
 * @property message Mensaje explicativo detallado y localizado.
 * @property difference Magnitud de la discrepancia numérica si aplica.
 */
data class ProjectInconsistency(
    val partId: String,
    val partName: String,
    val roundIndex: Int,
    val roundNumber: Int?,
    val rawLine: String,
    val type: InconsistencyType,
    val message: String,
    val difference: Int? = null
)

/**
 * Definición de entrada para una parte o componente de crochet de un proyecto.
 */
data class ProjectPartDefinition(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val instructions: List<String>
)

/**
 * Definición de entrada para un proyecto amigurumi completo con múltiples partes.
 */
data class ProjectDefinition(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val parts: List<ProjectPartDefinition>
)

/**
 * Resultado detallado de validación de una parte individual del proyecto.
 */
data class PartValidationResult(
    val partId: String,
    val partName: String,
    val roundResults: List<RoundValidationResult>
) {
    /** Una parte es válida si contiene al menos una vuelta y el 100% de ellas son válidas. */
    val isValid: Boolean
        get() = roundResults.isNotEmpty() && roundResults.all { it.isValid }

    val totalRounds: Int
        get() = roundResults.size

    val validRoundsCount: Int
        get() = roundResults.count { it.isValid }

    val invalidRoundsCount: Int
        get() = roundResults.count { !it.isValid }

    /** Lista de inconsistencias registradas en esta parte. */
    val inconsistencies: List<ProjectInconsistency> = buildList {
        roundResults.forEachIndexed { index, roundResult ->
            if (!roundResult.isValid) {
                val type = when (roundResult) {
                    is RoundValidationResult.BaseMismatch -> InconsistencyType.BASE_MISMATCH
                    is RoundValidationResult.ClosureMismatch -> InconsistencyType.CLOSURE_MISMATCH
                    is RoundValidationResult.MultipleErrors -> InconsistencyType.MULTIPLE_ERRORS
                    is RoundValidationResult.SyntaxError -> InconsistencyType.SYNTAX_ERROR
                    is RoundValidationResult.Valid -> return@forEachIndexed
                }
                val diff = when (roundResult) {
                    is RoundValidationResult.BaseMismatch -> roundResult.difference
                    is RoundValidationResult.ClosureMismatch -> roundResult.difference
                    is RoundValidationResult.MultipleErrors -> roundResult.baseDifference
                    else -> null
                }
                add(
                    ProjectInconsistency(
                        partId = partId,
                        partName = partName,
                        roundIndex = index + 1,
                        roundNumber = roundResult.roundNumber,
                        rawLine = roundResult.rawLine,
                        type = type,
                        message = roundResult.message,
                        difference = diff
                    )
                )
            }
        }
    }
}

/**
 * Árbol formal de validez del proyecto completo según RF-1.4.
 *
 * "Un proyecto solo se marca como 'Válido' si el 100% de sus partes y el 100% de sus vueltas están libres de inconsistencias."
 */
data class ProjectValidationTree(
    val projectId: String,
    val projectName: String,
    val parts: List<PartValidationResult>
) {
    /**
     * Un proyecto es Válido si tiene al menos una parte y todas sus partes son 100% válidas.
     */
    val isValid: Boolean
        get() = parts.isNotEmpty() && parts.all { it.isValid }

    val totalParts: Int
        get() = parts.size

    val validPartsCount: Int
        get() = parts.count { it.isValid }

    val invalidPartsCount: Int
        get() = parts.count { !it.isValid }

    val totalRounds: Int
        get() = parts.sumOf { it.totalRounds }

    val validRounds: Int
        get() = parts.sumOf { it.validRoundsCount }

    val invalidRounds: Int
        get() = parts.sumOf { it.invalidRoundsCount }

    /**
     * Lista consolidada de todas las inconsistencias encontradas en cualquier parte del proyecto.
     */
    val allInconsistencies: List<ProjectInconsistency> = parts.flatMap { it.inconsistencies }
}
