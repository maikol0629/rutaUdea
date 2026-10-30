package com.udea.rutaudea.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Evalúa a diario si corresponde recordar al usuario que abra la app con
 * internet. No exige red: precisamente sirve para avisar cuando el
 * dispositivo lleva días sin poder sincronizar.
 */
class RecordatorioSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        RecordatorioSync.evaluarYNotificar(applicationContext)
        return Result.success()
    }
}
