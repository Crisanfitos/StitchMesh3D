package com.crisanfitos.stitchmesh3d.ui.viewport

import androidx.compose.ui.geometry.Offset
import com.crisanfitos.stitchmesh3d.ui.viewport.components.CameraPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ViewportViewModelTest {

    private lateinit var viewModel: ViewportViewModel

    @Before
    fun setUp() {
        viewModel = ViewportViewModel()
    }

    @Test
    fun `initial state has expected CAD defaults`() {
        val state = viewModel.state.value

        assertFalse(state.isFullscreen)
        assertFalse(state.isWireframe)
        assertEquals(3, state.currentPeelRound)
        assertEquals(3, state.totalRounds)
        assertEquals(CameraPreset.ISOMETRIC, state.selectedCameraPreset)
        assertEquals(1.0f, state.zoomScale)
        assertEquals(45f, state.orbitYaw)
        assertEquals(30f, state.orbitPitch)
    }

    @Test
    fun `selecting peel round clamps to valid 1 to totalRounds range`() {
        viewModel.processIntent(ViewportIntent.UpdatePatternParameters(totalRounds = 5, hookSizeMm = 4.0f, yarnWeightName = "#4"))

        viewModel.processIntent(ViewportIntent.SelectPeelRound(2))
        assertEquals(2, viewModel.state.value.currentPeelRound)

        // Lower than 1 should clamp to 1
        viewModel.processIntent(ViewportIntent.SelectPeelRound(-1))
        assertEquals(1, viewModel.state.value.currentPeelRound)

        // Greater than total should clamp to 5
        viewModel.processIntent(ViewportIntent.SelectPeelRound(10))
        assertEquals(5, viewModel.state.value.currentPeelRound)
    }

    @Test
    fun `toggling wireframe updates state accurately`() {
        viewModel.processIntent(ViewportIntent.ToggleWireframe(true))
        assertTrue(viewModel.state.value.isWireframe)

        viewModel.processIntent(ViewportIntent.ToggleWireframe(false))
        assertFalse(viewModel.state.value.isWireframe)
    }

    @Test
    fun `selecting camera presets adjusts angles correctly`() {
        viewModel.processIntent(ViewportIntent.SelectCameraPreset(CameraPreset.FRONT))
        assertEquals(CameraPreset.FRONT, viewModel.state.value.selectedCameraPreset)
        assertEquals(0f, viewModel.state.value.orbitYaw)
        assertEquals(0f, viewModel.state.value.orbitPitch)

        viewModel.processIntent(ViewportIntent.SelectCameraPreset(CameraPreset.SIDE))
        assertEquals(CameraPreset.SIDE, viewModel.state.value.selectedCameraPreset)
        assertEquals(90f, viewModel.state.value.orbitYaw)

        viewModel.processIntent(ViewportIntent.SelectCameraPreset(CameraPreset.TOP))
        assertEquals(CameraPreset.TOP, viewModel.state.value.selectedCameraPreset)
        assertEquals(90f, viewModel.state.value.orbitPitch)

        viewModel.processIntent(ViewportIntent.SelectCameraPreset(CameraPreset.RESET))
        assertEquals(CameraPreset.ISOMETRIC, viewModel.state.value.selectedCameraPreset)
        assertEquals(45f, viewModel.state.value.orbitYaw)
        assertEquals(30f, viewModel.state.value.orbitPitch)
        assertEquals(1.0f, viewModel.state.value.zoomScale)
    }

    @Test
    fun `fullscreen toggles between embedded and maximized mode`() {
        assertFalse(viewModel.state.value.isFullscreen)

        viewModel.processIntent(ViewportIntent.ToggleFullscreen)
        assertTrue(viewModel.state.value.isFullscreen)

        viewModel.processIntent(ViewportIntent.ToggleFullscreen)
        assertFalse(viewModel.state.value.isFullscreen)
    }

    @Test
    fun `updating orbit clamps pitch within safe visual bounds`() {
        viewModel.processIntent(ViewportIntent.UpdateOrbit(deltaYaw = 20f, deltaPitch = 100f))
        assertEquals(65f, viewModel.state.value.orbitYaw)
        assertEquals(89f, viewModel.state.value.orbitPitch)

        viewModel.processIntent(ViewportIntent.UpdateOrbit(deltaYaw = -10f, deltaPitch = -200f))
        assertEquals(55f, viewModel.state.value.orbitYaw)
        assertEquals(-89f, viewModel.state.value.orbitPitch)
    }

    @Test
    fun `updating zoom clamps scale between 0_5 and 3_5`() {
        viewModel.processIntent(ViewportIntent.UpdateZoom(scaleMultiplier = 2.0f))
        assertEquals(2.0f, viewModel.state.value.zoomScale)

        viewModel.processIntent(ViewportIntent.UpdateZoom(scaleMultiplier = 3.0f))
        assertEquals(3.5f, viewModel.state.value.zoomScale)

        viewModel.processIntent(ViewportIntent.UpdateZoom(scaleMultiplier = 0.05f))
        assertEquals(0.5f, viewModel.state.value.zoomScale)
    }

    @Test
    fun `pattern parameters update recomputes metric dimensions and telemetry`() {
        viewModel.processIntent(
            ViewportIntent.UpdatePatternParameters(
                totalRounds = 6,
                hookSizeMm = 5.0f,
                yarnWeightName = "#5 Bulky"
            )
        )

        val state = viewModel.state.value
        assertEquals(6, state.totalRounds)
        assertTrue(state.dimensions.widthMm > 60f)
        assertTrue(state.dimensions.heightMm > 30f)
        assertTrue(state.telemetry.polygonCount > 500)
    }
}
