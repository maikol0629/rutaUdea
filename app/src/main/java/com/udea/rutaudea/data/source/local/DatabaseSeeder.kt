package com.udea.rutaudea.data.source.local

import android.content.Context
import com.google.gson.Gson
import com.udea.rutaudea.data.local.dao.QuestionDao
import com.udea.rutaudea.data.repository.QuestionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Siembra la base local con el banco de preguntas empaquetado en assets.
 *
 * Además de la primera ejecución (tabla vacía), re-siembra cuando la
 * versión del banco empaquetado sube ([BANK_SEED_VERSION]): así los
 * dispositivos con la app ya instalada reciben las correcciones del
 * banco sin reinstalar ni borrar datos.
 *
 * Desde la Fase 7 el asset es el mismo JSONL del ETL v2
 * (`questions_v2.jsonl`), por lo que Room guarda estado, imágenes,
 * contexto y versión del banco sin perder campos.
 */
class DatabaseSeeder(
    private val context: Context,
    private val questionDao: QuestionDao
) {

    private val repository = QuestionRepository(questionDao)
    private val loader = QuestionJsonlLoader(context, Gson())

    /** Ruta del JSONL empaquetado en assets. */
    private val assetPath = "questions/questions.jsonl"

    suspend fun seedIfNeeded() {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val versionInstalada = prefs.getInt(KEY_VERSION, 0)

        if (!debeSembrar(questionDao.countQuestions(), versionInstalada)) return

        val entities = loader.loadFromAsset(assetPath)
        repository.replaceBankEntities(entities)

        prefs.edit().putInt(KEY_VERSION, BANK_SEED_VERSION).apply()
    }

    fun seedIfNeededAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            seedIfNeeded()
        }
    }

    companion object {
        private const val PREFS = "bank_seed"
        private const val KEY_VERSION = "version"

        /**
         * Versión del banco empaquetado. **Incrémentala** cada vez que
         * cambies `app/src/main/assets/questions/questions.jsonl` para
         * que los dispositivos existentes se re-siembren.
         *
         * v1: banco original (subtemas genéricos mezclados).
         * v2: normalización a componentes oficiales RL01–RL18 / CL01–CL14.
         * v3: limpieza del banco — frase plantilla deduplicada en textos
         *     CL-ORG, scraping de profe_alex_52 truncado, 12 preguntas RL
         *     con figuras inexistentes deshabilitadas.
         * v4: frase plantilla eliminada por completo de los textos CL-ORG
         *     (era relleno sin aporte de contenido).
         * v5: banco fusionado Fase 1-4 (530 filas: 183 aprobado / 335
         *     pendiente / 12 deshabilitado). 26 filas base sin respuesta
         *     pasan a pendiente y entran 21 preguntas nuevas verificadas.
         * v6: **Fase 7** — asset v2 del ETL (`udea-2026-10-v1`, schema 2):
         *     400 preguntas (269 aprobado / 127 aprobado_con_imagen /
         *     3 pendiente / 1 deshabilitado), 55 contextos, 93 imágenes
         *     publicadas. Se conservan estado, imágenes con rol, contexto,
         *     metadata de revisión y `bank_version`.
         */
        const val BANK_SEED_VERSION = 6

        /**
         * Política de siembra (pura y testeable):
         * - siembra si no hay preguntas aprobadas (primera ejecución), o
         * - re-siembra si el banco instalado es de una versión anterior.
         */
        fun debeSembrar(preguntasEnBd: Int, versionInstalada: Int): Boolean =
            preguntasEnBd == 0 || versionInstalada < BANK_SEED_VERSION
    }
}
