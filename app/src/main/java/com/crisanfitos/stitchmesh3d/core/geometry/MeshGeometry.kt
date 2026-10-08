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
    val triangleCount: Int,
    val vertexNormals: FloatArray = FloatArray(vertexPositions.size)
) {
    /**
     * Retorna las coordenadas [X, Y, Z] del vértice en milímetros.
     */
    fun getVertexPosition(index: Int): FloatArray {
        require(index in 0 until vertexCount) { "Índice de vértice fuera de rango: $index (total: $vertexCount)" }
        return floatArrayOf(
            vertexPositions[index * 3],
            vertexPositions[index * 3 + 1],
            vertexPositions[index * 3 + 2]
        )
    }

    /**
     * Retorna el vector normal unitario [Nx, Ny, Nz] del vértice.
     */
    fun getVertexNormal(index: Int): FloatArray {
        require(index in 0 until vertexCount) { "Índice de vértice fuera de rango: $index (total: $vertexCount)" }
        if (vertexNormals.isEmpty()) return floatArrayOf(0f, 0f, 1f)
        return floatArrayOf(
            vertexNormals[index * 3],
            vertexNormals[index * 3 + 1],
            vertexNormals[index * 3 + 2]
        )
    }

    /**
     * Retorna la magnitud euclidiana ||n|| del vector normal del vértice.
     */
    fun getNormalMagnitude(index: Int): Float {
        val n = getVertexNormal(index)
        return kotlin.math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2])
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MeshGeometry

        if (!vertexPositions.contentEquals(other.vertexPositions)) return false
        if (!indices.contentEquals(other.indices)) return false
        if (!vertexNormals.contentEquals(other.vertexNormals)) return false
        if (vertexCount != other.vertexCount) return false
        if (triangleCount != other.triangleCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vertexPositions.contentHashCode()
        result = 31 * result + indices.contentHashCode()
        result = 31 * result + vertexNormals.contentHashCode()
        result = 31 * result + vertexCount
        result = 31 * result + triangleCount
        return result
    }
}

