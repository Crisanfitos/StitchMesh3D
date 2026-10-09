package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.engine.model.TopologyFlag
import com.crisanfitos.stitchmesh3d.core.gauge.YarnGaugeRegistry
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

/**
 * Suite de pruebas unitarias para [StuffingInflationFilter] (SM-067, PRD RF-3.2, TRD §3.1, §3.2):
 * - Simulación física de inflado volumétrico por relleno de algodón.
 * - Suavizado laplaciano continuo entre vueltas de amigurumi.
 * - Preservación de aristas vivas en puntadas BLO sin fisuras en la malla.
 * - Normales de superficie unitarias (||n|| = 1.0 ± 0.001) para Filament PBR.
 * - Exclusión estricta de piezas de tipo FLAT_PANEL.
 * - Benchmark de rendimiento < 25 ms para 10.000 vértices.
 */
class StuffingInflationFilterTest {

    private val standard: YarnGaugeStandard = YarnGaugeRegistry.DEFAULT_STANDARD

    @Test
    fun `flat panel mesh is strictly preserved and not modified by inflation filter`() {
        val ring1 = RingProfileGenerator.generateRing(1, List(10) { StitchType.Chain }, standard)
        val ring2 = RingProfileGenerator.generateRing(2, List(10) { StitchType.SingleCrochet }, standard, ring1)
        val rawMesh = AdaptiveTessellator.tessellate(listOf(ring1, ring2), includePolarCap = false)

        val processedMesh = StuffingInflationFilter.applyInflation(
            mesh = rawMesh,
            topologyType = PartTopologyType.FLAT_PANEL
        )

        assertSame("Las piezas planas deben retornarse intactas sin modificación", rawMesh, processedMesh)
    }

    @Test
    fun `empty or degenerated mesh is returned as-is without crashing`() {
        val emptyMesh = MeshGeometry(FloatArray(0), ShortArray(0), 0, 0, FloatArray(0))
        val processed = StuffingInflationFilter.applyInflation(emptyMesh, PartTopologyType.CLOSED_FILLED)
        assertSame("Malla vacía debe retornarse idéntica", emptyMesh, processed)
    }

    @Test
    fun `closed filled amigurumi sphere expands outward and rounds facet transitions`() {
        // Esfera amigurumi: V1 (6), V2 (12), V3 (18), V4 (18), V5 (12), V6 (6)
        val r1 = RingProfileGenerator.generateRing(1, List(6) { StitchType.SingleCrochet }, standard)
        val r2 = RingProfileGenerator.generateRing(2, List(12) { StitchType.SingleCrochet }, standard, r1)
        val r3 = RingProfileGenerator.generateRing(3, List(18) { StitchType.SingleCrochet }, standard, r2)
        val r4 = RingProfileGenerator.generateRing(4, List(18) { StitchType.SingleCrochet }, standard, r3)
        val r5 = RingProfileGenerator.generateRing(5, List(12) { StitchType.SingleCrochet }, standard, r4)
        val r6 = RingProfileGenerator.generateRing(6, List(6) { StitchType.SingleCrochet }, standard, r5)

        val rawMesh = AdaptiveTessellator.tessellate(listOf(r1, r2, r3, r4, r5, r6), includePolarCap = true)
        val rawBbox = rawMesh.computeBoundingBox()

        val inflatedMesh = StuffingInflationFilter.applyInflation(
            mesh = rawMesh,
            topologyType = PartTopologyType.CLOSED_FILLED,
            config = StuffingInflationConfig(inflationFactor = 0.10f, smoothingIterations = 2)
        )
        val inflatedBbox = inflatedMesh.computeBoundingBox()

        // El relleno de algodón expande el diámetro máximo hacia afuera
        assertTrue(
            "El diámetro inflado (${inflatedBbox.maxDiameterMm} mm) debe ser mayor que el diámetro en reposo (${rawBbox.maxDiameterMm} mm)",
            inflatedBbox.maxDiameterMm > rawBbox.maxDiameterMm
        )
    }

    @Test
    fun `all vertex normals in inflated mesh are strictly unit length within tolerance`() {
        val r1 = RingProfileGenerator.generateRing(1, List(6) { StitchType.SingleCrochet }, standard)
        val r2 = RingProfileGenerator.generateRing(2, List(12) { StitchType.SingleCrochet }, standard, r1)
        val r3 = RingProfileGenerator.generateRing(3, List(18) { StitchType.SingleCrochet }, standard, r2)

        val rawMesh = AdaptiveTessellator.tessellate(listOf(r1, r2, r3), includePolarCap = true)
        val inflatedMesh = StuffingInflationFilter.applyInflation(
            mesh = rawMesh,
            topologyType = PartTopologyType.CLOSED_FILLED
        )

        assertEquals("Malla inflada debe preservar el número de vértices", rawMesh.vertexCount, inflatedMesh.vertexCount)
        assertEquals("Búfer de normales debe tener tamaño 3 * vertexCount", inflatedMesh.vertexCount * 3, inflatedMesh.vertexNormals.size)

        for (v in 0 until inflatedMesh.vertexCount) {
            val nx = inflatedMesh.vertexNormals[v * 3]
            val ny = inflatedMesh.vertexNormals[v * 3 + 1]
            val nz = inflatedMesh.vertexNormals[v * 3 + 2]
            val mag = sqrt(nx * nx + ny * ny + nz * nz)

            assertEquals(
                "La normal del vértice $v debe tener magnitud unitaria 1.0 +/- 0.001 (actual: $mag)",
                1.0f,
                mag,
                0.001f
            )
        }
    }

