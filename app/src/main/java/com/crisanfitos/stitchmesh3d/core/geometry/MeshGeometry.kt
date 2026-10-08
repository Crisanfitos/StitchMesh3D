package com.crisanfitos.stitchmesh3d.core.geometry

/**
 * Representa una geometría 3D indexada de superficie continua para crochet.
 *
 * Compatible con Filament / Sceneview / OpenGL ES:
 * @property vertexPositions Búfer continuo de coordenadas de vértices [X, Y, Z, X, Y, Z, ...] en FloatArray.
 * @property indices Búfer indexado de triángulos [i0, i1, i2, ...] en ShortArray.
 * @property vertexCount Cantidad total de vértices en la malla.
 * @property triangleCount Cantidad total de triángulos (indices.size / 3).
 */
data class MeshGeometry(
    val vertexPositions: FloatArray,
    val indices: ShortArray,
    val vertexCount: Int,
    val triangleCount: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MeshGeometry

        if (!vertexPositions.contentEquals(other.vertexPositions)) return false
        if (!indices.contentEquals(other.indices)) return false
        if (vertexCount != other.vertexCount) return false
        if (triangleCount != other.triangleCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vertexPositions.contentHashCode()
        result = 31 * result + indices.contentHashCode()
        result = 31 * result + vertexCount
        result = 31 * result + triangleCount
        return result
    }
}
