package com.crisanfitos.stitchmesh3d.core.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para [ColorSpaceHelper] (SM-085).
 */
class ColorSpaceHelperTest {

    @Test
    fun `srgbToLinear handles boundary conditions correctly`() {
        assertEquals(0.0f, ColorSpaceHelper.srgbToLinear(0.0f), 1e-6f)
        assertEquals(1.0f, ColorSpaceHelper.srgbToLinear(1.0f), 1e-5f)
        // Valor umbral 0.04045f
        val thresholdLinear = ColorSpaceHelper.srgbToLinear(0.04045f)
        assertEquals(0.04045f / 12.92f, thresholdLinear, 1e-5f)
    }

    @Test
    fun `srgbToLinear and linearToSrgb roundtrip accurately`() {
        for (v in listOf(0.05f, 0.1f, 0.25f, 0.5f, 0.75f, 0.9f)) {
            val linear = ColorSpaceHelper.srgbToLinear(v)
            val srgb = ColorSpaceHelper.linearToSrgb(linear)
            assertEquals("Roundtrip para $v", v, srgb, 0.001f)
        }
    }

    @Test
    fun `hexToLinearRgba converts Design Brief dark background 121316 accurately`() {
        val linear = ColorSpaceHelper.hexToLinearRgba("#121316")
        assertEquals(4, linear.size)

        // R = 18/255 ~ 0.070588 -> linear ~ 0.00599
        assertEquals(0.00599f, linear[0], 0.0005f)
        // G = 19/255 ~ 0.074510 -> linear ~ 0.00654
        assertEquals(0.00654f, linear[1], 0.0005f)
        // B = 22/255 ~ 0.086275 -> linear ~ 0.00832
        assertEquals(0.00832f, linear[2], 0.0005f)
        assertEquals(1.0f, linear[3], 1e-5f)

        // Comprobar que en espacio lineal los valores son notablemente inferiores a los crudos
        assertTrue(linear[0] < 0.07f)
        assertTrue(linear[1] < 0.074f)
        assertTrue(linear[2] < 0.086f)
    }
}
