package com.crisanfitos.stitchmesh3d.core.geometry

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Representa un rayo en el espacio tridimensional definido por su origen y vector director unitario.
 */
data class Ray3D(
    val origin: FloatArray,
    val direction: FloatArray
) {
    init {
        require(origin.size == 3) { "El origen del rayo debe tener 3 componentes [X, Y, Z]" }
        require(direction.size == 3) { "La dirección del rayo debe tener 3 componentes [X, Y, Z]" }
    }

    companion object {
        /**
         * Crea un rayo garantizando que la dirección esté normalizada (longitud = 1.0).
         */
        fun create(originX: Float, originY: Float, originZ: Float, dirX: Float, dirY: Float, dirZ: Float): Ray3D {
            val len = sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
            val inv = if (len > 1e-6f) 1f / len else 1f
            return Ray3D(
                origin = floatArrayOf(originX, originY, originZ),
                direction = floatArrayOf(dirX * inv, dirY * inv, dirZ * inv)
            )
        }
    }
}

/**
 * Resultado de un impacto de raycast contra una malla de crochet.
 *
 * @property point Coordenada cartesiana 3D [X, Y, Z] del punto de intersección en milímetros.
 * @property normal Vector normal unitario [Nx, Ny, Nz] de la superficie en el punto impactado.
 * @property distance Distancia escalar t a lo largo del rayo desde el origen.
 * @property triangleIndex Índice ordinal del triángulo impactado en la malla.
 */
data class RaycastHit(
    val point: FloatArray,
    val normal: FloatArray,
    val distance: Float,
    val triangleIndex: Int
)

/**
 * Módulo geométrico de trazado de rayos (Raycasting) y anclaje superficial (Surface Snapping)
 * para el ensamblador 3D de amigurumis (SM-070, PRD RF-4.1, RF-4.2).
 *
 * Implementa el algoritmo Möller–Trumbore para intersección rayo-triángulo de alto rendimiento
 * (< 5 ms por consulta para mallas de amigurumi de miles de polígonos).
 */
object MeshRaycaster {

    private const val EPSILON = 1e-7f

    /**
     * Crea una malla paramétrica esférica indexada [MeshGeometry] para pruebas o proxy de colisión.
     *
     * @param radiusMm Radio en milímetros.
     * @param rings Número de subdivisiones en latitud.
     * @param sectors Número de subdivisiones en longitud.
     * @param centerX Centro X en milímetros.
     * @param centerY Centro Y en milímetros.
     * @param centerZ Centro Z en milímetros.
     */
    fun createSphereMesh(
        radiusMm: Float,
        rings: Int = 10,
        sectors: Int = 16,
        centerX: Float = 0f,
        centerY: Float = 0f,
        centerZ: Float = 0f
    ): MeshGeometry {
        val vertexCount = (rings + 1) * (sectors + 1)
        val positions = FloatArray(vertexCount * 3)
        val normals = FloatArray(vertexCount * 3)
        val indicesList = mutableListOf<Short>()

        var vertIdx = 0
        val pi = Math.PI.toFloat()
        val twoPi = (2.0 * Math.PI).toFloat()

        for (r in 0..rings) {
            val phi = -pi / 2f + pi * (r.toFloat() / rings)
            val cosPhi = cos(phi)
            val sinPhi = sin(phi)

            for (s in 0..sectors) {
                val theta = twoPi * (s.toFloat() / sectors)
                val cosTheta = cos(theta)
                val sinTheta = sin(theta)

                val nx = cosPhi * cosTheta
                val ny = sinPhi
                val nz = cosPhi * sinTheta

                val vx = centerX + radiusMm * nx
                val vy = centerY + radiusMm * ny
                val vz = centerZ + radiusMm * nz

                positions[vertIdx * 3] = vx
                positions[vertIdx * 3 + 1] = vy
                positions[vertIdx * 3 + 2] = vz

                normals[vertIdx * 3] = nx
                normals[vertIdx * 3 + 1] = ny
                normals[vertIdx * 3 + 2] = nz

                vertIdx++
            }
        }

        for (r in 0 until rings) {
            for (s in 0 until sectors) {
                val first = (r * (sectors + 1) + s).toShort()
                val second = (first + sectors + 1).toShort()

                indicesList.add(first)
                indicesList.add(second)
                indicesList.add((first + 1).toShort())

                indicesList.add(second)
                indicesList.add((second + 1).toShort())
                indicesList.add((first + 1).toShort())
            }
        }

        val indices = ShortArray(indicesList.size) { indicesList[it] }
        return MeshGeometry(
            vertexPositions = positions,
            indices = indices,
            vertexCount = vertexCount,
            triangleCount = indices.size / 3,
            vertexNormals = normals
        )
    }

