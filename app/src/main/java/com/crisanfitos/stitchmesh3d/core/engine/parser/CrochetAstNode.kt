package com.crisanfitos.stitchmesh3d.core.engine.parser

import com.crisanfitos.stitchmesh3d.core.engine.lexer.CrochetToken
import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType

/**
 * Nodos del Árbol de Sintaxis Abstracta (AST) para instrucciones de crochet.
 */
sealed interface CrochetAstNode {

    /**
     * Nodo raíz que representa una vuelta completa analizada.
     */
    data class RoundNode(
        val header: CrochetToken.RoundHeader?,
        val children: List<CrochetAstNode>,
        val declaredCount: Int?,
        val rawLine: String
    ) : CrochetAstNode {

        /**
         * Aplana recursivamente el AST (expandiendo repeticiones y multiplicadores)
         * para obtener la secuencia lineal exacta de todas las puntadas elementales de la vuelta.
         */
        fun flatten(): List<StitchInstance> {
            val result = mutableListOf<StitchInstance>()
            var currentIndex = 0

            fun expand(node: CrochetAstNode) {
                when (node) {
                    is StitchNode -> {
                        repeat(node.count) {
                            result.add(StitchInstance(node.stitchType, currentIndex++))
                        }
                    }
                    is RepeatNode -> {
                        repeat(node.times) {
                            for (child in node.children) {
                                expand(child)
                            }
                        }
                    }
                    is RoundNode -> {
                        for (child in node.children) {
                            expand(child)
                        }
                    }
                }
            }

            for (child in children) {
                expand(child)
            }

            return result
        }

        /** Total de puntos base consumidos de la vuelta previa (Σ C_i) */
        val totalConsumedStitches: Int
            get() = flatten().sumOf { it.consumedStitches }

        /** Total de puntos nuevos producidos en esta vuelta (Σ P_i) */
        val totalProducedStitches: Int
            get() = flatten().sumOf { it.producedStitches }

        /** Variación neta de puntos en la vuelta (Σ P - Σ C) */
        val deltaStitches: Int
            get() = totalProducedStitches - totalConsumedStitches
    }

    /**
     * Representa una invocación atómica o repetida consecutivamente de una puntada.
     * Ejemplo: "3 pb" -> StitchNode(SingleCrochet, 3)
     */
    data class StitchNode(
        val stitchType: StitchType,
        val count: Int = 1
    ) : CrochetAstNode {
        init {
            require(count > 0) { "El contador de puntada debe ser > 0: $count" }
        }
    }

    /**
     * Representa un bloque repetido N veces.
     * Ejemplo: "[1 pb, 1 aum] * 6" -> RepeatNode(children=[StitchNode(pb, 1), StitchNode(aum, 1)], times=6)
     */
    data class RepeatNode(
        val children: List<CrochetAstNode>,
        val times: Int
    ) : CrochetAstNode {
        init {
            require(times > 0) { "El multiplicador de repetición debe ser > 0: $times" }
        }
    }
}
