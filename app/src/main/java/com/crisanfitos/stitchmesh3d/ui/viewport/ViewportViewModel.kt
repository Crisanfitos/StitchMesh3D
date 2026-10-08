package com.crisanfitos.stitchmesh3d.ui.viewport

import androidx.compose.ui.geometry.Offset
import com.crisanfitos.stitchmesh3d.core.mvi.BaseViewModel
import com.crisanfitos.stitchmesh3d.ui.viewport.components.CameraPreset
import com.crisanfitos.stitchmesh3d.ui.viewport.components.ViewportDimensionsUiModel
import com.crisanfitos.stitchmesh3d.ui.viewport.components.ViewportTelemetryUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel que gestiona la máquina de estados del visor 3D paramétrico,
 * incluyendo peeling de capas, modo alambre, cámara orbital y cotas calibradas.
 */
@HiltViewModel
class ViewportViewModel @Inject constructor() :
    BaseViewModel<ViewportState, ViewportIntent, ViewportEffect>(ViewportState()) {

    override fun processIntent(intent: ViewportIntent) {
        when (intent) {
            is ViewportIntent.SelectPeelRound -> {
                val clamped = intent.round.coerceIn(1, state.value.totalRounds.coerceAtLeast(1))
                setState {
                    val updatedDimensions = calculateDimensions(clamped, 3.5f)
                    val updatedTelemetry = calculateTelemetry(clamped, telemetry.tensionGaugeLabel.substringBefore(" ·"), 3.5f)
                    copy(
                        currentPeelRound = clamped,
                        dimensions = updatedDimensions,
                        telemetry = updatedTelemetry
                    )
                }
            }
            is ViewportIntent.ToggleWireframe -> {
                setState { copy(isWireframe = intent.isWireframe) }
            }
            is ViewportIntent.SelectCameraPreset -> {
                setState {
                    when (intent.preset) {
                        CameraPreset.ISOMETRIC -> copy(
                            selectedCameraPreset = intent.preset,
                            orbitYaw = 45f,
                            orbitPitch = 30f
                        )
                        CameraPreset.FRONT -> copy(
                            selectedCameraPreset = intent.preset,
                            orbitYaw = 0f,
                            orbitPitch = 0f
                        )
                        CameraPreset.SIDE -> copy(
                            selectedCameraPreset = intent.preset,
                            orbitYaw = 90f,
                            orbitPitch = 0f
                        )
                        CameraPreset.TOP -> copy(
                            selectedCameraPreset = intent.preset,
                            orbitYaw = 0f,
                            orbitPitch = 90f
                        )
                        CameraPreset.RESET -> copy(
                            selectedCameraPreset = CameraPreset.ISOMETRIC,
                            orbitYaw = 45f,
                            orbitPitch = 30f,
                            zoomScale = 1.0f,
                            panOffset = Offset.Zero
                        )
                    }
                }
            }
            is ViewportIntent.ToggleFullscreen -> {
                setState { copy(isFullscreen = !isFullscreen) }
            }
            is ViewportIntent.UpdateOrbit -> {
                setState {
                    copy(
                        orbitYaw = (orbitYaw + intent.deltaYaw) % 360f,
                        orbitPitch = (orbitPitch + intent.deltaPitch).coerceIn(-89f, 89f)
                    )
                }
            }
            is ViewportIntent.UpdateZoom -> {
                setState {
                    val newZoom = (zoomScale * intent.scaleMultiplier).coerceIn(0.5f, 3.5f)
                    copy(zoomScale = newZoom)
                }
            }
            is ViewportIntent.UpdatePan -> {
                setState {
                    copy(panOffset = panOffset + intent.deltaOffset)
                }
            }
            is ViewportIntent.ResetCamera -> {
                setState {
                    copy(
                        selectedCameraPreset = CameraPreset.ISOMETRIC,
                        orbitYaw = 45f,
                        orbitPitch = 30f,
                        zoomScale = 1.0f,
                        panOffset = Offset.Zero
                    )
                }
            }
            is ViewportIntent.UpdatePatternParameters -> {
                val rounds = intent.totalRounds.coerceAtLeast(1)
                val clampedPeel = state.value.currentPeelRound.coerceIn(1, rounds)
                setState {
                    copy(
                        totalRounds = rounds,
                        currentPeelRound = clampedPeel,
                        dimensions = calculateDimensions(clampedPeel, intent.hookSizeMm),
                        telemetry = calculateTelemetry(clampedPeel, intent.yarnWeightName, intent.hookSizeMm)
                    )
                }
            }
        }
    }

    private fun calculateDimensions(peelRound: Int, hookSizeMm: Float): ViewportDimensionsUiModel {
        val baseRadiusMm = (peelRound * hookSizeMm * 2.2f).coerceAtLeast(30f)
        val heightMm = (peelRound * hookSizeMm * 3.1f).coerceAtLeast(25f)
        return ViewportDimensionsUiModel(
            widthMm = baseRadiusMm * 2f,
            heightMm = heightMm,
            depthMm = baseRadiusMm * 2f
        )
    }

    private fun calculateTelemetry(
        peelRound: Int,
        yarnWeightName: String,
        hookSizeMm: Float
    ): ViewportTelemetryUiModel {
        val estimatedPolys = (peelRound * 180).coerceAtLeast(360)
        return ViewportTelemetryUiModel(
            polygonCount = estimatedPolys,
            vertexCount = estimatedPolys / 2 + 32,
            fps = 60,
            tensionGaugeLabel = "$yarnWeightName · ${hookSizeMm} mm"
        )
    }
}
