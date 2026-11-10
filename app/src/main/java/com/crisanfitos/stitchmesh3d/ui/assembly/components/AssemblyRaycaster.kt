package com.crisanfitos.stitchmesh3d.ui.assembly.components

import androidx.compose.ui.geometry.Offset
import com.crisanfitos.stitchmesh3d.core.geometry.MeshGeometry
import com.crisanfitos.stitchmesh3d.core.geometry.MeshRaycaster
import com.crisanfitos.stitchmesh3d.core.geometry.RaycastHit

/**
 * Resultado de una selección o consulta de raycast contra el ensamblado multipieza 3D (SM-087).
 */
data class AssemblyPickResult(
    val partId: String,
    val hit: RaycastHit
)

/**
 * Módulo de trazado de rayos para selección de piezas y anclaje superficial en el ensamblador (SM-087).
 */
object AssemblyRaycaster {

    /**
     * Evalúa qué pieza del ensamblado intersecta un toque de pantalla mediante raycasting Möller-Trumbore.
     *
     * @return [AssemblyPickResult] con la pieza impactada más cercana a la cámara, o null si el rayo no impacta ninguna pieza.
     */
    fun pickPart(
        screenX: Float,
        screenY: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        orbitYawDeg: Float,
        orbitPitchDeg: Float,
        zoomScale: Float,
        panOffset: Offset,
        parts: List<SceneGraphItem>,
        transformsByPartId: Map<String, PartTransformState>,
        meshesByPartId: Map<String, MeshGeometry>,
        isIsolatedMode: Boolean = false,
        selectedPartId: String = "",
        excludedPartId: String? = null
    ): AssemblyPickResult? {
        val centerX = viewportWidth / 2f + panOffset.x
        val centerY = viewportHeight / 2f + panOffset.y

        val ray = MeshRaycaster.screenPointToWorldRay(
            screenX = screenX,
            screenY = screenY,
            centerX = centerX,
            centerY = centerY,
            zoomScale = zoomScale,
            orbitYawDeg = orbitYawDeg,
            orbitPitchDeg = orbitPitchDeg
        )

        val candidateParts = if (isIsolatedMode && selectedPartId.isNotBlank()) {
            parts.filter { it.id == selectedPartId }
        } else {
            parts.filter { it.isVisible && (excludedPartId == null || it.id != excludedPartId) }
        }

        var closestDistance = Float.MAX_VALUE
        var closestResult: AssemblyPickResult? = null

        for (part in candidateParts) {
            val sourceMesh = meshesByPartId[part.id] ?: AssemblyMeshCombiner.createDefaultPartMesh(part)
            val transform = transformsByPartId[part.id]
            val transformedMesh = AssemblyMeshCombiner.transformPartMesh(part, sourceMesh, transform)

            val hit = MeshRaycaster.castRay(transformedMesh, ray) ?: continue
            if (hit.distance in 1e-4f..closestDistance) {
                closestDistance = hit.distance
                closestResult = AssemblyPickResult(part.id, hit)
            }
        }

        return closestResult
    }
}
