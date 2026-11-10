package com.crisanfitos.stitchmesh3d.ui.viewport.components

import com.crisanfitos.stitchmesh3d.core.geometry.BoundingBox3D

/**
 * Calculador matemático de encuadre métrico orbital y presets de cámara para Google Filament (SM-085).
 *
 * Mantiene la escala real en milímetros de la pieza de crochet sin deformarla a un cubo unitario,
 * permitiendo comparar visualmente tamaños de piezas generadas con distintos calibres de hilado.
 */
object MetricCameraFramingHelper {

    /**
     * Parámetros de encuadre de cámara calculados a partir de las cotas reales en milímetros.
     */
    data class CameraFraming(
        val targetX: Float,
        val targetY: Float,
        val targetZ: Float,
        val eyeX: Float,
        val eyeY: Float,
        val eyeZ: Float,
        val upX: Float,
        val upY: Float,
        val upZ: Float,
        val fitDistanceMm: Float
    )

    /**
     * Calcula los vectores de posición del ojo, objetivo y vector Up para un [CameraPreset] dado,
     * alineados con el sistema de coordenadas estándar glTF 2.0 (Y-up, X-right, Z-forward).
     *
     * @param bbox Bounding box real en milímetros de la geometría 3D.
     * @param preset Orientación deseada de la cámara orbital.
     */
    fun calculateFraming(
        bbox: BoundingBox3D,
        preset: CameraPreset = CameraPreset.ISOMETRIC
    ): CameraFraming {
        val (targetX, targetY, targetZ) = if (bbox.isEmpty) {
            floatArrayOf(0f, 25f, 0f)
        } else {
            floatArrayOf(
                (bbox.minX + bbox.maxX) * 0.5f,
                (bbox.minY + bbox.maxY) * 0.5f,
                (bbox.minZ + bbox.maxZ) * 0.5f
            )
        }

        val maxDim = if (bbox.isEmpty) {
            50f
        } else {
            maxOf(
                (bbox.maxX - bbox.minX).coerceAtLeast(0f),
                (bbox.maxY - bbox.minY).coerceAtLeast(0f),
                (bbox.maxZ - bbox.minZ).coerceAtLeast(0f)
            ).coerceAtLeast(10f)
        }

        // Distancia para encuadre con FOV vertical ~ 45° y 40% de margen métrico de visualización
        val fitDistance = (maxDim * 1.8f).coerceAtLeast(35f)

        val vectors = when (preset) {
            CameraPreset.ISOMETRIC, CameraPreset.RESET -> {
                CameraVectors(
                    eyeX = targetX + fitDistance * 0.707f,
                    eyeY = targetY + fitDistance * 0.55f,
                    eyeZ = targetZ + fitDistance * 0.707f,
                    upX = 0f, upY = 1f, upZ = 0f
                )
            }
            CameraPreset.FRONT -> {
                CameraVectors(
                    eyeX = targetX,
                    eyeY = targetY,
                    eyeZ = targetZ + fitDistance,
                    upX = 0f, upY = 1f, upZ = 0f
                )
            }
            CameraPreset.SIDE -> {
                CameraVectors(
                    eyeX = targetX + fitDistance,
                    eyeY = targetY,
                    eyeZ = targetZ,
                    upX = 0f, upY = 1f, upZ = 0f
                )
            }
            CameraPreset.TOP -> {
                CameraVectors(
                    eyeX = targetX + 0.001f,
                    eyeY = targetY + fitDistance,
                    eyeZ = targetZ,
                    upX = 0f, upY = 0f, upZ = -1f
                )
            }
        }

        return CameraFraming(
            targetX = targetX,
            targetY = targetY,
            targetZ = targetZ,
            eyeX = vectors.eyeX,
            eyeY = vectors.eyeY,
            eyeZ = vectors.eyeZ,
            upX = vectors.upX,
            upY = vectors.upY,
            upZ = vectors.upZ,
            fitDistanceMm = fitDistance
        )
    }

    private data class CameraVectors(
        val eyeX: Float,
        val eyeY: Float,
        val eyeZ: Float,
        val upX: Float,
        val upY: Float,
        val upZ: Float
    )
}
