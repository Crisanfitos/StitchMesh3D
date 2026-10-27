package com.crisanfitos.stitchmesh3d.ui.assembly

import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import com.crisanfitos.stitchmesh3d.ui.assembly.components.PartTransformState
import com.crisanfitos.stitchmesh3d.ui.assembly.components.SceneGraphItem
import com.crisanfitos.stitchmesh3d.ui.workspace.ProjectPartUiModel
import com.crisanfitos.stitchmesh3d.ui.workspace.ProjectPartsSessionStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

/**
 * Tests unitarios para el algoritmo de auto-disposición y alineación espacial
 * "Shelf / Side-by-Side" en el visor de ensamblado (SM-076).
 */
class AssemblyLayoutPlannerTest {

    @Before
    fun setUp() {
        ProjectPartsSessionStore.clear()
    }

    @After
    fun tearDown() {
        ProjectPartsSessionStore.clear()
    }

    private fun makeParts(count: Int): List<SceneGraphItem> {
        return (1..count).map { idx ->
            SceneGraphItem(
                id = "p$idx",
                name = "Pieza $idx",
                roundCount = 6,
                stitchCount = 36,
                topologyType = PartTopologyType.CLOSED_FILLED
            )
        }
    }

    @Test
    fun `computeShelfLayout for N=1, 2 and 5 parts produces distinct non-overlapping X coordinates`() {
        for (n in listOf(1, 2, 5)) {
            val parts = makeParts(n)
            val transforms = AssemblyLayoutPlanner.computeShelfLayout(parts)

            assertEquals(n, transforms.size)
            assertFalse(
                "No debe haber colisiones ni solapamientos en el layout para N=$n",
                AssemblyLayoutPlanner.hasAnySpatialOverlap(transforms.values, minSeparationMm = 40.0f)
            )

            val xCoords = parts.map { transforms[it.id]!!.posX }
            // Verificar que X_i != X_j para todo i != j y que están ordenadas de izquierda a derecha
            for (i in 0 until n) {
                assertEquals(0f, transforms[parts[i].id]!!.posY, 0.001f)
                assertEquals(0f, transforms[parts[i].id]!!.posZ, 0.001f)
                for (j in (i + 1) until n) {
                    assertNotEquals(xCoords[i], xCoords[j], 1.0f)
                    assertTrue("Las piezas deben ordenarse de izquierda a derecha", xCoords[i] < xCoords[j])
                }
            }

            // Verificar centrado simétrico respecto al origen (promedio de X == 0)
            val meanX = xCoords.average().toFloat()
            assertEquals("La fila horizontal para N=$n debe estar centrada en X=0", 0f, meanX, 0.01f)
        }
    }

    @Test
    fun `computeShelfLayout untangles parts that were all stacked at origin (0, 0, 0)`() {
        val parts = makeParts(3)
        val stackedAtOrigin = parts.associate {
            it.id to PartTransformState(partId = it.id, partName = it.name, posX = 0f, posY = 0f, posZ = 0f)
        }

        val resolved = AssemblyLayoutPlanner.computeShelfLayout(
            parts = parts,
            existingTransforms = stackedAtOrigin,
            forceRealignAll = false
        )

        assertFalse(
            "Las piezas apiladas en (0,0,0) deben desplegarse automáticamente sin colisión",
            AssemblyLayoutPlanner.hasAnySpatialOverlap(resolved.values, minSeparationMm = 40.0f)
        )
        assertTrue(resolved["p1"]!!.posX < 0f)
        assertEquals(0f, resolved["p2"]!!.posX, 0.01f)
        assertTrue(resolved["p3"]!!.posX > 0f)
    }

