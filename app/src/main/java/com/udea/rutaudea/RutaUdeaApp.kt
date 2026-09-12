package com.udea.rutaudea

import android.app.Application
import com.udea.rutaudea.data.local.RutaUdeaDatabase
import com.udea.rutaudea.data.repository.QuestionRepository
import com.udea.rutaudea.data.source.local.DatabaseSeeder

/**
 * Punto de entrada de la aplicación. Expone los singletons de
 * la base de datos y el repository, y siembra el banco inicial.
 */
class RutaUdeaApp : Application() {

    private val database by lazy { RutaUdeaDatabase.getInstance(this) }

    val questionRepository: QuestionRepository by lazy {
        QuestionRepository(database.questionDao())
    }

    override fun onCreate() {
        super.onCreate()
        // Siembra el banco de preguntas local en la primera ejecución.
        DatabaseSeeder(this, database.questionDao()).seedIfNeededAsync()
    }
}