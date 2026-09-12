package com.udea.rutaudea.data.repository

import com.udea.rutaudea.data.local.dao.QuestionDao
import com.udea.rutaudea.data.mapper.QuestionMapper
import com.udea.rutaudea.domain.model.Question
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Punto único de acceso a las preguntas.
 *
 * La fuente principal de consulta es la base local (Room) para
 * garantizar rapidez y funcionamiento offline. La actualización
 * del banco se realiza a través de [updateBankFromFirestore].
 */
class QuestionRepository(
    private val questionDao: QuestionDao
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

    suspend fun getDistinctDificultades(): List<String> = questionDao.getDistinctDificultades()

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
    // Nota: Firebase se configurará en una fase posterior. Este método
    // define el contrato: descargar el banco maestro y actualizar Room.
    suspend fun updateBankFromFirestore() {
        // TODO: fase Firebase
        // 1. Consultar Firestore (colección questions) con estado = "aprobado"
        // 2. Mapear a List<Question>
        // 3. replaceBank(preguntasDescargadas)
    }
}