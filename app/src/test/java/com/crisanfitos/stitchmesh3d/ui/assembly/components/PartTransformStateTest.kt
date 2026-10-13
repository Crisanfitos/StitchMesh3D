package com.crisanfitos.stitchmesh3d.ui.assembly.components

import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para PartTransformState, cálculos de espejo y reseteo (SM-054).
 */
class PartTransformStateTest {

    @Test
    fun `PartTransformState initializes with correct default values`() {
        val state = PartTransformState(
            partId = "ear_left",
            partName = "Oreja Izquierda",
            posX = -15.5f,
            posY = 22.0f,
            posZ = 5.0f,
            rotX = 10.0f,
            rotY = -25.0f,
            rotZ = 45.0f
        )

        assertEquals("ear_left", state.partId)
        assertEquals("Oreja Izquierda", state.partName)
        assertEquals(-15.5f, state.posX, 0.001f)
        assertEquals(22.0f, state.posY, 0.001f)
        assertEquals(5.0f, state.posZ, 0.001f)
        assertEquals(10.0f, state.rotX, 0.001f)
        assertEquals(-25.0f, state.rotY, 0.001f)
        assertEquals(45.0f, state.rotZ, 0.001f)
        assertFalse(state.isMirrored)
        assertFalse(state.isLocked)
    }

    @Test
    fun `resetToOrigin sets all position and rotation coordinates to zero and unsets mirror`() {
        val modified = PartTransformState(
            partId = "arm",
            partName = "Brazo",
            posX = 30.0f,
            posY = -10.0f,
            posZ = 12.5f,
            rotX = 90.0f,
            rotY = 45.0f,
            rotZ = -30.0f,
            isMirrored = true
        )

        val reset = modified.resetToOrigin()

        assertEquals(0f, reset.posX, 0.001f)
        assertEquals(0f, reset.posY, 0.001f)
        assertEquals(0f, reset.posZ, 0.001f)
        assertEquals(0f, reset.rotX, 0.001f)
        assertEquals(0f, reset.rotY, 0.001f)
        assertEquals(0f, reset.rotZ, 0.001f)
        assertFalse(reset.isMirrored)
        assertEquals("arm", reset.partId)
        assertEquals("Brazo", reset.partName)
    }

    @Test
    fun `mirrorSagittal reflects X axis and angles, and toggles isMirrored`() {
        val initial = PartTransformState(
            partId = "ear_left",
            partName = "Oreja Izquierda",
            posX = -12.5f,
            posY = 18.0f,
            posZ = 4.0f,
            rotX = 15.0f,
            rotY = -30.0f,
            rotZ = 45.0f,
            isMirrored = false
        )

        val mirrored = initial.mirrorSagittal()

        assertEquals(12.5f, mirrored.posX, 0.001f)
        assertEquals(18.0f, mirrored.posY, 0.001f) // Eje Y preservado
        assertEquals(4.0f, mirrored.posZ, 0.001f)  // Eje Z preservado
        assertEquals(15.0f, mirrored.rotX, 0.001f) // Roll preservado
        assertEquals(30.0f, mirrored.rotY, 0.001f) // Pitch invertido
        assertEquals(-45.0f, mirrored.rotZ, 0.001f) // Yaw invertido
        assertTrue(mirrored.isMirrored)

        // Aplicar espejo de nuevo devuelve las coordenadas originales
        val doubleMirrored = mirrored.mirrorSagittal()
        assertEquals(-12.5f, doubleMirrored.posX, 0.001f)
        assertEquals(-30.0f, doubleMirrored.rotY, 0.001f)
        assertEquals(45.0f, doubleMirrored.rotZ, 0.001f)
        assertFalse(doubleMirrored.isMirrored)
    }

    @Test
    fun `toPartTransformState maps properties from ProjectPart domain model`() {
        val projectPart = ProjectPart(
            id = "part_head",
            projectId = "proj_1",
            name = "Cabeza",
            topologyType = PartTopologyType.CLOSED_FILLED,
            transformPositionX = 0f,
            transformPositionY = 50.0f,
            transformPositionZ = -10.0f,
            transformRotationX = 0f,
            transformRotationY = 180.0f,
            transformRotationZ = 0f
        )

        val state = projectPart.toPartTransformState(isLocked = true)

        assertEquals("part_head", state.partId)
        assertEquals("Cabeza", state.partName)
        assertEquals(0f, state.posX, 0.001f)
        assertEquals(50.0f, state.posY, 0.001f)
        assertEquals(-10.0f, state.posZ, 0.001f)
        assertEquals(180.0f, state.rotY, 0.001f)
        assertTrue(state.isLocked)
        assertFalse(state.isMirrored)
    }
}
