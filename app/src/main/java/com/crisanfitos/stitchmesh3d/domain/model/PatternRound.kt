package com.crisanfitos.stitchmesh3d.domain.model

/**
 * Modelo de dominio para una vuelta o hilera en el patrón de una parte.
 */
data class PatternRound(
    val id: String,
    val partId: String,
    val roundNumber: Int,
    val rawInstruction: String,
    val colorHex: String = "#E06D53",
    val consumedStitches: Int = 0,
    val producedStitches: Int = 0,
    val declaredStitches: Int? = null,
    val isValid: Boolean = false,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
