package com.crisanfitos.stitchmesh3d.core.engine.lexer

/**
 * Resultado completo del análisis léxico de una línea de instrucción de vuelta.
 *
 * @property roundHeader Información del encabezado de vuelta (número o rango) si está presente.
 * @property tokens Secuencia lineal de tokens identificados en el cuerpo de la instrucción.
 * @property declaredCount Recuento total de puntos declarado al final de la línea si existe.
 * @property rawLine Texto original de la línea analizada.
 */
data class LexerResult(
    val roundHeader: CrochetToken.RoundHeader?,
    val tokens: List<CrochetToken>,
    val declaredCount: Int?,
    val rawLine: String
) {
    /** True si hay algún token no reconocido en la instrucción */
    val hasUnknownTokens: Boolean
        get() = tokens.any { it is CrochetToken.Unknown }

    /** Lista de tokens que no pudieron ser clasificados */
    val unknownTokens: List<CrochetToken.Unknown>
        get() = tokens.filterIsInstance<CrochetToken.Unknown>()
}
