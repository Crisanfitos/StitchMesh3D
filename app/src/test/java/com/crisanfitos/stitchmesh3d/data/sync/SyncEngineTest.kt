package com.crisanfitos.stitchmesh3d.data.sync

import com.crisanfitos.stitchmesh3d.data.remote.sync.SupabasePostgrestSyncGateway
import com.crisanfitos.stitchmesh3d.domain.model.AuthSession
import com.crisanfitos.stitchmesh3d.domain.model.AuthState
import com.crisanfitos.stitchmesh3d.domain.model.PatternRound
import com.crisanfitos.stitchmesh3d.domain.model.Project
import com.crisanfitos.stitchmesh3d.domain.model.ProjectPart
import com.crisanfitos.stitchmesh3d.domain.model.SyncStatus
import com.crisanfitos.stitchmesh3d.domain.model.UserProfile
import com.crisanfitos.stitchmesh3d.domain.repository.AuthRepository
import com.crisanfitos.stitchmesh3d.domain.repository.ProjectRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncEngineTest {

    private class FakeProjectRepository(initial: List<Project> = emptyList()) : ProjectRepository {
        private val projectsState = MutableStateFlow(initial.associateBy { it.id })

        override fun getAllProjectsFlow(): Flow<List<Project>> = projectsState.map { it.values.toList() }
        override suspend fun getProjectById(projectId: String): Project? = projectsState.value[projectId]
        override fun getProjectByIdFlow(projectId: String): Flow<Project?> = projectsState.map { it[projectId] }

        override suspend fun insertOrUpdateProject(project: Project) {
            projectsState.value = projectsState.value.toMutableMap().apply { put(project.id, project) }
        }

        override suspend fun deleteProjectById(projectId: String) {
            projectsState.value = projectsState.value.toMutableMap().apply { remove(projectId) }
        }

        override suspend fun insertPart(part: ProjectPart) = Unit
        override suspend fun deletePartById(partId: String) = Unit
        override suspend fun insertRound(round: PatternRound) = Unit
        override suspend fun deleteRoundById(roundId: String) = Unit
        override suspend fun replaceRoundsForPart(partId: String, rounds: List<PatternRound>) = Unit
    }

    private class FakeAuthRepository(authenticated: Boolean = true) : AuthRepository {
        private val _state = MutableStateFlow<AuthState>(
            if (authenticated) {
                AuthState.Authenticated(
                    AuthSession(
                        accessToken = "jwt_token",
                        refreshToken = "refresh_token",
                        expiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 3600L,
                        user = UserProfile("user-1", "cris@stitchmesh.dev", "crisanfitos", "Cris")
                    )
                )
            } else {
                AuthState.Unauthenticated
            }
        )
        override val authState: StateFlow<AuthState> = _state.asStateFlow()

        override suspend fun signUpWithEmail(email: String, password: String, username: String, displayName: String?) =
            Result.failure<AuthSession>(UnsupportedOperationException())

        override suspend fun signInWithEmail(email: String, password: String) =
            Result.failure<AuthSession>(UnsupportedOperationException())

        override suspend fun restoreSession() = Result.success<AuthSession?>(null)
        override suspend fun updateProfile(username: String, displayName: String?) =
            Result.failure<UserProfile>(UnsupportedOperationException())

        override suspend fun signOut() = Result.success(Unit)
    }

    private class FakeSyncGateway(
        initialRemote: List<Project> = emptyList(),
        var shouldFailNetwork: Boolean = false
    ) : SupabasePostgrestSyncGateway {
        val remoteStore = initialRemote.associateBy { it.id }.toMutableMap()

        override suspend fun fetchRemoteProjects(accessToken: String, userId: String): List<Project> {
            if (shouldFailNetwork) throw IllegalStateException("Network unreachable")
            return remoteStore.values.toList()
        }

        override suspend fun upsertProjectGraph(accessToken: String, userId: String, project: Project) {
            if (shouldFailNetwork) throw IllegalStateException("Network unreachable")
            remoteStore[project.id] = project
        }
    }

    @Test
    fun `offline created project is uploaded to Supabase when connection is available`() = runTest {
        val localProject = Project(id = "proj-1", title = "Pulpo Offline", updatedAt = 1_000L)
        val projectRepo = FakeProjectRepository(listOf(localProject))
        val gateway = FakeSyncGateway()
        val engine = OfflineFirstSyncEngine(
            projectRepository = projectRepo,
            authRepository = FakeAuthRepository(authenticated = true),
            syncGateway = gateway,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        val report = engine.syncAllProjects().getOrThrow()

        assertEquals(1, report.uploadedProjects)
        assertEquals(0, report.downloadedProjects)
        assertEquals("Pulpo Offline", gateway.remoteStore["proj-1"]?.title)
        assertEquals(SyncStatus.SYNCED, engine.syncStatus.value)
    }

    @Test
    fun `remote download does not overwrite more recent local changes (last-write-wins)`() = runTest {
        val newerLocal = Project(id = "proj-1", title = "Versión Local Reciente", updatedAt = 5_000L)
        val olderRemote = Project(id = "proj-1", title = "Versión Remota Antigua", updatedAt = 2_000L)

        val projectRepo = FakeProjectRepository(listOf(newerLocal))
        val gateway = FakeSyncGateway(listOf(olderRemote))
        val engine = OfflineFirstSyncEngine(
            projectRepository = projectRepo,
            authRepository = FakeAuthRepository(authenticated = true),
            syncGateway = gateway,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        val report = engine.syncAllProjects().getOrThrow()

        assertEquals(1, report.conflictsResolvedLocalWins)
        assertEquals(0, report.conflictsResolvedRemoteWins)
        assertEquals("Versión Local Reciente", projectRepo.getProjectById("proj-1")?.title)
        assertEquals("Versión Local Reciente", gateway.remoteStore["proj-1"]?.title)
    }

    @Test
    fun `newer remote project updates older local project and SyncWorker executes cleanly`() = runTest {
        val olderLocal = Project(id = "proj-1", title = "Versión Local Antigua", updatedAt = 1_500L)
        val newerRemote = Project(id = "proj-1", title = "Versión Remota Nueva", updatedAt = 9_000L)

        val projectRepo = FakeProjectRepository(listOf(olderLocal))
        val gateway = FakeSyncGateway(listOf(newerRemote))
        val engine = OfflineFirstSyncEngine(
            projectRepository = projectRepo,
            authRepository = FakeAuthRepository(authenticated = true),
            syncGateway = gateway,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )
        val worker = SyncWorker(engine)

        val report = worker.doWork().getOrThrow()

        assertEquals(0, report.conflictsResolvedLocalWins)
        assertEquals(1, report.conflictsResolvedRemoteWins)
        assertEquals("Versión Remota Nueva", projectRepo.getProjectById("proj-1")?.title)
    }

    @Test
    fun `network failure preserves local data and sets status to PENDING`() = runTest {
        val localProject = Project(id = "proj-1", title = "Proyecto Seguro", updatedAt = 3_000L)
        val projectRepo = FakeProjectRepository(listOf(localProject))
        val gateway = FakeSyncGateway(shouldFailNetwork = true)
        val engine = OfflineFirstSyncEngine(
            projectRepository = projectRepo,
            authRepository = FakeAuthRepository(authenticated = true),
            syncGateway = gateway,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        val result = engine.syncAllProjects()

        assertTrue(result.isFailure)
        assertEquals(SyncStatus.PENDING, engine.syncStatus.value)
        assertEquals("Proyecto Seguro", projectRepo.getProjectById("proj-1")?.title)
    }
}
