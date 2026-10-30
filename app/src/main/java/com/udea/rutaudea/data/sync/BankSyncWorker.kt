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
 * Trabajo periódico de sincronización del banco (D-02 §4.1.3).
 * Solo corre con red disponible y conserva Room si algo falla.
 */
class BankSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? RutaUdeaApp ?: return Result.success()
        val resultado = app.bankSync.sincronizar()
        return when (resultado) {
            is ResultadoSync.Error -> Result.retry()
            is ResultadoSync.Actualizado -> {
                RecordatorioSync.cancelar(applicationContext)
                Result.success()
            }
            is ResultadoSync.SinCambios -> {
                RecordatorioSync.cancelar(applicationContext)
                Result.success()
            }
            else -> Result.success()
        }
    }
}

/** Programa la sincronización cada 24 h con reintento exponencial. */
object BankSyncScheduler {

    const val WORK_NAME = "bank_sync_periodico"
    const val WORK_RECORDATORIO = "recordatorio_sync"

    fun programar(context: Context) {
        programarSincronizacion(context)
        programarRecordatorio(context)
    }

    private fun programarSincronizacion(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<BankSyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Recordatorio diario sin restricción de red: si el equipo lleva más de
     * tres días sin sincronizar, la notificación local invita a abrir la app
     * con internet. La política de enfriamiento evita avisos repetidos.
     */
    private fun programarRecordatorio(context: Context) {
        val request = PeriodicWorkRequestBuilder<RecordatorioSyncWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_RECORDATORIO,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
