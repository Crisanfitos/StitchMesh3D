package com.crisanfitos.stitchmesh3d.ui.assembly.components

import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart

/**
 * Estado inmutable de transformación espacial (posición en milímetros y rotación en grados)
 * para una pieza del modelo dentro del ensamblaje 3D (SM-054).
 */
data class PartTransformState(
    val partId: String,
    val partName: String,
    val posX: Float = 0f, // milímetros
    val posY: Float = 0f, // milímetros
    val posZ: Float = 0f, // milímetros
    val rotX: Float = 0f, // grados sexagesimales (Roll)
    val rotY: Float = 0f, // grados sexagesimales (Pitch)
    val rotZ: Float = 0f, // grados sexagesimales (Yaw)
    val isMirrored: Boolean = false,
    val isLocked: Boolean = false
) {
    /**
     * Devuelve una nueva copia de la transformación reseteada a las coordenadas de origen (0, 0, 0).
     */
    fun resetToOrigin(): PartTransformState {
        return copy(
            posX = 0f,
            posY = 0f,
            posZ = 0f,
            rotX = 0f,
            rotY = 0f,
            rotZ = 0f,
            isMirrored = false
        )
    }

    /**
     * Aplica o invierte la simetría sagital (espejo en plano sagital X = 0).
     * Refleja el eje transversal X y los ángulos de orientación horizontal.
     */
    fun mirrorSagittal(): PartTransformState {
        return copy(
            posX = -posX,
            rotY = -rotY,
            rotZ = -rotZ,
            isMirrored = !isMirrored
        )
    }
}

/**
 * Convierte un ProjectPart de dominio a su estado de transformación para el inspector.
 */
fun ProjectPart.toPartTransformState(isLocked: Boolean = false): PartTransformState {
    return PartTransformState(
        partId = id,
        partName = name,
        posX = transformPositionX,
        posY = transformPositionY,
        posZ = transformPositionZ,
        rotX = transformRotationX,
        rotY = transformRotationY,
        rotZ = transformRotationZ,
        isMirrored = false,
        isLocked = isLocked
    )
}
