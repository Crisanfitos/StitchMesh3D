package com.crisanfitos.stitchmesh3d.core.engine.validator

import com.crisanfitos.stitchmesh3d.core.engine.parser.CrochetAstNode
import com.crisanfitos.stitchmesh3d.core.engine.parser.ParseError

/**
 * Idioma utilizado para la generación de mensajes diagnósticos de validación.
 */
enum class ValidationLanguage {
    ES,
    EN
}

/**
 * Representa el resultado formal de la validación matemática de una vuelta según RF-1.2 y RF-1.3.
 */
sealed interface RoundValidationResult {
    /** Indica si la vuelta cumple estrictamente con todos los invariantes formales. */
    val isValid: Boolean

    /** Número ordinal de la vuelta analizada si fue detectado en la cabecera. */
    val roundNumber: Int?

    /** Texto original no modificado de la instrucción de la vuelta. */
    val rawLine: String

    /** Árbol de sintaxis abstracta de la vuelta si el parsing sintáctico fue exitoso. */
    val ast: CrochetAstNode.RoundNode?

    /** Puntos base totales consumidos por las puntadas de esta vuelta (Σ C_i). */
    val totalConsumed: Int

    /** Puntos nuevos totales producidos por las puntadas de esta vuelta (Σ P_i). */
    val totalProduced: Int

    /** Recuento de puntos declarado entre paréntesis o corchetes al final de la línea si existe. */
    val declaredCount: Int?

    /** Mensaje de diagnóstico explicativo o consolidado. */
    val message: String

    /** Indica si hubo discrepancia en el Invariante 1 (Base Consumida vs Vuelta Previa). */
    val hasBaseMismatch: Boolean get() = this is BaseMismatch || this is MultipleErrors

    /** Indica si hubo discrepancia en el Invariante 2 (Puntos Producidos vs Total Declarado). */
    val hasClosureMismatch: Boolean get() = this is ClosureMismatch || this is MultipleErrors

    /** Indica si ocurrió un error de sintaxis durante el parsing. */
    val hasSyntaxError: Boolean get() = this is SyntaxError

    /**
     * Vuelta matemáticamente válida que satisface el Invariante de Base (RF-1.2)
     * y el Invariante de Cierre (RF-1.3).
     */
    data class Valid(
        override val roundNumber: Int?,
        override val rawLine: String,
        override val ast: CrochetAstNode.RoundNode,
        override val totalConsumed: Int,
        override val totalProduced: Int,
        override val declaredCount: Int?
    ) : RoundValidationResult {
        override val isValid: Boolean get() = true
        override val message: String = "Vuelta válida (Consume: $totalConsumed, Produce: $totalProduced)"
    }

    /**
     * Error sintáctico durante la fase de análisis léxico o sintáctico previo a la validación aritmética.
     */
    data class SyntaxError(
        override val roundNumber: Int?,
        override val rawLine: String,
        val parseErrors: List<ParseError>
    ) : RoundValidationResult {
        override val isValid: Boolean get() = false
        override val ast: CrochetAstNode.RoundNode? get() = null
        override val totalConsumed: Int get() = 0
        override val totalProduced: Int get() = 0
        override val declaredCount: Int? get() = null
        override val message: String = parseErrors.joinToString("; ") { it.message }
    }

    /**
     * Infracción del Invariante 1 (RF-1.2):
     * La sumatoria de puntos base consumidos (Σ C_i) difiere de los puntos disponibles de la vuelta previa (S_{k-1}).
     *
     * @property expectedBaseStitches Puntos disponibles en la vuelta anterior (S_{k-1}).
     * @property difference Diferencia aritmética (totalConsumed - expectedBaseStitches). Negativo si faltaron puntos; positivo si sobraron.
     */
    data class BaseMismatch(
        override val roundNumber: Int?,
        override val rawLine: String,
        override val ast: CrochetAstNode.RoundNode,
        override val totalConsumed: Int,
        val expectedBaseStitches: Int,
        override val totalProduced: Int,
        override val declaredCount: Int?,
        override val message: String
    ) : RoundValidationResult {
        override val isValid: Boolean get() = false
        val difference: Int get() = totalConsumed - expectedBaseStitches
    }

    /**
     * Infracción del Invariante 2 (RF-1.3):
     * La sumatoria de puntos nuevos producidos (Σ P_i) difiere del recuento declarado explícitamente entre paréntesis.
     *
     * @property declaredCount Recuento numérico declarado al final de la línea.
     * @property difference Diferencia aritmética (totalProduced - declaredCount). Negativo si produjo menos; positivo si produjo de más.
     */
    data class ClosureMismatch(
        override val roundNumber: Int?,
        override val rawLine: String,
        override val ast: CrochetAstNode.RoundNode,
        override val totalConsumed: Int,
        override val totalProduced: Int,
        override val declaredCount: Int,
        override val message: String
    ) : RoundValidationResult {
        override val isValid: Boolean get() = false
        val difference: Int get() = totalProduced - declaredCount
    }

    /**
     * Infracción simultánea del Invariante 1 (Base) y del Invariante 2 (Cierre).
     */
    data class MultipleErrors(
        override val roundNumber: Int?,
        override val rawLine: String,
        override val ast: CrochetAstNode.RoundNode,
        override val totalConsumed: Int,
        val expectedBaseStitches: Int,
        override val totalProduced: Int,
        override val declaredCount: Int,
        val baseMessage: String,
        val closureMessage: String,
        override val message: String = "$baseMessage | $closureMessage"
    ) : RoundValidationResult {
        override val isValid: Boolean get() = false
        val baseDifference: Int get() = totalConsumed - expectedBaseStitches
        val closureDifference: Int get() = totalProduced - declaredCount
    }
}
