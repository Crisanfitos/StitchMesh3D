package com.crisanfitos.stitchmesh3d.ui.viewport.components

import android.annotation.SuppressLint
import android.view.Choreographer
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.crisanfitos.stitchmesh3d.core.geometry.BoundingBox3D
import com.crisanfitos.stitchmesh3d.core.geometry.ColorSpaceHelper
import com.crisanfitos.stitchmesh3d.core.geometry.GlbMeshBuilder
import com.crisanfitos.stitchmesh3d.core.geometry.MeshGeometry
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshNeutralDark
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Skybox
import com.google.android.filament.View
import com.google.android.filament.utils.GestureDetector
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.ModelViewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * Componente Composable que aloja el visualizador 3D nativo de Google Filament.
 *
 * Configura una escena PBR con:
 * - Material textil mate de lana (Wool PBR: roughness ~ 0.9, metallic = 0.0)
 * - Iluminación multicapa calibrada para resaltar el relieve del tejido
 * - Fondo neutro oscuro (#121316)
 * - Controles táctiles de órbita 360°, zoom por pellizco y paneo (RF-3.3)
 * - Presets de cámara matemáticos deterministas con Bookmarks nativos (Isométrica, Frontal, Lateral, Superior)
 * - Ciclo de vida robusto protegido contra cierres inesperados en cambios de Surface o de ventana
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun CrochetViewport3D(
    meshGeometry: MeshGeometry?,
    modifier: Modifier = Modifier,
    yarnColor: Color = StitchMeshTerracotta,
    roughness: Float = StudioLightingConfig.CALIBRATED_TEXTILE.defaultRoughness,
    cameraPreset: CameraPreset = CameraPreset.ISOMETRIC,
    isWireframe: Boolean = false,
    lightingConfig: StudioLightingConfig = StudioLightingConfig.CALIBRATED_TEXTILE
) {
    val modelViewerRef = remember { arrayOfNulls<ModelViewer>(1) }
    val manipulatorRef = remember { arrayOfNulls<Manipulator>(1) }
    val surfaceViewRef = remember { arrayOfNulls<SurfaceView>(1) }
    val choreographerCallbackRef = remember { arrayOfNulls<Choreographer.FrameCallback>(1) }
    val lightEntitiesRef = remember { arrayOf(IntArray(4)) }
    val indirectLightRef = remember { arrayOfNulls<IndirectLight>(1) }
    val isReleasedRef = remember { booleanArrayOf(false) }

    // Generar binario GLB de forma asíncrona fuera del hilo de UI (Dispatchers.Default) para evitar jank (RND-2, RF-3.5)
    var glbBuffer by remember { mutableStateOf<ByteBuffer?>(null) }

    LaunchedEffect(meshGeometry, yarnColor, roughness, isWireframe) {
        if (meshGeometry != null && meshGeometry.vertexCount > 0 && meshGeometry.indices.isNotEmpty()) {
            val buffer = withContext(Dispatchers.Default) {
                GlbMeshBuilder.buildGlb(
                    mesh = meshGeometry,
                    yarnColor = yarnColor,
                    roughness = roughness,
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
                // Preservar la orientación y zoom actual de cámara para evitar saltos o parpadeos (RF-3.5)
                val currentCameraBookmark = try { manipulator?.currentBookmark } catch (_: Throwable) { null }

                // Destruir limpiamente el modelo anterior para evitar acumulación de entidades y fugas nativas Filament
                try {
                    viewer.destroyModel()
                } catch (_: Throwable) {
                    viewer.asset?.let { asset ->
                        try { viewer.scene.removeEntities(asset.entities) } catch (_: Throwable) {}
                        try { viewer.scene.removeEntities(asset.lightEntities) } catch (_: Throwable) {}
                    }
                }

                viewer.loadModelGlb(currentGlb)
                // Conservar escala métrica real en milímetros sin transformToUnitCube (SM-085)
                if (currentCameraBookmark != null) {
                    try {
                        manipulator?.jumpToBookmark(currentCameraBookmark)
                    } catch (_: Throwable) {}
                } else {
                    applyCameraPreset(viewer, surfaceViewRef[0], cameraPreset, meshGeometry, manipulatorRef)
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        } else {
            try {
                viewer.destroyModel()
            } catch (_: Throwable) {
                try {
                    viewer.asset?.let { asset ->
                        viewer.scene.removeEntities(asset.entities)
                        viewer.scene.removeEntities(asset.lightEntities)
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    // Respuesta reactiva a presets y reset de cámara (Frontal, Lateral, Superior, Iso, Reset) (RF-3.3)
    LaunchedEffect(cameraPreset) {
        val viewer = modelViewerRef[0] ?: return@LaunchedEffect
        val surfaceView = surfaceViewRef[0]
        applyCameraPreset(viewer, surfaceView, cameraPreset, meshGeometry, manipulatorRef)
    }

    // Teardown unificado e idempotente para evitar SIGSEGV en gltfio al navegar fuera del Workspace (SM-060)
    val performTeardown = remember {
        {
            if (!isReleasedRef[0]) {
                isReleasedRef[0] = true

                // 1. Detener incondicionalmente el loop de renderizado en Choreographer
                try {
                    choreographerCallbackRef[0]?.let { cb ->
                        Choreographer.getInstance().removeFrameCallback(cb)
                    }
                } catch (_: Throwable) {}
                choreographerCallbackRef[0] = null

                // 2. Limpieza segura de luces y entidades del Scene sin invocar asyncCancelLoad() nativo
                val viewer = modelViewerRef[0]
                if (viewer != null) {
                    val lights = lightEntitiesRef[0]
                    for (i in lights.indices) {
                        val light = lights[i]
                        if (light != 0) {
                            try {
                                viewer.scene.removeEntity(light)
                            } catch (_: Throwable) {}
                            try {
                                EntityManager.get().destroy(light)
                            } catch (_: Throwable) {}
                            lights[i] = 0
                        }
                    }
                    try {
                        viewer.scene.indirectLight = null
                        indirectLightRef[0]?.let { ibl ->
                            viewer.engine.destroyIndirectLight(ibl)
                        }
                    } catch (_: Throwable) {}
                    indirectLightRef[0] = null
                    try {
                        viewer.asset?.let { asset ->
                            viewer.scene.removeEntities(asset.entities)
                            viewer.scene.removeEntities(asset.lightEntities)
                        }
                    } catch (_: Throwable) {}
                }

                modelViewerRef[0] = null
                manipulatorRef[0] = null
                surfaceViewRef[0] = null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StitchMeshNeutralDark)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                isReleasedRef[0] = false
                SurfaceView(context).apply {
                    try {
                        val initialBbox = meshGeometry?.computeBoundingBox() ?: BoundingBox3D(0f, 0f, 0f, 0f, 0f, 0f)
                        val initialFraming = MetricCameraFramingHelper.calculateFraming(initialBbox, cameraPreset)

                        val manipulator = Manipulator.Builder()
                            .targetPosition(initialFraming.targetX, initialFraming.targetY, initialFraming.targetZ)
                            .orbitHomePosition(initialFraming.eyeX, initialFraming.eyeY, initialFraming.eyeZ)
                            .upVector(initialFraming.upX, initialFraming.upY, initialFraming.upZ)
                            .orbitSpeed(0.005f, 0.005f)
                            .zoomSpeed(0.01f)
                            .panning(true)
                            .build(Manipulator.Mode.ORBIT)
                        manipulatorRef[0] = manipulator
                        surfaceViewRef[0] = this

                        val viewer = ModelViewer(
                            surfaceView = this,
                            manipulator = manipulator
                        )
                        modelViewerRef[0] = viewer

                        // Eliminar la luz solar por defecto de ModelViewer (~100k lux) para evitar sobreexposición (SM-085)
                        try {
                            viewer.scene.removeEntity(viewer.light)
                        } catch (_: Throwable) {}

                        // Habilitar interacción táctil de órbita 360°, zoom y paneo
                        setOnTouchListener(viewer)

                        // Configurar color de fondo neutro oscuro (#121316) convertido a espacio lineal IEC 61966-2-1 (SM-085)
                        val linearBg = ColorSpaceHelper.hexToLinearRgba("#121316")
                        val skybox = Skybox.Builder()
                            .color(linearBg[0], linearBg[1], linearBg[2], linearBg[3])
                            .build(viewer.engine)
                        viewer.scene.skybox = skybox

                        // Configurar oclusión ambiental SSAO y sombras PCF en la vista (SM-085)
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

                        // Configurar iluminación PBR de estudio balanceada y luz ambiental indirecta (SM-073 / SM-085)
                        val indirectLight = IndirectLight.Builder()
                            .irradiance(
                                lightingConfig.sphericalHarmonicsBands,
                                lightingConfig.sphericalHarmonicsIrradiance
                            )
                            .intensity(lightingConfig.indirectLightIntensityLux)
                            .build(viewer.engine)
                        viewer.scene.indirectLight = indirectLight
                        indirectLightRef[0] = indirectLight

                        // Rig envolvente de luces direccionales de estudio (Key con sombras, Fill, Rim, Bounce)
                        val lights = lightEntitiesRef[0]
                        val directionalSpecs = lightingConfig.directionalLights
                        for (i in directionalSpecs.indices) {
                            val spec = directionalSpecs[i]
                            val entity = EntityManager.get().create()
                            LightManager.Builder(LightManager.Type.DIRECTIONAL)
                                .color(spec.colorR, spec.colorG, spec.colorB)
                                .intensity(spec.intensityLux)
                                .direction(spec.dirX, spec.dirY, spec.dirZ)
                                .castShadows(spec.castShadows)
                                .build(viewer.engine, entity)
                            viewer.scene.addEntity(entity)
                            if (i < lights.size) {
                                lights[i] = entity
                            }
                        }

                        val frameCallback = object : Choreographer.FrameCallback {
                            override fun doFrame(frameTimeNanos: Long) {
                                if (isReleasedRef[0]) return
                                val cb = choreographerCallbackRef[0] ?: return
                                try {
                                     viewer.render(frameTimeNanos)
                                    if (!isReleasedRef[0]) {
                                        Choreographer.getInstance().postFrameCallback(cb)
                                    }
                                } catch (_: Throwable) {
                                    // Protección contra invalidación transitoria de SwapChain
                                }
                            }
                        }
                        choreographerCallbackRef[0] = frameCallback
                        Choreographer.getInstance().postFrameCallback(frameCallback)

                        val initGlb = glbBuffer
                        if (initGlb != null) {
                            initGlb.rewind()
                            viewer.loadModelGlb(initGlb)
                            // Escala métrica real sin transformToUnitCube (SM-085)
                            applyCameraPreset(viewer, this, cameraPreset, meshGeometry, manipulatorRef)
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }
            },
            onRelease = {
                performTeardown()
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            performTeardown()
        }
    }
}

/**
 * Aplica un preset orbital de cámara determinista (Frontal, Lateral, Superior, Iso, Reset)
 * en escala métrica real sobre las cotas físicas de la pieza (SM-085, RF-3.3).
 */
private fun applyCameraPreset(
    viewer: ModelViewer,
    surfaceView: SurfaceView?,
    preset: CameraPreset,
    meshGeometry: MeshGeometry?,
    manipulatorRef: Array<Manipulator?>
) {
    val bbox = meshGeometry?.computeBoundingBox() ?: BoundingBox3D(0f, 0f, 0f, 0f, 0f, 0f)
    val framing = MetricCameraFramingHelper.calculateFraming(bbox, preset)

    if (surfaceView != null) {
        try {
            val width = surfaceView.width.coerceAtLeast(1)
            val height = surfaceView.height.coerceAtLeast(1)
            val newManipulator = Manipulator.Builder()
                .viewport(width, height)
                .targetPosition(framing.targetX, framing.targetY, framing.targetZ)
                .orbitHomePosition(framing.eyeX, framing.eyeY, framing.eyeZ)
                .upVector(framing.upX, framing.upY, framing.upZ)
                .zoomSpeed(0.01f)
                .orbitSpeed(0.005f, 0.005f)
                .panning(true)
                .build(Manipulator.Mode.ORBIT)

            val manipulatorField = ModelViewer::class.java.getDeclaredField("cameraManipulator")
            manipulatorField.isAccessible = true
            manipulatorField.set(viewer, newManipulator)

            val detectorField = ModelViewer::class.java.getDeclaredField("gestureDetector")
            detectorField.isAccessible = true
            detectorField.set(viewer, GestureDetector(surfaceView, newManipulator))

            manipulatorRef[0] = newManipulator
            return
        } catch (_: Throwable) {
            // Continuar con fallback directo
        }
    }

    try {
        viewer.camera.lookAt(
            framing.eyeX.toDouble(), framing.eyeY.toDouble(), framing.eyeZ.toDouble(),
            framing.targetX.toDouble(), framing.targetY.toDouble(), framing.targetZ.toDouble(),
            framing.upX.toDouble(), framing.upY.toDouble(), framing.upZ.toDouble()
        )
    } catch (_: Throwable) {}
}
