package com.udea.rutaudea.ui.screen.common

import com.udea.rutaudea.data.repository.ReportePreguntaRepository
import com.udea.rutaudea.domain.model.ModoReporte
import com.udea.rutaudea.domain.model.MotivoReporte
import com.udea.rutaudea.domain.model.Question
import com.udea.rutaudea.domain.model.ReportePregunta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado y acciones del reporte de pregunta compartidos por las sesiones
 * de simulacro y práctica.
 *
 * Mantiene el diálogo ([estado]), las preguntas ya reportadas en la
 * sesión ([reportadas]) y el envío local-first a través de
 * [ReportePreguntaRepository]. Al centralizarlo aquí, ambas pantallas se
 * comportan igual y no duplican la lógica de validación ni los mensajes.
 */
class ReportePreguntaSesion(
    private val repository: ReportePreguntaRepository,
    private val scope: CoroutineScope,
    private val sesionId: String,
    private val modo: ModoReporte
) {

    private val _estado = MutableStateFlow(ReporteUiState())
    val estado: StateFlow<ReporteUiState> = _estado.asStateFlow()

    private val _reportadas = MutableStateFlow<Set<String>>(emptySet())
    val reportadas: StateFlow<Set<String>> = _reportadas.asStateFlow()

    /**
     * Restaura las preguntas ya reportadas tras rotación o muerte de
     * proceso, para que el icono aparezca deshabilitado.
     */
    fun cargarReportadas() {
        scope.launch {
            val ids = repository.getReportadasDeSesion(sesionId)
            if (ids.isNotEmpty()) _reportadas.value = ids
        }
    }

    /**
     * Abre el diálogo para [question] si aún no fue reportada y la sesión
     * permite reportar ([bloqueado] falso).
     */
    fun abrir(question: Question?, bloqueado: Boolean = false) {
        if (question == null || bloqueado || question.id in _reportadas.value) return
        _estado.value = ReporteUiState(visible = true)
    }

    /** Cierra el diálogo (no se puede cerrar mientras se está enviando). */
    fun cerrar() {
        if (_estado.value.enviando) return
        _estado.value = ReporteUiState()
    }

    /** Limpia el diálogo al cambiar de pregunta. */
    fun limpiar() {
        if (!_estado.value.enviando) _estado.value = ReporteUiState()
    }

    /**
     * Guarda el reporte local-first. La UI muestra si se envió ya o si
     * queda pendiente de sincronizar cuando haya conexión.
     */
    fun enviar(
        question: Question?,
        respuestaUsuario: String?,
        motivo: MotivoReporte,
        comentario: String
    ) {
        val estadoActual = _estado.value
        if (question == null || estadoActual.enviando || question.id in _reportadas.value) return

        val reporte = ReportePregunta(
            questionId = question.id,
            motivo = motivo,
            comentario = comentario.trim(),
            modo = modo,
            sesionId = sesionId,
            area = question.area,
            subtema = question.subtema,
            bancoVersion = question.bancoVersion,
            respuestaUsuario = respuestaUsuario,
            respuestaCorrecta = question.respuestaCorrecta
        )

        _estado.value = estadoActual.copy(enviando = true, error = null)

        scope.launch {
            when (val resultado = repository.guardar(reporte)) {
                is ReportePreguntaRepository.ResultadoReporte.Guardado -> {
                    _reportadas.value = _reportadas.value + question.id
                    _estado.value = ReporteUiState(
                        visible = true,
                        mensaje = if (resultado.sincronizada) {
                            "Reporte enviado. ¡Gracias por ayudarnos a mejorar!"
                        } else {
                            "Reporte guardado. Se enviará automáticamente cuando haya conexión."
                        }
                    )
                }

                is ReportePreguntaRepository.ResultadoReporte.Invalido -> {
                    _estado.value = _estado.value.copy(
                        enviando = false,
                        error = resultado.errores.joinToString(" ")
                    )
                }

                ReportePreguntaRepository.ResultadoReporte.Duplicado -> {
                    _reportadas.value = _reportadas.value + question.id
                    _estado.value = ReporteUiState(
                        visible = true,
                        mensaje = "Ya reportaste esta pregunta en esta sesión."
                    )
                }

                ReportePreguntaRepository.ResultadoReporte.SinSesion -> {
                    _estado.value = _estado.value.copy(
                        enviando = false,
                        error = "Inicia sesión para enviar el reporte."
                    )
                }

                ReportePreguntaRepository.ResultadoReporte.ErrorLocal -> {
                    _estado.value = _estado.value.copy(
                        enviando = false,
                        error = "No se pudo guardar el reporte. Intenta de nuevo."
                    )
                }
            }
        }
    }
}
