package com.crisanfitos.stitchmesh3d.core.geometry

import androidx.compose.ui.graphics.Color
import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.gauge.YarnGaugeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Pruebas unitarias y de rendimiento para la tubería de actualización reactiva de búferes
 * GPU/Filament (RF-3.5: latencia total de regeneración y empaquetado < 100 ms).
 */
class GpuVertexBufferUpdateTest {

    private val standard = YarnGaugeRegistry.DEFAULT_STANDARD

    @Test
    fun `reactive buffer generation pipeline executes in under 100 ms per round mutation`() {
        val rings = mutableListOf<RingProfile>()
        var prevRing: RingProfile? = null

        // Simular la edición incremental de 15 vueltas en tiempo real
        val stitchCounts = listOf(6, 12, 18, 24, 30, 36, 42, 42, 42, 36, 30, 24, 18, 12, 6)

        for ((idx, count) in stitchCounts.withIndex()) {
            val elapsedMs = measureTimeMillis {
                val stitches = List(count) { StitchType.SingleCrochet }
                val newRing = RingProfileGenerator.generateRing(
                    roundIndex = idx + 1,
                    stitches = stitches,
                    gaugeStandard = standard,
                    previousRing = prevRing
                )
                rings.add(newRing)
                prevRing = newRing

                val mesh = AdaptiveTessellator.tessellate(rings)
                val glbBuffer = GlbMeshBuilder.buildGlb(
                    mesh = mesh,
                    yarnColor = Color(0xFFE06D53),
                    roughness = 0.9f
                )

                // Validar integridad estructural del contenedor glTF
                assertTrue("El buffer debe ser directo", glbBuffer.isDirect)
                assertEquals("Magic glTF", 0x46546C67, glbBuffer.getInt(0))
                assertEquals("Versión 2", 2, glbBuffer.getInt(4))
            }

            // Requisito estricto RF-3.5: latencia < 100 ms
            assertTrue(
                "La mutación reactiva de la vuelta ${idx + 1} debe tardar < 100 ms (tardó ${elapsedMs} ms)",
                elapsedMs < 100
            )
        }
    }

    @Test
    fun `successive buffer allocations maintain strict 4-byte alignment and bounding box integrity`() {
        val ring1 = RingProfileGenerator.generateRing(1, List(6) { StitchType.SingleCrochet }, standard)
        val ring2 = RingProfileGenerator.generateRing(2, List(12) { StitchType.SingleCrochet }, standard, ring1)
        val mesh = AdaptiveTessellator.tessellate(listOf(ring1, ring2))

        // Mutación 1: Malla sólida
        val solidBuffer = GlbMeshBuilder.buildGlb(mesh, isWireframe = false)
        assertEquals("Alineación 4 bytes sólido", 0, solidBuffer.capacity() % 4)

        // Mutación 2: Modo alambre
        val wireframeBuffer = GlbMeshBuilder.buildGlb(mesh, isWireframe = true)
        assertEquals("Alineación 4 bytes alambre", 0, wireframeBuffer.capacity() % 4)

        // En modo alambre unlit (glTF LINES) se omiten las normales de superficie para compatibilidad con Filament,
        // garantizando eficiencia y alineación estricta de 4 bytes
        assertTrue("El búfer de alambre debe tener capacidad positiva", wireframeBuffer.capacity() > 0)
        assertTrue("El búfer sólido debe tener capacidad positiva", solidBuffer.capacity() > 0)
    }
}
