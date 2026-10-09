package com.crisanfitos.stitchmesh3d.core.engine.lexer

import com.crisanfitos.stitchmesh3d.core.engine.color.CrochetColorHelper
import com.crisanfitos.stitchmesh3d.core.engine.model.StitchRegistry
import com.crisanfitos.stitchmesh3d.core.engine.model.TopologyFlag
import java.util.Locale

/**
 * Lexer / Tokenizer multilingüe para instrucciones de tejido de crochet y amigurumi.
 * Implementa la especificación RF-1.1 y TRD §4.
 */
object CrochetLexer {

    // Regex para prefijo de cabecera de vuelta o rango de vueltas
    // Ejemplos: "V1:", "Vuelta 1:", "R1:", "Round 1:", "V3-V8:", "V3-8:", "Vueltas 3 a 8:", "Rounds 3-5:"
    private val roundHeaderRegex = Regex(
        """^\s*(?:vueltas?|rounds?|vtas?|[vr])\s*(\d+)(?:\s*(?:[-–]|a)\s*(?:[vr]|vuelta|round)?\s*(\d+))?\s*[:.-]?\s*""",
        RegexOption.IGNORE_CASE
    )

    // Regex para recuento declarado final entre paréntesis o corchetes
    // Ejemplos: "(12)", "( 18 pts )", "(24 puntos)", "(30 sts)", "(approx. 12)", "[12]"
    private val declaredCountRegex = Regex(
        """\s*(?:[\(\[]\s*(?:approx\.?|aprox\.?)?\s*(\d+)\s*(?:pts?|puntos?|sts?|stitches?)?\s*[\)\]])\s*$""",
        RegexOption.IGNORE_CASE
    )

    // Regex para prefijo de etiqueta de color (ej. "Color A:", "Col 1:")
    private val colorLabelPrefixRegex = Regex(
        """^(?:color|col)\s*([a-zA-Z0-9_-]+)\s*:""",
        RegexOption.IGNORE_CASE
    )

