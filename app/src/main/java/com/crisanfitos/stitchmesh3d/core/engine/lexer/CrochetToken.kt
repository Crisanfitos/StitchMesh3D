package com.crisanfitos.stitchmesh3d.core.engine.lexer

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.engine.model.TopologyFlag

/**
 * Representación léxica de los elementos de una instrucción de crochet según TRD §4.
 */
sealed interface CrochetToken {
    val startIndex: Int
    val endIndex: Int

    /** Prefijo de vuelta: V1:, R1:, Vuelta 3-5:, Round 4-8:, etc. */
    data class RoundHeader(
        val startRound: Int,
        val endRound: Int,
        val raw: String,
        override val startIndex: Int,
        override val endIndex: Int
    ) : CrochetToken {
        val isRange: Boolean get() = startRound != endRound
    }

    /** Cantidad numérica que precede a una puntada o multiplicador (ej. "3", "6") */
    data class Number(
        val value: Int,
        override val startIndex: Int,
        override val endIndex: Int
    ) : CrochetToken

    /** Puntada elemental, modificada o volumétrica reconocida por el catálogo */
    data class StitchToken(
        val stitchType: StitchType,
        val rawSymbol: String,
        override val startIndex: Int,
        override val endIndex: Int
    ) : CrochetToken

    /** Modificador topológico de bucle (BLO, FLO) */
    data class ModifierToken(
        val flag: TopologyFlag,
        val raw: String,
        override val startIndex: Int,
        override val endIndex: Int
    ) : CrochetToken

    /** Delimitador de apertura de agrupación: '[' o '(' */
    data class BracketOpen(
        val isSquare: Boolean,
        override val startIndex: Int,
        override val endIndex: Int
    ) : CrochetToken

    /** Delimitador de cierre de agrupación: ']' o ')' */
    data class BracketClose(
        val isSquare: Boolean,
        override val startIndex: Int,
        override val endIndex: Int
    ) : CrochetToken

    /** Operador de repetición o multiplicación ('*', 'x') */
    data class Multiply(
        override val startIndex: Int,
        override val endIndex: Int
    ) : CrochetToken

    /** Separador de coma (',') */
    data class Comma(
        override val startIndex: Int,
        override val endIndex: Int
    ) : CrochetToken

    /** Recuento total declarado al final de la vuelta (ej. "(12)", "( 18 pts )") */
    data class DeclaredCount(
        val count: Int,
        override val startIndex: Int,
        override val endIndex: Int
    ) : CrochetToken

    /** Token léxico no reconocido */
    data class Unknown(
        val raw: String,
        override val startIndex: Int,
        override val endIndex: Int
    ) : CrochetToken
}
