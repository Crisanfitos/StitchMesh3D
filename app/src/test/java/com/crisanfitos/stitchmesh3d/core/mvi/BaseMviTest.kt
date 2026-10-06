package com.crisanfitos.stitchmesh3d.core.mvi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

// Implementaciones de prueba para el flujo MVI
data class TestState(val count: Int = 0, val text: String = "") : ViewState

sealed interface TestIntent : ViewIntent {
    data class Increment(val amount: Int = 1) : TestIntent
    data class SetText(val newText: String) : TestIntent
    object TriggerEffect : TestIntent
}

sealed interface TestEffect : ViewEffect {
    data class ShowToast(val message: String) : TestEffect
}

class TestViewModel(initial: TestState = TestState()) :
    BaseViewModel<TestState, TestIntent, TestEffect>(initial) {

    override fun processIntent(intent: TestIntent) {
        when (intent) {
            is TestIntent.Increment -> setState { copy(count = count + intent.amount) }
            is TestIntent.SetText -> setState { copy(text = intent.newText) }
            is TestIntent.TriggerEffect -> sendEffect(TestEffect.ShowToast("Alerta"))
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BaseMviTest {

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
    fun initialState_isReflectedCorrectly() = runTest(testDispatcher) {
        val viewModel = TestViewModel(TestState(count = 10, text = "Init"))
        assertEquals(10, viewModel.state.value.count)
        assertEquals("Init", viewModel.state.value.text)
    }

    @Test
    fun processIntent_updatesStateAtomically() = runTest(testDispatcher) {
        val viewModel = TestViewModel()

        viewModel.processIntent(TestIntent.Increment(5))
        assertEquals(5, viewModel.state.value.count)

        viewModel.processIntent(TestIntent.SetText("StitchMesh"))
        assertEquals("StitchMesh", viewModel.state.value.text)

        viewModel.processIntent(TestIntent.Increment(3))
        assertEquals(8, viewModel.state.value.count)
    }

    @Test
    fun sendEffect_emitsSideEffectToSharedFlow() = runTest(testDispatcher) {
        val viewModel = TestViewModel()
        var receivedEffect: TestEffect? = null

        val job = launch {
            receivedEffect = viewModel.effect.first()
        }

        viewModel.processIntent(TestIntent.TriggerEffect)
        advanceUntilIdle()

        assertEquals(TestEffect.ShowToast("Alerta"), receivedEffect)
        job.cancel()
    }
}
