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
    revelada: Boolean = false,
    /**
     * D-06: las opciones reales están dentro de la figura; los botones
     * solo muestran la letra (o el texto si es una opción de texto real,
     * como los falsos positivos T4-P10/T4-P11).
     */
    opcionesEnImagen: Boolean = false
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
                            text = textoBoton(letter, option, opcionesEnImagen),
                            fontSize = if (opcionesEnImagen && esPlaceholder(option, letter)) 22.sp else 16.sp,
                            fontWeight = if (opcionesEnImagen && esPlaceholder(option, letter))
                                FontWeight.Bold else FontWeight.Normal,
                            color = contentColor
                        )
                        val indicador: String? = when {
                            revelada && esLaCorrecta -> "✓"
                            revelada && isSelected -> "✗"
                            isSelected -> "✓"
                            else -> null
                        }
                        if (indicador != null) {
                            Text(
                                text = indicador,
                                fontSize = 20.sp,
                                color = if (indicador == "✗")
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Texto del botón de una opción. Con `opciones_en_imagen` se oculta el
 * texto de relleno ("A (ver opción en la imagen)") y queda solo la letra;
 * si la opción es texto real (p. ej. una fórmula) se conserva.
 */
private fun textoBoton(letra: String, opcion: String, opcionesEnImagen: Boolean): String {
    if (!opcionesEnImagen) return "$letra) $opcion"
    return if (esPlaceholder(opcion, letra)) letra else "$letra) ${opcion.trim()}"
}

private fun esPlaceholder(opcion: String, letra: String): Boolean {
    val limpio = opcion.trim()
    if (limpio.isEmpty() || limpio.equals(letra, ignoreCase = true)) return true
    val minus = limpio.lowercase()
    return minus.contains("ver opci") ||
        (minus.startsWith("${letra.lowercase()} (") && minus.contains("imagen"))
}
