package com.crisanfitos.stitchmesh3d.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.crisanfitos.stitchmesh3d.data.local.entity.YarnGaugeStandardEntity
import kotlinx.coroutines.flow.Flow

/**
 * Objeto de acceso a datos (DAO) de estándares de tensión CYC.
 */
@Dao
interface YarnGaugeStandardDao {

    @Query("SELECT * FROM yarn_gauge_standards ORDER BY yarn_weight_category ASC, hook_size_mm ASC")
    fun getAllGaugeStandardsFlow(): Flow<List<YarnGaugeStandardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(standards: List<YarnGaugeStandardEntity>)

    @Query("SELECT COUNT(*) FROM yarn_gauge_standards")
    suspend fun getCount(): Int
}
