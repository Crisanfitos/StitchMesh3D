package com.crisanfitos.stitchmesh3d.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.crisanfitos.stitchmesh3d.domain.model.SyncReport
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * CoroutineWorker de AndroidX WorkManager para sincronización en background off-thread
 * con restricciones de red (NetworkType.CONNECTED) (SM-037, SM-089, RF-5.2).
 */
class SupabaseSyncCoroutineWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun syncEngine(): SyncEngine
    }

    override suspend fun doWork(): Result {
        return runCatching {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                SyncWorkerEntryPoint::class.java
            )
            val result = entryPoint.syncEngine().syncAllProjects()
            if (result.isSuccess) Result.success() else Result.retry()
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_ONE_TIME_WORK_NAME = "stitchmesh_sync_one_time"
        const val UNIQUE_PERIODIC_WORK_NAME = "stitchmesh_sync_periodic"

        fun enqueueOneTimeSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SupabaseSyncCoroutineWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun enqueuePeriodicSync(context: Context, repeatIntervalMinutes: Long = 15L) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SupabaseSyncCoroutineWorker>(
                repeatIntervalMinutes,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

/**
 * Trabajador en segundo plano y planificador reactivo de sincronización periódica y al reconectar (SM-037, SM-089).
 */
@Singleton
class SyncWorker @Inject constructor(
    private val syncEngine: SyncEngine
) {
    private var periodicJob: Job? = null

    /**
     * Ejecuta una pasada de sincronización inmediata directa.
     */
    suspend fun doWork(): Result<SyncReport> {
        return syncEngine.syncAllProjects()
    }

    /**
     * Encola una sincronización con WorkManager bajo restricción NetworkType.CONNECTED.
     */
    fun enqueueWithWorkManager(context: Context) {
        SupabaseSyncCoroutineWorker.enqueueOneTimeSync(context)
    }

    /**
     * Planifica sincronización periódica con WorkManager bajo restricción de red conectada.
     */
    fun schedulePeriodicWorkManager(context: Context, intervalMinutes: Long = 15L) {
        SupabaseSyncCoroutineWorker.enqueuePeriodicSync(context, intervalMinutes)
    }

    /**
     * Inicia un bucle periódico de sincronización en segundo plano mientras el scope permanezca activo.
     */
    fun startPeriodicSync(
        scope: CoroutineScope,
        intervalMillis: Long = 60_000L
    ) {
        periodicJob?.cancel()
        periodicJob = scope.launch {
            while (isActive) {
                syncEngine.syncAllProjects()
                delay(intervalMillis)
            }
        }
    }

    fun stopPeriodicSync() {
        periodicJob?.cancel()
        periodicJob = null
    }
}
