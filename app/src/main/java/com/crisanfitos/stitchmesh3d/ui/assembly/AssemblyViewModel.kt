package com.crisanfitos.stitchmesh3d.ui.assembly

import androidx.compose.ui.geometry.Offset
import com.crisanfitos.stitchmesh3d.core.mvi.BaseViewModel
import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import com.crisanfitos.stitchmesh3d.ui.assembly.components.PartTransformState
import com.crisanfitos.stitchmesh3d.ui.assembly.components.SceneGraphItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel que gestiona la máquina de estados del ensamblador CAD multipieza 3D (MVI),
 * conectando el árbol Scene Graph, la manipulación mediante gizmo 3D y el inspector numérico.
 * Conforme a Stitch Screen 14f11294, PRD RF-4.1..4.3 y SM-055.
 */
@HiltViewModel
class AssemblyViewModel @Inject constructor() :
    BaseViewModel<AssemblyState, AssemblyIntent, AssemblyEffect>(AssemblyState()) {

    init {
        loadDefaultAmigurumiAssembly()
    }

    override fun processIntent(intent: AssemblyIntent) {
        when (intent) {
            is AssemblyIntent.LoadProject -> {
                if (intent.projectId.isNotBlank()) {
                    setState {
                        copy(
                            projectId = intent.projectId,
                            projectTitle = if (projectTitle.isBlank() || projectTitle == "Proyecto Amigurumi") {
                                "Proyecto ${intent.projectId}"
                            } else projectTitle
                        )
                    }
                }
            }

            is AssemblyIntent.SelectPart -> {
                setState { copy(selectedPartId = intent.partId) }
            }

            is AssemblyIntent.ToggleVisibility -> {
                setState {
                    copy(
                        parts = parts.map { part ->
                            if (part.id == intent.partId) {
                                part.copy(isVisible = !part.isVisible)
                            } else part
                        }
                    )
                }
            }

            is AssemblyIntent.ToggleLock -> {
                setState {
                    val updatedParts = parts.map { part ->
                        if (part.id == intent.partId) {
                            part.copy(isLocked = !part.isLocked)
                        } else part
                    }
                    val currentTransform = transformsByPartId[intent.partId]
                    val updatedTransforms = if (currentTransform != null) {
                        transformsByPartId + (intent.partId to currentTransform.copy(isLocked = !currentTransform.isLocked))
                    } else transformsByPartId

                    copy(
                        parts = updatedParts,
                        transformsByPartId = updatedTransforms
                    )
                }
            }

            is AssemblyIntent.UpdateColor -> {
                setState {
                    copy(
                        parts = parts.map { part ->
                            if (part.id == intent.partId) {
                                part.copy(colorHex = intent.newColorHex)
                            } else part
                        }
                    )
                }
            }

            is AssemblyIntent.UpdatePosition -> {
                setState {
                    val transform = transformsByPartId[intent.partId]
                    if (transform != null && !transform.isLocked) {
                        val updated = transform.copy(
                            posX = intent.x,
                            posY = intent.y,
                            posZ = intent.z
                        )
                        copy(transformsByPartId = transformsByPartId + (intent.partId to updated))
                    } else this
                }
            }

            is AssemblyIntent.UpdateRotation -> {
                setState {
                    val transform = transformsByPartId[intent.partId]
                    if (transform != null && !transform.isLocked) {
                        val updated = transform.copy(
                            rotX = intent.rotX,
                            rotY = intent.rotY,
                            rotZ = intent.rotZ
                        )
                        copy(transformsByPartId = transformsByPartId + (intent.partId to updated))
                    } else this
                }
            }

            is AssemblyIntent.ToggleMirror -> {
                setState {
                    val transform = transformsByPartId[intent.partId]
                    if (transform != null && !transform.isLocked) {
                        val updated = transform.mirrorSagittal()
                        copy(transformsByPartId = transformsByPartId + (intent.partId to updated))
                    } else this
                }
            }

            is AssemblyIntent.ResetOrigin -> {
                setState {
                    val transform = transformsByPartId[intent.partId]
                    if (transform != null && !transform.isLocked) {
                        val updated = transform.resetToOrigin()
                        copy(transformsByPartId = transformsByPartId + (intent.partId to updated))
                    } else this
                }
            }

            is AssemblyIntent.SetGizmoMode -> {
                setState { copy(activeGizmoMode = intent.mode) }
            }

            is AssemblyIntent.ToggleWireframe -> {
                setState { copy(isWireframe = !isWireframe) }
            }

            is AssemblyIntent.ToggleCoordinateGrid -> {
                setState { copy(showCoordinateGrid = !showCoordinateGrid) }
            }

            is AssemblyIntent.ToggleIsolatedMode -> {
                setState { copy(isIsolatedMode = !isIsolatedMode) }
            }

            is AssemblyIntent.UpdateOrbit -> {
                setState {
                    copy(
                        orbitYaw = (orbitYaw + intent.deltaYaw) % 360f,
                        orbitPitch = (orbitPitch + intent.deltaPitch).coerceIn(-89f, 89f)
                    )
                }
            }

            is AssemblyIntent.UpdateZoom -> {
                setState {
                    val updatedZoom = (zoomScale * intent.scaleMultiplier).coerceIn(0.4f, 3.5f)
                    copy(zoomScale = updatedZoom)
                }
            }

            is AssemblyIntent.UpdatePan -> {
                setState {
                    copy(panOffset = panOffset + intent.deltaOffset)
                }
            }

            is AssemblyIntent.ResetCamera -> {
                setState {
                    copy(
                        orbitYaw = 45f,
                        orbitPitch = 30f,
                        zoomScale = 1.0f,
                        panOffset = Offset.Zero
                    )
                }
            }

            is AssemblyIntent.OpenAddPartDialog -> {
                setState { copy(showAddPartDialog = true) }
            }

            is AssemblyIntent.OpenRenamePartDialog -> {
                val target = state.value.parts.find { it.id == intent.partId }
                setState {
                    copy(
                        showRenamePartDialog = true,
                        partActionTarget = target
                    )
                }
            }

            is AssemblyIntent.OpenDeletePartDialog -> {
                val target = state.value.parts.find { it.id == intent.partId }
                setState {
                    copy(
                        showDeletePartDialog = true,
                        partActionTarget = target
                    )
                }
            }

            is AssemblyIntent.DismissDialogs -> {
                setState {
                    copy(
                        showAddPartDialog = false,
                        showRenamePartDialog = false,
                        showDeletePartDialog = false,
                        partActionTarget = null
                    )
                }
            }

            is AssemblyIntent.CreatePart -> {
                val newId = "part-${UUID.randomUUID().toString().take(8)}"
                val newPart = SceneGraphItem(
                    id = newId,
                    name = intent.name,
                    depth = 0,
                    isVisible = true,
                    isLocked = false,
                    colorHex = intent.colorHex,
                    roundCount = 1,
                    stitchCount = 6,
                    topologyType = intent.topologyType
                )
                val newTransform = PartTransformState(
                    partId = newId,
                    partName = intent.name,
                    posX = 0f,
                    posY = 0f,
                    posZ = 0f
                )

                setState {
                    copy(
                        parts = parts + newPart,
                        transformsByPartId = transformsByPartId + (newId to newTransform),
                        selectedPartId = newId,
                        showAddPartDialog = false
                    )
                }
                sendEffect(AssemblyEffect.ShowSnackbar("Pieza '${intent.name}' añadida"))
            }

            is AssemblyIntent.RenamePart -> {
                setState {
                    val updatedParts = parts.map {
                        if (it.id == intent.partId) it.copy(name = intent.newName) else it
                    }
                    val currentTransform = transformsByPartId[intent.partId]
                    val updatedTransforms = if (currentTransform != null) {
                        transformsByPartId + (intent.partId to currentTransform.copy(partName = intent.newName))
                    } else transformsByPartId

                    copy(
                        parts = updatedParts,
                        transformsByPartId = updatedTransforms,
                        showRenamePartDialog = false,
                        partActionTarget = null
                    )
                }
                sendEffect(AssemblyEffect.ShowSnackbar("Pieza renombrada a '${intent.newName}'"))
            }

            is AssemblyIntent.DuplicatePart -> {
                val source = state.value.parts.find { it.id == intent.partId } ?: return
                val newId = "part-${UUID.randomUUID().toString().take(8)}"
                val duplicatedName = "${source.name} (Copia)"
                val duplicatedPart = source.copy(
                    id = newId,
                    name = duplicatedName
                )
                val sourceTransform = state.value.transformsByPartId[intent.partId]
                val duplicatedTransform = (sourceTransform ?: PartTransformState(
                    partId = newId,
                    partName = duplicatedName
                )).copy(
                    partId = newId,
                    partName = duplicatedName,
                    posX = (sourceTransform?.posX ?: 0f) + 15f
                )

                setState {
                    copy(
                        parts = parts + duplicatedPart,
                        transformsByPartId = transformsByPartId + (newId to duplicatedTransform),
                        selectedPartId = newId
                    )
                }
                sendEffect(AssemblyEffect.ShowSnackbar("Pieza '${source.name}' duplicada"))
            }

            is AssemblyIntent.DeletePart -> {
                val target = state.value.parts.find { it.id == intent.partId }
                setState {
                    val remainingParts = parts.filterNot { it.id == intent.partId }
                    val updatedTransforms = transformsByPartId - intent.partId
                    val newSelectedId = if (selectedPartId == intent.partId) {
                        remainingParts.firstOrNull()?.id ?: ""
                    } else selectedPartId

                    copy(
                        parts = remainingParts,
                        transformsByPartId = updatedTransforms,
                        selectedPartId = newSelectedId,
                        showDeletePartDialog = false,
                        partActionTarget = null
                    )
                }
                sendEffect(AssemblyEffect.ShowSnackbar("Pieza '${target?.name ?: intent.partId}' eliminada"))
            }
        }
    }

    private fun loadDefaultAmigurumiAssembly() {
        val defaultParts = listOf(
            SceneGraphItem(
                id = "part-torso",
                name = "Cuerpo Principal",
                depth = 0,
                isVisible = true,
                isLocked = false,
                colorHex = "#E06D53",
                roundCount = 18,
                stitchCount = 432,
                topologyType = PartTopologyType.CLOSED_FILLED,
                hasChildren = true,
                isExpanded = true
            ),
            SceneGraphItem(
                id = "part-head",
                name = "Cabeza",
                parentId = "part-torso",
                depth = 1,
                isVisible = true,
                isLocked = false,
                colorHex = "#E06D53",
                roundCount = 16,
                stitchCount = 384,
                topologyType = PartTopologyType.CLOSED_FILLED
            ),
            SceneGraphItem(
                id = "part-arm-left",
                name = "Brazo Izquierdo",
                parentId = "part-torso",
                depth = 1,
                isVisible = true,
                isLocked = false,
                colorHex = "#52A474",
                roundCount = 10,
                stitchCount = 120,
                topologyType = PartTopologyType.SEMI_CLOSED_TUBE
            ),
            SceneGraphItem(
                id = "part-arm-right",
                name = "Brazo Derecho",
                parentId = "part-torso",
                depth = 1,
                isVisible = true,
                isLocked = false,
                colorHex = "#52A474",
                roundCount = 10,
                stitchCount = 120,
                topologyType = PartTopologyType.SEMI_CLOSED_TUBE
            ),
            SceneGraphItem(
                id = "part-leg-left",
                name = "Pierna Izquierda",
                parentId = "part-torso",
                depth = 1,
                isVisible = true,
                isLocked = false,
                colorHex = "#E5A93C",
                roundCount = 12,
                stitchCount = 180,
                topologyType = PartTopologyType.SEMI_CLOSED_TUBE
            ),
            SceneGraphItem(
                id = "part-leg-right",
                name = "Pierna Derecha",
                parentId = "part-torso",
                depth = 1,
                isVisible = true,
                isLocked = false,
                colorHex = "#E5A93C",
                roundCount = 12,
                stitchCount = 180,
                topologyType = PartTopologyType.SEMI_CLOSED_TUBE
            )
        )

        val defaultTransforms = mapOf(
            "part-torso" to PartTransformState("part-torso", "Cuerpo Principal", 0f, 0f, 0f),
            "part-head" to PartTransformState("part-head", "Cabeza", 0f, 45f, 0f),
            "part-arm-left" to PartTransformState("part-arm-left", "Brazo Izquierdo", -25f, 20f, 0f, rotZ = 20f),
            "part-arm-right" to PartTransformState("part-arm-right", "Brazo Derecho", 25f, 20f, 0f, rotZ = -20f),
            "part-leg-left" to PartTransformState("part-leg-left", "Pierna Izquierda", -15f, -35f, 0f),
            "part-leg-right" to PartTransformState("part-leg-right", "Pierna Derecha", 15f, -35f, 0f)
        )

        setState {
            copy(
                parts = defaultParts,
                transformsByPartId = defaultTransforms,
                selectedPartId = "part-torso"
            )
        }
    }

    /**
     * Sincroniza las transformaciones espaciales calculadas en el ensamblador
     * con una lista de piezas del modelo de dominio [ProjectPart] (SM-033, RF-4.2).
     */
    fun syncToProjectParts(domainParts: List<ProjectPart>): List<ProjectPart> {
        val transforms = state.value.transformsByPartId
        return domainParts.map { part ->
            val t = transforms[part.id]
            if (t != null) {
                part.copy(
                    transformPositionX = t.posX,
                    transformPositionY = t.posY,
                    transformPositionZ = t.posZ,
                    transformRotationX = t.rotX,
                    transformRotationY = t.rotY,
                    transformRotationZ = t.rotZ
                )
            } else {
                part
            }
        }
    }
}
