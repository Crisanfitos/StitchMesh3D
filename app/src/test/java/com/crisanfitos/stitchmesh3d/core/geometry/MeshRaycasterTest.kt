package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.ui.assembly.AssemblyIntent
import com.crisanfitos.stitchmesh3d.ui.assembly.AssemblyViewModel
import com.crisanfitos.stitchmesh3d.ui.assembly.components.PartTransformState
import com.crisanfitos.stitchmesh3d.ui.assembly.components.SceneGraphItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.system.measureNanoTime

/**
 * Suite de pruebas unitarias para el motor de raycasting y anclaje superficial (SM-070, RF-4.1, RF-4.2).
 */
class MeshRaycasterTest {

    @Test
    fun `ray hits known triangle with exact intersection point and normal`() {
        // Triángulo en el plano Z = 0: (0, 0, 0), (10, 0, 0), (0, 10, 0)
        val positions = floatArrayOf(
            0f, 0f, 0f,
            10f, 0f, 0f,
            0f, 10f, 0f
        )
        val normals = floatArrayOf(
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f
        )
        val indices = shortArrayOf(0, 1, 2)
        val mesh = MeshGeometry(
            vertexPositions = positions,
            indices = indices,
            vertexCount = 3,
            triangleCount = 1,
            vertexNormals = normals
        )

        // Rayo desde (2, 2, 15) hacia abajo en eje Z: (0, 0, -1)
        val ray = Ray3D.create(2f, 2f, 15f, 0f, 0f, -1f)
        val hit = MeshRaycaster.castRay(mesh, ray)

        assertNotNull("El rayo debió impactar el triángulo", hit)
        hit!!
        assertEquals("Distancia t debe ser 15 mm", 15f, hit.distance, 1e-4f)
        assertEquals("Coordenada X del impacto", 2f, hit.point[0], 1e-4f)
        assertEquals("Coordenada Y del impacto", 2f, hit.point[1], 1e-4f)
        assertEquals("Coordenada Z del impacto (en plano)", 0f, hit.point[2], 1e-4f)

        // Vector normal
        assertEquals("Normal Z debe ser 1.0", 1f, hit.normal[2], 1e-4f)
        assertEquals("Normal X debe ser 0.0", 0f, hit.normal[0], 1e-4f)
        assertEquals("Normal Y debe ser 0.0", 0f, hit.normal[1], 1e-4f)
        assertEquals(0, hit.triangleIndex)
    }

    @Test
    fun `ray misses triangle when origin or direction is outside boundaries`() {
        val positions = floatArrayOf(
            0f, 0f, 0f,
            10f, 0f, 0f,
            0f, 10f, 0f
        )
        val indices = shortArrayOf(0, 1, 2)
        val mesh = MeshGeometry(
            vertexPositions = positions,
            indices = indices,
            vertexCount = 3,
            triangleCount = 1
        )

        // Rayo fuera del triángulo (X=15, Y=15)
        val rayOutside = Ray3D.create(15f, 15f, 10f, 0f, 0f, -1f)
        val hit = MeshRaycaster.castRay(mesh, rayOutside)
        assertNull("Rayo fuera del triángulo debe devolver null", hit)

        // Rayo apuntando en sentido contrario (hacia +Z)
        val rayAway = Ray3D.create(2f, 2f, 15f, 0f, 0f, 1f)
        val hitAway = MeshRaycaster.castRay(mesh, rayAway)
        assertNull("Rayo que se aleja del plano debe devolver null", hitAway)
    }

    @Test
    fun `ray parallel to triangle plane is rejected by determinant check`() {
        val positions = floatArrayOf(
            0f, 0f, 0f,
            10f, 0f, 0f,
            0f, 10f, 0f
        )
        val indices = shortArrayOf(0, 1, 2)
        val mesh = MeshGeometry(
            vertexPositions = positions,
            indices = indices,
            vertexCount = 3,
            triangleCount = 1
        )

        // Rayo rasante paralelo al plano XY
        val rayParallel = Ray3D.create(2f, 2f, 0f, 1f, 0f, 0f)
        val hit = MeshRaycaster.castRay(mesh, rayParallel)
        assertNull("Rayo paralelo no debe intersecar", hit)
    }

    @Test
    fun `raycast against parametric amigurumi sphere returns closest surface point and outward normal`() {
        val radius = 25f
        val sphere = MeshRaycaster.createSphereMesh(
            radiusMm = radius,
            rings = 12,
            sectors = 20,
            centerX = 0f,
            centerY = 0f,
            centerZ = 0f
        )

        assertTrue(sphere.triangleCount > 0)

        // Rayo frontal desde (0, 0, 50) hacia el origen (0, 0, -1)
        val rayFront = Ray3D.create(0f, 0f, 50f, 0f, 0f, -1f)
        val hit = MeshRaycaster.castRay(sphere, rayFront)

        assertNotNull("Rayo debió impactar la esfera", hit)
        hit!!

        // Distancia esperada: 50 - 25 = 25 mm (tolerancia por teselación discreta)
        assertEquals(25f, hit.distance, 1.5f)
        assertEquals(0f, hit.point[0], 1.5f)
        assertEquals(0f, hit.point[1], 1.5f)
        assertEquals(25f, hit.point[2], 1.5f)

        // Normal en el polo frontal (+Z) debe apuntar aproximadamente hacia (0, 0, 1)
        assertTrue("Normal Z debe ser dominante positiva", hit.normal[2] > 0.9f)
    }

