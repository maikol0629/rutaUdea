package com.udea.rutaudea.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.udea.rutaudea.data.local.dao.ProgressDao
import com.udea.rutaudea.data.local.dao.QuestionDao
import com.udea.rutaudea.data.local.entity.PracticeQuestionEntity
import com.udea.rutaudea.data.local.entity.PracticeSummaryEntity
import com.udea.rutaudea.data.local.entity.QuestionEntity
import com.udea.rutaudea.data.local.entity.SimulationQuestionEntity
import com.udea.rutaudea.data.local.entity.SimulationSummaryEntity

/**
 * Base de datos local de RutaUdeA.
 *
 * - `questions`: banco de preguntas offline (Room/SQLite).
 * - `*_cache`: historial de simulacros y prácticas para el módulo de
 *   Progreso. Es la fuente primaria de lectura (funciona sin conexión);
 *   Firestore se usa para respaldar y sincronizar.
 */
@Database(
    entities = [
        QuestionEntity::class,
        SimulationSummaryEntity::class,
        PracticeSummaryEntity::class,
        SimulationQuestionEntity::class,
        PracticeQuestionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class RutaUdeaDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao

    abstract fun progressDao(): ProgressDao

    companion object {
        @Volatile
        private var INSTANCE: RutaUdeaDatabase? = null

        /**
         * v1 → v2: agrega las tablas de cache del módulo de Progreso.
         * No borra el banco de preguntas ni datos existentes.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `simulations_cache` (
                        `id` TEXT NOT NULL,
                        `uid` TEXT NOT NULL,
                        `fecha_ms` INTEGER NOT NULL,
                        `score` INTEGER NOT NULL,
                        `total` INTEGER NOT NULL,
                        `tiempo_used_ms` INTEGER NOT NULL,
                        `respondidas` INTEGER NOT NULL,
                        `omitidas` INTEGER NOT NULL,
                        `synced` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_simulations_cache_uid_fecha_ms` ON `simulations_cache` (`uid`, `fecha_ms`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_simulations_cache_synced` ON `simulations_cache` (`synced`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `practice_sessions_cache` (
                        `id` TEXT NOT NULL,
                        `uid` TEXT NOT NULL,
                        `fecha_ms` INTEGER NOT NULL,
                        `area` TEXT NOT NULL,
                        `subtema` TEXT,
                        `score` INTEGER NOT NULL,
                        `total` INTEGER NOT NULL,
                        `tiempo_used_ms` INTEGER NOT NULL,
                        `respondidas` INTEGER NOT NULL,
                        `omitidas` INTEGER NOT NULL,
                        `synced` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_practice_sessions_cache_uid_fecha_ms` ON `practice_sessions_cache` (`uid`, `fecha_ms`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_practice_sessions_cache_synced` ON `practice_sessions_cache` (`synced`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `simulation_questions_cache` (
                        `session_id` TEXT NOT NULL,
                        `question_id` TEXT NOT NULL,
                        `indice` INTEGER NOT NULL,
                        `area` TEXT NOT NULL,
                        `subtema` TEXT NOT NULL,
                        `dificultad` TEXT NOT NULL,
                        `respuesta_usuario` TEXT NOT NULL,
                        `respuesta_correcta` TEXT NOT NULL,
                        `es_correcta` INTEGER NOT NULL,
                        PRIMARY KEY(`session_id`, `question_id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_simulation_questions_cache_session_id` ON `simulation_questions_cache` (`session_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_simulation_questions_cache_subtema` ON `simulation_questions_cache` (`subtema`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `practice_questions_cache` (
                        `session_id` TEXT NOT NULL,
                        `question_id` TEXT NOT NULL,
                        `indice` INTEGER NOT NULL,
                        `subtema` TEXT NOT NULL,
                        `dificultad` TEXT NOT NULL,
                        `respuesta_usuario` TEXT NOT NULL,
                        `respuesta_correcta` TEXT NOT NULL,
                        `es_correcta` INTEGER NOT NULL,
                        PRIMARY KEY(`session_id`, `question_id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_practice_questions_cache_session_id` ON `practice_questions_cache` (`session_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_practice_questions_cache_subtema` ON `practice_questions_cache` (`subtema`)"
                )
            }
        }

        fun getInstance(context: Context): RutaUdeaDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RutaUdeaDatabase::class.java,
                    "rutaudea.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
