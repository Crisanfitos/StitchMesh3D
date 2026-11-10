package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StitchTbnFrameTest {

    @Test
    fun isOrthonormal_returnsTrueForStandardBasis() {
        val frame = StitchTbnFrame(
            origin = floatArrayOf(10f, 20f, 30f),
            tangent = floatArrayOf(1f, 0f, 0f),
            bitangent = floatArrayOf(0f, 1f, 0f),
            normal = floatArrayOf(0f, 0f, 1f),
            stitchType = StitchType.SingleCrochet,
            colorHex = "#E06D53"
        )
        assertTrue(frame.isOrthonormal())
    }

    @Test
    fun toTransformMatrix_correctlyEmbedsBasisAndOrigin() {
        val origin = floatArrayOf(15f, 25f, 35f)
        val tangent = floatArrayOf(0f, 1f, 0f)
        val bitangent = floatArrayOf(0f, 0f, 1f)
        val normal = floatArrayOf(1f, 0f, 0f)

        val frame = StitchTbnFrame(
            origin = origin,
            tangent = tangent,
            bitangent = bitangent,
            normal = normal,
            stitchType = StitchType.DoubleCrochet,
            widthMm = 2f,
            heightMm = 3f,
            thicknessMm = 4f
        )

        val matrix = frame.toTransformMatrix()
        assertNotNull(matrix)

        // Transforming local origin (0, 0, 0) should yield frame.origin
        val transformedOrigin = matrix.transformPoint(0f, 0f, 0f)
        assertEquals(origin[0], transformedOrigin[0], 0.001f)
        assertEquals(origin[1], transformedOrigin[1], 0.001f)
        assertEquals(origin[2], transformedOrigin[2], 0.001f)

        // Transforming local X unit point (1, 0, 0) should yield origin + tangent * widthMm
        val transformedX = matrix.transformPoint(1f, 0f, 0f)
        assertEquals(origin[0] + tangent[0] * 2f, transformedX[0], 0.001f)
        assertEquals(origin[1] + tangent[1] * 2f, transformedX[1], 0.001f)
        assertEquals(origin[2] + tangent[2] * 2f, transformedX[2], 0.001f)
    }
}
