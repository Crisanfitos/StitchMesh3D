package com.crisanfitos.stitchmesh3d.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.crisanfitos.stitchmesh3d.data.local.dao.PatternRoundDao
import com.crisanfitos.stitchmesh3d.data.local.dao.ProjectDao
import com.crisanfitos.stitchmesh3d.data.local.dao.ProjectPartDao
import com.crisanfitos.stitchmesh3d.data.local.dao.YarnGaugeStandardDao
import com.crisanfitos.stitchmesh3d.data.local.entity.PatternRoundEntity
import com.crisanfitos.stitchmesh3d.data.local.entity.ProjectEntity
import com.crisanfitos.stitchmesh3d.data.local.entity.ProjectPartEntity
import com.crisanfitos.stitchmesh3d.data.local.entity.YarnGaugeStandardEntity

/**
 * Base de datos principal Room para persistencia local de StitchMesh 3D.
 */
@Database(
    entities = [
        ProjectEntity::class,
        ProjectPartEntity::class,
        PatternRoundEntity::class,
        YarnGaugeStandardEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StitchMeshDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun projectPartDao(): ProjectPartDao
    abstract fun patternRoundDao(): PatternRoundDao
    abstract fun yarnGaugeStandardDao(): YarnGaugeStandardDao

    companion object {
        const val DATABASE_NAME = "stitchmesh_local.db"
    }
}
