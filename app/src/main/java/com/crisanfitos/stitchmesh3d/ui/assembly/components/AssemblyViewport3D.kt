package com.crisanfitos.stitchmesh3d.ui.assembly.components

import android.annotation.SuppressLint
import android.view.Choreographer
import android.view.SurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.crisanfitos.stitchmesh3d.core.geometry.BoundingBox3D
import com.crisanfitos.stitchmesh3d.core.geometry.ColorSpaceHelper
import com.crisanfitos.stitchmesh3d.core.geometry.GlbMeshBuilder
import com.crisanfitos.stitchmesh3d.core.geometry.MeshGeometry
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
import com.crisanfitos.stitchmesh3d.ui.viewport.components.MetricCameraFramingHelper
import com.crisanfitos.stitchmesh3d.ui.viewport.components.StudioLightingConfig
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Skybox
import com.google.android.filament.View
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.ModelViewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.sin

/**
 * Viewport interactivo 3D para el ensamblador CAD de piezas amigurumi (SM-055, SM-087).
 *
 * Renderiza todas las piezas activas en una escena 3D multi-nodo real sobre Google Filament con:
 * - Geometría paramétrica real de cada pieza en milímetros con sus colores de hilado y texturas PBR textiles.
 * - Sombreado fotométrico calibrado (StudioLightingConfig.CALIBRATED_TEXTILE), SSAO y fondo lineal `#121316`.
 * - Selección directa en 3D por raycasting Möller-Trumbore (RF-4.1).
 * - Manipulación interactiva mediante Gizmo CAD de traslación, rotación de Euler y anclaje superficial (Surface Snap).
 * - Ciclo de vida robusto e idempotente libre de fugas nativas o cierres inesperados.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun AssemblyViewport3D(
    parts: List<SceneGraphItem>,
    selectedPartId: String,
    transformsByPartId: Map<String, PartTransformState>,
    meshesByPartId: Map<String, MeshGeometry> = emptyMap(),
    gizmoMode: GizmoMode = GizmoMode.TRANSLATE,
    isWireframe: Boolean = false,
    showGrid: Boolean = true,
    orbitYaw: Float = 45f,
    orbitPitch: Float = 30f,
    zoomScale: Float = 1.0f,
    panOffset: Offset = Offset.Zero,
    isIsolatedMode: Boolean = false,
    onPartSelected: (partId: String) -> Unit,
    onPositionChange: (partId: String, x: Float, y: Float, z: Float) -> Unit,
    onRotationChange: (partId: String, rotX: Float, rotY: Float, rotZ: Float) -> Unit,
    onGizmoModeChange: (mode: GizmoMode) -> Unit,
    onToggleWireframe: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleIsolatedMode: (() -> Unit)? = null,
    onSurfaceSnap: ((sourcePartId: String, px: Float, py: Float, pz: Float, nx: Float, ny: Float, nz: Float) -> Unit)? = null,
    onOrbitRotate: (deltaYaw: Float, deltaPitch: Float) -> Unit,
    onZoomChange: (scaleMultiplier: Float) -> Unit,
    onPanChange: (deltaOffset: Offset) -> Unit,
    onResetCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedPart = parts.find { it.id == selectedPartId }
    val selectedTransform = transformsByPartId[selectedPartId]

    val modelViewerRef = remember { arrayOfNulls<ModelViewer>(1) }
    val manipulatorRef = remember { arrayOfNulls<Manipulator>(1) }
    val surfaceViewRef = remember { arrayOfNulls<SurfaceView>(1) }
    val choreographerCallbackRef = remember { arrayOfNulls<Choreographer.FrameCallback>(1) }
    val isReleasedRef = remember { booleanArrayOf(false) }

    // Generar geometría ensamblada en segundo plano (Dispatchers.Default, RF-3.5, RND-2)
    var glbBuffer by remember { mutableStateOf<ByteBuffer?>(null) }
    var combinedSceneMesh by remember { mutableStateOf<MeshGeometry?>(null) }

    LaunchedEffect(parts, transformsByPartId, meshesByPartId, isIsolatedMode, selectedPartId, isWireframe) {
        val combined = withContext(Dispatchers.Default) {
            AssemblyMeshCombiner.combineParts(
                parts = parts,
                transformsByPartId = transformsByPartId,
                meshesByPartId = meshesByPartId,
                isIsolatedMode = isIsolatedMode,
                selectedPartId = selectedPartId
            )
        }
        combinedSceneMesh = combined

        if (combined.vertexCount > 0 && combined.indices.isNotEmpty()) {
            val buffer = withContext(Dispatchers.Default) {
                GlbMeshBuilder.buildGlb(
                    mesh = combined,
                    isWireframe = isWireframe
                )
            }
            glbBuffer = buffer
        } else {
            glbBuffer = null
        }
    }

    LaunchedEffect(glbBuffer) {
        val viewer = modelViewerRef[0] ?: return@LaunchedEffect
        val manipulator = manipulatorRef[0]
        val currentGlb = glbBuffer
        if (currentGlb != null) {
            try {
                currentGlb.rewind()
                val currentCameraBookmark = try { manipulator?.currentBookmark } catch (_: Throwable) { null }

                try {
                    viewer.destroyModel()
                } catch (_: Throwable) {
                    viewer.asset?.let { asset ->
                        try { viewer.scene.removeEntities(asset.entities) } catch (_: Throwable) {}
                        try { viewer.scene.removeEntities(asset.lightEntities) } catch (_: Throwable) {}
                    }
                }

                viewer.loadModelGlb(currentGlb)

                if (currentCameraBookmark != null) {
                    try {
                        manipulator?.jumpToBookmark(currentCameraBookmark)
                    } catch (_: Throwable) {}
                } else {
                    combinedSceneMesh?.let { mesh ->
                        applyAssemblyCamera(viewer, surfaceViewRef[0], mesh, manipulatorRef)
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        } else {
            try {
                viewer.destroyModel()
            } catch (_: Throwable) {
                viewer.asset?.let { asset ->
                    try { viewer.scene.removeEntities(asset.entities) } catch (_: Throwable) {}
                    try { viewer.scene.removeEntities(asset.lightEntities) } catch (_: Throwable) {}
                }
            }
        }
    }

    val performTeardown = remember {
        {
            if (!isReleasedRef[0]) {
                isReleasedRef[0] = true
                try {
                    choreographerCallbackRef[0]?.let { cb ->
                        Choreographer.getInstance().removeFrameCallback(cb)
                    }
                } catch (_: Throwable) {}

                val viewer = modelViewerRef[0]
                if (viewer != null) {
                    try {
                        viewer.destroyModel()
                    } catch (_: Throwable) {}
                }
                modelViewerRef[0] = null
                manipulatorRef[0] = null
                surfaceViewRef[0] = null
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            performTeardown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(StitchMeshNeutralDark)
            .border(1.dp, StitchMeshSurfaceBorder, RoundedCornerShape(12.dp))
            .testTag("assembly_viewport_3d")
    ) {
        // 1. Visor Nativo 3D de Google Filament
        AndroidView(
            factory = { ctx ->
                val surfaceView = SurfaceView(ctx)
                surfaceViewRef[0] = surfaceView
                try {
                    val viewer = ModelViewer(surfaceView = surfaceView)
                    modelViewerRef[0] = viewer

                    // Remover luz solar por defecto de 100k lux de ModelViewer para evitar sobreexposición (SM-085)
                    try {
                        viewer.scene.removeEntity(viewer.light)
                    } catch (_: Throwable) {}

                    // Fondo exacto #121316 con conversión lineal IEC 61966-2-1
                    val linearBg = ColorSpaceHelper.hexToLinearRgba("#121316")
                    viewer.scene.skybox = Skybox.Builder()
                        .color(linearBg[0], linearBg[1], linearBg[2], linearBg[3])
                        .build(viewer.engine)

                    // Rig de iluminación de estudio fotográfico textil calibrado
                    val lightEntities = IntArray(4)
                    val r = StudioLightingConfig.CALIBRATED_TEXTILE
                    val directionalSpecs = r.directionalLights

                    for (i in directionalSpecs.indices) {
                        if (i >= lightEntities.size) break
                        val spec = directionalSpecs[i]
                        val entity = EntityManager.get().create()
                        lightEntities[i] = entity
                        LightManager.Builder(LightManager.Type.DIRECTIONAL)
                            .color(spec.colorR, spec.colorG, spec.colorB)
                            .intensity(spec.intensityLux)
                            .direction(spec.dirX, spec.dirY, spec.dirZ)
                            .castShadows(spec.castShadows)
                            .build(viewer.engine, entity)
                        viewer.scene.addEntity(entity)
                    }

                    // SSAO y sombras suaves PCF
                    try {
                        viewer.view.setShadowingEnabled(true)
                        viewer.view.setShadowType(View.ShadowType.PCF)
                        viewer.view.ambientOcclusionOptions = View.AmbientOcclusionOptions().apply {
                            enabled = true
                            intensity = 1.2f
                            radius = 0.25f
                            bentNormals = true
                        }
                    } catch (_: Throwable) {}

                    val indirectLight = IndirectLight.Builder()
                        .irradiance(
                            r.sphericalHarmonicsBands,
                            r.sphericalHarmonicsIrradiance
                        )
                        .intensity(r.indirectLightIntensityLux)
                        .build(viewer.engine)
                    viewer.scene.indirectLight = indirectLight

                    val choreographer = Choreographer.getInstance()
                    val frameCallback = object : Choreographer.FrameCallback {
                        override fun doFrame(frameTimeNanos: Long) {
                            if (!isReleasedRef[0]) {
                                try {
                                    viewer.render(frameTimeNanos)
                                    choreographer.postFrameCallback(this)
                                } catch (_: Throwable) {}
                            }
                        }
                    }
                    choreographerCallbackRef[0] = frameCallback
                    choreographer.postFrameCallback(frameCallback)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
                surfaceView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Capa interactiva de toques, raycasting 3D y cuadrícula/gizmo CAD
        AssemblyGestureAndGizmoLayer(
            parts = parts,
            selectedPartId = selectedPartId,
            transformsByPartId = transformsByPartId,
            meshesByPartId = meshesByPartId,
            gizmoMode = gizmoMode,
            showGrid = showGrid,
            isIsolatedMode = isIsolatedMode,
            orbitYaw = orbitYaw,
            orbitPitch = orbitPitch,
            zoomScale = zoomScale,
            panOffset = panOffset,
            onPartSelected = onPartSelected,
            onPositionChange = onPositionChange,
            onRotationChange = onRotationChange,
            onSurfaceSnap = onSurfaceSnap,
            onOrbitRotate = onOrbitRotate,
            onZoomChange = onZoomChange,
            onPanChange = onPanChange,
            modifier = Modifier.fillMaxSize()
        )

        // 3. HUD Superior Izquierdo: Selector de Modos de Gizmo CAD
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

            IconButton(
                onClick = { onGizmoModeChange(GizmoMode.SNAP) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (gizmoMode == GizmoMode.SNAP) StitchMeshTerracotta else Color.Transparent
                ),
                modifier = Modifier
                    .size(32.dp)
                    .testTag("gizmo_snap_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "Anclar sobre superficie (Snapping)",
                    tint = if (gizmoMode == GizmoMode.SNAP) Color.White else StitchMeshTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

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

        // 4. HUD Superior Derecho: Herramientas del Visor CAD
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

        // 5. HUD Inferior: Telemetría de la Pieza Activa
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
 * Aplica el encuadre métrico orbital al visor Filament de ensamblado.
 */