    @Test
    fun `adding a new part to an ongoing assembly appends it at the end of the shelf without moving placed parts`() {
        val initialParts = makeParts(2)
        val customTransforms = mapOf(
            "p1" to PartTransformState("p1", "Pieza 1", posX = -12f, posY = 25f, posZ = 5f),
            "p2" to PartTransformState("p2", "Pieza 2", posX = 30f, posY = -10f, posZ = 0f)
        )

        val newPart = SceneGraphItem(
            id = "p3",
            name = "Pieza Nueva",
            roundCount = 6,
            stitchCount = 36
        )
        val updatedParts = initialParts + newPart

        val layout = AssemblyLayoutPlanner.computeShelfLayout(
            parts = updatedParts,
            existingTransforms = customTransforms,
            forceRealignAll = false
        )

        // Las piezas p1 y p2 conservan intacta su posición manipulada por el usuario
        assertEquals(-12f, layout["p1"]!!.posX, 0.001f)
        assertEquals(25f, layout["p1"]!!.posY, 0.001f)
        assertEquals(30f, layout["p2"]!!.posX, 0.001f)
        assertEquals(-10f, layout["p2"]!!.posY, 0.001f)

        // La nueva pieza p3 se coloca al final de la fila (posX > max(p1.posX, p2.posX))
        assertTrue(
            "La nueva pieza (${layout["p3"]!!.posX}) debe colocarse a la derecha de p2 (30f)",
            layout["p3"]!!.posX >= 30f + AssemblyLayoutPlanner.MIN_SHELF_SPACING_MM
        )
    }

    @Test
    fun `computeEffectiveSpacingMm scales proportionally with tension stitchWidthMm`() {
        val parts = makeParts(3)
        val fineSpacing = AssemblyLayoutPlanner.computeEffectiveSpacingMm(parts, stitchWidthMm = 2.5f)
        val standardSpacing = AssemblyLayoutPlanner.computeEffectiveSpacingMm(parts, stitchWidthMm = 3.85f)
        val jumboSpacing = AssemblyLayoutPlanner.computeEffectiveSpacingMm(parts, stitchWidthMm = 7.5f)

        assertTrue("El espaciado debe crecer con muestras de tensión más gruesas", fineSpacing < standardSpacing)
        assertTrue("El espaciado Jumbo debe ser mayor que el estándar", standardSpacing < jumboSpacing)
    }

    @Test
    fun `AssemblyViewModel AutoAlignShelfLayout intent realigns unlocked parts side by side on X axis`() {
        ProjectPartsSessionStore.publishFromWorkbench(
            projectId = "proj-shelf-test",
            projectTitle = "Proyecto Shelf",
            parts = listOf(
                ProjectPartUiModel("p1", "Cabeza", 4, "#E06D53"),
                ProjectPartUiModel("p2", "Cuerpo", 6, "#F2C94C"),
                ProjectPartUiModel("p3", "Brazo", 3, "#52A474")
            ),
            roundsByPartId = emptyMap()
        )

        val vm = AssemblyViewModel()
        vm.processIntent(AssemblyIntent.LoadProject("proj-shelf-test"))

        // Mover una pieza manualmente fuera de la fila
        vm.processIntent(AssemblyIntent.UpdatePosition("p1", 120f, 85f, -40f))
        assertEquals(85f, vm.state.value.transformsByPartId["p1"]!!.posY, 0.001f)

        // Ejecutar acción "Alinear piezas en cuadrícula"
        vm.processIntent(AssemblyIntent.AutoAlignShelfLayout)

        val transforms = vm.state.value.transformsByPartId
        assertEquals(0f, transforms["p1"]!!.posY, 0.001f)
        assertEquals(0f, transforms["p2"]!!.posY, 0.001f)
        assertEquals(0f, transforms["p3"]!!.posY, 0.001f)
        assertTrue(transforms["p1"]!!.posX < transforms["p2"]!!.posX)
        assertTrue(transforms["p2"]!!.posX < transforms["p3"]!!.posX)
        assertTrue(abs(transforms["p2"]!!.posX) < 0.01f)
    }
}
