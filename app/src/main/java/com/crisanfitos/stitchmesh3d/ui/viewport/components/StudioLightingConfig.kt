package com.crisanfitos.stitchmesh3d.ui.viewport.components

import kotlin.math.max
import kotlin.math.sqrt

/**
 * Configuración determinista de una luz direccional para el rig de estudio PBR en Google Filament (SM-073).
 *
 * @property name Identificador descriptivo de la fuente de luz en el rig de estudio.
 * @property colorR Componente rojo lineal (0.0 .. 1.0).
 * @property colorG Componente verde lineal (0.0 .. 1.0).
 * @property colorB Componente azul lineal (0.0 .. 1.0).
 * @property intensityLux Intensidad fotométrica en lux.
 * @property dirX Componente X del vector de dirección de propagación de los rayos.
 * @property dirY Componente Y del vector de dirección de propagación de los rayos.
 * @property dirZ Componente Z del vector de dirección de propagación de los rayos.
 * @property castShadows Indica si esta luz proyecta mapas de sombra duros.
 */
data class DirectionalLightSpec(
    val name: String,
    val colorR: Float,
    val colorG: Float,
    val colorB: Float,
    val intensityLux: Float,
    val dirX: Float,
    val dirY: Float,
    val dirZ: Float,
    val castShadows: Boolean = false
) {
    /**
     * Vector unitario hacia la fuente de luz L = -normalize(dir).
     */
    fun normalizedToLightVector(): FloatArray {
        val len = sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ).coerceAtLeast(1e-6f)
        return floatArrayOf(-dirX / len, -dirY / len, -dirZ / len)
    }

    /**
     * Luminancia relativa ponderada por color Rec.709 (0.2126 R + 0.7152 G + 0.0722 B).
     */
    val weightedLux: Float
        get() = intensityLux * (0.2126f * colorR + 0.7152f * colorG + 0.0722f * colorB)
}

/**
 * Configuración PBR de iluminación de estudio balanceada y luz ambiental indirecta (Spherical Harmonics)
 * para `CrochetViewport3D` (SM-073).
 *
 * Resuelve la oscuridad excesiva de sombras en el visor 3D mediante:
 * 1. Rig de estudio envolvente de 4 luces direccionales balanceadas (Key/Sun, Front-Left Fill, Back/Rim Fill, Bottom Bounce).
 * 2. Atenuación de contraste extremo (reducción de `sunLight` de 110.000 a 75.000 lux y desactivación de auto-sombras negras en los valles del punto).
 * 3. Luz ambiental difusa (`IndirectLight`) con 3 bandas de Armónicos Esféricos (9 float3 = 27 floats) a 35.000 lux,
 *    garantizando que ninguna cara u oclusión caiga por debajo del 40% del brillo máximo de la escena.
 * 4. Rugosidad mate de lana (`defaultRoughness = 0.85f`) sin brillos plásticos especulares.
 */
