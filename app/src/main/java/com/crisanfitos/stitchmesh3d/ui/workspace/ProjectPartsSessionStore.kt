package com.crisanfitos.stitchmesh3d.ui.workspace

import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import com.crisanfitos.stitchmesh3d.ui.assembly.components.PartTransformState
import com.crisanfitos.stitchmesh3d.ui.assembly.components.SceneGraphItem
import com.crisanfitos.stitchmesh3d.ui.workspace.components.RoundItemUiModel
import java.util.concurrent.ConcurrentHashMap

/**
 * Snapshot inmutable de las piezas de un proyecto compartido entre el Editor de Vueltas (Workbench)
 * y el Ensamblador 3D Multipieza (SM-075).
 */
data class ProjectAssemblySnapshot(
    val projectId: String,
    val projectTitle: String,
    val parts: List<ProjectPartUiModel>,
    val roundsByPartId: Map<String, List<RoundItemUiModel>>,
    val transformsByPartId: Map<String, PartTransformState> = emptyMap(),
    val visibilityByPartId: Map<String, Boolean> = emptyMap(),
    val lockByPartId: Map<String, Boolean> = emptyMap()
) {
    /**
     * Convierte las piezas del Workbench en nodos [SceneGraphItem] para el árbol de ensamblado,
     * preservando el recuento de vueltas, el total de puntos producidos, color, topología,
     * visibilidad y bloqueo.
     */
    fun toSceneGraphItems(): List<SceneGraphItem> {
        return parts.map { part ->
            val rounds = roundsByPartId[part.id].orEmpty()
            val roundCount = if (rounds.isNotEmpty()) rounds.size else part.roundCount
            val stitchCount = if (rounds.isNotEmpty()) {
                rounds.sumOf { it.producedStitches }
            } else {
                part.roundCount * 6
            }
            SceneGraphItem(
                id = part.id,
                name = part.name,
                depth = 0,
                isVisible = visibilityByPartId[part.id] ?: true,
                isLocked = lockByPartId[part.id] ?: false,
                colorHex = part.colorHex,
                roundCount = roundCount,
                stitchCount = stitchCount,
                topologyType = part.topologyType
            )
        }
    }
}

/**
 * Almacén reactivo en memoria que sincroniza bidireccionalmente las piezas y sus transformaciones TRS
 * entre [WorkspaceViewModel] y [com.crisanfitos.stitchmesh3d.ui.assembly.AssemblyViewModel] (SM-075).
 */
object ProjectPartsSessionStore {

    private val snapshotsByProjectId = ConcurrentHashMap<String, ProjectAssemblySnapshot>()

    /**
     * Obtiene el snapshot sincronizado de un proyecto si existe.
     */
    fun getSnapshot(projectId: String): ProjectAssemblySnapshot? {
        if (projectId.isBlank()) return null
        return snapshotsByProjectId[projectId]
    }

    /**
     * Publica o actualiza el estado de piezas y vueltas desde el Workbench,
     * conservando intactas las transformaciones espaciales TRS previamente calculadas en el ensamblador.
     */
    fun publishFromWorkbench(
        projectId: String,
        projectTitle: String,
        parts: List<ProjectPartUiModel>,
        roundsByPartId: Map<String, List<RoundItemUiModel>>
    ): ProjectAssemblySnapshot {
        val key = projectId.ifBlank { "proj-default" }
        val existing = snapshotsByProjectId[key]

        val validPartIds = parts.map { it.id }.toSet()
        val preservedTransforms = existing?.transformsByPartId
            ?.filterKeys { it in validPartIds }
            ?.mapValues { (partId, transform) ->
                val updatedName = parts.find { it.id == partId }?.name ?: transform.partName
                transform.copy(partName = updatedName)
            }
            ?: emptyMap()

        val preservedVisibility = existing?.visibilityByPartId
            ?.filterKeys { it in validPartIds }
            ?: emptyMap()

        val preservedLocks = existing?.lockByPartId
            ?.filterKeys { it in validPartIds }
            ?: emptyMap()

        val snapshot = ProjectAssemblySnapshot(
            projectId = key,
            projectTitle = projectTitle,
            parts = parts,
            roundsByPartId = roundsByPartId,
            transformsByPartId = preservedTransforms,
            visibilityByPartId = preservedVisibility,
            lockByPartId = preservedLocks
        )
        snapshotsByProjectId[key] = snapshot
        return snapshot
    }

    /**
     * Publica o actualiza las piezas, colores, visibilidad, bloqueo y transformaciones TRS
     * desde el Ensamblador 3D hacia el proyecto activo.
     */
    fun publishFromAssembly(
        projectId: String,
        projectTitle: String,
        sceneItems: List<SceneGraphItem>,
        transformsByPartId: Map<String, PartTransformState>
    ): ProjectAssemblySnapshot {
        val key = projectId.ifBlank { "proj-default" }
        val existing = snapshotsByProjectId[key]
        val existingRoundsMap = existing?.roundsByPartId?.toMutableMap() ?: mutableMapOf()

        val updatedParts = sceneItems.mapIndexed { idx, item ->
            val existingPart = existing?.parts?.find { it.id == item.id }
            val rounds = existingRoundsMap[item.id]
            if (rounds == null) {
                val initialInstruction = when (item.topologyType) {
                    PartTopologyType.FLAT_PANEL -> "10 cad"
                    else -> "AM 6"
                }
                val initialProduced = when (item.topologyType) {
                    PartTopologyType.FLAT_PANEL -> 10
                    else -> 6
                }
                existingRoundsMap[item.id] = listOf(
                    RoundItemUiModel(
                        id = "${item.id}_r1",
                        roundNumber = 1,
                        rawInstruction = initialInstruction,
                        producedStitches = initialProduced,
                        consumedStitches = 0,
                        declaredStitches = null,
                        isValid = true,
                        colorHex = item.colorHex
                    )
                )
            } else if (existingPart != null && existingPart.colorHex != item.colorHex) {
                // Propagar cambio de color de pieza a las vueltas que tenían el color base anterior
                existingRoundsMap[item.id] = rounds.map { r ->
                    if (r.colorHex == existingPart.colorHex) r.copy(colorHex = item.colorHex) else r
                }
            }

            ProjectPartUiModel(
                id = item.id,
                name = item.name,
                roundCount = existingRoundsMap[item.id]?.size ?: item.roundCount.coerceAtLeast(1),
                colorHex = item.colorHex,
                isValid = existingPart?.isValid ?: true,
                sortOrder = idx,
                topologyType = item.topologyType
            )
        }

        val validIds = updatedParts.map { it.id }.toSet()
        val cleanedRounds = existingRoundsMap.filterKeys { it in validIds }
        val cleanedTransforms = transformsByPartId.filterKeys { it in validIds }
        val visibilityMap = sceneItems.associate { it.id to it.isVisible }
        val lockMap = sceneItems.associate { it.id to it.isLocked }

        val snapshot = ProjectAssemblySnapshot(
            projectId = key,
            projectTitle = existing?.projectTitle ?: projectTitle,
            parts = updatedParts,
            roundsByPartId = cleanedRounds,
            transformsByPartId = cleanedTransforms,
            visibilityByPartId = visibilityMap,
            lockByPartId = lockMap
        )
        snapshotsByProjectId[key] = snapshot
        return snapshot
    }

    /**
     * Limpia todas las sesiones almacenadas (útil para aislamiento en tests unitarios).
     */
    fun clear() {
        snapshotsByProjectId.clear()
    }
}