    // Regex para token de Anillo Mágico: "AM [6]", "MR [6]", "AM 6", "MR 8"
    private val magicRingTokenRegex = Regex(
        """^(?:am|mr)\s*\[?(\d+)\]?""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Analiza una línea de texto de vuelta y genera su representación tokenizada.
     */
    fun tokenize(rawLine: String): LexerResult {
        if (rawLine.isBlank()) {
            return LexerResult(roundHeader = null, tokens = emptyList(), declaredCount = null, rawLine = rawLine)
        }

        var startIndex = 0
        var endIndex = rawLine.length
        var header: CrochetToken.RoundHeader? = null
        var declaredCount: Int? = null

        // 1. Extraer encabezado de vuelta si existe al inicio
        val headerMatch = roundHeaderRegex.find(rawLine)
        if (headerMatch != null) {
            val startRound = headerMatch.groupValues[1].toInt()
            val endRound = if (headerMatch.groupValues[2].isNotEmpty()) {
                headerMatch.groupValues[2].toInt()
            } else {
                startRound
            }
            header = CrochetToken.RoundHeader(
                startRound = startRound,
                endRound = endRound,
                raw = headerMatch.value.trim(),
                startIndex = headerMatch.range.first,
                endIndex = headerMatch.range.last + 1
            )
            startIndex = headerMatch.range.last + 1
        }

        // 2. Extraer recuento final declarado si existe al final
        val declaredMatch = declaredCountRegex.find(rawLine)
        if (declaredMatch != null && declaredMatch.range.first >= startIndex) {
            declaredCount = declaredMatch.groupValues[1].toInt()
            endIndex = declaredMatch.range.first
        }

        // 3. Tokenizar el cuerpo de la instrucción entre startIndex y endIndex
        val bodyText = rawLine.substring(startIndex, endIndex)
        val tokens = tokenizeBody(bodyText, offset = startIndex)

        // Si se extrajo un recuento declarado, agregarlo al final como token explícito
        val allTokens = if (declaredMatch != null && declaredCount != null) {
            tokens + CrochetToken.DeclaredCount(
                count = declaredCount,
                startIndex = declaredMatch.range.first,
                endIndex = declaredMatch.range.last + 1
            )
        } else {
            tokens
        }

        return LexerResult(
            roundHeader = header,
            tokens = allTokens,
            declaredCount = declaredCount,
            rawLine = rawLine
        )
    }

    private fun tokenizeBody(text: String, offset: Int): List<CrochetToken> {
        val tokens = mutableListOf<CrochetToken>()
        var cursor = 0
        val len = text.length

        while (cursor < len) {
            val char = text[cursor]

            // Ignorar espacios en blanco
            if (char.isWhitespace()) {
                cursor++
                continue
            }

            val absoluteIndex = offset + cursor

            // Detección de prefijo de etiqueta de color: "Color A:", "Col 1:"
            val colorLabelMatch = colorLabelPrefixRegex.find(text.substring(cursor))
            if (colorLabelMatch != null) {
                val colorVal = colorLabelMatch.groupValues[1]
                val raw = colorLabelMatch.value
                val matchLen = raw.length
                tokens.add(
                    CrochetToken.ColorToken(
                        hexOrName = CrochetColorHelper.normalizeToHex(colorVal),
                        raw = raw,
                        startIndex = absoluteIndex,
                        endIndex = absoluteIndex + matchLen
                    )
                )
                cursor += matchLen
                continue
            }

            // Detección de código hexadecimal directo: "#FFFFFF", "#E06D53"
            if (char == '#') {
                val hexStart = cursor
                cursor++
                while (cursor < len && text[cursor].isLetterOrDigit()) {
                    cursor++
                }
                val hexCandidate = text.substring(hexStart, cursor)
                if (CrochetColorHelper.isColorIdentifier(hexCandidate)) {
                    tokens.add(
                        CrochetToken.ColorToken(
                            hexOrName = CrochetColorHelper.normalizeToHex(hexCandidate),
                            raw = hexCandidate,
                            startIndex = absoluteIndex,
                            endIndex = offset + cursor
                        )
                    )
                    continue
                } else {
                    cursor = hexStart + 1
                }
            }

            // Comas y delimitadores
            when (char) {
                ',' -> {
                    tokens.add(CrochetToken.Comma(absoluteIndex, absoluteIndex + 1))
                    cursor++
                    continue
                }
                '[' -> {
                    val closeIdx = text.indexOf(']', cursor)
                    if (closeIdx != -1) {
                        val inner = text.substring(cursor + 1, closeIdx).trim()
                        if (CrochetColorHelper.isColorIdentifier(inner)) {
                            val raw = text.substring(cursor, closeIdx + 1)
                            tokens.add(
                                CrochetToken.ColorToken(
                                    hexOrName = CrochetColorHelper.normalizeToHex(inner),
                                    raw = raw,
                                    startIndex = absoluteIndex,
                                    endIndex = offset + closeIdx + 1
                                )
                            )
                            cursor = closeIdx + 1
                            continue
                        }
                    }
                    tokens.add(CrochetToken.BracketOpen(isSquare = true, absoluteIndex, absoluteIndex + 1))
                    cursor++
                    continue
                }
                ']' -> {
                    tokens.add(CrochetToken.BracketClose(isSquare = true, absoluteIndex, absoluteIndex + 1))
                    cursor++
                    continue
                }
                '(' -> {
                    val closeIdx = text.indexOf(')', cursor)
                    if (closeIdx != -1) {
                        val inner = text.substring(cursor + 1, closeIdx).trim()
                        if (CrochetColorHelper.isColorIdentifier(inner)) {
                            val raw = text.substring(cursor, closeIdx + 1)
                            tokens.add(
                                CrochetToken.ColorToken(
                                    hexOrName = CrochetColorHelper.normalizeToHex(inner),
                                    raw = raw,
                                    startIndex = absoluteIndex,
                                    endIndex = offset + closeIdx + 1
                                )
                            )
                            cursor = closeIdx + 1
                            continue
                        }
                    }
                    tokens.add(CrochetToken.BracketOpen(isSquare = false, absoluteIndex, absoluteIndex + 1))
                    cursor++
                    continue
                }
                ')' -> {
                    tokens.add(CrochetToken.BracketClose(isSquare = false, absoluteIndex, absoluteIndex + 1))
                    cursor++
                    continue
                }
                '*' -> {
                    tokens.add(CrochetToken.Multiply(absoluteIndex, absoluteIndex + 1))
                    cursor++
                    continue
                }
            }

            // Operador 'x' como multiplicador si está seguido de espacio o número (ej. "x 6")
            if ((char == 'x' || char == 'X') && (cursor + 1 < len && (text[cursor + 1].isWhitespace() || text[cursor + 1].isDigit()))) {
                tokens.add(CrochetToken.Multiply(absoluteIndex, absoluteIndex + 1))
                cursor++
                continue
            }

            // Dígitos / Números
            if (char.isDigit()) {
                val numStart = cursor
                while (cursor < len && text[cursor].isDigit()) {
                    cursor++
                }
                val numStr = text.substring(numStart, cursor)
                val numVal = numStr.toInt()
                tokens.add(CrochetToken.Number(numVal, offset + numStart, offset + cursor))
                continue
            }

            // Anillo Mágico (AM [6], MR 6, etc.)
            val remaining = text.substring(cursor)
            val mrMatch = magicRingTokenRegex.find(remaining)
            if (mrMatch != null) {
                val mrText = mrMatch.value
                val count = mrMatch.groupValues[1].toInt()
                val stitch = StitchRegistry.resolve(mrText)
                if (stitch != null) {
                    tokens.add(
                        CrochetToken.StitchToken(
                            stitchType = stitch,
                            rawSymbol = mrText,
                            startIndex = absoluteIndex,
                            endIndex = absoluteIndex + mrText.length
                        )
                    )
                    cursor += mrText.length
                    continue
                }
            }

            // Modificadores de bucle y palabras de puntada
            val wordStart = cursor
            while (cursor < len && (text[cursor].isLetterOrDigit() || text[cursor] == '-' || text[cursor] == '_')) {
                cursor++
            }
            val word = text.substring(wordStart, cursor)

            if (word.equals("blo", ignoreCase = true)) {
                tokens.add(CrochetToken.ModifierToken(TopologyFlag.BLO, word, offset + wordStart, offset + cursor))
                continue
            }
            if (word.equals("flo", ignoreCase = true)) {
                tokens.add(CrochetToken.ModifierToken(TopologyFlag.FLO, word, offset + wordStart, offset + cursor))
                continue
            }

            // Palabras clave de multiplicación: "veces", "times"
            if (word.equals("veces", ignoreCase = true) || word.equals("times", ignoreCase = true)) {
                tokens.add(CrochetToken.Multiply(offset + wordStart, offset + cursor))
                continue
            }

            // Intentar resolver primero palabras compuestas de 2 palabras (ej. "sl st", "sc2tog", "dc inc")
            val candidateTwoWords = if (cursor < len && text[cursor].isWhitespace()) {
                val nextStart = cursor + 1
                var nextEnd = nextStart
                while (nextEnd < len && (text[nextEnd].isLetterOrDigit() || text[nextEnd] == '-' || text[nextEnd] == '_')) {
                    nextEnd++
                }
                if (nextEnd > nextStart) {
                    val twoWords = "$word ${text.substring(nextStart, nextEnd)}"
                    val stitch = StitchRegistry.resolve(twoWords)
                    if (stitch != null) {
                        tokens.add(
                            CrochetToken.StitchToken(
                                stitchType = stitch,
                                rawSymbol = twoWords,
                                startIndex = offset + wordStart,
                                endIndex = offset + nextEnd
                            )
                        )
                        cursor = nextEnd
                        twoWords
                    } else null
                } else null
            } else null

            if (candidateTwoWords != null) {
                continue
            }

            // Intentar resolver palabra simple con StitchRegistry
            val singleStitch = StitchRegistry.resolve(word)
            if (singleStitch != null) {
                tokens.add(
                    CrochetToken.StitchToken(
                        stitchType = singleStitch,
                        rawSymbol = word,
                        startIndex = offset + wordStart,
                        endIndex = offset + cursor
                    )
                )
                continue
            }

            // Si no se reconoció, emitir Unknown
            tokens.add(CrochetToken.Unknown(word, offset + wordStart, offset + cursor))
        }

        return tokens
    }
}
