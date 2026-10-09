package com.crisanfitos.stitchmesh3d.domain.model

/**
 * Topología geométrica de una pieza individual dentro de un proyecto.
 * Determina si la pieza es volumétrica con inflado de relleno de algodón, tubular hueca o planar.
 */
enum class PartTopologyType(
    val code: String,
    val displayName: String,
    val description: String
) {
    CLOSED_FILLED(
        code = "closed_filled",
        displayName = "Cerrada / Rellena",
        description = "Estructura 3D cerrada inflada por relleno de algodón (ej. cabeza, cuerpo)"
    ),
    SEMI_CLOSED_TUBE(
        code = "semi_closed_tube",
        displayName = "Tubular / Abierta",
        description = "Estructura cilíndrica hueca sin relleno interior (ej. patas, cuello, gorro)"
    ),
    FLAT_PANEL(
        code = "flat_panel",
        displayName = "Panel Plano",
        description = "Superficie 2D con espesor de hilado (ej. orejas, alas, prendas, parches)"
    );

    companion object {
        fun fromCode(code: String): PartTopologyType {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: CLOSED_FILLED
        }
    }
}
