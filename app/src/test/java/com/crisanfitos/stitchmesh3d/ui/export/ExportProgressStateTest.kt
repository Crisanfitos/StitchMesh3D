package com.crisanfitos.stitchmesh3d.ui.export

import com.crisanfitos.stitchmesh3d.ui.export.components.ExportProgressState
import com.crisanfitos.stitchmesh3d.ui.export.components.TelemetryLogLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportProgressStateTest {

    @Test
    fun `appendLog updates progress percent and formats telemetry lines`() {
        var state = ExportProgressState()

        state = state.appendLog("Validador formal verificado", TelemetryLogLevel.SUCCESS, 0.35f)
        state = state.appendLog("Generando geometría OBJ...", TelemetryLogLevel.ACTIVE, 0.68f)

        assertEquals(68, state.progressPercent)
        assertEquals(2, state.logs.size)
        assertEquals("[✓] Validador formal verificado", state.logs[0].formattedLine)
        assertEquals("[⚡] Generando geometría OBJ...", state.logs[1].formattedLine)
    }

    @Test
    fun `progressFraction is clamped between 0 and 100 percent`() {
        val over = ExportProgressState(progressFraction = 1.45f)
        val under = ExportProgressState(progressFraction = -0.25f)

        assertEquals(100, over.progressPercent)
        assertEquals(0, under.progressPercent)
    }
}