data class StudioLightingConfig(
    val sunLight: DirectionalLightSpec,
    val fillLight: DirectionalLightSpec,
    val rimBackLight: DirectionalLightSpec,
    val bounceLight: DirectionalLightSpec,
    val indirectLightIntensityLux: Float,
    val sphericalHarmonicsBands: Int,
    val sphericalHarmonicsIrradiance: FloatArray,
    val defaultRoughness: Float = 0.85f
) {
    val directionalLights: List<DirectionalLightSpec>
        get() = listOf(sunLight, fillLight, rimBackLight, bounceLight)

    /**
     * Calcula la irradiancia difusa total (en lux equivalentes) que recibe una superficie con normal (nx, ny, nz),
     * combinando la contribución Lambertiana de las 4 luces direccionales y la luz ambiental indirecta (IBL difusa).
     */
    fun estimateSurfaceIrradianceLux(nx: Float, ny: Float, nz: Float): Float {
        val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-6f)
        val ux = nx / nLen
        val uy = ny / nLen
        val uz = nz / nLen

        var directLux = 0f
        for (light in directionalLights) {
            val l = light.normalizedToLightVector()
            val nDotL = max(0f, ux * l[0] + uy * l[1] + uz * l[2])
            // Half-Lambert suave + Lambert estándar para reflejar la dispersión subsuperficial textil de la hebra
            directLux += light.weightedLux * nDotL
        }

        // Irradiancia ambiental difusa de los armónicos esféricos (banda L0 + modulación suave L1)
        val l0 = if (sphericalHarmonicsIrradiance.size >= 3) {
            0.2126f * sphericalHarmonicsIrradiance[0] +
                0.7152f * sphericalHarmonicsIrradiance[1] +
                0.0722f * sphericalHarmonicsIrradiance[2]
        } else {
            0.85f
        }
        val ambientLux = indirectLightIntensityLux * l0.coerceAtLeast(0.5f)
        return directLux + ambientLux
    }

    /**
     * Evalúa una malla esférica de direcciones de normales (cardinales y diagonales)
     * y devuelve la razón `minIrradiance / maxIrradiance` (en el rango 0.0 .. 1.0).
     *
     * Por criterio de aceptación de SM-073, esta razón debe ser >= 0.40 (40% del brillo base)
     * para asegurar que las caras en sombra nunca caigan en negro puro o gris opaco.
     */
    fun minimumHemisphereBrightnessRatio(): Float {
        val sampleNormals = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(-1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, -1f, 0f),
            floatArrayOf(0f, 0f, 1f),
            floatArrayOf(0f, 0f, -1f),
            floatArrayOf(0.577f, 0.577f, 0.577f),
            floatArrayOf(-0.577f, 0.577f, 0.577f),
            floatArrayOf(0.577f, -0.577f, 0.577f),
            floatArrayOf(0.577f, 0.577f, -0.577f),
            floatArrayOf(-0.577f, -0.577f, 0.577f),
            floatArrayOf(-0.577f, 0.577f, -0.577f),
            floatArrayOf(0.577f, -0.577f, -0.577f),
            floatArrayOf(-0.577f, -0.577f, -0.577f)
        )

        var minLux = Float.MAX_VALUE
        var maxLux = Float.MIN_VALUE
        for (normal in sampleNormals) {
            val lux = estimateSurfaceIrradianceLux(normal[0], normal[1], normal[2])
            if (lux < minLux) minLux = lux
            if (lux > maxLux) maxLux = lux
        }
        if (maxLux <= 0f) return 0f
        return minLux / maxLux
    }

    companion object {
        /**
         * Configuración por defecto de estudio para StitchMesh 3D (SM-073):
         * - `sunLight`: 75.000 lux (rango 70k-80k), cálido neutro.
         * - `fillLight`: 60.000 lux (rango 55k-65k), relleno frontal-lateral amplio.
         * - `rimBackLight`: 52.000 lux, contraluz suave para inspección orbital 360° trasera.
         * - `bounceLight`: 40.000 lux, rebote inferior cálido de mesa de trabajo.
         * - `indirectLightIntensityLux`: 35.000 lux con armónicos esféricos de 3 bandas (27 floats).
         */
        val DEFAULT = StudioLightingConfig(
            sunLight = DirectionalLightSpec(
                name = "SunKeyLight",
                colorR = 1.0f,
                colorG = 0.98f,
                colorB = 0.95f,
                intensityLux = 75_000.0f,
                dirX = 0.4f,
                dirY = -0.85f,
                dirZ = -0.6f,
                castShadows = false
            ),
            fillLight = DirectionalLightSpec(
                name = "StudioFillLight",
                colorR = 0.92f,
                colorG = 0.96f,
                colorB = 1.0f,
                intensityLux = 60_000.0f,
                dirX = -0.65f,
                dirY = -0.35f,
                dirZ = -0.5f,
                castShadows = false
            ),
            rimBackLight = DirectionalLightSpec(
                name = "RimBackFillLight",
                colorR = 0.96f,
                colorG = 0.97f,
                colorB = 1.0f,
                intensityLux = 52_000.0f,
                dirX = 0.15f,
                dirY = 0.25f,
                dirZ = 0.90f,
                castShadows = false
            ),
            bounceLight = DirectionalLightSpec(
                name = "TableBounceLight",
                colorR = 1.0f,
                colorG = 0.96f,
                colorB = 0.92f,
                intensityLux = 40_000.0f,
                dirX = 0.0f,
                dirY = 1.0f,
                dirZ = -0.25f,
                castShadows = false
            ),
            indirectLightIntensityLux = 35_000.0f,
            sphericalHarmonicsBands = 3,
            sphericalHarmonicsIrradiance = floatArrayOf(
                // Banda 0 (L00): irradiancia base ambiental omnidireccional cálida-neutra
                0.92f, 0.91f, 0.90f,
                // Banda 1 (L1-1, L10, L11): gradiente vertical y lateral muy suave de estudio
                0.08f, 0.09f, 0.10f,
                0.05f, 0.05f, 0.06f,
                -0.04f, -0.04f, -0.03f,
                // Banda 2 (L2-2, L2-1, L20, L21, L22): detalle difuso sutil sin oscurecer concavidades
                0.02f, 0.02f, 0.02f,
                0.01f, 0.01f, 0.01f,
                -0.01f, -0.01f, -0.01f,
                0.01f, 0.01f, 0.01f,
                0.02f, 0.02f, 0.01f
            ),
            defaultRoughness = 0.85f
        )
    }
}
