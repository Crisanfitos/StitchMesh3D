package com.crisanfitos.stitchmesh3d.core.gauge.model

/**
 * Categorías estándar de grosor de hilado según el Craft Yarn Council (CYC) #0 a #7.
 * Utilizado para el cálculo y calibración de tensión dimensional del tejido.
 *
 * @property code Código numérico CYC (0 a 7).
 * @property displayName Nombre comercial canónico.
 * @property minHookSizeMm Calibre mínimo recomendado de aguja en mm.
 * @property maxHookSizeMm Calibre máximo recomendado de aguja en mm.
 */
enum class YarnWeightCategory(
    val code: Int,
    val displayName: String,
    val minHookSizeMm: Float,
    val maxHookSizeMm: Float
) {
    LACE(0, "Lace / #0", 1.50f, 2.25f),
    SUPER_FINE(1, "Super Fine / #1", 2.25f, 3.25f),
    FINE(2, "Fine / Sport / #2", 3.25f, 3.75f),
    LIGHT(3, "Light / DK / #3", 3.75f, 4.50f),
    MEDIUM(4, "Worsted / Medium / #4", 4.50f, 5.50f),
    BULKY(5, "Bulky / #5", 5.50f, 6.50f),
    SUPER_BULKY(6, "Super Bulky / #6", 6.50f, 9.00f),
    JUMBO(7, "Jumbo / #7", 9.00f, 15.00f);

    companion object {
        fun fromCode(code: Int): YarnWeightCategory {
            return entries.firstOrNull { it.code == code } ?: MEDIUM
        }
    }
}
