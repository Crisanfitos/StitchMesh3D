package com.crisanfitos.stitchmesh3d.ui.assembly

import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias de arquitectura MVI para [AssemblyViewModel] (SM-055).
 * Valida la gestión de partes, transformaciones numéricas espaciales, modos de gizmo y CRUD.
 */
class AssemblyViewModelTest {

    private lateinit var viewModel: AssemblyViewModel

    @Before
    fun setUp() {
        viewModel = AssemblyViewModel()
    }

    @Test
    fun initialState_loadsDefaultAmigurumiPartsAndTransforms() {
        val state = viewModel.state.value

        assertTrue(state.parts.isNotEmpty())
        assertEquals(6, state.parts.size)
        assertEquals("part-torso", state.selectedPartId)
        assertEquals(GizmoMode.TRANSLATE, state.activeGizmoMode)
        assertTrue(state.showCoordinateGrid)
        assertFalse(state.isWireframe)

        assertNotNull(state.selectedPart)
        assertEquals("Cuerpo Principal", state.selectedPart?.name)
        assertNotNull(state.selectedTransform)
        assertEquals(0f, state.selectedTransform?.posX)
    }

    @Test
    fun selectPart_updatesSelectedPartIdAndSelectedTransform() {
        viewModel.processIntent(AssemblyIntent.SelectPart("part-head"))

        val state = viewModel.state.value
        assertEquals("part-head", state.selectedPartId)
        assertEquals("Cabeza", state.selectedPart?.name)
        assertEquals(45f, state.selectedTransform?.posY)
    }

    @Test
    fun toggleVisibility_flipsIsVisible() {
        val initialPart = viewModel.state.value.parts.first { it.id == "part-head" }
        assertTrue(initialPart.isVisible)

        viewModel.processIntent(AssemblyIntent.ToggleVisibility("part-head"))

        val updatedPart = viewModel.state.value.parts.first { it.id == "part-head" }
        assertFalse(updatedPart.isVisible)

        viewModel.processIntent(AssemblyIntent.ToggleVisibility("part-head"))
        val toggledBack = viewModel.state.value.parts.first { it.id == "part-head" }
        assertTrue(toggledBack.isVisible)
    }

    @Test
    fun toggleLock_locksPartAndTransformState() {
        viewModel.processIntent(AssemblyIntent.ToggleLock("part-head"))

        val state = viewModel.state.value
        val part = state.parts.first { it.id == "part-head" }
        val transform = state.transformsByPartId["part-head"]

        assertTrue(part.isLocked)
        assertNotNull(transform)
        assertTrue(transform!!.isLocked)
    }

    @Test
    fun updatePosition_whenUnlocked_updatesCoordinates() {
        viewModel.processIntent(AssemblyIntent.UpdatePosition("part-head", 10.5f, 52.0f, -8.0f))

        val transform = viewModel.state.value.transformsByPartId["part-head"]
        assertNotNull(transform)
        assertEquals(10.5f, transform!!.posX, 0.001f)
        assertEquals(52.0f, transform.posY, 0.001f)
        assertEquals(-8.0f, transform.posZ, 0.001f)
    }

    @Test
    fun updatePosition_whenLocked_doesNotModifyCoordinates() {
        val initialY = viewModel.state.value.transformsByPartId["part-head"]?.posY ?: 0f
        viewModel.processIntent(AssemblyIntent.ToggleLock("part-head"))
        viewModel.processIntent(AssemblyIntent.UpdatePosition("part-head", 99f, 99f, 99f))

        val transform = viewModel.state.value.transformsByPartId["part-head"]
        assertNotNull(transform)
        assertEquals(initialY, transform!!.posY, 0.001f)
    }

    @Test
    fun updateRotation_updatesOrientationAngles() {
        viewModel.processIntent(AssemblyIntent.UpdateRotation("part-arm-left", 15f, 30f, 45f))

        val transform = viewModel.state.value.transformsByPartId["part-arm-left"]
        assertNotNull(transform)
        assertEquals(15f, transform!!.rotX, 0.001f)
        assertEquals(30f, transform.rotY, 0.001f)
        assertEquals(45f, transform.rotZ, 0.001f)
    }

    @Test
    fun mirrorSagittal_invertsXAndYaw() {
        val initialX = viewModel.state.value.transformsByPartId["part-arm-right"]?.posX ?: 0f
        viewModel.processIntent(AssemblyIntent.ToggleMirror("part-arm-right"))

        val transform = viewModel.state.value.transformsByPartId["part-arm-right"]
        assertNotNull(transform)
        assertEquals(-initialX, transform!!.posX, 0.001f)
        assertTrue(transform.isMirrored)
    }

