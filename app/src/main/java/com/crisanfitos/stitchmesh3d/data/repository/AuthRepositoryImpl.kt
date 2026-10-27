package com.crisanfitos.stitchmesh3d.data.repository

import com.crisanfitos.stitchmesh3d.data.remote.auth.SessionTokenStore
import com.crisanfitos.stitchmesh3d.data.remote.auth.SupabaseAuthGateway
import com.crisanfitos.stitchmesh3d.di.IoDispatcher
import com.crisanfitos.stitchmesh3d.domain.model.AuthSession
import com.crisanfitos.stitchmesh3d.domain.model.AuthState
import com.crisanfitos.stitchmesh3d.domain.model.UserProfile
import com.crisanfitos.stitchmesh3d.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Implementación de [AuthRepository] respaldada por Supabase Auth y [SessionTokenStore] (SM-036).
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val gateway: SupabaseAuthGateway,
    private val tokenStore: SessionTokenStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(
        tokenStore.loadSession()?.let { AuthState.Authenticated(it) } ?: AuthState.Unauthenticated
    )
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        username: String,
        displayName: String?
    ): Result<AuthSession> = withContext(ioDispatcher) {
        val cleanEmail = email.trim()
        val cleanUsername = username.trim()
        if (cleanEmail.isBlank() || password.length < 6 || cleanUsername.isBlank()) {
            val error = IllegalArgumentException("Credenciales inválidas: email, usuario y contraseña (>=6) son obligatorios")
            _authState.value = AuthState.Error(error.message ?: "Datos inválidos")
            return@withContext Result.failure(error)
        }

        _authState.value = AuthState.Loading
        runCatching {
            val session = gateway.signUp(
                email = cleanEmail,
                password = password,
                username = cleanUsername,
                displayName = displayName?.trim()?.ifBlank { null }
            )
            tokenStore.saveSession(session)
            _authState.value = AuthState.Authenticated(session)
            session
        }.onFailure { throwable ->
            _authState.value = AuthState.Error(throwable.message ?: "Error al registrar usuario")
        }
    }

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<AuthSession> = withContext(ioDispatcher) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.isBlank()) {
            val error = IllegalArgumentException("Introduce email y contraseña")
            _authState.value = AuthState.Error(error.message ?: "Datos inválidos")
            return@withContext Result.failure(error)
        }

        _authState.value = AuthState.Loading
        runCatching {
            val session = gateway.signInWithPassword(cleanEmail, password)
            tokenStore.saveSession(session)
            _authState.value = AuthState.Authenticated(session)
            session
        }.onFailure { throwable ->
            _authState.value = AuthState.Error(throwable.message ?: "Error de inicio de sesión")
        }
    }

    override suspend fun restoreSession(): Result<AuthSession?> = withContext(ioDispatcher) {
        val cached = tokenStore.loadSession()
        if (cached == null) {
            _authState.value = AuthState.Unauthenticated
            return@withContext Result.success(null)
        }

        if (!cached.isExpired()) {
            _authState.value = AuthState.Authenticated(cached)
            return@withContext Result.success(cached)
        }

        runCatching {
            val refreshed = gateway.refreshSession(cached.refreshToken)
            tokenStore.saveSession(refreshed)
            _authState.value = AuthState.Authenticated(refreshed)
            refreshed
        }.onFailure {
            tokenStore.clearSession()
            _authState.value = AuthState.Unauthenticated
        }
    }

    override suspend fun updateProfile(
        username: String,
        displayName: String?
    ): Result<UserProfile> = withContext(ioDispatcher) {
        val current = (_authState.value as? AuthState.Authenticated)?.session
            ?: tokenStore.loadSession()
            ?: return@withContext Result.failure(IllegalStateException("No hay sesión activa"))

        val updatedCandidate = current.user.copy(
            username = username.trim().ifBlank { current.user.username },
            displayName = displayName?.trim()?.ifBlank { null }
        )

        runCatching {
            val savedProfile = gateway.upsertProfile(current.accessToken, updatedCandidate)
            val newSession = current.copy(user = savedProfile)
            tokenStore.saveSession(newSession)
            _authState.value = AuthState.Authenticated(newSession)
            savedProfile
        }
    }

    override suspend fun signOut(): Result<Unit> = withContext(ioDispatcher) {
        val current = tokenStore.loadSession()
        runCatching {
            if (current != null) {
                runCatching { gateway.signOut(current.accessToken) }
            }
            tokenStore.clearSession()
            _authState.value = AuthState.Unauthenticated
        }
    }
}
