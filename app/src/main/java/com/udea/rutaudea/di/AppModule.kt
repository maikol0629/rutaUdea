package com.udea.rutaudea.di

import android.app.Application
import com.udea.rutaudea.data.repository.QuestionRepository
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
}