package com.crisanfitos.stitchmesh3d.ui.viewport.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios para la configuración de iluminación PBR de estudio y atenuación de sombras (SM-073).
 */
class StudioLightingConfigTest {

    @Test
    fun `default studio lighting rig respects balanced lux ranges for sun, fill, rim and bounce lights`() {
        val config = StudioLightingConfig.DEFAULT

        assertTrue(
            "Sun light (${config.sunLight.intensityLux} lux) debe estar entre 70.000 y 80.000 lux",
            config.sunLight.intensityLux in 70_000f..80_000f
        )
        assertTrue(
            "Fill light (${config.fillLight.intensityLux} lux) debe estar entre 55.000 y 65.000 lux",
            config.fillLight.intensityLux in 55_000f..65_000f
        )
        assertEquals(40_000f, config.bounceLight.intensityLux, 2_000f)
        assertTrue(
            "Rim/Back light (${config.rimBackLight.intensityLux} lux) debe cubrir la cara posterior (>= 40.000 lux)",
            config.rimBackLight.intensityLux >= 40_000f
        )
        assertEquals(4, config.directionalLights.size)
    }

    @Test
    fun `default studio lighting disables hard shadow maps and preserves matte wool roughness`() {
        val config = StudioLightingConfig.DEFAULT

        for (light in config.directionalLights) {
            assertFalse(
                "La luz ${light.name} no debe proyectar sombras negras duras en las hendiduras de los puntos",
                light.castShadows
            )
        }
        assertEquals(0.85f, config.defaultRoughness, 0.05f)
    }

    @Test
    fun `spherical harmonics indirect light provides valid 3-band 27-float diffuse irradiance`() {
        val config = StudioLightingConfig.DEFAULT

        assertEquals(3, config.sphericalHarmonicsBands)
        assertEquals(27, config.sphericalHarmonicsIrradiance.size)
        assertTrue(config.indirectLightIntensityLux >= 30_000f)
        // La banda L00 debe ser cálida-neutra y positiva en los 3 canales RGB
        assertTrue(config.sphericalHarmonicsIrradiance[0] > 0.8f)
        assertTrue(config.sphericalHarmonicsIrradiance[1] > 0.8f)
        assertTrue(config.sphericalHarmonicsIrradiance[2] > 0.8f)
    }

    @Test
    fun `shadowed hemisphere brightness never drops below 40 percent of peak brightness across 360 orbit`() {
        val config = StudioLightingConfig.DEFAULT

        val ratio = config.minimumHemisphereBrightnessRatio()
        assertTrue(
            "El ratio mínimo de brillo en zonas sombreadas ($ratio) debe ser >= 0.40 (40% del brillo base)",
            ratio >= 0.40f
        )

        // Comprobación explícita de las 6 normales cardinales (+X, -X, +Y, -Y, +Z, -Z)
        val frontLux = config.estimateSurfaceIrradianceLux(0f, 0f, 1f)
        val backLux = config.estimateSurfaceIrradianceLux(0f, 0f, -1f)
        val topLux = config.estimateSurfaceIrradianceLux(0f, 1f, 0f)
        val bottomLux = config.estimateSurfaceIrradianceLux(0f, -1f, 0f)
        val leftLux = config.estimateSurfaceIrradianceLux(-1f, 0f, 0f)
        val rightLux = config.estimateSurfaceIrradianceLux(1f, 0f, 0f)

        val maxCardinal = maxOf(frontLux, backLux, topLux, bottomLux, leftLux, rightLux)
        val minCardinal = minOf(frontLux, backLux, topLux, bottomLux, leftLux, rightLux)

        assertTrue(
            "Ninguna cara cardinal puede caer por debajo del 40% de la cara más iluminada (${minCardinal / maxCardinal})",
            (minCardinal / maxCardinal) >= 0.40f
        )
    }

    @Test
    fun `calibrated textile studio lighting enables soft directional shadows and prevents blowout (SM-085)`() {
        val config = StudioLightingConfig.CALIBRATED_TEXTILE

        // Key light rasante con sombras activadas
        assertTrue("Key light debe proyectar sombras para generar relieve textil", config.sunLight.castShadows)
        assertTrue("Key light debe estar en rango fotométrico calibrado [60k..70k]", config.sunLight.intensityLux in 60_000f..70_000f)

        // Relleno y rim cálido para sheen
        assertFalse("Fill light no debe proyectar sombras", config.fillLight.castShadows)
        assertTrue(config.fillLight.intensityLux in 15_000f..25_000f)
        assertTrue(config.rimBackLight.intensityLux in 18_000f..26_000f)

        // Evitar el lavado de más de 300.000 lux del rig anterior
        val totalDirectLux = config.directionalLights.sumOf { it.intensityLux.toDouble() }.toFloat()
        assertTrue("La suma total de luces directas no debe sobreexponer la escena ($totalDirectLux < 130.000 lux)", totalDirectLux < 130_000f)
    }
}
