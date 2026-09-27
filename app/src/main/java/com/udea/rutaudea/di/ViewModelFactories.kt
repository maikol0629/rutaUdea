package com.udea.rutaudea.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.udea.rutaudea.data.repository.AuthRepository
import com.udea.rutaudea.data.repository.QuestionRepository
import com.udea.rutaudea.data.repository.SimulationRepository
import com.udea.rutaudea.domain.model.Question
import com.udea.rutaudea.domain.services.QuestionSelector
import com.udea.rutaudea.ui.screen.perfil.PerfilViewModel
import com.udea.rutaudea.ui.screen.resultado.ResultadoViewModel
import com.udea.rutaudea.ui.screen.simulacro.SimulacroViewModel
import com.udea.rutaudea.ui.screen.splash.SplashViewModel

object ViewModelFactories {

    class SplashFactory(private val repository: QuestionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SplashViewModel(repository) as T
    }

    class SimulacroFactory(
        private val selector: QuestionSelector,
        private val repository: QuestionRepository,
        private val simulationRepository: SimulationRepository,
        private val savedStateHandle: SavedStateHandle
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SimulacroViewModel(selector, repository, simulationRepository, savedStateHandle) as T
    }

    class ResultadoFactory(
        private val questions: List<Question>,
        private val simulationRepository: SimulationRepository,
        private val userAnswers: Map<Int, String>,
        private val score: Int,
        private val total: Int,
        private val timeUsedMs: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ResultadoViewModel(questions, simulationRepository, userAnswers, score, total, timeUsedMs) as T
    }

    class PerfilFactory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PerfilViewModel(authRepository) as T
    }
}
