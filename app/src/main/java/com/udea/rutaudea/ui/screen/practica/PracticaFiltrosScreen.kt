package com.udea.rutaudea.ui.screen.practica

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Pantalla de filtros del módulo de Práctica:
 * área → subtema → cantidad. La dificultad NO se filtra: la selección
 * se estratifica proporcionalmente al banco (PracticeSelector).
 */
@Composable
fun PracticaFiltrosScreen(
    viewModel: PracticaFiltrosViewModel,
    onIniciar: () -> Unit
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(Unit) {
        viewModel.evento.collect { evento ->
            when (evento) {
                PracticaFiltrosViewModel.FiltrosEvento.PracticaLista -> onIniciar()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "📝 Práctica dirigida",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Elige área, subtema y cantidad. Recibirás feedback " +
                        "inmediato con explicación en cada pregunta.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        if (state.isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            // Área
            SeccionFiltros(titulo = "Área") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.area == PracticaFiltrosViewModel.AREA_RL,
                        onClick = { viewModel.onAreaChange(PracticaFiltrosViewModel.AREA_RL) },
                        label = { Text("Razonamiento Lógico") }
                    )
                    FilterChip(
                        selected = state.area == PracticaFiltrosViewModel.AREA_CL,
                        onClick = { viewModel.onAreaChange(PracticaFiltrosViewModel.AREA_CL) },
                        label = { Text("Competencia Lectora") }
                    )
                }
            }

            // Subtema
            SeccionFiltros(titulo = "Subtema") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.subtema == null,
                        onClick = { viewModel.onSubtemaChange(null) },
                        label = { Text("Todos los subtemas") }
                    )
                    state.subtemas.forEach { subtema ->
                        FilterChip(
                            selected = state.subtema == subtema,
                            onClick = { viewModel.onSubtemaChange(subtema) },
                            label = { Text(subtema) }
                        )
                    }
                }
            }

            // Cantidad
            SeccionFiltros(titulo = "Cantidad de preguntas") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 20).forEach { cantidad ->
                        FilterChip(
                            selected = state.cantidad == cantidad,
                            onClick = { viewModel.onCantidadChange(cantidad) },
                            enabled = state.disponibles > 0,
                            label = { Text("$cantidad") }
                        )
                    }
                }
                Text(
                    text = if (state.disponibles > 0)
                        "Disponibles con estos filtros: ${state.disponibles}" +
                            if (state.cantidadEfectiva < state.cantidad) {
                                " · se practicarán ${state.cantidadEfectiva}"
                            } else ""
                    else "No hay preguntas disponibles con estos filtros",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            // Botón iniciar
            Button(
                onClick = viewModel::iniciarPractica,
                enabled = state.puedeIniciar,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                if (state.isStarting) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Iniciar práctica (${state.cantidadEfectiva})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SeccionFiltros(
    titulo: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = titulo,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}