    @Test
    fun resetOrigin_resetsCoordinatesToZero() {
        viewModel.processIntent(AssemblyIntent.UpdatePosition("part-head", 20f, 50f, 10f))
        viewModel.processIntent(AssemblyIntent.ResetOrigin("part-head"))

        val transform = viewModel.state.value.transformsByPartId["part-head"]
        assertNotNull(transform)
        assertEquals(0f, transform!!.posX, 0.001f)
        assertEquals(0f, transform.posY, 0.001f)
        assertEquals(0f, transform.posZ, 0.001f)
    }

    @Test
    fun setGizmoMode_updatesActiveGizmoMode() {
        viewModel.processIntent(AssemblyIntent.SetGizmoMode(GizmoMode.ROTATE))
        assertEquals(GizmoMode.ROTATE, viewModel.state.value.activeGizmoMode)

        viewModel.processIntent(AssemblyIntent.SetGizmoMode(GizmoMode.NONE))
        assertEquals(GizmoMode.NONE, viewModel.state.value.activeGizmoMode)
    }

    @Test
    fun toggleWireframeAndGrid_flipsFlags() {
        viewModel.processIntent(AssemblyIntent.ToggleWireframe)
        assertTrue(viewModel.state.value.isWireframe)

        viewModel.processIntent(AssemblyIntent.ToggleCoordinateGrid)
        assertFalse(viewModel.state.value.showCoordinateGrid)
    }

    @Test
    fun cameraGestures_modifyAndResetCameraTransform() {
        viewModel.processIntent(AssemblyIntent.UpdateOrbit(15f, 10f))
        assertEquals(60f, viewModel.state.value.orbitYaw, 0.01f)
        assertEquals(40f, viewModel.state.value.orbitPitch, 0.01f)

        viewModel.processIntent(AssemblyIntent.UpdateZoom(1.5f))
        assertEquals(1.5f, viewModel.state.value.zoomScale, 0.01f)

        viewModel.processIntent(AssemblyIntent.ResetCamera)
        assertEquals(45f, viewModel.state.value.orbitYaw, 0.01f)
        assertEquals(30f, viewModel.state.value.orbitPitch, 0.01f)
        assertEquals(1.0f, viewModel.state.value.zoomScale, 0.01f)
    }

    @Test
    fun createPart_addsPartAndSetsAsSelected() {
        val initialCount = viewModel.state.value.parts.size
        viewModel.processIntent(
            AssemblyIntent.CreatePart(
                name = "Oreja Izquierda",
                colorHex = "#E06D53",
                topologyType = PartTopologyType.CLOSED_FILLED
            )
        )

        val state = viewModel.state.value
        assertEquals(initialCount + 1, state.parts.size)
        val created = state.parts.last()
        assertEquals("Oreja Izquierda", created.name)
        assertEquals(created.id, state.selectedPartId)
        assertNotNull(state.transformsByPartId[created.id])
    }

    @Test
    fun duplicatePart_createsCloneWithPositionOffset() {
        val initialCount = viewModel.state.value.parts.size
        viewModel.processIntent(AssemblyIntent.DuplicatePart("part-head"))

        val state = viewModel.state.value
        assertEquals(initialCount + 1, state.parts.size)
        val duplicated = state.parts.last()
        assertEquals("Cabeza (Copia)", duplicated.name)
        assertEquals(duplicated.id, state.selectedPartId)
        val transform = state.transformsByPartId[duplicated.id]
        assertNotNull(transform)
        assertEquals(15f, transform!!.posX, 0.001f)
    }

    @Test
    fun renamePart_updatesNameInPartAndTransform() {
        viewModel.processIntent(AssemblyIntent.RenamePart("part-head", "Cabeza Modificada"))

        val state = viewModel.state.value
        val part = state.parts.first { it.id == "part-head" }
        val transform = state.transformsByPartId["part-head"]

        assertEquals("Cabeza Modificada", part.name)
        assertEquals("Cabeza Modificada", transform?.partName)
    }

    @Test
    fun deletePart_removesPartAndUpdatesSelection() {
        viewModel.processIntent(AssemblyIntent.SelectPart("part-head"))
        viewModel.processIntent(AssemblyIntent.DeletePart("part-head"))

        val state = viewModel.state.value
        assertFalse(state.parts.any { it.id == "part-head" })
        assertFalse(state.transformsByPartId.containsKey("part-head"))
        assertTrue(state.selectedPartId != "part-head")
    }
}
