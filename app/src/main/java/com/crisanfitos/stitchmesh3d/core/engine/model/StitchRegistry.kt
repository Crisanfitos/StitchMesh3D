package com.crisanfitos.stitchmesh3d.core.engine.model

import java.util.Locale

/**
 * Registro y catálogo de búsqueda de tipos de puntada por abreviatura en español o inglés.
 */
object StitchRegistry {

    private val staticStitches: List<StitchType> = listOf(
        StitchType.Chain,
        StitchType.SlipStitch,
        StitchType.SingleCrochet,
        StitchType.HalfDoubleCrochet,
        StitchType.DoubleCrochet,
        StitchType.TrebleCrochet,
        StitchType.DoubleTrebleCrochet,
        StitchType.Increase,
        StitchType.HdcIncrease,
        StitchType.DcIncrease,
        StitchType.TripleIncrease,
        StitchType.Decrease,
        StitchType.InvisibleDecrease,
        StitchType.HdcDecrease,
        StitchType.DcDecrease,
        StitchType.TripleDecrease,
        StitchType.Skip,
        // Post stitches (TRD §2.3.B)
        StitchType.FrontPostSingleCrochet,
        StitchType.FrontPostHalfDoubleCrochet,
        StitchType.FrontPostDoubleCrochet,
        StitchType.BackPostSingleCrochet,
        StitchType.BackPostHalfDoubleCrochet,
        StitchType.BackPostDoubleCrochet,
        // Volumetric stitches (TRD §2.3.C)
        StitchType.BobbleStitch,
        StitchType.PopcornStitch,
        StitchType.PuffStitch,
        StitchType.ReverseSingleCrochet
    )

    private val lookupMap: Map<String, StitchType> = buildMap {
        for (stitch in staticStitches) {
            for (abbr in stitch.spanishAbbreviations) {
                put(normalize(abbr), stitch)
            }
            for (abbr in stitch.englishAbbreviations) {
                put(normalize(abbr), stitch)
            }
        }
    }

    private val magicRingRegex = Regex("""^(?:am|mr)\s*\[?(\d+)\]?$""", RegexOption.IGNORE_CASE)

    /**
     * Devuelve la lista de todas las definiciones estándar no dinámicas.
     */
    fun allStandardStitches(): List<StitchType> = staticStitches

    /**
     * Resuelve una abreviatura o símbolo de texto al [StitchType] correspondiente.
     * Soporta indistintamente mayúsculas/minúsculas, modificadores topológicos (BLO/FLO)
     * y notación dinámica de Anillo Mágico (ej. "AM [6]", "mr 6").
     */
    fun resolve(symbol: String): StitchType? {
        val trimmed = symbol.trim()
        if (trimmed.isEmpty()) return null

        val normalized = normalize(trimmed)

        // 1. Coincidencia directa con puntadas estáticas
        lookupMap[normalized]?.let { return it }

        // 2. Modificadores topológicos de prefijo: BLO / FLO (ej. "blo pb", "flo sc")
        if (normalized.startsWith("blo ") || normalized.startsWith("blo-")) {
            val remainder = normalized.substring(4).trim()
            val base = resolve(remainder)
            if (base != null) return base.withTopology(TopologyFlag.BLO)
        }
        if (normalized.startsWith("flo ") || normalized.startsWith("flo-")) {
            val remainder = normalized.substring(4).trim()
            val base = resolve(remainder)
            if (base != null) return base.withTopology(TopologyFlag.FLO)
        }

        // 3. Detección de Anillo Mágico dinámico: AM [6], MR [6], AM 6, MR 8...
        val mrMatch = magicRingRegex.matchEntire(trimmed.lowercase(Locale.ROOT))
        if (mrMatch != null) {
            val count = mrMatch.groupValues[1].toIntOrNull()
            if (count != null && count > 0) {
                return StitchType.MagicRing(count)
            }
        }

        return null
    }

    private fun normalize(str: String): String {
        return str.trim()
            .lowercase(Locale.ROOT)
            .replace("\\s+".toRegex(), " ")
    }
}
