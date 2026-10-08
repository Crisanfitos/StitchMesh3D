package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.engine.model.TopologyFlag
import com.crisanfitos.stitchmesh3d.core.gauge.YarnGaugeRegistry
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Suite de pruebas unitarias para RF-3.2 y TRD §2.3 / §3.2:
 * - Cálculo y normalización estricta de normales de vértice (||n|| = 1.0 +/- 0.001).
 * - Duplicación de vértices en puntadas BLO para corte de normales a 90° (Normal Split) sin gaps.
 * - Desplazamientos volumétricos en coordenadas para puntos con relieve (Bobble, Popcorn, Puff).
 */
class MeshNormalsAndBumpsTest {

    private val standard: YarnGaugeStandard = YarnGaugeRegistry.DEFAULT_STANDARD

    @Test
    fun `all vertex normals in generated mesh are strictly normalized with magnitude 1_0 plus-minus 0_001`() {
        val ring1 = RingProfileGenerator.generateRing(
            roundIndex = 1,
            stitches = List(6) { StitchType.SingleCrochet },
            gaugeStandard = standard
        )
        val ring2 = RingProfileGenerator.generateRing(
            roundIndex = 2,
            stitches = List(12) { StitchType.SingleCrochet },
            gaugeStandard = standard,
            previousRing = ring1
        )
        val ring3 = RingProfileGenerator.generateRing(
            roundIndex = 3,
            stitches = List(18) { StitchType.SingleCrochet },
            gaugeStandard = standard,
            previousRing = ring2
        )

        val mesh = AdaptiveTessellator.tessellate(listOf(ring1, ring2, ring3), includePolarCap = true)

        assertTrue("La malla debe tener vértices", mesh.vertexCount > 0)
        assertEquals(mesh.vertexCount * 3, mesh.vertexNormals.size)

        for (v in 0 until mesh.vertexCount) {
            val mag = mesh.getNormalMagnitude(v)
            assertEquals(
                "La magnitud del vector normal en el vértice $v debe ser estrictamente 1.0 +/- 0.001",
                1.0f,
                mag,
                0.001f
            )
        }
    }

    @Test
    fun `BLO stitches duplicate ring vertices and preserve closed contiguous geometry without gaps`() {
        // Modelo de 3 anillos: base plana -> vuelta BLO a 90° -> pared cilíndrica
        val ring1 = RingProfileGenerator.generateRing(
            roundIndex = 1,
            stitches = List(6) { StitchType.SingleCrochet },
            gaugeStandard = standard
        )
        val ring2Blo = RingProfileGenerator.generateRing(
            roundIndex = 2,
            stitches = List(6) { StitchType.SingleCrochet.withTopology(TopologyFlag.BLO) },
            gaugeStandard = standard,
            previousRing = ring1
        )
        val ring3 = RingProfileGenerator.generateRing(
            roundIndex = 3,
            stitches = List(6) { StitchType.SingleCrochet },
            gaugeStandard = standard,
            previousRing = ring2Blo
        )

        val meshWithoutBlo = AdaptiveTessellator.tessellate(
            listOf(
                ring1,
                RingProfileGenerator.generateRing(2, List(6) { StitchType.SingleCrochet }, standard, ring1),
                ring3
            ),
            includePolarCap = true
        )

        val meshWithBlo = AdaptiveTessellator.tessellate(
            listOf(ring1, ring2Blo, ring3),
            includePolarCap = true
        )

        // Sin BLO: 1 pole + 6 (R1) + 6 (R2) + 6 (R3) = 19 vértices
        assertEquals(19, meshWithoutBlo.vertexCount)

        // Con BLO en R2: 6 vértices duplicados en R2 -> 19 + 6 = 25 vértices
        assertEquals(25, meshWithBlo.vertexCount)

        // Verificar contigüidad espacial: los vértices duplicados (in/out) de R2 deben tener exactamente las mismas coordenadas
        // En meshWithBlo:
        // Polo: 0
        // R1: 1..6 (in == out)
        // R2: cada punto tiene [in, out] consecutivo: 7, 8 (pto 0), 9, 10 (pto 1), 11, 12 (pto 2), 13, 14 (pto 3), 15, 16 (pto 4), 17, 18 (pto 5)
        for (p in 0 until 6) {
            val inIdx = 7 + (p * 2)
            val outIdx = inIdx + 1

            val posIn = meshWithBlo.getVertexPosition(inIdx)
            val posOut = meshWithBlo.getVertexPosition(outIdx)

            assertEquals("Coordenada X idéntica sin gap", posIn[0], posOut[0], 1e-4f)
            assertEquals("Coordenada Y idéntica sin gap", posIn[1], posOut[1], 1e-4f)
            assertEquals("Coordenada Z idéntica sin gap", posIn[2], posOut[2], 1e-4f)
        }

        // Todos los índices apuntan al rango válido
        for (idx in meshWithBlo.indices) {
            assertTrue("Índice debe ser >= 0", idx >= 0)
            assertTrue("Índice debe ser < ${meshWithBlo.vertexCount}", idx < meshWithBlo.vertexCount)
        }
    }

