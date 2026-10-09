package com.crisanfitos.stitchmesh3d.core.engine.color

import com.crisanfitos.stitchmesh3d.core.engine.lexer.CrochetLexer
import com.crisanfitos.stitchmesh3d.core.engine.lexer.CrochetToken
import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.engine.parser.CrochetParser
import com.crisanfitos.stitchmesh3d.core.engine.validator.ArithmeticValidator
import com.crisanfitos.stitchmesh3d.core.engine.validator.RoundValidationResult
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import com.crisanfitos.stitchmesh3d.core.geometry.AdaptiveTessellator
import com.crisanfitos.stitchmesh3d.core.geometry.GlbMeshBuilder
import com.crisanfitos.stitchmesh3d.core.geometry.RingProfileGenerator
import com.crisanfitos.stitchmesh3d.core.geometry.StuffingInflationConfig
import com.crisanfitos.stitchmesh3d.core.geometry.StuffingInflationFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de pruebas unitarias para el soporte de cambios de color intra-vuelta (SM-069, Jacquard / Tapestry Crochet).
 */
class CrochetColorTest {

    private val testGauge = YarnGaugeStandard(
        id = 4,
        yarnWeightCategory = YarnWeightCategory.MEDIUM,
        categoryName = "Worsted",
        hookSizeMm = 4.0f,
        stitchWidthMm = 4.2f,
        stitchHeightMm = 4.8f,
        stitchThicknessMm = 2.1f
    )

    // ==========================================
    // 1. CrochetColorHelper tests
    // ==========================================

    @Test
    fun `CrochetColorHelper normalizes hex codes accurately`() {
        assertEquals("#FFFFFF", CrochetColorHelper.normalizeToHex("#FFFFFF"))
        assertEquals("#FFFFFF", CrochetColorHelper.normalizeToHex("#FFF"))
        assertEquals("#E06D53", CrochetColorHelper.normalizeToHex("#E06D53"))
        assertEquals("#1E1F24", CrochetColorHelper.normalizeToHex("#1e1f24"))
    }

    @Test
    fun `CrochetColorHelper resolves named colors in Spanish and English`() {
        assertEquals("#FFFFFF", CrochetColorHelper.normalizeToHex("blanco"))
        assertEquals("#FFFFFF", CrochetColorHelper.normalizeToHex("white"))
        assertEquals("#1E1F24", CrochetColorHelper.normalizeToHex("negro"))
        assertEquals("#1E1F24", CrochetColorHelper.normalizeToHex("black"))
        assertEquals("#E54D42", CrochetColorHelper.normalizeToHex("rojo"))
        assertEquals("#52A474", CrochetColorHelper.normalizeToHex("verde"))
        assertEquals("#E06D53", CrochetColorHelper.normalizeToHex("terracota"))
        assertEquals("#E06D53", CrochetColorHelper.normalizeToHex("terracotta"))
    }

    @Test
    fun `CrochetColorHelper resolves palette aliases`() {
        assertEquals("#E06D53", CrochetColorHelper.normalizeToHex("Color A"))
        assertEquals("#FFFFFF", CrochetColorHelper.normalizeToHex("Color B"))
        assertEquals("#52A474", CrochetColorHelper.normalizeToHex("Col C"))
        assertEquals("#E06D53", CrochetColorHelper.normalizeToHex("Color 1"))
        assertEquals("#FFFFFF", CrochetColorHelper.normalizeToHex("Color 2"))
    }

    @Test
    fun `CrochetColorHelper toRgbaFloats produces valid 0 to 1 float values`() {
        val white = CrochetColorHelper.toRgbaFloats("#FFFFFF")
        assertEquals(1.0f, white[0], 0.001f)
        assertEquals(1.0f, white[1], 0.001f)
        assertEquals(1.0f, white[2], 0.001f)
        assertEquals(1.0f, white[3], 0.001f)

        val black = CrochetColorHelper.toRgbaFloats("#000000")
        assertEquals(0.0f, black[0], 0.001f)
        assertEquals(0.0f, black[1], 0.001f)
        assertEquals(0.0f, black[2], 0.001f)
        assertEquals(1.0f, black[3], 0.001f)
    }

    // ==========================================
    // 2. CrochetLexer tests
    // ==========================================

    @Test
    fun `CrochetLexer tokenizes bracketed hex colors without breaking stitches`() {
        val input = "V1: [#FFFFFF] 3 pb, [#E06D53] 3 pb (6)"
        val result = CrochetLexer.tokenize(input)

        val colorTokens = result.tokens.filterIsInstance<CrochetToken.ColorToken>()
        assertEquals("Debe encontrar 2 ColorTokens", 2, colorTokens.size)
        assertEquals("#FFFFFF", colorTokens[0].hexOrName)
        assertEquals("#E06D53", colorTokens[1].hexOrName)

        val stitchTokens = result.tokens.filterIsInstance<CrochetToken.StitchToken>()
        assertEquals("Debe encontrar 2 StitchTokens", 2, stitchTokens.size)
        assertEquals(6, result.declaredCount)
    }

