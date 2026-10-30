package com.udea.rutaudea.ui.screen.perfil

import com.udea.rutaudea.data.sync.ResultadoSync
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilidades de la tarjeta «Estado del banco» del perfil.
 *
 * Traduce el [ResultadoSync] a un mensaje legible y formatea los datos
 * que se muestran al usuario (fecha de sincronización y huella del lote).
 * Es código puro para poder probarlo sin Android.
 */
object BancoDiagnostico {

    /** Mensaje para mostrar tras una sincronización manual. */
    data class Mensaje(
        val texto: String,
        val esError: Boolean = false,
        val esExito: Boolean = false
    )

    fun mensaje(resultado: ResultadoSync): Mensaje = when (resultado) {
        ResultadoSync.SinConfiguracion -> Mensaje(
            texto = "Firestore no está configurado en este build.",
            esError = true
        )

        ResultadoSync.SinManifiesto -> Mensaje(
            texto = "El panel todavía no publicó un banco activo.",
            esError = true
        )

        is ResultadoSync.SinCambios -> Mensaje(
            texto = "Ya tienes la última versión (${resultado.bankVersion}, " +
                "${resultado.total} preguntas).",
            esExito = true
        )

        is ResultadoSync.Actualizado -> Mensaje(
            texto = "Banco actualizado a ${resultado.bankVersion}: " +
                "${resultado.total} preguntas.",
            esExito = true
        )

        is ResultadoSync.Rechazado -> Mensaje(
            texto = "El lote no se aplicó: ${resultado.motivos.joinToString()}",
            esError = true
        )

        is ResultadoSync.Error -> Mensaje(
            texto = "No se pudo actualizar: ${resultado.mensaje}",
            esError = true
        )
    }

    /** Fecha y hora local de la última sincronización; `null` si nunca hubo. */
    fun formatearFechaHora(ms: Long): String? {
        if (ms <= 0L) return null
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return formato.format(Date(ms))
    }

    /** Primeros 12 caracteres de la huella, con elipsis. */
    fun huellaCorta(hash: String?): String? {
        val limpio = hash?.trim().orEmpty()
        if (limpio.isEmpty()) return null
        return if (limpio.length <= 12) limpio else limpio.take(12) + "…"
    }
}
