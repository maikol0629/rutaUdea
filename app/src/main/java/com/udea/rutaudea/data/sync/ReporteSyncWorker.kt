package com.udea.rutaudea.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.udea.rutaudea.RutaUdeaApp
import java.util.concurrent.TimeUnit

/**
 * Trabajo periódico que reintenta subir los reportes de preguntas que
 * quedaron pendientes por falta de conexión.
 */
class ReporteSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? RutaUdeaApp ?: return Result.success()
        return if (app.reporteRepository.sincronizarPendientes()) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}

/** Programa la sincronización de reportes cada 6 h con red disponible. */
object ReporteSyncScheduler {

    const val WORK_NAME = "reportes_sync_periodico"

    fun programar(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<ReporteSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
