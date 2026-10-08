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
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.ModelViewer
import java.nio.ByteBuffer

/**
 * Componente Composable que aloja el visualizador 3D nativo de Google Filament.
 *
 * Configura una escena PBR con:
 * - Material textil mate de lana (Wool PBR: roughness ~ 0.9, metallic = 0.0)
 * - Iluminación multicapa: Sol principal direccional + luz de relleno fría + rebote suave
 * - Fondo neutro oscuro (#121316)
 * - Controles táctiles de órbita 360°, zoom por pellizco y paneo (RF-3.3)
 * - Presets de cámara y botón de reset (Iso, Frontal, Lateral, Superior)
 * - Ciclo de vida sincronizado con Choreographer y Compose DisposableEffect
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
    val choreographerCallbackRef = remember { arrayOfNulls<Choreographer.FrameCallback>(1) }
    val lightEntitiesRef = remember { arrayOf(IntArray(3)) }

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
                val currentCameraBookmark = manipulator?.currentBookmark
                viewer.destroyModel()
                viewer.loadModelGlb(glbBuffer)
                viewer.transformToUnitCube()
                if (currentCameraBookmark != null) {
                    manipulator.jumpToBookmark(currentCameraBookmark)
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        } else {
            viewer.destroyModel()
        }
    }

    // Respuesta reactiva a presets y reset de cámara (RF-3.3)
    LaunchedEffect(cameraPreset) {
        val manipulator = manipulatorRef[0] ?: return@LaunchedEffect
        try {
            when (cameraPreset) {
                CameraPreset.RESET, CameraPreset.ISOMETRIC -> {
                    manipulator.jumpToBookmark(manipulator.homeBookmark)
                }
                CameraPreset.FRONT -> {
                    manipulator.jumpToBookmark(manipulator.homeBookmark)
                }
                CameraPreset.SIDE -> {
                    manipulator.jumpToBookmark(manipulator.homeBookmark)
                    manipulator.grabBegin(0, 0, false)
                    manipulator.grabUpdate(250, 0)
                    manipulator.grabEnd()
                }
                CameraPreset.TOP -> {
                    manipulator.jumpToBookmark(manipulator.homeBookmark)
                    manipulator.grabBegin(0, 0, false)
                    manipulator.grabUpdate(0, -250)
                    manipulator.grabEnd()
                }
            }
        } catch (_: Throwable) {
            // Protección ante manipulador no disponible en frame actual
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
                SurfaceView(context).apply {
                    try {
                        val manipulator = Manipulator.Builder()
                            .targetPosition(0f, 0f, 0f)
                            .orbitHomePosition(0f, 0f, 3.5f)
                            .orbitSpeed(0.005f, 0.005f)
                            .zoomSpeed(0.01f)
                            .panning(true)
                            .build(Manipulator.Mode.ORBIT)
                        manipulatorRef[0] = manipulator

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
                                choreographerCallbackRef[0]?.let {
                                    Choreographer.getInstance().postFrameCallback(it)
                                }
                                viewer.render(frameTimeNanos)
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
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
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
        }
    }
}
