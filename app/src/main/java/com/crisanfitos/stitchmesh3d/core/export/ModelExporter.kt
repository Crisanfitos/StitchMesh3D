package com.crisanfitos.stitchmesh3d.core.export

import com.crisanfitos.stitchmesh3d.core.geometry.TransformMatrix4x4
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import java.util.Locale

/**
 * Pieza 3D individual lista para exportación geométrica con su malla indexada
 * y transformación espacial en el mundo de ensamblado (SM-038, SM-088, RF-5.3).
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
    val rotateXDeg: Float = 0f,
    val rotateYDeg: Float = 0f,
    val rotateZDeg: Float = 0f,
    val isMirrored: Boolean = false,
    val tensionScaleFactor: Float = 1.0f
)

/**
 * Paquete de exportación Wavefront OBJ + librería de materiales MTL (SM-038, SM-088).
 */
data class WavefrontObjBundle(
    val objContent: String,
    val mtlContent: String,
    val totalVertices: Int,
    val totalFaces: Int
)

/**
 * Exportador de modelos 3D ensamblados a formatos estándar Wavefront `.obj`/`.mtl`
 * y `.gltf` 2.0 / `.glb` conservando la escala dimensional en milímetros definida por la Muestra de tensión (SM-038, SM-088, RF-5.3).
 */
object ModelExporter {

    /**
     * Aplica la matriz de transformación 4x4 (traslación, rotación Euler XYZ, escala y simetría)
     * a una [ExportableMeshPart], transformando posiciones y rotando normales con la transpuesta de la inversa.
     */
    fun applyTransformToMesh(part: ExportableMeshPart): ExportableMeshPart {
        val scale = part.tensionScaleFactor.coerceAtLeast(0.01f)
        val matrix = TransformMatrix4x4.fromTransform(
            posX = part.translateXMm,
            posY = part.translateYMm,
            posZ = part.translateZMm,
            rotXDeg = part.rotateXDeg,
            rotYDeg = part.rotateYDeg,
            rotZDeg = part.rotateZDeg,
            scaleX = scale,
            scaleY = scale,
            scaleZ = scale,
            isMirrored = part.isMirrored
        )

        val vertexCount = part.positions.size / 3
        val newPositions = FloatArray(part.positions.size)
        for (v in 0 until vertexCount) {
            val px = part.positions[v * 3]
            val py = part.positions[v * 3 + 1]
            val pz = part.positions[v * 3 + 2]
            val p = matrix.transformPoint(px, py, pz)
            newPositions[v * 3] = p[0]
            newPositions[v * 3 + 1] = p[1]
            newPositions[v * 3 + 2] = p[2]
        }

        val normalCount = part.normals.size / 3
        val newNormals = FloatArray(part.normals.size)
        for (n in 0 until normalCount) {
            val nx = part.normals[n * 3]
            val ny = part.normals[n * 3 + 1]
            val nz = part.normals[n * 3 + 2]
            val transformedN = matrix.transformNormal(nx, ny, nz)
            newNormals[n * 3] = transformedN[0]
            newNormals[n * 3 + 1] = transformedN[1]
            newNormals[n * 3 + 2] = transformedN[2]
        }

        return part.copy(
            positions = newPositions,
            normals = newNormals,
            translateXMm = 0f,
            translateYMm = 0f,
            translateZMm = 0f,
            rotateXDeg = 0f,
            rotateYDeg = 0f,
            rotateZDeg = 0f,
            isMirrored = false,
            tensionScaleFactor = 1.0f
        )
    }

    /**
     * Genera el paquete Wavefront `.obj` y `.mtl` con coordenadas de mundo en milímetros
     * y vectores normales unitarios correctamente rotados en 3D.
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

        for (rawPart in parts) {
            val part = applyTransformToMesh(rawPart)
            val safeName = rawPart.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { rawPart.partId }
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

            val vertexCount = part.positions.size / 3
            for (v in 0 until vertexCount) {
                val wx = part.positions[v * 3]
                val wy = part.positions[v * 3 + 1]
                val wz = part.positions[v * 3 + 2]
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
                obj.appendLine(String.format(Locale.US, "vn %.4f %.4f %.4f", nx, ny, nz))
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
     * Estructura interna para empaquetar los buffers geométricos binarios de glTF 2.0.
     */
    private data class GltfBinaryPayload(
        val binaryBytes: ByteArray,
        val bufferViewsJson: String,
        val accessorsJson: String,
        val meshesJson: String
    )

