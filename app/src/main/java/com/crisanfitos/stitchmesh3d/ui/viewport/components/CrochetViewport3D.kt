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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.crisanfitos.stitchmesh3d.core.geometry.GlbMeshBuilder
import com.crisanfitos.stitchmesh3d.core.geometry.MeshGeometry
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshNeutralDark
import com.crisanfitos.stitchmesh3d.ui.theme.StitchMeshTerracotta
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.android.filament.Skybox
import com.google.android.filament.utils.GestureDetector
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.ModelViewer
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
    roughness: Float = 0.9f,
    cameraPreset: CameraPreset = CameraPreset.ISOMETRIC,
    isWireframe: Boolean = false
) {
    val modelViewerRef = remember { arrayOfNulls<ModelViewer>(1) }
    val manipulatorRef = remember { arrayOfNulls<Manipulator>(1) }
    val surfaceViewRef = remember { arrayOfNulls<SurfaceView>(1) }
    val choreographerCallbackRef = remember { arrayOfNulls<Choreographer.FrameCallback>(1) }
    val lightEntitiesRef = remember { arrayOf(IntArray(3)) }
    val isReleasedRef = remember { booleanArrayOf(false) }

    // Generar binario GLB directo si hay geometría disponible
    val glbBuffer: ByteBuffer? = remember(meshGeometry, yarnColor, roughness, isWireframe) {
        if (meshGeometry != null && meshGeometry.vertexCount > 0 && meshGeometry.indices.isNotEmpty()) {
            GlbMeshBuilder.buildGlb(
                mesh = meshGeometry,
                yarnColor = yarnColor,
                roughness = roughness,
                isWireframe = isWireframe
            )
        } else {
            null
        }
    }

    LaunchedEffect(glbBuffer) {
        val viewer = modelViewerRef[0] ?: return@LaunchedEffect
        val manipulator = manipulatorRef[0]
        if (glbBuffer != null) {
            try {
                glbBuffer.rewind()
                // Preservar la orientación y zoom actual de cámara para evitar saltos o parpadeos (RF-3.5)
                val currentCameraBookmark = try { manipulator?.currentBookmark } catch (_: Throwable) { null }
                try {
                    viewer.destroyModel()
                } catch (_: Throwable) {}
                viewer.loadModelGlb(glbBuffer)
                viewer.transformToUnitCube()
                if (currentCameraBookmark != null) {
                    try {
                        manipulator?.jumpToBookmark(currentCameraBookmark)
                    } catch (_: Throwable) {}
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        } else {
            try {
                viewer.destroyModel()
            } catch (_: Throwable) {}
        }
    }

    // Respuesta reactiva a presets y reset de cámara (Frontal, Lateral, Superior, Iso, Reset) (RF-3.3)
    LaunchedEffect(cameraPreset) {
        val viewer = modelViewerRef[0] ?: return@LaunchedEffect
        val surfaceView = surfaceViewRef[0]
        applyCameraPreset(viewer, surfaceView, cameraPreset, manipulatorRef)
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
                        val manipulator = Manipulator.Builder()
                            .targetPosition(0f, 0f, -4f)
                            .orbitHomePosition(2.2f, 1.8f, -1.8f)
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

                        // Habilitar interacción táctil de órbita 360°, zoom y paneo
                        setOnTouchListener(viewer)

                        // Configurar color de fondo neutro oscuro (#121316 -> R: 0.07, G: 0.074, B: 0.086)
                        val skybox = Skybox.Builder()
                            .color(0.07f, 0.074f, 0.086f, 1.0f)
                            .build(viewer.engine)
                        viewer.scene.skybox = skybox

                        // Configurar iluminación PBR para resaltar el relieve y trama del tejido de crochet
                        val lights = lightEntitiesRef[0]

                        // 1. Luz direccional principal (Sol superior en ángulo)
                        val sunLight = EntityManager.get().create()
                        LightManager.Builder(LightManager.Type.DIRECTIONAL)
                            .color(1.0f, 0.98f, 0.95f)
                            .intensity(110_000.0f)
                            .direction(0.4f, -1.0f, -0.6f)
                            .castShadows(true)
                            .build(viewer.engine, sunLight)
                        viewer.scene.addEntity(sunLight)
                        lights[0] = sunLight

                        // 2. Luz de relleno difusa para suavizar caras opuestas
                        val fillLight = EntityManager.get().create()
                        LightManager.Builder(LightManager.Type.DIRECTIONAL)
                            .color(0.85f, 0.92f, 1.0f)
                            .intensity(45_000.0f)
                            .direction(-0.6f, -0.5f, 0.4f)
                            .castShadows(false)
                            .build(viewer.engine, fillLight)
                        viewer.scene.addEntity(fillLight)
                        lights[1] = fillLight

                        // 3. Luz de rebote inferior suave
                        val bounceLight = EntityManager.get().create()
                        LightManager.Builder(LightManager.Type.DIRECTIONAL)
                            .color(1.0f, 0.95f, 0.90f)
                            .intensity(30_000.0f)
                            .direction(0.0f, 1.0f, -0.3f)
                            .castShadows(false)
                            .build(viewer.engine, bounceLight)
                        viewer.scene.addEntity(bounceLight)
                        lights[2] = bounceLight

                        val frameCallback = object : Choreographer.FrameCallback {
                            override fun doFrame(frameTimeNanos: Long) {
                                if (isReleasedRef[0]) return
                                choreographerCallbackRef[0]?.let {
                                    Choreographer.getInstance().postFrameCallback(it)
                                }
                                try {
                                    viewer.render(frameTimeNanos)
                                } catch (_: Throwable) {
                                    // Protección contra invalidación transitoria de SwapChain
                                }
                            }
                        }
                        choreographerCallbackRef[0] = frameCallback
                        Choreographer.getInstance().postFrameCallback(frameCallback)

                        if (glbBuffer != null) {
                            glbBuffer.rewind()
                            viewer.loadModelGlb(glbBuffer)
                            viewer.transformToUnitCube()
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }
            },
            onRelease = {
                isReleasedRef[0] = true
                try {
                    choreographerCallbackRef[0]?.let {
                        Choreographer.getInstance().removeFrameCallback(it)
                    }
                } catch (_: Throwable) {}
                choreographerCallbackRef[0] = null

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
                        viewer.destroyModel()
                    } catch (_: Throwable) {}
                }
                modelViewerRef[0] = null
                manipulatorRef[0] = null
                surfaceViewRef[0] = null
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            isReleasedRef[0] = true
            try {
                choreographerCallbackRef[0]?.let {
                    Choreographer.getInstance().removeFrameCallback(it)
                }
            } catch (_: Throwable) {}
            choreographerCallbackRef[0] = null

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
                    viewer.destroyModel()
                } catch (_: Throwable) {}
            }
            modelViewerRef[0] = null
            manipulatorRef[0] = null
            surfaceViewRef[0] = null
        }
    }
}