    @Test
    fun `CrochetLexer tokenizes parenthesized and direct hex and named colors`() {
        val input1 = "V2: (Color A) 6 pb, (Color B) 6 pb (12)"
        val res1 = CrochetLexer.tokenize(input1)
        val colors1 = res1.tokens.filterIsInstance<CrochetToken.ColorToken>()
        assertEquals(2, colors1.size)
        assertEquals("#E06D53", colors1[0].hexOrName)
        assertEquals("#FFFFFF", colors1[1].hexOrName)

        val input2 = "V3: #E06D53 12 pb (12)"
        val res2 = CrochetLexer.tokenize(input2)
        val colors2 = res2.tokens.filterIsInstance<CrochetToken.ColorToken>()
        assertEquals(1, colors2.size)
        assertEquals("#E06D53", colors2[0].hexOrName)

        val input3 = "V4: (blanco) 4 pb, (rojo) 4 pb (8)"
        val res3 = CrochetLexer.tokenize(input3)
        val colors3 = res3.tokens.filterIsInstance<CrochetToken.ColorToken>()
        assertEquals(2, colors3.size)
        assertEquals("#FFFFFF", colors3[0].hexOrName)
        assertEquals("#E54D42", colors3[1].hexOrName)
    }

    @Test
    fun `CrochetLexer does not mistake stitch repeat groups or declared counts for colors`() {
        val normalGroup = "V2: [1 pb, 1 aum] * 6 (18)"
        val res = CrochetLexer.tokenize(normalGroup)
        val colors = res.tokens.filterIsInstance<CrochetToken.ColorToken>()
        assertTrue("No debe haber ColorTokens en una instrucción normal", colors.isEmpty())
        assertEquals(1, res.tokens.filterIsInstance<CrochetToken.BracketOpen>().size)
        assertEquals(1, res.tokens.filterIsInstance<CrochetToken.BracketClose>().size)
        assertEquals(18, res.declaredCount)
    }

    // ==========================================
    // 3. CrochetParser and AST tests
    // ==========================================

    @Test
    fun `CrochetParser propagates color to AST StitchNodes and flattened instances`() {
        val line = "V1: [#FFFFFF] 3 pb, [#E06D53] 3 pb (6)"
        val parseResult = CrochetParser.parse(line)
        assertTrue("El parseo debe ser exitoso", parseResult.isSuccess)
        assertNotNull(parseResult.ast)

        val roundAst = parseResult.ast!!
        assertEquals("Total consumido debe ser 6", 6, roundAst.totalConsumedStitches)
        assertEquals("Total producido debe ser 6", 6, roundAst.totalProducedStitches)

        val flattened = roundAst.flatten()
        assertEquals("Debe tener 6 instancias", 6, flattened.size)
        assertEquals("#FFFFFF", flattened[0].colorHex)
        assertEquals("#FFFFFF", flattened[1].colorHex)
        assertEquals("#FFFFFF", flattened[2].colorHex)
        assertEquals("#E06D53", flattened[3].colorHex)
        assertEquals("#E06D53", flattened[4].colorHex)
        assertEquals("#E06D53", flattened[5].colorHex)
    }

    @Test
    fun `CrochetParser propagates colors inside repeat groups`() {
        val line = "V2: [ (Color A) 1 pb, (Color B) 1 aum ] * 3 (9)"
        val parseResult = CrochetParser.parse(line)
        assertTrue(parseResult.isSuccess)

        val roundAst = parseResult.ast!!
        val instances = roundAst.flatten()
        // Cada repetición tiene 1 pb + 1 aum = 2 puntadas (3 repeticiones = 6 instancias)
        assertEquals(6, instances.size)
        // Rep 0
        assertEquals("#E06D53", instances[0].colorHex)
        assertEquals("#FFFFFF", instances[1].colorHex)
        // Rep 1
        assertEquals("#E06D53", instances[2].colorHex)
        assertEquals("#FFFFFF", instances[3].colorHex)
        // Rep 2
        assertEquals("#E06D53", instances[4].colorHex)
        assertEquals("#FFFFFF", instances[5].colorHex)
    }

    @Test
    fun `ArithmeticValidator validates multicolor round invariants without error`() {
        val line = "V2: [#FFFFFF] 6 pb, [#E06D53] 6 aum (18)"
        // Consumidos: 6 + 6 = 12. Producidos: 6 + 12 = 18.
        val validation = ArithmeticValidator.validateRound(line, previousRoundStitches = 12)
        assertTrue("La vuelta debe ser válida aritméticamente", validation is RoundValidationResult.Valid)
    }

