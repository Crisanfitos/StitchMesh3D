package com.crisanfitos.stitchmesh3d.core.engine.model

/**
 * Catálogo de tipos de puntadas elementales de crochet y amigurumi según TRD §2.2.
 */
sealed interface StitchType {
    val technicalName: String
    val definition: StitchDefinition
    val spanishAbbreviations: List<String>
    val englishAbbreviations: List<String>

    /** C_i: Puntos base consumidos. */
    val consumedStitches: Int get() = definition.consumedStitches

    /** P_i: Puntos nuevos producidos. */
    val producedStitches: Int get() = definition.producedStitches

    /** Δ = P - C: Variación neta de puntos. */
    val deltaStitches: Int get() = definition.deltaStitches

    /** h_rel: Factor de altura respecto a punto bajo base. */
    val hRel: Double get() = definition.hRel

    /** w_rel: Factor de ancho respecto a punto bajo base. */
    val wRel: Double get() = definition.wRel

    /** Δr_rel: Desplazamiento radial relativo. */
    val deltaRRel: Double get() = definition.deltaRRel

    /** Desplazamiento normal tridimensional relativo (extrusión volumétrica). */
    val normalDisplacement: Double get() = definition.normalDisplacement

    /** Banderas topológicas de lazo o relieve. */
    val topologyFlags: Set<TopologyFlag> get() = definition.topologyFlags

    /**
     * Aplica modificadores topológicos (ej. BLO, FLO) a este punto manteniendo invariantes C y P.
     */
    fun withTopology(vararg flags: TopologyFlag): StitchType {
        val newFlags = flags.toSet()
        if (newFlags == definition.topologyFlags) return this
        return ModifiedStitch(
            baseStitch = this,
            flags = newFlags
        )
    }

    // --- PUNTOS ELEMENTALES (TRD §2.2) ---

    /** Cadeneta / Chain (cad / c / ch) */
    data object Chain : StitchType {
        override val technicalName = "Cadeneta / Chain"
        override val definition = StitchDefinition(consumedStitches = 0, producedStitches = 1, hRel = 0.8, wRel = 0.8, deltaRRel = 0.0)
        override val spanishAbbreviations = listOf("cad", "c")
        override val englishAbbreviations = listOf("ch")
    }

    /** Punto Enano / Raso / Deslizado / Slip Stitch (pe / pr / pd / sl st) */
    data object SlipStitch : StitchType {
        override val technicalName = "Punto Enano / Slip Stitch"
        override val definition = StitchDefinition(consumedStitches = 1, producedStitches = 1, hRel = 0.2, wRel = 0.9, deltaRRel = 0.0)
        override val spanishAbbreviations = listOf("pe", "pr", "pd")
        override val englishAbbreviations = listOf("sl st", "slst")
    }

    /** Punto Bajo / Medio Punto / Single Crochet (pb / mp / sc) - Unidad estándar */
    data object SingleCrochet : StitchType {
        override val technicalName = "Punto Bajo / Single Crochet"
        override val definition = StitchDefinition(consumedStitches = 1, producedStitches = 1, hRel = 1.0, wRel = 1.0, deltaRRel = 0.0)
        override val spanishAbbreviations = listOf("pb", "mp")
        override val englishAbbreviations = listOf("sc")
    }

    /** Punto Medio Alto / Half Double Crochet (pma / mpa / mv / hdc) */
    data object HalfDoubleCrochet : StitchType {
        override val technicalName = "Punto Medio Alto / Half Double Crochet"
        override val definition = StitchDefinition(consumedStitches = 1, producedStitches = 1, hRel = 1.4, wRel = 1.1, deltaRRel = 0.05)
        override val spanishAbbreviations = listOf("pma", "mpa", "mv")
        override val englishAbbreviations = listOf("hdc")
    }

    /** Punto Alto / Vareta / Double Crochet (pa / pv / v / dc) */
    data object DoubleCrochet : StitchType {
        override val technicalName = "Punto Alto / Double Crochet"
        override val definition = StitchDefinition(consumedStitches = 1, producedStitches = 1, hRel = 2.0, wRel = 1.2, deltaRRel = 0.0)
        override val spanishAbbreviations = listOf("pa", "pv", "v")
        override val englishAbbreviations = listOf("dc")
    }

    /** Punto Alto Doble / Treble Crochet (pad / padbl / dv / tr) */
    data object TrebleCrochet : StitchType {
        override val technicalName = "Punto Alto Doble / Treble Crochet"
        override val definition = StitchDefinition(consumedStitches = 1, producedStitches = 1, hRel = 3.0, wRel = 1.3, deltaRRel = 0.0)
        override val spanishAbbreviations = listOf("pad", "padbl", "dv")
        override val englishAbbreviations = listOf("tr")
    }

