package com.crisanfitos.stitchmesh3d.core.geometry

import com.crisanfitos.stitchmesh3d.core.engine.color.CrochetColorHelper
import com.crisanfitos.stitchmesh3d.core.engine.model.StitchType
import com.crisanfitos.stitchmesh3d.core.engine.parser.StitchInstance
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import com.crisanfitos.stitchmesh3d.domain.model.ProjectStructureType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Generador geométrico de mallas 3D para piezas planas y prendas de crochet ([PartTopologyType.FLAT_PANEL]).
 *
 * A diferencia de los cuerpos cerrados/tubulares de amigurumi que crecen en revolución axial en el eje Z,
 * las piezas planas se desarrollan como superficies laminares en el plano XY con un espesor real de hilado
 * (t_stitch = [YarnGaugeStandard.stitchThicknessMm]).
 *
 * Produce mallas 2-variedad cerradas con volumen real:
 * - Cara superior a cota +t_stitch/2 (normales [0, 0, 1]).
 * - Cara inferior a cota -t_stitch/2 (normales [0, 0, -1]).
 * - Paredes perimetrales en los bordes que sellan el grosor de la lámina con normales ortogonales hacia afuera.
 * - Búfer de color de vértices [MeshGeometry.vertexColors] con soporte intra-vuelta (Jacquard/Tapestry).
 */
object PlanarMeshGenerator {

    private const val TWO_PI = 2.0 * PI

    /**
     * Genera la geometría 3D de una pieza plana despachando automáticamente entre:
     * - Modo Concéntrico Planar (Granny Squares o parches circulares que inician con Anillo Mágico).
     * - Modo Lineal en Hileras (Paneles de prendas, bufandas, mantas o parches que inician con cadeneta).
     */
    fun generatePlanarMesh(
        rounds: List<List<StitchInstance>>,
        gauge: YarnGaugeStandard,
        structureType: ProjectStructureType = ProjectStructureType.FLAT_GARMENT,
        defaultColorHex: String = "#E06D53"
    ): MeshGeometry {
        val validRounds = rounds.filter { it.isNotEmpty() }
        if (validRounds.isEmpty()) {
            return MeshGeometry(
                vertexPositions = FloatArray(0),
                indices = ShortArray(0),
                vertexCount = 0,
                triangleCount = 0,
                vertexNormals = FloatArray(0),
                vertexColors = FloatArray(0)
            )
        }

        val firstRoundStitches = validRounds.first()
        val isConcentric = structureType == ProjectStructureType.GRANNY_SQUARE ||
                firstRoundStitches.any { it.stitchType is StitchType.MagicRing }

        return if (isConcentric) {
            generateConcentricMesh(
                rounds = validRounds,
                gauge = gauge,
                isSquare = (structureType == ProjectStructureType.GRANNY_SQUARE),
                defaultColorHex = defaultColorHex
            )
        } else {
            generateRowByRowMesh(
                rows = validRounds,
                gauge = gauge,
                defaultColorHex = defaultColorHex
            )
        }
    }

