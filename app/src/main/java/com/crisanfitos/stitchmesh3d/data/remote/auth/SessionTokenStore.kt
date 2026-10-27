package com.crisanfitos.stitchmesh3d.data.remote.auth

import android.content.Context
import com.crisanfitos.stitchmesh3d.domain.model.AuthSession
import com.crisanfitos.stitchmesh3d.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Almacén de persistencia local para la sesión de autenticación de Supabase (SM-036).
 */
interface SessionTokenStore {
    fun saveSession(session: AuthSession)
    fun loadSession(): AuthSession?
    fun clearSession()
}

/**
 * Implementación en memoria para tests unitarios y previsualizaciones.
 */
class InMemorySessionTokenStore(
    initialSession: AuthSession? = null
) : SessionTokenStore {
    private var current: AuthSession? = initialSession

    override fun saveSession(session: AuthSession) {
        current = session
    }

    override fun loadSession(): AuthSession? = current

    override fun clearSession() {
        current = null
    }
}

/**
 * Implementación Android respaldada por SharedPreferences privadas de la aplicación.
 */
@Singleton
class SharedPreferencesSessionTokenStore @Inject constructor(
    @ApplicationContext context: Context
) : SessionTokenStore {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun saveSession(session: AuthSession) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochSeconds)
            .putString(KEY_USER_ID, session.user.id)
            .putString(KEY_USER_EMAIL, session.user.email)
            .putString(KEY_USERNAME, session.user.username)
            .putString(KEY_DISPLAY_NAME, session.user.displayName)
            .apply()
    }

    override fun loadSession(): AuthSession? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val email = prefs.getString(KEY_USER_EMAIL, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val displayName = prefs.getString(KEY_DISPLAY_NAME, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)

        return AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochSeconds = expiresAt,
            user = UserProfile(
                id = userId,
                email = email,
                username = username,
                displayName = displayName
            )
        )
    }

    override fun clearSession() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "stitchmesh_supabase_auth_session"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USERNAME = "username"
        const val KEY_DISPLAY_NAME = "display_name"
    }
}