    @Test
    fun `calculateSeamAlignmentEulerAngles aligns secondary part base along surface normal`() {
        // 1. Normal hacia arriba (+Y, ej: corona de la cabeza)
        val normalTop = floatArrayOf(0f, 1f, 0f)
        val eulerTop = MeshRaycaster.calculateSeamAlignmentEulerAngles(normalTop)
        assertEquals("RotX en posición cenital debe ser 0", 0f, eulerTop[0], 1e-3f)
        assertEquals("RotY en posición cenital debe ser 0", 0f, eulerTop[1], 1e-3f)
        assertEquals("RotZ en posición cenital debe ser 0", 0f, eulerTop[2], 1e-3f)

        // 2. Normal frontal (+Z, ej: costura de hocico/nariz)
        val normalFront = floatArrayOf(0f, 0f, 1f)
        val eulerFront = MeshRaycaster.calculateSeamAlignmentEulerAngles(normalFront)
        // atan2(1, 0) = 90 grados en X
        assertEquals("RotX debe ser 90 grados", 90f, eulerFront[0], 1e-3f)

        // 3. Normal lateral (+X, ej: costura de oreja derecha)
        val normalRight = floatArrayOf(1f, 0f, 0f)
        val eulerRight = MeshRaycaster.calculateSeamAlignmentEulerAngles(normalRight)
        assertTrue("RotZ o RotY debe tener rotación significativa", abs(eulerRight[1]) > 45f || abs(eulerRight[2]) > 45f)
    }

    @Test
    fun `screenPointToWorldRay produces correct direction vector`() {
        val rayCenter = MeshRaycaster.screenPointToWorldRay(
            screenX = 400f,
            screenY = 300f,
            centerX = 400f,
            centerY = 300f,
            zoomScale = 1.0f,
            orbitYawDeg = 0f,
            orbitPitchDeg = 0f
        )

        // En órbita 0, 0, el rayo de cámara apunta a lo largo del eje -Z
        assertEquals("Dir X debe ser 0", 0f, rayCenter.direction[0], 1e-4f)
        assertEquals("Dir Y debe ser 0", 0f, rayCenter.direction[1], 1e-4f)
        assertEquals("Dir Z debe ser -1", -1f, rayCenter.direction[2], 1e-4f)
    }

    @Test
    fun `raycast benchmark executes in less than 5ms for dense amigurumi mesh`() {
        // Esfera densa con más de 2000 triángulos
        val denseSphere = MeshRaycaster.createSphereMesh(
            radiusMm = 30f,
            rings = 30,
            sectors = 40
        )
        assertTrue("Malla debe ser densa", denseSphere.triangleCount >= 2000)

        val ray = Ray3D.create(5f, 5f, 60f, 0f, 0f, -1f)

        // Calentamiento JIT
        repeat(10) {
            MeshRaycaster.castRay(denseSphere, ray)
        }

        // Medición de 20 consultas consecutivas de raycast
        val nanos = measureNanoTime {
            repeat(20) {
                val hit = MeshRaycaster.castRay(denseSphere, ray)
                assertNotNull(hit)
            }
        }

        val avgMs = (nanos / 20) / 1_000_000.0
        println("MeshRaycaster benchmark: tiempo promedio por consulta = %.3f ms".format(avgMs))
        assertTrue("Tiempo de raycast ($avgMs ms) debe ser < 5.0 ms", avgMs < 5.0)
    }

    @Test
    fun `AssemblyViewModel SnapPartToSurface updates position and rotation correctly`() {
        val viewModel = AssemblyViewModel()
        val partId = "part-arm-left"

        // Anclamos el brazo en la superficie en (15, 20, 5) con normal frontal (0, 0, 1)
        val snapIntent = AssemblyIntent.SnapPartToSurface(
            sourcePartId = partId,
            pointX = 15f,
            pointY = 20f,
            pointZ = 5f,
            normalX = 0f,
            normalY = 0f,
            normalZ = 1f
        )

        viewModel.processIntent(snapIntent)

        val updatedTransform = viewModel.state.value.transformsByPartId[partId]
        assertNotNull(updatedTransform)
        assertEquals(15f, updatedTransform!!.posX, 1e-4f)
        assertEquals(20f, updatedTransform.posY, 1e-4f)
        assertEquals(5f, updatedTransform.posZ, 1e-4f)
        assertEquals(90f, updatedTransform.rotX, 1e-3f)

        // Verificamos sincronización hacia el modelo de dominio
        val synced = viewModel.syncToProjectParts(
            listOf(
                com.crisanfitos.stitchmesh3d.domain.model.ProjectPart(
                    id = partId,
                    projectId = "p-1",
                    name = "Brazo Izquierdo",
                    sortOrder = 0
                )
            )
        )
        assertEquals(15f, synced[0].transformPositionX, 1e-4f)
        assertEquals(20f, synced[0].transformPositionY, 1e-4f)
        assertEquals(5f, synced[0].transformPositionZ, 1e-4f)
        assertEquals(90f, synced[0].transformRotationX, 1e-3f)
    }
}
