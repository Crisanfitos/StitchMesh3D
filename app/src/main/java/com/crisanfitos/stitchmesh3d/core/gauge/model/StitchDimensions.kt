package com.crisanfitos.stitchmesh3d.core.gauge.model

/**
 * Dimensiones métricas calculadas en milímetros para una puntada de crochet específica.
 * Derivadas formalmente de la 6-tupla multiplicada por el estándar de tensión.
 *
 * @property widthMm Ancho circunferencial ocupado en la vuelta en milímetros.
 * @property heightMm Altura vertical aportada a la vuelta en milímetros.
 * @property radialOffsetMm Desplazamiento radial respecto a la superficie de referencia.
 * @property normalDisplacementMm Extrusión sobre el vector normal local (volumetría 3D).
 */
data class StitchDimensions(
    val widthMm: Double,
    val heightMm: Double,
    val radialOffsetMm: Double = 0.0,
    val normalDisplacementMm: Double = 0.0
) {
    init {
        require(widthMm >= 0.0) { "widthMm no puede ser negativo: $widthMm" }
        require(heightMm >= 0.0) { "heightMm no puede ser negativo: $heightMm" }
    }
}

/**
 * Resumen dimensional agregado para una vuelta completa de patrón.
 *
 * @property totalCircumferenceMm Perímetro circunferencial total de la vuelta en milímetros.
 * @property meanHeightMm Altura vertical promedio aportada por la vuelta en milímetros.
 * @property maxHeightMm Altura máxima detectada en la vuelta (útil para transiciones asimétricas).
 */
data class RoundDimensions(
    val totalCircumferenceMm: Double,
    val meanHeightMm: Double,
    val maxHeightMm: Double
)
