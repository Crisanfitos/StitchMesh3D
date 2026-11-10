package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.engine.parser.StitchInstance
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Generador matemático de perfiles de anillos 3D no planares con curvatura adaptativa.
 *
 * Implementa estrictamente el modelo geométrico y ecuaciones de TRD §3.1:
 * 1. Ángulo azimutal acumulado:
 *    theta_{k,j} = 2 * PI * (sum_{m=0}^{j-1} w_rel(m) / sum_{m=0}^{M-1} w_rel(m))
 * 2. Perímetro real de la vuelta:
 *    P_k = sum_{j=0}^{M-1} (w_stitch * w_rel(j)) = w_stitch * sum_{j=0}^{M-1} w_rel(j)
 * 3. Radio local:
 *    r_{k,j} = (P_k / (2 * PI)) + (t_stitch * delta_r_rel(j))
 * 4. Altura axial adaptativa con ángulo de conicidad alpha:
 *    z_{k,j} = z_{k-1,j} + (h_stitch * h_rel(j) * cos(alpha_{k,j}))
 *    donde cos(alpha_{k,j}) = delta_h_nom / sqrt(delta_h_nom^2 + delta_r^2)
 * 5. Coordenadas cartesianas y deformación volumétrica:
 *    V_{k,j} = [(r + delta_n) * cos(theta), (r + delta_n) * sin(theta), z]
 */
object RingProfileGenerator {

    private const val TWO_PI = 2.0 * Math.PI

    /**
     * Genera un perfil geométrico de anillo a partir de una lista de [StitchType].
     */
    fun generateRing(
        roundIndex: Int,
        stitches: List<StitchType>,
        gaugeStandard: YarnGaugeStandard,
        previousRing: RingProfile? = null,
        isContinuousSpiral: Boolean = false
    ): RingProfile {
        val instances = stitches.mapIndexed { index, stitch ->
            StitchInstance(stitchType = stitch, indexInRound = index)
        }
        return generateRingFromInstances(roundIndex, instances, gaugeStandard, previousRing, isContinuousSpiral)
    }

