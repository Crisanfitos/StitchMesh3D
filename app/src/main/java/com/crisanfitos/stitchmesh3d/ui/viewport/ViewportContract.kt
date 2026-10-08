package com.crisanfitos.stitchmesh3d.ui.viewport

import androidx.compose.ui.geometry.Offset
import com.crisanfitos.stitchmesh3d.core.mvi.ViewEffect
import com.crisanfitos.stitchmesh3d.core.mvi.ViewIntent
import com.crisanfitos.stitchmesh3d.core.mvi.ViewState
import com.crisanfitos.stitchmesh3d.ui.viewport.components.CameraPreset
import com.crisanfitos.stitchmesh3d.ui.viewport.components.ViewportDimensionsUiModel
import com.crisanfitos.stitchmesh3d.ui.viewport.components.ViewportTelemetryUiModel

/**
 * Estado inmutable de la pantalla y módulo del Visor 3D Paramétrico (MVI).
 */
data class ViewportState(
    val isFullscreen: Boolean = false,
    val currentPeelRound: Int = 3,
    val totalRounds: Int = 3,
    val isWireframe: Boolean = false,
    val selectedCameraPreset: CameraPreset = CameraPreset.ISOMETRIC,
    val dimensions: ViewportDimensionsUiModel = ViewportDimensionsUiModel(60f, 32f, 60f),
    val telemetry: ViewportTelemetryUiModel = ViewportTelemetryUiModel(
        polygonCount = 540,
        vertexCount = 302,
        fps = 60,
        tensionGaugeLabel = "#4 Worsted · 3.5 mm"
    ),
    val orbitYaw: Float = 45f,
    val orbitPitch: Float = 30f,
    val zoomScale: Float = 1.0f,
    val panOffset: Offset = Offset.Zero,
    val meshGeometry: com.crisanfitos.stitchmesh3d.core.geometry.MeshGeometry? = null
) : ViewState

/**
 * Intenciones de usuario en el visor 3D paramétrico.
 */
sealed interface ViewportIntent : ViewIntent {
    data class SetMeshGeometry(
        val meshGeometry: com.crisanfitos.stitchmesh3d.core.geometry.MeshGeometry?
    ) : ViewportIntent
    data class SelectPeelRound(val round: Int) : ViewportIntent
    data class ToggleWireframe(val isWireframe: Boolean) : ViewportIntent
    data class SelectCameraPreset(val preset: CameraPreset) : ViewportIntent
    data object ToggleFullscreen : ViewportIntent
    data class UpdateOrbit(val deltaYaw: Float, val deltaPitch: Float) : ViewportIntent
    data class UpdateZoom(val scaleMultiplier: Float) : ViewportIntent
    data class UpdatePan(val deltaOffset: Offset) : ViewportIntent
    data object ResetCamera : ViewportIntent
    data class UpdatePatternParameters(
        val totalRounds: Int,
        val hookSizeMm: Float,
        val yarnWeightName: String
    ) : ViewportIntent
}

/**
 * Efectos secundarios de un solo uso para el visor 3D.
 */
sealed interface ViewportEffect : ViewEffect {
    data class ShowToast(val message: String) : ViewportEffect
    data object ExitFullscreen : ViewportEffect
}