/**
 * Aplica un preset orbital de cámara determinista (Frontal, Lateral, Superior, Iso, Reset)
 * actualizando el Manipulator y el GestureDetector de Filament para una rotación inmediata y reactiva (RF-3.3).
 */
private fun applyCameraPreset(
    viewer: ModelViewer,
    surfaceView: SurfaceView?,
    preset: CameraPreset,
    manipulatorRef: Array<Manipulator?>
) {
    val (eyeX, eyeY, eyeZ) = when (preset) {
        CameraPreset.ISOMETRIC, CameraPreset.RESET -> floatArrayOf(2.2f, 1.8f, -1.8f)
        CameraPreset.FRONT -> floatArrayOf(0f, 0f, -0.8f)
        CameraPreset.SIDE -> floatArrayOf(3.2f, 0f, -4.0f)
        CameraPreset.TOP -> floatArrayOf(0.001f, 3.2f, -4.0f)
    }
    val (upX, upY, upZ) = when (preset) {
        CameraPreset.TOP -> floatArrayOf(0f, 0f, -1f)
        else -> floatArrayOf(0f, 1f, 0f)
    }

    if (surfaceView != null) {
        try {
            val width = surfaceView.width.coerceAtLeast(1)
            val height = surfaceView.height.coerceAtLeast(1)
            val newManipulator = Manipulator.Builder()
                .viewport(width, height)
                .targetPosition(0f, 0f, -4f)
                .orbitHomePosition(eyeX, eyeY, eyeZ)
                .upVector(upX, upY, upZ)
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
            eyeX.toDouble(), eyeY.toDouble(), eyeZ.toDouble(),
            0.0, 0.0, -4.0,
            upX.toDouble(), upY.toDouble(), upZ.toDouble()
        )
    } catch (_: Throwable) {}
}
