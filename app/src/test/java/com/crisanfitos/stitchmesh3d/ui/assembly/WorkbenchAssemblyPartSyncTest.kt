package com.crisanfitos.stitchmesh3d.ui.assembly

import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import com.crisanfitos.stitchmesh3d.ui.workspace.ProjectPartsSessionStore
import com.crisanfitos.stitchmesh3d.ui.workspace.WorkspaceIntent
import com.crisanfitos.stitchmesh3d.ui.workspace.WorkspaceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios de sincronización reactiva bidireccional de piezas entre el Editor de Vueltas (Workbench)
 * y el Scene Graph de Ensamblado 3D (SM-075).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkbenchAssemblyPartSyncTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        ProjectPartsSessionStore.clear()
    }

    @After
    fun tearDown() {
        ProjectPartsSessionStore.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `navigating from Workbench to Assembly loads exact project parts, round counts, stitch counts and colors`() = runTest(testDispatcher) {
        val workspaceVm = WorkspaceViewModel(testDispatcher)
        workspaceVm.processIntent(WorkspaceIntent.LoadProject("proj-fox-01"))
        testScheduler.advanceUntilIdle()

        val assemblyVm = AssemblyViewModel()
        assemblyVm.processIntent(AssemblyIntent.LoadProject("proj-fox-01"))

        val wsParts = workspaceVm.state.value.parts
        val asmParts = assemblyVm.state.value.parts

        assertEquals(wsParts.size, asmParts.size)
        for (i in wsParts.indices) {
            assertEquals(wsParts[i].id, asmParts[i].id)
            assertEquals(wsParts[i].name, asmParts[i].name)
            assertEquals(wsParts[i].colorHex, asmParts[i].colorHex)
            assertEquals(wsParts[i].roundCount, asmParts[i].roundCount)
            assertEquals(wsParts[i].topologyType, asmParts[i].topologyType)
        }

        // p1 ("Cabeza") tiene 3 vueltas con 6 + 12 + 18 = 36 puntos producidos
        val headNode = asmParts.first { it.id == "p1" }
        assertEquals(3, headNode.roundCount)
        assertEquals(36, headNode.stitchCount)
    }

    @Test
    fun `creating, renaming or deleting a part in Workbench immediately reflects in Assembly SceneGraph`() = runTest(testDispatcher) {
        val workspaceVm = WorkspaceViewModel(testDispatcher)
        workspaceVm.processIntent(WorkspaceIntent.LoadProject("proj-sync-02"))
        testScheduler.advanceUntilIdle()

        // 1. Añadir pieza nueva en el Workbench
        workspaceVm.processIntent(
            WorkspaceIntent.CreatePart(
                name = "Cola Esponjosa",
                colorHex = "#E5A93C",
                topologyType = PartTopologyType.SEMI_CLOSED_TUBE
            )
        )
        // 2. Renombrar pieza existente p1
        workspaceVm.processIntent(WorkspaceIntent.ConfirmRenamePart("p1", "Cabeza Zorro"))
        // 3. Eliminar pieza p2
        workspaceVm.processIntent(WorkspaceIntent.ConfirmDeletePart("p2"))
        testScheduler.advanceUntilIdle()

        val assemblyVm = AssemblyViewModel()
        assemblyVm.processIntent(AssemblyIntent.LoadProject("proj-sync-02"))

        val asmParts = assemblyVm.state.value.parts
        assertEquals(3, asmParts.size)
        assertTrue(asmParts.any { it.name == "Cabeza Zorro" && it.id == "p1" })
        assertFalse(asmParts.any { it.id == "p2" })
        val tailPart = asmParts.firstOrNull { it.name == "Cola Esponjosa" }
        assertNotNull(tailPart)
        assertEquals("#E5A93C", tailPart!!.colorHex)
        assertEquals(PartTopologyType.SEMI_CLOSED_TUBE, tailPart.topologyType)
    }

    @Test
    fun `spatial transforms and part edits in Assembly persist when returning to Workbench and reopening Assembly`() = runTest(testDispatcher) {
        val workspaceVm = WorkspaceViewModel(testDispatcher)
        workspaceVm.processIntent(WorkspaceIntent.LoadProject("proj-trs-03"))
        testScheduler.advanceUntilIdle()

        val assemblyVm = AssemblyViewModel()
        assemblyVm.processIntent(AssemblyIntent.LoadProject("proj-trs-03"))

        // Mover y rotar p1 en el ensamblador y cambiar color de p3
        assemblyVm.processIntent(AssemblyIntent.UpdatePosition("p1", 18.5f, 42.0f, -12.0f))
        assemblyVm.processIntent(AssemblyIntent.UpdateRotation("p1", 10f, 25f, -15f))
        assemblyVm.processIntent(AssemblyIntent.UpdateColor("p3", "#3B82F6"))
        assemblyVm.processIntent(AssemblyIntent.RenamePart("p3", "Orejas Azules"))

        // Volver al Workbench y sincronizar sesión
        workspaceVm.processIntent(WorkspaceIntent.SyncFromSession)
        testScheduler.advanceUntilIdle()
        val updatedP3 = workspaceVm.state.value.parts.first { it.id == "p3" }
        assertEquals("Orejas Azules", updatedP3.name)
        assertEquals("#3B82F6", updatedP3.colorHex)

        // Añadir una vuelta más en el Workbench y reabrir una nueva instancia de AssemblyViewModel
        workspaceVm.processIntent(WorkspaceIntent.AddRound)
        testScheduler.advanceUntilIdle()

        val reopenedAssemblyVm = AssemblyViewModel()
        reopenedAssemblyVm.processIntent(AssemblyIntent.LoadProject("proj-trs-03"))

        val restoredTransform = reopenedAssemblyVm.state.value.transformsByPartId["p1"]
        assertNotNull(restoredTransform)
        assertEquals(18.5f, restoredTransform!!.posX, 0.001f)
        assertEquals(42.0f, restoredTransform.posY, 0.001f)
        assertEquals(-12.0f, restoredTransform.posZ, 0.001f)
        assertEquals(10f, restoredTransform.rotX, 0.001f)
        assertEquals(25f, restoredTransform.rotY, 0.001f)
        assertEquals(-15f, restoredTransform.rotZ, 0.001f)
    }
}
