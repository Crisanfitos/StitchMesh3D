package com.crisanfitos.stitchmesh3d.domain.model

/**
 * Estado visual y operativo del motor de sincronización offline-first (RF-5.2, SM-037).
 */
enum class SyncStatus(val labelEs: String) {
    SYNCED("Sincronizado"),
    PENDING("Cambios pendientes"),
    IN_PROGRESS("Sincronizando…"),
    OFFLINE("Modo offline")
}

/**
 * Resumen estadístico de una ejecución de sincronización bidireccional Room <-> Supabase.
 */
data class SyncReport(
    val uploadedProjects: Int = 0,
    val downloadedProjects: Int = 0,
    val conflictsResolvedLocalWins: Int = 0,
    val conflictsResolvedRemoteWins: Int = 0
)
