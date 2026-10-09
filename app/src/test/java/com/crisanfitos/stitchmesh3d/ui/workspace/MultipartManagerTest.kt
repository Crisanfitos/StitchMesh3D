package com.crisanfitos.stitchmesh3d.ui.workspace

import com.crisanfitos.stitchmesh3d.ui.dashboard.components.LinterValidationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas unitarias completas de la gestión de partes compuestas (CRUD de piezas en proyecto) (RF-4.1).
 * Verifica SM-031 y el aislamiento de vueltas y mallas 3D por parte.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MultipartManagerTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state contains multiple default parts with isolated rounds`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(3, state.parts.size)
        assertEquals("p1", state.selectedPartId)
        assertEquals("Cabeza", state.parts[0].name)
        assertEquals("Cuerpo", state.parts[1].name)
        assertEquals("Orejas (x2)", state.parts[2].name)

        // Vueltas activas iniciales corresponden a la cabeza (p1)
        assertEquals(3, state.rounds.size)
        assertEquals(3, state.roundsByPartId["p1"]?.size)
        assertEquals(1, state.roundsByPartId["p2"]?.size)
        assertEquals(1, state.roundsByPartId["p3"]?.size)
    }

    @Test
    fun `selecting different part switches active rounds and preserves isolated round history`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        // Editar una vuelta en la cabeza (p1)
        viewModel.processIntent(WorkspaceIntent.EditRoundInstruction("r2", "6 aum"))
        testScheduler.advanceUntilIdle()
        assertEquals(3, viewModel.state.value.rounds.size)

        // Cambiar al cuerpo (p2)
        viewModel.processIntent(WorkspaceIntent.SelectPart("p2"))
        testScheduler.advanceUntilIdle()

        val stateP2 = viewModel.state.value
        assertEquals("p2", stateP2.selectedPartId)
        assertEquals(1, stateP2.rounds.size)
        assertEquals("AM 6", stateP2.rounds.first().rawInstruction)
        assertEquals(LinterValidationStatus.VALID, stateP2.validationStatus)

        // Agregar una vuelta al cuerpo
        viewModel.processIntent(WorkspaceIntent.AddRound)
        testScheduler.advanceUntilIdle()
        assertEquals(2, viewModel.state.value.rounds.size)
        assertEquals(2, viewModel.state.value.parts.find { it.id == "p2" }?.roundCount)

        // Cambiar de nuevo a la cabeza (p1) y verificar que sus 3 vueltas siguen intactas
        viewModel.processIntent(WorkspaceIntent.SelectPart("p1"))
        testScheduler.advanceUntilIdle()

        val stateP1 = viewModel.state.value
        assertEquals("p1", stateP1.selectedPartId)
        assertEquals(3, stateP1.rounds.size)
        assertEquals(3, stateP1.parts.find { it.id == "p1" }?.roundCount)
    }

    @Test
    fun `creating a new part adds it with unique name, default initial round and selects it`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        val initialCount = viewModel.state.value.parts.size

        viewModel.processIntent(WorkspaceIntent.CreatePart(name = "Brazo Izquierdo", colorHex = "#52A474"))
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(initialCount + 1, state.parts.size)

        val newPart = state.parts.last()
        assertEquals("Brazo Izquierdo", newPart.name)
        assertEquals("#52A474", newPart.colorHex)
        assertEquals(1, newPart.roundCount)
        assertEquals(newPart.id, state.selectedPartId)

        // Tiene 1 vuelta inicial creada automáticamente
        assertEquals(1, state.rounds.size)
        assertEquals("AM 6", state.rounds.first().rawInstruction)
        assertEquals(LinterValidationStatus.VALID, state.validationStatus)
        assertNotNull(state.meshGeometry)
    }

    @Test
    fun `creating part with blank name or duplicate name fails validation`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        val initialCount = viewModel.state.value.parts.size

        // Nombre en blanco
        viewModel.processIntent(WorkspaceIntent.CreatePart(name = "   "))
        testScheduler.advanceUntilIdle()
        assertEquals(initialCount, viewModel.state.value.parts.size)
        assertNotNull(viewModel.state.value.partActionError)

        // Nombre duplicado (case insensitive)
        viewModel.processIntent(WorkspaceIntent.CreatePart(name = "cabeza"))
        testScheduler.advanceUntilIdle()
        assertEquals(initialCount, viewModel.state.value.parts.size)
        assertNotNull(viewModel.state.value.partActionError)
    }

    @Test
    fun `renaming a part updates its title and prevents duplicate naming`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        // Renombrar p1 a "Cabeza Principal"
        viewModel.processIntent(WorkspaceIntent.ConfirmRenamePart(partId = "p1", newName = "Cabeza Principal"))
        testScheduler.advanceUntilIdle()

        assertEquals("Cabeza Principal", viewModel.state.value.parts.first { it.id == "p1" }.name)

        // Intentar renombrar p2 a "Cabeza Principal" (colisión)
        viewModel.processIntent(WorkspaceIntent.ConfirmRenamePart(partId = "p2", newName = "Cabeza Principal"))
        testScheduler.advanceUntilIdle()

        assertEquals("Cuerpo", viewModel.state.value.parts.first { it.id == "p2" }.name)
        assertNotNull(viewModel.state.value.partActionError)
    }

    @Test
    fun `duplicating a part creates a deep copy with all rounds and unique name`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        // p1 tiene 3 vueltas
        viewModel.processIntent(WorkspaceIntent.DuplicatePart(partId = "p1"))
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(4, state.parts.size)

        val duplicated = state.parts.last()
        assertEquals("Cabeza (Copia)", duplicated.name)
        assertEquals(3, duplicated.roundCount)
        assertEquals(duplicated.id, state.selectedPartId)
        assertEquals(3, state.rounds.size)

        // Verificar que los IDs de vueltas son distintos
        val originalRounds = state.roundsByPartId["p1"]!!
        val duplicatedRounds = state.roundsByPartId[duplicated.id]!!
        assertNotEquals(originalRounds[0].id, duplicatedRounds[0].id)
        assertEquals(originalRounds[0].rawInstruction, duplicatedRounds[0].rawInstruction)
    }

    @Test
    fun `deleting a part removes it, its rounds and switches selection to remaining part`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        assertEquals(3, viewModel.state.value.parts.size)

        // Eliminar la parte actualmente seleccionada (p1)
        viewModel.processIntent(WorkspaceIntent.ConfirmDeletePart(partId = "p1"))
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.parts.size)
        assertFalse(state.parts.any { it.id == "p1" })
        assertFalse(state.roundsByPartId.containsKey("p1"))

        // Selección automática migrada a otra parte existente (ej. p2)
        assertEquals("p2", state.selectedPartId)
        assertEquals(1, state.rounds.size)
    }

    @Test
    fun `prevent deleting the last remaining part of the project`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        viewModel.processIntent(WorkspaceIntent.ConfirmDeletePart("p1"))
        testScheduler.advanceUntilIdle()
        viewModel.processIntent(WorkspaceIntent.ConfirmDeletePart("p2"))
        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.parts.size)
        val lastPartId = viewModel.state.value.parts.first().id

        // Intentar eliminar la última pieza
        viewModel.processIntent(WorkspaceIntent.ConfirmDeletePart(lastPartId))
        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.parts.size)
    }

    @Test
    fun `reordering parts updates sortOrder list`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        // Mover p1 (índice 0) al índice 2
        viewModel.processIntent(WorkspaceIntent.ReorderPart(partId = "p1", toIndex = 2))
        testScheduler.advanceUntilIdle()

        val parts = viewModel.state.value.parts
        assertEquals("p2", parts[0].id)
        assertEquals(0, parts[0].sortOrder)
        assertEquals("p3", parts[1].id)
        assertEquals(1, parts[1].sortOrder)
        assertEquals("p1", parts[2].id)
        assertEquals(2, parts[2].sortOrder)
    }
}