private fun applyAssemblyCamera(
    viewer: ModelViewer,
    surfaceView: SurfaceView?,
    mesh: MeshGeometry,
    manipulatorRef: Array<Manipulator?>
) {
    val width = surfaceView?.width ?: 1920
    val height = surfaceView?.height ?: 1080
    val aspect = (width.toFloat() / height.toFloat().coerceAtLeast(1f)).coerceAtLeast(0.1f)

    var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE

    for (i in 0 until mesh.vertexCount) {
        val x = mesh.vertexPositions[i * 3]
        val y = mesh.vertexPositions[i * 3 + 1]
        val z = mesh.vertexPositions[i * 3 + 2]
        if (x < minX) minX = x; if (y < minY) minY = y; if (z < minZ) minZ = z
        if (x > maxX) maxX = x; if (y > maxY) maxY = y; if (z > maxZ) maxZ = z
    }

    val bbox = if (mesh.vertexCount > 0) {
        BoundingBox3D(minX, minY, minZ, maxX, maxY, maxZ)
    } else {
        BoundingBox3D(-50f, 0f, -50f, 50f, 100f, 50f)
    }

    val framing = MetricCameraFramingHelper.calculateFraming(
        bbox = bbox,
        preset = com.crisanfitos.stitchmesh3d.ui.viewport.components.CameraPreset.ISOMETRIC
    )

    try {
        viewer.camera.setLensProjection(28.0, aspect.toDouble(), 0.1, 5000.0)
        viewer.camera.lookAt(
            framing.eyeX.toDouble(), framing.eyeY.toDouble(), framing.eyeZ.toDouble(),
            framing.targetX.toDouble(), framing.targetY.toDouble(), framing.targetZ.toDouble(),
            framing.upX.toDouble(), framing.upY.toDouble(), framing.upZ.toDouble()
        )
    } catch (_: Throwable) {}
}

