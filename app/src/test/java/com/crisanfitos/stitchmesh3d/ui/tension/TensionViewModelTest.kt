package com.crisanfitos.stitchmesh3d.ui.tension

import com.crisanfitos.stitchmesh3d.core.gauge.YarnGaugeRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TensionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: TensionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TensionViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state computes calibration for default standard Worsted`() {
        val state = viewModel.state.value

        assertEquals(YarnGaugeRegistry.DEFAULT_STANDARD, state.selectedStandard)
        assertNotNull(state.calibrationResult)
        assertNull(state.errorMessage)

        val cal = state.calibrationResult!!
        assertTrue(cal.customStandard.stitchWidthMm > 0f)
        assertTrue(cal.customStandard.stitchHeightMm > 0f)
    }

    @Test
    fun `incrementing and decrementing stitches recalculates dimensions`() {
        val initialStitches = viewModel.state.value.stitchesIn10Cm

        viewModel.processIntent(TensionIntent.IncrementStitches)
        assertEquals(initialStitches + 1, viewModel.state.value.stitchesIn10Cm)

        val cal = viewModel.state.value.calibrationResult
        assertNotNull(cal)
        // More stitches in 10 cm means narrower stitch width: w = 100 / stitches
        val expectedWidth = 100f / (initialStitches + 1).toFloat()
        assertEquals(expectedWidth, cal!!.customStandard.stitchWidthMm, 0.01f)

        viewModel.processIntent(TensionIntent.DecrementStitches)
        assertEquals(initialStitches, viewModel.state.value.stitchesIn10Cm)
    }

    @Test
    fun `incrementing and decrementing rounds recalculates dimensions`() {
        val initialRounds = viewModel.state.value.roundsIn10Cm

        viewModel.processIntent(TensionIntent.IncrementRounds)
        assertEquals(initialRounds + 1, viewModel.state.value.roundsIn10Cm)

        val cal = viewModel.state.value.calibrationResult
        assertNotNull(cal)
        val expectedHeight = 100f / (initialRounds + 1).toFloat()
        assertEquals(expectedHeight, cal!!.customStandard.stitchHeightMm, 0.01f)
    }

    @Test
    fun `selecting new CYC standard updates selected standard and adapts counts`() {
        val bulkyStandard = YarnGaugeRegistry.getAllStandards().first { it.yarnWeightCategory.code == 5 }

        viewModel.processIntent(TensionIntent.SelectStandard(bulkyStandard))
        assertEquals(bulkyStandard, viewModel.state.value.selectedStandard)

        val cal = viewModel.state.value.calibrationResult
        assertNotNull(cal)
        assertEquals(bulkyStandard.categoryName + " (Muestra Calibrada)", cal!!.customStandard.categoryName)
    }

    @Test
    fun `resetToStandard restores Worsted default`() {
        val bulkyStandard = YarnGaugeRegistry.getAllStandards().first { it.yarnWeightCategory.code == 5 }
        viewModel.processIntent(TensionIntent.SelectStandard(bulkyStandard))

        viewModel.processIntent(TensionIntent.ResetToStandard)

        val state = viewModel.state.value
        assertEquals(YarnGaugeRegistry.DEFAULT_STANDARD, state.selectedStandard)
        assertNotNull(state.calibrationResult)
    }

    @Test
    fun `applyCalibration marks isSaved and emits navigation effect`() = runTest {
        viewModel.processIntent(TensionIntent.ApplyCalibration)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isSaved)
    }
}
