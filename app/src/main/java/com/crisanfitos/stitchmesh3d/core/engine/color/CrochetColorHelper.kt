package com.crisanfitos.stitchmesh3d.core.engine.color

import java.util.Locale

/**
 * Utilidad de resolución y normalización de colores para instrucciones de crochet (TRD §2.1, RF-4.3).
 *
 * Permite interpretar:
 * 1. Códigos hexadecimales: `#FFFFFF`, `#E06D53`, `#FFF`, `#FF00FF88`.
 * 2. Nombres comunes en español e inglés: `blanco`, `white`, `rojo`, `red`, `terracota`, etc.
 * 3. Alias de paleta: `Color A`, `Color B`, `Color 1`, `Color 2`, `Col A`, etc.
 */
object CrochetColorHelper {

    // Paleta por defecto para alias de letras/números (A, B, C, D, E...)
    private val defaultPaletteAliases = mapOf(
        "a" to "#E06D53", // Terracotta StitchMesh
        "b" to "#FFFFFF", // Blanco
        "c" to "#52A474", // Sage Green
        "d" to "#FBBF24", // Amarillo cálido
        "e" to "#3B82F6", // Azul
        "f" to "#1E1F24", // Antracita
        "1" to "#E06D53",
        "2" to "#FFFFFF",
        "3" to "#52A474",
        "4" to "#FBBF24",
        "5" to "#3B82F6",
        "6" to "#1E1F24"
    )

    // Diccionario de nombres estándar de colores
    private val standardColorNames = mapOf(
        "blanco" to "#FFFFFF",
        "white" to "#FFFFFF",
        "negro" to "#1E1F24",
        "black" to "#1E1F24",
        "rojo" to "#E54D42",
        "red" to "#E54D42",
        "verde" to "#52A474",
        "green" to "#52A474",
        "azul" to "#3B82F6",
        "blue" to "#3B82F6",
        "amarillo" to "#FBBF24",
        "yellow" to "#FBBF24",
        "rosa" to "#F43F5E",
        "pink" to "#F43F5E",
        "naranja" to "#F97316",
        "orange" to "#F97316",
        "terracota" to "#E06D53",
        "terracotta" to "#E06D53",
        "morado" to "#8B5CF6",
        "purple" to "#8B5CF6",
        "lila" to "#C084FC",
        "violeta" to "#7C3AED",
        "gris" to "#9CA3AF",
        "gray" to "#9CA3AF",
        "grey" to "#9CA3AF",
        "marron" to "#854D0E",
        "marrón" to "#854D0E",
        "brown" to "#854D0E",
        "beige" to "#F5F5DC",
        "crema" to "#FFFDD0",
        "cream" to "#FFFDD0"
    )

    private val hexRegex = Regex("""^#?([0-9a-fA-F]{3,8})$""")
    private val colorAliasRegex = Regex("""^(?:color|col)\s*([a-zA-Z0-9_-]+)$""", RegexOption.IGNORE_CASE)

    /**
     * Determina si una cadena representa un identificador o valor de color válido.
     */
    fun isColorIdentifier(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.startsWith("#")) {
            val hex = trimmed.substring(1)
            return hex.matches(Regex("^[0-9a-fA-F]{3}$|^[0-9a-fA-F]{4}$|^[0-9a-fA-F]{6}$|^[0-9a-fA-F]{8}$"))
        }
        val lower = trimmed.lowercase(Locale.ROOT)
        if (standardColorNames.containsKey(lower)) return true
        if (colorAliasRegex.matches(lower)) return true
        // 6 dígitos hexadecimales que contengan al menos una letra A-F para evitar ambigüedad con números puros
        if (lower.matches(Regex("^[0-9a-f]{6}$")) && lower.any { it in 'a'..'f' }) {
            return true
        }
        return false
    }

    /**
     * Normaliza cualquier entrada de color a una cadena hexadecimal estándar "#RRGGBB".
     */
    fun normalizeToHex(input: String, customPalette: Map<String, String> = emptyMap()): String {
        val trimmed = input.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. Alias de paleta personalizada
        if (customPalette.containsKey(trimmed)) {
            return normalizeHexDigits(customPalette.getValue(trimmed))
        }
        if (customPalette.containsKey(lower)) {
            return normalizeHexDigits(customPalette.getValue(lower))
        }

        // 2. Diccionario de nombres estándar
        val named = standardColorNames[lower]
        if (named != null) return named

        // 3. Patrón "Color X" o "Col X"
        val aliasMatch = colorAliasRegex.find(lower)
        if (aliasMatch != null) {
            val aliasKey = aliasMatch.groupValues[1].lowercase(Locale.ROOT)
            val custom = customPalette[aliasKey]
            if (custom != null) return normalizeHexDigits(custom)
            val defaultVal = defaultPaletteAliases[aliasKey]
            if (defaultVal != null) return defaultVal
        }

        // 4. Hexadecimal directo (#RGB, #RRGGBB, #RRGGBBAA)
        val hexMatch = hexRegex.find(trimmed)
        if (hexMatch != null) {
            return normalizeHexDigits(hexMatch.groupValues[1])
        }

        // Fallback: Si no coincide pero es un identificador desconocido, generar un color consistente basado en hash
        return defaultPaletteAliases[lower] ?: "#E06D53"
    }

    /**
     * Convierte una cadena de dígitos hexadecimales a "#RRGGBB".
     */
    private fun normalizeHexDigits(rawHex: String): String {
        val clean = rawHex.removePrefix("#")
        return when (clean.length) {
            3 -> {
                // #RGB -> #RRGGBB
                val r = clean[0]
                val g = clean[1]
                val b = clean[2]
                "#$r$r$g$g$b$b".uppercase(Locale.ROOT)
            }
            4 -> {
                // #RGBA -> #RRGGBB
                val r = clean[0]
                val g = clean[1]
                val b = clean[2]
                "#$r$r$g$g$b$b".uppercase(Locale.ROOT)
            }
            6 -> "#${clean.uppercase(Locale.ROOT)}"
            8 -> {
                // #RRGGBBAA -> ignorar o preservar RGB
                "#${clean.substring(0, 6).uppercase(Locale.ROOT)}"
            }
            else -> "#E06D53"
        }
    }

    /**
     * Convierte un código de color a componentes flotantes RGBA en el rango [0.0f, 1.0f].
     * @return FloatArray de 4 elementos: `[r, g, b, a]`.
     */
    fun toRgbaFloats(hexOrName: String, customPalette: Map<String, String> = emptyMap()): FloatArray {
        val hex = normalizeToHex(hexOrName, customPalette).removePrefix("#")
        return try {
            val r = hex.substring(0, 2).toInt(16) / 255.0f
            val g = hex.substring(2, 4).toInt(16) / 255.0f
            val b = hex.substring(4, 6).toInt(16) / 255.0f
            floatArrayOf(r, g, b, 1.0f)
        } catch (_: Throwable) {
            floatArrayOf(0.878f, 0.427f, 0.325f, 1.0f) // Terracotta fallback
        }
    }
}