    /**
     * Genera una malla laminar rectangular/trapezoidal para tejido en hileras de ida y vuelta.
     */
    fun generateRowByRowMesh(
        rows: List<List<StitchInstance>>,
        gauge: YarnGaugeStandard,
        defaultColorHex: String = "#E06D53"
    ): MeshGeometry {
        val validRows = rows.filter { it.isNotEmpty() }
        if (validRows.isEmpty()) {
            return emptyMesh()
        }

        val wStitch = gauge.stitchWidthMm.toFloat().coerceAtLeast(0.5f)
        val hStitch = gauge.stitchHeightMm.toFloat().coerceAtLeast(0.5f)
        val tStitch = gauge.stitchThicknessMm.toFloat().coerceAtLeast(0.5f)
        val halfT = tStitch / 2.0f

        val positions = ArrayList<Float>()
        val normals = ArrayList<Float>()
        val colors = ArrayList<Float>()
        val indices = ArrayList<Short>()

        // 1. Calcular alturas Y de cada hilera
        val rowHeights = validRows.map { row ->
            val avgHRel = row.map { it.hRel }.average().toFloat()
            (hStitch * avgHRel).coerceAtLeast(0.5f)
        }
        val totalHeight = rowHeights.sum()
        var currentY = -totalHeight / 2.0f

        // Helper para agregar un vértice
        fun addVertex(
            x: Float, y: Float, z: Float,
            nx: Float, ny: Float, nz: Float,
            colorRgba: FloatArray
        ): Short {
            val idx = (positions.size / 3).toShort()
            positions.add(x)
            positions.add(y)
            positions.add(z)
            normals.add(nx)
            normals.add(ny)
            normals.add(nz)
            colors.add(colorRgba[0])
            colors.add(colorRgba[1])
            colors.add(colorRgba[2])
            colors.add(colorRgba[3])
            return idx
        }

        // Helper para agregar un quad triangulado (v00 -> v10 -> v11 -> v01)
        fun addQuad(v00: Short, v10: Short, v11: Short, v01: Short) {
            // Triángulo 1
            indices.add(v00)
            indices.add(v10)
            indices.add(v11)
            // Triángulo 2
            indices.add(v00)
            indices.add(v11)
            indices.add(v01)
        }

        for (r in validRows.indices) {
            val row = validRows[r]
            val rowH = rowHeights[r]
            val yBottom = currentY
            val yTop = currentY + rowH
            currentY = yTop

            // Anchos relativos y acumulación en X
            val widths = row.map { (wStitch * it.wRel.toFloat()).coerceAtLeast(0.5f) }
            val totalW = widths.sum()
            var currentX = -totalW / 2.0f

            for (j in row.indices) {
                val inst = row[j]
                val stitchW = widths[j]
                val xLeft = currentX
                val xRight = currentX + stitchW
                currentX = xRight

                val colorRgba = CrochetColorHelper.toRgbaFloats(inst.colorHex ?: defaultColorHex)

                // --- CARA SUPERIOR (+Z) ---
                val top00 = addVertex(xLeft, yBottom, halfT, 0f, 0f, 1f, colorRgba)
                val top10 = addVertex(xRight, yBottom, halfT, 0f, 0f, 1f, colorRgba)
                val top11 = addVertex(xRight, yTop, halfT, 0f, 0f, 1f, colorRgba)
                val top01 = addVertex(xLeft, yTop, halfT, 0f, 0f, 1f, colorRgba)
                addQuad(top00, top10, top11, top01)

                // --- CARA INFERIOR (-Z) ---
                // Orden invertido para winding CCW visto desde abajo
                val bot00 = addVertex(xLeft, yBottom, -halfT, 0f, 0f, -1f, colorRgba)
                val bot10 = addVertex(xRight, yBottom, -halfT, 0f, 0f, -1f, colorRgba)
                val bot11 = addVertex(xRight, yTop, -halfT, 0f, 0f, -1f, colorRgba)
                val bot01 = addVertex(xLeft, yTop, -halfT, 0f, 0f, -1f, colorRgba)
                addQuad(bot00, bot01, bot11, bot10)

                // --- BORDES PERIMETRALES ---
                // Borde Inferior (solo en la primera hilera r == 0)
                if (r == 0) {
                    val s0 = addVertex(xLeft, yBottom, halfT, 0f, -1f, 0f, colorRgba)
                    val s1 = addVertex(xRight, yBottom, halfT, 0f, -1f, 0f, colorRgba)
                    val s2 = addVertex(xRight, yBottom, -halfT, 0f, -1f, 0f, colorRgba)
                    val s3 = addVertex(xLeft, yBottom, -halfT, 0f, -1f, 0f, colorRgba)
                    addQuad(s0, s3, s2, s1)
                }

                // Borde Superior (solo en la última hilera r == validRows.lastIndex)
                if (r == validRows.lastIndex) {
                    val s0 = addVertex(xLeft, yTop, halfT, 0f, 1f, 0f, colorRgba)
                    val s1 = addVertex(xRight, yTop, halfT, 0f, 1f, 0f, colorRgba)
                    val s2 = addVertex(xRight, yTop, -halfT, 0f, 1f, 0f, colorRgba)
                    val s3 = addVertex(xLeft, yTop, -halfT, 0f, 1f, 0f, colorRgba)
                    addQuad(s0, s1, s2, s3)
                }

                // Borde Izquierdo (primer punto de la hilera j == 0)
                if (j == 0) {
                    val s0 = addVertex(xLeft, yBottom, halfT, -1f, 0f, 0f, colorRgba)
                    val s1 = addVertex(xLeft, yTop, halfT, -1f, 0f, 0f, colorRgba)
                    val s2 = addVertex(xLeft, yTop, -halfT, -1f, 0f, 0f, colorRgba)
                    val s3 = addVertex(xLeft, yBottom, -halfT, -1f, 0f, 0f, colorRgba)
                    addQuad(s0, s1, s2, s3)
                }

                // Borde Derecho (último punto de la hilera j == row.lastIndex)
                if (j == row.lastIndex) {
                    val s0 = addVertex(xRight, yBottom, halfT, 1f, 0f, 0f, colorRgba)
                    val s1 = addVertex(xRight, yTop, halfT, 1f, 0f, 0f, colorRgba)
                    val s2 = addVertex(xRight, yTop, -halfT, 1f, 0f, 0f, colorRgba)
                    val s3 = addVertex(xRight, yBottom, -halfT, 1f, 0f, 0f, colorRgba)
                    addQuad(s0, s3, s2, s1)
                }
            }
        }

        return createMesh(positions, normals, colors, indices)
    }