    @Test
    fun `BLO normal split produces decoupled hard edge with sharp normal angle`() {
        val ring1 = RingProfileGenerator.generateRing(
            roundIndex = 1,
            stitches = List(6) { StitchType.SingleCrochet },
            gaugeStandard = standard
        )
        val ring2Blo = RingProfileGenerator.generateRing(
            roundIndex = 2,
            stitches = List(6) { StitchType.SingleCrochet.withTopology(TopologyFlag.BLO) },
            gaugeStandard = standard,
            previousRing = ring1
        )
        val ring3 = RingProfileGenerator.generateRing(
            roundIndex = 3,
            stitches = List(6) { StitchType.SingleCrochet },
            gaugeStandard = standard,
            previousRing = ring2Blo
        )

        val mesh = AdaptiveTessellator.tessellate(listOf(ring1, ring2Blo, ring3), includePolarCap = true)

        // Verificar desacoplamiento de normales en cada par duplicado de R2
        for (p in 0 until 6) {
            val inIdx = 7 + (p * 2)
            val outIdx = inIdx + 1

            val nIn = mesh.getVertexNormal(inIdx)
            val nOut = mesh.getVertexNormal(outIdx)

            // Producto escalar n_in . n_out
            val dot = nIn[0] * nOut[0] + nIn[1] * nOut[1] + nIn[2] * nOut[2]

            // En una arista viva normal split, las normales de caras adyacentes no son paralelas (dot < 1.0)
            assertTrue(
                "Las normales del punto BLO $p deben estar desacopladas (dot=$dot < 0.99)",
                dot < 0.99f
            )
        }
    }

    @Test
    fun `partial BLO within a round only duplicates the specific BLO stitches`() {
        val ring1 = RingProfileGenerator.generateRing(
            roundIndex = 1,
            stitches = List(6) { StitchType.SingleCrochet },
            gaugeStandard = standard
        )
        // 3 puntos normales y 3 puntos BLO
        val partialBloStitches = listOf(
            StitchType.SingleCrochet,
            StitchType.SingleCrochet.withTopology(TopologyFlag.BLO),
            StitchType.SingleCrochet,
            StitchType.SingleCrochet.withTopology(TopologyFlag.BLO),
            StitchType.SingleCrochet,
            StitchType.SingleCrochet.withTopology(TopologyFlag.BLO)
        )
        val ring2 = RingProfileGenerator.generateRing(
            roundIndex = 2,
            stitches = partialBloStitches,
            gaugeStandard = standard,
            previousRing = ring1
        )
        val ring3 = RingProfileGenerator.generateRing(
            roundIndex = 3,
            stitches = List(6) { StitchType.SingleCrochet },
            gaugeStandard = standard,
            previousRing = ring2
        )

        val mesh = AdaptiveTessellator.tessellate(listOf(ring1, ring2, ring3), includePolarCap = true)

        // Base: 1 pole + 6 (R1) + 6 (R2) + 6 (R3) = 19
        // 3 puntos BLO en R2 -> +3 vértices duplicados = 22 vértices
        assertEquals(22, mesh.vertexCount)
    }

