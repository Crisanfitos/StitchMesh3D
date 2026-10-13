package com.crisanfitos.stitchmesh3d.ui.assembly.components

import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import com.crisanfitos.stitchmesh3d.ui.workspace.ProjectPartUiModel

/**
 * Mappers para convertir modelos de dominio y UI a nodos del Scene Graph.
 */
fun ProjectPart.toSceneGraphItem(
    depth: Int = 0,
    isVisible: Boolean = true,
    isLocked: Boolean = false,
    colorHex: String = "#E06D53",
    hasChildren: Boolean = false,
    isExpanded: Boolean = true
): SceneGraphItem {
    return SceneGraphItem(
        id = id,
        name = name,
        parentId = null,
        depth = depth,
        isVisible = isVisible,
        isLocked = isLocked,
        colorHex = colorHex,
        roundCount = rounds.size,
        stitchCount = rounds.sumOf { it.producedStitches },
        topologyType = topologyType,
        hasChildren = hasChildren,
        isExpanded = isExpanded
    )
}

fun ProjectPartUiModel.toSceneGraphItem(
    depth: Int = 0,
    isVisible: Boolean = true,
    isLocked: Boolean = false,
    hasChildren: Boolean = false,
    isExpanded: Boolean = true
): SceneGraphItem {
    return SceneGraphItem(
        id = id,
        name = name,
        parentId = null,
        depth = depth,
        isVisible = isVisible,
        isLocked = isLocked,
        colorHex = colorHex,
        roundCount = roundCount,
        stitchCount = 0,
        topologyType = topologyType,
        hasChildren = hasChildren,
        isExpanded = isExpanded
    )
}
