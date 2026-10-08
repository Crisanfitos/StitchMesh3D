package com.crisanfitos.stitchmesh3d.core.engine.parser

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.engine.model.TopologyFlag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrochetParserTest {

    @Test
    fun `parses and flattens standard bracket repeat expression`() {
        val result = CrochetParser.parse("V3: [1 pb, 1 aum] * 6 (18)")

        assertTrue(result.isSuccess)
        val ast = result.ast
        assertNotNull(ast)
        assertEquals(3, ast?.header?.startRound)
        assertEquals(18, ast?.declaredCount)

        // Verificación de estructura del AST
        assertEquals(1, ast?.children?.size)
        assertTrue(ast?.children?.get(0) is CrochetAstNode.RepeatNode)
        val repeatNode = ast?.children?.get(0) as CrochetAstNode.RepeatNode
        assertEquals(6, repeatNode.times)
        assertEquals(2, repeatNode.children.size)

        // Verificación de aplanado secuencial (12 puntadas atómicas)
        val flat = ast.flatten()
        assertEquals(12, flat.size)

        // Verificación de recuentos matemáticos formales
        assertEquals(12, ast.totalConsumedStitches)
        assertEquals(18, ast.totalProducedStitches)
        assertEquals(6, ast.deltaStitches)
    }

    @Test
    fun `parses isolated repeat without explicit stitch counts`() {
        val result = CrochetParser.parse("V2: [aum] * 6 (12)")

        assertTrue(result.isSuccess)
        val ast = result.ast
        assertNotNull(ast)

        val flat = ast?.flatten() ?: emptyList()
        assertEquals(6, flat.size)
        assertTrue(flat.all { it.stitchType is StitchType.Increase })

        assertEquals(6, ast?.totalConsumedStitches)
        assertEquals(12, ast?.totalProducedStitches)
        assertEquals(6, ast?.deltaStitches)
    }

    @Test
    fun `parses mixed line with linear stitches and repeat blocks`() {
        val result = CrochetParser.parse("V4: 2 pb, [1 pb, 1 aum] * 4, 2 pb (18)")

        assertTrue(result.isSuccess)
        val ast = result.ast
        assertNotNull(ast)

        // Children: [StitchNode(pb, 2), RepeatNode(4), StitchNode(pb, 2)]
        assertEquals(3, ast?.children?.size)

        val flat = ast?.flatten() ?: emptyList()
        // 2 + (2 * 4) + 2 = 12 puntadas elementales
        assertEquals(12, flat.size)

        // Consumidos: 2 + (1+1)*4 + 2 = 12
        // Producidos: 2 + (1+2)*4 + 2 = 16. Delta: +4
        assertEquals(12, ast?.totalConsumedStitches)
        assertEquals(16, ast?.totalProducedStitches)
        assertEquals(4, ast?.deltaStitches)
    }

    @Test
    fun `parses prefix multiplication syntax 6 times brackets`() {
        val result = CrochetParser.parse("R3: 6 * [1 sc, 1 inc] (18)")

        assertTrue(result.isSuccess)
        val ast = result.ast
        assertNotNull(ast)

        val flat = ast?.flatten() ?: emptyList()
        assertEquals(12, flat.size)
        assertEquals(12, ast?.totalConsumedStitches)
        assertEquals(18, ast?.totalProducedStitches)
    }

    @Test
    fun `applies BLO modifier to stitch in AST preserving count`() {
        val result = CrochetParser.parse("V5: BLO 2 pb, 4 pb (6)")

        assertTrue(result.isSuccess)
        val ast = result.ast
        assertNotNull(ast)

        val flat = ast?.flatten() ?: emptyList()
        assertEquals(6, flat.size)

        // Primeras 2 puntadas deben tener la bandera BLO
        assertTrue(flat[0].topologyFlags.contains(TopologyFlag.BLO))
        assertTrue(flat[1].topologyFlags.contains(TopologyFlag.BLO))

        // Las siguientes 4 no deben tener BLO
        assertFalse(flat[2].topologyFlags.contains(TopologyFlag.BLO))
        assertFalse(flat[5].topologyFlags.contains(TopologyFlag.BLO))
    }

    @Test
    fun `reports syntax error on unclosed bracket`() {
        val result = CrochetParser.parse("V1: [1 pb, 1 aum (6)")
        assertFalse(result.isSuccess)
        assertTrue(result.errors.any { it.message.contains("Falta corchete de cierre") })
    }

    @Test
    fun `reports syntax error when multiplier is missing count`() {
        val result = CrochetParser.parse("V1: [1 pb] * (4)")
        assertFalse(result.isSuccess)
        assertTrue(result.errors.any { it.message.contains("sin número de repeticiones") })
    }

    @Test
    fun `reports syntax error on unrecognized symbol`() {
        val result = CrochetParser.parse("V1: 3 pb, invalidToken (3)")
        assertFalse(result.isSuccess)
        assertTrue(result.errors.any { it.message.contains("no reconocido") })
    }
}
