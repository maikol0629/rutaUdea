package com.udea.rutaudea.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.udea.rutaudea.data.local.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para el acceso a las preguntas del banco local.
 *
 * Todas las consultas se ejecutan contra Room (SQLite), lo que
 * garantiza respuestas rápidas y funcionamiento offline.
 */
@Dao
interface QuestionDao {

    // ----- INSERT / UPDATE -----

    @Upsert
    suspend fun upsertAll(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions")
    suspend fun deleteAll()

    // ----- CONSULTAS DE PRÁCTICA (filtros combinados) -----

    /** Preguntas filtradas por área, subtema y/o dificultad (práctica dirigida). */
    @Query(
        """
        SELECT * FROM questions
        WHERE (:area IS NULL OR area = :area)
          AND (:subtema IS NULL OR subtema = :subtema)
          AND (:dificultad IS NULL OR dificultad = :dificultad)
          AND estado = 'aprobado'
        ORDER BY RANDOM()
        """
    )
    suspend fun getQuestions(
        area: String? = null,
        subtema: String? = null,
        dificultad: String? = null,
        limit: Int
    ): List<QuestionEntity>

    /** Todas las preguntas aprobadas de un área (usado por el motor de selección). */
    @Query("SELECT * FROM questions WHERE area = :area AND estado = 'aprobado' ORDER BY RANDOM()")
    suspend fun getQuestionsByArea(area: String, limit: Int): List<QuestionEntity>

    /** Preguntas por subtema específico. */
    @Query("SELECT * FROM questions WHERE subtema = :subtema AND estado = 'aprobado'")
    suspend fun getQuestionsBySubtema(subtema: String): List<QuestionEntity>

    /** Preguntas con contexto compartido (lecturas con texto base). */
    @Query("SELECT * FROM questions WHERE contextoId = :contextoId AND estado = 'aprobado'")
    suspend fun getQuestionsByContext(contextoId: String): List<QuestionEntity>

    // ----- CONSULTAS ÚTILES -----

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getById(id: String): QuestionEntity?

    @Query("SELECT COUNT(*) FROM questions WHERE estado = 'aprobado'")
    suspend fun countQuestions(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE area = :area AND estado = 'aprobado'")
    suspend fun countByArea(area: String): Int

    @Query("SELECT COUNT(*) FROM questions WHERE area = :area AND dificultad = :dificultad AND estado = 'aprobado'")
    suspend fun countByAreaAndDifficulty(area: String, dificultad: String): Int

    @Query("SELECT DISTINCT area FROM questions ORDER BY area")
    suspend fun getDistinctAreas(): List<String>

    @Query("SELECT DISTINCT subtema FROM questions ORDER BY subtema")
    suspend fun getDistinctSubtemas(): List<String>

    @Query("SELECT DISTINCT dificultad FROM questions ORDER BY dificultad")
    suspend fun getDistinctDificultades(): List<String>

    /** Flujo reactivo con todas las preguntas aprobadas. */
    @Query("SELECT * FROM questions WHERE estado = 'aprobado'")
    fun observeQuestions(): Flow<List<QuestionEntity>>
}