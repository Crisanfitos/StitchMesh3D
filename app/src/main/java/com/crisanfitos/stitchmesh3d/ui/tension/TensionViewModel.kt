package com.crisanfitos.stitchmesh3d.ui.tension

import androidx.lifecycle.viewModelScope
import com.crisanfitos.stitchmesh3d.core.gauge.SwatchCalibrator
import com.crisanfitos.stitchmesh3d.core.gauge.YarnGaugeRegistry
import com.crisanfitos.stitchmesh3d.core.gauge.model.TensionCalibrationResult
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import com.crisanfitos.stitchmesh3d.core.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * ViewModel que gestiona la calibración manual de muestra de tensión 10x10 cm (RF-2.3).
 */
@HiltViewModel
class TensionViewModel @Inject constructor() :
    BaseViewModel<TensionState, TensionIntent, TensionEffect>(TensionState()) {

    companion object {
        const val MIN_STITCHES_COUNT = 3
        const val MAX_STITCHES_COUNT = 80
        const val MIN_ROUNDS_COUNT = 3
        const val MAX_ROUNDS_COUNT = 80
    }

    init {
        recalculate(
            stitches = state.value.stitchesIn10Cm,
            rounds = state.value.roundsIn10Cm,
            standard = state.value.selectedStandard
        )
    }

    override fun processIntent(intent: TensionIntent) {
        when (intent) {
            is TensionIntent.SelectStandard -> onStandardSelected(intent.standard)
            TensionIntent.IncrementStitches -> updateStitches(state.value.stitchesIn10Cm + 1)
            TensionIntent.DecrementStitches -> updateStitches(state.value.stitchesIn10Cm - 1)
            is TensionIntent.SetStitches -> updateStitches(intent.stitches)
            TensionIntent.IncrementRounds -> updateRounds(state.value.roundsIn10Cm + 1)
            TensionIntent.DecrementRounds -> updateRounds(state.value.roundsIn10Cm - 1)
            is TensionIntent.SetRounds -> updateRounds(intent.rounds)
            TensionIntent.ResetToStandard -> resetToStandard()
            TensionIntent.ApplyCalibration -> applyCalibration()
        }
    }

    private fun onStandardSelected(standard: YarnGaugeStandard) {
        val theoreticalStitches = (100f / standard.stitchWidthMm).roundToInt().coerceIn(MIN_STITCHES_COUNT, MAX_STITCHES_COUNT)
        val theoreticalRounds = (100f / standard.stitchHeightMm).roundToInt().coerceIn(MIN_ROUNDS_COUNT, MAX_ROUNDS_COUNT)

        setState {
            copy(
                selectedStandard = standard,
                stitchesIn10Cm = theoreticalStitches,
                roundsIn10Cm = theoreticalRounds
            )
        }
        recalculate(theoreticalStitches, theoreticalRounds, standard)
    }

    private fun updateStitches(newStitches: Int) {
        val clamped = newStitches.coerceIn(MIN_STITCHES_COUNT, MAX_STITCHES_COUNT)
        setState { copy(stitchesIn10Cm = clamped) }
        recalculate(clamped, state.value.roundsIn10Cm, state.value.selectedStandard)
    }

    private fun updateRounds(newRounds: Int) {
        val clamped = newRounds.coerceIn(MIN_ROUNDS_COUNT, MAX_ROUNDS_COUNT)
        setState { copy(roundsIn10Cm = clamped) }
        recalculate(state.value.stitchesIn10Cm, clamped, state.value.selectedStandard)
    }

    private fun resetToStandard() {
        val standard = YarnGaugeRegistry.DEFAULT_STANDARD
        val theoreticalStitches = (100f / standard.stitchWidthMm).roundToInt()
        val theoreticalRounds = (100f / standard.stitchHeightMm).roundToInt()

        setState {
            copy(
                selectedStandard = standard,
                stitchesIn10Cm = theoreticalStitches,
                roundsIn10Cm = theoreticalRounds,
                errorMessage = null
            )
        }
        recalculate(theoreticalStitches, theoreticalRounds, standard)
    }

    private fun recalculate(stitches: Int, rounds: Int, standard: YarnGaugeStandard) {
        val result = SwatchCalibrator.calibrateFrom10x10Cm(
            stitchesIn10Cm = stitches.toFloat(),
            roundsIn10Cm = rounds.toFloat(),
            baseStandard = standard
        )

        result.fold(
            onSuccess = { calibrationResult ->
                val scaleComparison = computeScaleComparison(standard, calibrationResult)
                setState {
                    copy(
                        calibrationResult = calibrationResult,
                        scaleComparison = scaleComparison,
                        errorMessage = null
                    )
                }
            },
            onFailure = { error ->
                setState {
                    copy(
                        calibrationResult = null,
                        scaleComparison = null,
                        errorMessage = error.localizedMessage ?: "Error al calibrar las medidas de la muestra"
                    )
                }
            }
        )
    }

    private fun computeScaleComparison(
        standard: YarnGaugeStandard,
        calibrationResult: TensionCalibrationResult
    ): TensionScaleComparison {
        val widthDev = calibrationResult.widthDeviationPercent
        val heightDev = calibrationResult.heightDeviationPercent
        val widthRatio = calibrationResult.customStandard.stitchWidthMm / standard.stitchWidthMm
        val heightRatio = calibrationResult.customStandard.stitchHeightMm / standard.stitchHeightMm
        val theoreticalVol = standard.unitVolumeMm3
        val calibratedVol = calibrationResult.customStandard.unitVolumeMm3
        val volumeDev = ((calibratedVol - theoreticalVol) / theoreticalVol) * 100f
        val volumeRatio = calibratedVol / theoreticalVol

        val diagnosis: TensionDiagnosis
        val diagnosisSummary: String
        val hookSuggestion: String?

        when {
            volumeDev > 5f -> {
                diagnosis = TensionDiagnosis.LOOSE
                val formatted = "%.1f".format(volumeDev)
                diagnosisSummary = "+$formatted% de volumen por tensión holgada"
                hookSuggestion = if (volumeDev > 15f) {
                    val suggested = (standard.hookSizeMm - 0.5f).coerceAtLeast(1.5f)
                    "Para mayor firmeza en amigurumis, prueba con aguja de ${suggested} mm."
                } else null
            }
            volumeDev < -5f -> {
                diagnosis = TensionDiagnosis.TIGHT
                val formatted = "%.1f".format(volumeDev)
                diagnosisSummary = "$formatted% de volumen por tensión apretada"
                hookSuggestion = if (volumeDev < -15f) {
                    val suggested = standard.hookSizeMm + 0.5f
                    "Si el tejido resulta excesivamente rígido, prueba con aguja de ${suggested} mm."
                } else null
            }
            else -> {
                diagnosis = TensionDiagnosis.BALANCED
                val sign = if (volumeDev >= 0f) "+" else ""
                val formatted = "%.1f".format(volumeDev)
                diagnosisSummary = "$sign$formatted% de volumen (tensión equilibrada)"
                hookSuggestion = null
            }
        }

        // Estimación de una pieza de muestra de referencia (ej. 30 puntos por 30 vueltas)
        val refStitches = 30
        val refRounds = 30
        val theoreticalDims = Pair(
            refStitches * standard.stitchWidthMm,
            refRounds * standard.stitchHeightMm
        )
        val calibratedDims = Pair(
            refStitches * calibrationResult.customStandard.stitchWidthMm,
            refRounds * calibrationResult.customStandard.stitchHeightMm
        )

        return TensionScaleComparison(
            widthDeviationPercent = widthDev,
            heightDeviationPercent = heightDev,
            volumeDeviationPercent = volumeDev,
            widthScaleRatio = widthRatio,
            heightScaleRatio = heightRatio,
            volumeScaleRatio = volumeRatio,
            diagnosis = diagnosis,
            diagnosisSummary = diagnosisSummary,
            hookSuggestion = hookSuggestion,
            theoreticalDimensionsMm = theoreticalDims,
            calibratedDimensionsMm = calibratedDims
        )
    }

    private fun applyCalibration() {
        val result = state.value.calibrationResult
        if (result != null) {
            setState { copy(isSaved = true) }
            viewModelScope.launch {
                sendEffect(TensionEffect.ShowSnackbar("Tensión de muestra 10×10 cm calibrada correctamente"))
                sendEffect(TensionEffect.NavigateBack)
            }
        } else {
            viewModelScope.launch {
                sendEffect(TensionEffect.ShowSnackbar("No se puede aplicar: revisa los valores de la muestra"))
            }
        }
    }
}

/**
 * Alias de compatibilidad semántica para el Calibrador Dimensional completo.
 */
typealias TensionCalibratorViewModel = TensionViewModel