    /** Punto Alto Triple / Double Treble (pat / patr / dtr) */
    data object DoubleTrebleCrochet : StitchType {
        override val technicalName = "Punto Alto Triple / Double Treble"
        override val definition = StitchDefinition(consumedStitches = 1, producedStitches = 1, hRel = 4.0, wRel = 1.4, deltaRRel = 0.0)
        override val spanishAbbreviations = listOf("pat", "patr")
        override val englishAbbreviations = listOf("dtr")
    }

    // --- AUMENTOS (TRD §2.2) ---

    /** Aumento Simple (2 pb en 1) (aum / inc) */
    data object Increase : StitchType {
        override val technicalName = "Aumento Simple / Increase"
        override val definition = StitchDefinition(consumedStitches = 1, producedStitches = 2, hRel = 1.0, wRel = 1.8, deltaRRel = 0.05)
        override val spanishAbbreviations = listOf("aum")
        override val englishAbbreviations = listOf("inc")
    }

    /** Aumento en Punto Medio Alto (aum-pma / aumpma / hdc inc) */
    data object HdcIncrease : StitchType {
        override val technicalName = "Aumento Punto Medio Alto / HDC Increase"
        override val definition = StitchDefinition(consumedStitches = 1, producedStitches = 2, hRel = 1.4, wRel = 2.0, deltaRRel = 0.05)
        override val spanishAbbreviations = listOf("aum-pma", "aumpma")
        override val englishAbbreviations = listOf("hdc inc", "hdcinc")
    }

    /** Aumento en Punto Alto (aum-pa / aumpa / dc inc) */
    data object DcIncrease : StitchType {
        override val technicalName = "Aumento Punto Alto / DC Increase"
        override val definition = StitchDefinition(consumedStitches = 1, producedStitches = 2, hRel = 2.0, wRel = 2.2, deltaRRel = 0.0)
        override val spanishAbbreviations = listOf("aum-pa", "aumpa")
        override val englishAbbreviations = listOf("dc inc", "dcinc")
    }

    /** Aumento Triple (3 pb en 1) (aum3 / aum triple / 3sc inc) */
    data object TripleIncrease : StitchType {
        override val technicalName = "Aumento Triple / 3SC Increase"
        override val definition = StitchDefinition(consumedStitches = 1, producedStitches = 3, hRel = 1.0, wRel = 2.5, deltaRRel = 0.10)
        override val spanishAbbreviations = listOf("aum3", "aum triple")
        override val englishAbbreviations = listOf("3sc inc", "3scinc")
    }

    // --- DISMINUCIONES (TRD §2.2) ---

    /** Disminución Simple (dism / dec / sc2tog) */
    data object Decrease : StitchType {
        override val technicalName = "Disminución Simple / Decrease"
        override val definition = StitchDefinition(consumedStitches = 2, producedStitches = 1, hRel = 1.0, wRel = 0.9, deltaRRel = -0.05)
        override val spanishAbbreviations = listOf("dism")
        override val englishAbbreviations = listOf("dec", "sc2tog")
    }

    /** Disminución Invisible (invdec) */
    data object InvisibleDecrease : StitchType {
        override val technicalName = "Disminución Invisible / Invisible Decrease"
        override val definition = StitchDefinition(consumedStitches = 2, producedStitches = 1, hRel = 1.0, wRel = 1.0, deltaRRel = 0.0)
        override val spanishAbbreviations = listOf("invdec", "dism-inv")
        override val englishAbbreviations = listOf("invdec", "inv dec")
    }

    /** Disminución en Punto Medio Alto (dism-pma / dismpma / hdc2tog) */
    data object HdcDecrease : StitchType {
        override val technicalName = "Disminución Punto Medio Alto / HDC Decrease"
        override val definition = StitchDefinition(consumedStitches = 2, producedStitches = 1, hRel = 1.4, wRel = 1.0, deltaRRel = 0.0)
        override val spanishAbbreviations = listOf("dism-pma", "dismpma")
        override val englishAbbreviations = listOf("hdc2tog", "hdc dec")
    }

    /** Disminución en Punto Alto (dism-pa / dismpa / dc2tog) */
    data object DcDecrease : StitchType {
        override val technicalName = "Disminución Punto Alto / DC Decrease"
        override val definition = StitchDefinition(consumedStitches = 2, producedStitches = 1, hRel = 2.0, wRel = 1.1, deltaRRel = 0.0)
        override val spanishAbbreviations = listOf("dism-pa", "dismpa")
        override val englishAbbreviations = listOf("dc2tog", "dc dec")
    }

