package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import com.crisanfitos.stitchmesh3d.domain.model.toTransformMatrix
import com.crisanfitos.stitchmesh3d.ui.assembly.AssemblyIntent
import com.crisanfitos.stitchmesh3d.ui.assembly.AssemblyViewModel
import com.crisanfitos.stitchmesh3d.ui.assembly.components.PartTransformState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Suite de pruebas unitarias para transformaciones matemáticas del Scene Graph multipieza (SM-033, RF-4.2).
 */
class SceneGraphTransformTest {

    private val testGauge = YarnGaugeStandard(
        id = 1,
        yarnWeightCategory = YarnWeightCategory.MEDIUM,
        categoryName = "Worsted",
        hookSizeMm = 4.0f,
        stitchWidthMm = 4.0f,
        stitchHeightMm = 4.5f,
        stitchThicknessMm = 2.0f
    )

    @Test
    fun `identity matrix leaves points and normals unaltered`() {
        val identity = TransformMatrix4x4.identity()

        val p = identity.transformPoint(10f, -20f, 30f)
        assertEquals(10f, p[0], 1e-4f)
        assertEquals(-20f, p[1], 1e-4f)
        assertEquals(30f, p[2], 1e-4f)

        val n = identity.transformNormal(0f, 1f, 0f)
        assertEquals(0f, n[0], 1e-4f)
        assertEquals(1f, n[1], 1e-4f)
        assertEquals(0f, n[2], 1e-4f)
    }

    @Test
    fun `translation matrix shifts point coordinates and leaves normals intact`() {
        val t = TransformMatrix4x4.translation(15f, -25f, 50f)

        val p = t.transformPoint(5f, 10f, 0f)
        assertEquals(20f, p[0], 1e-4f)
        assertEquals(-15f, p[1], 1e-4f)
        assertEquals(50f, p[2], 1e-4f)

        // Las normales de superficie no se ven afectadas por traslación pura
        val n = t.transformNormal(0f, 0f, 1f)
        assertEquals(0f, n[0], 1e-4f)
        assertEquals(0f, n[1], 1e-4f)
        assertEquals(1f, n[2], 1e-4f)
    }

    @Test
    fun `rotation matrices rotate points and normals correctly`() {
        // Rotación de 90° en Z: (1, 0, 0) -> (0, 1, 0)
        val rotZ90 = TransformMatrix4x4.rotationZ(90f)
        val pZ = rotZ90.transformPoint(1f, 0f, 0f)
        assertEquals(0f, pZ[0], 1e-4f)
        assertEquals(1f, pZ[1], 1e-4f)
        assertEquals(0f, pZ[2], 1e-4f)

        // Rotación de 90° en X: (0, 1, 0) -> (0, 0, 1)
        val rotX90 = TransformMatrix4x4.rotationX(90f)
        val pX = rotX90.transformPoint(0f, 1f, 0f)
        assertEquals(0f, pX[0], 1e-4f)
        assertEquals(0f, pX[1], 1e-4f)
        assertEquals(1f, pX[2], 1e-4f)

        // Normal rotada debe mantener longitud unitaria = 1.0
        val nRot = rotX90.transformNormal(0f, 1f, 0f)
        assertEquals(0f, nRot[0], 1e-4f)
        assertEquals(0f, nRot[1], 1e-4f)
        assertEquals(1f, nRot[2], 1e-4f)
    }

