package com.crisanfitos.stitchmesh3d.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertEquals
import org.junit.Test

class StitchMeshThemeTest {

    @Test
    fun `theme colors match Design Brief section 1 specifications exactly`() {
        assertEquals(Color(0xFF121316), StitchMeshNeutralDark)
        assertEquals(Color(0xFF1E1F24), StitchMeshSurfaceContainer)
        assertEquals(Color(0xFFE06D53), StitchMeshTerracotta)
        assertEquals(Color(0xFF52A474), StitchMeshSageGreen)
        assertEquals(Color(0xFFE54D42), StitchMeshCoralRed)
        assertEquals(Color(0xFFF2C94C), StitchMeshYarnGold)
    }

    @Test
    fun `crochet typography styles use monospace font family for column alignment`() {
        assertEquals(FontFamily.Monospace, CrochetTypography.formulaInput.fontFamily)
        assertEquals(FontFamily.Monospace, CrochetTypography.tokenBadge.fontFamily)
        assertEquals(FontFamily.Monospace, CrochetTypography.matrixValue.fontFamily)
    }
}
