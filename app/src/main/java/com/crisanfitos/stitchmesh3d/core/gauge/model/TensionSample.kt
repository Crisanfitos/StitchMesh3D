package com.crisanfitos.stitchmesh3d.core.gauge.model

/**
 * Entrada de datos para la calibración manual de muestra de tensión de ganchillo.
 * Permite introducir tanto las dimensiones de un cuadro de muestra tejido con número fijo de puntos/vueltas
 * (ej. 10x10 puntos) o la medición estándar en una ventana de 10x10 cm.
 *
 * @property measuredWidthCm Ancho medido de la muestra en centímetros.
 * @property measuredHeightCm Alto medido de la muestra en centímetros.
 * @property stitchesCount Cantidad de puntos en el ancho medido (por defecto 10).
 * @property roundsCount Cantidad de vueltas en el alto medido (por defecto 10).
 */
data class TensionSwatchInput(
    val measuredWidthCm: Float,
    val measuredHeightCm: Float,
    val stitchesCount: Int = 10,
    val roundsCount: Int = 10
) {
    init {
        require(measuredWidthCm > 0f) { "measuredWidthCm debe ser mayor que 0: $measuredWidthCm" }
        require(measuredHeightCm > 0f) { "measuredHeightCm debe ser mayor que 0: $measuredHeightCm" }
        require(stitchesCount > 0) { "stitchesCount debe ser mayor que 0: $stitchesCount" }
        require(roundsCount > 0) { "roundsCount debe ser mayor que 0: $roundsCount" }
    }
}

/**
 * Resultado de calibración de tensión paramétrica.
 *
 * @property customStandard Estándar de tensión personalizado recalculado con las medidas reales.
 * @property widthDeviationPercent Desviación porcentual del ancho respecto al estándar teórico CYC.
 * @property heightDeviationPercent Desviación porcentual del alto respecto al estándar teórico CYC.
 * @property isWithinStandardTolerance Indica si la tensión del tejedor se encuentra dentro de una tolerancia razonable (+-15%).
 */
data class TensionCalibrationResult(
    val customStandard: YarnGaugeStandard,
    val widthDeviationPercent: Float,
    val heightDeviationPercent: Float,
    val isWithinStandardTolerance: Boolean
)
