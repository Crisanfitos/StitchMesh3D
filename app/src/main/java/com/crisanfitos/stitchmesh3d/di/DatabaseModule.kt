package com.crisanfitos.stitchmesh3d.di

import android.content.Context
import androidx.room.Room
import com.crisanfitos.stitchmesh3d.data.local.StitchMeshDatabase
import com.crisanfitos.stitchmesh3d.data.local.dao.PatternRoundDao
import com.crisanfitos.stitchmesh3d.data.local.dao.ProjectDao
import com.crisanfitos.stitchmesh3d.data.local.dao.ProjectPartDao
import com.crisanfitos.stitchmesh3d.data.local.dao.YarnGaugeStandardDao
import com.crisanfitos.stitchmesh3d.data.repository.ProjectRepositoryImpl
import com.crisanfitos.stitchmesh3d.domain.repository.ProjectRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de inyección de dependencias para Room Database y sus DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideStitchMeshDatabase(
        @ApplicationContext context: Context
    ): StitchMeshDatabase {
        return Room.databaseBuilder(
            context,
            StitchMeshDatabase::class.java,
            StitchMeshDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    @Provides
    fun provideProjectDao(database: StitchMeshDatabase): ProjectDao {
        return database.projectDao()
    }

    @Provides
    fun provideProjectPartDao(database: StitchMeshDatabase): ProjectPartDao {
        return database.projectPartDao()
    }

    @Provides
    fun providePatternRoundDao(database: StitchMeshDatabase): PatternRoundDao {
        return database.patternRoundDao()
    }

    @Provides
    fun provideYarnGaugeStandardDao(database: StitchMeshDatabase): YarnGaugeStandardDao {
        return database.yarnGaugeStandardDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProjectRepository(
        impl: ProjectRepositoryImpl
    ): ProjectRepository
}
