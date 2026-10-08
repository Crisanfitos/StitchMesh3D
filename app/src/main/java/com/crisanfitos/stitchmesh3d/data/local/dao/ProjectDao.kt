package com.crisanfitos.stitchmesh3d.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.crisanfitos.stitchmesh3d.data.local.entity.ProjectEntity
import com.crisanfitos.stitchmesh3d.data.local.entity.ProjectWithPartsAndRounds
import kotlinx.coroutines.flow.Flow

/**
 * Objeto de acceso a datos (DAO) de proyectos de crochet.
 */
@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects ORDER BY updated_at DESC")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>

    @Transaction
    @Query("SELECT * FROM projects ORDER BY updated_at DESC")
    fun getAllProjectsWithDetailsFlow(): Flow<List<ProjectWithPartsAndRounds>>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectWithDetailsFlow(id: String): Flow<ProjectWithPartsAndRounds?>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectWithDetails(id: String): ProjectWithPartsAndRounds?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String): Int

    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects(): Int
}
