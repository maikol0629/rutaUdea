package com.udea.rutaudea.ui.screen.progreso

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udea.rutaudea.domain.model.ItemHistorial
import com.udea.rutaudea.domain.model.SubtemaEstadistica
import com.udea.rutaudea.domain.model.TipoActividad

/**
 * Dashboard de progreso (plan, sección 12): resumen, evolución de
 * simulacros, acierto por subtema (fortalezas y áreas de mejora),
 * recomendaciones e historial unificado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgresoScreen(
    viewModel: ProgresoViewModel,
    onSimulacroClick: () -> Unit,
    onPracticaClick: () -> Unit
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    // Refresco silencioso al volver a la pantalla (no en la primera entrada,
    // que ya la cubre el `init` del ViewModel).
    var primeraReanudacion by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        if (primeraReanudacion) {
            primeraReanudacion = false
        } else {
            viewModel.refrescar()
        }
        onPauseOrDispose { }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refrescar() },
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.sinDatos && !state.isLoading) {
            ProgresoVacio(onSimulacroClick, onPracticaClick)
            return@PullToRefreshBox
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // Resumen global
        item {
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
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "📈 Tu progreso",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (state.isLoading) {
                        Text(
                            text = "Cargando tu actividad…",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Metrica("Simulacros", "${state.simulacros.size}")
                            Metrica("Prácticas", "${state.practicas.size}")
                            Metrica("Promedio", "${state.promedioGlobal}%")
                            Metrica("Mejor", "${state.mejorPuntaje}%")
                        }
                    }
                }
            }
        }

        // Evolución de simulacros
        if (state.simulacros.isNotEmpty()) {
            item {
                Seccion(titulo = "Evolución de simulacros") {
                    if (state.evolucion.size >= 2) {
                        LineaEvolucion(
                            porcentajes = state.evolucion,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                        Text(
                            text = "Del más antiguo (${state.evolucion.first()}%) al más reciente (${state.evolucion.last()}%)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Realiza al menos 2 simulacros para ver tu evolución.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Áreas de mejora y fortalezas
        if (state.areasDeMejora.isNotEmpty()) {
            item {
                Seccion(titulo = "🎯 Áreas de mejora") {
                    state.areasDeMejora.forEach { FilaSubtema(it) }
                }
            }
        }

        if (state.fortalezas.isNotEmpty()) {
            item {
                Seccion(titulo = "💪 Fortalezas") {
                    state.fortalezas.forEach { FilaSubtema(it) }
                }
            }
        }

        // Recomendaciones (plan §12)
        if (state.recomendacionesTexto.isNotEmpty()) {
            item {
                Seccion(titulo = "📚 Recomendaciones") {
                    state.recomendacionesTexto.forEach { texto ->
                        Text(
                            text = "• $texto",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(onClick = onPracticaClick) {
                        Text("Ir a práctica")
                    }
                }
            }
        }

        // Historial unificado
        item {
            Seccion(titulo = "🗂 Historial") {
                Text(
                    text = "Últimas ${state.historial.size} actividades",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(state.historial, key = { "${it.tipo}-${it.id}" }) { item ->
            FilaHistorial(item, modifier = Modifier.padding(horizontal = 16.dp))
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ---------- componentes ----------

@Composable
private fun Metrica(titulo: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = valor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = titulo,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun Seccion(
    titulo: String,
    contenido: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = titulo,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                contenido()
            }
        }
    }
}

@Composable
private fun FilaSubtema(stat: SubtemaEstadistica) {
    val color = if (stat.porcentaje >= 60)
        MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.error

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stat.subtema,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${stat.porcentaje}%  (${stat.correctas}/${stat.respondidas})",
                fontSize = 13.sp,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = stat.porcentaje / 100f,
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FilaHistorial(item: ItemHistorial, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = if (item.tipo == TipoActividad.SIMULACRO) "🧪" else "📝",
                    fontSize = 20.sp
                )
                Column {
                    Text(
                        text = item.titulo,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tiempo: ${formatTiempo(item.tiempoUsedMs)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.score}/${item.total}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${item.porcentaje}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Gráfica de línea simple (Canvas) de los porcentajes por simulacro. */
@Composable
private fun LineaEvolucion(
    porcentajes: List<Int>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (porcentajes.isEmpty()) return@Canvas

        val paddingH = 16f
        val paddingV = 16f
        val anchoUtil = size.width - paddingH * 2
        val altoUtil = size.height - paddingV * 2

        fun x(i: Int): Float = paddingH + anchoUtil * i / (porcentajes.size - 1).coerceAtLeast(1)
        fun y(pct: Int): Float = paddingV + altoUtil * (1 - pct / 100f)

        // Línea base (0%)
        drawLine(
            color = color.copy(alpha = 0.15f),
            start = Offset(paddingH, y(0)),
            end = Offset(size.width - paddingH, y(0)),
            strokeWidth = 2f
        )

        val path = Path()
        porcentajes.forEachIndexed { i, pct ->
            val punto = Offset(x(i), y(pct))
            if (i == 0) path.moveTo(punto.x, punto.y) else path.lineTo(punto.x, punto.y)
        }
        drawPath(path, color = color, style = Stroke(width = 4f))

        porcentajes.forEachIndexed { i, pct ->
            drawCircle(
                color = color,
                radius = 6f,
                center = Offset(x(i), y(pct))
            )
        }
    }
}

@Composable
private fun ProgresoVacio(
    onSimulacroClick: () -> Unit,
    onPracticaClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "📈", fontSize = 48.sp)
                Text(
                    text = "Aún no tienes actividad",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Haz un simulacro o practica por tema y aquí verás tu evolución, fortalezas y recomendaciones.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onSimulacroClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Iniciar simulacro", fontWeight = FontWeight.Medium)
                }
                OutlinedButton(onClick = onPracticaClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Ir a práctica", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

private fun formatTiempo(ms: Long): String {
    val totalSegundos = (ms / 1000).toInt()
    val horas = totalSegundos / 3600
    val mins = (totalSegundos % 3600) / 60
    val segs = totalSegundos % 60
    return if (horas > 0) String.format("%d:%02d:%02d", horas, mins, segs)
    else String.format("%02d:%02d", mins, segs)
}
