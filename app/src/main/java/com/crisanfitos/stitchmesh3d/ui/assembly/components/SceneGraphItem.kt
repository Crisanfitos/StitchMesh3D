package com.crisanfitos.stitchmesh3d.ui.assembly.components

import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType

/**
 * Modelo de datos inmutable para un nodo del árbol Scene Graph de piezas amigurumi.
 * Representa la jerarquía, estado de visibilidad, bloqueo y aspecto cromático en la escena CAD.
 */
data class SceneGraphItem(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val depth: Int = 0,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val colorHex: String = "#E06D53",
    val roundCount: Int = 0,
    val stitchCount: Int = 0,
    val topologyType: PartTopologyType = PartTopologyType.CLOSED_FILLED,
    val hasChildren: Boolean = false,
    val isExpanded: Boolean = true
)