    /**
     * Traza un rayo [ray] contra [mesh] y devuelve el punto de impacto más cercano al origen del rayo.
     * Retorna null si no hay ninguna intersección.
     */
    fun castRay(mesh: MeshGeometry, ray: Ray3D): RaycastHit? {
        val indices = mesh.indices
        val positions = mesh.vertexPositions
        val normals = mesh.vertexNormals
        val triangleCount = mesh.triangleCount

        if (triangleCount == 0 || indices.isEmpty() || positions.isEmpty()) {
            return null
        }

        var closestDistance = Float.MAX_VALUE
        var closestHit: RaycastHit? = null

        val origX = ray.origin[0]
        val origY = ray.origin[1]
        val origZ = ray.origin[2]

        val dirX = ray.direction[0]
        val dirY = ray.direction[1]
        val dirZ = ray.direction[2]

        for (t in 0 until triangleCount) {
            val i0 = (indices[t * 3].toInt() and 0xFFFF)
            val i1 = (indices[t * 3 + 1].toInt() and 0xFFFF)
            val i2 = (indices[t * 3 + 2].toInt() and 0xFFFF)

            val v0x = positions[i0 * 3]
            val v0y = positions[i0 * 3 + 1]
            val v0z = positions[i0 * 3 + 2]

            val v1x = positions[i1 * 3]
            val v1y = positions[i1 * 3 + 1]
            val v1z = positions[i1 * 3 + 2]

            val v2x = positions[i2 * 3]
            val v2y = positions[i2 * 3 + 1]
            val v2z = positions[i2 * 3 + 2]

            val e1x = v1x - v0x
            val e1y = v1y - v0y
            val e1z = v1z - v0z

            val e2x = v2x - v0x
            val e2y = v2y - v0y
            val e2z = v2z - v0z

            // pvec = dir x e2
            val pvecX = dirY * e2z - dirZ * e2y
            val pvecY = dirZ * e2x - dirX * e2z
            val pvecZ = dirX * e2y - dirY * e2x

            val det = e1x * pvecX + e1y * pvecY + e1z * pvecZ

            if (det > -EPSILON && det < EPSILON) continue // Rayo paralelo al triángulo

            val invDet = 1.0f / det

            // tvec = origin - v0
            val tvecX = origX - v0x
            val tvecY = origY - v0y
            val tvecZ = origZ - v0z

            val u = (tvecX * pvecX + tvecY * pvecY + tvecZ * pvecZ) * invDet
            if (u < 0.0f || u > 1.0f) continue

            // qvec = tvec x e1
            val qvecX = tvecY * e1z - tvecZ * e1y
            val qvecY = tvecZ * e1x - tvecX * e1z
            val qvecZ = tvecX * e1y - tvecY * e1x

            val v = (dirX * qvecX + dirY * qvecY + dirZ * qvecZ) * invDet
            if (v < 0.0f || (u + v) > 1.0f) continue

            val dist = (e2x * qvecX + e2y * qvecY + e2z * qvecZ) * invDet
            if (dist > 1e-4f && dist < closestDistance) {
                closestDistance = dist

                val hitPx = origX + dist * dirX
                val hitPy = origY + dist * dirY
                val hitPz = origZ + dist * dirZ

                // Calcular normal en el punto de impacto
                val hitNormal = if (normals.isNotEmpty()) {
                    val w = 1.0f - u - v
                    val n0x = normals[i0 * 3]
                    val n0y = normals[i0 * 3 + 1]
                    val n0z = normals[i0 * 3 + 2]

                    val n1x = normals[i1 * 3]
                    val n1y = normals[i1 * 3 + 1]
                    val n1z = normals[i1 * 3 + 2]

                    val n2x = normals[i2 * 3]
                    val n2y = normals[i2 * 3 + 1]
                    val n2z = normals[i2 * 3 + 2]

                    val nx = w * n0x + u * n1x + v * n2x
                    val ny = w * n0y + u * n1y + v * n2y
                    val nz = w * n0z + u * n1z + v * n2z

                    val nLen = sqrt(nx * nx + ny * ny + nz * nz)
                    if (nLen > 1e-6f) {
                        floatArrayOf(nx / nLen, ny / nLen, nz / nLen)
                    } else {
                        calculateFaceNormal(e1x, e1y, e1z, e2x, e2y, e2z)
                    }
                } else {
                    calculateFaceNormal(e1x, e1y, e1z, e2x, e2y, e2z)
                }

                closestHit = RaycastHit(
                    point = floatArrayOf(hitPx, hitPy, hitPz),
                    normal = hitNormal,
                    distance = dist,
                    triangleIndex = t
                )
            }
        }

        return closestHit
    }

