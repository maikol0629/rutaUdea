package com.udea.rutaudea.data.sync

/**
 * Reglas del recordatorio local para sincronizar el banco.
 *
 * La app avisa cuando lleva al menos [DIAS_SIN_SYNC] días sin una
 * sincronización exitosa, contados desde la última sincronización o desde
 * el primer uso (lo que sea más reciente). El aviso tiene un enfriamiento
 * de [COOLDOWN_MS] para no repetirse todos los días.
 *
 * Código puro (sin Android) para poder probarlo en JVM.
 */
object PoliticaRecordatorio {

    const val DIAS_SIN_SYNC = 3
    const val COOLDOWN_MS = 24L * 60L * 60L * 1000L

    private const val DIA_MS = 24L * 60L * 60L * 1000L

    /**
     * Días completos transcurridos desde la referencia más reciente entre
     * la última sincronización y el primer uso. `0` si aún no hay datos.
     */
    fun diasSinSincronizar(
        ultimaSyncMs: Long,
        primeraUsoMs: Long,
        ahoraMs: Long
    ): Long {
        val referencia = maxOf(ultimaSyncMs, primeraUsoMs)
        if (referencia <= 0L) return 0L
        val transcurrido = ahoraMs - referencia
        if (transcurrido <= 0L) return 0L
        return transcurrido / DIA_MS
    }

    /** `true` si corresponde mostrar el recordatorio ahora. */
    fun debeRecordar(
        ultimaSyncMs: Long,
        primeraUsoMs: Long,
        ultimoAvisoMs: Long,
        ahoraMs: Long
    ): Boolean {
        if (diasSinSincronizar(ultimaSyncMs, primeraUsoMs, ahoraMs) < DIAS_SIN_SYNC) {
            return false
        }
        if (ultimoAvisoMs > 0L && ahoraMs - ultimoAvisoMs < COOLDOWN_MS) {
            return false
        }
        return true
    }
}
