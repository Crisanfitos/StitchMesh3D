package com.crisanfitos.stitchmesh3d.core.engine.model

/**
 * 6-tupla formal de puntada: T_i = (C_i, P_i, h_rel, w_rel, Δr_rel, TopologyFlags)
 * Definida formalmente en TRD §2.1.
 *
 * @property consumedStitches C_i: Puntos base consumidos de la vuelta previa (C_i >= 0).
 * @property producedStitches P_i: Puntos nuevos producidos en la vuelta activa (P_i >= 0).
 * @property hRel h_rel: Factor multiplicador sobre la altura base calibrada (h_stitch > 0).
 * @property wRel w_rel: Factor multiplicador sobre el ancho base calibrado (w_stitch > 0).
 * @property deltaRRel Δr_rel: Desplazamiento radial/normal relativo respecto a la superficie.
 * @property topologyFlags Modificadores topológicos y de inserción de la hebra.
 */
data class StitchDefinition(
    val consumedStitches: Int,
    val producedStitches: Int,
    val hRel: Double,
    val wRel: Double,
    val deltaRRel: Double = 0.0,
    val normalDisplacement: Double = 0.0,
    val topologyFlags: Set<TopologyFlag> = setOf(TopologyFlag.NORMAL)
) {
    init {
        require(consumedStitches >= 0) { "consumedStitches (C) no puede ser negativo: $consumedStitches" }
        require(producedStitches >= 0) { "producedStitches (P) no puede ser negativo: $producedStitches" }
        require(hRel >= 0.0) { "hRel no puede ser negativo: $hRel" }
        require(wRel >= 0.0) { "wRel no puede ser negativo: $wRel" }
    }

    /**
     * Delta neto de puntadas producidas menos consumidas: Δ = P - C.
     * Representa la expansión (>0), contracción (<0) o mantenimiento neutro (=0) del perímetro.
     */
    val deltaStitches: Int
        get() = producedStitches - consumedStitches
}
