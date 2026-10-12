package com.udea.rutaudea.ui.screen.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udea.rutaudea.R
import com.udea.rutaudea.domain.model.Racha
import com.udea.rutaudea.domain.services.Saludo
import com.udea.rutaudea.ui.theme.RutaUdeaTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSimulacroClick: () -> Unit,
    onPracticaClick: () -> Unit,
    onProgresoClick: () -> Unit,
    onInfoClick: () -> Unit,
    onPerfilClick: () -> Unit
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    // Refresca la racha al volver al Home (por ejemplo, tras completar un
    // simulacro o una práctica). El guard "primera vez" vive en el
    // ViewModel porque el `remember` se pierde al salir de la composición.
    LifecycleResumeEffect(Unit) {
        viewModel.alReanudar()
        onPauseOrDispose { }
    }

    HomeContenido(
        saludo = state.saludo,
        racha = state.racha,
        onSimulacroClick = onSimulacroClick,
        onPracticaClick = onPracticaClick,
        onProgresoClick = onProgresoClick,
        onInfoClick = onInfoClick,
        onPerfilClick = onPerfilClick
    )
}

@Composable
private fun HomeContenido(
    saludo: Saludo.Encabezado,
    racha: Racha,
    onSimulacroClick: () -> Unit,
    onPracticaClick: () -> Unit,
    onProgresoClick: () -> Unit,
    onInfoClick: () -> Unit,
    onPerfilClick: () -> Unit
) {
    // Un único contenedor scrolleable: en horizontal (poca altura) el header y
    // la card de Simulacro ya no empujan a las demás secciones fuera de pantalla.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado: marca con logo, saludo personalizado y propuesta de valor.
        item {
            EncabezadoHome(
                saludo = saludo,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // Racha de actividad
        item {
            RachaHomeCard(
                racha = racha,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // Acción principal: simulacro completo.
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Examen real · 120 min",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Simulacro completo",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) {
                            Box(
                                modifier = Modifier.size(56.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🧪", fontSize = 28.sp)
                            }
                        }
                    }
                    Text(
                        text = "80 preguntas · 40 de razonamiento lógico + 40 de competencia lectora",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Button(
                        onClick = onSimulacroClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Iniciar simulacro", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Acceso al resto de secciones.
        item {
            Text(
                text = "Explora la app",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        items(listOf(
            SectionItem(TipoSeccion.PRACTICA, "Práctica", "Preguntas por área y subtema, con explicación inmediata", "📝") {
                onPracticaClick()
            },
            SectionItem(TipoSeccion.PROGRESO, "Progreso", "Tu evolución, fortalezas y temas a reforzar", "📈") {
                onProgresoClick()
            },
            SectionItem(TipoSeccion.INFO, "Info", "Estrategias y errores comunes por componente", "📚") {
                onInfoClick()
            },
            SectionItem(TipoSeccion.PERFIL, "Perfil", "Tu cuenta y configuración", "👤") {
                onPerfilClick()
            }
        )) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                onClick = item.onClick
            ) {
                ListItem(
                    headlineContent = { Text(item.title, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(item.description, fontSize = 14.sp) },
                    leadingContent = { IconoSeccion(item) },
                    trailingContent = {
                        Text(
                            text = "▶",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun EncabezadoHome(saludo: Saludo.Encabezado, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Barra de marca: título a un lado y logosímbolo al opuesto.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("Ruta ")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("UdeA")
                        }
                    },
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Examen de admisión",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Image(
                painter = painterResource(R.drawable.logosimbolo_vertical),
                contentDescription = "Logosímbolo de la Universidad de Antioquia",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(64.dp)
            )
        }

        // Saludo hero: momento del día y nombre en grande.
        if (saludo.momento.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (saludo.nombre.isNotBlank()) {
                    Text(
                        text = "${saludo.momento},",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = saludo.nombre,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = saludo.momento,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Text(
            text = "Simulacros completos, práctica por tema y seguimiento " +
                "de tu progreso para llegar listo al examen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IconoSeccion(item: SectionItem) {
    Surface(
        color = colorInsignia(item.tipo),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = item.icon, fontSize = 24.sp)
        }
    }
}

/** Color tonal de la insignia según la sección (resuelto en contexto @Composable). */
@Composable
private fun colorInsignia(tipo: TipoSeccion): Color = when (tipo) {
    TipoSeccion.PRACTICA -> MaterialTheme.colorScheme.secondaryContainer
    TipoSeccion.PROGRESO -> MaterialTheme.colorScheme.tertiaryContainer
    TipoSeccion.INFO -> MaterialTheme.colorScheme.primaryContainer
    TipoSeccion.PERFIL -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun RachaHomeCard(racha: Racha, modifier: Modifier = Modifier) {
    val activa = racha.actual > 0
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🔥", fontSize = 34.sp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Tu racha",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = if (activa) "${racha.actual} ${pluralDias(racha.actual)}" else "0 días",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Mejor",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = "${racha.mejor}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

private fun pluralDias(dias: Int): String = if (dias == 1) "día" else "días"

private enum class TipoSeccion { PRACTICA, PROGRESO, INFO, PERFIL }

private data class SectionItem(
    val tipo: TipoSeccion,
    val title: String,
    val description: String,
    val icon: String,
    val onClick: () -> Unit
)

@Preview(showBackground = true, name = "Home · vertical")
@Preview(showBackground = true, widthDp = 800, heightDp = 360, name = "Home · horizontal")
@Composable
private fun HomeScreenPreview() {
    RutaUdeaTheme {
        HomeContenido(
            saludo = Saludo.Encabezado(momento = "Buenas tardes", nombre = "Maikol"),
            racha = Racha(actual = 4, mejor = 9),
            onSimulacroClick = {},
            onPracticaClick = {},
            onProgresoClick = {},
            onInfoClick = {},
            onPerfilClick = {}
        )
    }
}
