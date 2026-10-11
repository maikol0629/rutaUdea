package com.udea.rutaudea.ui.screen.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Lista de opciones de una pregunta (A, B, C, D) con estado de selección y,
 * opcionalmente, el resultado revelado (✓ correcta / ✗ incorrecta).
 *
 * Compartida por la sesión de Simulacro (sin revelar) y la de Práctica
 * (feedback inmediato) para no duplicar el renderizado de las opciones.
 *
 * @param respuestaCorrecta letra correcta; solo se usa si [revelada] es `true`.
 * @param revelada si es `true` se colorean la opción correcta y, si falló,
 *   la elegida por el usuario.
 */
@Composable
fun OpcionesPreguntaCard(
    opciones: List<String>,
    seleccionada: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    respuestaCorrecta: String? = null,
    revelada: Boolean = false
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val surface = MaterialTheme.colorScheme.surface
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val onErrorContainer = MaterialTheme.colorScheme.onErrorContainer
    val onSurface = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            opciones.forEachIndexed { index, option ->
                val letter = ('A' + index).toString()
                val isSelected = seleccionada == letter
                val esLaCorrecta = letter == respuestaCorrecta

                val (container, contentColor) = when {
                    revelada && esLaCorrecta -> primaryContainer to onPrimaryContainer
                    revelada && isSelected -> errorContainer to onErrorContainer
                    !revelada && isSelected -> primaryContainer to onPrimaryContainer
                    else -> surface to onSurface
                }
                val destacada = isSelected || (revelada && esLaCorrecta)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (destacada) 3.dp else 1.dp
                    ),
                    colors = CardDefaults.cardColors(containerColor = container),
                    onClick = { onSelect(letter) }
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
                            color = contentColor
                        )
                        when {
                            revelada && esLaCorrecta -> Text(
                                text = "✓",
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            revelada && isSelected -> Text(
                                text = "✗",
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            isSelected -> Text(
                                text = "✓",
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
