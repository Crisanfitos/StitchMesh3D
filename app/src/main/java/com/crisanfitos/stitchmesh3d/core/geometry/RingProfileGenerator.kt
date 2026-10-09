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
        previousRing: RingProfile? = null
    ): RingProfile {
        val instances = stitches.mapIndexed { index, stitch ->
            StitchInstance(stitchType = stitch, indexInRound = index)
        }
        return generateRingFromInstances(roundIndex, instances, gaugeStandard, previousRing)
    }

    /**
     * Genera un perfil geométrico de anillo a partir de una lista de [StitchInstance].
     */
    fun generateRingFromInstances(
        roundIndex: Int,
        instances: List<StitchInstance>,
        gaugeStandard: YarnGaugeStandard,
        previousRing: RingProfile? = null
    ): RingProfile {
        if (instances.isEmpty()) {
            return RingProfile(
                roundIndex = roundIndex,
                vertices = emptyList(),
                circumferenceMm = 0.0,
                meanRadiusMm = 0.0,
                minZ = previousRing?.maxZ ?: 0.0,
                maxZ = previousRing?.maxZ ?: 0.0
            )
        }

        val wStitch = gaugeStandard.stitchWidthMm
        val hStitch = gaugeStandard.stitchHeightMm
        val tStitch = gaugeStandard.stitchThicknessMm

        // 1. Suma total de anchos relativos
        val totalWRel = instances.sumOf { it.wRel }.coerceAtLeast(1e-6)

        // 2. Perímetro circunferencial real de la vuelta (P_k)
        val circumferenceMm = instances.sumOf { wStitch * it.wRel }

        // Prefijos acumulados para calcular el ángulo azimutal acumulado theta_{k, j}
        var accumulatedWRel = 0.0
        val vertices = ArrayList<RingVertex>(instances.size)

        var minZ = Double.MAX_VALUE
        var maxZ = -Double.MAX_VALUE
        var sumRadius = 0.0

        for (j in instances.indices) {
            val inst = instances[j]

            // 1. Ángulo azimutal acumulado theta_{k, j}
            val theta = TWO_PI * (accumulatedWRel / totalWRel)
            accumulatedWRel += inst.wRel

            // 3. Radio local r_{k, j}
            val baseRadius = circumferenceMm / TWO_PI
            val deltaR = tStitch * inst.deltaRRel
            val localRadius = (baseRadius + deltaR).coerceAtLeast(0.0)
            sumRadius += localRadius

            // 4. Altura axial adaptativa z_{k, j} con conicidad
            val deltaHNominal = (hStitch * inst.hRel).coerceAtLeast(1e-4)

            val z: Double
            if (previousRing != null && previousRing.vertices.isNotEmpty()) {
                val closestPrev = previousRing.findClosestVertex(theta)
                val zPrev = closestPrev?.z ?: previousRing.meanRadiusMm
                val rPrev = closestPrev?.radiusMm ?: 0.0

                val radiusDiff = localRadius - rPrev
                // Factor de conicidad cos(alpha) = delta_h_nom / sqrt(delta_h_nom^2 + delta_r^2)
                val cosAlpha = deltaHNominal / sqrt((deltaHNominal * deltaHNominal) + (radiusDiff * radiusDiff))

                z = zPrev + (deltaHNominal * cosAlpha)
            } else {
                // Primer anillo (origen de la labor, ej. Anillo Mágico)
                // Conicidad inicial respecto al polo central (r=0, z=0)
                val cosAlpha = deltaHNominal / sqrt((deltaHNominal * deltaHNominal) + (localRadius * localRadius))
                z = deltaHNominal * cosAlpha
            }

            if (z < minZ) minZ = z
            if (z > maxZ) maxZ = z

            // 5. Deformación volumétrica 3D (bobble, popcorn, puff)
            val normalBump = inst.normalDisplacement * tStitch
            val effectiveRadius = localRadius + normalBump

            val x = effectiveRadius * cos(theta)
            val y = effectiveRadius * sin(theta)

            vertices.add(
                RingVertex(
                    index = j,
                    theta = theta,
                    radiusMm = localRadius,
                    x = x,
                    y = y,
                    z = z,
                    stitchType = inst.stitchType,
                    normalBumpMm = normalBump,
                    colorHex = inst.colorHex
                )
            )
        }

        val meanRadius = if (vertices.isNotEmpty()) sumRadius / vertices.size else 0.0

        return RingProfile(
            roundIndex = roundIndex,
            vertices = vertices,
            circumferenceMm = circumferenceMm,
            meanRadiusMm = meanRadius,
            minZ = if (vertices.isEmpty()) 0.0 else minZ,
            maxZ = if (vertices.isEmpty()) 0.0 else maxZ
        )
    }
}
