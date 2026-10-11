package com.udea.rutaudea.ui.screen.practica

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udea.rutaudea.domain.model.Question
import com.udea.rutaudea.ui.screen.common.AppBackgroundEffect
import com.udea.rutaudea.ui.screen.common.BarraProgresoSesion
import com.udea.rutaudea.ui.screen.common.OpcionesPreguntaCard
import com.udea.rutaudea.ui.screen.common.PauseOverlay
import com.udea.rutaudea.ui.screen.common.TextoApoyo
import com.udea.rutaudea.ui.screen.common.textoApoyoDe

/**
 * Sesión de práctica: una pregunta a la vez con retroalimentación
 * inmediata (✓/✗ + explicación + subtema/dificultad + recomendación).
 * Sin cronómetro límite; el reloj es informativo.
 */
@Composable
fun PracticaSesionScreen(
    viewModel: PracticaSesionViewModel,
    onAbandonar: () -> Unit,
    onTerminada: () -> Unit
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    // Pausa al cambiar de app (solo background real, no rotación).
    AppBackgroundEffect(
        onBackground = { viewModel.onAppBackgrounded() },
        onForeground = { viewModel.onAppResumed() }
    )

    // El botón atrás abre la pausa en vez de salir sin confirmar.
    BackHandler(enabled = state.questions.isNotEmpty()) {
        viewModel.onPauseRequested()
    }

    LaunchedEffect(Unit) {
        viewModel.evento.collect { evento ->
            when (evento) {
                PracticaSesionViewModel.SesionEvento.PracticaTerminada -> onTerminada()
            }
        }
    }

    val question: Question? = state.currentQuestion
    val isRevealed = state.isRevealed
    val isCorrect = state.isCorrect

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val errorColor = MaterialTheme.colorScheme.error
    val errorContainerColor = MaterialTheme.colorScheme.errorContainer
    val onPrimaryContainerColor = MaterialTheme.colorScheme.onPrimaryContainer

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Barra superior: progreso + reloj informativo + abandono
            BarraProgresoSesion(
                texto = "Pregunta ${state.currentIndex + 1} de ${state.questions.size}",
                progreso = if (state.questions.isNotEmpty())
                    (state.currentIndex + 1).toFloat() / state.questions.size else 0f,
                modifier = Modifier.padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⏱ ${formatTime(state.elapsedSeconds)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onAbandonar) {
                        Text("Abandonar", fontSize = 13.sp, color = errorColor)
                    }
                }
            }

            // Enunciado + chips de contexto
            if (question != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChipInfo(texto = question.subtema)
                            ChipInfo(texto = "Dificultad: ${question.dificultad}")
                        }
                        textoApoyoDe(question)?.let { textoApoyo ->
                            TextoApoyo(texto = textoApoyo)
                        }
                        Text(
                            text = question.pregunta,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Opciones con feedback inmediato
                OpcionesPreguntaCard(
                    opciones = question.opciones,
                    seleccionada = state.currentAnswer,
                    onSelect = viewModel::onOptionSelected,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    respuestaCorrecta = question.respuestaCorrecta,
                    revelada = isRevealed
                )

                // Feedback inmediato (plan §10)
                if (isRevealed) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCorrect) primaryContainerColor else errorContainerColor
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isCorrect) "✓ ¡Correcto!" else "✗ Incorrecto",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = onPrimaryContainerColor
                            )
                            if (!isCorrect) {
                                Text(
                                    text = "Tu respuesta: ${state.currentAnswer} · Correcta: ${question.respuestaCorrecta}",
                                    fontSize = 13.sp,
                                    color = onPrimaryContainerColor
                                )
                            }
                            val explicacion = question.explicacion.orEmpty()
                            if (explicacion.isNotBlank()) {
                                Text(
                                    text = "💡 $explicacion",
                                    fontSize = 13.sp,
                                    color = onPrimaryContainerColor
                                )
                            }
                            Text(
                                text = "📚 ${recomendacion(isCorrect, question.subtema)}",
                                fontSize = 12.sp,
                                color = onPrimaryContainerColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Botones de navegación
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = viewModel::onPrevious,
                    enabled = state.currentIndex > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("← Anterior")
                }
                Button(
                    onClick = viewModel::onNext,
                    enabled = state.isRevealed,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text(
                        text = if (state.isLastQuestion) "Ver resultado" else "Siguiente →",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!state.isRevealed) {
                TextButton(
                    onClick = viewModel::onTerminar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text("Terminar práctica ahora", fontSize = 13.sp)
                }
            }
        }

        if (state.showPauseOverlay) {
            PauseOverlay(
                titulo = "Práctica en pausa",
                mensaje = "Tu práctica está en pausa. ¿Qué deseas hacer?",
                onSalir = onAbandonar,
                onResumir = { viewModel.onResumeConfirmed() }
            )
        }
    }
}

@Composable
private fun ChipInfo(texto: String) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = texto,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Recomendación estática por subtema (coherente con PracticaResultadoViewModel). */
private fun recomendacion(esCorrecta: Boolean, subtema: String): String = if (esCorrecta)
    "Buen manejo de «$subtema». Sigue practicando para mantenerlo."
else
    "Refuerza «$subtema»: revisa la explicación y practica más preguntas de este tema."

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
