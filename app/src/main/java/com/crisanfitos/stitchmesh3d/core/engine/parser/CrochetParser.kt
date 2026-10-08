package com.crisanfitos.stitchmesh3d.core.engine.parser

import com.crisanfitos.stitchmesh3d.core.engine.lexer.CrochetLexer
import com.crisanfitos.stitchmesh3d.core.engine.lexer.CrochetToken
import com.crisanfitos.stitchmesh3d.core.engine.lexer.LexerResult
import com.crisanfitos.stitchmesh3d.core.engine.model.TopologyFlag

/**
 * Parser sintáctico para convertir la corriente de tokens de [CrochetLexer] en un [CrochetAstNode.RoundNode].
 * Maneja repeticiones con corchetes/paréntesis, multiplicadores ('* N', 'x N'), modificadores y puntadas.
 */
object CrochetParser {

    /**
     * Parsea directamente una línea de texto de vuelta.
     */
    fun parse(rawLine: String): ParseResult {
        val lexerResult = CrochetLexer.tokenize(rawLine)
        return parse(lexerResult)
    }

    /**
     * Parsea los tokens contenidos en un [LexerResult].
     */
    fun parse(lexerResult: LexerResult): ParseResult {
        val errors = mutableListOf<ParseError>()

        // 1. Verificar tokens desconocidos reportados por el lexer
        for (unknown in lexerResult.unknownTokens) {
            errors.add(
                ParseError(
                    message = "Símbolo de crochet no reconocido: '${unknown.raw}'",
                    startIndex = unknown.startIndex,
                    endIndex = unknown.endIndex
                )
            )
        }

        // 2. Filtrar comas y el conteo declarado final para la construcción del árbol
        val tokens = lexerResult.tokens.filter { it !is CrochetToken.Comma && it !is CrochetToken.DeclaredCount }

        var cursor = 0
        val nodes = mutableListOf<CrochetAstNode>()

        while (cursor < tokens.size) {
            val token = tokens[cursor]

            when (token) {
                // Caso A: Prefijo multiplicador antes de grupo (ej. "6 * [1 pb, 1 aum]")
                is CrochetToken.Number -> {
                    if (cursor + 2 < tokens.size && tokens[cursor + 1] is CrochetToken.Multiply && tokens[cursor + 2] is CrochetToken.BracketOpen) {
                        val times = token.value
                        val openToken = tokens[cursor + 2] as CrochetToken.BracketOpen
                        cursor += 3
                        val (groupNodes, newCursor, groupErr) = parseGroup(tokens, cursor, openToken.isSquare)
                        if (groupErr != null) errors.add(groupErr)
                        cursor = newCursor
                        nodes.add(CrochetAstNode.RepeatNode(children = groupNodes, times = times))
                        continue
                    }

                    // Caso B: Número que precede a una puntada (ej. "3 pb", "1 aum")
                    // O a un modificador seguido de puntada (ej. "2 BLO pb")
                    val count = token.value
                    cursor++
                    if (cursor < tokens.size) {
                        val next = tokens[cursor]
                        if (next is CrochetToken.ModifierToken && cursor + 1 < tokens.size && tokens[cursor + 1] is CrochetToken.StitchToken) {
                            val stitchToken = tokens[cursor + 1] as CrochetToken.StitchToken
                            nodes.add(CrochetAstNode.StitchNode(stitchToken.stitchType.withTopology(next.flag), count = count))
                            cursor += 2
                        } else if (next is CrochetToken.StitchToken) {
                            nodes.add(CrochetAstNode.StitchNode(next.stitchType, count = count))
                            cursor++
                        } else {
                            errors.add(ParseError("Número $count no está seguido de un tipo de puntada", token.startIndex, next.endIndex))
                        }
                    } else {
                        errors.add(ParseError("Número $count al final sin puntada asociada", token.startIndex, token.endIndex))
                    }
                }

                // Caso C: Modificador que precede a número y/o puntada (ej. "BLO 2 pb", "BLO pb")
                is CrochetToken.ModifierToken -> {
                    val flag = token.flag
                    cursor++
                    if (cursor < tokens.size) {
                        val next = tokens[cursor]
                        if (next is CrochetToken.Number && cursor + 1 < tokens.size && tokens[cursor + 1] is CrochetToken.StitchToken) {
                            val count = next.value
                            val stitchToken = tokens[cursor + 1] as CrochetToken.StitchToken
                            nodes.add(CrochetAstNode.StitchNode(stitchToken.stitchType.withTopology(flag), count = count))
                            cursor += 2
                        } else if (next is CrochetToken.StitchToken) {
                            nodes.add(CrochetAstNode.StitchNode(next.stitchType.withTopology(flag), count = 1))
                            cursor++
                        } else {
                            errors.add(ParseError("Modificador ${token.raw} sin puntada asociada", token.startIndex, next.endIndex))
                        }
                    } else {
                        errors.add(ParseError("Modificador ${token.raw} al final de la línea", token.startIndex, token.endIndex))
                    }
                }

                // Caso D: Puntada aislada sin número previo (ej. "aum", "pb", "AM [6]") -> count = 1
                is CrochetToken.StitchToken -> {
                    nodes.add(CrochetAstNode.StitchNode(token.stitchType, count = 1))
                    cursor++
                }

                // Caso E: Apertura de grupo: '[' o '(' (ej. "[1 pb, 1 aum] * 6")
                is CrochetToken.BracketOpen -> {
                    cursor++
                    val (groupNodes, newCursor, groupErr) = parseGroup(tokens, cursor, token.isSquare)
                    if (groupErr != null) errors.add(groupErr)
                    cursor = newCursor

                    // Comprobar si hay multiplicador a continuación ('* N', 'x N' o 'N')
                    var times = 1
                    if (cursor < tokens.size) {
                        val after = tokens[cursor]
                        if (after is CrochetToken.Multiply) {
                            cursor++
                            if (cursor < tokens.size && tokens[cursor] is CrochetToken.Number) {
                                times = (tokens[cursor] as CrochetToken.Number).value
                                cursor++
                            } else {
                                errors.add(ParseError("Operador de multiplicación '*' sin número de repeticiones", after.startIndex, after.endIndex))
                            }
                        } else if (after is CrochetToken.Number) {
                            times = after.value
                            cursor++
                        }
                    }

                    if (times > 1) {
                        nodes.add(CrochetAstNode.RepeatNode(children = groupNodes, times = times))
                    } else {
                        nodes.addAll(groupNodes)
                    }
                }

                is CrochetToken.BracketClose -> {
                    errors.add(ParseError("Cierre de corchete o paréntesis inesperado", token.startIndex, token.endIndex))
                    cursor++
                }

                is CrochetToken.Multiply -> {
                    errors.add(ParseError("Operador de multiplicación '*' fuera de contexto", token.startIndex, token.endIndex))
                    cursor++
                }

                is CrochetToken.Unknown -> {
                    cursor++
                }

                else -> {
                    cursor++
                }
            }
        }

        val roundAst = if (errors.isEmpty() || nodes.isNotEmpty()) {
            CrochetAstNode.RoundNode(
                header = lexerResult.roundHeader,
                children = nodes,
                declaredCount = lexerResult.declaredCount,
                rawLine = lexerResult.rawLine
            )
        } else {
            null
        }

        return ParseResult(ast = roundAst, errors = errors)
    }

