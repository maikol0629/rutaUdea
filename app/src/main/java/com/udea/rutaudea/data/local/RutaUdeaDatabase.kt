package com.udea.rutaudea.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.udea.rutaudea.data.local.dao.EncuestaDao
import com.udea.rutaudea.data.local.dao.ProgressDao
import com.udea.rutaudea.data.local.dao.QuestionDao
import com.udea.rutaudea.data.local.dao.RachaDao
import com.udea.rutaudea.data.local.entity.EncuestaEntity
import com.udea.rutaudea.data.local.entity.PracticeQuestionEntity
import com.udea.rutaudea.data.local.entity.PracticeSummaryEntity
import com.udea.rutaudea.data.local.entity.QuestionEntity
import com.udea.rutaudea.data.local.entity.RachaEntity
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
        PracticeQuestionEntity::class,
        RachaEntity::class,
        EncuestaEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class RutaUdeaDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao

    abstract fun progressDao(): ProgressDao

    abstract fun rachaDao(): RachaDao

    abstract fun encuestaDao(): EncuestaDao

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

        /**
         * v2 → v3: agrega la tabla de cache de la racha de actividad.
         * No borra el banco de preguntas ni los datos existentes.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `rachas_cache` (
                        `uid` TEXT NOT NULL,
                        `actual` INTEGER NOT NULL,
                        `mejor` INTEGER NOT NULL,
                        `ultimo_dia` INTEGER,
                        `ultima_actividad_ms` INTEGER NOT NULL,
                        `synced` INTEGER NOT NULL,
                        PRIMARY KEY(`uid`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * v3 → v4: agrega la tabla de cache del cuestionario de satisfacción
         * de la prueba piloto. No borra ningún dato existente.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `encuestas_cache` (
                        `uid` TEXT NOT NULL,
                        `fecha_ms` INTEGER NOT NULL,
                        `tipo_institucion` TEXT NOT NULL,
                        `nombre_institucion` TEXT NOT NULL,
                        `grado` TEXT NOT NULL,
                        `semestre` TEXT NOT NULL,
                        `presento_examen` INTEGER,
                        `likert_simulacro` INTEGER NOT NULL,
                        `likert_practica` INTEGER NOT NULL,
                        `likert_progreso` INTEGER NOT NULL,
                        `likert_info` INTEGER NOT NULL,
                        `claridad_rl` INTEGER NOT NULL,
                        `claridad_cl` INTEGER NOT NULL,
                        `dificultad_adecuada` INTEGER NOT NULL,
                        `facilidad_uso` INTEGER NOT NULL,
                        `nps` INTEGER NOT NULL,
                        `gusto_mas` TEXT NOT NULL,
                        `mejorar` TEXT NOT NULL,
                        `synced` INTEGER NOT NULL,
                        PRIMARY KEY(`uid`)
                    )
                    """.trimIndent()
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