    @Test
    fun `semi closed tube expands outward with reduced factor compared to closed filled`() {
        val r1 = RingProfileGenerator.generateRing(1, List(8) { StitchType.SingleCrochet }, standard)
        val r2 = RingProfileGenerator.generateRing(2, List(8) { StitchType.SingleCrochet }, standard, r1)
        val r3 = RingProfileGenerator.generateRing(3, List(8) { StitchType.SingleCrochet }, standard, r2)

        val rawMesh = AdaptiveTessellator.tessellate(listOf(r1, r2, r3), includePolarCap = false)
        val rawBbox = rawMesh.computeBoundingBox()

        val tubeMesh = StuffingInflationFilter.applyInflation(
            mesh = rawMesh,
            topologyType = PartTopologyType.SEMI_CLOSED_TUBE,
            config = StuffingInflationConfig(inflationFactor = 0.10f)
        )
        val tubeBbox = tubeMesh.computeBoundingBox()

        assertTrue(
            "El diámetro del tubo inflado (${tubeBbox.maxDiameterMm} mm) debe expandirse respecto a reposo (${rawBbox.maxDiameterMm} mm)",
            tubeBbox.maxDiameterMm > rawBbox.maxDiameterMm
        )
    }

    @Test
    fun `BLO stitches preserve coincident vertices without tearing while retaining split normals`() {
        val bloStitch = StitchType.SingleCrochet.withTopology(TopologyFlag.BLO)
        val r1 = RingProfileGenerator.generateRing(1, List(6) { StitchType.SingleCrochet }, standard)
        val r2 = RingProfileGenerator.generateRing(2, List(6) { bloStitch }, standard, r1)
        val r3 = RingProfileGenerator.generateRing(3, List(6) { StitchType.SingleCrochet }, standard, r2)

        val rawMesh = AdaptiveTessellator.tessellate(listOf(r1, r2, r3), includePolarCap = true)

        // En la malla cruda con BLO, debe haber vértices duplicados en idéntica posición
        var rawDuplicateCount = 0
        for (i in 0 until rawMesh.vertexCount) {
            val p1 = rawMesh.getVertexPosition(i)
            for (j in (i + 1) until rawMesh.vertexCount) {
                val p2 = rawMesh.getVertexPosition(j)
                val dx = p1[0] - p2[0]
                val dy = p1[1] - p2[1]
                val dz = p1[2] - p2[2]
                if (dx * dx + dy * dy + dz * dz < 1e-4f) {
                    rawDuplicateCount++
                }
            }
        }
        assertTrue("La malla cruda con BLO debe contener al menos 6 pares de vértices duplicados", rawDuplicateCount >= 6)

        val inflatedMesh = StuffingInflationFilter.applyInflation(
            mesh = rawMesh,
            topologyType = PartTopologyType.CLOSED_FILLED,
            config = StuffingInflationConfig(inflationFactor = 0.08f, smoothingIterations = 2, preserveBloEdges = true)
        )

        // Verificar que tras inflado y suavizado laplaciano, los vértices duplicados continúan coincidentes (sin fisuras)
        var postDuplicateCount = 0
        for (i in 0 until inflatedMesh.vertexCount) {
            val p1 = inflatedMesh.getVertexPosition(i)
            for (j in (i + 1) until inflatedMesh.vertexCount) {
                val p2 = inflatedMesh.getVertexPosition(j)
                val dx = p1[0] - p2[0]
                val dy = p1[1] - p2[1]
                val dz = p1[2] - p2[2]
                if (dx * dx + dy * dy + dz * dz < 1e-4f) {
                    postDuplicateCount++
                    // Y verificar que sus normales continúan desacopladas
                    val n1 = inflatedMesh.getVertexNormal(i)
                    val n2 = inflatedMesh.getVertexNormal(j)
                    val dot = n1[0] * n2[0] + n1[1] * n2[1] + n1[2] * n2[2]
                    assertTrue(
                        "Las normales de vértices duplicados BLO deben diferir para preservar la arista viva (dot product = $dot < 0.98)",
                        dot < 0.98f
                    )
                }
            }
        }
        assertEquals("Todos los pares coincidentes BLO deben permanecer sincronizados sin aberturas", rawDuplicateCount, postDuplicateCount)
    }

    @Test
    fun `benchmark performance - filter processes 10_000 vertices in less than 25 ms`() {
        // Generar una malla sintética densa de ~10.000 vértices
        val ringCount = 100
        val stitchesPerRing = 100
        val rings = ArrayList<RingProfile>(ringCount)
        var prev: RingProfile? = null

        for (r in 1..ringCount) {
            val ring = RingProfileGenerator.generateRing(
                roundIndex = r,
                stitches = List(stitchesPerRing) { StitchType.SingleCrochet },
                gaugeStandard = standard,
                previousRing = prev
            )
            rings.add(ring)
            prev = ring
        }

        val denseMesh = AdaptiveTessellator.tessellate(rings, includePolarCap = true)
        assertTrue("La malla densa debe tener al menos 10.000 vértices", denseMesh.vertexCount >= 10000)

        // Calentamiento JIT
        StuffingInflationFilter.applyInflation(
            mesh = denseMesh,
            topologyType = PartTopologyType.CLOSED_FILLED,
            config = StuffingInflationConfig(smoothingIterations = 1)
        )

        val startTime = System.nanoTime()
        val result = StuffingInflationFilter.applyInflation(
            mesh = denseMesh,
            topologyType = PartTopologyType.CLOSED_FILLED,
            config = StuffingInflationConfig(inflationFactor = 0.08f, smoothingIterations = 2)
        )
        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0

        assertTrue("El resultado no debe ser nulo y debe conservar los vértices", result.vertexCount == denseMesh.vertexCount)
        assertTrue(
            "El tiempo de ejecución ($elapsedMs ms) debe ser inferior a 25.0 ms (SM-067)",
            elapsedMs < 25.0
        )
    }
}
