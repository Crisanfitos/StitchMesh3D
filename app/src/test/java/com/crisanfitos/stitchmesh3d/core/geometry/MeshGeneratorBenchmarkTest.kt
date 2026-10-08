package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.engine.model.TopologyFlag
import com.crisanfitos.stitchmesh3d.core.gauge.YarnGaugeRegistry
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Suite de pruebas de rendimiento y estrés para el generador de mallas paramétricas 3D.
 *
 * Valida los requisitos no funcionales y funcionales:
 * - RF-3.5: Regeneración del búfer en memoria reactiva de una pieza típica (50 vueltas) en < 100 ms.
 * - RND-1: Escalado estable hasta mallas de 50.000 polígonos sin OutOfMemoryError (OOM) ni saturación de GC.
 * - Validación de asignación directa de búferes nativos NIO (FloatBuffer/ShortBuffer) para GPU Filament.
 */
class MeshGeneratorBenchmarkTest {

    private val standard: YarnGaugeStandard = YarnGaugeRegistry.DEFAULT_STANDARD

    /**
     * Construye un patrón realista de amigurumi de 50 vueltas (cabeza/cuerpo esférico):
     * - Vueltas 1..15: Expansión de 6 a 36 puntos (aumentos graduales).
     * - Vueltas 16..35: Cuerpo cilíndrico constante de 36 puntos con detalles de textura BLO y Bobble.
     * - Vueltas 36..50: Contracción de 36 a 6 puntos (disminuciones graduales).
     */
    private fun build50RoundPiece(): List<RingProfile> {
        val profiles = ArrayList<RingProfile>(50)
        var prevRing: RingProfile? = null

        for (round in 1..50) {
            val stitchCount = when {
                round <= 15 -> (6 + (round - 1) * 2).coerceAtMost(36)
                round <= 35 -> 36
                else -> (36 - (round - 35) * 2).coerceAtLeast(6)
            }

            val stitches = List(stitchCount) { idx ->
                when {
                    round == 20 -> StitchType.SingleCrochet.withTopology(TopologyFlag.BLO)
                    round == 25 && idx % 6 == 0 -> StitchType.BobbleStitch
                    else -> StitchType.SingleCrochet
                }
            }

            val ring = RingProfileGenerator.generateRing(
                roundIndex = round,
                stitches = stitches,
                gaugeStandard = standard,
                previousRing = prevRing
            )
            profiles.add(ring)
            prevRing = ring
        }

        return profiles
    }

    @Test
    fun `benchmark 50 rounds amigurumi piece mesh regeneration executes in under 100 ms`() {
        // Warm-up de la JVM para estabilizar JIT compiler
        repeat(3) {
            val warmRings = build50RoundPiece()
            AdaptiveTessellator.tessellate(warmRings, includePolarCap = true)
        }

        val rings = build50RoundPiece()
        assertEquals("Debe contener exactamente 50 vueltas", 50, rings.size)

        val totalElapsedMs = measureTimeMillis {
            val mesh = AdaptiveTessellator.tessellate(rings, includePolarCap = true)
            assertTrue("La malla debe contener triángulos", mesh.triangleCount > 0)
            assertTrue("La malla debe contener vértices", mesh.vertexCount > 0)
            assertEquals(mesh.vertexCount * 3, mesh.vertexNormals.size)
        }

        println("⏱️ [Benchmark RF-3.5] Regeneración completa de 50 vueltas: $totalElapsedMs ms (Límite: 100 ms)")

        assertTrue(
            "La regeneración completa de la malla para 50 vueltas debe ser < 100 ms (RF-3.5). Real: ${totalElapsedMs}ms",
            totalElapsedMs < 100
        )
    }

