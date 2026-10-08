package com.crisanfitos.stitchmesh3d

import android.app.Application
import com.google.android.filament.Filament
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.utils.Utils
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase principal de aplicación de StitchMesh 3D.
 * Inicializa el grafo de dependencias de Hilt para toda la aplicación y el motor nativo de Google Filament.
 */
@HiltAndroidApp
class StitchMeshApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            Filament.init()
            Gltfio.init()
            Utils.init()
        } catch (_: Throwable) {
            // En entornos JVM unit tests las librerías JNI de Filament no están presentes en host
        }
    }
}
