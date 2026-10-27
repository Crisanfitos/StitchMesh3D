package com.crisanfitos.stitchmesh3d.domain.model

/**
 * Perfil de usuario sincronizado con la tabla `public.profiles` de Supabase (SM-036).
 */
data class UserProfile(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String? = null
)

/**
 * Sesión autenticada activa con tokens JWT de Supabase GoTrue.
 */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val user: UserProfile
) {
    fun isExpired(nowEpochSeconds: Long = System.currentTimeMillis() / 1000L): Boolean {
        return nowEpochSeconds >= expiresAtEpochSeconds
    }
}

/**
 * Máquina de estados de autenticación de la aplicación (SM-036).
 */
sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val session: AuthSession) : AuthState
    data class Error(val message: String) : AuthState
}
