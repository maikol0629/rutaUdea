package com.udea.rutaudea.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    /**
     * Reemplazo atómico del banco local: si la inserción falla, Room
     * revierte el borrado y se conserva la última versión buena (D-02 §4.2).
     */
    @Transaction
    suspend fun replaceAll(questions: List<QuestionEntity>) {
        deleteAll()
        insertAll(questions)
    }

    /** Total de filas del banco (aprobadas + pendientes + deshabilitadas). */
    @Query("SELECT COUNT(*) FROM questions")
    suspend fun countAll(): Int

    // ----- CONSULTAS DE PRÁCTICA (filtros combinados) -----

    /** Preguntas filtradas por área, subtema y/o dificultad (práctica dirigida). */
    @Query(
        """
        SELECT * FROM questions
        WHERE (:area IS NULL OR area = :area)
          AND (:subtema IS NULL OR subtema = :subtema)
          AND (:dificultad IS NULL OR dificultad = :dificultad)
          AND estado IN ('aprobado', 'aprobado_con_imagen')
        ORDER BY RANDOM()
        LIMIT :limit
        """
    )
    suspend fun getQuestions(
        area: String? = null,
        subtema: String? = null,
        dificultad: String? = null,
        limit: Int
    ): List<QuestionEntity>

    /** Todas las preguntas aprobadas de un área (usado por el motor de selección). */
    @Query("SELECT * FROM questions WHERE area = :area AND estado IN ('aprobado', 'aprobado_con_imagen') ORDER BY RANDOM() LIMIT :limit")
    suspend fun getQuestionsByArea(area: String, limit: Int): List<QuestionEntity>

    /** Preguntas por subtema específico. */
    @Query("SELECT * FROM questions WHERE subtema = :subtema AND estado IN ('aprobado', 'aprobado_con_imagen')")
    suspend fun getQuestionsBySubtema(subtema: String): List<QuestionEntity>

    /** Preguntas con contexto compartido (lecturas con texto base). */
    @Query("SELECT * FROM questions WHERE contexto_id = :contextoId AND estado IN ('aprobado', 'aprobado_con_imagen')")
    suspend fun getQuestionsByContext(contextoId: String): List<QuestionEntity>

    // ----- CONSULTAS ÚTILES -----

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getById(id: String): QuestionEntity?

    @Query("SELECT COUNT(*) FROM questions WHERE estado IN ('aprobado', 'aprobado_con_imagen')")
    suspend fun countQuestions(): Int

    @Query("SELECT COUNT(*) FROM questions WHERE area = :area AND estado IN ('aprobado', 'aprobado_con_imagen')")
    suspend fun countByArea(area: String): Int

    @Query("SELECT COUNT(*) FROM questions WHERE area = :area AND dificultad = :dificultad AND estado IN ('aprobado', 'aprobado_con_imagen')")
    suspend fun countByAreaAndDifficulty(area: String, dificultad: String): Int

    @Query("SELECT DISTINCT area FROM questions ORDER BY area")
    suspend fun getDistinctAreas(): List<String>

    @Query("SELECT DISTINCT subtema FROM questions ORDER BY subtema")
    suspend fun getDistinctSubtemas(): List<String>

    /** Subtemas con preguntas aprobadas de un área (filtros de práctica). */
    @Query("SELECT DISTINCT subtema FROM questions WHERE area = :area AND estado IN ('aprobado', 'aprobado_con_imagen') ORDER BY subtema")
    suspend fun getDistinctSubtemasByArea(area: String): List<String>

    @Query("SELECT DISTINCT dificultad FROM questions ORDER BY dificultad")
    suspend fun getDistinctDificultades(): List<String>

    /** Preguntas aprobadas disponibles con los filtros de práctica (área y/o subtema). */
    @Query(
        """
        SELECT COUNT(*) FROM questions
        WHERE (:area IS NULL OR area = :area)
          AND (:subtema IS NULL OR subtema = :subtema)
          AND estado IN ('aprobado', 'aprobado_con_imagen')
        """
    )
    suspend fun countByFilters(area: String? = null, subtema: String? = null): Int

    /** Flujo reactivo con todas las preguntas aprobadas. */
    @Query("SELECT * FROM questions WHERE estado IN ('aprobado', 'aprobado_con_imagen')")
    fun observeQuestions(): Flow<List<QuestionEntity>>
}