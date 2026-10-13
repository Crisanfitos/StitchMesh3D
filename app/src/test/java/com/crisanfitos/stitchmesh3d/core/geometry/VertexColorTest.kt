package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.color.CrochetColorHelper
import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.engine.parser.StitchInstance
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import com.crisanfitos.stitchmesh3d.ui.workspace.WorkspaceIntent
import com.crisanfitos.stitchmesh3d.ui.workspace.WorkspaceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets

/**
 * Suite de pruebas unitarias para zonificación cromática por vuelta y asignación de materiales (SM-032, RF-4.3).
 *
 * Verifica:
 * 1. Zonificación cromática multi-vuelta con colores diferenciados.
 * 2. Transiciones nítidas sin artefactos de sangrado (duplicación de vértices en frontera).
 * 3. Preservación de vectores normales suaves en fronteras cromáticas no BLO.
 * 4. Actualización reactiva mediante MVI WorkspaceIntent.ChangeRoundColor.
 * 5. Inclusión de atributo COLOR_0 en GLB exportado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VertexColorTest {

    private val testDispatcher = StandardTestDispatcher()

    private val testGauge = YarnGaugeStandard(
        id = 1,
        yarnWeightCategory = YarnWeightCategory.MEDIUM,
        categoryName = "Worsted",
        hookSizeMm = 4.0f,
        stitchWidthMm = 4.0f,
        stitchHeightMm = 4.5f,
        stitchThicknessMm = 2.0f
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `multi-round pattern with distinct round colors produces segmented vertex colors in mesh`() {
        val colorRound1 = "#E06D53" // Terracotta
        val colorRound2 = "#52A474" // Sage Green
        val colorRound3 = "#E5A93C" // Yarn Gold

        val ring1 = RingProfileGenerator.generateRingFromInstances(
            roundIndex = 1,
            instances = List(6) { idx ->
                StitchInstance(StitchType.SingleCrochet, idx, colorHex = colorRound1)
            },
            gaugeStandard = testGauge
        )

        val ring2 = RingProfileGenerator.generateRingFromInstances(
            roundIndex = 2,
            instances = List(12) { idx ->
                StitchInstance(StitchType.SingleCrochet, idx, colorHex = colorRound2)
            },
            gaugeStandard = testGauge,
            previousRing = ring1
        )

        val ring3 = RingProfileGenerator.generateRingFromInstances(
            roundIndex = 3,
            instances = List(18) { idx ->
                StitchInstance(StitchType.SingleCrochet, idx, colorHex = colorRound3)
            },
            gaugeStandard = testGauge,
            previousRing = ring2
        )

        val mesh = AdaptiveTessellator.tessellate(listOf(ring1, ring2, ring3), includePolarCap = true)

        assertTrue("vertexColors no debe estar vacío con colores diferenciados", mesh.vertexColors.isNotEmpty())
        assertEquals("Búfer de colores debe tener exactamente 4 floats por vértice", mesh.vertexCount * 4, mesh.vertexColors.size)

        // Vértices polar y de vuelta 1 deben tener colorRound1
        val expectedRgba1 = CrochetColorHelper.toRgbaFloats(colorRound1)
        val poleColor = mesh.getVertexColor(0)
        assertEquals(expectedRgba1[0], poleColor[0], 0.01f)
        assertEquals(expectedRgba1[1], poleColor[1], 0.01f)
        assertEquals(expectedRgba1[2], poleColor[2], 0.01f)

        // Verificar que existen vértices con el color de vuelta 2 y 3
        val expectedRgba2 = CrochetColorHelper.toRgbaFloats(colorRound2)
        val expectedRgba3 = CrochetColorHelper.toRgbaFloats(colorRound3)

        var hasColor2 = false
        var hasColor3 = false

        for (v in 0 until mesh.vertexCount) {
            val c = mesh.getVertexColor(v)
            if (kotlin.math.abs(c[0] - expectedRgba2[0]) < 0.01f &&
                kotlin.math.abs(c[1] - expectedRgba2[1]) < 0.01f &&
                kotlin.math.abs(c[2] - expectedRgba2[2]) < 0.01f
            ) {
                hasColor2 = true
            }
            if (kotlin.math.abs(c[0] - expectedRgba3[0]) < 0.01f &&
                kotlin.math.abs(c[1] - expectedRgba3[1]) < 0.01f &&
                kotlin.math.abs(c[2] - expectedRgba3[2]) < 0.01f
            ) {
                hasColor3 = true
            }
        }

        assertTrue("La malla debe contener vértices con color de vuelta 2 (#52A474)", hasColor2)
        assertTrue("La malla debe contener vértices con color de vuelta 3 (#E5A93C)", hasColor3)
    }

    @Test
    fun `chromatic transition duplicates boundary vertices for sharp transition and preserves smooth normals`() {
        val colorA = "#E06D53"
        val colorB = "#52A474"

        val ring1 = RingProfileGenerator.generateRingFromInstances(
            roundIndex = 1,
            instances = List(6) { idx ->
                StitchInstance(StitchType.SingleCrochet, idx, colorHex = colorA)
            },
            gaugeStandard = testGauge
        )

        val ring2 = RingProfileGenerator.generateRingFromInstances(
            roundIndex = 2,
            instances = List(6) { idx ->
                StitchInstance(StitchType.SingleCrochet, idx, colorHex = colorB)
            },
            gaugeStandard = testGauge,
            previousRing = ring1
        )

        // Con cambio de color entre ring1 y ring2, AdaptiveTessellator duplica los 6 vértices de ring1
        // (1 polo + 6 base ring1 + 6 duplicados salida ring1 + 6 ring2 = 19 vértices)
        val mesh = AdaptiveTessellator.tessellate(listOf(ring1, ring2), includePolarCap = true)

        assertEquals("Debe duplicar los vértices perimetrales en la frontera cromática (1 + 6 + 6 + 6)", 19, mesh.vertexCount)

        // Verificar que los vértices duplicados están en la misma posición que sus homólogos base
        for (i in 0 until 6) {
            val baseIdx = 1 + 2 * i
            val dupIdx = baseIdx + 1

            val pBase = mesh.getVertexPosition(baseIdx)
            val pDup = mesh.getVertexPosition(dupIdx)

            assertEquals("X debe coincidir exactamente", pBase[0], pDup[0], 1e-4f)
            assertEquals("Y debe coincidir exactamente", pBase[1], pDup[1], 1e-4f)
            assertEquals("Z debe coincidir exactamente", pBase[2], pDup[2], 1e-4f)

            // Colores: baseIdx tiene colorA, dupIdx tiene colorB (nitidez absoluta de banda)
            val cBase = mesh.getVertexColor(baseIdx)
            val cDup = mesh.getVertexColor(dupIdx)
            val expectedA = CrochetColorHelper.toRgbaFloats(colorA)
            val expectedB = CrochetColorHelper.toRgbaFloats(colorB)

            assertEquals(expectedA[0], cBase[0], 0.01f)
            assertEquals(expectedB[0], cDup[0], 0.01f)

            // Normales: para transición cromática estándar sin arista viva (no-BLO),
            // la normal debe mantenerse suave y compartida para un sombreado continuo
            val nBase = mesh.getVertexNormal(baseIdx)
            val nDup = mesh.getVertexNormal(dupIdx)

            assertEquals("Normal X suave debe coincidir", nBase[0], nDup[0], 1e-3f)
            assertEquals("Normal Y suave debe coincidir", nBase[1], nDup[1], 1e-3f)
            assertEquals("Normal Z suave debe coincidir", nBase[2], nDup[2], 1e-3f)

            // Magnitud unitaria
            assertEquals(1.0f, mesh.getNormalMagnitude(baseIdx), 0.01f)
            assertEquals(1.0f, mesh.getNormalMagnitude(dupIdx), 0.01f)
        }
    }

    @Test
    fun `WorkspaceViewModel ChangeRoundColor intent updates round color and regenerates mesh`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        advanceUntilIdle()

        val initialRounds = viewModel.state.value.rounds
        assertTrue("Debe tener vueltas iniciales", initialRounds.isNotEmpty())

        val targetRound = initialRounds.first()
        val newColor = "#52A474"

        // Procesar cambio de color
        viewModel.processIntent(WorkspaceIntent.ChangeRoundColor(targetRound.id, newColor))
        advanceUntilIdle()

        val updatedRound = viewModel.state.value.rounds.first { it.id == targetRound.id }
        assertEquals(newColor, updatedRound.colorHex)

        // Verificar que la malla generada contiene los colores asignados
        val mesh = viewModel.state.value.meshGeometry
        assertNotNull("La malla geométrica debe haberse generado", mesh)
        assertTrue("La malla debe tener vertexColors", mesh!!.vertexColors.isNotEmpty())
    }

    @Test
    fun `GlbMeshBuilder includes COLOR_0 attribute when round color zoning is applied`() {
        val ring1 = RingProfileGenerator.generateRingFromInstances(
            roundIndex = 1,
            instances = List(6) { idx ->
                StitchInstance(StitchType.SingleCrochet, idx, colorHex = "#E06D53")
            },
            gaugeStandard = testGauge
        )
        val ring2 = RingProfileGenerator.generateRingFromInstances(
            roundIndex = 2,
            instances = List(12) { idx ->
                StitchInstance(StitchType.SingleCrochet, idx, colorHex = "#52A474")
            },
            gaugeStandard = testGauge,
            previousRing = ring1
        )

        val mesh = AdaptiveTessellator.tessellate(listOf(ring1, ring2), includePolarCap = true)
        val glbBuffer = GlbMeshBuilder.buildGlb(mesh)

        assertTrue("El búfer GLB debe tener contenido", glbBuffer.capacity() > 0)
        val jsonLength = glbBuffer.getInt(12)
        val jsonBytes = ByteArray(jsonLength)
        val dup = glbBuffer.duplicate()
        dup.position(20)
        dup.get(jsonBytes)
        val glbJson = String(jsonBytes, StandardCharsets.UTF_8)
        assertTrue("El GLB debe declarar el atributo COLOR_0 en los primitivos", glbJson.contains("COLOR_0"))
    }
}
