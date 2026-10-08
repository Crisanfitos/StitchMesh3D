package com.crisanfitos.stitchmesh3d.core.engine.validator

/**
 * Clasificación de la acción correctiva propuesta según RF-1.5.
 */
enum class SuggestionActionType {
    /** Actualizar el recuento declarado entre paréntesis para reflejar la producción real calculada. */
    UPDATE_DECLARED_COUNT,

    /** Modificar el multiplicador de repetición (* N) para que consuma exactamente los puntos base disponibles. */
    ADJUST_MULTIPLIER,

    /** Añadir puntos adicionales al final de la vuelta para completar la base requerida. */
    APPEND_STITCHES,

    /** Reemplazar la instrucción completa (ej. usar Anillo Mágico en V1 o igualar vuelta completa). */
    REPLACE_INSTRUCTION
}

/**
 * Representa una sugerencia inteligente de corrección matemática de patrones según RF-1.5.
 *
 * @property title Título conciso y descriptivo de la acción propuesta.
 * @property explanation Razón matemática detallada de la discrepancia y cómo la sugerencia la solventa.
 * @property actionType Tipo estructural de corrección sugerida.
 * @property originalText Texto original de la instrucción antes de la sugerencia.
 * @property correctedText Texto completo de la instrucción corregida, listo para sustituir directamente la línea.
 */
data class CorrectionSuggestion(
    val title: String,
    val explanation: String,
    val actionType: SuggestionActionType,
    val originalText: String,
    val correctedText: String
)
