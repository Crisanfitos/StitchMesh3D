package com.crisanfitos.stitchmesh3d.data.sync

import com.crisanfitos.stitchmesh3d.data.remote.sync.SupabasePostgrestSyncGateway
import com.crisanfitos.stitchmesh3d.di.IoDispatcher
import com.crisanfitos.stitchmesh3d.domain.model.AuthState
import com.crisanfitos.stitchmesh3d.domain.model.SyncReport
import com.crisanfitos.stitchmesh3d.domain.model.SyncStatus
import com.crisanfitos.stitchmesh3d.domain.repository.AuthRepository
import com.crisanfitos.stitchmesh3d.domain.repository.ProjectRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Contrato del motor de sincronización offline-first bidireccional (SM-037, RF-5.2).
 */
interface SyncEngine {
    val syncStatus: StateFlow<SyncStatus>
    fun markPendingChanges()
    suspend fun syncAllProjects(): Result<SyncReport>
}

/**
 * Motor de sincronización offline-first entre Room (`ProjectRepository`) y Supabase PostgREST.
 * Aplica resolución de conflictos *last-write-wins* basada en marcas de tiempo UTC (`updatedAt`).
 */
@Singleton
class OfflineFirstSyncEngine @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository,
    private val syncGateway: SupabasePostgrestSyncGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SyncEngine {

    private val _syncStatus = MutableStateFlow(SyncStatus.SYNCED)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    override fun markPendingChanges() {
        if (_syncStatus.value != SyncStatus.IN_PROGRESS) {
            _syncStatus.value = SyncStatus.PENDING
        }
    }

    override suspend fun syncAllProjects(): Result<SyncReport> = withContext(ioDispatcher) {
        val currentAuth = authRepository.authState.value
        if (currentAuth !is AuthState.Authenticated) {
            _syncStatus.value = SyncStatus.OFFLINE
            return@withContext Result.failure(IllegalStateException("Usuario no autenticado para sincronización remota"))
        }

        _syncStatus.value = SyncStatus.IN_PROGRESS

        runCatching {
            val accessToken = currentAuth.session.accessToken
            val userId = currentAuth.session.user.id

            val localProjects = projectRepository.getAllProjectsFlow().first().associateBy { it.id }
            val remoteProjects = syncGateway.fetchRemoteProjects(accessToken, userId).associateBy { it.id }

            var uploaded = 0
            var downloaded = 0
            var localWins = 0
            var remoteWins = 0

            val allProjectIds = (localProjects.keys + remoteProjects.keys).toSet()

            for (projectId in allProjectIds) {
                val local = localProjects[projectId]
                val remote = remoteProjects[projectId]

                when {
                    local != null && remote == null -> {
                        syncGateway.upsertProjectGraph(accessToken, userId, local)
                        uploaded++
                    }

                    local == null && remote != null -> {
                        projectRepository.insertOrUpdateProject(remote)
                        downloaded++
                    }

                    local != null && remote != null -> {
                        // Estrategia Last-Write-Wins basada en updatedAt (UTC epoch millis)
                        when {
                            local.updatedAt > remote.updatedAt -> {
                                syncGateway.upsertProjectGraph(accessToken, userId, local)
                                uploaded++
                                localWins++
                            }

                            remote.updatedAt > local.updatedAt -> {
                                projectRepository.insertOrUpdateProject(remote)
                                downloaded++
                                remoteWins++
                            }
                        }
                    }
                }
            }

            _syncStatus.value = SyncStatus.SYNCED
            SyncReport(
                uploadedProjects = uploaded,
                downloadedProjects = downloaded,
                conflictsResolvedLocalWins = localWins,
                conflictsResolvedRemoteWins = remoteWins
            )
        }.onFailure {
            _syncStatus.value = SyncStatus.PENDING
        }
    }
}
