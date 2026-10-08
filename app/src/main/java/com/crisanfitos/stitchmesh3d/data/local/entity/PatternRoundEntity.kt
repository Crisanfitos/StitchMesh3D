package com.crisanfitos.stitchmesh3d.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad de persistencia Room para vueltas de patrón con unicidad por pieza y vuelta.
 */
@Entity(
    tableName = "pattern_rounds",
    foreignKeys = [
        ForeignKey(
            entity = ProjectPartEntity::class,
            parentColumns = ["id"],
            childColumns = ["part_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["part_id"]),
        Index(value = ["part_id", "round_number"], unique = true)
    ]
)
data class PatternRoundEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "part_id")
    val partId: String,

    @ColumnInfo(name = "round_number")
    val roundNumber: Int,

    @ColumnInfo(name = "raw_instruction")
    val rawInstruction: String,

    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#E06D53",

    @ColumnInfo(name = "consumed_stitches")
    val consumedStitches: Int = 0,

    @ColumnInfo(name = "produced_stitches")
    val producedStitches: Int = 0,

    @ColumnInfo(name = "declared_stitches")
    val declaredStitches: Int? = null,

    @ColumnInfo(name = "is_valid")
    val isValid: Boolean = false,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
