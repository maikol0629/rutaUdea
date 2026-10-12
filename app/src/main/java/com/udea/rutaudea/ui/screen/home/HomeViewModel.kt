package com.udea.rutaudea.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udea.rutaudea.data.repository.AuthRepository
import com.udea.rutaudea.data.repository.EncuestaRepository
import com.udea.rutaudea.data.repository.RachaRepository
import com.udea.rutaudea.domain.model.Racha
import com.udea.rutaudea.domain.services.Saludo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Estado del Home: racha de actividad, saludo personalizado con el
 * nombre del usuario y estado de la encuesta piloto. La racha lee de la
 * cache local (offline-first) y se refresca al reanudar la pantalla, de
 * modo que refleja de inmediato la actividad que se acaba de completar.
 */
class HomeViewModel(
    private val rachaRepository: RachaRepository,
    private val authRepository: AuthRepository,
    private val encuestaRepository: EncuestaRepository
) : ViewModel() {

    data class UiState(
        val cargando: Boolean = true,
        val racha: Racha = Racha(),
        val saludo: Saludo.Encabezado = Saludo.Encabezado(momento = "", nombre = ""),
        /** `true` si el usuario ya respondió la encuesta piloto. */
        val encuestaRespondida: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Indica si ya se consumió la carga inicial del [init]. Vive en el
     * ViewModel (no en la UI con `remember`) porque al navegar fuera del
     * Home la composición se destruye y el `remember` se pierde: al volver
     * se trataría como "primera vez" y nunca se recargaría la racha.
     */
    private var arranque = true

    init {
        cargar()
    }

    /** Carga la racha vigente, el saludo y el estado de la encuesta. */
    fun cargar() {
        viewModelScope.launch {
            val racha = rachaRepository.cargarVigente()
            val nombre = authRepository.restoreSession()?.nombre?.trim().orEmpty()
            val respondida = try {
                encuestaRepository.cargar() != null
            } catch (e: Exception) {
                false
            }
            val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            _uiState.value = UiState(
                cargando = false,
                racha = racha,
                saludo = Saludo.partes(nombre, hora),
                encuestaRespondida = respondida
            )
        }
    }

    /**
     * Refresca al volver al Home. Omite la primera llamada porque el [init]
     * ya cargó; en las siguientes recarga siempre.
     */
    fun alReanudar() {
        if (arranque) {
            arranque = false
            return
        }
        cargar()
    }
}
