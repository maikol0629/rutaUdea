package com.udea.rutaudea.ui.screen.info

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udea.rutaudea.data.source.local.InfoContentLoader
import com.udea.rutaudea.domain.model.TemaEducativo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Módulo Info: contenido educativo por componente del examen,
 * filtrable por área (RL/CL).
 */
class InfoViewModel(context: Context) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val area: String = TemaEducativo.AREA_RL,
        val todos: List<TemaEducativo> = emptyList(),
        val expandidoId: String? = null
    ) {
        val visibles: List<TemaEducativo>
            get() = filtrarPorArea(todos, area)
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val loader = InfoContentLoader(context, com.google.gson.Gson())

    init {
        cargar()
    }

    private fun cargar() {
        viewModelScope.launch {
            val temas = loader.loadFromAsset()
            _uiState.update { it.copy(isLoading = false, todos = temas) }
        }
    }

    fun onAreaChange(area: String) {
        _uiState.update { it.copy(area = area, expandidoId = null) }
    }

    fun onToggleExpandido(id: String) {
        _uiState.update {
            it.copy(expandidoId = if (it.expandidoId == id) null else id)
        }
    }

    companion object {
        /** Filtra los temas por área (pura, testeable). */
        fun filtrarPorArea(temas: List<TemaEducativo>, area: String): List<TemaEducativo> =
            temas.filter { it.area == area }
    }
}
