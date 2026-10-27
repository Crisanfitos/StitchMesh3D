package com.crisanfitos.stitchmesh3d.data.remote.auth

import com.crisanfitos.stitchmesh3d.data.remote.SupabaseConfig
import com.crisanfitos.stitchmesh3d.domain.model.AuthSession
import com.crisanfitos.stitchmesh3d.domain.model.UserProfile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

/**
 * Puerta de enlace de red hacia Supabase GoTrue (`/auth/v1`) y PostgREST (`/rest/v1/profiles`) (SM-036).
 */
interface SupabaseAuthGateway {
    suspend fun signUp(
        email: String,
        password: String,
        username: String,
        displayName: String?
    ): AuthSession

    suspend fun signInWithPassword(
        email: String,
        password: String
    ): AuthSession

    suspend fun refreshSession(refreshToken: String): AuthSession

    suspend fun upsertProfile(
        accessToken: String,
        profile: UserProfile
    ): UserProfile

    suspend fun signOut(accessToken: String)
}

/**
 * Implementación HTTPS nativa compatible con Supabase GoTrue v1 y PostgREST v1.
 */
@Singleton
class HttpUrlSupabaseAuthGateway @Inject constructor(
    private val config: SupabaseConfig
) : SupabaseAuthGateway {

    override suspend fun signUp(
        email: String,
        password: String,
        username: String,
        displayName: String?
    ): AuthSession {
        val payload = JSONObject().apply {
            put("email", email)
            put("password", password)
            put(
                "data",
                JSONObject().apply {
                    put("username", username)
                    if (!displayName.isNullOrBlank()) {
                        put("display_name", displayName)
                    }
                }
            )
        }
        val responseJson = executeJsonRequest(
            method = "POST",
            url = "${config.authEndpointUrl}/signup",
            body = payload.toString()
        )
        return parseAuthSession(responseJson, fallbackUsername = username, fallbackDisplayName = displayName)
    }

    override suspend fun signInWithPassword(
        email: String,
        password: String
    ): AuthSession {
        val payload = JSONObject().apply {
            put("email", email)
            put("password", password)
        }
        val responseJson = executeJsonRequest(
            method = "POST",
            url = "${config.authEndpointUrl}/token?grant_type=password",
            body = payload.toString()
        )
        return parseAuthSession(responseJson)
    }

    override suspend fun refreshSession(refreshToken: String): AuthSession {
        val payload = JSONObject().apply {
            put("refresh_token", refreshToken)
        }
        val responseJson = executeJsonRequest(
            method = "POST",
            url = "${config.authEndpointUrl}/token?grant_type=refresh_token",
            body = payload.toString()
        )
        return parseAuthSession(responseJson)
    }

    override suspend fun upsertProfile(
        accessToken: String,
        profile: UserProfile
    ): UserProfile {
        val payload = JSONObject().apply {
            put("id", profile.id)
            put("username", profile.username)
            put("display_name", profile.displayName)
        }
        val raw = executeRawRequest(
            method = "POST",
            url = "${config.restEndpointUrl}/profiles?on_conflict=id",
            body = payload.toString(),
            accessToken = accessToken,
            extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=representation")
        )
        val array = JSONArray(raw.ifBlank { "[]" })
        if (array.length() > 0) {
            val row = array.getJSONObject(0)
            return profile.copy(
                username = row.optString("username", profile.username),
                displayName = row.optString("display_name", profile.displayName ?: "").ifBlank { null }
            )
        }
        return profile
    }

    override suspend fun signOut(accessToken: String) {
        executeRawRequest(
            method = "POST",
            url = "${config.authEndpointUrl}/logout",
            body = null,
            accessToken = accessToken
        )
    }

    private fun parseAuthSession(
        json: JSONObject,
        fallbackUsername: String? = null,
        fallbackDisplayName: String? = null
    ): AuthSession {
        val userObj = json.optJSONObject("user") ?: json
        val userId = userObj.optString("id").ifBlank {
            error("Respuesta de Supabase Auth sin identificador de usuario")
        }
        val email = userObj.optString("email")
        val metadata = userObj.optJSONObject("user_metadata")
        val resolvedUsername = metadata?.optString("username")?.takeIf { it.isNotBlank() }
            ?: fallbackUsername
            ?: email.substringBefore('@').ifBlank { "user_${userId.take(6)}" }
        val resolvedDisplayName = metadata?.optString("display_name")?.takeIf { it.isNotBlank() }
            ?: fallbackDisplayName

        val accessToken = json.optString("access_token").ifBlank { "pending_confirmation_$userId" }
        val refreshToken = json.optString("refresh_token").ifBlank { "refresh_$userId" }
        val expiresIn = json.optLong("expires_in", 3600L)
        val expiresAt = json.optLong(
            "expires_at",
            (System.currentTimeMillis() / 1000L) + expiresIn
        )

        return AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochSeconds = expiresAt,
            user = UserProfile(
                id = userId,
                email = email,
                username = resolvedUsername,
                displayName = resolvedDisplayName
            )
        )
    }

    private fun executeJsonRequest(
        method: String,
        url: String,
        body: String?,
        accessToken: String? = null
    ): JSONObject {
        val raw = executeRawRequest(method, url, body, accessToken)
        return JSONObject(raw.ifBlank { "{}" })
    }

    private fun executeRawRequest(
        method: String,
        url: String,
        body: String?,
        accessToken: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): String {
        check(config.isReadyForNetwork) {
            "SupabaseConfig no está inicializado con SUPABASE_URL y SUPABASE_ANON_KEY"
        }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("apikey", config.supabaseAnonKey)
            setRequestProperty("Authorization", "Bearer ${accessToken ?: config.supabaseAnonKey}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
            if (body != null) {
                doOutput = true
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val responseBody = stream?.let {
            BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader -> reader.readText() }
        }.orEmpty()
        if (code !in 200..299) {
            throw IllegalStateException("Supabase HTTP $code: $responseBody")
        }
        return responseBody
    }
}
