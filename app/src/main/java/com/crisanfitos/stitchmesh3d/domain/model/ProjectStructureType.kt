package com.crisanfitos.stitchmesh3d.domain.model

/**
 * Define la estructura global del proyecto de crochet.
 * Condiciona la tipología de las piezas por defecto y el pipeline de renderizado y simulación.
 */
enum class ProjectStructureType(
    val code: String,
    val displayName: String,
    val description: String,
    val defaultTopology: PartTopologyType
) {
    AMIGURUMI_3D(
        code = "amigurumi_3d",
        displayName = "Amigurumi (3D)",
        description = "Piezas volumétricas cerradas infladas por relleno de algodón",
        defaultTopology = PartTopologyType.CLOSED_FILLED
    ),
    FLAT_GARMENT(
        code = "flat_garment",
        displayName = "Prenda Plana",
        description = "Tejido planar bidimensional en hileras o paneles (ej. bufanda, manta)",
        defaultTopology = PartTopologyType.FLAT_PANEL
    ),
    GRANNY_SQUARE(
        code = "granny_square",
        displayName = "Granny Square",
        description = "Motivos concéntricos poligonales planos",
        defaultTopology = PartTopologyType.FLAT_PANEL
    ),
    ACCESSORY(
        code = "accessory",
        displayName = "Accesorio",
        description = "Estructuras tubulares huecas o sin relleno (gorros, cestas, bolsos)",
        defaultTopology = PartTopologyType.SEMI_CLOSED_TUBE
    );

    companion object {
        fun fromCode(code: String): ProjectStructureType {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: AMIGURUMI_3D
        }
    }
}
