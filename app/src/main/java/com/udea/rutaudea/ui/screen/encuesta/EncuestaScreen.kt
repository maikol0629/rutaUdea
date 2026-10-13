package com.udea.rutaudea.ui.screen.encuesta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.udea.rutaudea.domain.model.EncuestaSatisfaccion
import com.udea.rutaudea.domain.model.EncuestaSatisfaccion.Companion.LIKERT_EMOJIS
import com.udea.rutaudea.domain.model.EncuestaSatisfaccion.Companion.LIKERT_ETIQUETAS
import com.udea.rutaudea.ui.theme.RutaUdeaTheme

/** Total de campos obligatorios (para la barra de avance). */
private const val CAMPOS_TOTALES = 14

@Composable
fun EncuestaScreen(
    viewModel: EncuestaViewModel,
    onVolver: () -> Unit
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    EncuestaContenido(
        cargando = state.cargando,
        encuesta = state.encuesta,
        yaRespondio = state.yaRespondio,
        enviando = state.enviando,
        errores = state.errores,
        enviada = state.enviada,
        sincronizada = state.sincronizada,
        onActualizar = viewModel::actualizar,
        onEnviar = viewModel::enviar,
        onSeguirEditando = viewModel::seguirEditando,
        onVolver = onVolver
    )
}

