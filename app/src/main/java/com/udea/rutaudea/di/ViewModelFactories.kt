package com.udea.rutaudea.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.udea.rutaudea.data.repository.QuestionRepository
import com.udea.rutaudea.data.source.mvp.MvpQuestionProvider
import com.udea.rutaudea.ui.screen.home.HomeViewModel
import com.udea.rutaudea.ui.screen.resultado.ResultadoViewModel
import com.udea.rutaudea.ui.screen.simulacro.SimulacroViewModel
import com.udea.rutaudea.ui.screen.splash.SplashViewModel

object ViewModelFactories {
    fun createSplashViewModel(questionRepository: QuestionRepository): SplashViewModel {
        return SplashViewModel(questionRepository)
    }

    fun createHomeViewModel(): HomeViewModel {
        return HomeViewModel()
    }

    fun createSimulacroViewModel(
        mvpProvider: MvpQuestionProvider,
        savedStateHandle: SavedStateHandle
    ): SimulacroViewModel {
        return SimulacroViewModel(
            mvpProvider = mvpProvider,
            savedStateHandle = savedStateHandle
        )
    }

    fun createResultadoViewModel(
        mvpProvider: MvpQuestionProvider,
        userAnswers: Map<Int, String>,
        score: Int,
        total: Int,
        timeUsedMs: Long
    ): ResultadoViewModel {
        return ResultadoViewModel(mvpProvider, userAnswers, score, total, timeUsedMs)
    }

    class SplashFactory(private val repository: QuestionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SplashViewModel(repository) as T
    }

    class HomeFactory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel() as T
    }

    class SimulacroFactory(
        private val mvpProvider: MvpQuestionProvider,
        private val savedStateHandle: SavedStateHandle
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SimulacroViewModel(mvpProvider, savedStateHandle) as T
    }

    class ResultadoFactory(
        private val mvpProvider: MvpQuestionProvider,
        private val userAnswers: Map<Int, String>,
        private val score: Int,
        private val total: Int,
        private val timeUsedMs: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ResultadoViewModel(mvpProvider, userAnswers, score, total, timeUsedMs) as T
    }
}