    // ==========================================
    // 4. Geometry and AdaptiveTessellator tests
    // ==========================================

    @Test
    fun `AdaptiveTessellator generates vertexColors when intra-round color changes exist`() {
        val parsed = CrochetParser.parse("V1: [#FFFFFF] 3 pb, [#E06D53] 3 pb (6)")
        val instances = parsed.ast!!.flatten()

        val ring = RingProfileGenerator.generateRingFromInstances(1, instances, testGauge, null)
        assertEquals("Ring debe tener 6 vértices", 6, ring.vertexCount)
        assertEquals("#FFFFFF", ring.vertices[0].colorHex)
        assertEquals("#E06D53", ring.vertices[3].colorHex)

        val mesh = AdaptiveTessellator.tessellate(listOf(ring), includePolarCap = false)
        assertTrue("vertexColors no debe estar vacío", mesh.vertexColors.isNotEmpty())
        assertEquals("vertexColors debe tener 4 floats por vértice", mesh.vertexCount * 4, mesh.vertexColors.size)

        // Vértice 0 (#FFFFFF) -> R=1.0, G=1.0, B=1.0
        val c0 = mesh.getVertexColor(0)
        assertEquals(1.0f, c0[0], 0.01f)
        assertEquals(1.0f, c0[1], 0.01f)
        assertEquals(1.0f, c0[2], 0.01f)

        // Vértice 3 (#E06D53) -> R≈0.878, G≈0.427, B≈0.325
        val c3 = mesh.getVertexColor(3)
        assertEquals(0.878f, c3[0], 0.01f)
        assertEquals(0.427f, c3[1], 0.01f)
        assertEquals(0.325f, c3[2], 0.01f)
    }

    @Test
    fun `AdaptiveTessellator leaves vertexColors empty for standard monochrome rings`() {
        val parsed = CrochetParser.parse("V1: 6 pb (6)")
        val instances = parsed.ast!!.flatten()

        val ring = RingProfileGenerator.generateRingFromInstances(1, instances, testGauge, null)
        val mesh = AdaptiveTessellator.tessellate(listOf(ring), includePolarCap = false)

        assertTrue("vertexColors debe estar vacío en mallas monocromáticas", mesh.vertexColors.isEmpty())
    }

    // ==========================================
    // 5. GlbMeshBuilder and StuffingInflationFilter tests
    // ==========================================

    @Test
    fun `GlbMeshBuilder includes COLOR_0 attribute and unmodulated baseColorFactor when vertexColors present`() {
        val parsed = CrochetParser.parse("V1: [#FFFFFF] 3 pb, [#E06D53] 3 pb (6)")
        val instances = parsed.ast!!.flatten()
        val ring = RingProfileGenerator.generateRingFromInstances(1, instances, testGauge, null)
        val mesh = AdaptiveTessellator.tessellate(listOf(ring), includePolarCap = true)

        val glbBuffer = GlbMeshBuilder.buildGlb(mesh)
        assertNotNull(glbBuffer)

        // Extraer JSON
        val jsonLength = glbBuffer.getInt(12)
        val jsonBytes = ByteArray(jsonLength)
        glbBuffer.position(20)
        glbBuffer.get(jsonBytes)
        val jsonStr = String(jsonBytes, Charsets.UTF_8)

        assertTrue("Debe declarar atributo COLOR_0", jsonStr.contains("\"COLOR_0\": 2"))
        assertTrue("Debe tener baseColorFactor unitario para no tintar los colores de vértice",
            jsonStr.contains("\"baseColorFactor\": [1.0, 1.0, 1.0, 1.0]"))
        assertTrue("Debe declarar índices en accessor 3", jsonStr.contains("\"indices\": 3"))
    }

    @Test
    fun `StuffingInflationFilter preserves vertexColors intact during volume expansion`() {
        val parsed = CrochetParser.parse("V1: [#FFFFFF] 3 pb, [#E06D53] 3 pb (6)")
        val instances = parsed.ast!!.flatten()
        val ring = RingProfileGenerator.generateRingFromInstances(1, instances, testGauge, null)
        val mesh = AdaptiveTessellator.tessellate(listOf(ring), includePolarCap = true)

        val inflated = StuffingInflationFilter.applyInflation(
            mesh = mesh,
            topologyType = com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType.CLOSED_FILLED,
            config = StuffingInflationConfig(smoothingIterations = 1)
        )

        assertEquals("Debe conservar la cantidad de colores de vértice", mesh.vertexColors.size, inflated.vertexColors.size)
        assertTrue("Los colores de vértice deben ser idénticos tras el inflado",
            mesh.vertexColors.contentEquals(inflated.vertexColors))
    }
}
