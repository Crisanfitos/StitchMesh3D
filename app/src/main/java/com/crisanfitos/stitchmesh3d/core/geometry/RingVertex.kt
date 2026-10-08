package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType

/**
 * Representa un vértice geométrico 3D generado a partir de una puntada en un anillo de crochet.
 *
 * Implementa formalmente las especificaciones de TRD §3.1:
 * @property index Índice ordinal base cero de la puntada en el anillo.
 * @property theta Ángulo azimutal acumulado en radianes en el rango [0, 2pi].
 * @property radiusMm Radio local en milímetros r_k,j.
 * @property x Coordenada cartesiana X en milímetros (r * cos(theta)).
 * @property y Coordenada cartesiana Y en milímetros (r * sin(theta)).
 * @property z Coordenada cartesiana Z en milímetros (altura axial adaptativa con conicidad).
 * @property stitchType Tipo de puntada y su 6-tupla asociada.
 * @property normalBumpMm Deformación volumétrica aplicada en la normal exterior (bobble/popcorn/puff).
 */
data class RingVertex(
    val index: Int,
    val theta: Double,
    val radiusMm: Double,
    val x: Double,
    val y: Double,
    val z: Double,
    val stitchType: StitchType,
    val normalBumpMm: Double = 0.0
)
