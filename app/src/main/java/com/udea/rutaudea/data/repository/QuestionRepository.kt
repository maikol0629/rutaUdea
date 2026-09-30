package com.udea.rutaudea.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.udea.rutaudea.data.local.dao.QuestionDao
import com.udea.rutaudea.data.mapper.QuestionMapper
import com.udea.rutaudea.domain.model.Question
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Punto único de acceso a las preguntas.
 *
 * La fuente principal de consulta es la base local (Room) para
 * garantizar rapidez y funcionamiento offline. La actualización
 * del banco se realiza a través de [updateBankFromFirestore].
 */
class QuestionRepository(
    private val questionDao: QuestionDao,
    private val firestore: FirebaseFirestore? = null
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

    // ----- SEED (carga inicial) -----

    /** Reemplaza el banco local por completo (usado al cargar el JSONL o al sincronizar). */
    suspend fun replaceBank(questions: List<Question>) {
        questionDao.deleteAll()
        questionDao.insertAll(questions.map(QuestionMapper::domainToEntity))
    }

    suspend fun upsertAll(questions: List<Question>) {
        questionDao.upsertAll(questions.map(QuestionMapper::domainToEntity))
    }

    // ----- SINCRONIZACIÓN CON FIRESTORE -----

    /**
     * Descarga el banco maestro desde Firestore (colección `questions`,
     * documentos con estado = "aprobado") y reemplaza el banco local Room.
     */
    suspend fun updateBankFromFirestore(): Result<Int> {
        val fs = firestore
            ?: return Result.failure(Exception("Firestore no está configurado"))
        return try {
            val snapshot = fs.collection(COLECCION_QUESTIONS)
                .whereEqualTo(CAMPO_ESTADO, ESTADO_APROBADO)
                .get()
                .await()
            val questions = snapshot.documents.mapNotNull { doc -> documentToQuestion(doc) }
            if (questions.isEmpty()) {
                Result.failure(Exception("El banco de Firestore está vacío o sin preguntas aprobadas"))
            } else {
                replaceBank(questions)
                Result.success(questions.size)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun documentToQuestion(doc: com.google.firebase.firestore.DocumentSnapshot): Question? {
        val id = doc.id
        val area = doc.getString("area") ?: return null
        val subtema = doc.getString("subtema") ?: return null
        val dificultad = doc.getString("dificultad") ?: return null
        val pregunta = doc.getString("pregunta") ?: return null
        val opciones = (doc.get("opciones") as? List<*>)?.map { it.toString() }
        val respuestaCorrecta = doc.getString("respuesta_correcta") ?: return null
        if (opciones.isNullOrEmpty() || respuestaCorrecta !in listOf("A", "B", "C", "D")) {
            return null
        }
        return Question(
            id = id,
            area = area,
            componente = doc.getString("componente"),
            subtema = subtema,
            competencia = doc.getString("competencia"),
            tipoTexto = doc.getString("tipo_texto"),
            dificultad = dificultad,
            textoBase = doc.getString("texto_base"),
            contexto = doc.getString("contexto"),
            pregunta = pregunta,
            opciones = opciones,
            respuestaCorrecta = respuestaCorrecta,
            explicacion = doc.getString("explicacion"),
            esOriginal = doc.getBoolean("es_original") ?: false
        )
    }

    companion object {
        const val COLECCION_QUESTIONS = "questions"
        const val CAMPO_ESTADO = "estado"
        const val ESTADO_APROBADO = "aprobado"
    }
}