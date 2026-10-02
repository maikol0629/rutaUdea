package com.udea.rutaudea.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.udea.rutaudea.di.AppModule
import com.udea.rutaudea.di.ViewModelFactories
import com.udea.rutaudea.ui.screen.common.ComingSoonScreen
import com.udea.rutaudea.ui.screen.home.HomeScreen
import com.udea.rutaudea.ui.screen.perfil.PerfilScreen
import com.udea.rutaudea.ui.screen.perfil.PerfilViewModel
import com.udea.rutaudea.ui.screen.practica.PracticaFiltrosScreen
import com.udea.rutaudea.ui.screen.practica.PracticaFiltrosViewModel
import com.udea.rutaudea.ui.screen.practica.PracticaResultadoScreen
import com.udea.rutaudea.ui.screen.practica.PracticaResultadoViewModel
import com.udea.rutaudea.ui.screen.practica.PracticaSesionScreen
import com.udea.rutaudea.ui.screen.practica.PracticaSesionViewModel
import com.udea.rutaudea.ui.screen.progreso.ProgresoScreen
import com.udea.rutaudea.ui.screen.progreso.ProgresoViewModel
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
    val selector = AppModule.provideQuestionSelector()
    val simulationRepository = AppModule.provideSimulationRepository(app)

    NavHost(navController, startDestination = AppNavGraph.SPLASH) {
        composable(AppNavGraph.SPLASH) {
            SplashScreen(
                onReady = { sesionActiva ->
                    // Gate de autenticación: con sesión → Home; sin sesión → login.
                    if (sesionActiva) {
                        navController.navigate(AppNavGraph.HOME) {
                            popUpTo(AppNavGraph.SPLASH) { inclusive = true }
                        }
                    } else {
                        navController.navigate(AppNavGraph.perfilRoute(gate = true)) {
                            popUpTo(AppNavGraph.SPLASH) { inclusive = true }
                        }
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
                factory = ViewModelFactories.SimulacroFactory(
                    selector,
                    repository,
                    simulationRepository,
                    savedStateHandle
                )
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
                    ),
                    questions = parseQuestions(
                        simulacroSavedStateHandle.get<String>("simulation_questions_json") ?: "[]"
                    )
                )
            }
            
            val resultadoViewModel: ResultadoViewModel = viewModel(
                factory = ViewModelFactories.ResultadoFactory(
                    resultadoData.questions,
                    simulationRepository,
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

        // ----- Módulo de Práctica (filtros → sesión → resultado) -----
        composable(AppNavGraph.PRACTICA) { entry ->
            val filtrosViewModel: PracticaFiltrosViewModel = viewModel(
                factory = ViewModelFactories.PracticaFiltrosFactory(
                    repository,
                    AppModule.providePracticeSelector(),
                    entry.savedStateHandle
                )
            )
            PracticaFiltrosScreen(
                viewModel = filtrosViewModel,
                onIniciar = { navController.navigate(AppNavGraph.PRACTICA_SESION) }
            )
        }

        composable(AppNavGraph.PRACTICA_SESION) {
            // Lectura única de la selección hecha en Filtros (SAH de esa entrada).
            val sesionData = remember {
                val filtrosSah = navController.getBackStackEntry(AppNavGraph.PRACTICA).savedStateHandle
                PracticaSesionData(
                    questions = parseQuestions(
                        filtrosSah.get<String>(PracticaFiltrosViewModel.PREGUNTAS_KEY) ?: "[]"
                    ),
                    area = filtrosSah.get<String>(PracticaFiltrosViewModel.AREA_KEY)
                        ?: PracticaFiltrosViewModel.AREA_RL,
                    subtema = filtrosSah.get<String>(PracticaFiltrosViewModel.SUBTEMA_KEY)?.ifBlank { null }
                )
            }
            val sesionViewModel: PracticaSesionViewModel = viewModel(
                factory = ViewModelFactories.PracticaSesionFactory(
                    sesionData.questions,
                    sesionData.area,
                    sesionData.subtema,
                    it.savedStateHandle
                )
            )
            PracticaSesionScreen(
                viewModel = sesionViewModel,
                onAbandonar = {
                    navController.popBackStack(AppNavGraph.PRACTICA, inclusive = false)
                },
                onTerminada = {
                    navController.navigate(AppNavGraph.PRACTICA_RESULTADO)
                }
            )
        }

        composable(AppNavGraph.PRACTICA_RESULTADO) {
            // Lectura única de los resultados (SAH de la sesión) con remember:
            // durante la animación de salida getBackStackEntry() podría fallar.
            val resultadoData = remember {
                val sesionSah = navController.getBackStackEntry(AppNavGraph.PRACTICA_SESION).savedStateHandle
                PracticaResultadoData(
                    score = sesionSah.get<Int>(PracticaSesionViewModel.SCORE_KEY) ?: 0,
                    total = sesionSah.get<Int>(PracticaSesionViewModel.TOTAL_KEY) ?: 0,
                    timeUsedMs = sesionSah.get<Long>(PracticaSesionViewModel.TIME_USED_KEY) ?: 0L,
                    userAnswers = parseUserAnswers(
                        sesionSah.get<String>(PracticaSesionViewModel.ANSWERS_JSON_KEY) ?: "{}"
                    ),
                    questions = parseQuestions(
                        sesionSah.get<String>(PracticaSesionViewModel.QUESTIONS_JSON_KEY) ?: "[]"
                    ),
                    area = sesionSah.get<String>(PracticaSesionViewModel.AREA_KEY)
                        ?: PracticaFiltrosViewModel.AREA_RL,
                    subtema = sesionSah.get<String>(PracticaSesionViewModel.SUBTEMA_KEY)?.ifBlank { null }
                )
            }
            val practiceRepository = AppModule.providePracticeRepository(app)
            val resultadoViewModel: PracticaResultadoViewModel = viewModel(
                factory = ViewModelFactories.PracticaResultadoFactory(
                    resultadoData.questions,
                    practiceRepository,
                    resultadoData.userAnswers,
                    resultadoData.area,
                    resultadoData.subtema,
                    resultadoData.score,
                    resultadoData.total,
                    resultadoData.timeUsedMs
                )
            )
            PracticaResultadoScreen(
                viewModel = resultadoViewModel,
                onRepetir = {
                    navController.popBackStack(AppNavGraph.PRACTICA, inclusive = false)
                },
                onVolverInicio = {
                    navController.popBackStack(AppNavGraph.HOME, inclusive = false)
                }
            )
        }

        composable(AppNavGraph.PROGRESO) {
            val progresoViewModel: ProgresoViewModel = viewModel(
                factory = ViewModelFactories.ProgresoFactory(
                    AppModule.provideProgressRepository(app)
                )
            )
            ProgresoScreen(
                viewModel = progresoViewModel,
                onSimulacroClick = { navController.navigate(AppNavGraph.SIMULACRO) },
                onPracticaClick = { navController.navigate(AppNavGraph.PRACTICA) }
            )
        }

        composable(AppNavGraph.INFO) {
            ComingSoonScreen(
                title = "Información educativa",
                description = "Contenido teórico por componente y subtema\nde Razonamiento Lógico y Competencia Lectora.",
                onBack = { navController.navigate(AppNavGraph.HOME) }
            )
        }

        composable(
            route = AppNavGraph.PERFIL_ARGS,
            arguments = listOf(navArgument("gate") {
                type = NavType.BoolType
                defaultValue = false
            })
        ) { entry ->
            val esGate = entry.arguments?.getBoolean("gate") ?: false
            val authRepository = AppModule.provideAuthRepository(app)
            val perfilViewModel: PerfilViewModel = viewModel(
                factory = ViewModelFactories.PerfilFactory(authRepository)
            )
            PerfilScreen(
                viewModel = perfilViewModel,
                esGate = esGate,
                onAuthSuccess = {
                    // Login completado desde el gate: Home se vuelve la raíz.
                    navController.navigate(AppNavGraph.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onLogoutNavigate = {
                    // Logout desde cualquier punto: el gate de login vuelve a ser raíz.
                    navController.navigate(AppNavGraph.perfilRoute(gate = true)) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private data class ResultadoData(
    val score: Int,
    val total: Int,
    val timeUsed: Long,
    val userAnswers: Map<Int, String>,
    val questions: List<com.udea.rutaudea.domain.model.Question>
)

private data class PracticaSesionData(
    val questions: List<com.udea.rutaudea.domain.model.Question>,
    val area: String,
    val subtema: String?
)

private data class PracticaResultadoData(
    val score: Int,
    val total: Int,
    val timeUsedMs: Long,
    val userAnswers: Map<Int, String>,
    val questions: List<com.udea.rutaudea.domain.model.Question>,
    val area: String,
    val subtema: String?
)

private val gson = Gson()
private val typeToken = object : TypeToken<Map<Int, String>>() {}.type
private val questionListType = object : TypeToken<List<com.udea.rutaudea.domain.model.Question>>() {}.type

private fun parseUserAnswers(json: String): Map<Int, String> {
    return try {
        gson.fromJson(json, typeToken)
    } catch (e: Exception) {
        emptyMap()
    }
}

private fun parseQuestions(json: String): List<com.udea.rutaudea.domain.model.Question> {
    return try {
        gson.fromJson(json, questionListType)
    } catch (e: Exception) {
        emptyList()
    }
}