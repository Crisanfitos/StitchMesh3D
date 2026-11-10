package com.crisanfitos.stitchmesh3d.data.remote.auth

import com.crisanfitos.stitchmesh3d.domain.model.AuthSession
import com.crisanfitos.stitchmesh3d.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTokenStoreTest {

    @Test
    fun `AesGcmSessionTokenEncryptor encrypts plaintext so it does not appear in ciphertext`() {
        val encryptor = AesGcmSessionTokenEncryptor()
        val plainJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.sflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

        val cipherText = encryptor.encrypt(plainJwt)

        assertFalse("El token JWT en texto plano no debe aparecer en el texto cifrado", cipherText.contains("eyJhbGciOi"))
        assertFalse("El ciphertext no debe ser igual al plaintext", cipherText == plainJwt)

        val decrypted = encryptor.decrypt(cipherText)
        assertEquals(plainJwt, decrypted)
    }

    @Test
    fun `encrypting twice generates different ciphertexts due to random GCM IV`() {
        val encryptor = AesGcmSessionTokenEncryptor()
        val token = "sample-secret-access-token"

        val cipher1 = encryptor.encrypt(token)
        val cipher2 = encryptor.encrypt(token)

        assertNotEquals(cipher1, cipher2)
        assertEquals(token, encryptor.decrypt(cipher1))
        assertEquals(token, encryptor.decrypt(cipher2))
    }

    @Test
    fun `tampered ciphertext returns empty string without crashing`() {
        val encryptor = AesGcmSessionTokenEncryptor()
        val token = "sensitive-refresh-token"
        val cipher = encryptor.encrypt(token)

        // Corromper el ciphertext
        val tampered = cipher.substring(0, cipher.length - 4) + "AAAA"
        val result = encryptor.decrypt(tampered)

        assertEquals("", result)
    }

    @Test
    fun `InMemorySessionTokenStore correctly stores and clears session`() {
        val store = InMemorySessionTokenStore()
        assertNull(store.loadSession())

        val session = AuthSession(
            accessToken = "token-123",
            refreshToken = "refresh-456",
            expiresAtEpochSeconds = 1890000000L,
            user = UserProfile(
                id = "user-uuid-1",
                email = "crochet@stitchmesh.app",
                username = "crochet_dev",
                displayName = "Crochet Master"
            )
        )

        store.saveSession(session)
        val loaded = store.loadSession()

        assertNotNull(loaded)
        assertEquals("token-123", loaded?.accessToken)
        assertEquals("user-uuid-1", loaded?.user?.id)

        store.clearSession()
        assertNull(store.loadSession())
    }
}
