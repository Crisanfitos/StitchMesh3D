package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import com.crisanfitos.stitchmesh3d.ui.assembly.components.PartTransformState

/**
 * Nodo del Scene Graph jerárquico para amigurumi y piezas compuestas multipieza (SM-033, RF-4.2).
 *
 * Permite componer traslaciones y rotaciones locales respecto a piezas padre
 * (ej. Torso -> Cabeza -> Hocico/Oreja).
 */
data class SceneGraphNode(
    val partId: String,
    val name: String,
    val localMatrix: TransformMatrix4x4 = TransformMatrix4x4.identity(),
    val children: List<SceneGraphNode> = emptyList(),
    val isVisible: Boolean = true
) {
    /**
     * Calcula recursivamente el mapa de matrices de transformación mundial (World Matrices)
     * para este nodo y todos sus descendientes en el árbol.
     *
     * @param parentWorldMatrix Matriz mundial acumulada del nodo padre (por defecto Identidad).
     * @return Mapa de [partId] a su correspondiente [TransformMatrix4x4] en coordenadas del mundo.
     */
    fun computeWorldTransforms(
        parentWorldMatrix: TransformMatrix4x4 = TransformMatrix4x4.identity()
    ): Map<String, TransformMatrix4x4> {
        val worldMatrix = parentWorldMatrix * localMatrix
        val result = mutableMapOf<String, TransformMatrix4x4>()
        result[partId] = worldMatrix

        for (child in children) {
            result.putAll(child.computeWorldTransforms(worldMatrix))
        }
        return result
    }

    companion object {
        /**
         * Construye un nodo a partir de un [PartTransformState] de la interfaz de usuario.
         */
        fun fromTransformState(state: PartTransformState): SceneGraphNode {
            val matrix = TransformMatrix4x4.fromTransform(
                posX = state.posX,
                posY = state.posY,
                posZ = state.posZ,
                rotXDeg = state.rotX,
                rotYDeg = state.rotY,
                rotZDeg = state.rotZ,
                isMirrored = state.isMirrored
            )
            return SceneGraphNode(
                partId = state.partId,
                name = state.partName,
                localMatrix = matrix
            )
        }

        /**
         * Construye un nodo a partir de un [ProjectPart] del modelo de dominio.
         */
        fun fromProjectPart(part: ProjectPart): SceneGraphNode {
            val matrix = TransformMatrix4x4.fromTransform(
                posX = part.transformPositionX,
                posY = part.transformPositionY,
                posZ = part.transformPositionZ,
                rotXDeg = part.transformRotationX,
                rotYDeg = part.transformRotationY,
                rotZDeg = part.transformRotationZ
            )
            return SceneGraphNode(
                partId = part.id,
                name = part.name,
                localMatrix = matrix
            )
        }
    }
}
