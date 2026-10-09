package com.crisanfitos.stitchmesh3d.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de persistencia local Room para proyectos de amigurumi y crochet.
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "structure_type")
    val structureType: String = "amigurumi_3d",

    @ColumnInfo(name = "yarn_weight_category")
    val yarnWeightCategory: Int = 4,

    @ColumnInfo(name = "hook_size_mm")
    val hookSizeMm: Float = 3.50f,

    @ColumnInfo(name = "custom_stitch_width_mm")
    val customStitchWidthMm: Float? = null,

    @ColumnInfo(name = "custom_stitch_height_mm")
    val customStitchHeightMm: Float? = null,

    @ColumnInfo(name = "is_valid")
    val isValid: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
