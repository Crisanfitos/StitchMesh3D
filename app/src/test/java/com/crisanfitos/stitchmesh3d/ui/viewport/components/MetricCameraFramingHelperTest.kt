package com.crisanfitos.stitchmesh3d.ui.viewport.components

import com.crisanfitos.stitchmesh3d.core.geometry.BoundingBox3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para [MetricCameraFramingHelper] (SM-085).
 */
class MetricCameraFramingHelperTest {

    @Test
    fun `calculateFraming for empty bbox returns valid safe metric defaults`() {
        val framing = MetricCameraFramingHelper.calculateFraming(
            bbox = BoundingBox3D(0f, 0f, 0f, 0f, 0f, 0f),
            preset = CameraPreset.ISOMETRIC
        )
        assertEquals(0f, framing.targetX, 1e-4f)
        assertEquals(25f, framing.targetY, 1e-4f)
        assertEquals(0f, framing.targetZ, 1e-4f)
        assertTrue(framing.fitDistanceMm >= 35f)
        assertEquals(1f, framing.upY, 1e-4f)
    }

    @Test
    fun `calculateFraming scales fitDistanceMm with model physical dimensions`() {
        // Muestra de tensión pequeña (Lace 2.0 mm): 20 mm de ancho, 30 mm de alto, 20 mm de profundidad
        val smallBbox = BoundingBox3D(
            minX = -10f, maxX = 10f,
            minY = 0f, maxY = 30f,
            minZ = -10f, maxZ = 10f
        )
        val smallFraming = MetricCameraFramingHelper.calculateFraming(smallBbox, CameraPreset.ISOMETRIC)

        // Muestra de tensión grande (Super Bulky 9.0 mm): 80 mm de ancho, 120 mm de alto, 80 mm de profundidad
        val largeBbox = BoundingBox3D(
            minX = -40f, maxX = 40f,
            minY = 0f, maxY = 120f,
            minZ = -40f, maxZ = 40f
        )
        val largeFraming = MetricCameraFramingHelper.calculateFraming(largeBbox, CameraPreset.ISOMETRIC)

        // El centroide Y debe ubicarse en la mitad de la altura
        assertEquals(15f, smallFraming.targetY, 1e-4f)
        assertEquals(60f, largeFraming.targetY, 1e-4f)

        // La distancia métrica de encuadre debe ser mayor en la pieza grande proporcionalmente
        assertTrue(
            "La pieza grande debe requerir mayor distancia de encuadre (${largeFraming.fitDistanceMm} vs ${smallFraming.fitDistanceMm})",
            largeFraming.fitDistanceMm > smallFraming.fitDistanceMm * 2.5f
        )
    }

    @Test
    fun `calculateFraming presets orient camera correctly in Y-up coordinate space`() {
        val bbox = BoundingBox3D(
            minX = -20f, maxX = 20f,
            minY = 0f, maxY = 50f,
            minZ = -20f, maxZ = 20f
        )

        // Preset FRONT: mirando desde +Z hacia el centroide
        val front = MetricCameraFramingHelper.calculateFraming(bbox, CameraPreset.FRONT)
        assertEquals(bbox.centerX, front.eyeX, 1e-4f)
        assertEquals(bbox.centerY, front.eyeY, 1e-4f)
        assertTrue(front.eyeZ > bbox.centerZ)
        assertEquals(0f, front.upX, 1e-4f)
        assertEquals(1f, front.upY, 1e-4f)
        assertEquals(0f, front.upZ, 1e-4f)

        // Preset SIDE: mirando desde +X hacia el centroide
        val side = MetricCameraFramingHelper.calculateFraming(bbox, CameraPreset.SIDE)
        assertTrue(side.eyeX > bbox.centerX)
        assertEquals(bbox.centerY, side.eyeY, 1e-4f)
        assertEquals(bbox.centerZ, side.eyeZ, 1e-4f)
        assertEquals(1f, side.upY, 1e-4f)

        // Preset TOP: mirando desde +Y hacia abajo (-Y), upVector apunta hacia -Z
        val top = MetricCameraFramingHelper.calculateFraming(bbox, CameraPreset.TOP)
        assertTrue(top.eyeY > bbox.centerY)
        assertEquals(0f, top.upX, 1e-4f)
        assertEquals(0f, top.upY, 1e-4f)
        assertEquals(-1f, top.upZ, 1e-4f)

        // Preset ISOMETRIC: visión diagonal positiva
        val iso = MetricCameraFramingHelper.calculateFraming(bbox, CameraPreset.ISOMETRIC)
        assertTrue(iso.eyeX > bbox.centerX)
        assertTrue(iso.eyeY > bbox.centerY)
        assertTrue(iso.eyeZ > bbox.centerZ)
        assertEquals(1f, iso.upY, 1e-4f)
    }
}
