package com.crisanfitos.stitchmesh3d.data.repository

import com.crisanfitos.stitchmesh3d.data.remote.auth.InMemorySessionTokenStore
import com.crisanfitos.stitchmesh3d.data.remote.auth.SupabaseAuthGateway
import com.crisanfitos.stitchmesh3d.domain.model.AuthSession
import com.crisanfitos.stitchmesh3d.domain.model.AuthState
import com.crisanfitos.stitchmesh3d.domain.model.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private class FakeSupabaseAuthGateway : SupabaseAuthGateway {
        var signedOutTokens = mutableListOf<String>()

        override suspend fun signUp(
            email: String,
            password: String,
            username: String,
            displayName: String?
        ): AuthSession {
            return AuthSession(
                accessToken = "jwt_signup_token",
                refreshToken = "refresh_signup_token",
                expiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 3600L,
                user = UserProfile(
                    id = "user-uuid-001",
                    email = email,
                    username = username,
                    displayName = displayName
                )
            )
        }

        override suspend fun signInWithPassword(email: String, password: String): AuthSession {
            if (password != "correct-horse-battery") {
                throw IllegalStateException("Invalid login credentials")
            }
            return AuthSession(
                accessToken = "jwt_signin_token",
                refreshToken = "refresh_signin_token",
                expiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 3600L,
                user = UserProfile(
                    id = "user-uuid-001",
                    email = email,
                    username = "crisanfitos",
                    displayName = "Cris"
                )
            )
        }

        override suspend fun refreshSession(refreshToken: String): AuthSession {
            return AuthSession(
                accessToken = "jwt_refreshed_token",
                refreshToken = "refresh_new_token",
                expiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 7200L,
                user = UserProfile(
                    id = "user-uuid-001",
                    email = "cris@stitchmesh.dev",
                    username = "crisanfitos",
                    displayName = "Cris"
                )
            )
        }

        override suspend fun upsertProfile(accessToken: String, profile: UserProfile): UserProfile {
            return profile
        }

        override suspend fun signOut(accessToken: String) {
            signedOutTokens.add(accessToken)
        }
    }

    @Test
    fun `signUpWithEmail persists session and transitions state to Authenticated`() = runTest {
        val store = InMemorySessionTokenStore()
        val repo = AuthRepositoryImpl(FakeSupabaseAuthGateway(), store, UnconfinedTestDispatcher(testScheduler))

        val result = repo.signUpWithEmail(
            email = "cris@stitchmesh.dev",
            password = "secretPassword123",
            username = "crisanfitos",
            displayName = "Cris Amigurumi"
        )

        assertTrue(result.isSuccess)
        val state = repo.authState.value
        assertTrue(state is AuthState.Authenticated)
        assertEquals("crisanfitos", (state as AuthState.Authenticated).session.user.username)
        assertEquals("jwt_signup_token", store.loadSession()?.accessToken)
    }

    @Test
    fun `restoreSession reuses valid non-expired token without network call`() = runTest {
        val validSession = AuthSession(
            accessToken = "cached_valid_token",
            refreshToken = "cached_refresh",
            expiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 1800L,
            user = UserProfile("u1", "cris@stitchmesh.dev", "crisanfitos", "Cris")
        )
        val store = InMemorySessionTokenStore(validSession)
        val repo = AuthRepositoryImpl(FakeSupabaseAuthGateway(), store, UnconfinedTestDispatcher(testScheduler))

        val restored = repo.restoreSession().getOrThrow()

        assertEquals("cached_valid_token", restored?.accessToken)
        assertTrue(repo.authState.value is AuthState.Authenticated)
    }

    @Test
    fun `restoreSession refreshes expired token automatically`() = runTest {
        val expiredSession = AuthSession(
            accessToken = "expired_token",
            refreshToken = "cached_refresh",
            expiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) - 60L,
            user = UserProfile("u1", "cris@stitchmesh.dev", "crisanfitos", "Cris")
        )
        val store = InMemorySessionTokenStore(expiredSession)
        val repo = AuthRepositoryImpl(FakeSupabaseAuthGateway(), store, UnconfinedTestDispatcher(testScheduler))

        val restored = repo.restoreSession().getOrThrow()

        assertEquals("jwt_refreshed_token", restored?.accessToken)
        assertEquals("jwt_refreshed_token", store.loadSession()?.accessToken)
    }

    @Test
    fun `updateProfile updates username and displayName in active session`() = runTest {
        val store = InMemorySessionTokenStore()
        val repo = AuthRepositoryImpl(FakeSupabaseAuthGateway(), store, UnconfinedTestDispatcher(testScheduler))
        repo.signInWithEmail("cris@stitchmesh.dev", "correct-horse-battery")

        val updated = repo.updateProfile("cris_3d", "Cris CAD").getOrThrow()

        assertEquals("cris_3d", updated.username)
        assertEquals("Cris CAD", updated.displayName)
        assertEquals("cris_3d", store.loadSession()?.user?.username)
    }

    @Test
    fun `signOut clears local credentials and sets state to Unauthenticated`() = runTest {
        val gateway = FakeSupabaseAuthGateway()
        val store = InMemorySessionTokenStore()
        val repo = AuthRepositoryImpl(gateway, store, UnconfinedTestDispatcher(testScheduler))
        repo.signInWithEmail("cris@stitchmesh.dev", "correct-horse-battery")

        repo.signOut()

        assertNull(store.loadSession())
        assertEquals(AuthState.Unauthenticated, repo.authState.value)
        assertEquals(listOf("jwt_signin_token"), gateway.signedOutTokens)
    }
}
