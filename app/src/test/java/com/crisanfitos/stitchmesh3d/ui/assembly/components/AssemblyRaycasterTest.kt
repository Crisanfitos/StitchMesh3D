package com.crisanfitos.stitchmesh3d.ui.assembly.components

import androidx.compose.ui.geometry.Offset
import com.crisanfitos.stitchmesh3d.core.geometry.MeshRaycaster
import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AssemblyRaycasterTest {

    private fun createTestPart(id: String, name: String): SceneGraphItem {
        return SceneGraphItem(
            id = id,
            name = name,
            isVisible = true,
            isLocked = false,
            roundCount = 10,
            topologyType = PartTopologyType.CLOSED_FILLED
        )
    }

    @Test
    fun pickPart_correctlySelectsIntersectedPart() {
        val p1 = createTestPart("p1", "Torso")
        val p2 = createTestPart("p2", "Head")

        val m1 = MeshRaycaster.createSphereMesh(radiusMm = 15f)
        val m2 = MeshRaycaster.createSphereMesh(radiusMm = 10f)

        val parts = listOf(p1, p2)
        val meshes = mapOf("p1" to m1, "p2" to m2)
        val transforms = mapOf(
            "p1" to PartTransformState("p1", "Torso", posX = 0f, posY = 0f, posZ = 0f),
            "p2" to PartTransformState("p2", "Head", posX = 0f, posY = 40f, posZ = 0f)
        )

        // Viewport: 800x800, camera looking at origin from front (orbitYaw = 0, orbitPitch = 0)
        // Center of viewport (400, 400) casts a ray straight towards (0, 0, 0), where Torso is located!
        val hitCenter = AssemblyRaycaster.pickPart(
            screenX = 400f,
            screenY = 400f,
            viewportWidth = 800f,
            viewportHeight = 800f,
            orbitYawDeg = 0f,
            orbitPitchDeg = 0f,
            zoomScale = 1.0f,
            panOffset = Offset.Zero,
            parts = parts,
            transformsByPartId = transforms,
            meshesByPartId = meshes
        )

        assertNotNull("Ray through center should hit p1 (Torso)", hitCenter)
        assertEquals("p1", hitCenter!!.partId)

        // Pointing far to the side (screenX = 50, screenY = 50) misses all parts
        val hitMiss = AssemblyRaycaster.pickPart(
            screenX = 50f,
            screenY = 50f,
            viewportWidth = 800f,
            viewportHeight = 800f,
            orbitYawDeg = 0f,
            orbitPitchDeg = 0f,
            zoomScale = 1.0f,
            panOffset = Offset.Zero,
            parts = parts,
            transformsByPartId = transforms,
            meshesByPartId = meshes
        )

        assertNull("Ray far from meshes should return null", hitMiss)
    }

    @Test
    fun pickPart_respectsExcludedPartIdForSurfaceSnapping() {
        val p1 = createTestPart("p1", "Base")
        val p2 = createTestPart("p2", "Arm")

        val m1 = MeshRaycaster.createSphereMesh(radiusMm = 20f)
        val parts = listOf(p1, p2)
        val meshes = mapOf("p1" to m1, "p2" to m1)
        val transforms = mapOf(
            "p1" to PartTransformState("p1", "Base", posX = 0f, posY = 0f, posZ = 0f),
            "p2" to PartTransformState("p2", "Arm", posX = 0f, posY = 0f, posZ = 0f)
        )

        // If p1 is excluded (it is the source part being snapped), the ray must ignore p1
        val result = AssemblyRaycaster.pickPart(
            screenX = 400f,
            screenY = 400f,
            viewportWidth = 800f,
            viewportHeight = 800f,
            orbitYawDeg = 0f,
            orbitPitchDeg = 0f,
            zoomScale = 1.0f,
            panOffset = Offset.Zero,
            parts = parts,
            transformsByPartId = transforms,
            meshesByPartId = meshes,
            excludedPartId = "p1"
        )

        assertNotNull(result)
        assertEquals("p2", result!!.partId)
    }
}
