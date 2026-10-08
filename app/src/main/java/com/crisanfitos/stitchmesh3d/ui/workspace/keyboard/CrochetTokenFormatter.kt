package com.crisanfitos.stitchmesh3d.ui.workspace.keyboard

/**
 * Utilidad para el formateo inteligente de inserción y borrado de tokens de crochet
 * desde el teclado virtual rápido.
 */
object CrochetTokenFormatter {

    /**
     * Inserta un token en el texto actual añadiendo espacios inteligentemente
     * según los delimitadores técnicos de crochet.
     */
    fun insertToken(currentText: String, token: String): String {
        val noSpacePrefixTokens = setOf(",", ")", "]", "*")
        val noSpaceSuffixEnds = setOf('(', '[')

        return when {
            currentText.isEmpty() -> token
            currentText.endsWith(" ") -> currentText + token
            token in noSpacePrefixTokens -> currentText + token
            currentText.last() in noSpaceSuffixEnds -> currentText + token
            else -> "$currentText $token"
        }
    }

    /**
     * Elimina el último carácter del texto actual.
     */
    fun deleteLastChar(currentText: String): String {
        return if (currentText.isNotEmpty()) {
            currentText.dropLast(1)
        } else {
            ""
        }
    }
}