/**
 * Capa de gestos Compose que gestiona selección por raycasting 3D, arrastre TRS y trazado del Gizmo CAD.
 */
@Composable
private fun AssemblyGestureAndGizmoLayer(
    parts: List<SceneGraphItem>,
    selectedPartId: String,
    transformsByPartId: Map<String, PartTransformState>,
    meshesByPartId: Map<String, MeshGeometry>,
    gizmoMode: GizmoMode,
    showGrid: Boolean,
    isIsolatedMode: Boolean,
    orbitYaw: Float,
    orbitPitch: Float,
    zoomScale: Float,
    panOffset: Offset,
    onPartSelected: (partId: String) -> Unit,
    onPositionChange: (partId: String, x: Float, y: Float, z: Float) -> Unit,
    onRotationChange: (partId: String, rotX: Float, rotY: Float, rotZ: Float) -> Unit,
    onSurfaceSnap: ((sourcePartId: String, px: Float, py: Float, pz: Float, nx: Float, ny: Float, nz: Float) -> Unit)?,
    onOrbitRotate: (deltaYaw: Float, deltaPitch: Float) -> Unit,
    onZoomChange: (scaleMultiplier: Float) -> Unit,
    onPanChange: (deltaOffset: Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val yawRad = Math.toRadians(orbitYaw.toDouble()).toFloat()
    val pitchRad = Math.toRadians(orbitPitch.toDouble()).toFloat()

    val cosYaw = cos(yawRad)
    val sinYaw = sin(yawRad)
    val cosPitch = cos(pitchRad)
    val sinPitch = sin(pitchRad)

    fun project3DTo2D(xMm: Float, yMm: Float, zMm: Float, centerX: Float, centerY: Float): Offset {
        val rotX = xMm * cosYaw - zMm * sinYaw
        val rotZ = xMm * sinYaw + zMm * cosYaw
        val finalY = yMm * cosPitch - rotZ * sinPitch
        val screenX = centerX + rotX * 3.2f * zoomScale
        val screenY = centerY - finalY * 3.2f * zoomScale
        return Offset(screenX, screenY)
    }

    Canvas(
        modifier = modifier
            .pointerInput(parts, transformsByPartId, meshesByPartId, zoomScale, panOffset, orbitYaw, orbitPitch, gizmoMode, selectedPartId) {
                detectTapGestures { tapOffset ->
                    // 1. Raycasting Möller-Trumbore para selección o anclaje superficial (SM-087)
                    if (gizmoMode == GizmoMode.SNAP && selectedPartId.isNotBlank() && onSurfaceSnap != null) {
                        val snapResult = AssemblyRaycaster.pickPart(
                            screenX = tapOffset.x,
                            screenY = tapOffset.y,
                            viewportWidth = size.width.toFloat(),
                            viewportHeight = size.height.toFloat(),
                            orbitYawDeg = orbitYaw,
                            orbitPitchDeg = orbitPitch,
                            zoomScale = zoomScale,
                            panOffset = panOffset,
                            parts = parts,
                            transformsByPartId = transformsByPartId,
                            meshesByPartId = meshesByPartId,
                            isIsolatedMode = isIsolatedMode,
                            selectedPartId = selectedPartId,
                            excludedPartId = selectedPartId
                        )
                        if (snapResult != null) {
                            onSurfaceSnap(
                                selectedPartId,
                                snapResult.hit.point[0],
                                snapResult.hit.point[1],
                                snapResult.hit.point[2],
                                snapResult.hit.normal[0],
                                snapResult.hit.normal[1],
                                snapResult.hit.normal[2]
                            )
                            return@detectTapGestures
                        }
                    }

                    // 2. Selección directa de pieza por raycasting 3D
                    val pickResult = AssemblyRaycaster.pickPart(
                        screenX = tapOffset.x,
                        screenY = tapOffset.y,
                        viewportWidth = size.width.toFloat(),
                        viewportHeight = size.height.toFloat(),
                        orbitYawDeg = orbitYaw,
                        orbitPitchDeg = orbitPitch,
                        zoomScale = zoomScale,
                        panOffset = panOffset,
                        parts = parts,
                        transformsByPartId = transformsByPartId,
                        meshesByPartId = meshesByPartId,
                        isIsolatedMode = isIsolatedMode,
                        selectedPartId = selectedPartId
                    )

                    if (pickResult != null) {
                        onPartSelected(pickResult.partId)
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
                            val deltaX = pan.x / (3.2f * zoomScale)
                            val deltaY = -pan.y / (3.2f * zoomScale)
                            onPositionChange(
                                selectedPartId,
                                activeTransform.posX + deltaX,
                                activeTransform.posY + deltaY,
                                activeTransform.posZ
                            )
                        } else if (activeTransform != null && !activeTransform.isLocked && gizmoMode == GizmoMode.ROTATE) {
                            val deltaRotZ = pan.x * 0.5f
                            onRotationChange(
                                selectedPartId,
                                activeTransform.rotX,
                                activeTransform.rotY,
                                activeTransform.rotZ + deltaRotZ
                            )
                        } else {
                            onOrbitRotate(pan.x * 0.35f, -pan.y * 0.35f)
                        }
                    }
                }
            }
    ) {
        val centerX = size.width / 2f + panOffset.x
        val centerY = size.height / 2f + panOffset.y

        // 1. Cuadrícula métrica de suelo CAD (plano Y = 0)
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

        // 3. Gizmo CAD sobre la pieza activa seleccionada
        val selectedTransform = transformsByPartId[selectedPartId]
        if (selectedTransform != null && gizmoMode != GizmoMode.NONE) {
            val gizmoOrigin = project3DTo2D(selectedTransform.posX, selectedTransform.posY, selectedTransform.posZ, centerX, centerY)
            drawCadGizmo(
                origin = gizmoOrigin,
                mode = gizmoMode,
                zoomScale = zoomScale
            )
        }
    }
}

