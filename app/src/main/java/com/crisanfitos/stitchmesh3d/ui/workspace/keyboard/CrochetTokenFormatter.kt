package com.crisanfitos.stitchmesh3d.ui.workspace.keyboard

/**
 * Utilidad para el formateo inteligente de inserción y borrado de tokens de crochet
 * desde el teclado virtual rápido.
 */
object CrochetTokenFormatter {

    private val noSpacePrefixTokens = setOf(",", ")", "]", "*")
    private val noSpaceSuffixEnds = setOf('(', '[')

    /**
     * Inserta un token en el texto actual añadiendo espacios inteligentemente
     * según los delimitadores técnicos de crochet.
     */
    fun insertToken(currentText: String, token: String): String {
        val effectiveToken = if (token == "( )") "()" else token
        return when {
            currentText.isEmpty() -> effectiveToken
            currentText.endsWith(" ") -> currentText + effectiveToken
            effectiveToken in noSpacePrefixTokens -> currentText + effectiveToken
            currentText.last() in noSpaceSuffixEnds -> currentText + effectiveToken
            else -> "$currentText $effectiveToken"
        }
    }

    /**
     * Inserta un token en una posición de cursor arbitraria dentro de la cadena,
     * retornando el texto resultante y la nueva posición sugerida del cursor.
     */
    fun insertTokenAtCursor(currentText: String, token: String, cursorPosition: Int): Pair<String, Int> {
        val safeCursor = cursorPosition.coerceIn(0, currentText.length)
        val prefix = currentText.substring(0, safeCursor)
        val suffix = currentText.substring(safeCursor)

        val inserted = if (token == "( )") "()" else token
        val separatorBefore = if (prefix.isNotEmpty() && !prefix.endsWith(" ") && inserted !in noSpacePrefixTokens && !noSpaceSuffixEnds.contains(prefix.last())) " " else ""
        val separatorAfter = if (suffix.isNotEmpty() && !suffix.startsWith(" ") && suffix.first() !in noSpacePrefixTokens.map { it.first() }) "" else ""

        val newText = "$prefix$separatorBefore$inserted$separatorAfter$suffix"
        val newCursor = prefix.length + separatorBefore.length + if (token == "( )") 1 else inserted.length
        return Pair(newText, newCursor)
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

    /**
     * Inserta un token reemplazando la selección actual [selectionStart, selectionEnd)
     * y devuelve el nuevo texto junto con la posición del cursor resultante (SM-062).
     */
    fun insertTokenReplacingSelection(
        currentText: String,
        token: String,
        selectionStart: Int,
        selectionEnd: Int
    ): Pair<String, Int> {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, currentText.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, currentText.length)
        val withoutSelection = currentText.substring(0, start) + currentText.substring(end)
        return insertTokenAtCursor(withoutSelection, token, start)
    }

    /**
     * Borrado con semántica de cursor: elimina la selección si existe, o el carácter previo
     * al cursor en caso contrario. Devuelve el texto y la nueva posición del cursor (SM-062).
     */
    fun backspaceAt(currentText: String, selectionStart: Int, selectionEnd: Int): Pair<String, Int> {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, currentText.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(0, currentText.length)
        if (start != end) {
            return Pair(currentText.substring(0, start) + currentText.substring(end), start)
        }
        return deleteCharBeforeCursor(currentText, start)
    }

    /**
     * Elimina el carácter anterior a una posición específica de cursor.
     */
    fun deleteCharBeforeCursor(currentText: String, cursorPosition: Int): Pair<String, Int> {
        val safeCursor = cursorPosition.coerceIn(0, currentText.length)
        if (safeCursor == 0 || currentText.isEmpty()) {
            return Pair(currentText, 0)
        }
        val prefix = currentText.substring(0, safeCursor - 1)
        val suffix = currentText.substring(safeCursor)
        return Pair("$prefix$suffix", safeCursor - 1)
    }
}
