package com.crisanfitos.stitchmesh3d.ui.tension

import com.crisanfitos.stitchmesh3d.core.gauge.YarnGaugeRegistry
import com.crisanfitos.stitchmesh3d.core.gauge.model.TensionCalibrationResult
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import com.crisanfitos.stitchmesh3d.core.mvi.ViewEffect
import com.crisanfitos.stitchmesh3d.core.mvi.ViewIntent
import com.crisanfitos.stitchmesh3d.core.mvi.ViewState

/**
 * Estado inmutable de la pantalla de Calibración de Tensión (Muestra 10x10 cm).
 */
data class TensionState(
    val selectedStandard: YarnGaugeStandard = YarnGaugeRegistry.DEFAULT_STANDARD,
    val availableStandards: List<YarnGaugeStandard> = YarnGaugeRegistry.getAllStandards(),
    val stitchesIn10Cm: Int = 16,
    val roundsIn10Cm: Int = 20,
    val calibrationResult: TensionCalibrationResult? = null,
    val errorMessage: String? = null,
    val isSaved: Boolean = false
) : ViewState

/**
 * Intenciones de usuario para la interacción con la muestra de tensión.
 */
sealed interface TensionIntent : ViewIntent {
    data class SelectStandard(val standard: YarnGaugeStandard) : TensionIntent
    data object IncrementStitches : TensionIntent
    data object DecrementStitches : TensionIntent
    data class SetStitches(val stitches: Int) : TensionIntent
    data object IncrementRounds : TensionIntent
    data object DecrementRounds : TensionIntent
    data class SetRounds(val rounds: Int) : TensionIntent
    data object ResetToStandard : TensionIntent
    data object ApplyCalibration : TensionIntent
}

/**
 * Efectos de navegación y notificaciones efímeras para la pantalla de tensión.
 */
sealed interface TensionEffect : ViewEffect {
    data class ShowSnackbar(val message: String) : TensionEffect
    data object NavigateBack : TensionEffect
}
