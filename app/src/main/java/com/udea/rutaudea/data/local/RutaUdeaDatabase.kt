package com.udea.rutaudea.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.udea.rutaudea.data.local.dao.QuestionDao
import com.udea.rutaudea.data.local.entity.QuestionEntity

/**
 * Base de datos local de RutaUdeA.
 *
 * Contiene el banco de preguntas en SQLite (Room) para consultas
 * rápidas y offline. Los datos dinámicos del usuario (simulacros,
 * resultados) se gestionan en Firestore.
 */
@Database(
    entities = [QuestionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class RutaUdeaDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao

    companion object {
        @Volatile
        private var INSTANCE: RutaUdeaDatabase? = null

        fun getInstance(context: Context): RutaUdeaDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RutaUdeaDatabase::class.java,
                    "rutaudea.db"
                )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}