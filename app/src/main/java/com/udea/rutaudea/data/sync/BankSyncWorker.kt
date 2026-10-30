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
        return when (app.bankSync.sincronizar()) {
            is ResultadoSync.Error -> Result.retry()
            else -> Result.success()
        }
    }
}

/** Programa la sincronización cada 24 h con reintento exponencial. */
object BankSyncScheduler {

    const val WORK_NAME = "bank_sync_periodico"

    fun programar(context: Context) {
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
}
