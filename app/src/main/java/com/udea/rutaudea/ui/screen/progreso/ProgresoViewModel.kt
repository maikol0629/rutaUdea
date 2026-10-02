package com.udea.rutaudea.ui.screen.progreso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udea.rutaudea.data.repository.ProgressRepository
import com.udea.rutaudea.domain.model.ItemHistorial
import com.udea.rutaudea.domain.model.SubtemaEstadistica
import com.udea.rutaudea.domain.services.AnalizadorProgreso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado del módulo de Progreso: historial de simulacros y prácticas,
 * evolución, fortalezas, áreas de mejora y recomendaciones
 * (plan, sección 12).
 */
class ProgresoViewModel(
    private val repository: ProgressRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val simulacros: List<com.udea.rutaudea.domain.model.SimulacroResumen> = emptyList(),
        val practicas: List<com.udea.rutaudea.domain.model.PracticaResumen> = emptyList(),
        val evolucion: List<Int> = emptyList(),
        val areasDeMejora: List<SubtemaEstadistica> = emptyList(),
        val fortalezas: List<SubtemaEstadistica> = emptyList(),
        val historial: List<ItemHistorial> = emptyList(),
        val recomendaciones: List<SubtemaEstadistica> = emptyList()
    ) {
        val sinDatos: Boolean
            get() = simulacros.isEmpty() && practicas.isEmpty()

        val promedioGlobal: Int
            get() = AnalizadorProgreso.promedio(simulacros)

        val mejorPuntaje: Int
            get() = AnalizadorProgreso.mejorPuntaje(simulacros)

        /** Texto de las recomendaciones del plan §12 (subtemas débiles con evidencia). */
        val recomendacionesTexto: List<String>
            get() = recomendaciones.map {
                "Practica «${it.subtema}» — acierto ${it.porcentaje}% en ${it.respondidas} preguntas"
            }
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val simulacros = repository.cargarSimulacros()
            val practicas = repository.cargarPracticas()
            val stats = repository.cargarEstadisticasPorSubtema(simulacros, practicas)

            _uiState.value = UiState(
                isLoading = false,
                simulacros = simulacros,
                practicas = practicas,
                evolucion = AnalizadorProgreso.evolucion(simulacros),
                areasDeMejora = AnalizadorProgreso.areasDeMejora(stats),
                fortalezas = AnalizadorProgreso.fortalezas(stats),
                historial = AnalizadorProgreso.historial(simulacros, practicas),
                recomendaciones = AnalizadorProgreso.areasDeMejora(stats)
                    .take(AnalizadorProgreso.RECOMENDACIONES)
            )
        }
    }
}
