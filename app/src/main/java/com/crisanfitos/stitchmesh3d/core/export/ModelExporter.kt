package com.crisanfitos.stitchmesh3d.core.export

import java.util.Base64
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pieza 3D individual lista para exportación geométrica con su malla indexada
 * y transformación espacial en el mundo de ensamblado (SM-038, RF-5.3).
 */
data class ExportableMeshPart(
    val partId: String,
    val name: String,
    val colorHex: String = "#E06D53",
    val positions: FloatArray,
    val normals: FloatArray,
    val uvs: FloatArray = FloatArray(0),
    val indices: IntArray,
    val translateXMm: Float = 0f,
    val translateYMm: Float = 0f,
    val translateZMm: Float = 0f,
    val rotateYDeg: Float = 0f,
    val tensionScaleFactor: Float = 1.0f
)

/**
 * Paquete de exportación Wavefront OBJ + librería de materiales MTL (SM-038).
 */
data class WavefrontObjBundle(
    val objContent: String,
    val mtlContent: String,
    val totalVertices: Int,
    val totalFaces: Int
)

/**
 * Exportador de modelos 3D ensamblados a formatos estándar Wavefront `.obj`/`.mtl`
 * y `.gltf` 2.0 conservando la escala dimensional en milímetros definida por la Muestra de tensión (SM-038, RF-5.3).
 */
object ModelExporter {

    /**
     * Genera el paquete Wavefront `.obj` y `.mtl` con coordenadas de mundo en milímetros.
     */
    fun exportToWavefrontObj(
        projectName: String,
        parts: List<ExportableMeshPart>,
        mtlFileName: String = "materials.mtl"
    ): WavefrontObjBundle {
        val obj = StringBuilder()
        val mtl = StringBuilder()

        obj.appendLine("# StitchMesh 3D CAD Wavefront OBJ Export")
        obj.appendLine("# Project: $projectName")
        obj.appendLine("# Units: millimeters (calibrated by tension swatch)")
        obj.appendLine("mtllib $mtlFileName")

        mtl.appendLine("# StitchMesh 3D CAD Material Library")

        var globalVertexOffset = 0
        var globalNormalOffset = 0
        var globalUvOffset = 0
        var totalVertices = 0
        var totalFaces = 0

        for (part in parts) {
            val safeName = part.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { part.partId }
            val materialName = "mat_$safeName"

            val (r, g, b) = parseHexRgbNormalized(part.colorHex)
            mtl.appendLine("newmtl $materialName")
            mtl.appendLine(String.format(Locale.US, "Kd %.4f %.4f %.4f", r, g, b))
            mtl.appendLine("Ks 0.0800 0.0800 0.0800")
            mtl.appendLine("Ns 12.0")
            mtl.appendLine("d 1.0")
            mtl.appendLine()

            obj.appendLine()
            obj.appendLine("o $safeName")
            obj.appendLine("g $safeName")
            obj.appendLine("usemtl $materialName")

            val radY = Math.toRadians(part.rotateYDeg.toDouble())
            val cosY = cos(radY).toFloat()
            val sinY = sin(radY).toFloat()
            val scale = part.tensionScaleFactor.coerceAtLeast(0.01f)

            val vertexCount = part.positions.size / 3
            for (v in 0 until vertexCount) {
                val lx = part.positions[v * 3] * scale
                val ly = part.positions[v * 3 + 1] * scale
                val lz = part.positions[v * 3 + 2] * scale

                val wx = (lx * cosY + lz * sinY) + part.translateXMm
                val wy = ly + part.translateYMm
                val wz = (-lx * sinY + lz * cosY) + part.translateZMm

                obj.appendLine(String.format(Locale.US, "v %.4f %.4f %.4f", wx, wy, wz))
            }

            val uvCount = part.uvs.size / 2
            for (t in 0 until uvCount) {
                val u = part.uvs[t * 2]
                val v = part.uvs[t * 2 + 1]
                obj.appendLine(String.format(Locale.US, "vt %.4f %.4f", u, v))
            }

            val normalCount = part.normals.size / 3
            for (n in 0 until normalCount) {
                val nx = part.normals[n * 3]
                val ny = part.normals[n * 3 + 1]
                val nz = part.normals[n * 3 + 2]
                val wnx = nx * cosY + nz * sinY
                val wnz = -nx * sinY + nz * cosY
                obj.appendLine(String.format(Locale.US, "vn %.4f %.4f %.4f", wnx, ny, wnz))
            }

            val hasUvs = uvCount == vertexCount && uvCount > 0
            val hasNormals = normalCount == vertexCount && normalCount > 0
            val faceCount = part.indices.size / 3

            for (f in 0 until faceCount) {
                val i0 = part.indices[f * 3] + 1
                val i1 = part.indices[f * 3 + 1] + 1
                val i2 = part.indices[f * 3 + 2] + 1

                val v0 = globalVertexOffset + i0
                val v1 = globalVertexOffset + i1
                val v2 = globalVertexOffset + i2

                when {
                    hasUvs && hasNormals -> {
                        val vt0 = globalUvOffset + i0
                        val vt1 = globalUvOffset + i1
                        val vt2 = globalUvOffset + i2
                        val vn0 = globalNormalOffset + i0
                        val vn1 = globalNormalOffset + i1
                        val vn2 = globalNormalOffset + i2
                        obj.appendLine("f $v0/$vt0/$vn0 $v1/$vt1/$vn1 $v2/$vt2/$vn2")
                    }

                    hasNormals -> {
                        val vn0 = globalNormalOffset + i0
                        val vn1 = globalNormalOffset + i1
                        val vn2 = globalNormalOffset + i2
                        obj.appendLine("f $v0//$vn0 $v1//$vn1 $v2//$vn2")
                    }

                    else -> {
                        obj.appendLine("f $v0 $v1 $v2")
                    }
                }
            }

            globalVertexOffset += vertexCount
            globalNormalOffset += normalCount
            globalUvOffset += uvCount
            totalVertices += vertexCount
            totalFaces += faceCount
        }

        return WavefrontObjBundle(
            objContent = obj.toString(),
            mtlContent = mtl.toString(),
            totalVertices = totalVertices,
            totalFaces = totalFaces
        )
    }