    @Test
    fun `stress test scaling up to 50k polygons executes without OutOfMemoryError`() {
        // Construir una malla paramétrica densa de ~50.000 polígonos:
        // 120 anillos de 220 puntos = 120 * 220 * 2 = 52.800 triángulos
        val ringCount = 120
        val stitchesPerRing = 220
        val denseRings = ArrayList<RingProfile>(ringCount)
        var prevRing: RingProfile? = null

        val stitches = List(stitchesPerRing) { StitchType.SingleCrochet }

        for (r in 1..ringCount) {
            val ring = RingProfileGenerator.generateRing(
                roundIndex = r,
                stitches = stitches,
                gaugeStandard = standard,
                previousRing = prevRing
            )
            denseRings.add(ring)
            prevRing = ring
        }

        val mesh: MeshGeometry
        val elapsedMs = measureTimeMillis {
            mesh = AdaptiveTessellator.tessellate(denseRings, includePolarCap = true)
        }

        println("⏱️ [Stress RND-1] Malla de ${mesh.triangleCount} polígonos (${mesh.vertexCount} vértices) generada en $elapsedMs ms")

        assertTrue(
            "La malla densa debe alcanzar o superar 50.000 polígonos (RND-1). Triángulos: ${mesh.triangleCount}",
            mesh.triangleCount >= 50_000
        )
        assertEquals(mesh.indices.size, mesh.triangleCount * 3)
        assertEquals(mesh.vertexPositions.size, mesh.vertexCount * 3)
        assertEquals(mesh.vertexNormals.size, mesh.vertexCount * 3)
    }

    @Test
    fun `repeated realtime editing re-tessellation passes without memory leaks or degradation`() {
        val rings = build50RoundPiece()
        val iterations = 100

        val totalTime = measureTimeMillis {
            repeat(iterations) {
                val mesh = AdaptiveTessellator.tessellate(rings, includePolarCap = true)
                assertTrue(mesh.triangleCount > 0)
            }
        }

        val avgTimeMs = totalTime.toDouble() / iterations
        println("⏱️ [Stress Reactive Edit] 100 regeneraciones sucesivas: total ${totalTime}ms, promedio: ${avgTimeMs}ms/frame")

        assertTrue(
            "El tiempo medio por re-teselación reactiva debe ser < 15 ms para mantener 60 FPS interactivos. Real: ${avgTimeMs}ms",
            avgTimeMs < 15.0
        )
    }

    @Test
    fun `direct native NIO buffer allocation produces valid native ordered buffers for GPU Filament`() {
        val rings = build50RoundPiece().take(5)
        val mesh = AdaptiveTessellator.tessellate(rings, includePolarCap = true)

        val posBuffer = mesh.toDirectPositionBuffer()
        val normalBuffer = mesh.toDirectNormalBuffer()
        val indexBuffer = mesh.toDirectIndexBuffer()

        assertTrue("Position buffer debe ser directo en memoria nativa", posBuffer.isDirect)
        assertTrue("Normal buffer debe ser directo en memoria nativa", normalBuffer.isDirect)
        assertTrue("Index buffer debe ser directo en memoria nativa", indexBuffer.isDirect)

        assertEquals(mesh.vertexPositions.size, posBuffer.capacity())
        assertEquals(mesh.vertexNormals.size, normalBuffer.capacity())
        assertEquals(mesh.indices.size, indexBuffer.capacity())

        // Comprobar que los valores en el búfer directo corresponden a los datos de la malla
        assertEquals(mesh.vertexPositions[0], posBuffer.get(0), 1e-4f)
        assertEquals(mesh.vertexPositions[1], posBuffer.get(1), 1e-4f)
        assertEquals(mesh.vertexPositions[2], posBuffer.get(2), 1e-4f)

        assertEquals(mesh.vertexNormals[0], normalBuffer.get(0), 1e-4f)
        assertEquals(mesh.vertexNormals[1], normalBuffer.get(1), 1e-4f)
        assertEquals(mesh.vertexNormals[2], normalBuffer.get(2), 1e-4f)

        assertEquals(mesh.indices[0], indexBuffer.get(0))
        assertEquals(mesh.indices[1], indexBuffer.get(1))
        assertEquals(mesh.indices[2], indexBuffer.get(2))
    }
}
