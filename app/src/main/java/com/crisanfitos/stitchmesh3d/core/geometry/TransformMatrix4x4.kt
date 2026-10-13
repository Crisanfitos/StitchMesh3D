package com.crisanfitos.stitchmesh3d.core.geometry

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Representación matemática inmutable de una matriz afín de transformación 4x4 en orden Column-Major
 * (compatible con OpenGL ES, Google Filament y glTF 2.0).
 *
 * Los índices del array [m] representan la matriz:
 * | m[0]  m[4]  m[8]   m[12] |
 * | m[1]  m[5]  m[9]   m[13] |
 * | m[2]  m[6]  m[10]  m[14] |
 * | m[3]  m[7]  m[11]  m[15] |
 */
class TransformMatrix4x4(
    val values: FloatArray = FloatArray(16).apply {
        this[0] = 1f; this[5] = 1f; this[10] = 1f; this[15] = 1f
    }
) {
    init {
        require(values.size == 16) { "La matriz de transformación debe tener exactamente 16 elementos" }
    }

    /**
     * Multiplica esta matriz por otra matriz [other] (retorna this * other).
     */
    operator fun times(other: TransformMatrix4x4): TransformMatrix4x4 {
        val a = this.values
        val b = other.values
        val res = FloatArray(16)

        for (col in 0 until 4) {
            for (row in 0 until 4) {
                res[col * 4 + row] =
                    a[0 * 4 + row] * b[col * 4 + 0] +
                    a[1 * 4 + row] * b[col * 4 + 1] +
                    a[2 * 4 + row] * b[col * 4 + 2] +
                    a[3 * 4 + row] * b[col * 4 + 3]
            }
        }
        return TransformMatrix4x4(res)
    }

    /**
     * Transforma un punto tridimensional [x, y, z] aplicando traslación, rotación y escala afín.
     * Asume coordenada homogénea W = 1.0.
     */
    fun transformPoint(x: Float, y: Float, z: Float): FloatArray {
        val m = values
        val xPrime = m[0] * x + m[4] * y + m[8] * z + m[12]
        val yPrime = m[1] * x + m[5] * y + m[9] * z + m[13]
        val zPrime = m[2] * x + m[6] * y + m[10] * z + m[14]
        val wPrime = m[3] * x + m[7] * y + m[11] * z + m[15]

        val invW = if (kotlin.math.abs(wPrime) > 1e-6f) 1f / wPrime else 1f
        return floatArrayOf(xPrime * invW, yPrime * invW, zPrime * invW)
    }

    /**
     * Transforma un vector normal [nx, ny, nz] utilizando la transpuesta de la inversa de la submatriz 3x3,
     * garantizando ortogonalidad de superficie ante escalas o reflexiones, y renormalizando a ||n|| = 1.0.
     */
    fun transformNormal(nx: Float, ny: Float, nz: Float): FloatArray {
        val inv = invert() ?: identity()
        // Transpuesta de la inversa: filas pasan a columnas
        val m = inv.values
        val tNx = m[0] * nx + m[1] * ny + m[2] * nz
        val tNy = m[4] * nx + m[5] * ny + m[6] * nz
        val tNz = m[8] * nx + m[9] * ny + m[10] * nz

        val len = sqrt(tNx * tNx + tNy * tNy + tNz * tNz)
        return if (len > 1e-6f) {
            floatArrayOf(tNx / len, tNy / len, tNz / len)
        } else {
            floatArrayOf(nx, ny, nz)
        }
    }

    /**
     * Calcula la inversa de la matriz 4x4. Retorna null si la matriz es singular (determinante = 0).
     */
    fun invert(): TransformMatrix4x4? {
        val m = values
        val inv = FloatArray(16)

        inv[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15] +
                 m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10]

        inv[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15] -
                 m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10]

        inv[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15] +
                 m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9]

        inv[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14] -
                  m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9]

        inv[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15] -
                 m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10]

        inv[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15] +
                 m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10]

        inv[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15] -
                 m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9]

        inv[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14] +
                  m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9]

        inv[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15] +
                 m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6]

        inv[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15] -
                 m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6]

        inv[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15] +
                  m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5]

        inv[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14] -
                  m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5]

        inv[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11] -
                 m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6]

        inv[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11] +
                 m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6]

        inv[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11] -
                  m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5]

        inv[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10] +
                  m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5]

        val det = m[0] * inv[0] + m[1] * inv[4] + m[2] * inv[8] + m[3] * inv[12]
        if (kotlin.math.abs(det) < 1e-8f) return null

        val invDet = 1.0f / det
        val out = FloatArray(16)
        for (i in 0 until 16) {
            out[i] = inv[i] * invDet
        }
        return TransformMatrix4x4(out)
    }

    /**
     * Aplica la transformación espacial completa a una [MeshGeometry], creando una nueva geometría transformada.
     */
    fun transformMesh(source: MeshGeometry): MeshGeometry {
        val newPositions = FloatArray(source.vertexPositions.size)
        val newNormals = FloatArray(source.vertexNormals.size)

        for (i in 0 until source.vertexCount) {
            val px = source.vertexPositions[i * 3]
            val py = source.vertexPositions[i * 3 + 1]
            val pz = source.vertexPositions[i * 3 + 2]
            val transformedP = transformPoint(px, py, pz)
            newPositions[i * 3] = transformedP[0]
            newPositions[i * 3 + 1] = transformedP[1]
            newPositions[i * 3 + 2] = transformedP[2]

            if (source.vertexNormals.isNotEmpty() && (i * 3 + 2) < source.vertexNormals.size) {
                val nx = source.vertexNormals[i * 3]
                val ny = source.vertexNormals[i * 3 + 1]
                val nz = source.vertexNormals[i * 3 + 2]
                val transformedN = transformNormal(nx, ny, nz)
                newNormals[i * 3] = transformedN[0]
                newNormals[i * 3 + 1] = transformedN[1]
                newNormals[i * 3 + 2] = transformedN[2]
            }
        }

        return source.copy(
            vertexPositions = newPositions,
            vertexNormals = newNormals
        )
    }

    companion object {
        /**
         * Retorna la matriz identidad 4x4.
         */
        fun identity(): TransformMatrix4x4 = TransformMatrix4x4()

        /**
         * Crea una matriz de traslación pura.
         */
        fun translation(x: Float, y: Float, z: Float): TransformMatrix4x4 {
            val m = FloatArray(16).apply {
                this[0] = 1f; this[5] = 1f; this[10] = 1f; this[15] = 1f
                this[12] = x
                this[13] = y
                this[14] = z
            }
            return TransformMatrix4x4(m)
        }

        /**
         * Crea una matriz de escala.
         */
        fun scale(sx: Float, sy: Float, sz: Float): TransformMatrix4x4 {
            val m = FloatArray(16).apply {
                this[0] = sx
                this[5] = sy
                this[10] = sz
                this[15] = 1f
            }
            return TransformMatrix4x4(m)
        }

        /**
         * Crea una matriz de rotación en torno al eje X (grados sexagesimales).
         */
        fun rotationX(deg: Float): TransformMatrix4x4 {
            val rad = Math.toRadians(deg.toDouble()).toFloat()
            val c = cos(rad)
            val s = sin(rad)
            val m = FloatArray(16).apply {
                this[0] = 1f
                this[5] = c;  this[9] = -s
                this[6] = s;  this[10] = c
                this[15] = 1f
            }
            return TransformMatrix4x4(m)
        }

        /**
         * Crea una matriz de rotación en torno al eje Y (grados sexagesimales).
         */
        fun rotationY(deg: Float): TransformMatrix4x4 {
            val rad = Math.toRadians(deg.toDouble()).toFloat()
            val c = cos(rad)
            val s = sin(rad)
            val m = FloatArray(16).apply {
                this[0] = c;   this[8] = s
                this[5] = 1f
                this[2] = -s;  this[10] = c
                this[15] = 1f
            }
            return TransformMatrix4x4(m)
        }

        /**
         * Crea una matriz de rotación en torno al eje Z (grados sexagesimales).
         */
        fun rotationZ(deg: Float): TransformMatrix4x4 {
            val rad = Math.toRadians(deg.toDouble()).toFloat()
            val c = cos(rad)
            val s = sin(rad)
            val m = FloatArray(16).apply {
                this[0] = c;  this[4] = -s
                this[1] = s;  this[5] = c
                this[10] = 1f
                this[15] = 1f
            }
            return TransformMatrix4x4(m)
        }

        /**
         * Crea una matriz de rotación de Euler combinada Rz * Ry * Rx (orden estándar CAD).
         */
        fun rotationEuler(rotXDeg: Float, rotYDeg: Float, rotZDeg: Float): TransformMatrix4x4 {
            return rotationZ(rotZDeg) * rotationY(rotYDeg) * rotationX(rotXDeg)
        }

        /**
         * Crea la matriz afín TRS completa: T(x,y,z) * R(rx,ry,rz) * S(sx,sy,sz).
         * Si [isMirrored] es verdadero, aplica simetría en plano sagital X = 0 (sx = -1).
         */
        fun fromTransform(
            posX: Float,
            posY: Float,
            posZ: Float,
            rotXDeg: Float = 0f,
            rotYDeg: Float = 0f,
            rotZDeg: Float = 0f,
            scaleX: Float = 1f,
            scaleY: Float = 1f,
            scaleZ: Float = 1f,
            isMirrored: Boolean = false
        ): TransformMatrix4x4 {
            val t = translation(posX, posY, posZ)
            val r = rotationEuler(rotXDeg, rotYDeg, rotZDeg)
            val effSx = if (isMirrored) -scaleX else scaleX
            val s = scale(effSx, scaleY, scaleZ)
            return t * r * s
        }
    }
}
