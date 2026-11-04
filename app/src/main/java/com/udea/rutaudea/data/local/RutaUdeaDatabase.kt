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
import com.udea.rutaudea.data.local.dao.ReportePreguntaDao
import com.udea.rutaudea.data.local.entity.EncuestaEntity
import com.udea.rutaudea.data.local.entity.PracticeQuestionEntity
import com.udea.rutaudea.data.local.entity.PracticeSummaryEntity
import com.udea.rutaudea.data.local.entity.PruebaQuestionEntity
import com.udea.rutaudea.data.local.entity.PruebaSummaryEntity
import com.udea.rutaudea.data.local.entity.QuestionEntity
import com.udea.rutaudea.data.local.entity.RachaEntity
import com.udea.rutaudea.data.local.entity.ReportePreguntaEntity
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
        PruebaSummaryEntity::class,
        PracticeSummaryEntity::class,
        SimulationQuestionEntity::class,
        PruebaQuestionEntity::class,
        PracticeQuestionEntity::class,
        RachaEntity::class,
        EncuestaEntity::class,
        ReportePreguntaEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class RutaUdeaDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao

    abstract fun progressDao(): ProgressDao

    abstract fun rachaDao(): RachaDao

    abstract fun encuestaDao(): EncuestaDao

    abstract fun reportePreguntaDao(): ReportePreguntaDao

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

        /**
         * v4 → v5 (Fase 2): agrega `imagen_url` a la tabla `questions`.
         * Las filas existentes quedan con NULL (= pregunta solo-texto).
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `imagen_url` TEXT")
            }
        }

        /**
         * v5 → v6 (Fase 7): campos del contrato v2 que antes se perdían.
         *
         * - `imagenes`/`opciones_en_imagen`/`requiere_imagen`: D-05 y D-06.
         * - `banco_version`: D-02 y D-07.
         * - metadatos de revisión: panel y trazabilidad.
         *
         * Los valores por defecto mantienen las filas existentes como
         * solo-texto y sin versión; el seeder las reemplaza al subir
         * `BANK_SEED_VERSION`.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `imagenes` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `opciones_en_imagen` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `requiere_imagen` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `banco_version` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `tarea` TEXT")
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `numero` INTEGER")
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `confianza_modelo` TEXT")
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `revisado_por` TEXT")
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `fecha_revision` TEXT")
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `notas` TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_estado` ON `questions` (`estado`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_banco_version` ON `questions` (`banco_version`)")
            }
        }

        /**
         * v6 → v7: agrega la tabla de cache de los reportes de preguntas.
         * No borra ningún dato existente.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reportes_preguntas_cache` (
                        `id` TEXT NOT NULL,
                        `uid` TEXT NOT NULL,
                        `question_id` TEXT NOT NULL,
                        `motivo` TEXT NOT NULL,
                        `comentario` TEXT NOT NULL,
                        `modo` TEXT NOT NULL,
                        `sesion_id` TEXT NOT NULL,
                        `area` TEXT NOT NULL,
                        `subtema` TEXT NOT NULL,
                        `banco_version` TEXT,
                        `respuesta_usuario` TEXT,
                        `respuesta_correcta` TEXT,
                        `fecha_ms` INTEGER NOT NULL,
                        `app_version_code` INTEGER NOT NULL,
                        `estado` TEXT NOT NULL,
                        `synced` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_reportes_preguntas_cache_uid_synced` ON `reportes_preguntas_cache` (`uid`, `synced`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_reportes_preguntas_cache_question_id` ON `reportes_preguntas_cache` (`question_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_reportes_preguntas_cache_fecha_ms` ON `reportes_preguntas_cache` (`fecha_ms`)"
                )
            }
        }

        /**
         * v7 → v8: agrega `tipo` al resumen de simulacros para distinguir
         * el examen completo de la prueba corta "Pruébate". Las filas
         * existentes quedan como `completo`.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `simulations_cache` ADD COLUMN `tipo` TEXT NOT NULL DEFAULT 'completo'"
                )
            }
        }

        /**
         * v8 → v9: separa las pruebas cortas "Pruébate" en sus propias
         * tablas (`pruebas_cache` y `prueba_questions_cache`).
         *
         * Las pruebas guardadas por el build intermedio (filas con
         * `tipo = 'prueba'`) se descartan: no se migran a las nuevas
         * tablas. Los simulacros completos se conservan y
         * `simulations_cache` se recrea sin la columna `tipo`.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pruebas_cache` (
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
                    "CREATE INDEX IF NOT EXISTS `index_pruebas_cache_uid_fecha_ms` ON `pruebas_cache` (`uid`, `fecha_ms`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_pruebas_cache_synced` ON `pruebas_cache` (`synced`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `prueba_questions_cache` (
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
                    "CREATE INDEX IF NOT EXISTS `index_prueba_questions_cache_session_id` ON `prueba_questions_cache` (`session_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_prueba_questions_cache_subtema` ON `prueba_questions_cache` (`subtema`)"
                )

                // Descarta el detalle de las pruebas del build intermedio.
                db.execSQL(
                    "DELETE FROM `simulation_questions_cache` WHERE `session_id` IN (SELECT `id` FROM `simulations_cache` WHERE `tipo` = 'prueba')"
                )

                // Recrea simulations_cache sin `tipo`, conservando solo los
                // simulacros completos.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `simulations_cache_new` (
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
                    """
                    INSERT OR REPLACE INTO `simulations_cache_new`
                        (`id`, `uid`, `fecha_ms`, `score`, `total`, `tiempo_used_ms`,
                         `respondidas`, `omitidas`, `synced`)
                    SELECT `id`, `uid`, `fecha_ms`, `score`, `total`, `tiempo_used_ms`,
                           `respondidas`, `omitidas`, `synced`
                    FROM `simulations_cache`
                    WHERE `tipo` != 'prueba'
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `simulations_cache`")
                db.execSQL("ALTER TABLE `simulations_cache_new` RENAME TO `simulations_cache`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_simulations_cache_uid_fecha_ms` ON `simulations_cache` (`uid`, `fecha_ms`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_simulations_cache_synced` ON `simulations_cache` (`synced`)"
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
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
