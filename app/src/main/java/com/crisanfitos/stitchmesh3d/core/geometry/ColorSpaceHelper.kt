package com.crisanfitos.stitchmesh3d.core.geometry

/**
 * Utilidades matemáticas para transformaciones de espacio de color conforme al estándar IEC 61966-2-1 (sRGB)
 * requeridas por Google Filament para Skybox y parámetros fotométricos (SM-085).
 */
object ColorSpaceHelper {

    /**
     * Convierte un componente de color sRGB [0.0..1.0] a espacio lineal fotométrico.
     *
     * Fórmula oficial sRGB (IEC 61966-2-1):
     * - Si C <= 0.04045: C_linear = C / 12.92
     * - Si C > 0.04045: C_linear = ((C + 0.055) / 1.055)^2.4
     */
    fun srgbToLinear(c: Float): Float {
        val clamped = c.coerceIn(0.0f, 1.0f)
        return if (clamped <= 0.04045f) {
            clamped / 12.92f
        } else {
            Math.pow(((clamped + 0.055) / 1.055), 2.4).toFloat()
        }
    }

    /**
     * Convierte un componente lineal fotométrico [0.0..1.0] a espacio sRGB perceptivo.
     */
    fun linearToSrgb(c: Float): Float {
        val clamped = c.coerceIn(0.0f, 1.0f)
        return if (clamped <= 0.0031308f) {
            clamped * 12.92f
        } else {
            (1.055 * Math.pow(clamped.toDouble(), 1.0 / 2.4) - 0.055).toFloat()
        }
    }

    /**
     * Convierte un código hexadecimal (ej. "#121316") a valores lineales RGBA [0.0..1.0] para Skybox y shaders Filament.
     */
    fun hexToLinearRgba(hex: String, alpha: Float = 1.0f): FloatArray {
        val clean = hex.removePrefix("#")
        require(clean.length == 6 || clean.length == 8) { "Hexadecimal de color inválido: $hex" }
        val r = clean.substring(0, 2).toInt(16) / 255.0f
        val g = clean.substring(2, 4).toInt(16) / 255.0f
        val b = clean.substring(4, 6).toInt(16) / 255.0f
        val a = if (clean.length == 8) clean.substring(6, 8).toInt(16) / 255.0f else alpha

        return floatArrayOf(
            srgbToLinear(r),
            srgbToLinear(g),
            srgbToLinear(b),
            a
        )
    }
}
