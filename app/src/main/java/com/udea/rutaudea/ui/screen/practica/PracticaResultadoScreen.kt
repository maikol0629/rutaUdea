package com.udea.rutaudea.ui.screen.practica

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udea.rutaudea.ui.screen.common.EstadoRespuesta
import com.udea.rutaudea.ui.screen.common.RepasoPreguntaCard

/**
 * Resumen de la sesión de práctica: score, tiempo, repaso por pregunta
 * con explicación y recomendación.
 */
@Composable
fun PracticaResultadoScreen(
    viewModel: PracticaResultadoViewModel,
    onRepetir: () -> Unit,
    onVolverInicio: () -> Unit
) {
    val score = viewModel.score.collectAsStateWithLifecycle().value
    val total = viewModel.total.collectAsStateWithLifecycle().value
    val timeUsedMs = viewModel.timeUsedMs.collectAsStateWithLifecycle().value
    val results = viewModel.results.collectAsStateWithLifecycle().value

    val percentage = if (total > 0) (score * 100 / total) else 0

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header con resumen
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Resultado de la práctica",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = viewModel.filtrosLabel,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$score / $total",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "correctas",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Box(modifier = Modifier.size(80.dp)) {
                        CircularProgressIndicator(
                            progress = percentage / 100f,
                            strokeWidth = 8.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$percentage%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = formatTime((timeUsedMs / 1000).toInt()),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tiempo usado",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Repaso por pregunta
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .weight(1f, fill = false),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Repaso por pregunta",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results.size) { index ->
                        PracticaResultItem(results[index])
                    }
                }
            }
        }

        // Botones
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRepetir,
                modifier = Modifier.weight(1f)
            ) {
                Text("Nueva práctica", fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = onVolverInicio,
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Volver al inicio", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun PracticaResultItem(result: PracticaResultadoViewModel.PracticaResult) {
    // Componente compartido con el resultado del simulacro: Card tintada por
    // estado y contenido plano (sin recuadros internos).
    val estado = when {
        result.esCorrecta -> EstadoRespuesta.CORRECTA
        result.respondida -> EstadoRespuesta.INCORRECTA
        else -> EstadoRespuesta.OMITIDA
    }
    RepasoPreguntaCard(
        encabezado = "Pregunta ${result.index + 1} · ${result.subtema} · ${result.dificultad}",
        estado = estado,
        enunciado = result.enunciado,
        textoApoyo = result.textoApoyo,
        correccion = if (result.respondida && !result.esCorrecta)
            "Tu respuesta: ${result.respuestaUsuario} · Correcta: ${result.respuestaCorrecta}"
        else null,
        explicacion = result.explicacion,
        recomendacion = result.recomendacion
    )
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
