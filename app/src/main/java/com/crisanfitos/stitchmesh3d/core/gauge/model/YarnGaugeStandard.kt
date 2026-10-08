package com.crisanfitos.stitchmesh3d.core.gauge.model

/**
 * Estándar métrico de tensión dimensional de hilado y aguja.
 * Modela la fila de la tabla maestra `yarn_gauge_standards` y los coeficientes métricos
 * para calcular las dimensiones reales en milímetros de cada punto base de amigurumi.
 *
 * @property id Identificador único del estándar o semilla.
 * @property yarnWeightCategory Categoría CYC asociada (0 a 7).
 * @property categoryName Nombre legible de la categoría.
 * @property hookSizeMm Calibre nominal de la aguja en milímetros.
 * @property stitchWidthMm Ancho base del punto bajo (pb / sc) en milímetros (w_stitch).
 * @property stitchHeightMm Alto base de la hilera/vuelta en milímetros (h_stitch).
 * @property stitchThicknessMm Grosor de pared / relieve en milímetros (t_stitch).
 */
data class YarnGaugeStandard(
    val id: Int,
    val yarnWeightCategory: YarnWeightCategory,
    val categoryName: String,
    val hookSizeMm: Float,
    val stitchWidthMm: Float,
    val stitchHeightMm: Float,
    val stitchThicknessMm: Float
) {
    init {
        require(hookSizeMm > 0f) { "hookSizeMm debe ser mayor que 0: $hookSizeMm" }
        require(stitchWidthMm > 0f) { "stitchWidthMm debe ser mayor que 0: $stitchWidthMm" }
        require(stitchHeightMm > 0f) { "stitchHeightMm debe ser mayor que 0: $stitchHeightMm" }
        require(stitchThicknessMm > 0f) { "stitchThicknessMm debe ser mayor que 0: $stitchThicknessMm" }
    }

    /**
     * Relación de aspecto geométrica del punto (w / h).
     * Típicamente cercana a 1.0 - 1.1 en punto bajo denso para amigurumi.
     */
    val aspectRatio: Float
        get() = stitchWidthMm / stitchHeightMm

    /**
     * Factor de escala volumétrica unitaria aproximada.
     */
    val unitVolumeMm3: Float
        get() = stitchWidthMm * stitchHeightMm * stitchThicknessMm
}