    /** Disminución Triple (3 en 1) (dism3 / dism triple / sc3tog) */
    data object TripleDecrease : StitchType {
        override val technicalName = "Disminución Triple / 3SC Decrease"
        override val definition = StitchDefinition(consumedStitches = 3, producedStitches = 1, hRel = 1.0, wRel = 0.8, deltaRRel = -0.10)
        override val spanishAbbreviations = listOf("dism3", "dism triple")
        override val englishAbbreviations = listOf("sc3tog", "3sc dec")
    }

    // --- ANILLO MÁGICO Y SALTAR ---

    /** Anillo Mágico polar parametrizado de N puntos (AM [N] / MR [N]) */
    data class MagicRing(val stitchCount: Int) : StitchType {
        init {
            require(stitchCount > 0) { "El Anillo Mágico debe contener al menos 1 punto: $stitchCount" }
        }
        override val technicalName = "Anillo Mágico ($stitchCount pts) / Magic Ring ($stitchCount sts)"
        override val definition = StitchDefinition(
            consumedStitches = 0,
            producedStitches = stitchCount,
            hRel = 0.5,
            wRel = 1.0,
            deltaRRel = 0.0
        )
        override val spanishAbbreviations = listOf("am", "am [$stitchCount]", "am $stitchCount")
        override val englishAbbreviations = listOf("mr", "mr [$stitchCount]", "mr $stitchCount")
    }

    /** Saltar Punto Base (saltar / sk) */
    data object Skip : StitchType {
        override val technicalName = "Saltar Punto / Skip Stitch"
        override val definition = StitchDefinition(consumedStitches = 1, producedStitches = 0, hRel = 0.0, wRel = 1.0, deltaRRel = 0.0)
        override val spanishAbbreviations = listOf("saltar", "sk")
        override val englishAbbreviations = listOf("sk", "skip")
    }

    // --- PUNTOS EN RELIEVE POR POSTE (TRD §2.3.B) ---

    /** Front Post Single Crochet (FPsc / Relieve Delantero Punto Bajo) */
    data object FrontPostSingleCrochet : StitchType {
        override val technicalName = "Relieve Delantero Punto Bajo / FPsc"
        override val definition = StitchDefinition(
            consumedStitches = 1, producedStitches = 1, hRel = 1.0, wRel = 1.0, deltaRRel = 0.3,
            topologyFlags = setOf(TopologyFlag.FRONT_POST)
        )
        override val spanishAbbreviations = listOf("fpsc", "rpd-pb")
        override val englishAbbreviations = listOf("fpsc")
    }

    /** Front Post Half Double Crochet (FPhdc / Relieve Delantero PMA) */
    data object FrontPostHalfDoubleCrochet : StitchType {
        override val technicalName = "Relieve Delantero Punto Medio Alto / FPhdc"
        override val definition = StitchDefinition(
            consumedStitches = 1, producedStitches = 1, hRel = 1.4, wRel = 1.1, deltaRRel = 0.3,
            topologyFlags = setOf(TopologyFlag.FRONT_POST)
        )
        override val spanishAbbreviations = listOf("fphdc", "rpd-pma")
        override val englishAbbreviations = listOf("fphdc")
    }

    /** Front Post Double Crochet (FPdc / Relieve Delantero Punto Alto) */
    data object FrontPostDoubleCrochet : StitchType {
        override val technicalName = "Relieve Delantero Punto Alto / FPdc"
        override val definition = StitchDefinition(
            consumedStitches = 1, producedStitches = 1, hRel = 2.0, wRel = 1.2, deltaRRel = 0.3,
            topologyFlags = setOf(TopologyFlag.FRONT_POST)
        )
        override val spanishAbbreviations = listOf("fpdc", "rpd-pa")
        override val englishAbbreviations = listOf("fpdc")
    }

    /** Back Post Single Crochet (BPsc / Relieve Trasero Punto Bajo) */
    data object BackPostSingleCrochet : StitchType {
        override val technicalName = "Relieve Trasero Punto Bajo / BPsc"
        override val definition = StitchDefinition(
            consumedStitches = 1, producedStitches = 1, hRel = 1.0, wRel = 1.0, deltaRRel = -0.3,
            topologyFlags = setOf(TopologyFlag.BACK_POST)
        )
        override val spanishAbbreviations = listOf("bpsc", "rpt-pb")
        override val englishAbbreviations = listOf("bpsc")
    }

