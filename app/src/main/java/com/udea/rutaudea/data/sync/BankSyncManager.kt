package com.udea.rutaudea.data.sync

import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.udea.rutaudea.data.local.dao.QuestionDao
import com.udea.rutaudea.data.local.entity.QuestionEntity
import com.udea.rutaudea.domain.model.Question
import kotlinx.coroutines.tasks.await

/** Resultado observable de una sincronización del banco (D-02). */
sealed class ResultadoSync {
    /** No hay Firestore configurado (tests o build sin google-services). */
    data object SinConfiguracion : ResultadoSync()

    /** El panel todavía no publicó `bank_manifest/active`. */
    data object SinManifiesto : ResultadoSync()

    /** La versión local ya coincide con la publicada. */
    data class SinCambios(val bankVersion: String, val total: Int) : ResultadoSync()

    /** Lote descargado, validado y guardado en Room. */
    data class Actualizado(val bankVersion: String, val total: Int) : ResultadoSync()

    /** El lote no cumple el contrato; no se tocó Room. */
    data class Rechazado(val motivos: List<String>) : ResultadoSync()

    /** Fallo de red, permisos o Firestore; se conserva el banco local. */
    data class Error(val mensaje: String) : ResultadoSync()
}

/**
 * Sincronización híbrida del banco (D-02 §4).
 *
 * Room es la fuente de lectura; esta clase solo actualiza el banco en
 * segundo plano cuando el manifiesto activo declara una versión distinta.
 * Nunca bloquea la UI y nunca deja Room vacío: el reemplazo es atómico y
 * solo ocurre después de validar el lote completo.
 *
 * @param appVersionCode `BuildConfig.VERSION_CODE`, para `min_app_version`.
 * @param alActualizar callback opcional para pre-cachear imágenes.
 */
class BankSyncManager(
    private val firestore: FirebaseFirestore?,
    private val questionDao: QuestionDao,
    private val prefs: SharedPreferences,
    private val appVersionCode: Int,
    private val alActualizar: (List<QuestionEntity>) -> Unit = {}
) {

    /** Acceso a Firestore para lectura de configuración (settings). */
    internal val fs: FirebaseFirestore? = firestore

    /**
     * Consulta el manifiesto y sincroniza si hace falta.
     * @param forzar ignora la comparación de versión (acción manual).
     */
    suspend fun sincronizar(forzar: Boolean = false): ResultadoSync {
        val fs = firestore ?: return ResultadoSync.SinConfiguracion
        return try {
            val manifiestoSnap = fs
                .collection(BankManifest.COLECCION)
                .document(BankManifest.DOCUMENTO_ACTIVO)
                .get()
                .await()
            if (!manifiestoSnap.exists()) return ResultadoSync.SinManifiesto

            val manifiesto = BankManifest.desdeMapa(manifiestoSnap.data)
                ?: return ResultadoSync.Rechazado(listOf("manifiesto_invalido"))

            if (!manifiesto.estaActivo) {
                return ResultadoSync.Rechazado(listOf("manifiesto_no_activo:${manifiesto.status}"))
            }
            if (!manifiesto.versionSoportada) {
                return ResultadoSync.Rechazado(
                    listOf("schema_no_soportado:${manifiesto.schemaVersion}")
                )
            }

            val totalEsperado = manifiesto.counts.esperadas
            val versionLocal = prefs.getString(KEY_VERSION, null)
            val hashLocal = prefs.getString(KEY_HASH, null)
            val totalLocal = questionDao.countAll()
            val igual = versionLocal == manifiesto.bankVersion &&
                hashLocal == manifiesto.hash &&
                totalLocal == totalEsperado
            if (!forzar && igual) {
                // La comprobación fue exitosa aunque no haya cambios: sirve
                // para el diagnóstico y para el recordatorio de 3 días.
                prefs.edit()
                    .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                    .apply()
                return ResultadoSync.SinCambios(manifiesto.bankVersion, totalLocal)
            }

            val snapshot = fs.collection(COLECCION_QUESTIONS_V2)
                .whereEqualTo("bank_version", manifiesto.bankVersion)
                .whereIn("estado", ESTADOS_PUBLICABLES)
                .get()
                .await()

            val entidades = snapshot.documents.mapNotNull { doc ->
                PreguntaFirestoreMapper.desdeDocumento(doc.id, doc.data)
            }

            when (val validacion = BankValidator.validar(
                manifiesto,
                entidades,
                appVersionCode
            )) {
                is ResultadoValidacion.Invalido -> {
                    Log.w(TAG, "Lote rechazado: ${validacion.motivos.joinToString()}")
                    ResultadoSync.Rechazado(validacion.motivos)
                }

                is ResultadoValidacion.Valido -> {
                    // Escritura atómica: si falla, Room conserva la versión anterior.
                    questionDao.replaceAll(entidades)
                    prefs.edit()
                        .putString(KEY_VERSION, manifiesto.bankVersion)
                        .putString(KEY_HASH, manifiesto.hash)
                        .putInt(KEY_TOTAL, validacion.total)
                        .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
                        .putInt(KEY_SCHEMA, manifiesto.schemaVersion)
                        .apply()
                    runCatching { alActualizar(entidades) }
                        .onFailure { Log.w(TAG, "No se pudieron pre-cachear imágenes", it) }
                    ResultadoSync.Actualizado(manifiesto.bankVersion, validacion.total)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sincronización fallida; se conserva el banco local", e)
            ResultadoSync.Error(e.message ?: e.javaClass.simpleName)
        }
    }

    /** Versión local conocida (para mostrar en ajustes/diagnóstico). */
    fun versionLocal(): String? = prefs.getString(KEY_VERSION, null)

    /** Última comprobación exitosa del manifiesto en milisegundos, o 0. */
    fun ultimaSincronizacion(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    /** Huella (hash) del último lote aplicado, si existe. */
    fun hashLocal(): String? = prefs.getString(KEY_HASH, null)

    /** Total de preguntas del último lote aplicado (0 si nunca sincronizó). */
    fun totalLocal(): Int = prefs.getInt(KEY_TOTAL, 0)

    /** Versión de esquema del último lote aplicado (0 si nunca sincronizó). */
    fun schemaLocal(): Int = prefs.getInt(KEY_SCHEMA, 0)

    companion object {
        private const val TAG = "BankSync"
        const val COLECCION_QUESTIONS_V2 = "questions_v2"
        const val PREFS = "bank_sync"
        const val KEY_VERSION = "bank_version"
        const val KEY_HASH = "manifest_hash"
        const val KEY_TOTAL = "total"
        const val KEY_LAST_SYNC = "last_sync_at"
        const val KEY_SCHEMA = "schema_version"
        private val ESTADOS_PUBLICABLES = listOf(
            Question.ESTADO_APROBADO,
            Question.ESTADO_APROBADO_CON_IMAGEN
        )
    }
}
