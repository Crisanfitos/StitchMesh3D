package com.crisanfitos.stitchmesh3d.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.crisanfitos.stitchmesh3d.data.local.entity.PatternRoundEntity
import kotlinx.coroutines.flow.Flow

/**
 * Objeto de acceso a datos (DAO) de vueltas e hileras de patrón.
 */
@Dao
abstract class PatternRoundDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRound(round: PatternRoundEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRounds(rounds: List<PatternRoundEntity>)

    @Query("DELETE FROM pattern_rounds WHERE id = :id")
    abstract suspend fun deleteRoundById(id: String): Int

    @Query("DELETE FROM pattern_rounds WHERE part_id = :partId")
    abstract suspend fun deleteRoundsByPartId(partId: String): Int

    @Transaction
    open suspend fun replaceRoundsForPart(partId: String, rounds: List<PatternRoundEntity>) {
        deleteRoundsByPartId(partId)
        insertRounds(rounds)
    }

    @Query("SELECT * FROM pattern_rounds WHERE part_id = :partId ORDER BY round_number ASC")
    abstract fun getRoundsForPartFlow(partId: String): Flow<List<PatternRoundEntity>>
}
