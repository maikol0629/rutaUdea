package com.udea.rutaudea.di

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.udea.rutaudea.data.repository.AuthRepository
import com.udea.rutaudea.data.repository.QuestionRepository
import com.udea.rutaudea.data.repository.SimulationRepository
import com.udea.rutaudea.data.source.mvp.MvpQuestionProvider
import com.udea.rutaudea.RutaUdeaApp

object AppModule {
    @Suppress("UNUSED_PARAMETER")
    fun provideQuestionRepository(app: Application): QuestionRepository {
        return (app as RutaUdeaApp).questionRepository
    }

    fun provideMvpQuestionProvider(repository: QuestionRepository): MvpQuestionProvider {
        return MvpQuestionProvider(repository)
    }

    fun provideAuthRepository(app: Application): AuthRepository {
        return AuthRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance()
        )
    }

    fun provideSimulationRepository(app: Application): SimulationRepository {
        return SimulationRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance()
        )
    }
}