    /**
     * Genera una malla concéntrica en el plano XY para Granny Squares o motivos planos.
     */
    fun generateConcentricMesh(
        rounds: List<List<StitchInstance>>,
        gauge: YarnGaugeStandard,
        isSquare: Boolean = true,
        defaultColorHex: String = "#E06D53"
    ): MeshGeometry {
        val validRounds = rounds.filter { it.isNotEmpty() }
        if (validRounds.isEmpty()) {
            return emptyMesh()
        }

        val wStitch = gauge.stitchWidthMm.toFloat().coerceAtLeast(0.5f)
        val hStitch = gauge.stitchHeightMm.toFloat().coerceAtLeast(0.5f)
        val tStitch = gauge.stitchThicknessMm.toFloat().coerceAtLeast(0.5f)
        val halfT = tStitch / 2.0f

        val positions = ArrayList<Float>()
        val normals = ArrayList<Float>()
        val colors = ArrayList<Float>()
        val indices = ArrayList<Short>()

        // Helper para radio con modulación cuadrada (superelipse redondeada)
        fun radiusFactor(theta: Double): Float {
            if (!isSquare) return 1.0f
            // Superelipse centrada en diagonales: r = 1 / (cos^4 + sin^4)^(1/4)
            val c = cos(theta - PI / 4.0)
            val s = sin(theta - PI / 4.0)
            val denom = (c.pow(4.0) + s.pow(4.0)).pow(0.25)
            return (1.0 / denom.coerceAtLeast(1e-4)).toFloat()
        }

        fun addVertex(
            x: Float, y: Float, z: Float,
            nx: Float, ny: Float, nz: Float,
            colorRgba: FloatArray
        ): Short {
            val idx = (positions.size / 3).toShort()
            positions.add(x)
            positions.add(y)
            positions.add(z)
            normals.add(nx)
            normals.add(ny)
            normals.add(nz)
            colors.add(colorRgba[0])
            colors.add(colorRgba[1])
            colors.add(colorRgba[2])
            colors.add(colorRgba[3])
            return idx
        }

        fun addQuad(v00: Short, v10: Short, v11: Short, v01: Short) {
            indices.add(v00)
            indices.add(v10)
            indices.add(v11)
            indices.add(v00)
            indices.add(v11)
            indices.add(v01)
        }

        // Radios concéntricos acumulados
        val ringRadii = ArrayList<Float>(validRounds.size + 1)
        var currentRadius = (wStitch * 0.8f).coerceAtLeast(1.0f)
        ringRadii.add(currentRadius)

        for (round in validRounds) {
            val avgHRel = round.map { it.hRel }.average().toFloat()
            currentRadius += (hStitch * avgHRel).coerceAtLeast(0.5f)
            ringRadii.add(currentRadius)
        }

        // Centro polar superior e inferior
        val centerColor = CrochetColorHelper.toRgbaFloats(
            validRounds.first().firstOrNull()?.colorHex ?: defaultColorHex
        )
        val topCenterIdx = addVertex(0f, 0f, halfT, 0f, 0f, 1f, centerColor)
        val botCenterIdx = addVertex(0f, 0f, -halfT, 0f, 0f, -1f, centerColor)

        // Teselación por anillos concéntricos
        for (r in validRounds.indices) {
            val round = validRounds[r]
            val rInner = ringRadii[r]
            val rOuter = ringRadii[r + 1]

            val totalWRel = round.sumOf { it.wRel }.coerceAtLeast(1e-6)
            var accumulatedWRel = 0.0

            for (j in round.indices) {
                val inst = round[j]
                val thetaStart = TWO_PI * (accumulatedWRel / totalWRel)
                accumulatedWRel += inst.wRel
                val thetaEnd = TWO_PI * (accumulatedWRel / totalWRel)

                val colorRgba = CrochetColorHelper.toRgbaFloats(inst.colorHex ?: defaultColorHex)

                val fStart = radiusFactor(thetaStart)
                val fEnd = radiusFactor(thetaEnd)

                val xInStart = (rInner * fStart * cos(thetaStart)).toFloat()
                val yInStart = (rInner * fStart * sin(thetaStart)).toFloat()
                val xInEnd = (rInner * fEnd * cos(thetaEnd)).toFloat()
                val yInEnd = (rInner * fEnd * sin(thetaEnd)).toFloat()

                val xOutStart = (rOuter * fStart * cos(thetaStart)).toFloat()
                val yOutStart = (rOuter * fStart * sin(thetaStart)).toFloat()
                val xOutEnd = (rOuter * fEnd * cos(thetaEnd)).toFloat()
                val yOutEnd = (rOuter * fEnd * sin(thetaEnd)).toFloat()

                if (r == 0) {
                    // Tapar el centro inicial mediante abanico (Triangle Fan)
                    val topInStart = addVertex(xInStart, yInStart, halfT, 0f, 0f, 1f, colorRgba)
                    val topInEnd = addVertex(xInEnd, yInEnd, halfT, 0f, 0f, 1f, colorRgba)
                    indices.add(topCenterIdx)
                    indices.add(topInStart)
                    indices.add(topInEnd)

                    val botInStart = addVertex(xInStart, yInStart, -halfT, 0f, 0f, -1f, colorRgba)
                    val botInEnd = addVertex(xInEnd, yInEnd, -halfT, 0f, 0f, -1f, colorRgba)
                    indices.add(botCenterIdx)
                    indices.add(botInEnd)
                    indices.add(botInStart)
                }

                // Cara Superior (+Z) del sector del anillo
                val topIn0 = addVertex(xInStart, yInStart, halfT, 0f, 0f, 1f, colorRgba)
                val topOut0 = addVertex(xOutStart, yOutStart, halfT, 0f, 0f, 1f, colorRgba)
                val topOut1 = addVertex(xOutEnd, yOutEnd, halfT, 0f, 0f, 1f, colorRgba)
                val topIn1 = addVertex(xInEnd, yInEnd, halfT, 0f, 0f, 1f, colorRgba)
                addQuad(topIn0, topOut0, topOut1, topIn1)

                // Cara Inferior (-Z) del sector del anillo
                val botIn0 = addVertex(xInStart, yInStart, -halfT, 0f, 0f, -1f, colorRgba)
                val botOut0 = addVertex(xOutStart, yOutStart, -halfT, 0f, 0f, -1f, colorRgba)
                val botOut1 = addVertex(xOutEnd, yOutEnd, -halfT, 0f, 0f, -1f, colorRgba)
                val botIn1 = addVertex(xInEnd, yInEnd, -halfT, 0f, 0f, -1f, colorRgba)
                addQuad(botIn0, botIn1, botOut1, botOut0)

                // Perímetro exterior (solo en la última vuelta)
                if (r == validRounds.lastIndex) {
                    val thetaMid = (thetaStart + thetaEnd) / 2.0
                    val nX = cos(thetaMid).toFloat()
                    val nY = sin(thetaMid).toFloat()
                    val sTop0 = addVertex(xOutStart, yOutStart, halfT, nX, nY, 0f, colorRgba)
                    val sTop1 = addVertex(xOutEnd, yOutEnd, halfT, nX, nY, 0f, colorRgba)
                    val sBot1 = addVertex(xOutEnd, yOutEnd, -halfT, nX, nY, 0f, colorRgba)
                    val sBot0 = addVertex(xOutStart, yOutStart, -halfT, nX, nY, 0f, colorRgba)
                    addQuad(sTop0, sTop1, sBot1, sBot0)
                }
            }
        }

        return createMesh(positions, normals, colors, indices)
    }

    private fun emptyMesh(): MeshGeometry = MeshGeometry(
        vertexPositions = FloatArray(0),
        indices = ShortArray(0),
        vertexCount = 0,
        triangleCount = 0,
        vertexNormals = FloatArray(0),
        vertexColors = FloatArray(0)
    )

    private fun createMesh(
        positions: List<Float>,
        normals: List<Float>,
        colors: List<Float>,
        indices: List<Short>
    ): MeshGeometry {
        val posArray = FloatArray(positions.size) { positions[it] }
        val normArray = FloatArray(normals.size) { normals[it] }
        val colArray = FloatArray(colors.size) { colors[it] }
        val idxArray = ShortArray(indices.size) { indices[it] }

        return MeshGeometry(
            vertexPositions = posArray,
            indices = idxArray,
            vertexCount = posArray.size / 3,
            triangleCount = idxArray.size / 3,
            vertexNormals = normArray,
            vertexColors = colArray
        )
    }
}