/**
 * Dibuja el gizmo CAD interactivo sobre las coordenadas proyectadas de la pieza.
 */
private fun DrawScope.drawCadGizmo(
    origin: Offset,
    mode: GizmoMode,
    zoomScale: Float
) {
    val gizmoLength = (46.dp.toPx() * zoomScale).coerceIn(40f, 90f)

    when (mode) {
        GizmoMode.TRANSLATE -> {
            val xEnd = Offset(origin.x + gizmoLength, origin.y)
            drawLine(color = StitchMeshCoralRed, start = origin, end = xEnd, strokeWidth = 3f)
            drawCircle(color = StitchMeshCoralRed, radius = 5f, center = xEnd)

            val yEnd = Offset(origin.x, origin.y - gizmoLength)
            drawLine(color = StitchMeshSageGreen, start = origin, end = yEnd, strokeWidth = 3f)
            drawCircle(color = StitchMeshSageGreen, radius = 5f, center = yEnd)

            val zEnd = Offset(origin.x - gizmoLength * 0.7f, origin.y + gizmoLength * 0.5f)
            drawLine(color = Color(0xFF3E7BFA), start = origin, end = zEnd, strokeWidth = 3f)
            drawCircle(color = Color(0xFF3E7BFA), radius = 5f, center = zEnd)

            drawCircle(color = StitchMeshYarnGold, radius = 6f, center = origin)
            drawCircle(color = StitchMeshNeutralDark, radius = 3f, center = origin)
        }

        GizmoMode.ROTATE -> {
            drawCircle(
                color = StitchMeshCoralRed,
                radius = gizmoLength * 0.8f,
                center = origin,
                style = Stroke(width = 2.5f)
            )
            drawOval(
                color = StitchMeshSageGreen,
                topLeft = Offset(origin.x - gizmoLength * 0.8f, origin.y - gizmoLength * 0.35f),
                size = Size(gizmoLength * 1.6f, gizmoLength * 0.7f),
                style = Stroke(width = 2.5f)
            )
            drawOval(
                color = Color(0xFF3E7BFA),
                topLeft = Offset(origin.x - gizmoLength * 0.35f, origin.y - gizmoLength * 0.8f),
                size = Size(gizmoLength * 0.7f, gizmoLength * 1.6f),
                style = Stroke(width = 2.5f)
            )
            drawCircle(color = StitchMeshTerracotta, radius = 5f, center = origin)
        }

        GizmoMode.SNAP -> {
            val reticleRadius = gizmoLength * 0.75f
            drawCircle(
                color = StitchMeshTerracotta,
                radius = reticleRadius,
                center = origin,
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = StitchMeshSageGreen.copy(alpha = 0.5f),
                radius = reticleRadius * 0.45f,
                center = origin,
                style = Stroke(width = 1.5f)
            )
            drawLine(
                color = StitchMeshTerracotta,
                start = Offset(origin.x - reticleRadius * 1.25f, origin.y),
                end = Offset(origin.x + reticleRadius * 1.25f, origin.y),
                strokeWidth = 2f
            )
            drawLine(
                color = StitchMeshTerracotta,
                start = Offset(origin.x, origin.y - reticleRadius * 1.25f),
                end = Offset(origin.x, origin.y + reticleRadius * 1.25f),
                strokeWidth = 2f
            )
            drawCircle(color = StitchMeshYarnGold, radius = 5f, center = origin)
            drawCircle(color = StitchMeshNeutralDark, radius = 2.5f, center = origin)
        }

        GizmoMode.NONE -> {}
    }
}

private fun parseHexColor(hexString: String): Color {
    return try {
        val clean = hexString.removePrefix("#")
        val colorInt = android.graphics.Color.parseColor("#$clean")
        Color(colorInt)
    } catch (_: Throwable) {
        StitchMeshTerracotta
    }
}
