package com.udea.rutaudea.di

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.udea.rutaudea.data.repository.AuthRepository
import com.udea.rutaudea.data.repository.PracticeRepository
import com.udea.rutaudea.data.repository.QuestionRepository
import com.udea.rutaudea.data.repository.SimulationRepository
import com.udea.rutaudea.domain.services.PracticeSelector
import com.udea.rutaudea.domain.services.QuestionSelector
import com.udea.rutaudea.RutaUdeaApp

object AppModule {
    @Suppress("UNUSED_PARAMETER")
    fun provideQuestionRepository(app: Application): QuestionRepository {
        return (app as RutaUdeaApp).questionRepository
    }

    fun provideQuestionSelector(): QuestionSelector {
        return QuestionSelector()
    }

    fun providePracticeSelector(): PracticeSelector {
        return PracticeSelector()
    }

    fun provideAuthRepository(app: Application): AuthRepository {
        return AuthRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance()
        )
    }

    fun providePracticeRepository(app: Application): PracticeRepository {
        return PracticeRepository(
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