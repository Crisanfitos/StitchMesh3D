package com.crisanfitos.stitchmesh3d.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de persistencia local Room para estándares de matriz de tensión (CYC).
 */
@Entity(tableName = "yarn_gauge_standards")
data class YarnGaugeStandardEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "yarn_weight_category")
    val yarnWeightCategory: Int,

    @ColumnInfo(name = "category_name")
    val categoryName: String,

    @ColumnInfo(name = "hook_size_mm")
    val hookSizeMm: Float,

    @ColumnInfo(name = "stitch_width_mm")
    val stitchWidthMm: Float,

    @ColumnInfo(name = "stitch_height_mm")
    val stitchHeightMm: Float,

    @ColumnInfo(name = "stitch_thickness_mm")
    val stitchThicknessMm: Float
)
