package com.crisanfitos.stitchmesh3d.ui.workspace.components

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class RoundItemRowTest {

    @Test
    fun `RoundItemUiModel sets valid state correctly`() {
        val model = RoundItemUiModel(
            id = "round-1",
            roundNumber = 1,
            rawInstruction = "6 pb",
            producedStitches = 6,
            consumedStitches = 0,
            declaredStitches = 6,
            isValid = true
        )

        assertEquals("round-1", model.id)
        assertEquals(1, model.roundNumber)
        assertEquals("6 pb", model.rawInstruction)
        assertEquals(6, model.producedStitches)
        assertEquals(0, model.consumedStitches)
        assertEquals(6, model.declaredStitches)
        assertTrue(model.isValid)
        assertNull(model.errorMessage)
        assertFalse(model.isHighlighted)
    }

    @Test
    fun `RoundItemUiModel sets invalid state with error message`() {
        val model = RoundItemUiModel(
            id = "round-2",
            roundNumber = 3,
            rawInstruction = "[1 pb, 1 aum] * 5 (15)",
            producedStitches = 15,
            consumedStitches = 10,
            declaredStitches = 15,
            isValid = false,
            errorMessage = "Faltan 2 puntos base de la vuelta anterior",
            isHighlighted = true
        )

        assertEquals("round-2", model.id)
        assertEquals(3, model.roundNumber)
        assertFalse(model.isValid)
        assertEquals("Faltan 2 puntos base de la vuelta anterior", model.errorMessage)
        assertTrue(model.isHighlighted)
    }

    @Test
    fun `debounce logic collapses rapid input changes within window`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val textFlow = MutableSharedFlow<String>()
        val emittedValues = mutableListOf<String>()

        val job = launch(testDispatcher) {
            textFlow.debounce(90L).toList(emittedValues)
        }

        // Simular escritura rápida: "6", "6 ", "6 p", "6 pb"
        textFlow.emit("6")
        testScheduler.advanceTimeBy(30L)
        textFlow.emit("6 ")
        testScheduler.advanceTimeBy(30L)
        textFlow.emit("6 p")
        testScheduler.advanceTimeBy(30L)
        textFlow.emit("6 pb")

        // Antes de que venza el debounce (90ms desde el último cambio)
        testScheduler.advanceTimeBy(50L)
        assertEquals(0, emittedValues.size)

        // Superar los 90ms de inactividad
        testScheduler.advanceTimeBy(50L)
        assertEquals(listOf("6 pb"), emittedValues)

        job.cancel()
    }
}
