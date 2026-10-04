package com.udea.rutaudea.ui.screen.practica

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udea.rutaudea.data.repository.PracticeRepository
import com.udea.rutaudea.domain.model.Question
import com.udea.rutaudea.ui.screen.common.textoApoyoDe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Resultado de una sesión de práctica: resumen del desempeño, repaso por
 * pregunta y persistencia en Firestore (`practiceSessions`).
 */
class PracticaResultadoViewModel(
    private val questions: List<Question>,
    private val practiceRepository: PracticeRepository,
    private val userAnswers: Map<Int, String>,
    private val area: String,
    private val subtema: String?,
    score: Int,
    total: Int,
    timeUsedMs: Long
) : ViewModel() {

    data class PracticaResult(
        val index: Int,
        val subtema: String,
        val dificultad: String,
        val esCorrecta: Boolean,
        val respondida: Boolean,
        val respuestaUsuario: String,
        val respuestaCorrecta: String,
        val enunciado: String,
        val textoApoyo: String?,
        val explicacion: String,
        val recomendacion: String
    )

    private val _results = MutableStateFlow<List<PracticaResult>>(emptyList())
    val results: StateFlow<List<PracticaResult>> = _results.asStateFlow()

    private val _score = MutableStateFlow(score)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _total = MutableStateFlow(total)
    val total: StateFlow<Int> = _total.asStateFlow()

    private val _timeUsedMs = MutableStateFlow(timeUsedMs)
    val timeUsedMs: StateFlow<Long> = _timeUsedMs.asStateFlow()

    /** Etiqueta legible de los filtros usados (área y subtema). */
    val filtrosLabel: String = buildString {
        append(if (area == "razonamiento_logico") "Razonamiento Lógico" else "Competencia Lectora")
        if (!subtema.isNullOrBlank()) append(" · $subtema")
    }

    init {
        loadResults()
        persistPracticeSession()
    }

    private fun loadResults() {
        val results = questions.mapIndexed { index, question ->
            val userAnswer = userAnswers[index]
            val esCorrecta = userAnswer != null && userAnswer == question.respuestaCorrecta
            PracticaResult(
                index = index,
                subtema = question.subtema,
                dificultad = question.dificultad,
                esCorrecta = esCorrecta,
                respondida = userAnswer != null,
                respuestaUsuario = userAnswer ?: "—",
                respuestaCorrecta = question.respuestaCorrecta,
                enunciado = question.pregunta,
                textoApoyo = textoApoyoDe(question),
                explicacion = question.explicacion.orEmpty(),
                recomendacion = recomendacionPara(question.subtema, esCorrecta, userAnswer != null)
            )
        }
        _results.value = results
    }

    /** Recomendación estática por subtema (motor de recomendaciones: P1 futuro). */
    private fun recomendacionPara(subtema: String, esCorrecta: Boolean, respondida: Boolean): String = when {
        !respondida -> "Omitaste esta pregunta: repasa «$subtema» y practícalo de nuevo."
        esCorrecta -> "Buen manejo de «$subtema». Sigue practicando para mantenerlo."
        else -> "Refuerza «$subtema»: revisa la explicación y practica más preguntas de este tema."
    }

    private fun persistPracticeSession() {
        viewModelScope.launch {
            practiceRepository.savePracticeSession(
                area = area,
                subtema = subtema,
                questions = questions,
                userAnswers = userAnswers,
                score = _score.value,
                total = _total.value,
                timeUsedMs = _timeUsedMs.value
            )
        }
    }
}
