package com.crisanfitos.stitchmesh3d.core.geometry

import androidx.compose.ui.graphics.Color
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Generador procedural de archivos binarios glTF 2.0 (.glb) a partir de [MeshGeometry].
 *
 * Implementa la especificación oficial Khronos glTF 2.0:
 * - Material Wool PBR: `roughnessFactor = 0.9` (textil mate), `metallicFactor = 0.0`, `doubleSided = true`.
 * - Modo Wireframe: compatible con Google Filament / Sceneview usando la extensión `KHR_materials_unlit`
 *   y primitivas `LINES` (mode 1) sin atributos de normales de superficie para prevenir cierres inesperados.
 * - Mapeado de textura de relieve y oclusión ambiental de puntadas de crochet (PBR Stitch Relief)
 *   mediante atributo `TEXCOORD_0`, muestreador con repetición (`REPEAT`), y estampa PNG incrustada
 *   modulando reactivamente el color de la lana en Filament (`baseColorTexture`).
 * - Búfer binario con alineación estricta de 4 bytes en encabezados, chunks y bufferViews.
 * - Compatible directamente con Google Filament [com.google.android.filament.utils.ModelViewer.loadModelGlb].
 */
object GlbMeshBuilder {

    private const val GLB_MAGIC = 0x46546C67 // "glTF" en ASCII Little-Endian
    private const val GLB_VERSION = 2
    private const val JSON_CHUNK_TYPE = 0x4E4F534A // "JSON"
    private const val BIN_CHUNK_TYPE = 0x004E4942 // "BIN\0"

