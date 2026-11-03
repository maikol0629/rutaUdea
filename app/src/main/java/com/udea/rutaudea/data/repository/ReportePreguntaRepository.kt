package com.udea.rutaudea.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.udea.rutaudea.data.local.dao.ReportePreguntaDao
import com.udea.rutaudea.data.mapper.ReportePreguntaMapper
import com.udea.rutaudea.domain.model.ReportePregunta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * Punto único de acceso a los reportes de preguntas.
 *
 * Estrategia **local-first**: el reporte se guarda primero en Room
 * (funciona sin conexión) y, cuando es posible, se sube a Firestore
 * (`reportesPreguntas/{id}`). Si la subida falla, queda marcado como
 * pendiente y se reintenta al volver a la app o desde WorkManager.
 *
 * Nunca lanza: los errores de red o de Firestore se registran y la cache
 * local queda intacta.
 */
class ReportePreguntaRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val dao: ReportePreguntaDao,
    private val appVersionCode: Int = 0,
    private val ahoraMs: () -> Long = { System.currentTimeMillis() },
    private val nuevoId: () -> String = { UUID.randomUUID().toString() }
) {

    /**
     * Guarda el reporte localmente y hace un intento best-effort de
     * subirlo. Devuelve el estado de sincronización para que la UI pueda
     * informar «enviado» o «pendiente de conexión».
     */
    suspend fun guardar(reporte: ReportePregunta): ResultadoReporte =
        withContext(Dispatchers.IO + NonCancellable) {
            val uid = auth.currentUser?.uid
                ?: return@withContext ResultadoReporte.SinSesion

            val errores = reporte.validar()
            if (errores.isNotEmpty()) {
                return@withContext ResultadoReporte.Invalido(errores)
            }

            try {
                if (dao.countReporte(uid, reporte.questionId, reporte.sesionId) > 0) {
                    return@withContext ResultadoReporte.Duplicado
                }
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo comprobar el reporte local", e)
                return@withContext ResultadoReporte.ErrorLocal
            }

            val completo = reporte.copy(
                id = nuevoId(),
                fechaMs = ahoraMs(),
                appVersionCode = appVersionCode
            )

            try {
                dao.upsert(
                    ReportePreguntaMapper.toEntity(uid, completo, synced = false)
                )
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo guardar el reporte localmente", e)
                return@withContext ResultadoReporte.ErrorLocal
            }

            val sincronizada = subirConTimeout(uid, completo)
            if (sincronizada) {
                try {
                    dao.marcarSincronizado(completo.id)
                } catch (e: Exception) {
                    Log.w(TAG, "Reporte subido, pero no se pudo marcar como sincronizado", e)
                }
            }
            ResultadoReporte.Guardado(sincronizada)
        }

    /**
     * Reintenta subir los reportes pendientes del usuario actual.
     * Devuelve `true` si al terminar no quedan pendientes.
     */
    suspend fun sincronizarPendientes(): Boolean =
        withContext(Dispatchers.IO + NonCancellable) {
            val uid = auth.currentUser?.uid ?: return@withContext false
            val pendientes = try {
                dao.getPendientes(uid)
            } catch (e: Exception) {
                Log.e(TAG, "No se pudieron leer los reportes pendientes", e)
                return@withContext false
            }
            if (pendientes.isEmpty()) return@withContext true

            var ok = true
            pendientes.forEach { entity ->
                val dominio = ReportePreguntaMapper.toDomain(entity)
                if (subirConTimeout(uid, dominio)) {
                    try {
                        dao.marcarSincronizado(entity.id)
                    } catch (e: Exception) {
                        Log.w(TAG, "Reporte subido, pero no se pudo marcar como sincronizado", e)
                        ok = false
                    }
                } else {
                    ok = false
                }
            }
            ok
        }

    /**
     * IDs de preguntas ya reportadas en una sesión. Se usa al restaurar
     * la pantalla para que el icono aparezca deshabilitado si el proceso
     * murió después de enviar el reporte.
     */
    suspend fun getReportadasDeSesion(sesionId: String): Set<String> =
        withContext(Dispatchers.IO) {
            val uid = auth.currentUser?.uid ?: return@withContext emptySet()
            try {
                dao.getQuestionIdsReportadas(uid, sesionId).toSet()
            } catch (e: Exception) {
                Log.w(TAG, "No se pudieron leer los reportes de la sesión", e)
                emptySet()
            }
        }

    private suspend fun subirConTimeout(
        uid: String,
        reporte: ReportePregunta
    ): Boolean = withTimeoutOrNull(TIMEOUT_SUBIDA_MS) {
        try {
            firestore.collection(COLECCION)
                .document(reporte.id)
                .set(ReportePreguntaMapper.toMap(uid, reporte))
                .await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Reporte guardado en local; pendiente de sincronizar", e)
            false
        }
    } ?: false

    sealed interface ResultadoReporte {
        data class Guardado(val sincronizada: Boolean) : ResultadoReporte
        data class Invalido(val errores: List<String>) : ResultadoReporte
        data object Duplicado : ResultadoReporte
        data object SinSesion : ResultadoReporte
        data object ErrorLocal : ResultadoReporte
    }

    companion object {
        private const val TAG = "ReportePreguntaRepository"
        const val COLECCION = "reportesPreguntas"

        /** Evita que la UI espere indefinidamente si Firestore no responde. */
        private const val TIMEOUT_SUBIDA_MS = 8_000L
    }
}
