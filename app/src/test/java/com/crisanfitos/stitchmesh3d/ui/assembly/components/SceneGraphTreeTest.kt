package com.crisanfitos.stitchmesh3d.ui.assembly.components

import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import com.crisanfitos.stitchmesh3d.domain.model.PatternRound
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import com.crisanfitos.stitchmesh3d.ui.workspace.ProjectPartUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para los modelos, mappers y lógica estructural del SceneGraphTree (SM-053).
 */
class SceneGraphTreeTest {

    @Test
    fun `SceneGraphItem initializes with correct attributes and default values`() {
        val item = SceneGraphItem(
            id = "part_head",
            name = "Cabeza",
            roundCount = 18,
            stitchCount = 108,
            colorHex = "#E06D53",
            topologyType = PartTopologyType.CLOSED_FILLED
        )

        assertEquals("part_head", item.id)
        assertEquals("Cabeza", item.name)
        assertNull(item.parentId)
        assertEquals(0, item.depth)
        assertTrue(item.isVisible)
        assertFalse(item.isLocked)
        assertEquals("#E06D53", item.colorHex)
        assertEquals(18, item.roundCount)
        assertEquals(108, item.stitchCount)
        assertEquals(PartTopologyType.CLOSED_FILLED, item.topologyType)
        assertFalse(item.hasChildren)
        assertTrue(item.isExpanded)
    }

    @Test
    fun `SceneGraphItem copy toggles visibility, lock and color correctly`() {
        val initial = SceneGraphItem(
            id = "part_arm",
            name = "Brazo Izquierdo",
            isVisible = true,
            isLocked = false,
            colorHex = "#E06D53"
        )

        val hidden = initial.copy(isVisible = false)
        assertFalse(hidden.isVisible)

        val locked = initial.copy(isLocked = true)
        assertTrue(locked.isLocked)

        val recolored = initial.copy(colorHex = "#52A474")
        assertEquals("#52A474", recolored.colorHex)
    }

    @Test
    fun `hierarchical items reflect depth and parent-child association`() {
        val parent = SceneGraphItem(
            id = "body",
            name = "Cuerpo",
            depth = 0,
            hasChildren = true,
            isExpanded = true
        )

        val child1 = SceneGraphItem(
            id = "left_arm",
            name = "Brazo Izq",
            parentId = "body",
            depth = 1
        )

        val child2 = SceneGraphItem(
            id = "right_arm",
            name = "Brazo Der",
            parentId = "body",
            depth = 1
        )

        val tree = listOf(parent, child1, child2)

        assertEquals(3, tree.size)
        assertEquals(1, tree.count { it.depth == 0 })
        assertEquals(2, tree.count { it.parentId == "body" })
        assertTrue(tree.first { it.id == "body" }.hasChildren)
    }

    @Test
    fun `toSceneGraphItem mapper from ProjectPart correctly maps properties`() {
        val domainPart = ProjectPart(
            id = "part_ear",
            projectId = "proj_fox",
            name = "Oreja Izquierda",
            topologyType = PartTopologyType.FLAT_PANEL,
            rounds = listOf(
                PatternRound(id = "r1", partId = "part_ear", roundNumber = 1, rawInstruction = "AM 4", producedStitches = 4),
                PatternRound(id = "r2", partId = "part_ear", roundNumber = 2, rawInstruction = "4 aum", producedStitches = 8)
            )
        )

        val item = domainPart.toSceneGraphItem(
            depth = 1,
            colorHex = "#F2C94C",
            isLocked = true
        )

        assertEquals("part_ear", item.id)
        assertEquals("Oreja Izquierda", item.name)
        assertEquals(1, item.depth)
        assertEquals("#F2C94C", item.colorHex)
        assertTrue(item.isLocked)
        assertTrue(item.isVisible)
        assertEquals(2, item.roundCount)
        assertEquals(12, item.stitchCount)
        assertEquals(PartTopologyType.FLAT_PANEL, item.topologyType)
    }

    @Test
    fun `toSceneGraphItem mapper from ProjectPartUiModel correctly maps properties`() {
        val uiModel = ProjectPartUiModel(
            id = "part_tail",
            name = "Cola",
            roundCount = 15,
            colorHex = "#E54D42",
            topologyType = PartTopologyType.SEMI_CLOSED_TUBE
        )

        val item = uiModel.toSceneGraphItem(
            depth = 0,
            isVisible = false
        )

        assertEquals("part_tail", item.id)
        assertEquals("Cola", item.name)
        assertEquals(0, item.depth)
        assertEquals("#E54D42", item.colorHex)
        assertFalse(item.isVisible)
        assertFalse(item.isLocked)
        assertEquals(15, item.roundCount)
        assertEquals(PartTopologyType.SEMI_CLOSED_TUBE, item.topologyType)
    }

    @Test
    fun `visible and locked metrics accurately count items in list`() {
        val items = listOf(
            SceneGraphItem("p1", "Cabeza", isVisible = true, isLocked = false),
            SceneGraphItem("p2", "Cuerpo", isVisible = true, isLocked = true),
            SceneGraphItem("p3", "Oreja", isVisible = false, isLocked = true),
            SceneGraphItem("p4", "Pata", isVisible = false, isLocked = false)
        )

        val visibleCount = items.count { it.isVisible }
        val lockedCount = items.count { it.isLocked }

        assertEquals(2, visibleCount)
        assertEquals(2, lockedCount)
        assertEquals(4, items.size)
    }
}
