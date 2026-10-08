package com.crisanfitos.stitchmesh3d.ui.viewport.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewportHudOverlayTest {

    @Test
    fun `dimensions model formats correctly with millimeter units`() {
        val dimensions = ViewportDimensionsUiModel(
            widthMm = 124.4f,
            heightMm = 98.2f,
            depthMm = 110.0f
        )

        assertEquals("124 mm × 98 mm × 110 mm", dimensions.formattedDimensions())
    }

    @Test
    fun `camera presets include all required CAD orientations`() {
        val presets = CameraPreset.entries

        assertEquals(5, presets.size)
        assertTrue(presets.contains(CameraPreset.FRONT))
        assertTrue(presets.contains(CameraPreset.SIDE))
        assertTrue(presets.contains(CameraPreset.TOP))
        assertTrue(presets.contains(CameraPreset.ISOMETRIC))
        assertTrue(presets.contains(CameraPreset.RESET))
    }

    @Test
    fun `telemetry model has expected default metrics`() {
        val telemetry = ViewportTelemetryUiModel()

        assertEquals(1240, telemetry.polygonCount)
        assertEquals(642, telemetry.vertexCount)
        assertEquals(60, telemetry.fps)
        assertEquals("#4 Worsted · 3.5 mm", telemetry.tensionGaugeLabel)
    }

    @Test
    fun `camera preset selection callback is triggered accurately`() {
        var selected = CameraPreset.ISOMETRIC

        fun selectPreset(preset: CameraPreset) {
            selected = preset
        }

        selectPreset(CameraPreset.FRONT)
        assertEquals(CameraPreset.FRONT, selected)

        selectPreset(CameraPreset.TOP)
        assertEquals(CameraPreset.TOP, selected)

        selectPreset(CameraPreset.RESET)
        assertEquals(CameraPreset.RESET, selected)
    }

    @Test
    fun `dimensions model formats correctly with centimeter units and diameter`() {
        val dimensions = ViewportDimensionsUiModel(
            widthMm = 80.0f,
            heightMm = 120.0f,
            depthMm = 65.0f
        )

        assertEquals("80 mm × 120 mm × 65 mm", dimensions.formattedDimensions())
        assertEquals("8.0 cm × 12.0 cm × 6.5 cm", dimensions.formattedDimensionsCm())
        assertEquals(80.0f, dimensions.maxDiameterMm, 0.001f)
        assertEquals("Ø 80 mm · H 120 mm", dimensions.formattedDiameterAndHeight())
    }

    @Test
    fun `toggle dimension callouts and unit system callbacks trigger properly`() {
        var calloutsVisible = true
        var isCm = false

        fun toggleCallouts() {
            calloutsVisible = !calloutsVisible
        }

        fun toggleUnit() {
            isCm = !isCm
        }

        toggleCallouts()
        assertTrue(!calloutsVisible)

        toggleCallouts()
        assertTrue(calloutsVisible)

        toggleUnit()
        assertTrue(isCm)

        toggleUnit()
        assertTrue(!isCm)
    }
}
