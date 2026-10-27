package com.crisanfitos.stitchmesh3d.data.sync

import com.crisanfitos.stitchmesh3d.domain.model.SyncReport
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Trabajador en segundo plano y planificador reactivo de sincronización periódica y al reconectar (SM-037).
 */
@Singleton
class SyncWorker @Inject constructor(
    private val syncEngine: SyncEngine
) {
    private var periodicJob: Job? = null

    /**
     * Ejecuta una pasada de sincronización inmediata (p. ej. al reconectar a red o guardar cambios).
     */
    suspend fun doWork(): Result<SyncReport> {
        return syncEngine.syncAllProjects()
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
