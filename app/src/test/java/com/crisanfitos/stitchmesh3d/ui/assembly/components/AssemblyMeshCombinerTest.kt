package com.crisanfitos.stitchmesh3d.ui.assembly.components

import com.crisanfitos.stitchmesh3d.core.geometry.MeshGeometry
import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssemblyMeshCombinerTest {

    private fun createTestPart(id: String, name: String, color: String = "#E06D53"): SceneGraphItem {
        return SceneGraphItem(
            id = id,
            name = name,
            isVisible = true,
            isLocked = false,
            colorHex = color,
            roundCount = 8,
            topologyType = PartTopologyType.CLOSED_FILLED
        )
    }

    private fun createSimpleCubeMesh(): MeshGeometry {
        val positions = floatArrayOf(
            -5f, -5f, -5f,
            5f, -5f, -5f,
            5f, 5f, -5f,
            -5f, 5f, -5f
        )
        val normals = floatArrayOf(
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f
        )
        val indices = intArrayOf(0, 1, 2, 0, 2, 3)
        return MeshGeometry(
            vertexPositions = positions,
            indices = indices,
            vertexCount = 4,
            triangleCount = 2,
            vertexNormals = normals
        )
    }

    @Test
    fun createDefaultPartMesh_generatesValidClosedMesh() {
        val part = createTestPart("p1", "Head")
        val mesh = AssemblyMeshCombiner.createDefaultPartMesh(part)

        assertNotNull(mesh)
        assertTrue("Default mesh must have vertices", mesh.vertexCount > 0)
        assertTrue("Default mesh must have triangles", mesh.triangleCount > 0)
        assertEquals(mesh.triangleCount * 3, mesh.indices.size)
    }

    @Test
    fun transformPartMesh_appliesTranslationAndColor() {
        val part = createTestPart("p1", "Torso", color = "#52A474")
        val mesh = createSimpleCubeMesh()
        val transform = PartTransformState(
            partId = "p1",
            partName = "Torso",
            posX = 10f,
            posY = 20f,
            posZ = 30f
        )

        val transformed = AssemblyMeshCombiner.transformPartMesh(part, mesh, transform)
        assertEquals(mesh.vertexCount, transformed.vertexCount)
        assertEquals(mesh.triangleCount, transformed.triangleCount)

        // Verify vertex positions are shifted by (10, 20, 30)
        val p0 = transformed.getVertexPosition(0)
        assertEquals(-5f + 10f, p0[0], 0.01f)
        assertEquals(-5f + 20f, p0[1], 0.01f)
        assertEquals(-5f + 30f, p0[2], 0.01f)

        // Verify vertex color matches #52A474 (R=0.32, G=0.64, B=0.45 approx)
        val col0 = transformed.getVertexColor(0)
        assertTrue("Color R should be > 0.2", col0[0] > 0.2f)
        assertTrue("Color G should be > 0.5", col0[1] > 0.5f)
    }

    @Test
    fun combineParts_mergesAllVisiblePartsIntoSingleMesh() {
        val p1 = createTestPart("p1", "Body")
        val p2 = createTestPart("p2", "Head")
        val p3 = createTestPart("p3", "HiddenPart").copy(isVisible = false)

        val parts = listOf(p1, p2, p3)
        val m1 = createSimpleCubeMesh()
        val m2 = createSimpleCubeMesh()

        val meshesByPartId = mapOf("p1" to m1, "p2" to m2)
        val transformsByPartId = mapOf(
            "p1" to PartTransformState("p1", "Body", 0f, 0f, 0f),
            "p2" to PartTransformState("p2", "Head", 0f, 30f, 0f),
            "p3" to PartTransformState("p3", "HiddenPart", 0f, 60f, 0f)
        )

        val combined = AssemblyMeshCombiner.combineParts(
            parts = parts,
            transformsByPartId = transformsByPartId,
            meshesByPartId = meshesByPartId
        )

        // Only p1 and p2 are visible -> 4 + 4 = 8 vertices, 2 + 2 = 4 triangles
        assertEquals(8, combined.vertexCount)
        assertEquals(4, combined.triangleCount)
        assertEquals(12, combined.indices.size)

        // Verify second mesh indices are offset by 4
        assertEquals(0, combined.indices[0])
        assertEquals(4, combined.indices[6])
    }

    @Test
    fun combineParts_respectsIsolatedMode() {
        val p1 = createTestPart("p1", "Body")
        val p2 = createTestPart("p2", "Head")
        val parts = listOf(p1, p2)

        val m1 = createSimpleCubeMesh()
        val m2 = createSimpleCubeMesh()

        val combined = AssemblyMeshCombiner.combineParts(
            parts = parts,
            transformsByPartId = emptyMap(),
            meshesByPartId = mapOf("p1" to m1, "p2" to m2),
            isIsolatedMode = true,
            selectedPartId = "p2"
        )

        // Only p2 should be in the combined mesh
        assertEquals(4, combined.vertexCount)
        assertEquals(2, combined.triangleCount)
    }
}