    private fun buildGltfPayload(parts: List<ExportableMeshPart>): GltfBinaryPayload {
        val outStream = ByteArrayOutputStream()
        val bufferViews = mutableListOf<String>()
        val accessors = mutableListOf<String>()
        val meshes = mutableListOf<String>()

        var currentOffset = 0
        var bufferViewIndex = 0
        var accessorIndex = 0

        parts.forEachIndexed { partIndex, part ->
            val scale = part.tensionScaleFactor.coerceAtLeast(0.01f)
            val vertexCount = part.positions.size / 3
            val normalCount = part.normals.size / 3
            val uvCount = part.uvs.size / 2
            val indexCount = part.indices.size

            // 1. POSITION buffer view & accessor
            var minX = Float.POSITIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var minZ = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY
            var maxZ = Float.NEGATIVE_INFINITY

            val posByteBuffer = ByteBuffer.allocate(part.positions.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until vertexCount) {
                val px = part.positions[i * 3] * scale
                val py = part.positions[i * 3 + 1] * scale
                val pz = part.positions[i * 3 + 2] * scale
                posByteBuffer.putFloat(px)
                posByteBuffer.putFloat(py)
                posByteBuffer.putFloat(pz)
                if (px < minX) minX = px
                if (py < minY) minY = py
                if (pz < minZ) minZ = pz
                if (px > maxX) maxX = px
                if (py > maxY) maxY = py
                if (pz > maxZ) maxZ = pz
            }
            if (vertexCount == 0) {
                minX = 0f; minY = 0f; minZ = 0f; maxX = 0f; maxY = 0f; maxZ = 0f
            }
            val posBytes = posByteBuffer.array()
            val posViewOffset = currentOffset
            val posViewLength = posBytes.size
            outStream.write(posBytes)
            currentOffset += posViewLength

            val posViewIdx = bufferViewIndex++
            bufferViews.add(
                """    {
      "buffer": 0,
      "byteOffset": $posViewOffset,
      "byteLength": $posViewLength,
      "target": 34962
    }"""
            )

            val posAccIdx = accessorIndex++
            accessors.add(
                """    {
      "bufferView": $posViewIdx,
      "byteOffset": 0,
      "componentType": 5126,
      "count": $vertexCount,
      "type": "VEC3",
      "max": [${String.format(Locale.US, "%.4f, %.4f, %.4f", maxX, maxY, maxZ)}],
      "min": [${String.format(Locale.US, "%.4f, %.4f, %.4f", minX, minY, minZ)}]
    }"""
            )

            // 2. NORMAL buffer view & accessor
            val normByteBuffer = ByteBuffer.allocate(part.normals.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until normalCount) {
                normByteBuffer.putFloat(part.normals[i * 3])
                normByteBuffer.putFloat(part.normals[i * 3 + 1])
                normByteBuffer.putFloat(part.normals[i * 3 + 2])
            }
            val normBytes = normByteBuffer.array()
            val normViewOffset = currentOffset
            val normViewLength = normBytes.size
            outStream.write(normBytes)
            currentOffset += normViewLength

            val normViewIdx = bufferViewIndex++
            bufferViews.add(
                """    {
      "buffer": 0,
      "byteOffset": $normViewOffset,
      "byteLength": $normViewLength,
      "target": 34962
    }"""
            )

            val normAccIdx = accessorIndex++
            accessors.add(
                """    {
      "bufferView": $normViewIdx,
      "byteOffset": 0,
      "componentType": 5126,
      "count": $normalCount,
      "type": "VEC3"
    }"""
            )

            // 3. Optional TEXCOORD_0
            var uvAccIdx: Int? = null
            if (uvCount == vertexCount && uvCount > 0) {
                val uvByteBuffer = ByteBuffer.allocate(part.uvs.size * 4).order(ByteOrder.LITTLE_ENDIAN)
                for (i in 0 until uvCount) {
                    uvByteBuffer.putFloat(part.uvs[i * 2])
                    uvByteBuffer.putFloat(part.uvs[i * 2 + 1])
                }
                val uvBytes = uvByteBuffer.array()
                val uvViewOffset = currentOffset
                val uvViewLength = uvBytes.size
                outStream.write(uvBytes)
                currentOffset += uvViewLength

                val uvViewIdx = bufferViewIndex++
                bufferViews.add(
                    """    {
      "buffer": 0,
      "byteOffset": $uvViewOffset,
      "byteLength": $uvViewLength,
      "target": 34962
    }"""
                )

                uvAccIdx = accessorIndex++
                accessors.add(
                    """    {
      "bufferView": $uvViewIdx,
      "byteOffset": 0,
      "componentType": 5126,
      "count": $uvCount,
      "type": "VEC2"
    }"""
                )
            }

            // 4. INDICES buffer view & accessor
            val idxByteBuffer = ByteBuffer.allocate(indexCount * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (idx in part.indices) {
                idxByteBuffer.putInt(idx)
            }
            val idxBytes = idxByteBuffer.array()
            val idxViewOffset = currentOffset
            val idxViewLength = idxBytes.size
            outStream.write(idxBytes)
            currentOffset += idxViewLength

            val idxViewIdx = bufferViewIndex++
            bufferViews.add(
                """    {
      "buffer": 0,
      "byteOffset": $idxViewOffset,
      "byteLength": $idxViewLength,
      "target": 34963
    }"""
            )

            val idxAccIdx = accessorIndex++
            accessors.add(
                """    {
      "bufferView": $idxViewIdx,
      "byteOffset": 0,
      "componentType": 5125,
      "count": $indexCount,
      "type": "SCALAR"
    }"""
            )

            // Primitive definition
            val meshName = escapeJson(part.name)
            val uvAttribute = if (uvAccIdx != null) ",\n          \"TEXCOORD_0\": $uvAccIdx" else ""
            meshes.add(
                """    {
      "name": "$meshName",
      "primitives": [
        {
          "attributes": {
            "POSITION": $posAccIdx,
            "NORMAL": $normAccIdx$uvAttribute
          },
          "indices": $idxAccIdx,
          "material": $partIndex,
          "mode": 4
        }
      ]
    }"""
            )
        }

        return GltfBinaryPayload(
            binaryBytes = outStream.toByteArray(),
            bufferViewsJson = bufferViews.joinToString(",\n"),
            accessorsJson = accessors.joinToString(",\n"),
            meshesJson = meshes.joinToString(",\n")
        )
    }

    /**
     * Genera un documento JSON glTF 2.0 estándar con búferes geométricos binarios reales
     * embebidos en Base64, accesores de posición, normal e índice, nodos y materiales PBR (SM-038, SM-088, RF-5.3).
     */
    fun exportToGltfJson(
        projectName: String,
        parts: List<ExportableMeshPart>
    ): String {
        val payload = buildGltfPayload(parts)
        val encodedUri = "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(payload.binaryBytes)
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
      "byteLength": ${payload.binaryBytes.size},
      "uri": "$encodedUri"
    }
  ],
  "bufferViews": [
${payload.bufferViewsJson}
  ],
  "accessors": [
${payload.accessorsJson}
  ],
  "materials": [
$materialsJson
  ],
  "meshes": [
${payload.meshesJson}
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

    /**
     * Genera un archivo binario glTF 2.0 (.glb) estándar autónomo con cabecera de 12 bytes,
     * chunk JSON estructurado y chunk binario alineado a 4 bytes (SM-088).
     */
    fun exportToGlb(
        projectName: String,
        parts: List<ExportableMeshPart>
    ): ByteArray {
        val payload = buildGltfPayload(parts)
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

        val jsonText = """{
  "asset": {
    "version": "2.0",
    "generator": "StitchMesh 3D Parametric Crochet CAD",
    "extras": {
      "projectTitle": "$safeTitle"
    }
  },
  "buffers": [
    {
      "byteLength": ${payload.binaryBytes.size}
    }
  ],
  "bufferViews": [
${payload.bufferViewsJson}
  ],
  "accessors": [
${payload.accessorsJson}
  ],
  "materials": [
$materialsJson
  ],
  "meshes": [
${payload.meshesJson}
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

        val jsonBytes = jsonText.toByteArray(Charsets.UTF_8)
        val jsonPadding = (4 - (jsonBytes.size % 4)) % 4
        val paddedJsonLength = jsonBytes.size + jsonPadding

        val binPadding = (4 - (payload.binaryBytes.size % 4)) % 4
        val paddedBinLength = payload.binaryBytes.size + binPadding

        val totalLength = 12 + 8 + paddedJsonLength + 8 + paddedBinLength
        val glbBuffer = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN)

        // GLB Header
        glbBuffer.putInt(0x46546C67) // "glTF"
        glbBuffer.putInt(2)          // version 2
        glbBuffer.putInt(totalLength)

        // Chunk 0: JSON
        glbBuffer.putInt(paddedJsonLength)
        glbBuffer.putInt(0x4E4F534A) // "JSON"
        glbBuffer.put(jsonBytes)
        repeat(jsonPadding) { glbBuffer.put(0x20.toByte()) } // space padding

        // Chunk 1: BIN
        glbBuffer.putInt(paddedBinLength)
        glbBuffer.putInt(0x004E4942) // "BIN\0"
        glbBuffer.put(payload.binaryBytes)
        repeat(binPadding) { glbBuffer.put(0x00.toByte()) } // null padding

        return glbBuffer.array()
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
