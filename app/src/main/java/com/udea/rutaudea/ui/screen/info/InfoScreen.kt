package com.udea.rutaudea.ui.screen.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udea.rutaudea.domain.model.TemaEducativo

/**
 * Tab 📚 Info: contenido educativo por componente del examen
 * (plan §2: "Consultar información educativa sobre componentes
 * y subtemas"). Acordeón expandible con descripción, estrategia
 * y errores comunes por componente.
 */
@Composable
fun InfoScreen(
    viewModel: InfoViewModel
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Encabezado + filtro por área
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "📚 Información educativa",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Qué evalúa cada componente del examen y cómo atacarlo.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.area == TemaEducativo.AREA_RL,
                        onClick = { viewModel.onAreaChange(TemaEducativo.AREA_RL) },
                        label = { Text("Razonamiento Lógico") }
                    )
                    FilterChip(
                        selected = state.area == TemaEducativo.AREA_CL,
                        onClick = { viewModel.onAreaChange(TemaEducativo.AREA_CL) },
                        label = { Text("Competencia Lectora") }
                    )
                }
            }
        }

        if (state.isLoading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(state.visibles, key = { it.id }) { tema ->
                TemaCard(
                    tema = tema,
                    expandido = state.expandidoId == tema.id,
                    onClick = { viewModel.onToggleExpandido(tema.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TemaCard(
    tema: TemaEducativo,
    expandido: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
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
                    text = tema.nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (expandido) "▲" else "▼",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = tema.descripcion,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(visible = expandido) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Bloque(titulo = "🧠 Cómo resolverlo", texto = tema.estrategia)
                    Bloque(titulo = "⚠️ Errores comunes", texto = tema.erroresComunes)
                }
            }
        }
    }
}

@Composable
private fun Bloque(titulo: String, texto: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = titulo,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = texto,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
