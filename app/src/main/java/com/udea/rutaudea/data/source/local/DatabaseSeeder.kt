package com.udea.rutaudea.data.source.local

import android.content.Context
import com.google.gson.Gson
import com.udea.rutaudea.data.local.dao.QuestionDao
import com.udea.rutaudea.data.mapper.QuestionMapper
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
 * banco (p. ej. la normalización de subtemas) sin reinstalar ni
 * borrar datos.
 */
class DatabaseSeeder(
    private val context: Context,
    private val questionDao: QuestionDao
) {

    private val repository = QuestionRepository(questionDao)
    private val loader = QuestionJsonlLoader(context, Gson())
    private val mapper = QuestionMapper

    /** Ruta del JSONL empaquetado en assets. */
    private val assetPath = "questions/questions.jsonl"

    suspend fun seedIfNeeded() {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val versionInstalada = prefs.getInt(KEY_VERSION, 0)

        if (!debeSembrar(questionDao.countQuestions(), versionInstalada)) return

        val entities = loader.loadFromAsset(assetPath)
        val questions = mapper.listToDomain(entities)
        repository.replaceBank(questions)

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
         * que los dispositivos existentes se re-siemhren.
         *
         * v1: banco original (subtemas genéricos mezclados).
         * v2: normalización a componentes oficiales RL01–RL18 / CL01–CL14.
         */
        const val BANK_SEED_VERSION = 2

        /**
         * Política de siembra (pura y testeable):
         * - siembra si la tabla está vacía (primera ejecución), o
         * - re-siembra si el banco instalado es de una versión anterior.
         */
        fun debeSembrar(preguntasEnBd: Int, versionInstalada: Int): Boolean =
            preguntasEnBd == 0 || versionInstalada < BANK_SEED_VERSION
    }
}
