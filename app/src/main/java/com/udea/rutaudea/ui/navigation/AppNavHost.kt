package com.udea.rutaudea.ui.navigation

import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.udea.rutaudea.di.AppModule
import com.udea.rutaudea.di.ViewModelFactories
import com.udea.rutaudea.ui.screen.common.ComingSoonScreen
import com.udea.rutaudea.ui.screen.home.HomeScreen
import com.udea.rutaudea.ui.screen.resultado.ResultadoScreen
import com.udea.rutaudea.ui.screen.resultado.ResultadoViewModel
import com.udea.rutaudea.ui.screen.simulacro.SimulacroScreen
import com.udea.rutaudea.ui.screen.simulacro.SimulacroViewModel
import com.udea.rutaudea.ui.screen.splash.SplashScreen
import com.udea.rutaudea.ui.screen.splash.SplashViewModel
import androidx.compose.ui.platform.LocalContext
import java.lang.reflect.Type

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(0) }
    val app = LocalContext.current.applicationContext as android.app.Application
    val repository = AppModule.provideQuestionRepository(app)
    val mvpProvider = AppModule.provideMvpQuestionProvider(repository)

    NavHost(navController, startDestination = AppNavGraph.SPLASH) {
        composable(AppNavGraph.SPLASH) {
            SplashScreen(
                onLoadingComplete = {
                    navController.navigate(AppNavGraph.HOME) {
                        popUpTo(AppNavGraph.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(AppNavGraph.HOME) {
            HomeScreen(
                onSimulacroClick = { navController.navigate(AppNavGraph.SIMULACRO) },
                onPracticaClick = { navController.navigate(AppNavGraph.PRACTICA) },
                onProgresoClick = { navController.navigate(AppNavGraph.PROGRESO) },
                onInfoClick = { navController.navigate(AppNavGraph.INFO) },
                onPerfilClick = { navController.navigate(AppNavGraph.PERFIL) }
            )
        }

        composable(AppNavGraph.SIMULACRO) {
            val backStackEntry = navController.getBackStackEntry(AppNavGraph.SIMULACRO)
            val savedStateHandle = backStackEntry.savedStateHandle
            val simulacroViewModel: SimulacroViewModel = viewModel(
                factory = ViewModelFactories.SimulacroFactory(mvpProvider, savedStateHandle)
            )
            val uiState = simulacroViewModel.uiState.value
            SimulacroScreen(
                viewModel = simulacroViewModel,
                onAbandon = { navController.popBackStack(AppNavGraph.HOME, inclusive = true) },
                onFinish = {
                    navController.navigate(AppNavGraph.RESULTADO)
                }
            )
        }

        composable(AppNavGraph.RESULTADO) { backStackEntry ->
            // Get data from Simulacro's SavedStateHandle via previous back stack entry
            val previousEntry = navController.getBackStackEntry(AppNavGraph.SIMULACRO)
            val simulacroSavedStateHandle = previousEntry.savedStateHandle
            
            val score = simulacroSavedStateHandle.get<Int>("simulation_score") ?: 0
            val total = simulacroSavedStateHandle.get<Int>("simulation_total") ?: 0
            val timeUsed = simulacroSavedStateHandle.get<Long>("simulation_time_used_ms") ?: 0L
            val userAnswersJson = simulacroSavedStateHandle.get<String>("simulation_user_answers_json") ?: "{}"
            val userAnswers = parseUserAnswers(userAnswersJson)
            
            val resultadoViewModel: ResultadoViewModel = viewModel(
                factory = ViewModelFactories.ResultadoFactory(mvpProvider, userAnswers, score, total, timeUsed)
            )
            ResultadoScreen(
                viewModel = resultadoViewModel,
                onFinish = {
                    navController.navigate(AppNavGraph.HOME) {
                        popUpTo(AppNavGraph.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Placeholder screens for other tabs
        composable(AppNavGraph.PRACTICA) {
            ComingSoonScreen(
                title = "Práctica",
                description = "Práctica filtrada por área, subtema y dificultad.\nCon retroalimentación inmediata.",
                onBack = { navController.navigate(AppNavGraph.HOME) }
            )
        }

        composable(AppNavGraph.PROGRESO) {
            ComingSoonScreen(
                title = "Progreso",
                description = "Historial de simulacros, gráficas de evolución\ny análisis de fortalezas y debilidades.",
                onBack = { navController.navigate(AppNavGraph.HOME) }
            )
        }

        composable(AppNavGraph.INFO) {
            ComingSoonScreen(
                title = "Información educativa",
                description = "Contenido teórico por componente y subtema\nde Razonamiento Lógico y Competencia Lectora.",
                onBack = { navController.navigate(AppNavGraph.HOME) }
            )
        }

        composable(AppNavGraph.PERFIL) {
            ComingSoonScreen(
                title = "Perfil",
                description = "Tu cuenta, configuración y preferencias.\nIntegración con Firebase Auth próximamente.",
                onBack = { navController.navigate(AppNavGraph.HOME) }
            )
        }
    }
}

private val gson = Gson()
private val typeToken = object : TypeToken<Map<Int, String>>() {}.type

private fun parseUserAnswers(json: String): Map<Int, String> {
    return try {
        gson.fromJson(json, typeToken)
    } catch (e: Exception) {
        emptyMap()
    }
}