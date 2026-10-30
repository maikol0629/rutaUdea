package com.udea.rutaudea.data.repository

import com.udea.rutaudea.data.local.dao.QuestionDao
import com.udea.rutaudea.data.local.entity.QuestionEntity
import com.udea.rutaudea.data.mapper.QuestionMapper
import com.udea.rutaudea.data.sync.BankSyncManager
import com.udea.rutaudea.data.sync.ResultadoSync
import com.udea.rutaudea.domain.model.DistribucionDificultad
import com.udea.rutaudea.domain.model.Question
import com.udea.rutaudea.domain.model.SimulacroConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Punto único de acceso a las preguntas.
 *
 * La fuente principal de consulta es la base local (Room) para
 * garantizar rapidez y funcionamiento offline. La actualización del banco
 * la hace [BankSyncManager] contra `questions_v2` (D-02).
 */
class QuestionRepository(
    private val questionDao: QuestionDao,
    private val bankSync: BankSyncManager? = null
) {

    // ----- LECTURA LOCAL (práctica y simulacro) -----

    /** Consulta filtrada por área, subtema y dificultad. `null` = sin filtro. */
    suspend fun getQuestions(
        area: String? = null,
        subtema: String? = null,
        dificultad: String? = null,
        limit: Int = 20
    ): List<Question> {
        val entities = questionDao.getQuestions(area, subtema, dificultad, limit)
        return QuestionMapper.listToDomain(entities)
    }

    /** Preguntas de un área para el simulacro (40 RL o 40 CL). */
    suspend fun getQuestionsForSimulacro(area: String, limit: Int): List<Question> {
        val entities = questionDao.getQuestionsByArea(area, limit)
        return QuestionMapper.listToDomain(entities)
    }

    suspend fun getById(id: String): Question? =
        questionDao.getById(id)?.let(QuestionMapper::entityToDomain)

    suspend fun getQuestionsBySubtema(subtema: String): List<Question> =
        QuestionMapper.listToDomain(questionDao.getQuestionsBySubtema(subtema))

    fun observeQuestions(): Flow<List<Question>> =
        questionDao.observeQuestions().map { QuestionMapper.listToDomain(it) }

    // ----- CONSULTAS AUXILIARES -----

    suspend fun countQuestions(): Int = questionDao.countQuestions()

    suspend fun countAll(): Int = questionDao.countAll()

    suspend fun countByArea(area: String): Int = questionDao.countByArea(area)

    suspend fun getDistinctAreas(): List<String> = questionDao.getDistinctAreas()

    suspend fun getDistinctSubtemas(): List<String> = questionDao.getDistinctSubtemas()

    /** Subtemas con preguntas aprobadas de un área (filtros del módulo de práctica). */
    suspend fun getDistinctSubtemas(area: String): List<String> =
        questionDao.getDistinctSubtemasByArea(area)

    suspend fun getDistinctDificultades(): List<String> = questionDao.getDistinctDificultades()

    /** Preguntas disponibles con los filtros de práctica (área y/o subtema). */
    suspend fun countByFilters(area: String? = null, subtema: String? = null): Int =
        questionDao.countByFilters(area, subtema)

    /** Versión del banco instalada (asset o última sincronización). */
    fun versionBanco(): String? = bankSync?.versionLocal()

    /**
     * Datos de diagnóstico del banco local: versión, huella, total y fecha
     * de la última sincronización. `null` si la sincronización no está
     * configurada (tests o build sin google-services).
     */
    fun infoBanco(): InfoBanco? = bankSync?.let { sync ->
        InfoBanco(
            version = sync.versionLocal(),
            hash = sync.hashLocal(),
            totalSincronizado = sync.totalLocal(),
            ultimaSincronizacion = sync.ultimaSincronizacion(),
            schemaVersion = sync.schemaLocal()
        )
    }

    /** Resumen del estado del banco local para la pantalla de diagnóstico. */
    data class InfoBanco(
        val version: String?,
        val hash: String?,
        val totalSincronizado: Int,
        val ultimaSincronizacion: Long,
        val schemaVersion: Int
    )

    // ----- CONFIGURACIÓN DEL SIMULACRO (settings/config) -----

    /**
     * Lee la configuración del simulacro desde `settings/config` en Firestore.
     * Devuelve valores por defecto si el documento no existe o no se puede leer.
     */
    suspend fun leerConfiguracionSimulacro(): SimulacroConfig {
        val fs = bankSync?.fs ?: return SimulacroConfig()
        return try {
            val snap = fs
                .collection("settings")
                .document("config")
                .get()
                .await()
            if (!snap.exists()) return SimulacroConfig()
            val data = snap.data ?: return SimulacroConfig()
            val d = data["distribucion_dificultad"] as? Map<*, *>
            SimulacroConfig(
                tiempoSimulacroMinutos =
                    (data["tiempo_simulacro_minutos"] as? Number)?.toInt() ?: 120,
                inactividadMaxMinutos =
                    (data["inactividad_max_minutos"] as? Number)?.toInt() ?: 30,
                politicaRepeticion = data["politica_repeticion"] as? String ?: "ultimos_3",
                distribucionDificultad = if (d != null) {
                    DistribucionDificultad(
                        baja = (d["baja"] as? Number)?.toInt() ?: 25,
                        media = (d["media"] as? Number)?.toInt() ?: 50,
                        alta = (d["alta"] as? Number)?.toInt() ?: 25
                    )
                } else {
                    DistribucionDificultad(25, 50, 25)
                }
            )
        } catch (e: Exception) {
            SimulacroConfig()
        }
    }

    // ----- SEED (carga inicial) -----

    /** Reemplaza el banco local por completo (usado al cargar el JSONL v2). */
    suspend fun replaceBankEntities(entities: List<QuestionEntity>) {
        questionDao.replaceAll(entities)
    }

    suspend fun replaceBank(questions: List<Question>) {
        questionDao.replaceAll(questions.map(QuestionMapper::domainToEntity))
    }

    suspend fun upsertAll(questions: List<Question>) {
        questionDao.upsertAll(questions.map(QuestionMapper::domainToEntity))
    }

    // ----- SINCRONIZACIÓN CON FIRESTORE -----

    /**
     * Comprueba el manifiesto y sincroniza solo si cambió (botón «Buscar
     * actualizaciones»). Devuelve el resultado detallado ([ResultadoSync])
     * para explicar qué pasó (sin manifiesto, sin cambios, actualizado o
     * rechazado).
     */
    suspend fun sincronizarBanco(): ResultadoSync =
        bankSync?.sincronizar(forzar = false) ?: ResultadoSync.SinConfiguracion

    /**
     * Fuerza una sincronización con `questions_v2` y devuelve el total
     * actualizado. Los fallos no borran el banco local.
     */
    suspend fun updateBankFromFirestore(): Result<Int> {
        val sync = bankSync
            ?: return Result.failure(Exception("La sincronización no está configurada"))
        return when (val resultado = sync.sincronizar(forzar = true)) {
            is ResultadoSync.Actualizado -> Result.success(resultado.total)
            is ResultadoSync.SinCambios -> Result.success(resultado.total)
            is ResultadoSync.SinManifiesto ->
                Result.failure(Exception("El panel aún no publicó un banco activo"))
            is ResultadoSync.SinConfiguracion ->
                Result.failure(Exception("Firestore no está configurado"))
            is ResultadoSync.Rechazado ->
                Result.failure(Exception("Lote rechazado: ${resultado.motivos.joinToString()}"))
            is ResultadoSync.Error ->
                Result.failure(Exception(resultado.mensaje))
        }
    }
}
