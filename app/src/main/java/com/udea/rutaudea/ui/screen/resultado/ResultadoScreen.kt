package com.udea.rutaudea.ui.screen.resultado

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udea.rutaudea.ui.screen.common.EstadoRespuesta
import com.udea.rutaudea.ui.screen.common.RepasoPreguntaCard
import com.udea.rutaudea.ui.theme.RutaUdeaTheme

@Composable
fun ResultadoScreen(
    viewModel: ResultadoViewModel,
    onFinish: () -> Unit
) {
    val score = viewModel.score.collectAsStateWithLifecycle().value
    val total = viewModel.total.collectAsStateWithLifecycle().value
    val timeUsedMs = viewModel.timeUsedMs.collectAsStateWithLifecycle().value
    val questionResults = viewModel.questionResults.collectAsStateWithLifecycle().value

    ResultadoContent(
        score = score,
        total = total,
        timeUsedMs = timeUsedMs,
        questionResults = questionResults,
        onFinish = onFinish
    )
}

/**
 * Contenido puro (sin ViewModel). Todo el resumen vive en un único
 * `LazyColumn` para que en horizontal (poca altura) el header no empuje al
 * detalle por pregunta fuera de pantalla: antes el detalle usaba
 * `weight(1f, fill = false)` y colapsaba a 0.
 */
@Composable
private fun ResultadoContent(
    score: Int,
    total: Int,
    timeUsedMs: Long,
    questionResults: List<QuestionResult>,
    onFinish: () -> Unit
) {
    val timeUsedSeconds = (timeUsedMs / 1000).toInt()
    val percentage = if (total > 0) (score * 100 / total) else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with score
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        text = "Resultado del Simulacro",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
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
                        Box(
                            modifier = Modifier.size(80.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { percentage / 100f },
                                strokeWidth = 8.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$percentage%",
                                    fontSize = 20.sp,
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
                                text = formatTime(timeUsedSeconds),
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
        }

        // Detail list header
        item {
            Text(
                text = "Detalle por pregunta",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        items(questionResults.size) { index ->
            QuestionResultItem(questionResults[index])
        }

        // Finish button - al final del scroll, siempre alcanzable
        item {
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Finalizar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun QuestionResultItem(result: QuestionResult) {
    // Componente compartido con el repaso de práctica: Card tintada por
    // estado y contenido plano (sin recuadros internos).
    val estado = when {
        result.isCorrect -> EstadoRespuesta.CORRECTA
        result.userAnswer == "—" -> EstadoRespuesta.OMITIDA
        else -> EstadoRespuesta.INCORRECTA
    }
    RepasoPreguntaCard(
        encabezado = "Pregunta ${result.index + 1} (${result.areaLabel})",
        estado = estado,
        enunciado = result.enunciado,
        textoApoyo = result.textoApoyo,
        correccion = if (estado != EstadoRespuesta.CORRECTA)
            "Tu respuesta: ${result.userAnswer} · Correcta: ${result.correctAnswer}"
        else null,
        explicacion = result.explanation
    )
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

private val previewResults = listOf(
    QuestionResult(
        index = 0,
        areaLabel = "Razonamiento Lógico",
        isCorrect = true,
        userAnswer = "C",
        correctAnswer = "C",
        explanation = "Cada término se obtiene duplicando el anterior.",
        enunciado = "Si la secuencia es 2, 4, 8, 16, ¿cuál es el siguiente término?",
        textoApoyo = null
    ),
    QuestionResult(
        index = 1,
        areaLabel = "Competencia Lectora",
        isCorrect = false,
        userAnswer = "A",
        correctAnswer = "B",
        explanation = "La idea principal aparece en el segundo párrafo.",
        enunciado = "¿Cuál es la idea principal del texto?",
        textoApoyo = "El autor sostiene que la lectura crítica exige contrastar fuentes."
    )
)

@Preview(showBackground = true, name = "Resultado simulacro · vertical")
@Preview(showBackground = true, widthDp = 800, heightDp = 360, name = "Resultado simulacro · horizontal")
@Composable
private fun ResultadoContentPreview() {
    RutaUdeaTheme {
        ResultadoContent(
            score = 1,
            total = 2,
            timeUsedMs = 42 * 60 * 1000L,
            questionResults = previewResults,
            onFinish = {}
        )
    }
}
