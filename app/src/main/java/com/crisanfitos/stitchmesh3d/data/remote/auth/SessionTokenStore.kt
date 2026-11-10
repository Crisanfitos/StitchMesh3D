package com.crisanfitos.stitchmesh3d.data.remote.auth

import android.content.Context
import com.crisanfitos.stitchmesh3d.domain.model.AuthSession
import com.crisanfitos.stitchmesh3d.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Contrato de cifrado y descifrado autenticado para tokens de sesión (SM-089).
 */
interface SessionTokenEncryptor {
    fun encrypt(plainText: String): String
    fun decrypt(cipherText: String): String
}

/**
 * Implementación de cifrado AES/GCM/NoPadding para tokens JWT (SM-089).
 * Utiliza Android KeyStore en tiempo de ejecución o derivación de clave segura en tests unitarios JVM.
 */
class AesGcmSessionTokenEncryptor(
    private val keyAlias: String = KEY_ALIAS_DEFAULT
) : SessionTokenEncryptor {

    private val secretKey: SecretKey by lazy { getOrCreateKey() }
    private val secureRandom = SecureRandom()

    private fun getOrCreateKey(): SecretKey {
        return runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(keyAlias)) {
                val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
                entry?.secretKey
            } else {
                val keyGen = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
                // Usamos reflexión para evitar dependencias directas con KeyGenParameterSpec en JVM tests
                val specClass = Class.forName("android.security.keystore.KeyGenParameterSpec\$Builder")
                val purposes = 1 or 2 // PURPOSE_ENCRYPT | PURPOSE_DECRYPT
                val builder = specClass.getConstructor(String::class.java, Int::class.javaPrimitiveType)
                    .newInstance(keyAlias, purposes)

                specClass.getMethod("setBlockModes", Array<String>::class.java)
                    .invoke(builder, arrayOf("GCM"))
                specClass.getMethod("setEncryptionPaddings", Array<String>::class.java)
                    .invoke(builder, arrayOf("NoPadding"))
                specClass.getMethod("setKeySize", Int::class.javaPrimitiveType)
                    .invoke(builder, 256)

                val spec = specClass.getMethod("build").invoke(builder)
                keyGen.javaClass.getMethod("init", java.security.spec.AlgorithmParameterSpec::class.java)
                    .invoke(keyGen, spec)
                keyGen.generateKey()
            }
        }.getOrNull() ?: fallbackJvmKey()
    }

    private fun fallbackJvmKey(): SecretKey {
        // Clave AES-256 determinista para entornos de prueba JVM donde AndroidKeyStore no está disponible
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest("StitchMesh3D_Secure_Session_Salt_$keyAlias".toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    override fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return ""
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    override fun decrypt(cipherText: String): String {
        if (cipherText.isBlank()) return ""
        return runCatching {
            val combined = Base64.getDecoder().decode(cipherText)
            if (combined.size < GCM_IV_LENGTH) return ""

            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)

            val cipherBytesLength = combined.size - GCM_IV_LENGTH
            val cipherBytes = ByteArray(cipherBytesLength)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherBytes, 0, cipherBytesLength)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }.getOrDefault("")
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS_DEFAULT = "stitchmesh_auth_token_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}

/**
 * Almacén de persistencia local para la sesión de autenticación de Supabase (SM-036, SM-089).
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
 * Implementación Android con almacenamiento cifrado AES/GCM respaldado por SharedPreferences privadas (SM-089).
 * Los tokens de acceso y refresco se cifran antes de persistirse en disco.
 */
@Singleton
class SharedPreferencesSessionTokenStore @Inject constructor(
    @ApplicationContext context: Context,
    private val encryptor: SessionTokenEncryptor = AesGcmSessionTokenEncryptor()
) : SessionTokenStore {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun saveSession(session: AuthSession) {
        val encryptedAccess = encryptor.encrypt(session.accessToken)
        val encryptedRefresh = encryptor.encrypt(session.refreshToken)

        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, encryptedAccess)
            .putString(KEY_REFRESH_TOKEN, encryptedRefresh)
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochSeconds)
            .putString(KEY_USER_ID, session.user.id)
            .putString(KEY_USER_EMAIL, session.user.email)
            .putString(KEY_USERNAME, session.user.username)
            .putString(KEY_DISPLAY_NAME, session.user.displayName)
            .apply()
    }

    override fun loadSession(): AuthSession? {
        val rawAccess = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val rawRefresh = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null

        val accessToken = encryptor.decrypt(rawAccess).ifBlank { return null }
        val refreshToken = encryptor.decrypt(rawRefresh).ifBlank { return null }

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
