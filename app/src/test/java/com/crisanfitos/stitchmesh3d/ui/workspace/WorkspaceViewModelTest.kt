package com.crisanfitos.stitchmesh3d.ui.workspace

import com.crisanfitos.stitchmesh3d.ui.dashboard.components.LinterValidationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModelTest {

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
    fun `initial state has valid default rounds and valid validation status`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Zorro Amigurumi", state.projectTitle)
        assertEquals(3, state.rounds.size)
        assertEquals(LinterValidationStatus.VALID, state.validationStatus)
        assertEquals(0, state.inconsistencyCount)
        assertFalse(state.isMeshFrozenDueToError)
        assertNotNull("La malla 3D debe estar generada para el patrón inicial válido", state.meshGeometry)
    }

    @Test
    fun `editing round with arithmetic error freezes mesh and sets error status`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        val initialMesh = viewModel.state.value.meshGeometry
        assertNotNull(initialMesh)

        // Introducir una inconsistencia: V3 consume 10 pts pero la base disponible es 12
        viewModel.processIntent(WorkspaceIntent.EditRoundInstruction("r3", "[1 pb, 1 aum] * 5"))
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(LinterValidationStatus.HAS_ERRORS, state.validationStatus)
        assertTrue(state.isMeshFrozenDueToError)
        assertEquals(1, state.inconsistencyCount)
        // La malla 3D previa se preserva congelada sin crashear el renderizado (SM-029)
        assertEquals(initialMesh, state.meshGeometry)
        assertNotNull(state.lastValidMesh)
    }

    @Test
    fun `correcting arithmetic error regenerates 3D mesh and restores valid status`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        // Introducir error
        viewModel.processIntent(WorkspaceIntent.EditRoundInstruction("r3", "[1 pb, 1 aum] * 5"))
        testScheduler.advanceUntilIdle()
        assertEquals(LinterValidationStatus.HAS_ERRORS, viewModel.state.value.validationStatus)

        // Corregir error
        viewModel.processIntent(WorkspaceIntent.EditRoundInstruction("r3", "[1 pb, 1 aum] * 6"))
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(LinterValidationStatus.VALID, state.validationStatus)
        assertFalse(state.isMeshFrozenDueToError)
        assertEquals(0, state.inconsistencyCount)
        assertNotNull(state.meshGeometry)
    }

    @Test
    fun `adding round creates instruction without parentheses and updates active selection`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        assertEquals(3, viewModel.state.value.rounds.size)

        viewModel.processIntent(WorkspaceIntent.AddRound)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(4, state.rounds.size)
        val newRound = state.rounds.last()
        assertEquals("r4", newRound.id)
        assertEquals(4, newRound.roundNumber)
        // La nueva vuelta no incluye (18) entre paréntesis en su texto crudo
        assertEquals("18 pb", newRound.rawInstruction)
        assertFalse("No debe contener paréntesis con recuento declarado", newRound.rawInstruction.contains("("))
        assertEquals(newRound.id, state.selectedRoundId)
    }

    @Test
    fun `selecting round updates selectedRoundId`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        viewModel.processIntent(WorkspaceIntent.SelectRound("r2"))
        testScheduler.advanceUntilIdle()

        assertEquals("r2", viewModel.state.value.selectedRoundId)
    }

    @Test
    fun `inserting token via quick keyboard targets active round`() = runTest(testDispatcher) {
        val viewModel = WorkspaceViewModel(defaultDispatcher = testDispatcher)
        testScheduler.advanceUntilIdle()

        // Seleccionar vuelta 2 ("6 aum")
        viewModel.processIntent(WorkspaceIntent.SelectRound("r2"))
        testScheduler.advanceUntilIdle()

        viewModel.processIntent(WorkspaceIntent.InsertTokenAtActiveRound("pe"))
        testScheduler.advanceUntilIdle()

        val r2 = viewModel.state.value.rounds.first { it.id == "r2" }
        assertEquals("6 aum pe", r2.rawInstruction)
    }
}
