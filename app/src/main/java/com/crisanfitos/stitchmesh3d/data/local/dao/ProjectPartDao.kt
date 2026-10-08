package com.crisanfitos.stitchmesh3d.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.crisanfitos.stitchmesh3d.data.local.entity.ProjectPartEntity
import kotlinx.coroutines.flow.Flow

/**
 * Objeto de acceso a datos (DAO) de piezas de proyectos.
 */
@Dao
interface ProjectPartDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPart(part: ProjectPartEntity)

    @Query("DELETE FROM project_parts WHERE id = :id")
    suspend fun deletePartById(id: String): Int

    @Query("SELECT * FROM project_parts WHERE project_id = :projectId ORDER BY sort_order ASC")
    fun getPartsForProjectFlow(projectId: String): Flow<List<ProjectPartEntity>>

    @Query("SELECT * FROM project_parts WHERE id = :id")
    suspend fun getPartById(id: String): ProjectPartEntity?
}
