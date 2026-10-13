package com.udea.rutaudea.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.udea.rutaudea.data.local.dao.EncuestaDao
import com.udea.rutaudea.data.mapper.EncuestaMapper
import com.udea.rutaudea.domain.model.EncuestaSatisfaccion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Punto único de acceso al cuestionario de satisfacción de la prueba piloto.
 *
 * Estrategia **local-first** (igual que la racha): la respuesta se guarda
 * primero en Room (funciona sin conexión) y, cuando es posible, se sube a
 * Firestore (`encuestasSatisfaccion/{uid}`, un documento por usuario para
 * facilitar el análisis). Si la subida falla, queda marcada como pendiente
 * y se reintenta en la siguiente sincronización.
 *
 * Nunca lanza: los errores de red o de Firestore se registran y la cache
 * local queda intacta.
 */
class EncuestaRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val encuestaDao: EncuestaDao,
    private val ahoraMs: () -> Long = { System.currentTimeMillis() }
) {

    /**
     * Respuesta guardada del usuario (`null` si aún no responde). Lee de
     * Room primero; si no hay nada local, intenta bajarla de la nube y la
     * cachea.
     */
    suspend fun cargar(): EncuestaSatisfaccion? = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext null
        try {
            encuestaDao.getEncuesta(uid)?.let {
                return@withContext EncuestaMapper.toDomain(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo leer la encuesta local", e)
        }
        bajar(uid)?.also { remota ->
            try {
                encuestaDao.upsert(EncuestaMapper.toEntity(uid, remota, synced = true))
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo cachear la encuesta de la nube", e)
            }
            remota
        }
    }

    /**
     * Guarda la respuesta: valida, hace upsert local y la sube a Firestore
     * de forma best-effort. Devuelve el resultado con el estado de
     * sincronización.
     */
    suspend fun guardar(encuesta: EncuestaSatisfaccion): ResultadoGuardado =
        withContext(Dispatchers.IO + NonCancellable) {
            val uid = auth.currentUser?.uid ?: return@withContext ResultadoGuardado.SinSesion
            val errores = encuesta.validar()
            if (errores.isNotEmpty()) return@withContext ResultadoGuardado.Invalida(errores)

            val completa = encuesta.copy(fechaMs = ahoraMs())
            try {
                encuestaDao.upsert(EncuestaMapper.toEntity(uid, completa, synced = false))
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo guardar la encuesta localmente", e)
                return@withContext ResultadoGuardado.ErrorLocal
            }

            return@withContext if (subir(uid, completa)) {
                encuestaDao.marcarSincronizada(uid)
                ResultadoGuardado.Guardada(sincronizada = true)
            } else {
                ResultadoGuardado.Guardada(sincronizada = false)
            }
        }

    /**
     * Sube la respuesta pendiente (si la hay). Devuelve `true` si al
     * terminar no quedan pendientes.
     */
    suspend fun sincronizar(): Boolean = withContext(Dispatchers.IO + NonCancellable) {
        val uid = auth.currentUser?.uid ?: return@withContext false
        try {
            val local = encuestaDao.getEncuesta(uid) ?: return@withContext true
            if (local.synced) return@withContext true
            val dominio = EncuestaMapper.toDomain(local) ?: return@withContext true
            if (subir(uid, dominio)) {
                encuestaDao.marcarSincronizada(uid)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo sincronizar la encuesta con la nube", e)
            false
        }
    }

    private suspend fun bajar(uid: String): EncuestaSatisfaccion? = try {
        val doc = firestore.collection(COLECCION).document(uid).get().await()
        if (doc.exists()) EncuestaMapper.fromMap(doc.data) else null
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo leer la encuesta de la nube", e)
        null
    }

    private suspend fun subir(uid: String, encuesta: EncuestaSatisfaccion): Boolean = try {
        firestore.collection(COLECCION).document(uid)
            .set(EncuestaMapper.toMap(encuesta)).await()
        true
    } catch (e: Exception) {
        Log.w(TAG, "Encuesta guardada en local; pendiente de sincronizar", e)
        false
    }

    sealed interface ResultadoGuardado {
        data class Guardada(val sincronizada: Boolean) : ResultadoGuardado
        data class Invalida(val errores: List<String>) : ResultadoGuardado
        data object SinSesion : ResultadoGuardado
        data object ErrorLocal : ResultadoGuardado
    }

    companion object {
        private const val TAG = "EncuestaRepository"
        const val COLECCION = "encuestasSatisfaccion"
    }
}
