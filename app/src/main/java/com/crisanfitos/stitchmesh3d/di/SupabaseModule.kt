package com.crisanfitos.stitchmesh3d.di

import com.crisanfitos.stitchmesh3d.data.remote.SupabaseConfig
import com.crisanfitos.stitchmesh3d.data.remote.auth.HttpUrlSupabaseAuthGateway
import com.crisanfitos.stitchmesh3d.data.remote.auth.SessionTokenStore
import com.crisanfitos.stitchmesh3d.data.remote.auth.SharedPreferencesSessionTokenStore
import com.crisanfitos.stitchmesh3d.data.remote.auth.SupabaseAuthGateway
import com.crisanfitos.stitchmesh3d.data.remote.sync.HttpUrlSupabasePostgrestSyncGateway
import com.crisanfitos.stitchmesh3d.data.remote.sync.SupabasePostgrestSyncGateway
import com.crisanfitos.stitchmesh3d.data.repository.AuthRepositoryImpl
import com.crisanfitos.stitchmesh3d.data.sync.OfflineFirstSyncEngine
import com.crisanfitos.stitchmesh3d.data.sync.SyncEngine
import com.crisanfitos.stitchmesh3d.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt para configuración remota de Supabase, autenticación y sincronización (SM-036, SM-037).
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseNetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseConfig(): SupabaseConfig = SupabaseConfig()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SupabaseBindingsModule {

    @Binds
    @Singleton
    abstract fun bindSessionTokenStore(
        impl: SharedPreferencesSessionTokenStore
    ): SessionTokenStore

    @Binds
    @Singleton
    abstract fun bindSupabaseAuthGateway(
        impl: HttpUrlSupabaseAuthGateway
    ): SupabaseAuthGateway

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSupabasePostgrestSyncGateway(
        impl: HttpUrlSupabasePostgrestSyncGateway
    ): SupabasePostgrestSyncGateway

    @Binds
    @Singleton
    abstract fun bindSyncEngine(
        impl: OfflineFirstSyncEngine
    ): SyncEngine
}