@Composable
private fun EncuestaContenido(
    cargando: Boolean,
    encuesta: EncuestaSatisfaccion,
    yaRespondio: Boolean,
    enviando: Boolean,
    errores: List<String>,
    enviada: Boolean,
    sincronizada: Boolean,
    onActualizar: (EncuestaSatisfaccion) -> Unit,
    onEnviar: () -> Unit,
    onSeguirEditando: () -> Unit,
    onVolver: () -> Unit
) {
    // Tras enviar, el agradecimiento ocupa la pantalla y queda centrado
    // (vertical y horizontal), en vez de un item más de la lista.
    if (enviada && !cargando) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AgradecimientoCard(
                sincronizada = sincronizada,
                yaRespondio = yaRespondio,
                onSeguirEditando = onSeguirEditando,
                onVolver = onVolver,
                modifier = Modifier.padding(24.dp)
            )
        }
        return
    }
    // Raíz scrolleable única (igual que el resto de la app): en horizontal
    // todo el contenido sigue alcanzable sin solapamientos.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            EncabezadoEncuesta(
                avance = avance(encuesta),
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        when {
            cargando -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            else -> {
                item {
                    SeccionEncuesta(
                        icono = "👤",
                        titulo = "1 · Sobre ti",
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "Tipo de institución",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            EncuestaSatisfaccion.TIPOS_INSTITUCION.forEach { tipo ->
                                FilterChip(
                                    selected = encuesta.tipoInstitucion == tipo,
                                    onClick = {
                                        onActualizar(
                                            encuesta.copy(
                                                tipoInstitucion = tipo,
                                                grado = "",
                                                semestre = ""
                                            )
                                        )
                                    },
                                    label = { Text(tipo) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = encuesta.nombreInstitucion,
                            onValueChange = { onActualizar(encuesta.copy(nombreInstitucion = it)) },
                            label = { Text("Nombre del colegio o institución") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (encuesta.tipoInstitucion == EncuestaSatisfaccion.TIPO_COLEGIO) {
                            CampoDesplegable(
                                etiqueta = "Grado",
                                opciones = EncuestaSatisfaccion.GRADOS,
                                valor = encuesta.grado,
                                onCambio = { onActualizar(encuesta.copy(grado = it)) }
                            )
                        }
                        if (encuesta.tipoInstitucion == EncuestaSatisfaccion.TIPO_UNIVERSIDAD) {
                            CampoDesplegable(
                                etiqueta = "Semestre",
                                opciones = EncuestaSatisfaccion.SEMESTRES,
                                valor = encuesta.semestre,
                                onCambio = { onActualizar(encuesta.copy(semestre = it)) }
                            )
                        }
                        Text(
                            text = "¿Ya presentaste el examen de admisión real?",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = encuesta.presentoExamen == true,
                                onClick = { onActualizar(encuesta.copy(presentoExamen = true)) },
                                label = { Text("Sí") }
                            )
                            FilterChip(
                                selected = encuesta.presentoExamen == false,
                                onClick = { onActualizar(encuesta.copy(presentoExamen = false)) },
                                label = { Text("No") }
                            )
                        }
                    }
                }

                item {
                    SeccionEncuesta(
                        icono = "🧩",
                        titulo = "2 · Cada módulo",
                        subtitulo = "¿Qué tanto estás de acuerdo?",
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        PreguntaLikert(
                            texto = "El Simulacro se parece al examen real",
                            valor = encuesta.likertSimulacro,
                            onCambio = { onActualizar(encuesta.copy(likertSimulacro = it)) }
                        )
                        PreguntaLikert(
                            texto = "La Práctica me ayuda con sus explicaciones",
                            valor = encuesta.likertPractica,
                            onCambio = { onActualizar(encuesta.copy(likertPractica = it)) }
                        )
                        PreguntaLikert(
                            texto = "El Progreso me muestra cómo voy",
                            valor = encuesta.likertProgreso,
                            onCambio = { onActualizar(encuesta.copy(likertProgreso = it)) }
                        )
                        PreguntaLikert(
                            texto = "El contenido de Info es claro y útil",
                            valor = encuesta.likertInfo,
                            onCambio = { onActualizar(encuesta.copy(likertInfo = it)) }
                        )
                    }
                }

                item {
                    SeccionEncuesta(
                        icono = "❓",
                        titulo = "3 · Las preguntas",
                        subtitulo = "¿Qué tanto estás de acuerdo?",
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        PreguntaLikert(
                            texto = "Las preguntas de Razonamiento Lógico son claras",
                            valor = encuesta.claridadRL,
                            onCambio = { onActualizar(encuesta.copy(claridadRL = it)) }
                        )
                        PreguntaLikert(
                            texto = "Las preguntas de Competencia Lectora son claras",
                            valor = encuesta.claridadCL,
                            onCambio = { onActualizar(encuesta.copy(claridadCL = it)) }
                        )
                        PreguntaLikert(
                            texto = "El nivel de dificultad es el adecuado",
                            valor = encuesta.dificultadAdecuada,
                            onCambio = { onActualizar(encuesta.copy(dificultadAdecuada = it)) }
                        )
                    }
                }

                item {
                    SeccionEncuesta(
                        icono = "💬",
                        titulo = "4 · Tu opinión",
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        PreguntaLikert(
                            texto = "La app es fácil de usar",
                            valor = encuesta.facilidadUso,
                            onCambio = { onActualizar(encuesta.copy(facilidadUso = it)) }
                        )
                        Text(
                            text = "¿Qué tanto recomendarías RutaUdeA? (0–10)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        SelectorNps(
                            valor = encuesta.nps,
                            onCambio = { onActualizar(encuesta.copy(nps = it)) }
                        )
                        OutlinedTextField(
                            value = encuesta.gustoMas,
                            onValueChange = { onActualizar(encuesta.copy(gustoMas = it)) },
                            label = { Text("Lo que más te gustó (opcional)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = encuesta.mejorar,
                            onValueChange = { onActualizar(encuesta.copy(mejorar = it)) },
                            label = { Text("¿Qué mejorarías? (opcional)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (errores.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Revisa lo siguiente:",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                errores.forEach { error ->
                                    Text(
                                        text = "• $error",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onEnviar,
                            enabled = !enviando,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (enviando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (yaRespondio) "Actualizar respuestas" else "Enviar encuesta",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onVolver,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Volver")
                        }
                    }
                }
            }
        }
    }
}

/** Campos obligatorios respondidos (0–14) para la barra de avance. */
private fun avance(e: EncuestaSatisfaccion): Int = listOf(
    e.tipoInstitucion in EncuestaSatisfaccion.TIPOS_INSTITUCION,
    e.nombreInstitucion.isNotBlank(),
    e.tipoInstitucion != EncuestaSatisfaccion.TIPO_COLEGIO || e.grado in EncuestaSatisfaccion.GRADOS,
    e.tipoInstitucion != EncuestaSatisfaccion.TIPO_UNIVERSIDAD || e.semestre in EncuestaSatisfaccion.SEMESTRES,
    e.presentoExamen != null,
    e.likertSimulacro in 1..5,
    e.likertPractica in 1..5,
    e.likertProgreso in 1..5,
    e.likertInfo in 1..5,
    e.claridadRL in 1..5,
    e.claridadCL in 1..5,
    e.dificultadAdecuada in 1..5,
    e.facilidadUso in 1..5,
    e.nps in 0..10
).count { it }

@Composable
private fun EncabezadoEncuesta(avance: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎓", fontSize = 30.sp)
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Encuesta piloto",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Tu opinión nos ayuda a mejorar la app · 2 min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
                LinearProgressIndicator(
                    progress = { avance / CAMPOS_TOTALES.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "$avance de $CAMPOS_TOTALES pasos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SeccionEncuesta(
    icono: String,
    titulo: String,
    subtitulo: String? = null,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = icono, fontSize = 22.sp)
                    }
                }
                Column {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    subtitulo?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            contenido()
        }
    }
}

@Composable
private fun PreguntaLikert(
    texto: String,
    valor: Int,
    onCambio: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            (1..5).forEach { opcion ->
                val seleccionado = valor == opcion
                TextButton(
                    onClick = { onCambio(opcion) },
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Surface(
                        color = if (seleccionado) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = LIKERT_EMOJIS[opcion - 1], fontSize = 24.sp)
                        }
                    }
                }
            }
        }
        Text(
            text = if (valor in 1..5) LIKERT_ETIQUETAS[valor - 1] else "Toca una carita",
            style = MaterialTheme.typography.labelMedium,
            color = if (valor in 1..5) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoDesplegable(
    etiqueta: String,
    opciones: List<String>,
    valor: String,
    onCambio: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it }
    ) {
        OutlinedTextField(
            value = valor.ifEmpty { "Elige…" },
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = {
                        onCambio(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorNps(valor: Int, onCambio: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (0..10).forEach { numero ->
                val seleccionado = valor == numero
                TextButton(
                    onClick = { onCambio(numero) },
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Surface(
                        color = if (seleccionado) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$numero",
                                fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                                color = if (seleccionado) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Nada probable",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Muy probable",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AgradecimientoCard(
    sincronizada: Boolean,
    yaRespondio: Boolean,
    onSeguirEditando: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🎉", fontSize = 56.sp)
            Text(
                text = "¡Gracias por tu opinión!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = if (sincronizada) "Tus respuestas ya quedaron registradas ☁️"
                else "Guardadas en tu dispositivo ⏳ · se sincronizarán solas",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            if (yaRespondio) {
                TextButton(onClick = onSeguirEditando) {
                    Text("Actualizar mis respuestas")
                }
            }
            OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
                Text("Volver al inicio")
            }
        }
    }
}

@Preview(showBackground = true, name = "Encuesta · vertical")
@Preview(showBackground = true, widthDp = 800, heightDp = 360, name = "Encuesta · horizontal")
@Composable
private fun EncuestaScreenPreview() {
    RutaUdeaTheme {
        EncuestaContenido(
            cargando = false,
            encuesta = EncuestaSatisfaccion(
                tipoInstitucion = "Colegio",
                nombreInstitucion = "I. E. Ejemplo",
                grado = "11°",
                presentoExamen = false,
                likertSimulacro = 5,
                likertPractica = 4,
                claridadRL = 4,
                nps = 9
            ),
            yaRespondio = false,
            enviando = false,
            errores = emptyList(),
            enviada = false,
            sincronizada = true,
            onActualizar = {},
            onEnviar = {},
            onSeguirEditando = {},
            onVolver = {}
        )
    }
}
