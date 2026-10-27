package com.crisanfitos.stitchmesh3d.ui.assembly

import com.crisanfitos.stitchmesh3d.ui.assembly.components.PartTransformState
import com.crisanfitos.stitchmesh3d.ui.assembly.components.SceneGraphItem
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Planificador geométrico de auto-disposición y alineación espacial "Shelf / Side-by-Side"
 * para piezas en el visor de ensamblado 3D (SM-076).
 *
 * Garantiza que:
 * 1. Cuando las piezas de un proyecto no tienen coordenadas guardadas (o cuando todas están apiladas en `(0,0,0)`),
 *    se distribuyen automáticamente en una fila horizontal centrada sobre el eje X (`Y = 0`, `Z = 0`):
 *    `X_i = (i - (N - 1) / 2.0f) * spacingMm`.
 * 2. El espaciado (`spacingMm`) se adapta dinámicamente al radio envolvente de las piezas y a la
 *    **Muestra de tensión** (`stitchWidthMm`) en milímetros reales (mínimo 45.0 mm).
 * 3. Si un ensamblado ya en curso recibe una pieza nueva, esta se posiciona al final de la fila
 *    sin alterar las coordenadas de las piezas ya colocadas.
 */
object AssemblyLayoutPlanner {

    const val MIN_SHELF_SPACING_MM = 45.0f
    const val DEFAULT_STITCH_WIDTH_MM = 3.85f
    private const val MARGIN_MM = 12.0f

    /**
     * Estima el radio envolvente horizontal (en milímetros reales) de una pieza a partir de su
     * recuento de vueltas/puntos y del ancho de punto de la **Muestra de tensión** (`stitchWidthMm`).
     */
    fun estimatePartRadiusMm(
        item: SceneGraphItem,
        stitchWidthMm: Float = DEFAULT_STITCH_WIDTH_MM
    ): Float {
        val effectiveW = stitchWidthMm.coerceAtLeast(1.5f)
        val rounds = item.roundCount.coerceAtLeast(1)
        val avgStitchesPerRound = if (item.stitchCount > 0) {
            (item.stitchCount.toFloat() / rounds.toFloat()).coerceAtLeast(6f)
        } else {
            max(6f, rounds * 3f)
        }
        val circumferenceMm = avgStitchesPerRound * effectiveW
        val radiusFromCircumference = (circumferenceMm / (2.0 * PI)).toFloat()
        val radiusFromRounds = rounds * effectiveW * 0.45f
        return max(radiusFromCircumference, radiusFromRounds).coerceIn(14.0f, 65.0f)
    }

    /**
     * Calcula el espaciado uniforme libre de colisiones entre centros en el eje X (en milímetros)
     * para un conjunto de piezas según la **Muestra de tensión** activa.
     */
    fun computeEffectiveSpacingMm(
        parts: List<SceneGraphItem>,
        stitchWidthMm: Float = DEFAULT_STITCH_WIDTH_MM
    ): Float {
        if (parts.isEmpty()) return MIN_SHELF_SPACING_MM
        val maxRadius = parts.maxOf { estimatePartRadiusMm(it, stitchWidthMm) }
        val diameterPlusMargin = (maxRadius * 2.0f) + MARGIN_MM
        val tensionScale = (stitchWidthMm / DEFAULT_STITCH_WIDTH_MM).coerceIn(0.6f, 2.5f)
        return max(MIN_SHELF_SPACING_MM * tensionScale, diameterPlusMargin)
    }

