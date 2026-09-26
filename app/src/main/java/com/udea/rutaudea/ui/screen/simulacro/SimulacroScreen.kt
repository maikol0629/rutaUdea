package com.udea.rutaudea.ui.screen.simulacro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

@Composable
fun SimulacroScreen(
    viewModel: SimulacroViewModel,
    onAbandon: () -> Unit,
    onFinish: (() -> Unit)? = null
) {
    val uiState: SimulacroState = viewModel.uiState.collectAsStateWithLifecycle().value

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
    val questionText = viewModel.currentQuestion?.pregunta ?: ""
    val options = viewModel.currentQuestion?.opciones ?: emptyList()
    val selectedOption = uiState.currentAnswer
    val timeRemainingSeconds = uiState.timeRemainingSeconds
    val isLastQuestion = uiState.isLastQuestion
    val showResumeOverlay = uiState.showResumeOverlay

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
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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

        // Resume Overlay
        if (showResumeOverlay) {
            ResumeOverlayDialog(
                viewModel = viewModel,
                timeRemainingSeconds = timeRemainingSeconds,
                onContinue = { viewModel.onResumeConfirmed() },
                onAbandon = onAbandon
            )
        }
    }
}

@Composable
private fun ResumeOverlayDialog(
    viewModel: SimulacroViewModel,
    timeRemainingSeconds: Int,
    onContinue: () -> Unit,
    onAbandon: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⏸",
                    fontSize = 48.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Simulacro en pausa",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Tu tiempo se ha guardado: ${formatTime(timeRemainingSeconds)} restantes.\n¿Qué deseas hacer?",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAbandon() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Abandonar", fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = { viewModel.onResumeConfirmed() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Continuar", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}