    private fun calculateFaceNormal(e1x: Float, e1y: Float, e1z: Float, e2x: Float, e2y: Float, e2z: Float): FloatArray {
        val nx = e1y * e2z - e1z * e2y
        val ny = e1z * e2x - e1x * e2z
        val nz = e1x * e2y - e1y * e2x
        val len = sqrt(nx * nx + ny * ny + nz * nz)
        return if (len > 1e-6f) {
            floatArrayOf(nx / len, ny / len, nz / len)
        } else {
            floatArrayOf(0f, 1f, 0f)
        }
    }

    /**
     * Calcula los ángulos de rotación de Euler [rotX, rotY, rotZ] en grados para orientar
     * automáticamente la base o costura de una pieza secundaria hacia afuera a lo largo
     * del vector normal de la superficie impactada [normal].
     *
     * Asume que la orientación natural base de la pieza apunta hacia arriba (+Y).
     */
    fun calculateSeamAlignmentEulerAngles(normal: FloatArray): FloatArray {
        val nx = normal[0]
        val ny = normal[1]
        val nz = normal[2]

        val len = sqrt(nx * nx + ny * ny + nz * nz)
        val unitNx = if (len > 1e-6f) nx / len else 0f
        val unitNy = if (len > 1e-6f) ny / len else 1f
        val unitNz = if (len > 1e-6f) nz / len else 0f

        // 1. Inclinación respecto al plano horizontal X-Z (Pitch / RotX)
        val horizontalDist = sqrt(unitNx * unitNx + unitNz * unitNz)
        val rotXRad = atan2(unitNz, unitNy)

        // 2. Azimut respecto al eje Z (Yaw / RotY)
        val rotYRad = atan2(-unitNx, unitNz)

        // 3. Roll / RotZ en plano frontal
        val rotZRad = atan2(-unitNx, unitNy)

        val rotXDeg = Math.toDegrees(rotXRad.toDouble()).toFloat()
        val rotYDeg = Math.toDegrees(rotYRad.toDouble()).toFloat()
        val rotZDeg = Math.toDegrees(rotZRad.toDouble()).toFloat()

        return floatArrayOf(rotXDeg, rotYDeg, rotZDeg)
    }

    /**
     * Alinea un punto en el espacio proyectado de pantalla hacia un rayo en el espacio 3D mundial.
     */
    fun screenPointToWorldRay(
        screenX: Float,
        screenY: Float,
        centerX: Float,
        centerY: Float,
        zoomScale: Float,
        orbitYawDeg: Float,
        orbitPitchDeg: Float
    ): Ray3D {
        val yawRad = Math.toRadians(orbitYawDeg.toDouble()).toFloat()
        val pitchRad = Math.toRadians(orbitPitchDeg.toDouble()).toFloat()

        val cosYaw = cos(yawRad)
        val sinYaw = sin(yawRad)
        val cosPitch = cos(pitchRad)
        val sinPitch = sin(pitchRad)

        // Vector director de cámara apuntando hacia el centro
        val dirX = -sinYaw * cosPitch
        val dirY = -sinPitch
        val dirZ = -cosYaw * cosPitch

        // Origen del rayo desplazado desde la pantalla
        val deltaScreenX = (screenX - centerX) / (3.2f * zoomScale)
        val deltaScreenY = -(screenY - centerY) / (3.2f * zoomScale)

        val origX = deltaScreenX * cosYaw - dirX * 100f
        val origY = deltaScreenY - dirY * 100f
        val origZ = deltaScreenX * sinYaw - dirZ * 100f

        return Ray3D.create(origX, origY, origZ, dirX, dirY, dirZ)
    }
}