    @Test
    fun `scale matrix transforms point and renormalizes normal vector`() {
        // Escala anisotrópica: X x 2, Y x 3, Z x 1
        val s = TransformMatrix4x4.scale(2f, 3f, 1f)
        val p = s.transformPoint(5f, 2f, 4f)
        assertEquals(10f, p[0], 1e-4f)
        assertEquals(6f, p[1], 1e-4f)
        assertEquals(4f, p[2], 1e-4f)

        // Normal ante escala no uniforme debe permanecer normalizada
        val n = s.transformNormal(1f, 1f, 0f)
        val mag = kotlin.math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2])
        assertEquals(1.0f, mag, 1e-4f)
    }

    @Test
    fun `matrix inverse roundtrip restores original point`() {
        val m = TransformMatrix4x4.fromTransform(
            posX = 22f,
            posY = -14f,
            posZ = 35f,
            rotXDeg = 30f,
            rotYDeg = 45f,
            rotZDeg = 60f,
            scaleX = 1.5f,
            scaleY = 1.5f,
            scaleZ = 1.5f
        )
        val inv = m.invert()
        assertNotNull("La matriz afín debe ser invertible", inv)

        val origX = 7.5f
        val origY = -12.3f
        val origZ = 18.0f

        val transformed = m.transformPoint(origX, origY, origZ)
        val restored = inv!!.transformPoint(transformed[0], transformed[1], transformed[2])

        assertEquals(origX, restored[0], 1e-3f)
        assertEquals(origY, restored[1], 1e-3f)
        assertEquals(origZ, restored[2], 1e-3f)
    }

    @Test
    fun `sagittal mirroring inverts X axis while keeping Y and Z`() {
        val mirrored = TransformMatrix4x4.fromTransform(
            posX = 20f,
            posY = 10f,
            posZ = 5f,
            isMirrored = true
        )

        val p = mirrored.transformPoint(5f, 10f, 0f)
        // Con sx = -1 y traslación posX = 20, X = 20 - 5 = 15
        assertEquals(15f, p[0], 1e-4f)
        assertEquals(20f, p[1], 1e-4f)
        assertEquals(5f, p[2], 1e-4f)
    }

    @Test
    fun `transformMesh updates positions and normal unit lengths for whole MeshGeometry`() {
        val ring1 = RingProfileGenerator.generateRing(
            roundIndex = 1,
            stitches = List(6) { StitchType.SingleCrochet },
            gaugeStandard = testGauge
        )
        val ring2 = RingProfileGenerator.generateRing(
            roundIndex = 2,
            stitches = List(12) { StitchType.SingleCrochet },
            gaugeStandard = testGauge,
            previousRing = ring1
        )
        val mesh = AdaptiveTessellator.tessellate(listOf(ring1, ring2), includePolarCap = true)

        val transform = TransformMatrix4x4.fromTransform(
            posX = 50f,
            posY = 20f,
            posZ = -10f,
            rotYDeg = 45f
        )

        val transformedMesh = transform.transformMesh(mesh)

        assertEquals(mesh.vertexCount, transformedMesh.vertexCount)
        assertEquals(mesh.triangleCount, transformedMesh.triangleCount)

        // Todos los vértices deben haberse desplazado
        for (i in 0 until transformedMesh.vertexCount) {
            val orig = mesh.getVertexPosition(i)
            val trans = transformedMesh.getVertexPosition(i)
            assertTrue("La coordenada transformada debe diferir de la original", abs(trans[0] - orig[0]) > 1e-3f || abs(trans[1] - orig[1]) > 1e-3f)

            // Magnitud de la normal debe ser unitaria
            assertEquals(1.0f, transformedMesh.getNormalMagnitude(i), 1e-3f)
        }
    }

    @Test
    fun `SceneGraphNode computes hierarchical world transforms compounding parent and child offsets`() {
        // Torso en (0, 0, 0)
        // Cabeza hija en (0, 50, 0) respecto al torso
        // Oreja nieta en (20, 15, 0) respecto a la cabeza
        val earNode = SceneGraphNode(
            partId = "ear-left",
            name = "Oreja Izquierda",
            localMatrix = TransformMatrix4x4.translation(20f, 15f, 0f)
        )
        val headNode = SceneGraphNode(
            partId = "head",
            name = "Cabeza",
            localMatrix = TransformMatrix4x4.translation(0f, 50f, 0f),
            children = listOf(earNode)
        )
        val torsoNode = SceneGraphNode(
            partId = "torso",
            name = "Torso",
            localMatrix = TransformMatrix4x4.translation(0f, 0f, 0f),
            children = listOf(headNode)
        )

        val worldTransforms = torsoNode.computeWorldTransforms()

        // Torso en (0, 0, 0)
        val torsoWorldP = worldTransforms["torso"]!!.transformPoint(0f, 0f, 0f)
        assertEquals(0f, torsoWorldP[0], 1e-4f)
        assertEquals(0f, torsoWorldP[1], 1e-4f)
        assertEquals(0f, torsoWorldP[2], 1e-4f)

        // Cabeza en (0, 50, 0)
        val headWorldP = worldTransforms["head"]!!.transformPoint(0f, 0f, 0f)
        assertEquals(0f, headWorldP[0], 1e-4f)
        assertEquals(50f, headWorldP[1], 1e-4f)
        assertEquals(0f, headWorldP[2], 1e-4f)

        // Oreja en (20, 50 + 15 = 65, 0)
        val earWorldP = worldTransforms["ear-left"]!!.transformPoint(0f, 0f, 0f)
        assertEquals(20f, earWorldP[0], 1e-4f)
        assertEquals(65f, earWorldP[1], 1e-4f)
        assertEquals(0f, earWorldP[2], 1e-4f)
    }

    @Test
    fun `ProjectPart toTransformMatrix extension creates matching affine transform`() {
        val part = ProjectPart(
            id = "part-1",
            projectId = "proj-1",
            name = "Brazo",
            transformPositionX = 30f,
            transformPositionY = -10f,
            transformPositionZ = 5f,
            transformRotationX = 0f,
            transformRotationY = 90f,
            transformRotationZ = 0f
        )

        val matrix = part.toTransformMatrix()
        val originTransformed = matrix.transformPoint(0f, 0f, 0f)

        assertEquals(30f, originTransformed[0], 1e-4f)
        assertEquals(-10f, originTransformed[1], 1e-4f)
        assertEquals(5f, originTransformed[2], 1e-4f)
    }

    @Test
    fun `AssemblyViewModel handles ToggleIsolatedMode and syncToProjectParts`() {
        val viewModel = AssemblyViewModel()
        assertFalse("El modo aislado debe iniciar desactivado", viewModel.state.value.isIsolatedMode)

        viewModel.processIntent(AssemblyIntent.ToggleIsolatedMode)
        assertTrue("ToggleIsolatedMode debe activar el modo aislado", viewModel.state.value.isIsolatedMode)

        viewModel.processIntent(AssemblyIntent.ToggleIsolatedMode)
        assertFalse("ToggleIsolatedMode debe desactivar el modo aislado", viewModel.state.value.isIsolatedMode)

        // Mover una pieza en el ViewModel
        val targetPartId = "part-torso"
        viewModel.processIntent(AssemblyIntent.UpdatePosition(targetPartId, 12f, 34f, 56f))
        viewModel.processIntent(AssemblyIntent.UpdateRotation(targetPartId, 10f, 20f, 30f))

        val domainParts = listOf(
            ProjectPart(id = targetPartId, projectId = "proj-1", name = "Torso")
        )
        val synced = viewModel.syncToProjectParts(domainParts)

        assertEquals(12f, synced.first().transformPositionX)
        assertEquals(34f, synced.first().transformPositionY)
        assertEquals(56f, synced.first().transformPositionZ)
        assertEquals(10f, synced.first().transformRotationX)
        assertEquals(20f, synced.first().transformRotationY)
        assertEquals(30f, synced.first().transformRotationZ)
    }
}