    /**
     * Genera un perfil geométrico de anillo a partir de una lista de [StitchInstance].
     *
     * Expande aumentos (producedStitches > 1) en columnas geométricas individuales con ancho unitario,
     * orienta la malla en el estándar Y-up (glTF 2.0 / Filament) y aplica elevación helicoidal progresiva
     * cuando [isContinuousSpiral] es true (SM-083).
     */
    fun generateRingFromInstances(
        roundIndex: Int,
        instances: List<StitchInstance>,
        gaugeStandard: YarnGaugeStandard,
        previousRing: RingProfile? = null,
        isContinuousSpiral: Boolean = false
    ): RingProfile {
        if (instances.isEmpty()) {
            val fallbackY = previousRing?.maxY ?: 0.0
            return RingProfile(
                roundIndex = roundIndex,
                vertices = emptyList(),
                circumferenceMm = 0.0,
                meanRadiusMm = 0.0,
                minY = fallbackY,
                maxY = fallbackY
            )
        }

        val wStitch = gaugeStandard.stitchWidthMm
        val hStitch = gaugeStandard.stitchHeightMm
        val tStitch = gaugeStandard.stitchThicknessMm

        // Expansión anatómica de cada instrucción a sus puntos de corona producidos (SM-083)
        // Ejemplo: V2: 6 aum -> 12 puntos de corona producidos con ancho w_stitch exacto
        val crownPoints = instances.flatMap { inst ->
            val count = maxOf(1, inst.producedStitches)
            if (count > 1) {
                val subWRel = when (inst.stitchType) {
                    is StitchType.HdcIncrease -> 1.1
                    is StitchType.DcIncrease -> 1.2
                    else -> 1.0
                }
                List(count) {
                    CrownPoint(
                        stitchType = inst.stitchType,
                        wRel = subWRel,
                        hRel = inst.hRel,
                        deltaRRel = inst.deltaRRel,
                        normalDisplacement = inst.normalDisplacement,
                        colorHex = inst.colorHex
                    )
                }
            } else {
                listOf(
                    CrownPoint(
                        stitchType = inst.stitchType,
                        wRel = inst.wRel,
                        hRel = inst.hRel,
                        deltaRRel = inst.deltaRRel,
                        normalDisplacement = inst.normalDisplacement,
                        colorHex = inst.colorHex
                    )
                )
            }
        }

        // 1. Suma total de anchos relativos de corona
        val totalWRel = crownPoints.sumOf { it.wRel }.coerceAtLeast(1e-6)

        // 2. Perímetro circunferencial real de la vuelta (P_k)
        val circumferenceMm = crownPoints.sumOf { wStitch * it.wRel }

        // Prefijos acumulados para calcular el ángulo azimutal acumulado theta_{k, j}
        var accumulatedWRel = 0.0
        val vertices = ArrayList<RingVertex>(crownPoints.size)

        var minY = Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        var sumRadius = 0.0

        for (j in crownPoints.indices) {
            val pt = crownPoints[j]

            // 1. Ángulo azimutal acumulado theta_{k, j}
            val theta = TWO_PI * (accumulatedWRel / totalWRel)
            accumulatedWRel += pt.wRel

            // 3. Radio local r_{k, j}
            val baseRadius = circumferenceMm / TWO_PI
            val deltaR = tStitch * pt.deltaRRel
            val localRadius = (baseRadius + deltaR).coerceAtLeast(0.0)
            sumRadius += localRadius

            // 4. Altura axial adaptativa y_{k, j} en eje Y-up con conicidad y elevación helicoidal
            val deltaHNominal = (hStitch * pt.hRel).coerceAtLeast(1e-4)

            val baseY: Double
            if (previousRing != null && previousRing.vertices.isNotEmpty()) {
                val closestPrev = previousRing.findClosestVertex(theta)
                val yPrev = closestPrev?.y ?: previousRing.meanRadiusMm
                val rPrev = closestPrev?.radiusMm ?: 0.0

                val radiusDiff = localRadius - rPrev
                // Factor de conicidad cos(alpha) = delta_h_nom / sqrt(delta_h_nom^2 + delta_r^2)
                val cosAlpha = deltaHNominal / sqrt((deltaHNominal * deltaHNominal) + (radiusDiff * radiusDiff))

                baseY = yPrev + (deltaHNominal * cosAlpha)
            } else {
                // Primer anillo (origen de la labor, ej. Anillo Mágico)
                // Conicidad inicial respecto al polo central (r=0, y=0)
                val cosAlpha = deltaHNominal / sqrt((deltaHNominal * deltaHNominal) + (localRadius * localRadius))
                baseY = deltaHNominal * cosAlpha
            }

            // Elevación helicoidal progresiva en espiral continua (CONTINUOUS_SPIRAL: y + h * j / N)
            val spiralLift = if (isContinuousSpiral && crownPoints.isNotEmpty()) {
                deltaHNominal * (j.toDouble() / crownPoints.size)
            } else {
                0.0
            }
            val y = baseY + spiralLift

            if (y < minY) minY = y
            if (y > maxY) maxY = y

            // 5. Deformación volumétrica 3D y coordenadas cartesianas estándar Y-up (Filament / glTF 2.0)
            // Eje vertical: +Y (axial hacia arriba)
            // Plano transversal/profundidad: XZ (x = r * cos(theta), z = -r * sin(theta))
            val normalBump = pt.normalDisplacement * tStitch
            val effectiveRadius = localRadius + normalBump

            val x = effectiveRadius * cos(theta)
            val z = -effectiveRadius * sin(theta)

            vertices.add(
                RingVertex(
                    index = j,
                    theta = theta,
                    radiusMm = localRadius,
                    x = x,
                    y = y,
                    z = z,
                    stitchType = pt.stitchType,
                    normalBumpMm = normalBump,
                    colorHex = pt.colorHex
                )
            )
        }

        val meanRadius = if (vertices.isNotEmpty()) sumRadius / vertices.size else 0.0

        return RingProfile(
            roundIndex = roundIndex,
            vertices = vertices,
            circumferenceMm = circumferenceMm,
            meanRadiusMm = meanRadius,
            minY = if (vertices.isEmpty()) 0.0 else minY,
            maxY = if (vertices.isEmpty()) 0.0 else maxY
        )
    }

    private data class CrownPoint(
        val stitchType: StitchType,
        val wRel: Double,
        val hRel: Double,
        val deltaRRel: Double,
        val normalDisplacement: Double,
        val colorHex: String?
    )
}
