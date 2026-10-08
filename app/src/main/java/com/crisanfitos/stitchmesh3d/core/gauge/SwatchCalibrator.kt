package com.crisanfitos.stitchmesh3d.core.gauge

import com.crisanfitos.stitchmesh3d.core.gauge.model.TensionCalibrationResult
import com.crisanfitos.stitchmesh3d.core.gauge.model.TensionSwatchInput
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import kotlin.math.abs

/**
 * Calibrador manual de tensión con muestra de tejido (RF-2.3).
 * Convierte mediciones físicas de muestras (ej. 10x10 cm o N puntos x M vueltas)
 * en milímetros reales por punto ($w_{stitch}$ y $h_{stitch}$), recalculando el grosor y
 * detectando desviaciones de tensión respecto a la matriz estándar CYC.
 */
object SwatchCalibrator {

    const val MIN_STITCH_MM = 0.5f
    const val MAX_STITCH_MM = 30.0f
    const val STANDARD_TOLERANCE_PERCENT = 15.0f

    /**
     * Calibra la tensión a partir de un objeto [TensionSwatchInput].
     *
     * Fórmula:
     * - w_stitch (mm) = (measuredWidthCm * 10 mm/cm) / stitchesCount
     * - h_stitch (mm) = (measuredHeightCm * 10 mm/cm) / roundsCount
     */
    fun calibrate(
        input: TensionSwatchInput,
        baseStandard: YarnGaugeStandard = YarnGaugeRegistry.DEFAULT_STANDARD
    ): Result<TensionCalibrationResult> {
        val stitchWidthMm = (input.measuredWidthCm * 10f) / input.stitchesCount.toFloat()
        val stitchHeightMm = (input.measuredHeightCm * 10f) / input.roundsCount.toFloat()

        if (stitchWidthMm < MIN_STITCH_MM || stitchWidthMm > MAX_STITCH_MM) {
            return Result.failure(
                IllegalArgumentException("El ancho de punto calculado ($stitchWidthMm mm) está fuera del rango válido [$MIN_STITCH_MM, $MAX_STITCH_MM] mm.")
            )
        }

        if (stitchHeightMm < MIN_STITCH_MM || stitchHeightMm > MAX_STITCH_MM) {
            return Result.failure(
                IllegalArgumentException("El alto de punto calculado ($stitchHeightMm mm) está fuera del rango válido [$MIN_STITCH_MM, $MAX_STITCH_MM] mm.")
            )
        }

        // El grosor de pared escala proporcionalmente a la relación de ancho
        val thicknessScale = stitchWidthMm / baseStandard.stitchWidthMm
        val stitchThicknessMm = baseStandard.stitchThicknessMm * thicknessScale

        val customStandard = baseStandard.copy(
            id = -1,
            categoryName = "${baseStandard.categoryName} (Muestra Calibrada)",
            stitchWidthMm = stitchWidthMm,
            stitchHeightMm = stitchHeightMm,
            stitchThicknessMm = stitchThicknessMm
        )

        val widthDev = ((stitchWidthMm - baseStandard.stitchWidthMm) / baseStandard.stitchWidthMm) * 100f
        val heightDev = ((stitchHeightMm - baseStandard.stitchHeightMm) / baseStandard.stitchHeightMm) * 100f
        val isWithinTolerance = abs(widthDev) <= STANDARD_TOLERANCE_PERCENT && abs(heightDev) <= STANDARD_TOLERANCE_PERCENT

        return Result.success(
            TensionCalibrationResult(
                customStandard = customStandard,
                widthDeviationPercent = widthDev,
                heightDeviationPercent = heightDev,
                isWithinStandardTolerance = isWithinTolerance
            )
        )
    }

    /**
     * Calibra la tensión a partir de una muestra clásica de 10x10 cm,
     * donde el usuario cuenta cuántos puntos y vueltas caben en 10 cm.
     */
    fun calibrateFrom10x10Cm(
        stitchesIn10Cm: Float,
        roundsIn10Cm: Float,
        baseStandard: YarnGaugeStandard = YarnGaugeRegistry.DEFAULT_STANDARD
    ): Result<TensionCalibrationResult> {
        if (stitchesIn10Cm <= 0f || roundsIn10Cm <= 0f) {
            return Result.failure(IllegalArgumentException("El número de puntos y vueltas debe ser mayor que 0"))
        }

        val stitchWidthMm = 100f / stitchesIn10Cm
        val stitchHeightMm = 100f / roundsIn10Cm

        return calibrate(
            input = TensionSwatchInput(
                measuredWidthCm = 10f,
                measuredHeightCm = 10f,
                stitchesCount = stitchesIn10Cm.toInt().coerceAtLeast(1),
                roundsCount = roundsIn10Cm.toInt().coerceAtLeast(1)
            ),
            baseStandard = baseStandard
        )
    }
}
