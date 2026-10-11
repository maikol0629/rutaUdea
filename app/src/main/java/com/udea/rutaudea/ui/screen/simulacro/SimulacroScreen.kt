package com.udea.rutaudea.ui.screen.simulacro

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udea.rutaudea.domain.model.Question
import com.udea.rutaudea.ui.screen.common.AppBackgroundEffect
import com.udea.rutaudea.ui.screen.common.PauseOverlay
import com.udea.rutaudea.ui.screen.common.TextoApoyo
import com.udea.rutaudea.ui.screen.common.textoApoyoDe
import com.udea.rutaudea.ui.theme.RutaUdeaTheme

@Composable
fun SimulacroScreen(
    viewModel: SimulacroViewModel,
    onAbandon: () -> Unit,
    onFinish: (() -> Unit)? = null
) {
    val uiState: SimulacroState = viewModel.uiState.collectAsStateWithLifecycle().value

    // Pausa al cambiar de app (solo background real, no rotación).
    AppBackgroundEffect(
        onBackground = { viewModel.onAppBackgrounded() },
        onForeground = { viewModel.onAppResumed() }
    )

    // El botón atrás abre la pausa en vez de salir sin confirmar.
    BackHandler(enabled = uiState.questions.isNotEmpty() && !uiState.isFinished) {
        viewModel.onPauseRequested()
    }

    // Navigate to RESULTADO when simulation finishes
    LaunchedEffect(uiState) {
        if (uiState.isFinished) {
            onFinish?.invoke()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SimulacroContent(
            state = uiState,
            currentQuestion = viewModel.currentQuestion,
            onOptionSelected = viewModel::onOptionSelected,
            onNextOrFinish = viewModel::onNextOrFinish
        )

        if (uiState.showResumeOverlay) {
            PauseOverlay(
                titulo = "Simulacro en pausa",
                mensaje = "Tu tiempo sigue corriendo: ${formatTime(uiState.timeRemainingSeconds)} restantes.\n¿Qué deseas hacer?",
                onSalir = onAbandon,
                onResumir = { viewModel.onResumeConfirmed() }
            )
        }
    }
}

/**
 * Contenido puro (sin ViewModel) para poder previsualizar y validar la
 * orientación. Igual que la Práctica, todo el contenido vive en un único
 * `Column` scrolleable, con el enunciado y las opciones en tarjetas
 * separadas: así en horizontal (poca altura) la pregunta sigue siendo
 * legible y navegable en vez de quedar comprimida en una tarjeta con peso.
 */
@Composable
private fun SimulacroContent(
    state: SimulacroState,
    currentQuestion: Question?,
    onOptionSelected: (String) -> Unit,
    onNextOrFinish: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onPrimaryContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
    val errorColor = MaterialTheme.colorScheme.error

    val questionIndex = state.currentIndex
    val totalQuestions = state.questions.size
    val selectedOption = state.currentAnswer
    val timeRemainingSeconds = state.timeRemainingSeconds
    val isLastQuestion = state.isLastQuestion

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Barra superior: progreso + cronómetro
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pregunta ${questionIndex + 1} de $totalQuestions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatTime(timeRemainingSeconds),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (timeRemainingSeconds <= 60) errorColor else primaryColor
                    )
                }
                LinearProgressIndicator(
                    progress = {
                        if (totalQuestions > 0) (questionIndex + 1).toFloat() / totalQuestions else 0f
                    },
                    color = primaryColor,
                    trackColor = primaryContainerColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        currentQuestion?.let { question ->
            // Enunciado + texto de apoyo
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

            // Opciones
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    question.opciones.forEachIndexed { index, option ->
                        val letter = ('A' + index).toString()
                        val isSelected = selectedOption == letter
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isSelected) 4.dp else 1.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) primaryContainerColor else surfaceColor
                            ),
                            onClick = { onOptionSelected(letter) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$letter) $option",
                                    fontSize = 16.sp,
                                    color = if (isSelected) onPrimaryContainerColor
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        fontSize = 20.sp,
                                        color = primaryColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Botón de avance/finalizar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Button(
                onClick = onNextOrFinish,
                enabled = selectedOption != null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = if (isLastQuestion) "Finalizar" else "Siguiente →",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

private val previewQuestion = Question(
    id = "q1",
    area = "razonamiento_logico",
    subtema = "RL07 Sucesiones",
    dificultad = "media",
    pregunta = "Si la secuencia es 2, 4, 8, 16, ¿cuál es el siguiente término?",
    opciones = listOf("18", "24", "32", "64"),
    respuestaCorrecta = "C",
    explicacion = "Cada término se obtiene duplicando el anterior."
)

private fun previewState() = SimulacroState(
    questions = listOf(previewQuestion),
    currentIndex = 0,
    userAnswers = mapOf(0 to "C"),
    timeRemainingMs = 12 * 60 * 1000L
)

@Preview(showBackground = true, name = "Simulacro · vertical")
@Preview(showBackground = true, widthDp = 800, heightDp = 360, name = "Simulacro · horizontal")
@Composable
private fun SimulacroContentPreview() {
    RutaUdeaTheme {
        SimulacroContent(
            state = previewState(),
            currentQuestion = previewQuestion,
            onOptionSelected = {},
            onNextOrFinish = {}
        )
    }
}
