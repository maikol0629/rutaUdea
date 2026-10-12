package com.udea.rutaudea.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udea.rutaudea.data.repository.RachaRepository
import com.udea.rutaudea.domain.model.Racha
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado del Home: expone la racha de actividad del usuario para
 * mostrarla al entrar. Lee de la cache local (offline-first) y se
 * refresca al reanudar la pantalla, de modo que refleja de inmediato la
 * actividad que se acaba de completar.
 */
class HomeViewModel(
    private val rachaRepository: RachaRepository
) : ViewModel() {

    data class UiState(
        val cargando: Boolean = true,
        val racha: Racha = Racha()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    /** Carga la racha vigente. */
    fun cargar() {
        viewModelScope.launch {
            val racha = rachaRepository.cargarVigente()
            _uiState.value = UiState(cargando = false, racha = racha)
        }
    }
}