    /** Back Post Half Double Crochet (BPhdc / Relieve Trasero PMA) */
    data object BackPostHalfDoubleCrochet : StitchType {
        override val technicalName = "Relieve Trasero Punto Medio Alto / BPhdc"
        override val definition = StitchDefinition(
            consumedStitches = 1, producedStitches = 1, hRel = 1.4, wRel = 1.1, deltaRRel = -0.3,
            topologyFlags = setOf(TopologyFlag.BACK_POST)
        )
        override val spanishAbbreviations = listOf("bphdc", "rpt-pma")
        override val englishAbbreviations = listOf("bphdc")
    }

    /** Back Post Double Crochet (BPdc / Relieve Trasero Punto Alto) */
    data object BackPostDoubleCrochet : StitchType {
        override val technicalName = "Relieve Trasero Punto Alto / BPdc"
        override val definition = StitchDefinition(
            consumedStitches = 1, producedStitches = 1, hRel = 2.0, wRel = 1.2, deltaRRel = -0.3,
            topologyFlags = setOf(TopologyFlag.BACK_POST)
        )
        override val spanishAbbreviations = listOf("bpdc", "rpt-pa")
        override val englishAbbreviations = listOf("bpdc")
    }

    // --- PUNTOS CON RELIEVE VOLUMÉTRICO 3D (TRD §2.3.C) ---

    /** Punto Garbanzo / Bobble Stitch (bo / garbanzo) - C=1, P=1, Δn = +1.5 */
    data object BobbleStitch : StitchType {
        override val technicalName = "Punto Garbanzo / Bobble Stitch"
        override val definition = StitchDefinition(
            consumedStitches = 1, producedStitches = 1, hRel = 1.0, wRel = 1.4, deltaRRel = 1.5,
            normalDisplacement = 1.5
        )
        override val spanishAbbreviations = listOf("garbanzo", "bo", "pto garbanzo")
        override val englishAbbreviations = listOf("bo", "bobble")
    }

    /** Punto Palomita / Popcorn Stitch (pop / palomita) - C=1, P=1, Δn = +2.2 */
    data object PopcornStitch : StitchType {
        override val technicalName = "Punto Palomita / Popcorn Stitch"
        override val definition = StitchDefinition(
            consumedStitches = 1, producedStitches = 1, hRel = 1.2, wRel = 1.6, deltaRRel = 2.2,
            normalDisplacement = 2.2
        )
        override val spanishAbbreviations = listOf("palomita", "pop", "pto palomita")
        override val englishAbbreviations = listOf("pop", "popcorn")
    }

    /** Punto Piña / Puff Stitch (puff / piña) - C=1, P=1, Δn = +1.0 */
    data object PuffStitch : StitchType {
        override val technicalName = "Punto Piña / Puff Stitch"
        override val definition = StitchDefinition(
            consumedStitches = 1, producedStitches = 1, hRel = 1.0, wRel = 1.3, deltaRRel = 1.0,
            normalDisplacement = 1.0
        )
        override val spanishAbbreviations = listOf("piña", "puff", "pto piña")
        override val englishAbbreviations = listOf("puff")
    }

    /** Punto Cangrejo / Reverse Single Crochet (cangrejo / crab) - C=1, P=1 */
    data object ReverseSingleCrochet : StitchType {
        override val technicalName = "Punto Cangrejo / Reverse Single Crochet"
        override val definition = StitchDefinition(
            consumedStitches = 1, producedStitches = 1, hRel = 0.8, wRel = 1.0, deltaRRel = 0.0
        )
        override val spanishAbbreviations = listOf("cangrejo", "pto cangrejo")
        override val englishAbbreviations = listOf("crab", "reverse sc")
    }

    // --- PUNTADA CON MODIFICADOR TOPOLÓGICO (BLO / FLO / ETC) ---

    /**
     * Representa cualquier puntada base envuelta con modificadores topológicos (ej. BLO, FLO).
     * Mantiene invariantes C y P intactos.
     */
    data class ModifiedStitch(
        val baseStitch: StitchType,
        val flags: Set<TopologyFlag>
    ) : StitchType {
        override val technicalName: String = run {
            val prefix = flags.filter { it != TopologyFlag.NORMAL }.joinToString("-") { it.name }
            if (prefix.isNotEmpty()) "$prefix ${baseStitch.technicalName}" else baseStitch.technicalName
        }
        override val definition: StitchDefinition = baseStitch.definition.copy(
            topologyFlags = flags
        )
        override val spanishAbbreviations: List<String> = run {
            val flagStr = flags.joinToString(" ") { it.name.lowercase() }
            baseStitch.spanishAbbreviations.map { "$flagStr $it" }
        }
        override val englishAbbreviations: List<String> = run {
            val flagStr = flags.joinToString(" ") { it.name.lowercase() }
            baseStitch.englishAbbreviations.map { "$flagStr $it" }
        }
    }
}