    @Test
    fun `volumetric stitches bobble, popcorn and puff produce significant outward coordinates displacement`() {
        val stitches = listOf(
            StitchType.SingleCrochet,
            StitchType.BobbleStitch,
            StitchType.SingleCrochet,
            StitchType.PopcornStitch,
            StitchType.SingleCrochet,
            StitchType.PuffStitch
        )

        val ring1 = RingProfileGenerator.generateRing(
            roundIndex = 1,
            stitches = List(6) { StitchType.SingleCrochet },
            gaugeStandard = standard
        )
        val ring2 = RingProfileGenerator.generateRing(
            roundIndex = 2,
            stitches = stitches,
            gaugeStandard = standard,
            previousRing = ring1
        )

        val mesh = AdaptiveTessellator.tessellate(listOf(ring1, ring2), includePolarCap = true)

        // R2 empieza después del polo (1) y R1 (6): offset 7
        // Índice 7: SingleCrochet
        // Índice 8: BobbleStitch
        // Índice 9: SingleCrochet
        // Índice 10: PopcornStitch
        // Índice 11: SingleCrochet
        // Índice 12: PuffStitch
        val pBase = mesh.getVertexPosition(7)
        val pBobble = mesh.getVertexPosition(8)
        val pPopcorn = mesh.getVertexPosition(10)
        val pPuff = mesh.getVertexPosition(12)

        val rBase = hypot(pBase[0], pBase[1])
        val rBobble = hypot(pBobble[0], pBobble[1])
        val rPopcorn = hypot(pPopcorn[0], pPopcorn[1])
        val rPuff = hypot(pPuff[0], pPuff[1])

        assertTrue("Bobble debe sobresalir hacia el exterior respecto a punto base", rBobble > rBase)
        assertTrue("Popcorn debe sobresalir hacia el exterior respecto a punto base", rPopcorn > rBase)
        assertTrue("Puff debe sobresalir hacia el exterior respecto a punto base", rPuff > rBase)
        assertTrue("Popcorn (+2.2t) debe sobresalir más que Bobble (+1.5t)", rPopcorn > rBobble)
        assertTrue("Bobble (+1.5t) debe sobresalir más que Puff (+1.0t)", rBobble > rPuff)
    }

    @Test
    fun `area weighted normal accumulation correctly handles asymmetric triangle distributions`() {
        // Vuelta 1: 6 puntos, Vuelta 2: 12 puntos (aumentos asimétricos)
        val ring1 = RingProfileGenerator.generateRing(
            roundIndex = 1,
            stitches = List(6) { StitchType.SingleCrochet },
            gaugeStandard = standard
        )
        val ring2 = RingProfileGenerator.generateRing(
            roundIndex = 2,
            stitches = List(12) { StitchType.SingleCrochet },
            gaugeStandard = standard,
            previousRing = ring1
        )

        val mesh = AdaptiveTessellator.tessellate(listOf(ring1, ring2), includePolarCap = true)

        for (v in 0 until mesh.vertexCount) {
            val n = mesh.getVertexNormal(v)
            val mag = mesh.getNormalMagnitude(v)
            assertEquals("Normal unitaria", 1.0f, mag, 0.001f)

            // Vértices del cilindro/cono deben tener normal con componente radial hacia afuera
            if (v > 0) {
                val pos = mesh.getVertexPosition(v)
                val radialDot = n[0] * pos[0] + n[1] * pos[1]
                assertTrue("La normal en v=$v debe apuntar hacia el exterior radial: $radialDot", radialDot > 0f)
            }
        }
    }
}
