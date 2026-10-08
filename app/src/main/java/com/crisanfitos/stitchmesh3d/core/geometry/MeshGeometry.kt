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

    /**
     * Genera un [java.nio.FloatBuffer] directo en memoria nativa con orden de bytes del sistema,
     * optimizado para transferencia de vértices a Filament / Sceneview VertexBuffer sin copias intermedias.
     */
    fun toDirectPositionBuffer(): java.nio.FloatBuffer {
        val byteBuffer = java.nio.ByteBuffer.allocateDirect(vertexPositions.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
        val floatBuffer = byteBuffer.asFloatBuffer()
        floatBuffer.put(vertexPositions)
        floatBuffer.flip()
        return floatBuffer
    }

    /**
     * Genera un [java.nio.FloatBuffer] directo en memoria nativa con orden de bytes del sistema
     * conteniendo los vectores normales unitarios para sombreado PBR en GPU.
     */
    fun toDirectNormalBuffer(): java.nio.FloatBuffer {
        val byteBuffer = java.nio.ByteBuffer.allocateDirect(vertexNormals.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
        val floatBuffer = byteBuffer.asFloatBuffer()
        floatBuffer.put(vertexNormals)
        floatBuffer.flip()
        return floatBuffer
    }

    /**
     * Genera un [java.nio.ShortBuffer] directo en memoria nativa con orden de bytes del sistema
     * para el búfer de índices de Filament IndexBuffer.
     */
    fun toDirectIndexBuffer(): java.nio.ShortBuffer {
        val byteBuffer = java.nio.ByteBuffer.allocateDirect(indices.size * 2)
            .order(java.nio.ByteOrder.nativeOrder())
        val shortBuffer = byteBuffer.asShortBuffer()
        shortBuffer.put(indices)
        shortBuffer.flip()
        return shortBuffer
    }

    /**
     * Calcula la envolvente tridimensional (Bounding Box) de la geometría en milímetros (RF-2.2, RF-3.3).
     */
    fun computeBoundingBox(): BoundingBox3D {
        if (vertexCount == 0 || vertexPositions.isEmpty()) {
            return BoundingBox3D(0f, 0f, 0f, 0f, 0f, 0f)
        }
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY

        for (i in 0 until vertexCount) {
            val idx = i * 3
            val x = vertexPositions[idx]
            val y = vertexPositions[idx + 1]
            val z = vertexPositions[idx + 2]

            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
            if (z < minZ) minZ = z
            if (z > maxZ) maxZ = z
        }

        return BoundingBox3D(
            minX = if (minX.isInfinite()) 0f else minX,
            maxX = if (maxX.isInfinite()) 0f else maxX,
            minY = if (minY.isInfinite()) 0f else minY,
            maxY = if (maxY.isInfinite()) 0f else maxY,
            minZ = if (minZ.isInfinite()) 0f else minZ,
            maxZ = if (maxZ.isInfinite()) 0f else maxZ
        )
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

/**
 * Envolvente tridimensional ortogonal alineada a los ejes (AABB) en milímetros.
 *
 * @property minX Coordenada mínima en eje transversal X (mm).
 * @property maxX Coordenada máxima en eje transversal X (mm).
 * @property minY Coordenada mínima en eje sagital Y (mm).
 * @property maxY Coordenada máxima en eje sagital Y (mm).
 * @property minZ Coordenada mínima en eje vertical Z (mm).
 * @property maxZ Coordenada máxima en eje vertical Z (mm).
 */
data class BoundingBox3D(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val minZ: Float,
    val maxZ: Float
) {
    val widthMm: Float get() = (maxX - minX).coerceAtLeast(0f)
    val depthMm: Float get() = (maxY - minY).coerceAtLeast(0f)
    val heightMm: Float get() = (maxZ - minZ).coerceAtLeast(0f)
    val maxDiameterMm: Float get() = maxOf(widthMm, depthMm)

    val isEmpty: Boolean get() = widthMm == 0f && depthMm == 0f && heightMm == 0f
}
