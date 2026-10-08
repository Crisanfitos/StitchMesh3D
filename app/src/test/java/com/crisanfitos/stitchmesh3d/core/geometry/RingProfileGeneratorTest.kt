package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.gauge.YarnGaugeRegistry
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.hypot

class RingProfileGeneratorTest {

    private val standard: YarnGaugeStandard = YarnGaugeRegistry.DEFAULT_STANDARD // #4 Worsted (approx 5.5 x 6.5 x 2.8 mm)

    @Test
    fun `uniform ring of single crochet produces planar circular profile with constant radius and z`() {
        val stitches = List(12) { StitchType.SingleCrochet }

        val profile = RingProfileGenerator.generateRing(
            roundIndex = 2,
            stitches = stitches,
            gaugeStandard = standard
        )

        assertEquals(12, profile.vertexCount)
        assertTrue("El anillo con solo pb debe ser planar", profile.isPlanar)

        val expectedCircumference = 12 * standard.stitchWidthMm.toDouble()
        assertEquals(expectedCircumference, profile.circumferenceMm, 1e-4)

        val expectedRadius = expectedCircumference / (2 * PI)
        assertEquals(expectedRadius, profile.meanRadiusMm, 1e-4)

        // Todos los vértices deben tener idéntico radio y cota z
        for (v in profile.vertices) {
            assertEquals(expectedRadius, v.radiusMm, 1e-4)
            assertEquals(profile.vertices[0].z, v.z, 1e-4)
            // Verificar coordenadas cartesianas: x^2 + y^2 == r^2
            val calculatedRadius = hypot(v.x, v.y)
            assertEquals(expectedRadius, calculatedRadius, 1e-4)
        }
    }

    @Test
    fun `heterogeneous ring alternating pb and pa curves z coordinate reflecting stitch height`() {
        // Alternancia: 6 pb, 6 pa (por ejemplo formando el talón o la nariz de un amigurumi)
        val stitches = List(6) { StitchType.SingleCrochet } + List(6) { StitchType.DoubleCrochet }

        val baseRing = RingProfileGenerator.generateRing(
            roundIndex = 1,
            stitches = List(12) { StitchType.SingleCrochet },
            gaugeStandard = standard
        )

        val profile = RingProfileGenerator.generateRing(
            roundIndex = 2,
            stitches = stitches,
            gaugeStandard = standard,
            previousRing = baseRing
        )

        assertFalse("El anillo con alternancia pb y pa no debe ser planar", profile.isPlanar)
        assertTrue("maxZ debe ser estrictamente mayor que minZ", profile.maxZ > profile.minZ)

        // Los vértices con pa deben tener cota z significativamente mayor que los de pb
        val pbVertices = profile.vertices.filter { it.stitchType == StitchType.SingleCrochet }
        val paVertices = profile.vertices.filter { it.stitchType == StitchType.DoubleCrochet }

        val meanPbZ = pbVertices.map { it.z }.average()
        val meanPaZ = paVertices.map { it.z }.average()

        assertTrue(
            "La altura media de los pa ($meanPaZ) debe superar la de los pb ($meanPbZ)",
            meanPaZ > meanPbZ + (standard.stitchHeightMm * 0.5)
        )
    }

    @Test
    fun `azimuthal accumulated angle starts at zero and spans 2pi radians continuously`() {
        val stitches = listOf(
            StitchType.SingleCrochet,
            StitchType.Increase,
            StitchType.SingleCrochet,
            StitchType.HalfDoubleCrochet,
            StitchType.DoubleCrochet,
            StitchType.SingleCrochet
        )

        val profile = RingProfileGenerator.generateRing(
            roundIndex = 1,
            stitches = stitches,
            gaugeStandard = standard
        )

        assertEquals(6, profile.vertexCount)
        assertEquals(0.0, profile.vertices[0].theta, 1e-6)

        // Comprobar orden monótono estrictamente creciente
        for (i in 0 until profile.vertices.size - 1) {
            assertTrue(
                "theta_${i+1} debe ser estrictamente mayor que theta_$i",
                profile.vertices[i + 1].theta > profile.vertices[i].theta
            )
        }

        // El último vértice debe estar en un ángulo estrictamente menor que 2pi
        val lastVertex = profile.vertices.last()
        assertTrue(lastVertex.theta < 2 * PI)

        // El paso angular de cada puntada j debe coincidir con su fracción w_rel / sum(w_rel)
        val totalWRel = stitches.sumOf { it.wRel }
        var currentAccum = 0.0
        for ((idx, v) in profile.vertices.withIndex()) {
            val expectedTheta = 2 * PI * (currentAccum / totalWRel)
            assertEquals(expectedTheta, v.theta, 1e-5)
            currentAccum += stitches[idx].wRel
        }
        // Al terminar el último punto, el acumulado es exactamente 2*PI
        assertEquals(2 * PI, 2 * PI * (currentAccum / totalWRel), 1e-5)
    }

    @Test
    fun `volumetric stitches apply 3D normal displacement bumps`() {
        val stitches = listOf(
            StitchType.SingleCrochet,
            StitchType.BobbleStitch,
            StitchType.SingleCrochet,
            StitchType.PopcornStitch
        )

        val profile = RingProfileGenerator.generateRing(
            roundIndex = 3,
            stitches = stitches,
            gaugeStandard = standard
        )

        val bobbleVertex = profile.vertices[1]
        val popcornVertex = profile.vertices[3]

        assertTrue(bobbleVertex.normalBumpMm > 0.0)
        assertTrue(popcornVertex.normalBumpMm > 0.0)
        assertTrue(
            "Popcorn tiene mayor relieve volumétrico que Bobble",
            popcornVertex.normalBumpMm > bobbleVertex.normalBumpMm
        )

        val bobbleDist = hypot(bobbleVertex.x, bobbleVertex.y)
        assertTrue(
            "El relieve volumétrico desplaza el vértice radialmente hacia afuera",
            bobbleDist > bobbleVertex.radiusMm
        )
    }

    @Test
    fun `consecutive ring with increases applies conicity factor smoothly`() {
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

        assertTrue(ring2.meanRadiusMm > ring1.meanRadiusMm)
        assertTrue(ring2.minZ > ring1.maxZ)
    }

    @Test
    fun `empty stitches list returns safe empty profile without throwing`() {
        val profile = RingProfileGenerator.generateRing(
            roundIndex = 1,
            stitches = emptyList(),
            gaugeStandard = standard
        )

        assertEquals(0, profile.vertexCount)
        assertEquals(0.0, profile.circumferenceMm, 1e-4)
        assertEquals(0.0, profile.meanRadiusMm, 1e-4)
        assertNotNull(profile.vertices)
    }
}