    private data class GroupResult(
        val nodes: List<CrochetAstNode>,
        val newCursor: Int,
        val error: ParseError?
    )

    private fun parseGroup(tokens: List<CrochetToken>, startCursor: Int, isSquare: Boolean): GroupResult {
        var cursor = startCursor
        val nodes = mutableListOf<CrochetAstNode>()

        while (cursor < tokens.size) {
            val token = tokens[cursor]

            if (token is CrochetToken.BracketClose && token.isSquare == isSquare) {
                return GroupResult(nodes = nodes, newCursor = cursor + 1, error = null)
            }

            // Subgrupo recursivo
            if (token is CrochetToken.BracketOpen) {
                cursor++
                val (innerNodes, newCursor, innerErr) = parseGroup(tokens, cursor, token.isSquare)
                if (innerErr != null) return GroupResult(nodes, newCursor, innerErr)
                cursor = newCursor
                nodes.addAll(innerNodes)
                continue
            }

            if (token is CrochetToken.Number) {
                val count = token.value
                cursor++
                if (cursor < tokens.size) {
                    val next = tokens[cursor]
                    if (next is CrochetToken.ModifierToken && cursor + 1 < tokens.size && tokens[cursor + 1] is CrochetToken.StitchToken) {
                        val stitchToken = tokens[cursor + 1] as CrochetToken.StitchToken
                        nodes.add(CrochetAstNode.StitchNode(stitchToken.stitchType.withTopology(next.flag), count = count))
                        cursor += 2
                    } else if (next is CrochetToken.StitchToken) {
                        nodes.add(CrochetAstNode.StitchNode(next.stitchType, count = count))
                        cursor++
                    } else {
                        return GroupResult(nodes, cursor, ParseError("Número $count dentro de corchetes sin tipo de puntada", token.startIndex, next.endIndex))
                    }
                } else {
                    return GroupResult(nodes, cursor, ParseError("Número $count sin puntada al cerrar grupo", token.startIndex, token.endIndex))
                }
                continue
            }

            if (token is CrochetToken.ModifierToken) {
                val flag = token.flag
                cursor++
                if (cursor < tokens.size) {
                    val next = tokens[cursor]
                    if (next is CrochetToken.Number && cursor + 1 < tokens.size && tokens[cursor + 1] is CrochetToken.StitchToken) {
                        val count = next.value
                        val stitchToken = tokens[cursor + 1] as CrochetToken.StitchToken
                        nodes.add(CrochetAstNode.StitchNode(stitchToken.stitchType.withTopology(flag), count = count))
                        cursor += 2
                    } else if (next is CrochetToken.StitchToken) {
                        nodes.add(CrochetAstNode.StitchNode(next.stitchType.withTopology(flag), count = 1))
                        cursor++
                    } else {
                        return GroupResult(nodes, cursor, ParseError("Modificador ${token.raw} sin puntada", token.startIndex, next.endIndex))
                    }
                }
                continue
            }

            if (token is CrochetToken.Multiply) {
                return GroupResult(nodes, cursor + 1, ParseError("Operador de multiplicación '*' inesperado dentro de corchetes", token.startIndex, token.endIndex))
            }

            if (token is CrochetToken.StitchToken) {
                nodes.add(CrochetAstNode.StitchNode(token.stitchType, count = 1))
                cursor++
                continue
            }

            cursor++
        }

        // Si llegamos aquí, el corchete no fue cerrado
        val closingExpected = if (isSquare) "']'" else "')'"
        return GroupResult(nodes, cursor, ParseError("Falta corchete de cierre $closingExpected", startCursor, tokens.size))
    }
}
