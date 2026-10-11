package com.udea.rutaudea.ui.screen.practica

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udea.rutaudea.ui.screen.common.EstadoRespuesta
import com.udea.rutaudea.ui.screen.common.RepasoPreguntaCard
import com.udea.rutaudea.ui.screen.common.ResumenScoreCard
import com.udea.rutaudea.ui.theme.RutaUdeaTheme

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

    PracticaResultadoContent(
        score = score,
        total = total,
        timeUsedMs = timeUsedMs,
        results = results,
        filtrosLabel = viewModel.filtrosLabel,
        onRepetir = onRepetir,
        onVolverInicio = onVolverInicio
    )
}

/**
 * Contenido puro (sin ViewModel). Un único `LazyColumn` para que en
 * horizontal el header no deje sin espacio al repaso por pregunta (antes
 * la lista usaba `weight(1f, fill = false)` y colapsaba a 0).
 */
@Composable
private fun PracticaResultadoContent(
    score: Int,
    total: Int,
    timeUsedMs: Long,
    results: List<PracticaResultadoViewModel.PracticaResult>,
    filtrosLabel: String,
    onRepetir: () -> Unit,
    onVolverInicio: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ResumenScoreCard(
                titulo = "Resultado de la práctica",
                score = score,
                total = total,
                timeUsedMs = timeUsedMs,
                subtitulo = filtrosLabel
            )
        }

        // Repaso por pregunta (título)
        item {
            Text(
                text = "Repaso por pregunta",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        items(results.size) { index ->
            PracticaResultItem(results[index])
        }

        // Botones - al final del scroll, siempre alcanzables
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

private val previewResults = listOf(
    PracticaResultadoViewModel.PracticaResult(
        index = 0,
        subtema = "RL07 Sucesiones",
        dificultad = "media",
        esCorrecta = true,
        respondida = true,
        respuestaUsuario = "C",
        respuestaCorrecta = "C",
        enunciado = "Si la secuencia es 2, 4, 8, 16, ¿cuál es el siguiente término?",
        textoApoyo = null,
        explicacion = "Cada término se obtiene duplicando el anterior.",
        recomendacion = "Buen manejo de «RL07 Sucesiones». Sigue practicando."
    ),
    PracticaResultadoViewModel.PracticaResult(
        index = 1,
        subtema = "CL02 Idea principal",
        dificultad = "alta",
        esCorrecta = false,
        respondida = true,
        respuestaUsuario = "A",
        respuestaCorrecta = "B",
        enunciado = "¿Cuál es la idea principal del texto?",
        textoApoyo = "El autor sostiene que la lectura crítica exige contrastar fuentes.",
        explicacion = "La idea principal aparece en el segundo párrafo.",
        recomendacion = "Refuerza «CL02 Idea principal»: revisa la explicación."
    )
)

@Preview(showBackground = true, name = "Resultado práctica · vertical")
@Preview(showBackground = true, widthDp = 800, heightDp = 360, name = "Resultado práctica · horizontal")
@Composable
private fun PracticaResultadoContentPreview() {
    RutaUdeaTheme {
        PracticaResultadoContent(
            score = 1,
            total = 2,
            timeUsedMs = 15 * 60 * 1000L,
            results = previewResults,
            filtrosLabel = "Razonamiento Lógico · RL07 Sucesiones",
            onRepetir = {},
            onVolverInicio = {}
        )
    }
}
