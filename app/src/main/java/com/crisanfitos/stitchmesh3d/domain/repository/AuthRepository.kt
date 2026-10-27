package com.crisanfitos.stitchmesh3d.domain.repository

import com.crisanfitos.stitchmesh3d.domain.model.AuthSession
import com.crisanfitos.stitchmesh3d.domain.model.AuthState
import com.crisanfitos.stitchmesh3d.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato de dominio para autenticación de usuarios y gestión de perfil contra Supabase (SM-036).
 */
interface AuthRepository {

    /**
     * Flujo reactivo del estado actual de autenticación.
     */
    val authState: StateFlow<AuthState>

    /**
     * Registra un nuevo usuario con email, contraseña y nombre de usuario en Supabase Auth + profiles.
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        username: String,
        displayName: String? = null
    ): Result<AuthSession>

    /**
     * Inicia sesión con email y contraseña contra Supabase GoTrue.
     */
    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<AuthSession>

    /**
     * Restaura la sesión persistida localmente al abrir la aplicación, refrescando el token si ha expirado.
     */
    suspend fun restoreSession(): Result<AuthSession?>

    /**
     * Actualiza el nombre de usuario o nombre visible en `public.profiles`.
     */
    suspend fun updateProfile(
        username: String,
        displayName: String?
    ): Result<UserProfile>

    /**
     * Cierra la sesión activa en Supabase y limpia las credenciales locales de forma segura.
     */
    suspend fun signOut(): Result<Unit>
}
