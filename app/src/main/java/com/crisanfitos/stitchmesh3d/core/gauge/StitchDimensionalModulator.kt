package com.crisanfitos.stitchmesh3d.core.gauge

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchDefinition
import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.gauge.model.RoundDimensions
import com.crisanfitos.stitchmesh3d.core.gauge.model.StitchDimensions
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard

/**
 * Modulador dimensional de puntadas de StitchMesh 3D.
 * Convierte factores relativos de la matriz maestra de puntadas (6-tupla) en medidas métricas
 * reales en milímetros (ancho, alto, offset radial, extrusión normal) sobre el estándar de tensión.
 *
 * Implementa el requisito RF-2.2 y especificaciones TRD §2.2 / §2.3.
 */
object StitchDimensionalModulator {

    /**
     * Calcula las dimensiones métricas reales en mm de una puntada a partir de su definición y estándar de tensión.
     *
     * - Ancho (w): w_rel * w_stitch
     * - Alto (h): h_rel * h_stitch
     * - Desplazamiento radial (Δr): delta_r_rel * t_stitch
     * - Extrusión normal (Δn): normal_disp * t_stitch
     */
    fun calculate(definition: StitchDefinition, standard: YarnGaugeStandard): StitchDimensions {
        val width = definition.wRel * standard.stitchWidthMm
        val height = definition.hRel * standard.stitchHeightMm
        val radial = definition.deltaRRel * standard.stitchThicknessMm
        val normal = definition.normalDisplacement * standard.stitchThicknessMm

        return StitchDimensions(
            widthMm = width,
            heightMm = height,
            radialOffsetMm = radial,
            normalDisplacementMm = normal
        )
    }

    /**
     * Calcula las dimensiones métricas de un StitchType.
     */
    fun calculate(stitch: StitchType, standard: YarnGaugeStandard): StitchDimensions {
        return calculate(stitch.definition, standard)
    }

    /**
     * Calcula las dimensiones globales agregadas para una lista de puntadas que componen una vuelta.
     */
    fun calculateRoundDimensions(stitches: List<StitchType>, standard: YarnGaugeStandard): RoundDimensions {
        if (stitches.isEmpty()) {
            return RoundDimensions(
                totalCircumferenceMm = 0.0,
                meanHeightMm = 0.0,
                maxHeightMm = 0.0
            )
        }

        val dimensions = stitches.map { calculate(it, standard) }
        val totalCircumference = dimensions.sumOf { it.widthMm }
        val meanHeight = dimensions.map { it.heightMm }.average()
        val maxHeight = dimensions.maxOf { it.heightMm }

        return RoundDimensions(
            totalCircumferenceMm = totalCircumference,
            meanHeightMm = meanHeight,
            maxHeightMm = maxHeight
        )
    }
}
