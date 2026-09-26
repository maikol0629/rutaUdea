package com.udea.rutaudea.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
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

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
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
            SimulacroScreen(
                viewModel = simulacroViewModel,
                onAbandon = { navController.popBackStack(AppNavGraph.HOME, inclusive = false) },
                onFinish = {
                    navController.navigate(AppNavGraph.RESULTADO)
                }
            )
        }

        composable(AppNavGraph.RESULTADO) { backStackEntry ->
            // Get data from Simulacro's SavedStateHandle via previous back stack entry.
            // Read it ONCE with remember: during the exit animation (after SIMULACRO was
            // popped from the back stack) getBackStackEntry() would throw and crash the app.
            val resultadoData = remember {
                val simulacroSavedStateHandle =
                    navController.getBackStackEntry(AppNavGraph.SIMULACRO).savedStateHandle
                ResultadoData(
                    score = simulacroSavedStateHandle.get<Int>("simulation_score") ?: 0,
                    total = simulacroSavedStateHandle.get<Int>("simulation_total") ?: 0,
                    timeUsed = simulacroSavedStateHandle.get<Long>("simulation_time_used_ms") ?: 0L,
                    userAnswers = parseUserAnswers(
                        simulacroSavedStateHandle.get<String>("simulation_user_answers_json") ?: "{}"
                    )
                )
            }
            
            val resultadoViewModel: ResultadoViewModel = viewModel(
                factory = ViewModelFactories.ResultadoFactory(
                    mvpProvider,
                    resultadoData.userAnswers,
                    resultadoData.score,
                    resultadoData.total,
                    resultadoData.timeUsed
                )
            )
            ResultadoScreen(
                viewModel = resultadoViewModel,
                onFinish = {
                    // HOME is already in the back stack below SIMULACRO/RESULTADO,
                    // so simply pop back to it (avoids re-pushing HOME and crashes).
                    navController.popBackStack(AppNavGraph.HOME, inclusive = false)
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

private data class ResultadoData(
    val score: Int,
    val total: Int,
    val timeUsed: Long,
    val userAnswers: Map<Int, String>
)

private val gson = Gson()
private val typeToken = object : TypeToken<Map<Int, String>>() {}.type

private fun parseUserAnswers(json: String): Map<Int, String> {
    return try {
        gson.fromJson(json, typeToken)
    } catch (e: Exception) {
        emptyMap()
    }
}