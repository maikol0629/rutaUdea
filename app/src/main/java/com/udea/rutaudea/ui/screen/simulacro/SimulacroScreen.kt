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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.udea.rutaudea.ui.screen.common.AppBackgroundEffect
import com.udea.rutaudea.ui.screen.common.PauseOverlay
import com.udea.rutaudea.ui.screen.common.TextoApoyo
import com.udea.rutaudea.ui.screen.common.textoApoyoDe

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

    // Capture theme colors at composable level
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onPrimaryContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
    val errorColor = MaterialTheme.colorScheme.error

    // Navigate to RESULTADO when simulation finishes
    LaunchedEffect(uiState) {
        if (uiState.isFinished) {
            onFinish?.invoke()
        }
    }

    val questionIndex = uiState.currentIndex
    val totalQuestions = uiState.questions.size
    val currentQuestion = viewModel.currentQuestion
    val questionText = currentQuestion?.pregunta ?: ""
    val textoApoyo = currentQuestion?.let { textoApoyoDe(it) }
    val options = currentQuestion?.opciones ?: emptyList()
    val selectedOption = uiState.currentAnswer
    val timeRemainingSeconds = uiState.timeRemainingSeconds
    val isLastQuestion = uiState.isLastQuestion
    val showResumeOverlay = uiState.showResumeOverlay

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top bar with timer
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                    progress = (questionIndex + 1).toFloat() / totalQuestions,
                    color = primaryColor,
                    trackColor = primaryContainerColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Question content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Texto de apoyo (Competencia Lectora y textos compartidos)
                textoApoyo?.let { TextoApoyo(texto = it) }

                Text(
                    text = questionText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start
                )

                // Options
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    options.forEachIndexed { index, option ->
                        val letter = ('A' + index).toString()
                        val isSelected = selectedOption == letter
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isSelected) 4.dp else 1.dp
                            ),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    primaryContainerColor
                                    else surfaceColor
                            ),
                            onClick = { viewModel.onOptionSelected(letter) }
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
                                    color = if (isSelected)
                                        onPrimaryContainerColor
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

        // Bottom action button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Button(
                onClick = { viewModel.onNextOrFinish() },
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

    if (showResumeOverlay) {
        PauseOverlay(
            titulo = "Simulacro en pausa",
            mensaje = "Tu tiempo sigue corriendo: ${formatTime(timeRemainingSeconds)} restantes.\n¿Qué deseas hacer?",
            onSalir = onAbandon,
            onResumir = { viewModel.onResumeConfirmed() }
        )
    }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}