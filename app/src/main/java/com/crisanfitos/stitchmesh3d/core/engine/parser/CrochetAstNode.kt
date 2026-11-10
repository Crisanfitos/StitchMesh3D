package com.crisanfitos.stitchmesh3d.core.engine.parser

import com.crisanfitos.stitchmesh3d.core.engine.lexer.CrochetToken
import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType

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
         * para obtener la secuencia lineal exacta de todas las puntadas elementales de la vuelta,
         * preservando la asignación de color individual (Jacquard / Tapestry Crochet).
         */
        fun flatten(defaultColorHex: String? = null): List<StitchInstance> {
            val result = mutableListOf<StitchInstance>()
            var currentIndex = 0

            fun expand(node: CrochetAstNode, inheritedColor: String?) {
                when (node) {
                    is StitchNode -> {
                        val color = node.colorHex ?: inheritedColor ?: defaultColorHex
                        repeat(node.count) {
                            result.add(
                                StitchInstance(
                                    stitchType = node.stitchType,
                                    indexInRound = currentIndex++,
                                    colorHex = color
                                )
                            )
                        }
                    }
                    is RepeatNode -> {
                        val color = node.colorHex ?: inheritedColor
                        repeat(node.times) {
                            for (child in node.children) {
                                expand(child, color)
                            }
                        }
                    }
                    is RoundNode -> {
                        for (child in node.children) {
                            expand(child, inheritedColor)
                        }
                    }
                }
            }

            for (child in children) {
                expand(child, null)
            }

            return result
        }

        /** Total de puntos base consumidos de la vuelta previa (Σ C_i) */
        val totalConsumedStitches: Int
            get() = flatten().sumOf { it.consumedStitches }

        /** Total de puntos nuevos producidos en esta vuelta (Σ P_i) */
        val totalProducedStitches: Int
            get() = flatten().sumOf { it.producedStitches }

        /**
         * Puntos de trabajo efectivos producidos en la fila, excluyendo cadenetas de subida
         * (turning chains) iniciales o finales en labores planas ([PartTopologyType.FLAT_PANEL]).
         */
        fun getEffectiveProducedStitches(topologyType: PartTopologyType = PartTopologyType.CLOSED_FILLED): Int {
            if (topologyType != PartTopologyType.FLAT_PANEL) {
                return totalProducedStitches
            }
            val all = flatten()
            val hasWorkingStitches = all.any { it.stitchType !is StitchType.Chain }
            if (!hasWorkingStitches) {
                return totalProducedStitches
            }
            var startIdx = 0
            while (startIdx < all.size && all[startIdx].stitchType is StitchType.Chain && startIdx < 3) {
                startIdx++
            }
            var endIdx = all.size - 1
            var trailingChains = 0
            while (endIdx >= startIdx && all[endIdx].stitchType is StitchType.Chain && trailingChains < 3) {
                endIdx--
                trailingChains++
            }
            val workingStitches = if (startIdx <= endIdx) all.subList(startIdx, endIdx + 1) else emptyList()
            return workingStitches.sumOf { it.producedStitches }
        }

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
        val count: Int = 1,
        val colorHex: String? = null
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
        val times: Int,
        val colorHex: String? = null
    ) : CrochetAstNode {
        init {
            require(times > 0) { "El multiplicador de repetición debe ser > 0: $times" }
        }
    }
}
