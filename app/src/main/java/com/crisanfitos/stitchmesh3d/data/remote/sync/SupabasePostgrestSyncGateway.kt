package com.crisanfitos.stitchmesh3d.data.remote.sync

import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import com.crisanfitos.stitchmesh3d.data.remote.SupabaseConfig
import com.crisanfitos.stitchmesh3d.domain.model.PartTopologyType
import com.crisanfitos.stitchmesh3d.domain.model.PatternRound
import com.crisanfitos.stitchmesh3d.domain.model.Project
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import com.crisanfitos.stitchmesh3d.domain.model.ProjectStructureType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

/**
 * Puerta de enlace hacia Supabase PostgREST (`/rest/v1`) para sincronizar proyectos,
 * piezas 3D y vueltas de patrón (SM-037, RF-5.2).
 */
interface SupabasePostgrestSyncGateway {
    suspend fun fetchRemoteProjects(accessToken: String, userId: String): List<Project>
    suspend fun upsertProjectGraph(accessToken: String, userId: String, project: Project)
}

@Singleton
class HttpUrlSupabasePostgrestSyncGateway @Inject constructor(
    private val config: SupabaseConfig
) : SupabasePostgrestSyncGateway {

    override suspend fun fetchRemoteProjects(accessToken: String, userId: String): List<Project> {
        val raw = executeRequest(
            method = "GET",
            url = "${config.restEndpointUrl}/projects?user_id=eq.$userId&select=*,project_parts(*,pattern_rounds(*))",
            accessToken = accessToken
        )
        val array = JSONArray(raw.ifBlank { "[]" })
        return buildList {
            for (i in 0 until array.length()) {
                add(parseRemoteProject(array.getJSONObject(i)))
            }
        }
    }

    override suspend fun upsertProjectGraph(accessToken: String, userId: String, project: Project) {
        val projectJson = JSONObject().apply {
            put("id", project.id)
            put("user_id", userId)
            put("title", project.title)
            put("description", project.description)
            put("structure_type", project.structureType.code)
            put("yarn_weight_category", project.yarnWeightCategory.code)
            put("hook_size_mm", project.hookSizeMm.toDouble())
            put("custom_stitch_width_mm", project.customStitchWidthMm?.toDouble())
            put("custom_stitch_height_mm", project.customStitchHeightMm?.toDouble())
            put("is_valid", project.isValid)
            put("updated_at", Instant.ofEpochMilli(project.updatedAt).toString())
        }

        executeRequest(
            method = "POST",
            url = "${config.restEndpointUrl}/projects?on_conflict=id",
            accessToken = accessToken,
            body = projectJson.toString(),
            extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates")
        )

        for (part in project.parts) {
            val partJson = JSONObject().apply {
                put("id", part.id)
                put("project_id", project.id)
                put("name", part.name)
                put("topology_type", part.topologyType.code)
                put("sort_order", part.sortOrder)
                put("transform_position_x", part.transformPositionX.toDouble())
                put("transform_position_y", part.transformPositionY.toDouble())
                put("transform_position_z", part.transformPositionZ.toDouble())
                put("transform_rotation_x", part.transformRotationX.toDouble())
                put("transform_rotation_y", part.transformRotationY.toDouble())
                put("transform_rotation_z", part.transformRotationZ.toDouble())
                put("is_valid", part.isValid)
            }
            executeRequest(
                method = "POST",
                url = "${config.restEndpointUrl}/project_parts?on_conflict=id",
                accessToken = accessToken,
                body = partJson.toString(),
                extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates")
            )

            if (part.rounds.isNotEmpty()) {
                val roundsArray = JSONArray()
                part.rounds.forEach { round ->
                    roundsArray.put(
                        JSONObject().apply {
                            put("id", round.id)
                            put("part_id", part.id)
                            put("round_number", round.roundNumber)
                            put("raw_instruction", round.rawInstruction)
                            put("color_hex", round.colorHex)
                            put("consumed_stitches", round.consumedStitches)
                            put("produced_stitches", round.producedStitches)
                            put("declared_stitches", round.declaredStitches)
                            put("is_valid", round.isValid)
                            put("error_message", round.errorMessage)
                        }
                    )
                }
                executeRequest(
                    method = "POST",
                    url = "${config.restEndpointUrl}/pattern_rounds?on_conflict=id",
                    accessToken = accessToken,
                    body = roundsArray.toString(),
                    extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates")
                )
            }
        }
    }

    private fun parseRemoteProject(obj: JSONObject): Project {
        val partsArray = obj.optJSONArray("project_parts") ?: JSONArray()
        val parts = buildList {
            for (i in 0 until partsArray.length()) {
                val pObj = partsArray.getJSONObject(i)
                val roundsArray = pObj.optJSONArray("pattern_rounds") ?: JSONArray()
                val rounds = buildList {
                    for (j in 0 until roundsArray.length()) {
                        val rObj = roundsArray.getJSONObject(j)
                        add(
                            PatternRound(
                                id = rObj.getString("id"),
                                partId = rObj.getString("part_id"),
                                roundNumber = rObj.optInt("round_number", j + 1),
                                rawInstruction = rObj.optString("raw_instruction", ""),
                                colorHex = rObj.optString("color_hex", "#E06D53"),
                                consumedStitches = rObj.optInt("consumed_stitches", 0),
                                producedStitches = rObj.optInt("produced_stitches", 0),
                                declaredStitches = if (rObj.isNull("declared_stitches")) null else rObj.optInt("declared_stitches"),
                                isValid = rObj.optBoolean("is_valid", false),
                                errorMessage = rObj.optString("error_message", "").ifBlank { null }
                            )
                        )
                    }
                }.sortedBy { it.roundNumber }

                add(
                    ProjectPart(
                        id = pObj.getString("id"),
                        projectId = pObj.getString("project_id"),
                        name = pObj.optString("name", "Pieza"),
                        topologyType = PartTopologyType.fromCode(pObj.optString("topology_type", "closed_filled")),
                        sortOrder = pObj.optInt("sort_order", i),
                        transformPositionX = pObj.optDouble("transform_position_x", 0.0).toFloat(),
                        transformPositionY = pObj.optDouble("transform_position_y", 0.0).toFloat(),
                        transformPositionZ = pObj.optDouble("transform_position_z", 0.0).toFloat(),
                        transformRotationX = pObj.optDouble("transform_rotation_x", 0.0).toFloat(),
                        transformRotationY = pObj.optDouble("transform_rotation_y", 0.0).toFloat(),
                        transformRotationZ = pObj.optDouble("transform_rotation_z", 0.0).toFloat(),
                        isValid = pObj.optBoolean("is_valid", false),
                        rounds = rounds
                    )
                )
            }
        }.sortedBy { it.sortOrder }

        val updatedAtMillis = runCatching {
            Instant.parse(obj.optString("updated_at")).toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())

        val createdAtMillis = runCatching {
            Instant.parse(obj.optString("created_at")).toEpochMilli()
        }.getOrDefault(updatedAtMillis)

        return Project(
            id = obj.getString("id"),
            title = obj.optString("title", "Proyecto"),
            description = obj.optString("description", "").ifBlank { null },
            structureType = ProjectStructureType.fromCode(obj.optString("structure_type", "amigurumi_3d")),
            yarnWeightCategory = YarnWeightCategory.fromCode(obj.optInt("yarn_weight_category", 4)),
            hookSizeMm = obj.optDouble("hook_size_mm", 3.5).toFloat(),
            customStitchWidthMm = if (obj.isNull("custom_stitch_width_mm")) null else obj.optDouble("custom_stitch_width_mm").toFloat(),
            customStitchHeightMm = if (obj.isNull("custom_stitch_height_mm")) null else obj.optDouble("custom_stitch_height_mm").toFloat(),
            isValid = obj.optBoolean("is_valid", false),
            createdAt = createdAtMillis,
            updatedAt = updatedAtMillis,
            parts = parts
        )
    }

    private fun executeRequest(
        method: String,
        url: String,
        accessToken: String,
        body: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("apikey", config.supabaseAnonKey)
            setRequestProperty("Authorization", "Bearer $accessToken")
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
            throw IllegalStateException("Supabase PostgREST HTTP $code: $responseBody")
        }
        return responseBody
    }
}
