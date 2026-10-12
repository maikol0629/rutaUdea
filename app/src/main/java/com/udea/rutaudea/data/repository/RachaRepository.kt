package com.udea.rutaudea.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.udea.rutaudea.data.local.dao.RachaDao
import com.udea.rutaudea.data.local.entity.RachaEntity
import com.udea.rutaudea.data.mapper.RachaMapper
import com.udea.rutaudea.domain.model.Racha
import com.udea.rutaudea.domain.services.AnalizadorRacha
import com.udea.rutaudea.domain.services.Reloj
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Punto único de acceso a la racha de actividad del usuario.
 *
 * Estrategia **local-first**: el contador se calcula y guarda primero en
 * Room (funciona sin conexión y no se pierde al navegar) y, cuando es
 * posible, se sube a Firestore (`rachas/{uid}`). Si la subida falla, la
 * racha queda marcada como pendiente y se reintenta en la siguiente
 * sincronización.
 *
 * Si no hay sesión iniciada, la operación se omite (la app exige sesión,
 * pero el repositorio no asume que siempre la haya).
 */
class RachaRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val rachaDao: RachaDao,
    private val reloj: Reloj = Reloj.sistema
) {

    /** Racha almacenada (sin aplicar la vigencia de [Reloj.diaActual]). */
    suspend fun cargar(): Racha = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext Racha()
        leerLocal(uid)
    }

    /**
     * Racha lista para mostrar: aplica la vigencia según el día actual
     * (si el usuario lleva dos o más días sin actividad, se muestra 0).
     */
    suspend fun cargarVigente(): Racha {
        val racha = cargar()
        return AnalizadorRacha.vigente(racha, reloj.diaActual())
    }

    /**
     * Registra una actividad completada hoy: recalcula la racha, la guarda
     * en Room y la sube a Firestore de forma best-effort. Devuelve la
     * racha actualizada.
     */
    suspend fun registrarActividad(): Racha = withContext(Dispatchers.IO + NonCancellable) {
        val uid = auth.currentUser?.uid ?: return@withContext Racha()

        val nueva = AnalizadorRacha.aplicarActividad(
            racha = leerLocal(uid),
            dia = reloj.diaActual(),
            ahoraMs = reloj.ahoraMs()
        )
        try {
            rachaDao.upsert(RachaMapper.toEntity(uid, nueva, synced = false))
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo guardar la racha localmente", e)
            return@withContext nueva
        }

        if (subir(uid, nueva)) rachaDao.marcarSincronizada(uid)
        nueva
    }

    /**
     * Sincroniza en ambos sentidos:
     * 1. Si hay cambios locales pendientes, los sube a Firestore.
     * 2. Si no, descarga la racha de la nube y la fusiona con la local
     *    (conservando el mejor histórico).
     *
     * Nunca lanza: si no hay red o falla Firestore, deja intacta la cache.
     * Devuelve `true` si la operación con la nube fue correcta.
     */
    suspend fun sincronizar(): Boolean = withContext(Dispatchers.IO + NonCancellable) {
        val uid = auth.currentUser?.uid ?: return@withContext false
        try {
            val local = rachaDao.getRacha(uid)
            if (local != null && !local.synced) {
                if (subir(uid, RachaMapper.toDomain(local))) {
                    rachaDao.marcarSincronizada(uid)
                    true
                } else {
                    false
                }
            } else {
                fusionarConNube(uid, local)
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo sincronizar la racha con la nube", e)
            false
        }
    }

    private suspend fun fusionarConNube(uid: String, local: RachaEntity?): Boolean {
        val remoto = bajar(uid)
        if (remoto == null) {
            // No hay nada en la nube: sube lo local si existe.
            if (local == null) return true
            return if (subir(uid, RachaMapper.toDomain(local))) {
                rachaDao.marcarSincronizada(uid)
                true
            } else {
                false
            }
        }
        val fusion = AnalizadorRacha.fusionar(RachaMapper.toDomain(local), remoto)
        val subidaOk = subir(uid, fusion)
        rachaDao.upsert(RachaMapper.toEntity(uid, fusion, synced = subidaOk))
        return subidaOk
    }

    private suspend fun leerLocal(uid: String): Racha = try {
        RachaMapper.toDomain(rachaDao.getRacha(uid))
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo leer la racha local", e)
        Racha()
    }

    private suspend fun bajar(uid: String): Racha? = try {
        val doc = firestore.collection(COLECCION_RACHAS).document(uid).get().await()
        if (doc.exists()) leerDocumento(doc) else null
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo leer la racha de la nube", e)
        null
    }

    private fun leerDocumento(doc: DocumentSnapshot): Racha = Racha(
        actual = doc.getLong(CAMPO_ACTUAL)?.toInt() ?: 0,
        mejor = doc.getLong(CAMPO_MEJOR)?.toInt() ?: 0,
        ultimoDia = doc.getLong(CAMPO_ULTIMO_DIA),
        ultimaActividadMs = doc.getLong(CAMPO_ULTIMA_MS) ?: 0L
    )

    private suspend fun subir(uid: String, racha: Racha): Boolean = try {
        firestore.collection(COLECCION_RACHAS).document(uid).set(
            mapOf(
                CAMPO_ACTUAL to racha.actual,
                CAMPO_MEJOR to racha.mejor,
                CAMPO_ULTIMO_DIA to racha.ultimoDia,
                CAMPO_ULTIMA_MS to racha.ultimaActividadMs,
                CAMPO_ACTUALIZADO_MS to reloj.ahoraMs()
            )
        ).await()
        true
    } catch (e: Exception) {
        Log.w(TAG, "Racha guardada en local; pendiente de sincronizar", e)
        false
    }

    companion object {
        private const val TAG = "RachaRepository"
        const val COLECCION_RACHAS = "rachas"
        const val CAMPO_ACTUAL = "actual"
        const val CAMPO_MEJOR = "mejor"
        const val CAMPO_ULTIMO_DIA = "ultimoDia"
        const val CAMPO_ULTIMA_MS = "ultimaActividadMs"
        const val CAMPO_ACTUALIZADO_MS = "actualizadoMs"
    }
}
