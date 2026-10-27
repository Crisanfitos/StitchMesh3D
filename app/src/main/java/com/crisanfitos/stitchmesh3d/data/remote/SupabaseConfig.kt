package com.crisanfitos.stitchmesh3d.data.remote

import com.crisanfitos.stitchmesh3d.BuildConfig

/**
 * Configuración centralizada de conexión con Supabase Cloud para StitchMesh 3D (SM-078).
 * Lee las variables inyectadas de forma segura a través de BuildConfig (procedentes de
 * `local.properties` en desarrollo local o de GitHub Actions Secrets en CI/CD).
 */
data class SupabaseConfig(
    val projectId: String = BuildConfig.SUPABASE_PROJECT_ID,
    val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    val supabaseAnonKey: String = BuildConfig.SUPABASE_ANON_KEY
) {
    /**
     * Indica si la configuración dispone de una URL de proyecto HTTPS válida y clave anónima.
     */
    val isReadyForNetwork: Boolean
        get() = projectId.isNotBlank() &&
            supabaseUrl.startsWith("https://") &&
            supabaseUrl.endsWith(".supabase.co") &&
            supabaseAnonKey.isNotBlank()

    /**
     * Endpoint base de PostgREST v1 para sincronización de tablas.
     */
    val restEndpointUrl: String
        get() = "${supabaseUrl.trimEnd('/')}/rest/v1"

    /**
     * Endpoint base de GoTrue Auth v1 para autenticación de usuarios.
     */
    val authEndpointUrl: String
        get() = "${supabaseUrl.trimEnd('/')}/auth/v1"
}
