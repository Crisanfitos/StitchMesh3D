package com.crisanfitos.stitchmesh3d.core.engine.parser

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.engine.model.TopologyFlag

/**
 * Representa una instancia atómica individual de puntada dentro de una vuelta aplanada.
 *
 * @property stitchType Tipo de puntada y su 6-tupla formal asociada.
 * @property indexInRound Índice ordinal base cero de la puntada en la vuelta.
 */
data class StitchInstance(
    val stitchType: StitchType,
    val indexInRound: Int
) {
    val consumedStitches: Int get() = stitchType.consumedStitches
    val producedStitches: Int get() = stitchType.producedStitches
    val deltaStitches: Int get() = stitchType.deltaStitches
    val hRel: Double get() = stitchType.hRel
    val wRel: Double get() = stitchType.wRel
    val deltaRRel: Double get() = stitchType.deltaRRel
    val normalDisplacement: Double get() = stitchType.normalDisplacement
    val topologyFlags: Set<TopologyFlag> get() = stitchType.topologyFlags
}