    /**
     * Construye un [ByteBuffer] directo que contiene un archivo .glb válido listo para Filament.
     *
     * @param mesh Geometría triangulada con posiciones, normales, coordenadas UV e índices.
     * @param yarnColor Color base difuso de la lana (por defecto Terracotta).
     * @param roughness Factor de rugosidad PBR (0.9 para lana/textil).
     * @param isWireframe Si es true, genera aristas glTF LINES con shader Unlit en SageGreen.
     * @param stitchTexturePng Imagen PNG personalizada para el relieve del punto (por defecto la estampa de [CrochetTextureGenerator]).
     * @return [ByteBuffer] directo posicionado en 0 con capacidad exacta del GLB.
     */
    fun buildGlb(
        mesh: MeshGeometry,
        yarnColor: Color = Color(0xFFE06D53),
        roughness: Float = 0.9f,
        isWireframe: Boolean = false,
        stitchTexturePng: ByteArray? = null,
        normalTexturePng: ByteArray? = null,
        occlusionRoughnessPng: ByteArray? = null
    ): ByteBuffer {
        val vertexCount = mesh.vertexCount

        // Si es modo alambre (wireframe), descomponer triángulos en aristas (glTF LINES: 2 vértices por segmento)
        val indicesList = if (isWireframe && mesh.indices.isNotEmpty()) {
            val lineIndices = IntArray(mesh.indices.size * 2)
            var outIdx = 0
            for (i in 0 until (mesh.indices.size / 3)) {
                val a = mesh.indices[i * 3]
                val b = mesh.indices[i * 3 + 1]
                val c = mesh.indices[i * 3 + 2]
                lineIndices[outIdx++] = a
                lineIndices[outIdx++] = b
                lineIndices[outIdx++] = b
                lineIndices[outIdx++] = c
                lineIndices[outIdx++] = c
                lineIndices[outIdx++] = a
            }
            lineIndices
        } else {
            mesh.indices
        }
        val indexCount = indicesList.size

        // Soporte dinámico glTF 2.0 para mallas de alta densidad (> 65.535 vértices):
        // 5123 = UNSIGNED_SHORT (2 bytes), 5125 = UNSIGNED_INT (4 bytes)
        val use32BitIndices = vertexCount > 65535 || indicesList.any { it > 65535 }
        val indexComponentType = if (use32BitIndices) 5125 else 5123
        val indexBytesPerElement = if (use32BitIndices) 4 else 2

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

        // Longitudes de datos binarios base
        val posByteLength = vertexCount * 3 * 4
        val rawIndByteLength = indexCount * indexBytesPerElement
        val indPadding = if (rawIndByteLength % 4 != 0) 4 - (rawIndByteLength % 4) else 0
        val indByteLength = rawIndByteLength + indPadding

        // Color normalizado RGBA: si es wireframe, usar color técnico SageGreen (#52A474)
        val effectiveColor = if (isWireframe) Color(0xFF52A474) else yarnColor
        val r = effectiveColor.red
        val g = effectiveColor.green
        val b = effectiveColor.blue

        val hasVertexColors = !isWireframe && mesh.vertexColors.isNotEmpty() && mesh.vertexColors.size >= vertexCount * 4
        val hasUvs = !isWireframe && mesh.vertexUvs.isNotEmpty() && mesh.vertexUvs.size >= vertexCount * 2
        val hasTangents = !isWireframe && mesh.vertexTangents.isNotEmpty() && mesh.vertexTangents.size >= vertexCount * 4
        val tanByteLength = if (hasTangents) vertexCount * 4 * 4 else 0

        val stitchPng = if (hasUvs) {
            stitchTexturePng ?: CrochetTextureGenerator.defaultStitchTexturePng
        } else {
            null
        }
        val normalPng = if (hasUvs) {
            normalTexturePng ?: CrochetTextureGenerator.defaultNormalMapPng
        } else {
            null
        }
        val ormPng = if (hasUvs) {
            occlusionRoughnessPng ?: CrochetTextureGenerator.defaultOcclusionRoughnessPng
        } else {
            null
        }

        val rawImg0ByteLength = stitchPng?.size ?: 0
        val img0Padding = if (rawImg0ByteLength % 4 != 0) 4 - (rawImg0ByteLength % 4) else 0
        val img0ByteLength = rawImg0ByteLength + img0Padding

        val rawImg1ByteLength = normalPng?.size ?: 0
        val img1Padding = if (rawImg1ByteLength % 4 != 0) 4 - (rawImg1ByteLength % 4) else 0
        val img1ByteLength = rawImg1ByteLength + img1Padding

        val rawImg2ByteLength = ormPng?.size ?: 0
        val img2Padding = if (rawImg2ByteLength % 4 != 0) 4 - (rawImg2ByteLength % 4) else 0
        val img2ByteLength = rawImg2ByteLength + img2Padding

        val jsonString: String
        val totalBinLength: Int

        if (isWireframe) {
            // WIREFRAME: Primitiva LINES (mode 1) con KHR_materials_unlit y sin atributo NORMAL
            totalBinLength = posByteLength + indByteLength

            jsonString = """
{
  "asset": {
    "version": "2.0",
    "generator": "StitchMesh3D Wireframe Generator"
  },
  "extensionsUsed": [
    "KHR_materials_unlit"
  ],
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
      "name": "WireframeUnlitMaterial",
      "pbrMetallicRoughness": {
        "baseColorFactor": [$r, $g, $b, 1.0]
      },
      "extensions": {
        "KHR_materials_unlit": {}
      }
    }
  ],
  "meshes": [
    {
      "primitives": [
        {
          "attributes": {
            "POSITION": 0
          },
          "indices": 1,
          "material": 0,
          "mode": 1
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
      "componentType": $indexComponentType,
      "count": $indexCount,
      "type": "SCALAR"
    }
  ],
  "bufferViews": [
    {
      "buffer": 0,
      "byteOffset": 0,
      "byteLength": $posByteLength,
      "target": 34962
    },
    {
      "buffer": 0,
      "byteOffset": $posByteLength,
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
        } else if (hasUvs && hasVertexColors) {
            // SÓLIDO PBR MULTICROMÁTICO CON TEXTURA DE RELIEVE, NORMALES Y SHEEN (SM-084)
            val normByteLength = vertexCount * 3 * 4
            val colorByteLength = vertexCount * 4 * 4
            val uvByteLength = vertexCount * 2 * 4

            val posOffset = 0
            val normOffset = posByteLength
            val colorOffset = normOffset + normByteLength
            val uvOffset = colorOffset + colorByteLength
            val indOffset = uvOffset + uvByteLength
            val tanOffset = indOffset + indByteLength
            val img0Offset = tanOffset + tanByteLength
            val img1Offset = img0Offset + img0ByteLength
            val img2Offset = img1Offset + img1ByteLength
            totalBinLength = img2Offset + img2ByteLength

            val img0BufferViewIdx = if (hasTangents) 6 else 5
            val img1BufferViewIdx = img0BufferViewIdx + 1
            val img2BufferViewIdx = img0BufferViewIdx + 2

            jsonString = """
{
  "asset": {
    "version": "2.0",
    "generator": "StitchMesh3D Wool PBR Textured Multicolored Generator"
  },
  "extensionsUsed": [
    "KHR_materials_sheen"
  ],
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
  "samplers": [
    {
      "magFilter": 9729,
      "minFilter": 9729,
      "wrapS": 10497,
      "wrapT": 10497
    }
  ],
  "images": [
    {
      "bufferView": $img0BufferViewIdx,
      "mimeType": "image/png",
      "name": "CrochetStitchAlbedo"
    },
    {
      "bufferView": $img1BufferViewIdx,
      "mimeType": "image/png",
      "name": "CrochetStitchNormal"
    },
    {
      "bufferView": $img2BufferViewIdx,
      "mimeType": "image/png",
      "name": "CrochetStitchOcclusionRoughness"
    }
  ],
  "textures": [
    {
      "sampler": 0,
      "source": 0
    },
    {
      "sampler": 0,
      "source": 1
    },
    {
      "sampler": 0,
      "source": 2
    }
  ],
  "materials": [
    {
      "name": "WoolPbrMaterial",
      "pbrMetallicRoughness": {
        "baseColorFactor": [1.0, 1.0, 1.0, 1.0],
        "baseColorTexture": {
          "index": 0
        },
        "metallicRoughnessTexture": {
          "index": 2
        },
        "roughnessFactor": $roughness,
        "metallicFactor": 0.0
      },
      "normalTexture": {
        "index": 1,
        "scale": 1.0
      },
      "occlusionTexture": {
        "index": 2,
        "strength": 1.0
      },
      "extensions": {
        "KHR_materials_sheen": {
          "sheenColorFactor": [0.85, 0.85, 0.85],
          "sheenRoughnessFactor": 0.5
        }
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
            "NORMAL": 1,
            "COLOR_0": 2,
            "TEXCOORD_0": 3${if (hasTangents) ",\n            \"TANGENT\": 5" else ""}
          },
          "indices": 4,
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
      "componentType": 5126,
      "count": $vertexCount,
      "type": "VEC4"
    },
    {
      "bufferView": 3,
      "byteOffset": 0,
      "componentType": 5126,
      "count": $vertexCount,
      "type": "VEC2"
    },
    {
      "bufferView": 4,
      "byteOffset": 0,
      "componentType": $indexComponentType,
      "count": $indexCount,
      "type": "SCALAR"
    }${if (hasTangents) """,
    {
      "bufferView": 5,
      "byteOffset": 0,
      "componentType": 5126,
      "count": $vertexCount,
      "type": "VEC4"
    }""" else ""}
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
      "byteOffset": $colorOffset,
      "byteLength": $colorByteLength,
      "target": 34962
    },
    {
      "buffer": 0,
      "byteOffset": $uvOffset,
      "byteLength": $uvByteLength,
      "target": 34962
    },
    {
      "buffer": 0,
      "byteOffset": $indOffset,
      "byteLength": $rawIndByteLength,
      "target": 34963
    }${if (hasTangents) """,
    {
      "buffer": 0,
      "byteOffset": $tanOffset,
      "byteLength": $tanByteLength,
      "target": 34962
    }""" else ""},
    {
      "buffer": 0,
      "byteOffset": $img0Offset,
      "byteLength": $rawImg0ByteLength
    },
    {
      "buffer": 0,
      "byteOffset": $img1Offset,
      "byteLength": $rawImg1ByteLength
    },
    {
      "buffer": 0,
      "byteOffset": $img2Offset,
      "byteLength": $rawImg2ByteLength
    }
  ],
  "buffers": [
    {
      "byteLength": $totalBinLength
    }
  ]
}
""".trim()
        } else if (hasUvs) {
            // SÓLIDO PBR MONOCROMÁTICO CON TEXTURA DE RELIEVE, NORMALES Y SHEEN (SM-084)
            val normByteLength = vertexCount * 3 * 4
            val uvByteLength = vertexCount * 2 * 4

            val posOffset = 0
            val normOffset = posByteLength
            val uvOffset = normOffset + normByteLength
            val indOffset = uvOffset + uvByteLength
            val tanOffset = indOffset + indByteLength
            val img0Offset = tanOffset + tanByteLength
            val img1Offset = img0Offset + img0ByteLength
            val img2Offset = img1Offset + img1ByteLength
            totalBinLength = img2Offset + img2ByteLength

            val img0BufferViewIdx = if (hasTangents) 5 else 4
            val img1BufferViewIdx = img0BufferViewIdx + 1
            val img2BufferViewIdx = img0BufferViewIdx + 2

            jsonString = """
{
  "asset": {
    "version": "2.0",
    "generator": "StitchMesh3D Wool PBR Textured Generator"
  },
  "extensionsUsed": [
    "KHR_materials_sheen"
  ],
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
  "samplers": [
    {
      "magFilter": 9729,
      "minFilter": 9729,
      "wrapS": 10497,
      "wrapT": 10497
    }
  ],
  "images": [
    {
      "bufferView": $img0BufferViewIdx,
      "mimeType": "image/png",
      "name": "CrochetStitchAlbedo"
    },
    {
      "bufferView": $img1BufferViewIdx,
      "mimeType": "image/png",
      "name": "CrochetStitchNormal"
    },
    {
      "bufferView": $img2BufferViewIdx,
      "mimeType": "image/png",
      "name": "CrochetStitchOcclusionRoughness"
    }
  ],
  "textures": [
    {
      "sampler": 0,
      "source": 0
    },
    {
      "sampler": 0,
      "source": 1
    },
    {
      "sampler": 0,
      "source": 2
    }
  ],
  "materials": [
    {
      "name": "WoolPbrMaterial",
      "pbrMetallicRoughness": {
        "baseColorFactor": [$r, $g, $b, 1.0],
        "baseColorTexture": {
          "index": 0
        },
        "metallicRoughnessTexture": {
          "index": 2
        },
        "roughnessFactor": $roughness,
        "metallicFactor": 0.0
      },
      "normalTexture": {
        "index": 1,
        "scale": 1.0
      },
      "occlusionTexture": {
        "index": 2,
        "strength": 1.0
      },
      "extensions": {
        "KHR_materials_sheen": {
          "sheenColorFactor": [0.85, 0.85, 0.85],
          "sheenRoughnessFactor": 0.5
        }
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
            "NORMAL": 1,
            "TEXCOORD_0": 2${if (hasTangents) ",\n            \"TANGENT\": 4" else ""}
          },
          "indices": 3,
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
      "componentType": 5126,
      "count": $vertexCount,
      "type": "VEC2"
    },
    {
      "bufferView": 3,
      "byteOffset": 0,
      "componentType": $indexComponentType,
      "count": $indexCount,
      "type": "SCALAR"
    }${if (hasTangents) """,
    {
      "bufferView": 4,
      "byteOffset": 0,
      "componentType": 5126,
      "count": $vertexCount,
      "type": "VEC4"
    }""" else ""}
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
      "byteOffset": $uvOffset,
      "byteLength": $uvByteLength,
      "target": 34962
    },
    {
      "buffer": 0,
      "byteOffset": $indOffset,
      "byteLength": $rawIndByteLength,
      "target": 34963
    }${if (hasTangents) """,
    {
      "buffer": 0,
      "byteOffset": $tanOffset,
      "byteLength": $tanByteLength,
      "target": 34962
    }""" else ""},
    {
      "buffer": 0,
      "byteOffset": $img0Offset,
      "byteLength": $rawImg0ByteLength
    },
    {
      "buffer": 0,
      "byteOffset": $img1Offset,
      "byteLength": $rawImg1ByteLength
    },
    {
      "buffer": 0,
      "byteOffset": $img2Offset,
      "byteLength": $rawImg2ByteLength
    }
  ],
  "buffers": [
    {
      "byteLength": $totalBinLength
    }
  ]
}
""".trim()
        } else if (hasVertexColors) {
            // SÓLIDO PBR MULTICROMÁTICO SIN UV (Fallback)
            val normByteLength = vertexCount * 3 * 4
            val colorByteLength = vertexCount * 4 * 4
            val posOffset = 0
            val normOffset = posByteLength
            val colorOffset = posByteLength + normByteLength
            val indOffset = colorOffset + colorByteLength
            totalBinLength = indOffset + indByteLength

            jsonString = """
{
  "asset": {
    "version": "2.0",
    "generator": "StitchMesh3D Wool PBR Multicolored Generator"
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
        "baseColorFactor": [1.0, 1.0, 1.0, 1.0],
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
            "NORMAL": 1,
            "COLOR_0": 2
          },
          "indices": 3,
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
      "componentType": 5126,
      "count": $vertexCount,
      "type": "VEC4"
    },
    {
      "bufferView": 3,
      "byteOffset": 0,
      "componentType": $indexComponentType,
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
      "byteOffset": $colorOffset,
      "byteLength": $colorByteLength,
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
        } else {
            // SÓLIDO PBR MONOCROMÁTICO SIN UV (Fallback)
            val normByteLength = vertexCount * 3 * 4
            val posOffset = 0
            val normOffset = posByteLength
            val indOffset = posByteLength + normByteLength
            totalBinLength = indOffset + indByteLength

            jsonString = """
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
      "componentType": $indexComponentType,
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
        }

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

        if (!isWireframe) {
            // Payload binario: NORMALS (Float3) solo en modo sólido
            for (i in 0 until (vertexCount * 3)) {
                val n = if (i < mesh.vertexNormals.size) mesh.vertexNormals[i] else 0f
                byteBuffer.putFloat(n)
            }

            if (hasVertexColors) {
                // Payload binario: COLORS (Float4: RGBA)
                for (i in 0 until (vertexCount * 4)) {
                    val c = if (i < mesh.vertexColors.size) mesh.vertexColors[i] else 1.0f
                    byteBuffer.putFloat(c)
                }
            }

            if (hasUvs) {
                // Payload binario: TEXCOORD_0 (Float2: UV)
                for (i in 0 until (vertexCount * 2)) {
                    val uv = if (i < mesh.vertexUvs.size) mesh.vertexUvs[i] else 0f
                    byteBuffer.putFloat(uv)
                }
            }
        }

        // Payload binario: INDICES (UNSIGNED_SHORT o UNSIGNED_INT según use32BitIndices)
        if (use32BitIndices) {
            for (i in 0 until indexCount) {
                byteBuffer.putInt(indicesList[i])
            }
        } else {
            for (i in 0 until indexCount) {
                byteBuffer.putShort((indicesList[i] and 0xFFFF).toShort())
            }
        }

        // Rellenar padding binario de índices con ceros (0x00)
        repeat(indPadding) {
            byteBuffer.put(0x00.toByte())
        }

        if (hasTangents) {
            // Payload binario: TANGENTS (Float4: Tx, Ty, Tz, Tw)
            for (i in 0 until (vertexCount * 4)) {
                val tanVal = if (i < mesh.vertexTangents.size) mesh.vertexTangents[i] else 0f
                byteBuffer.putFloat(tanVal)
            }
        }

        if (hasUvs) {
            if (stitchPng != null) {
                byteBuffer.put(stitchPng)
                repeat(img0Padding) {
                    byteBuffer.put(0x00.toByte())
                }
            }
            if (normalPng != null) {
                byteBuffer.put(normalPng)
                repeat(img1Padding) {
                    byteBuffer.put(0x00.toByte())
                }
            }
            if (ormPng != null) {
                byteBuffer.put(ormPng)
                repeat(img2Padding) {
                    byteBuffer.put(0x00.toByte())
                }
            }
        }

        byteBuffer.flip()
        return byteBuffer
    }
}
