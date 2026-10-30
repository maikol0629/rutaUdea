package com.udea.rutaudea.data.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.udea.rutaudea.MainActivity
import com.udea.rutaudea.R

/**
 * Notificación local que invita a abrir la app con internet cuando el
 * banco lleva varios días sin sincronizarse.
 *
 * Usa las mismas preferencias que [BankSyncManager] para leer la última
 * sincronización exitosa y recuerda cuándo se mostró el último aviso.
 */
object RecordatorioSync {

    const val CANAL_ID = "actualizacion_banco"
    const val NOTIFICACION_ID = 4101
    private const val KEY_PRIMER_USO = "primer_uso_at"
    private const val KEY_ULTIMO_AVISO = "ultimo_aviso_at"

    /** Guarda el primer uso para no avisar en una instalación recién hecha. */
    fun registrarPrimerUso(context: Context, ahora: Long = System.currentTimeMillis()) {
        val prefs = prefs(context)
        if (!prefs.contains(KEY_PRIMER_USO)) {
            prefs.edit().putLong(KEY_PRIMER_USO, ahora).apply()
        }
    }

    /**
     * Evalúa la política y, si corresponde, publica la notificación.
     * @return `true` si se mostró el aviso.
     */
    fun evaluarYNotificar(
        context: Context,
        ahora: Long = System.currentTimeMillis()
    ): Boolean {
        val prefs = prefs(context)
        val primeraUso = prefs.getLong(KEY_PRIMER_USO, 0L)
        val ultimaSync = prefs.getLong(BankSyncManager.KEY_LAST_SYNC, 0L)
        val ultimoAviso = prefs.getLong(KEY_ULTIMO_AVISO, 0L)

        if (!PoliticaRecordatorio.debeRecordar(ultimaSync, primeraUso, ultimoAviso, ahora)) {
            return false
        }
        if (!puedeNotificar(context)) return false

        notificar(context)
        prefs.edit().putLong(KEY_ULTIMO_AVISO, ahora).apply()
        return true
    }

    /** Quita el recordatorio cuando una sincronización ya fue exitosa. */
    fun cancelar(context: Context) {
        context.getSystemService(NotificationManager::class.java)
            ?.cancel(NOTIFICACION_ID)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(BankSyncManager.PREFS, Context.MODE_PRIVATE)

    private fun puedeNotificar(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    private fun notificar(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val canal = NotificationChannel(
            CANAL_ID,
            context.getString(R.string.sync_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.sync_channel_desc)
        }
        manager.createNotificationChannel(canal)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val texto = context.getString(R.string.sync_reminder_text)
        val notificacion = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.drawable.ic_notif_sync)
            .setContentTitle(context.getString(R.string.sync_reminder_title))
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICACION_ID, notificacion)
    }
}
