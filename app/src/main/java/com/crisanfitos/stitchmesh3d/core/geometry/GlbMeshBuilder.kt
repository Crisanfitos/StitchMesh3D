package com.crisanfitos.stitchmesh3d.core.geometry

import androidx.compose.ui.graphics.Color
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Generador procedural de archivos binarios glTF 2.0 (.glb) a partir de [MeshGeometry].
 *
 * Implementa la especificación oficial Khronos glTF 2.0:
 * - Material Wool PBR: `roughnessFactor = 0.9` (textil mate), `metallicFactor = 0.0`, `doubleSided = true`.
 * - Búfer binario con atributos POSITION (Float3), NORMAL (Float3) e INDICES (Unsigned Short).
 * - Alineación estricta de 4 bytes en encabezados y chunks.
 * - Compatible directamente con Google Filament / Sceneview [com.google.android.filament.utils.ModelViewer.loadModelGlb].
 */
object GlbMeshBuilder {

    private const val GLB_MAGIC = 0x46546C67 // "glTF" en ASCII Little-Endian
    private const val GLB_VERSION = 2
    private const val JSON_CHUNK_TYPE = 0x4E4F534A // "JSON"
    private const val BIN_CHUNK_TYPE = 0x004E4942 // "BIN\0"

    /**
     * Construye un [ByteBuffer] directo que contiene un archivo .glb válido listo para Filament.
     *
     * @param mesh Geometría triangulada con posiciones, normales e índices.
     * @param yarnColor Color base difuso de la lana (por defecto Terracotta).
     * @param roughness Factor de rugosidad PBR (0.9 para lana/textil).
     * @return [ByteBuffer] directo posicionado en 0 con capacidad exacta del GLB.
     */
    fun buildGlb(
        mesh: MeshGeometry,
        yarnColor: Color = Color(0xFFE06D53),
        roughness: Float = 0.9f
    ): ByteBuffer {
        val vertexCount = mesh.vertexCount
        val indexCount = mesh.indices.size

        // Calcular límites de bounding box para el accessor POSITION
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        for (i in 0 until vertexCount) {
            val x = mesh.vertexPositions[i * 3]
            val y = mesh.vertexPositions[i * 3 + 1]
            val z = mesh.vertexPositions[i * 3 + 2]

            if (x < minX) minX = x
            if (y < minY) minY = y
            if (z < minZ) minZ = z
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
            if (z > maxZ) maxZ = z
        }

        if (vertexCount == 0) {
            minX = 0f; minY = 0f; minZ = 0f
            maxX = 0f; maxY = 0f; maxZ = 0f
        }

        // Longitudes de datos binarios
        val posByteLength = vertexCount * 3 * 4
        val normByteLength = vertexCount * 3 * 4
        val rawIndByteLength = indexCount * 2
        // Alinear longitud de índices a múltiplo de 4
        val indPadding = if (rawIndByteLength % 4 != 0) 4 - (rawIndByteLength % 4) else 0
        val indByteLength = rawIndByteLength + indPadding

        val posOffset = 0
        val normOffset = posByteLength
        val indOffset = posByteLength + normByteLength
        val totalBinLength = indOffset + indByteLength

        // Color normalizado RGBA
        val r = yarnColor.red
        val g = yarnColor.green
        val b = yarnColor.blue

        // Construir JSON metadata de glTF 2.0
        val jsonString = """
{
  "asset": {
    "version": "2.0",
    "generator": "StitchMesh3D Wool PBR Generator"
  },
  "scene": 0,
  "scenes": [
    {
      "nodes": [0]
    }
  ],
  "nodes": [
    {
      "mesh": 0
    }
  ],
  "materials": [
    {
      "name": "WoolPbrMaterial",
      "pbrMetallicRoughness": {
        "baseColorFactor": [$r, $g, $b, 1.0],
        "roughnessFactor": $roughness,
        "metallicFactor": 0.0
      },
      "doubleSided": true
    }
  ],
  "meshes": [
    {
      "primitives": [
        {
          "attributes": {
            "POSITION": 0,
            "NORMAL": 1
          },
          "indices": 2,
          "material": 0,
          "mode": 4
        }
      ]
    }
  ],
  "accessors": [
    {
      "bufferView": 0,
      "byteOffset": 0,
      "componentType": 5126,
      "count": $vertexCount,
      "type": "VEC3",
      "min": [$minX, $minY, $minZ],
      "max": [$maxX, $maxY, $maxZ]
    },
    {
      "bufferView": 1,
      "byteOffset": 0,
      "componentType": 5126,
      "count": $vertexCount,
      "type": "VEC3"
    },
    {
      "bufferView": 2,
      "byteOffset": 0,
      "componentType": 5123,
      "count": $indexCount,
      "type": "SCALAR"
    }
  ],
  "bufferViews": [
    {
      "buffer": 0,
      "byteOffset": $posOffset,
      "byteLength": $posByteLength,
      "target": 34962
    },
    {
      "buffer": 0,
      "byteOffset": $normOffset,
      "byteLength": $normByteLength,
      "target": 34962
    },
    {
      "buffer": 0,
      "byteOffset": $indOffset,
      "byteLength": $rawIndByteLength,
      "target": 34963
    }
  ],
  "buffers": [
    {
      "byteLength": $totalBinLength
    }
  ]
}
""".trim()

        val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)
        val jsonPadding = if (jsonBytes.size % 4 != 0) 4 - (jsonBytes.size % 4) else 0
        val paddedJsonLength = jsonBytes.size + jsonPadding

        // Tamaño total del contenedor GLB:
        // 12 (Header) + 8 (JSON Chunk Header) + paddedJsonLength + 8 (BIN Chunk Header) + totalBinLength
        val totalGlbLength = 12 + 8 + paddedJsonLength + 8 + totalBinLength

        val byteBuffer = ByteBuffer.allocateDirect(totalGlbLength)
            .order(ByteOrder.LITTLE_ENDIAN)

        // 1. GLB Header (12 bytes)
        byteBuffer.putInt(GLB_MAGIC)
        byteBuffer.putInt(GLB_VERSION)
        byteBuffer.putInt(totalGlbLength)

        // 2. JSON Chunk Header (8 bytes)
        byteBuffer.putInt(paddedJsonLength)
        byteBuffer.putInt(JSON_CHUNK_TYPE)
        byteBuffer.put(jsonBytes)
        // Rellenar padding con espacios (0x20) según spec glTF
        repeat(jsonPadding) {
            byteBuffer.put(0x20.toByte())
        }

        // 3. BIN Chunk Header (8 bytes)
        byteBuffer.putInt(totalBinLength)
        byteBuffer.putInt(BIN_CHUNK_TYPE)

        // Payload binario: POSITIONS (Float3)
        for (i in 0 until (vertexCount * 3)) {
            val p = if (i < mesh.vertexPositions.size) mesh.vertexPositions[i] else 0f
            byteBuffer.putFloat(p)
        }

        // Payload binario: NORMALS (Float3)
        for (i in 0 until (vertexCount * 3)) {
            val n = if (i < mesh.vertexNormals.size) mesh.vertexNormals[i] else 0f
            byteBuffer.putFloat(n)
        }

        // Payload binario: INDICES (Unsigned Short)
        for (i in 0 until indexCount) {
            byteBuffer.putShort(mesh.indices[i])
        }

        // Rellenar padding binario con ceros (0x00)
        repeat(indPadding) {
            byteBuffer.put(0x00.toByte())
        }

        byteBuffer.flip()
        return byteBuffer
    }
}
