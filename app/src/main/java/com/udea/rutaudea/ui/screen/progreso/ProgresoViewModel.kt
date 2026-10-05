package com.udea.rutaudea.ui.screen.progreso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udea.rutaudea.data.repository.ProgressRepository
import com.udea.rutaudea.domain.model.ItemHistorial
import com.udea.rutaudea.domain.model.PracticaResumen
import com.udea.rutaudea.domain.model.SimulacroResumen
import com.udea.rutaudea.domain.model.SubtemaEstadistica
import com.udea.rutaudea.domain.services.AnalizadorProgreso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado del módulo de Progreso: historial de simulacros y prácticas,
 * evolución, fortalezas, áreas de mejora y recomendaciones.
 *
 * Lee siempre de la cache local (Room) y, en paralelo, sincroniza con
 * Firestore para traer lo que falte o subir lo pendiente.
 */
class ProgresoViewModel(
    private val repository: ProgressRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val simulacros: List<SimulacroResumen> = emptyList(),
        val practicas: List<PracticaResumen> = emptyList(),
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

    /** Carga completa: local inmediato + sincronización con la nube. */
    fun cargar() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            aplicarLocal()
            repository.sincronizar()
            aplicarLocal()
        }
    }

    /** Refresco silencioso (pull-to-refresh / volver a la pantalla). */
    fun refrescar() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            repository.sincronizar()
            aplicarLocal()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    /** Recalcula el estado a partir de la cache local. */
    private suspend fun aplicarLocal() {
        val simulacros = repository.cargarSimulacros()
        val practicas = repository.cargarPracticas()
        val stats = repository.cargarEstadisticasPorSubtema(simulacros, practicas)

        _uiState.update { current ->
            current.copy(
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
