package com.crisanfitos.stitchmesh3d.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad de persistencia Room para piezas del proyecto con borrado en cascada.
 */
@Entity(
    tableName = "project_parts",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["project_id"])]
)
data class ProjectPartEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "project_id")
    val projectId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "transform_pos_x")
    val transformPosX: Float = 0f,

    @ColumnInfo(name = "transform_pos_y")
    val transformPosY: Float = 0f,

    @ColumnInfo(name = "transform_pos_z")
    val transformPosZ: Float = 0f,

    @ColumnInfo(name = "transform_rot_x")
    val transformRotX: Float = 0f,

    @ColumnInfo(name = "transform_rot_y")
    val transformRotY: Float = 0f,

    @ColumnInfo(name = "transform_rot_z")
    val transformRotZ: Float = 0f,

    @ColumnInfo(name = "is_valid")
    val isValid: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
