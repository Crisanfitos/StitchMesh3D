package com.crisanfitos.stitchmesh3d.core.engine.parser

/**
 * Representa un error sintáctico encontrado durante el parsing del patrón.
 *
 * @property message Descripción legible del error sintáctico.
 * @property startIndex Posición inicial de carácter en el texto original.
 * @property endIndex Posición final de carácter en el texto original.
 */
data class ParseError(
    val message: String,
    val startIndex: Int,
    val endIndex: Int
)

/**
 * Resultado de la fase de parsing sintáctico.
 *
 * @property ast Árbol AST resultante de la vuelta si el parsing pudo completarse.
 * @property errors Lista de errores sintácticos detectados.
 */
data class ParseResult(
    val ast: CrochetAstNode.RoundNode?,
    val errors: List<ParseError>
) {
    val isSuccess: Boolean
        get() = errors.isEmpty() && ast != null
}