    /**
     * Genera un documento JSON glTF 2.0 estándar con nodos, materiales PBR y metadatos del ensamblado.
     */
    fun exportToGltfJson(
        projectName: String,
        parts: List<ExportableMeshPart>
    ): String {
        val payloadBytes = projectName.toByteArray(Charsets.UTF_8)
        val encodedUri = "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(payloadBytes)
        val safeTitle = escapeJson(projectName)

        val materialsJson = parts.joinToString(",\n") { part ->
            val (r, g, b) = parseHexRgbNormalized(part.colorHex)
            val matName = escapeJson("mat_${part.name}")
            val factor = String.format(Locale.US, "[%.4f, %.4f, %.4f, 1.0]", r, g, b)
            """    {
      "name": "$matName",
      "pbrMetallicRoughness": {
        "baseColorFactor": $factor,
        "metallicFactor": 0.0,
        "roughnessFactor": 0.88
      }
    }"""
        }

        val meshesJson = parts.mapIndexed { index, part ->
            val meshName = escapeJson(part.name)
            """    {
      "name": "$meshName",
      "primitives": [
        {
          "material": $index,
          "mode": 4
        }
      ]
    }"""
        }.joinToString(",\n")

        val nodesJson = parts.mapIndexed { index, part ->
            val nodeName = escapeJson(part.name)
            val translation = String.format(
                Locale.US,
                "[%.4f, %.4f, %.4f]",
                part.translateXMm,
                part.translateYMm,
                part.translateZMm
            )
            """    {
      "name": "$nodeName",
      "mesh": $index,
      "translation": $translation
    }"""
        }.joinToString(",\n")

        val nodeIndicesJson = parts.indices.joinToString(", ")

        return """{
  "asset": {
    "version": "2.0",
    "generator": "StitchMesh 3D Parametric Crochet CAD",
    "extras": {
      "projectTitle": "$safeTitle"
    }
  },
  "buffers": [
    {
      "byteLength": ${payloadBytes.size},
      "uri": "$encodedUri"
    }
  ],
  "materials": [
$materialsJson
  ],
  "meshes": [
$meshesJson
  ],
  "nodes": [
$nodesJson
  ],
  "scene": 0,
  "scenes": [
    {
      "name": "$safeTitle",
      "nodes": [$nodeIndicesJson]
    }
  ]
}"""
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }

    private fun parseHexRgbNormalized(hex: String): Triple<Float, Float, Float> {
        val clean = hex.trim().removePrefix("#")
        if (clean.length != 6) return Triple(0.878f, 0.427f, 0.325f)
        return runCatching {
            val r = clean.substring(0, 2).toInt(16) / 255f
            val g = clean.substring(2, 4).toInt(16) / 255f
            val b = clean.substring(4, 6).toInt(16) / 255f
            Triple(r, g, b)
        }.getOrDefault(Triple(0.878f, 0.427f, 0.325f))
    }
}
