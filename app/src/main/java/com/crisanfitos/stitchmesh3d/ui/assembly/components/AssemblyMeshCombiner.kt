package com.crisanfitos.stitchmesh3d.ui.assembly.components

import com.crisanfitos.stitchmesh3d.core.engine.color.CrochetColorHelper
import com.crisanfitos.stitchmesh3d.core.geometry.MeshGeometry
import com.crisanfitos.stitchmesh3d.core.geometry.MeshRaycaster
import com.crisanfitos.stitchmesh3d.core.geometry.TransformMatrix4x4

/**
 * Gestor geométrico que fusiona múltiples piezas 3D del amigurumi en una única escena consolidada (SM-087).
 *
 * Aplica a cada pieza su matriz de transformación afín 4x4 (traslación, rotación de Euler y simetría)
 * y genera una [MeshGeometry] combinada lista para renderizado continuo PBR en Google Filament.
 */
object AssemblyMeshCombiner {

    /**
     * Genera o retorna una malla paramétrica por defecto para piezas que aún no tienen geometría generada.
     */
    fun createDefaultPartMesh(part: SceneGraphItem): MeshGeometry {
        val baseRadius = (part.roundCount * 2.5f).coerceIn(12f, 40f)
        return MeshRaycaster.createSphereMesh(
            radiusMm = baseRadius,
            rings = 12,
            sectors = 18
        )
    }

    /**
     * Obtiene la matriz afín 4x4 correspondiente a una pieza según su [PartTransformState].
     */
    fun getPartTransformMatrix(transformState: PartTransformState?): TransformMatrix4x4 {
        if (transformState == null) return TransformMatrix4x4.identity()
        return TransformMatrix4x4.fromTransform(
            posX = transformState.posX,
            posY = transformState.posY,
            posZ = transformState.posZ,
            rotXDeg = transformState.rotX,
            rotYDeg = transformState.rotY,
            rotZDeg = transformState.rotZ,
            scaleX = 1f,
            scaleY = 1f,
            scaleZ = 1f,
            isMirrored = transformState.isMirrored
        )
    }

    /**
     * Transforma la malla de una pieza específica a coordenadas de mundo según su estado TRS.
     */
    fun transformPartMesh(
        part: SceneGraphItem,
        mesh: MeshGeometry,
        transformState: PartTransformState?
    ): MeshGeometry {
        val matrix = getPartTransformMatrix(transformState)
        val transformed = matrix.transformMesh(mesh)
        val rgba = CrochetColorHelper.toRgbaFloats(part.colorHex)

        val colors = FloatArray(transformed.vertexCount * 4)
        for (i in 0 until transformed.vertexCount) {
            colors[i * 4] = rgba[0]
            colors[i * 4 + 1] = rgba[1]
            colors[i * 4 + 2] = rgba[2]
            colors[i * 4 + 3] = rgba[3]
        }

        return MeshGeometry(
            vertexPositions = transformed.vertexPositions,
            indices = transformed.indices,
            vertexCount = transformed.vertexCount,
            triangleCount = transformed.triangleCount,
            vertexNormals = transformed.vertexNormals,
            vertexColors = colors,
            vertexUvs = transformed.vertexUvs
        )
    }

    /**
     * Combina todas las piezas visibles en una única [MeshGeometry] continua en espacio de mundo.
     */
    fun combineParts(
        parts: List<SceneGraphItem>,
        transformsByPartId: Map<String, PartTransformState>,
        meshesByPartId: Map<String, MeshGeometry>,
        isIsolatedMode: Boolean = false,
        selectedPartId: String = ""
    ): MeshGeometry {
        val visibleParts = if (isIsolatedMode && selectedPartId.isNotBlank()) {
            parts.filter { it.id == selectedPartId }
        } else {
            parts.filter { it.isVisible }
        }

        if (visibleParts.isEmpty()) {
            return MeshGeometry(
                vertexPositions = FloatArray(0),
                indices = IntArray(0),
                vertexCount = 0,
                triangleCount = 0,
                vertexNormals = FloatArray(0),
                vertexColors = FloatArray(0)
            )
        }

        val transformedMeshes = visibleParts.map { part ->
            val sourceMesh = meshesByPartId[part.id] ?: createDefaultPartMesh(part)
            val transform = transformsByPartId[part.id]
            transformPartMesh(part, sourceMesh, transform)
        }

        var totalVertices = 0
        var totalIndices = 0
        for (m in transformedMeshes) {
            totalVertices += m.vertexCount
            totalIndices += m.indices.size
        }

        val combinedPositions = FloatArray(totalVertices * 3)
        val combinedNormals = FloatArray(totalVertices * 3)
        val combinedColors = FloatArray(totalVertices * 4)
        val combinedUvs = FloatArray(totalVertices * 2)
        val combinedIndices = IntArray(totalIndices)

        var vertexOffset = 0
        var indexOffset = 0

        for (m in transformedMeshes) {
            val vCount = m.vertexCount
            val indCount = m.indices.size
            val baseVertex = vertexOffset

            // Copiar posiciones
            System.arraycopy(m.vertexPositions, 0, combinedPositions, vertexOffset * 3, vCount * 3)

            // Copiar normales
            if (m.vertexNormals.isNotEmpty() && m.vertexNormals.size >= vCount * 3) {
                System.arraycopy(m.vertexNormals, 0, combinedNormals, vertexOffset * 3, vCount * 3)
            } else {
                for (i in 0 until vCount) {
                    combinedNormals[(vertexOffset + i) * 3 + 1] = 1.0f
                }
            }

            // Copiar colores
            if (m.vertexColors.isNotEmpty() && m.vertexColors.size >= vCount * 4) {
                System.arraycopy(m.vertexColors, 0, combinedColors, vertexOffset * 4, vCount * 4)
            } else {
                for (i in 0 until vCount) {
                    combinedColors[(vertexOffset + i) * 4] = 0.88f
                    combinedColors[(vertexOffset + i) * 4 + 1] = 0.43f
                    combinedColors[(vertexOffset + i) * 4 + 2] = 0.33f
                    combinedColors[(vertexOffset + i) * 4 + 3] = 1.0f
                }
            }

            // Copiar UVs si existen
            if (m.vertexUvs.isNotEmpty() && m.vertexUvs.size >= vCount * 2) {
                System.arraycopy(m.vertexUvs, 0, combinedUvs, vertexOffset * 2, vCount * 2)
            }

            // Copiar índices desplazados por baseVertex
            for (i in 0 until indCount) {
                combinedIndices[indexOffset + i] = baseVertex + m.indices[i]
            }

            vertexOffset += vCount
            indexOffset += indCount
        }

        return MeshGeometry(
            vertexPositions = combinedPositions,
            indices = combinedIndices,
            vertexCount = totalVertices,
            triangleCount = totalIndices / 3,
            vertexNormals = combinedNormals,
            vertexColors = combinedColors,
            vertexUvs = combinedUvs
        )
    }
}
