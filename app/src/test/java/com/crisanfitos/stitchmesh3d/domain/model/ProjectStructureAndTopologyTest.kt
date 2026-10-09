package com.crisanfitos.stitchmesh3d.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Pruebas unitarias para los enums de dominio ProjectStructureType y PartTopologyType (SM-065).
 */
class ProjectStructureAndTopologyTest {

    @Test
    fun `ProjectStructureType defines 4 structure models with proper default topologies`() {
        val amigurumi = ProjectStructureType.AMIGURUMI_3D
        assertEquals("amigurumi_3d", amigurumi.code)
        assertEquals("Amigurumi (3D)", amigurumi.displayName)
        assertEquals(PartTopologyType.CLOSED_FILLED, amigurumi.defaultTopology)

        val garment = ProjectStructureType.FLAT_GARMENT
        assertEquals("flat_garment", garment.code)
        assertEquals("Prenda Plana", garment.displayName)
        assertEquals(PartTopologyType.FLAT_PANEL, garment.defaultTopology)

        val granny = ProjectStructureType.GRANNY_SQUARE
        assertEquals("granny_square", granny.code)
        assertEquals("Granny Square", granny.displayName)
        assertEquals(PartTopologyType.FLAT_PANEL, granny.defaultTopology)

        val accessory = ProjectStructureType.ACCESSORY
        assertEquals("accessory", accessory.code)
        assertEquals("Accesorio", accessory.displayName)
        assertEquals(PartTopologyType.SEMI_CLOSED_TUBE, accessory.defaultTopology)
    }

    @Test
    fun `ProjectStructureType fromCode handles valid and fallback values`() {
        assertEquals(ProjectStructureType.AMIGURUMI_3D, ProjectStructureType.fromCode("amigurumi_3d"))
        assertEquals(ProjectStructureType.FLAT_GARMENT, ProjectStructureType.fromCode("flat_garment"))
        assertEquals(ProjectStructureType.GRANNY_SQUARE, ProjectStructureType.fromCode("granny_square"))
        assertEquals(ProjectStructureType.ACCESSORY, ProjectStructureType.fromCode("accessory"))

        // Case insensitivity & fallback
        assertEquals(ProjectStructureType.ACCESSORY, ProjectStructureType.fromCode("ACCESSORY"))
        assertEquals(ProjectStructureType.AMIGURUMI_3D, ProjectStructureType.fromCode("unknown_type"))
    }

    @Test
    fun `PartTopologyType defines 3 topologies with descriptive metadata`() {
        val closed = PartTopologyType.CLOSED_FILLED
        assertEquals("closed_filled", closed.code)
        assertEquals("Cerrada / Rellena", closed.displayName)
        assertNotNull(closed.description)

        val tube = PartTopologyType.SEMI_CLOSED_TUBE
        assertEquals("semi_closed_tube", tube.code)
        assertEquals("Tubular / Abierta", tube.displayName)
        assertNotNull(tube.description)

        val flat = PartTopologyType.FLAT_PANEL
        assertEquals("flat_panel", flat.code)
        assertEquals("Panel Plano", flat.displayName)
        assertNotNull(flat.description)
    }

    @Test
    fun `PartTopologyType fromCode handles valid and fallback values`() {
        assertEquals(PartTopologyType.CLOSED_FILLED, PartTopologyType.fromCode("closed_filled"))
        assertEquals(PartTopologyType.SEMI_CLOSED_TUBE, PartTopologyType.fromCode("semi_closed_tube"))
        assertEquals(PartTopologyType.FLAT_PANEL, PartTopologyType.fromCode("flat_panel"))

        // Fallback
        assertEquals(PartTopologyType.CLOSED_FILLED, PartTopologyType.fromCode("invalid_code"))
    }
}
