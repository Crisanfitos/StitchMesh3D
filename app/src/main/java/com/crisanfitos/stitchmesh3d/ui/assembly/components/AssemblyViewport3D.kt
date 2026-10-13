package com.crisanfitos.stitchmesh3d.ui.assembly.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crisanfitos.stitchmesh3d.ui.assembly.GizmoMode
import com.crisanfitos.stitchmesh3d.ui.theme.CrochetTypography
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshCoralRed
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshNeutralDark
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSageGreen
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceBorder
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceContainer
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshSurfaceHigh
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextPrimary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTextSecondary
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshYarnGold
import kotlin.math.cos
import kotlin.math.sin

/**
 * Viewport interactivo 3D para el ensamblador CAD de piezas amigurumi (SM-055).
 * Renderiza todas las piezas con sus colores de hilado y posiciones relativas,
 * incluye selección directa por toque, gizmo CAD de traslación/rotación y HUD de telemetría.
 */
@Composable
fun AssemblyViewport3D(
    parts: List<SceneGraphItem>,
    selectedPartId: String,
    transformsByPartId: Map<String, PartTransformState>,
    gizmoMode: GizmoMode = GizmoMode.TRANSLATE,
    isWireframe: Boolean,
    showGrid: Boolean,
    orbitYaw: Float,
    orbitPitch: Float,
    zoomScale: Float,
    panOffset: Offset,
    isIsolatedMode: Boolean = false,
    onPartSelected: (partId: String) -> Unit,
    onPositionChange: (partId: String, x: Float, y: Float, z: Float) -> Unit,
    onRotationChange: (partId: String, rotX: Float, rotY: Float, rotZ: Float) -> Unit,
    onGizmoModeChange: (mode: GizmoMode) -> Unit,
    onToggleWireframe: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleIsolatedMode: (() -> Unit)? = null,
    onOrbitRotate: (deltaYaw: Float, deltaPitch: Float) -> Unit,
    onZoomChange: (scaleMultiplier: Float) -> Unit,
    onPanChange: (deltaOffset: Offset) -> Unit,
    onResetCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedPart = parts.find { it.id == selectedPartId }
    val selectedTransform = transformsByPartId[selectedPartId]

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(StitchMeshNeutralDark)
            .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(12.dp))
            .testTag("assembly_viewport_3d")
    ) {
        // Lienzo interactivo principal
        AssemblyInteractiveCanvas(
            parts = parts,
            selectedPartId = selectedPartId,
            transformsByPartId = transformsByPartId,
            gizmoMode = gizmoMode,
            isWireframe = isWireframe,
            showGrid = showGrid,
            isIsolatedMode = isIsolatedMode,
            orbitYaw = orbitYaw,
            orbitPitch = orbitPitch,
            zoomScale = zoomScale,
            panOffset = panOffset,
            onPartSelected = onPartSelected,
            onPositionChange = onPositionChange,
            onRotationChange = onRotationChange,
            onOrbitRotate = onOrbitRotate,
            onZoomChange = onZoomChange,
            onPanChange = onPanChange,
            modifier = Modifier.fillMaxSize()
        )

        // HUD Superior Izquierdo: Selector de Modos de Gizmo CAD
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StitchMeshSurfaceContainer.copy(alpha = 0.92f))
                .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Modo Traslación
            IconButton(
                onClick = { onGizmoModeChange(GizmoMode.TRANSLATE) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (gizmoMode == GizmoMode.TRANSLATE) StitchMeshTerracotta else Color.Transparent
                ),
                modifier = Modifier
                    .size(32.dp)
                    .testTag("gizmo_translate_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.OpenWith,
                    contentDescription = "Mover pieza (Traslación)",
                    tint = if (gizmoMode == GizmoMode.TRANSLATE) Color.White else StitchMeshTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Modo Rotación
            IconButton(
                onClick = { onGizmoModeChange(GizmoMode.ROTATE) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (gizmoMode == GizmoMode.ROTATE) StitchMeshTerracotta else Color.Transparent
                ),
                modifier = Modifier
                    .size(32.dp)
                    .testTag("gizmo_rotate_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.RotateRight,
                    contentDescription = "Rotar pieza",
                    tint = if (gizmoMode == GizmoMode.ROTATE) Color.White else StitchMeshTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Modo Cámara Libre / Sin Gizmo
            IconButton(
                onClick = { onGizmoModeChange(GizmoMode.NONE) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (gizmoMode == GizmoMode.NONE) StitchMeshTerracotta else Color.Transparent
                ),
                modifier = Modifier
                    .size(32.dp)
                    .testTag("gizmo_camera_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = "Cámara orbital libre",
                    tint = if (gizmoMode == GizmoMode.NONE) Color.White else StitchMeshTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // HUD Superior Derecho: Herramientas del Visor CAD
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StitchMeshSurfaceContainer.copy(alpha = 0.92f))
                .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Toggle Cuadrícula CAD
            IconButton(
                onClick = onToggleGrid,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (showGrid) StitchMeshSurfaceHigh else Color.Transparent
                ),
                modifier = Modifier
                    .size(32.dp)
                    .testTag("toggle_grid_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = "Mostrar cuadrícula CAD",
                    tint = if (showGrid) StitchMeshTerracotta else StitchMeshTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Toggle Wireframe
            IconButton(
                onClick = onToggleWireframe,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isWireframe) StitchMeshSurfaceHigh else Color.Transparent
                ),
                modifier = Modifier
                    .size(32.dp)
                    .testTag("toggle_wireframe_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Modo estructura de alambre",
                    tint = if (isWireframe) StitchMeshTerracotta else StitchMeshTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Toggle Modo Aislado vs Ensamblado Global (RF-4.2, SM-033)
            if (onToggleIsolatedMode != null) {
                IconButton(
                    onClick = onToggleIsolatedMode,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isIsolatedMode) StitchMeshSurfaceHigh else Color.Transparent
                    ),
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("toggle_isolated_mode_btn")
                ) {
                    Icon(
                        imageVector = if (isIsolatedMode) Icons.Default.ViewInAr else Icons.Default.Layers,
                        contentDescription = if (isIsolatedMode) "Modo aislado (solo pieza activa)" else "Modo ensamblado global (todas las piezas)",
                        tint = if (isIsolatedMode) StitchMeshTerracotta else StitchMeshTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Reset Cámara
            IconButton(
                onClick = onResetCamera,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("reset_camera_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Restablecer cámara",
                    tint = StitchMeshTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // HUD Inferior: Telemetría de la Pieza Activa
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StitchMeshSurfaceContainer.copy(alpha = 0.92f))
                .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("assembly_telemetry_hud")
        ) {
            if (selectedPart != null && selectedTransform != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(selectedPart.colorHex))
                    )
                    Text(
                        text = selectedPart.name,
                        style = CrochetTypography.tokenBadge,
                        fontWeight = FontWeight.Bold,
                        color = StitchMeshTextPrimary
                    )
                    Text(
                        text = "·",
                        color = StitchMeshTextSecondary
                    )
                    Text(
                        text = "XYZ: (%.1f, %.1f, %.1f) mm".format(
                            selectedTransform.posX,
                            selectedTransform.posY,
                            selectedTransform.posZ
                        ),
                        style = CrochetTypography.tokenBadge,
                        color = StitchMeshTerracotta
                    )
                    Text(
                        text = "·",
                        color = StitchMeshTextSecondary
                    )
                    Text(
                        text = "Rot: (%.0f°, %.0f°, %.0f°)".format(
                            selectedTransform.rotX,
                            selectedTransform.rotY,
                            selectedTransform.rotZ
                        ),
                        style = CrochetTypography.tokenBadge,
                        color = StitchMeshSageGreen
                    )
                }
            } else {
                Text(
                    text = "Toca una pieza para manipularla en el espacio 3D",
                    style = CrochetTypography.tokenBadge,
                    color = StitchMeshTextSecondary
                )
            }
        }
    }
}

/**
 * Lienzo con renderizado proyectivo de las partes del amigurumi y gizmo CAD.
 */
@Composable
private fun AssemblyInteractiveCanvas(
    parts: List<SceneGraphItem>,
    selectedPartId: String,
    transformsByPartId: Map<String, PartTransformState>,
    gizmoMode: GizmoMode,
    isWireframe: Boolean,
    showGrid: Boolean,
    isIsolatedMode: Boolean = false,
    orbitYaw: Float,
    orbitPitch: Float,
    zoomScale: Float,
    panOffset: Offset,
    onPartSelected: (partId: String) -> Unit,
    onPositionChange: (partId: String, x: Float, y: Float, z: Float) -> Unit,
    onRotationChange: (partId: String, rotX: Float, rotY: Float, rotZ: Float) -> Unit,
    onOrbitRotate: (deltaYaw: Float, deltaPitch: Float) -> Unit,
    onZoomChange: (scaleMultiplier: Float) -> Unit,
    onPanChange: (deltaOffset: Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleParts = if (isIsolatedMode) {
        parts.filter { it.id == selectedPartId }
    } else {
        parts.filter { it.isVisible }
    }

    val yawRad = Math.toRadians(orbitYaw.toDouble()).toFloat()
    val pitchRad = Math.toRadians(orbitPitch.toDouble()).toFloat()

    val cosYaw = cos(yawRad)
    val sinYaw = sin(yawRad)
    val cosPitch = cos(pitchRad)
    val sinPitch = sin(pitchRad)

    fun project3DTo2D(xMm: Float, yMm: Float, zMm: Float, centerX: Float, centerY: Float): Offset {
        // Rotación orbital Y (Yaw)
        val rotX = xMm * cosYaw - zMm * sinYaw
        val rotZ = xMm * sinYaw + zMm * cosYaw

        // Rotación orbital X (Pitch)
        val finalY = yMm * cosPitch - rotZ * sinPitch

        // Escala y proyección isométrica en el lienzo
        val screenX = centerX + rotX * 3.2f * zoomScale
        val screenY = centerY - finalY * 3.2f * zoomScale

        return Offset(screenX, screenY)
    }

    Canvas(
        modifier = modifier
            .pointerInput(visibleParts, transformsByPartId, zoomScale, panOffset, orbitYaw, orbitPitch) {
                detectTapGestures { tapOffset ->
                    val centerX = size.width / 2f + panOffset.x
                    val centerY = size.height / 2f + panOffset.y

                    // Búsqueda de impacto en piezas proyectadas (picking por distancia)
                    var tappedPartId: String? = null
                    var minDistance = Float.MAX_VALUE

                    for (part in visibleParts) {
                        val transform = transformsByPartId[part.id] ?: continue
                        val projected = project3DTo2D(transform.posX, transform.posY, transform.posZ, centerX, centerY)
                        val dist = (projected - tapOffset).getDistance()
                        val hitRadius = (28.dp.toPx() * zoomScale).coerceAtLeast(24.dp.toPx())

                        if (dist <= hitRadius && dist < minDistance) {
                            minDistance = dist
                            tappedPartId = part.id
                        }
                    }

                    if (tappedPartId != null) {
                        onPartSelected(tappedPartId)
                    }
                }
            }
            .pointerInput(selectedPartId, gizmoMode, transformsByPartId, zoomScale) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (zoom != 1f) {
                        onZoomChange(zoom)
                    }
                    if (pan != Offset.Zero) {
                        val activeTransform = transformsByPartId[selectedPartId]
                        if (activeTransform != null && !activeTransform.isLocked && gizmoMode == GizmoMode.TRANSLATE) {
                            // Arrastre directo de la pieza en modo traslación
                            val deltaX = pan.x / (3.2f * zoomScale)
                            val deltaY = -pan.y / (3.2f * zoomScale)
                            onPositionChange(
                                selectedPartId,
                                activeTransform.posX + deltaX,
                                activeTransform.posY + deltaY,
                                activeTransform.posZ
                            )
                        } else if (activeTransform != null && !activeTransform.isLocked && gizmoMode == GizmoMode.ROTATE) {
                            // Arrastre en modo rotación
                            val deltaRotZ = pan.x * 0.5f
                            onRotationChange(
                                selectedPartId,
                                activeTransform.rotX,
                                activeTransform.rotY,
                                activeTransform.rotZ + deltaRotZ
                            )
                        } else {
                            // Órbita de cámara cuando no se arrastra pieza activa
                            onOrbitRotate(pan.x * 0.35f, -pan.y * 0.35f)
                        }
                    }
                }
            }
    ) {
        val centerX = size.width / 2f + panOffset.x
        val centerY = size.height / 2f + panOffset.y

        // 1. Cuadrícula de suelo CAD (plano Y = 0)
        if (showGrid) {
            val gridSpacingMm = 20f
            val gridCount = 6
            for (i in -gridCount..gridCount) {
                val start1 = project3DTo2D(i * gridSpacingMm, 0f, -gridCount * gridSpacingMm, centerX, centerY)
                val end1 = project3DTo2D(i * gridSpacingMm, 0f, gridCount * gridSpacingMm, centerX, centerY)
                drawLine(
                    color = StitchMeshSurfaceBorder.copy(alpha = 0.35f),
                    start = start1,
                    end = end1,
                    strokeWidth = 1f
                )

                val start2 = project3DTo2D(-gridCount * gridSpacingMm, 0f, i * gridSpacingMm, centerX, centerY)
                val end2 = project3DTo2D(gridCount * gridSpacingMm, 0f, i * gridSpacingMm, centerX, centerY)
                drawLine(
                    color = StitchMeshSurfaceBorder.copy(alpha = 0.35f),
                    start = start2,
                    end = end2,
                    strokeWidth = 1f
                )
            }
        }

        // 2. Tríada de ejes en el origen (0, 0, 0)
        val originProjected = project3DTo2D(0f, 0f, 0f, centerX, centerY)
        val axisLengthMm = 25f
        val axisX = project3DTo2D(axisLengthMm, 0f, 0f, centerX, centerY)
        val axisY = project3DTo2D(0f, axisLengthMm, 0f, centerX, centerY)
        val axisZ = project3DTo2D(0f, 0f, axisLengthMm, centerX, centerY)

        drawLine(color = StitchMeshCoralRed.copy(alpha = 0.8f), start = originProjected, end = axisX, strokeWidth = 2f)
        drawLine(color = StitchMeshSageGreen.copy(alpha = 0.8f), start = originProjected, end = axisY, strokeWidth = 2f)
        drawLine(color = Color(0xFF3E7BFA).copy(alpha = 0.8f), start = originProjected, end = axisZ, strokeWidth = 2f)

        // 3. Renderizado de cada pieza amigurumi visible
        for (part in visibleParts) {
            val isSelected = part.id == selectedPartId
            val transform = transformsByPartId[part.id] ?: continue
            val partOrigin = project3DTo2D(transform.posX, transform.posY, transform.posZ, centerX, centerY)
            val partColor = parseHexColor(part.colorHex)

            val partRadius = ((part.roundCount * 1.5f + 16f) * zoomScale).coerceAtLeast(18f)

            // Halo y contorno si está seleccionada
            if (isSelected) {
                drawCircle(
                    color = StitchMeshTerracotta.copy(alpha = 0.25f),
                    radius = partRadius + 10f,
                    center = partOrigin
                )
                drawCircle(
                    color = StitchMeshTerracotta,
                    radius = partRadius + 8f,
                    center = partOrigin,
                    style = Stroke(width = 2.5f)
                )
            }

            // Cuerpo de la pieza
            if (isWireframe) {
                drawCircle(
                    color = partColor,
                    radius = partRadius,
                    center = partOrigin,
                    style = Stroke(width = 2f)
                )
                // Anillos de alambre internos
                drawCircle(
                    color = partColor.copy(alpha = 0.5f),
                    radius = partRadius * 0.65f,
                    center = partOrigin,
                    style = Stroke(width = 1.2f)
                )
            } else {
                drawCircle(
                    color = partColor,
                    radius = partRadius,
                    center = partOrigin,
                    style = Fill
                )
                drawCircle(
                    color = StitchMeshSurfaceBorder,
                    radius = partRadius,
                    center = partOrigin,
                    style = Stroke(width = 1.5f)
                )
            }

            // 4. Gizmo CAD 3D sobre la pieza seleccionada
            if (isSelected && gizmoMode != GizmoMode.NONE) {
                drawCadGizmo(
                    origin = partOrigin,
                    mode = gizmoMode,
                    zoomScale = zoomScale
                )
            }
        }
    }
}

/**
 * Dibuja el gizmo de traslación o rotación 3D en las coordenadas de pantalla de la pieza.
 */
private fun DrawScope.drawCadGizmo(
    origin: Offset,
    mode: GizmoMode,
    zoomScale: Float
) {
    val gizmoLength = (46.dp.toPx() * zoomScale).coerceIn(40f, 90f)

    when (mode) {
        GizmoMode.TRANSLATE -> {
            // Eje X (Rojo / Horizontal)
            val xEnd = Offset(origin.x + gizmoLength, origin.y)
            drawLine(color = StitchMeshCoralRed, start = origin, end = xEnd, strokeWidth = 3f)
            drawCircle(color = StitchMeshCoralRed, radius = 5f, center = xEnd)

            // Eje Y (Verde / Vertical arriba)
            val yEnd = Offset(origin.x, origin.y - gizmoLength)
            drawLine(color = StitchMeshSageGreen, start = origin, end = yEnd, strokeWidth = 3f)
            drawCircle(color = StitchMeshSageGreen, radius = 5f, center = yEnd)

            // Eje Z (Azul / Diagonal de profundidad)
            val zEnd = Offset(origin.x - gizmoLength * 0.7f, origin.y + gizmoLength * 0.5f)
            drawLine(color = Color(0xFF3E7BFA), start = origin, end = zEnd, strokeWidth = 3f)
            drawCircle(color = Color(0xFF3E7BFA), radius = 5f, center = zEnd)

            // Núcleo central
            drawCircle(color = StitchMeshYarnGold, radius = 6f, center = origin)
            drawCircle(color = StitchMeshNeutralDark, radius = 3f, center = origin)
        }

        GizmoMode.ROTATE -> {
            // Anillo exterior Roll (Rojo)
            drawCircle(
                color = StitchMeshCoralRed,
                radius = gizmoLength * 0.8f,
                center = origin,
                style = Stroke(width = 2.5f)
            )

            // Anillo medio Pitch (Verde)
            drawOval(
                color = StitchMeshSageGreen,
                topLeft = Offset(origin.x - gizmoLength * 0.8f, origin.y - gizmoLength * 0.35f),
                size = Size(gizmoLength * 1.6f, gizmoLength * 0.7f),
                style = Stroke(width = 2.5f)
            )

            // Anillo interno Yaw (Azul)
            drawOval(
                color = Color(0xFF3E7BFA),
                topLeft = Offset(origin.x - gizmoLength * 0.35f, origin.y - gizmoLength * 0.8f),
                size = Size(gizmoLength * 0.7f, gizmoLength * 1.6f),
                style = Stroke(width = 2.5f)
            )

            // Núcleo central
            drawCircle(color = StitchMeshTerracotta, radius = 5f, center = origin)
        }

        GizmoMode.NONE -> {
            // Sin gizmo
        }
    }
}

/**
 * Parsea un string hexadecimal (#RRGGBB) a Color Compose de forma segura.
 */
private fun parseHexColor(hexString: String): Color {
    return try {
        val clean = hexString.removePrefix("#")
        val colorInt = android.graphics.Color.parseColor("#$clean")
        Color(colorInt)
    } catch (_: Throwable) {
        StitchMeshTerracotta
    }
}
