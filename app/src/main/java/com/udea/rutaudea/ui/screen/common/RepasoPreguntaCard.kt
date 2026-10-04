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

/** Estado de la pregunta en el repaso (define color de la card y la marca). */
enum class EstadoRespuesta { CORRECTA, INCORRECTA, OMITIDA }

/**
 * Card de repaso por pregunta, compartida por el resultado del simulacro
 * y el de la práctica.
 *
 * Estructura plana: la Card tintada según el estado contiene directamente
 * el texto (encabezado, enunciado, texto de apoyo compacto, corrección,
 * explicación y recomendación). Sin recuadros ni tarjetas anidadas que
 * dupliquen márgenes.
 */
@Composable
fun RepasoPreguntaCard(
    encabezado: String,
    estado: EstadoRespuesta,
    modifier: Modifier = Modifier,
    enunciado: String = "",
    textoApoyo: String? = null,
    correccion: String? = null,
    explicacion: String = "",
    recomendacion: String? = null
) {
    val (tinte, marca, colorMarca) = when (estado) {
        EstadoRespuesta.CORRECTA -> Triple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            "✓",
            MaterialTheme.colorScheme.primary
        )
        EstadoRespuesta.INCORRECTA -> Triple(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            "✗",
            MaterialTheme.colorScheme.error
        )
        EstadoRespuesta.OMITIDA -> Triple(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            "—",
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = tinte)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = encabezado,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = marca,
                    fontSize = 18.sp,
                    color = colorMarca,
                    fontWeight = FontWeight.Bold
                )
            }

            if (enunciado.isNotBlank()) {
                Text(
                    text = enunciado,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Texto de apoyo en versión compacta (listas largas de repaso).
            textoApoyo?.let {
                TextoApoyo(texto = it, alturaMaxima = 120.dp, tamanoFuente = 14.sp)
            }

            correccion?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (explicacion.isNotBlank()) {
                Text(
                    text = "💡 $explicacion",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            recomendacion?.let {
                Text(
                    text = "📚 $it",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
