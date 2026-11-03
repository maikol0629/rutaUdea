package com.udea.rutaudea.ui.screen.practica

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.udea.rutaudea.data.repository.ReportePreguntaRepository
import com.udea.rutaudea.domain.model.ModoReporte
import com.udea.rutaudea.domain.model.MotivoReporte
import com.udea.rutaudea.domain.model.Question
import com.udea.rutaudea.domain.model.ReportePregunta
import com.udea.rutaudea.ui.screen.common.ReporteUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Sesión de práctica: sin cronómetro límite, con retroalimentación
 * inmediata al responder (plan, sección 10): respuesta correcta,
 * respuesta seleccionada, explicación, subtema y dificultad.
 *
 * La respuesta queda bloqueada al revelarse (no se puede cambiar).
 * El progreso (índice, respuestas, reveladas) sobrevive rotación y
 * segundo plano vía [SavedStateHandle].
 */
class PracticaSesionViewModel(
    questions: List<Question>,
    val area: String,
    val subtema: String?,
    private val reporteRepository: ReportePreguntaRepository,
    private val savedStateHandle: SavedStateHandle,
    /** Desactivable en pruebas: el reloj es un bucle infinito. */
    private val relojActivo: Boolean = true
) : ViewModel() {

    /** Navegación: la sesión terminó y los resultados están listos. */
    sealed interface SesionEvento {
        data object PracticaTerminada : SesionEvento
    }

    data class SesionState(
        val questions: List<Question> = emptyList(),
        val currentIndex: Int = 0,
        /** Letra seleccionada por el usuario, por índice de pregunta. */
        val answers: Map<Int, String> = emptyMap(),
        /** Índices ya revelados (respuesta registrada y bloqueada). */
        val revealed: Set<Int> = emptySet(),
        val elapsedMs: Long = 0L,
        /** Overlay de pausa visible (app en segundo plano o botón atrás). */
        val showPauseOverlay: Boolean = false,
        val reporte: ReporteUiState = ReporteUiState(),
        /** IDs de preguntas ya reportadas en esta sesión. */
        val reportadas: Set<String> = emptySet()
    ) {
        val currentQuestion: Question?
            get() = questions.getOrNull(currentIndex)

        val isRevealed: Boolean
            get() = currentIndex in revealed

        val currentAnswer: String?
            get() = answers[currentIndex]

        val isCorrect: Boolean
            get() = currentQuestion?.let { answers[currentIndex] == it.respuestaCorrecta } ?: false

        val isLastQuestion: Boolean
            get() = currentIndex == questions.lastIndex

        val respondidas: Int
            get() = revealed.size

        val score: Int
            get() = questions.indices.count { index ->
                answers[index] == questions[index].respuestaCorrecta
            }

        val elapsedSeconds: Int
            get() = (elapsedMs / 1000).toInt()
    }

    private val _uiState = MutableStateFlow(SesionState(questions = questions))
    val uiState: StateFlow<SesionState> = _uiState.asStateFlow()

    private val _evento = MutableSharedFlow<SesionEvento>(extraBufferCapacity = 1)
    val evento: SharedFlow<SesionEvento> = _evento.asSharedFlow()

    private var tickerJob: Job? = null

    private var startTimeMs: Long

    /**
     * Identificador del intento. Se genera al iniciar y se conserva en el
     * SavedStateHandle para que los reportes de la sesión apunten a la
     * misma práctica que se guardará al finalizar.
     */
    private val sesionId: String = savedStateHandle.get<String>(SESION_ID_KEY)
        ?: UUID.randomUUID().toString().also { savedStateHandle[SESION_ID_KEY] = it }

    /**
     * Marca que la app pasó a segundo plano de verdad (no una rotación),
     * para no mostrar la pausa en el `ON_START` sintético del registro.
     */
    private var pausadoPorBackground: Boolean = false

    init {
        // Restauración de progreso (rotación / background).
        val saved: Long? = savedStateHandle[START_TIME_KEY]
        startTimeMs = saved ?: System.currentTimeMillis().also {
            savedStateHandle[START_TIME_KEY] = it
        }
        restoreProgreso()
        cargarReportadas()
        // Si el proceso muere mientras estaba en pausa, se reanuda en pausa.
        if (savedStateHandle.contains(PAUSED_AT_KEY)) {
            _uiState.value = _uiState.value.copy(showPauseOverlay = true)
        } else {
            iniciarTicker()
        }
    }

    /** Restaura el estado del icono de reporte tras rotación/muerte de proceso. */
    private fun cargarReportadas() {
        viewModelScope.launch {
            val ids = reporteRepository.getReportadasDeSesion(sesionId)
            if (ids.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(reportadas = ids)
            }
        }
    }

    private fun restoreProgreso() {
        val answers: Map<Int, String>? = savedStateHandle[ANSWERS_KEY]
        val revealed: List<Int>? = savedStateHandle[REVEALED_KEY]
        val index: Int = savedStateHandle[CURRENT_INDEX_KEY] ?: 0
        if (answers == null && revealed == null) return

        val lastIndex = _uiState.value.questions.lastIndex
        _uiState.value = _uiState.value.copy(
            currentIndex = index.coerceIn(0, lastIndex),
            answers = answers.orEmpty(),
            revealed = revealed?.toSet().orEmpty()
        )
    }

    /** Reloj informativo ascendente (la práctica no tiene límite de tiempo). */
    private fun iniciarTicker() {
        if (!relojActivo) return
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - startTimeMs
                _uiState.value = _uiState.value.copy(elapsedMs = elapsed)
                delay(1000)
            }
        }
    }

    // ----- Pausa / persistencia al cambiar de app -----

    /**
     * La app pasó a segundo plano: se congela el reloj informativo (el
     * tiempo de práctica no debe contar mientras no se practica).
     */
    fun onAppBackgrounded() {
        if (_uiState.value.questions.isEmpty()) return
        pausadoPorBackground = true
        pausarCronometro()
    }

    /** La app volvió a primer plano: mostrar el overlay de pausa. */
    fun onAppResumed() {
        if (!pausadoPorBackground) return
        pausadoPorBackground = false
        if (_uiState.value.questions.isEmpty()) return
        _uiState.value = _uiState.value.copy(showPauseOverlay = true)
    }

    /** El usuario pulsó atrás: abrir el overlay en vez de salir. */
    fun onPauseRequested() {
        if (_uiState.value.questions.isEmpty()) return
        pausarCronometro()
        _uiState.value = _uiState.value.copy(showPauseOverlay = true)
    }

    /** Detiene el reloj y registra el instante de pausa (idempotente). */
    private fun pausarCronometro() {
        if (savedStateHandle.contains(PAUSED_AT_KEY)) return
        savedStateHandle[PAUSED_AT_KEY] = System.currentTimeMillis()
        tickerJob?.cancel()
    }

    /** Reanuda la práctica descontando el tiempo que estuvo en pausa. */
    fun onResumeConfirmed() {
        val pausedAt: Long? = savedStateHandle[PAUSED_AT_KEY]
        if (pausedAt != null) {
            startTimeMs += (System.currentTimeMillis() - pausedAt)
            savedStateHandle[START_TIME_KEY] = startTimeMs
            savedStateHandle.remove<Long>(PAUSED_AT_KEY)
        }
        pausadoPorBackground = false
        _uiState.value = _uiState.value.copy(showPauseOverlay = false)
        iniciarTicker()
    }

    /** Registra la respuesta de la pregunta actual y revela el feedback. */
    fun onOptionSelected(letter: String) {
        val state = _uiState.value
        if (state.isRevealed) return // bloqueada: no se puede cambiar

        val index = state.currentIndex
        val answers = state.answers + (index to letter)
        val revealed = state.revealed + index

        savedStateHandle[ANSWERS_KEY] = answers
        savedStateHandle[REVEALED_KEY] = revealed.toList()
        _uiState.value = state.copy(answers = answers, revealed = revealed)
    }

    // ----- Reporte de pregunta -----

    /** Abre el diálogo de reporte para la pregunta actual. */
    fun onReportarClick() {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        if (question.id in state.reportadas) return
        _uiState.value = state.copy(reporte = ReporteUiState(visible = true))
    }

    /** Cierra el diálogo (no se puede cerrar mientras se está enviando). */
    fun onReporteDismiss() {
        val state = _uiState.value
        if (state.reporte.enviando) return
        _uiState.value = state.copy(reporte = ReporteUiState())
    }

    /**
     * Guarda el reporte local-first. La UI muestra si se envió ya o si
     * queda pendiente de sincronizar cuando haya conexión.
     */
    fun onReporteEnviar(motivo: MotivoReporte, comentario: String) {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        if (state.reporte.enviando || question.id in state.reportadas) return

        val reporte = ReportePregunta(
            questionId = question.id,
            motivo = motivo,
            comentario = comentario.trim(),
            modo = ModoReporte.PRACTICA,
            sesionId = sesionId,
            area = question.area,
            subtema = question.subtema,
            bancoVersion = question.bancoVersion,
            respuestaUsuario = state.currentAnswer,
            respuestaCorrecta = question.respuestaCorrecta
        )

        _uiState.value = state.copy(
            reporte = state.reporte.copy(enviando = true, error = null)
        )

        viewModelScope.launch {
            when (val resultado = reporteRepository.guardar(reporte)) {
                is ReportePreguntaRepository.ResultadoReporte.Guardado -> {
                    _uiState.value = _uiState.value.copy(
                        reportadas = _uiState.value.reportadas + question.id,
                        reporte = ReporteUiState(
                            visible = true,
                            mensaje = if (resultado.sincronizada) {
                                "Reporte enviado. ¡Gracias por ayudarnos a mejorar!"
                            } else {
                                "Reporte guardado. Se enviará automáticamente cuando haya conexión."
                            }
                        )
                    )
                }

                is ReportePreguntaRepository.ResultadoReporte.Invalido -> {
                    _uiState.value = _uiState.value.copy(
                        reporte = _uiState.value.reporte.copy(
                            enviando = false,
                            error = resultado.errores.joinToString(" ")
                        )
                    )
                }

                ReportePreguntaRepository.ResultadoReporte.Duplicado -> {
                    _uiState.value = _uiState.value.copy(
                        reportadas = _uiState.value.reportadas + question.id,
                        reporte = ReporteUiState(
                            visible = true,
                            mensaje = "Ya reportaste esta pregunta en esta sesión."
                        )
                    )
                }

                ReportePreguntaRepository.ResultadoReporte.SinSesion -> {
                    _uiState.value = _uiState.value.copy(
                        reporte = _uiState.value.reporte.copy(
                            enviando = false,
                            error = "Inicia sesión para enviar el reporte."
                        )
                    )
                }

                ReportePreguntaRepository.ResultadoReporte.ErrorLocal -> {
                    _uiState.value = _uiState.value.copy(
                        reporte = _uiState.value.reporte.copy(
                            enviando = false,
                            error = "No se pudo guardar el reporte. Intenta de nuevo."
                        )
                    )
                }
            }
        }
    }

    fun onNext() {
        val state = _uiState.value
        if (state.isLastQuestion) {
            finalizar(state)
        } else {
            val newIndex = state.currentIndex + 1
            savedStateHandle[CURRENT_INDEX_KEY] = newIndex
            _uiState.value = state.copy(
                currentIndex = newIndex,
                reporte = ReporteUiState()
            )
        }
    }

    fun onPrevious() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            val newIndex = state.currentIndex - 1
            savedStateHandle[CURRENT_INDEX_KEY] = newIndex
            _uiState.value = state.copy(
                currentIndex = newIndex,
                reporte = ReporteUiState()
            )
        }
    }

    /** Permite terminar en cualquier momento (las no reveladas quedan omitidas). */
    fun onTerminar() {
        finalizar(_uiState.value)
    }

    private fun finalizar(state: SesionState) {
        val score = state.score
        val total = state.questions.size
        val timeUsedMs = System.currentTimeMillis() - startTimeMs

        savedStateHandle[SCORE_KEY] = score
        savedStateHandle[TOTAL_KEY] = total
        savedStateHandle[TIME_USED_KEY] = timeUsedMs
        savedStateHandle[ANSWERS_JSON_KEY] = gson.toJson(state.answers)
        savedStateHandle[QUESTIONS_JSON_KEY] = gson.toJson(state.questions)
        savedStateHandle[AREA_KEY] = area
        savedStateHandle[SUBTEMA_KEY] = subtema ?: ""

        _evento.tryEmit(SesionEvento.PracticaTerminada)
    }

    override fun onCleared() {
        tickerJob?.cancel()
        super.onCleared()
    }

    companion object {
        const val SCORE_KEY = "practica_score"
        const val TOTAL_KEY = "practica_total"
        const val TIME_USED_KEY = "practica_time_used_ms"
        const val ANSWERS_JSON_KEY = "practica_user_answers_json"
        const val QUESTIONS_JSON_KEY = "practica_questions_json"
        const val AREA_KEY = "practica_area"
        const val SUBTEMA_KEY = "practica_subtema"
        const val SESION_ID_KEY = "practica_session_id"
        private const val START_TIME_KEY = "practica_start_time_ms"
        private const val PAUSED_AT_KEY = "practica_paused_at_ms"
        private const val CURRENT_INDEX_KEY = "practica_current_index"
        private const val ANSWERS_KEY = "practica_answers"
        private const val REVEALED_KEY = "practica_revealed"
        private val gson = Gson()
    }
}
