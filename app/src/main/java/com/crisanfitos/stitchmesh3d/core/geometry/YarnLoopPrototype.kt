package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.engine.model.TopologyFlag

/**
 * Categorías principales de síntesis geométrica de bucle de hilo volumétrico 3D (SM-086).
 */
enum class YarnLoopCategory {
    SINGLE_CROCHET,
    DOUBLE_CROCHET,
    HALF_DOUBLE_CROCHET,
    CHAIN,
    BLO,
    BOBBLE;

    companion object {
        /**
         * Mapea un [StitchType] cualquiera a su categoría representativa de prototipo tubular.
         */
        fun fromStitchType(stitchType: StitchType): YarnLoopCategory {
            if (stitchType.topologyFlags.contains(TopologyFlag.BLO)) {
                return BLO
            }
            return when (stitchType) {
                is StitchType.Chain -> CHAIN
                is StitchType.DoubleCrochet,
                is StitchType.TrebleCrochet,
                is StitchType.DoubleTrebleCrochet,
                is StitchType.DcIncrease,
                is StitchType.DcDecrease -> DOUBLE_CROCHET
                is StitchType.HalfDoubleCrochet,
                is StitchType.HdcIncrease,
                is StitchType.HdcDecrease,
                is StitchType.FrontPostHalfDoubleCrochet,
                is StitchType.BackPostHalfDoubleCrochet -> HALF_DOUBLE_CROCHET
                is StitchType.BobbleStitch,
                is StitchType.PopcornStitch -> BOBBLE
                else -> SINGLE_CROCHET
            }
        }
    }
}

/**
 * Representa una primitiva tubular maestra de bucle de hilo 3D (SM-086).
 *
 * @property category Categoría de puntada asociada.
 * @property lodLevel Nivel de detalle (0 = LOD0 ~48-72 tris, 1 = LOD1 ~24 tris).
 * @property mesh Malla 3D normalizada con normales unitarias estrictas (||n|| = 1.0).
 */
data class YarnLoopPrototype(
    val category: YarnLoopCategory,
    val lodLevel: Int,
    val mesh: MeshGeometry
) {
    val triangleCount: Int get() = mesh.triangleCount
    val vertexCount: Int get() = mesh.vertexCount
}
