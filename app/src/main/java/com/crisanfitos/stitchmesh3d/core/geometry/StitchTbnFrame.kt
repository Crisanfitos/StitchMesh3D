package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Representa el marco de referencia ortonormal local (Tangente, Bitangente, Normal)
 * y posición tridimensional de una celda de punto de crochet en el espacio CAD (SM-086).
 *
 * Utilizado para alinear proceduralmente los bucles volumétricos tubulares maestros
 * de hilo instanciados sobre la superficie del tejido (Fase 2 Realismo 3D).
 *
 * @property origin Coordenadas [X, Y, Z] del centroide del punto en milímetros.
 * @property tangent Vector unitario [Tx, Ty, Tz] a lo largo de la vuelta (eje local X).
 * @property bitangent Vector unitario [Bx, By, Bz] en la dirección de altura del punto (eje local Y).
 * @property normal Vector unitario [Nx, Ny, Nz] apuntando hacia la normal exterior de la cara (eje local Z).
 * @property stitchType Tipo de punto de crochet asociado.
 * @property colorHex Color hexadecimal de la lazada (#RRGGBB).
 * @property roundIndex Índice de la vuelta (1..N).
 * @property stitchIndex Índice ordinal del punto dentro de la vuelta (0..M-1).
 * @property widthMm Ancho calibrado del punto en milímetros (dimensión a lo largo de Tangente).
 * @property heightMm Altura calibrada del punto en milímetros (dimensión a lo largo de Bitangente).
 * @property thicknessMm Espesor calibrado del punto en milímetros (dimensión a lo largo de Normal).
 */
data class StitchTbnFrame(
    val origin: FloatArray,
    val tangent: FloatArray,
    val bitangent: FloatArray,
    val normal: FloatArray,
    val stitchType: StitchType,
    val colorHex: String = "#E06D53",
    val roundIndex: Int = 1,
    val stitchIndex: Int = 0,
    val widthMm: Float = 1.0f,
    val heightMm: Float = 1.0f,
    val thicknessMm: Float = 1.0f
) {
    init {
        require(origin.size == 3) { "origin must have 3 components" }
        require(tangent.size == 3) { "tangent must have 3 components" }
        require(bitangent.size == 3) { "bitangent must have 3 components" }
        require(normal.size == 3) { "normal must have 3 components" }
    }

    /**
     * Construye la matriz afín 4x4 de instanciación que transforma la geometría del prototipo
     * maestro centrado en el origen al marco TBN y escala física en milímetros de este punto.
     */
    fun toTransformMatrix(scaleOverride: FloatArray? = null): TransformMatrix4x4 {
        val sx = scaleOverride?.getOrNull(0) ?: widthMm
        val sy = scaleOverride?.getOrNull(1) ?: heightMm
        val sz = scaleOverride?.getOrNull(2) ?: thicknessMm

        return TransformMatrix4x4.fromBasisAndOrigin(
            origin = origin,
            tangent = tangent,
            bitangent = bitangent,
            normal = normal,
            scaleX = sx,
            scaleY = sy,
            scaleZ = sz
        )
    }

    /**
     * Verifica analíticamente si la base (T, B, N) es aproximadamente ortonormal:
     * - Magnitudes ||T|| ≈ 1.0, ||B|| ≈ 1.0, ||N|| ≈ 1.0
     * - Productos punto T·B ≈ 0, T·N ≈ 0, B·N ≈ 0
     */
    fun isOrthonormal(epsilon: Float = 0.05f): Boolean {
        fun len(v: FloatArray) = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        fun dot(a: FloatArray, b: FloatArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

        val lenT = len(tangent)
        val lenB = len(bitangent)
        val lenN = len(normal)

        if (abs(lenT - 1.0f) > epsilon) return false
        if (abs(lenB - 1.0f) > epsilon) return false
        if (abs(lenN - 1.0f) > epsilon) return false

        if (abs(dot(tangent, bitangent)) > epsilon) return false
        if (abs(dot(tangent, normal)) > epsilon) return false
        if (abs(dot(bitangent, normal)) > epsilon) return false

        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StitchTbnFrame

        if (!origin.contentEquals(other.origin)) return false
        if (!tangent.contentEquals(other.tangent)) return false
        if (!bitangent.contentEquals(other.bitangent)) return false
        if (!normal.contentEquals(other.normal)) return false
        if (stitchType != other.stitchType) return false
        if (colorHex != other.colorHex) return false
        if (roundIndex != other.roundIndex) return false
        if (stitchIndex != other.stitchIndex) return false
        if (widthMm != other.widthMm) return false
        if (heightMm != other.heightMm) return false
        if (thicknessMm != other.thicknessMm) return false

        return true
    }

    override fun hashCode(): Int {
        var result = origin.contentHashCode()
        result = 31 * result + tangent.contentHashCode()
        result = 31 * result + bitangent.contentHashCode()
        result = 31 * result + normal.contentHashCode()
        result = 31 * result + stitchType.hashCode()
        result = 31 * result + colorHex.hashCode()
        result = 31 * result + roundIndex
        result = 31 * result + stitchIndex
        result = 31 * result + widthMm.hashCode()
        result = 31 * result + heightMm.hashCode()
        result = 31 * result + thicknessMm.hashCode()
        return result
    }
}