    /**
     * Calcula el mapa de transformaciones `PartTransformState` aplicando la alineación "Shelf / Side-by-Side".
     *
     * @param parts Lista ordenada de piezas del proyecto en el Scene Graph.
     * @param existingTransforms Transformaciones previas guardadas (si existen).
     * @param forceRealignAll Si es `true`, realinea todas las piezas no bloqueadas en la cuadrícula horizontal.
     * @param stitchWidthMm Ancho de punto en mm según la **Muestra de tensión** del proyecto.
     */
    fun computeShelfLayout(
        parts: List<SceneGraphItem>,
        existingTransforms: Map<String, PartTransformState> = emptyMap(),
        forceRealignAll: Boolean = false,
        stitchWidthMm: Float = DEFAULT_STITCH_WIDTH_MM
    ): Map<String, PartTransformState> {
        if (parts.isEmpty()) return emptyMap()

        val spacing = computeEffectiveSpacingMm(parts, stitchWidthMm)
        val n = parts.size
        val result = LinkedHashMap<String, PartTransformState>(n)

        // Detectar si las transformaciones existentes están todas colapsadas en el origen (0,0,0) cuando N > 1
        val existingForParts = parts.mapNotNull { existingTransforms[it.id] }
        val allStackedAtOrigin = n > 1 && existingForParts.size > 1 && existingForParts.all {
            abs(it.posX) < 1e-3f && abs(it.posY) < 1e-3f && abs(it.posZ) < 1e-3f
        }

        val shouldDeployFullShelf = forceRealignAll || existingForParts.isEmpty() || allStackedAtOrigin

        if (shouldDeployFullShelf) {
            parts.forEachIndexed { index, item ->
                val prev = existingTransforms[item.id]
                val shelfX = (index - (n - 1) / 2.0f) * spacing
                if (prev != null && prev.isLocked && forceRealignAll) {
                    // Respetar piezas bloqueadas durante una realineación manual
                    result[item.id] = prev.copy(partName = item.name)
                } else {
                    result[item.id] = PartTransformState(
                        partId = item.id,
                        partName = item.name,
                        posX = shelfX,
                        posY = 0f,
                        posZ = 0f,
                        rotX = 0f,
                        rotY = 0f,
                        rotZ = 0f,
                        isMirrored = false,
                        isLocked = item.isLocked
                    )
                }
            }
            return result
        }

        // Caso mixto: preservar piezas que ya tienen posición y colocar las piezas nuevas al final de la fila
        var maxOccupiedX = existingForParts.maxOfOrNull { it.posX } ?: 0f
        for (item in parts) {
            val saved = existingTransforms[item.id]
            if (saved != null) {
                result[item.id] = saved.copy(
                    partName = item.name,
                    isLocked = item.isLocked
                )
            } else {
                maxOccupiedX += spacing
                result[item.id] = PartTransformState(
                    partId = item.id,
                    partName = item.name,
                    posX = maxOccupiedX,
                    posY = 0f,
                    posZ = 0f,
                    isLocked = item.isLocked
                )
            }
        }
        return result
    }

    /**
     * Calcula la transformación inicial para una pieza recién añadida en un ensamblado ya activo,
     * posicionándola al final de la fila horizontal sin solaparse con ninguna pieza existente.
     */
    fun computeNextPartTransform(
        newPart: SceneGraphItem,
        existingParts: List<SceneGraphItem>,
        existingTransforms: Map<String, PartTransformState>,
        stitchWidthMm: Float = DEFAULT_STITCH_WIDTH_MM
    ): PartTransformState {
        if (existingParts.isEmpty() || existingTransforms.isEmpty()) {
            return PartTransformState(
                partId = newPart.id,
                partName = newPart.name,
                posX = 0f,
                posY = 0f,
                posZ = 0f,
                isLocked = newPart.isLocked
            )
        }
        val allParts = existingParts + newPart
        val spacing = computeEffectiveSpacingMm(allParts, stitchWidthMm)
        val currentMaxX = existingTransforms.values.maxOfOrNull { it.posX } ?: 0f
        return PartTransformState(
            partId = newPart.id,
            partName = newPart.name,
            posX = currentMaxX + spacing,
            posY = 0f,
            posZ = 0f,
            isLocked = newPart.isLocked
        )
    }

    /**
     * Verifica que ninguna pareja de piezas en `transforms` colisione espacialmente (distancia > thresholdMm).
     */
    fun hasAnySpatialOverlap(
        transforms: Collection<PartTransformState>,
        minSeparationMm: Float = 10.0f
    ): Boolean {
        val list = transforms.toList()
        for (i in 0 until list.size) {
            for (j in (i + 1) until list.size) {
                val a = list[i]
                val b = list[j]
                val dx = a.posX - b.posX
                val dy = a.posY - b.posY
                val dz = a.posZ - b.posZ
                val dist = sqrt(dx * dx + dy * dy + dz * dz)
                if (dist < minSeparationMm) return true
            }
        }
        return false
    }
}
