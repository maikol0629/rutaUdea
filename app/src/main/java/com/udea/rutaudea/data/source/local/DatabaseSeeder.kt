package com.udea.rutaudea.data.source.local

import com.google.gson.Gson
import com.udea.rutaudea.data.local.dao.QuestionDao
import com.udea.rutaudea.data.repository.QuestionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Siembra la base local con el banco de preguntas la primera vez
 * que se ejecuta la app.
 */
class DatabaseSeeder(
    private val context: android.content.Context,
    private val questionDao: QuestionDao
) {

    private val repository = QuestionRepository(questionDao)
    private val loader = QuestionJsonlLoader(context, Gson())

    /** Ruta del JSONL empaquetado en assets. */
    private val assetPath = "questions/questions.jsonl"

    suspend fun seedIfNeeded() {
        // Solo siembra si la tabla está vacía (primera ejecución).
        if (questionDao.countQuestions() == 0) {
            val questions = loader.loadFromAsset(assetPath)
            repository.replaceBank(questions)
        }
    }

    fun seedIfNeededAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            seedIfNeeded()
        }
    }
}