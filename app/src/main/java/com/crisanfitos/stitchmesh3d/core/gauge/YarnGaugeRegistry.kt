package com.crisanfitos.stitchmesh3d.core.gauge

import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnGaugeStandard
import com.crisanfitos.stitchmesh3d.core.gauge.model.YarnWeightCategory
import kotlin.math.abs

/**
 * Registro y matriz canónica de tensión de hilados y calibres de aguja de StitchMesh 3D.
 * Contiene los datos semilla oficiales de la Craft Yarn Council (CYC) sincronizados con
 * el esquema del backend Supabase (`backend_schema_supabase_sql.md`).
 */
object YarnGaugeRegistry {

    /**
     * Semilla local de estándares de tensión de tejido.
     */
    private val STANDARDS: List<YarnGaugeStandard> = listOf(
        YarnGaugeStandard(
            id = 1,
            yarnWeightCategory = YarnWeightCategory.LACE,
            categoryName = "Lace / #0",
            hookSizeMm = 1.75f,
            stitchWidthMm = 1.50f,
            stitchHeightMm = 1.40f,
            stitchThicknessMm = 1.10f
        ),
        YarnGaugeStandard(
            id = 2,
            yarnWeightCategory = YarnWeightCategory.SUPER_FINE,
            categoryName = "Super Fine / #1",
            hookSizeMm = 2.25f,
            stitchWidthMm = 1.90f,
            stitchHeightMm = 1.80f,
            stitchThicknessMm = 1.40f
        ),
        YarnGaugeStandard(
            id = 3,
            yarnWeightCategory = YarnWeightCategory.FINE,
            categoryName = "Fine / Sport / #2",
            hookSizeMm = 2.50f,
            stitchWidthMm = 2.20f,
            stitchHeightMm = 2.10f,
            stitchThicknessMm = 1.80f
        ),
        YarnGaugeStandard(
            id = 4,
            yarnWeightCategory = YarnWeightCategory.LIGHT,
            categoryName = "Light / DK / #3",
            hookSizeMm = 3.00f,
            stitchWidthMm = 2.80f,
            stitchHeightMm = 2.60f,
            stitchThicknessMm = 2.20f
        ),
        YarnGaugeStandard(
            id = 5,
            yarnWeightCategory = YarnWeightCategory.MEDIUM,
            categoryName = "Worsted / Medium / #4",
            hookSizeMm = 3.50f,
            stitchWidthMm = 3.20f,
            stitchHeightMm = 3.00f,
            stitchThicknessMm = 2.50f
        ),
        YarnGaugeStandard(
            id = 6,
            yarnWeightCategory = YarnWeightCategory.MEDIUM,
            categoryName = "Worsted / Medium / #4 (4.0mm)",
            hookSizeMm = 4.00f,
            stitchWidthMm = 3.50f,
            stitchHeightMm = 3.30f,
            stitchThicknessMm = 2.70f
        ),
        YarnGaugeStandard(
            id = 7,
            yarnWeightCategory = YarnWeightCategory.BULKY,
            categoryName = "Bulky / #5",
            hookSizeMm = 5.50f,
            stitchWidthMm = 4.80f,
            stitchHeightMm = 4.40f,
            stitchThicknessMm = 3.80f
        ),
        YarnGaugeStandard(
            id = 8,
            yarnWeightCategory = YarnWeightCategory.SUPER_BULKY,
            categoryName = "Super Bulky / #6",
            hookSizeMm = 8.00f,
            stitchWidthMm = 7.00f,
            stitchHeightMm = 6.40f,
            stitchThicknessMm = 5.60f
        ),
        YarnGaugeStandard(
            id = 9,
            yarnWeightCategory = YarnWeightCategory.JUMBO,
            categoryName = "Jumbo / #7",
            hookSizeMm = 12.00f,
            stitchWidthMm = 10.50f,
            stitchHeightMm = 9.80f,
            stitchThicknessMm = 8.40f
        )
    )

    /**
     * Estándar por defecto para nuevos proyectos de amigurumi:
     * Hilado Worsted / Medium (#4) con aguja de 3.5 mm para tejido denso y compacto.
     */
    val DEFAULT_STANDARD: YarnGaugeStandard = STANDARDS[4] // Worsted 3.5mm

    /**
     * Obtiene la lista completa de estándares disponibles.
     */
    fun getAllStandards(): List<YarnGaugeStandard> = STANDARDS

    /**
     * Busca un estándar por su identificador.
     */
    fun getById(id: Int): YarnGaugeStandard? = STANDARDS.firstOrNull { it.id == id }

    /**
     * Busca el estándar más exacto o próximo para una categoría de hilado y calibre de aguja.
     */
    fun findStandard(category: YarnWeightCategory, hookSizeMm: Float? = null): YarnGaugeStandard {
        val forCategory = STANDARDS.filter { it.yarnWeightCategory == category }
        if (forCategory.isEmpty()) return DEFAULT_STANDARD

        return if (hookSizeMm != null) {
            forCategory.minByOrNull { abs(it.hookSizeMm - hookSizeMm) } ?: forCategory.first()
        } else {
            forCategory.first()
        }
    }

    /**
     * Encuentra el estándar más cercano únicamente por tamaño de aguja nominal.
     */
    fun findClosestByHookSize(hookSizeMm: Float): YarnGaugeStandard {
        return STANDARDS.minByOrNull { abs(it.hookSizeMm - hookSizeMm) } ?: DEFAULT_STANDARD
    }

    /**
     * Interpola o extrapola paramétricamente un estándar para un calibre de aguja personalizado.
     * Mantiene las proporciones del estándar base más próximo dentro de la categoría.
     */
    fun interpolateStandard(category: YarnWeightCategory, customHookSizeMm: Float): YarnGaugeStandard {
        val base = findStandard(category, customHookSizeMm)
        val scaleRatio = customHookSizeMm / base.hookSizeMm

        return YarnGaugeStandard(
            id = -1, // ID temporal o dinámico
            yarnWeightCategory = category,
            categoryName = "${base.categoryName} (Custom ${customHookSizeMm}mm)",
            hookSizeMm = customHookSizeMm,
            stitchWidthMm = base.stitchWidthMm * scaleRatio,
            stitchHeightMm = base.stitchHeightMm * scaleRatio,
            stitchThicknessMm = base.stitchThicknessMm * scaleRatio
        )
    }
}
