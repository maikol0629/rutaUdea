package com.udea.rutaudea.ui.screen.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.udea.rutaudea.domain.model.ImagenPregunta

/**
 * Figuras asociadas a una pregunta (D-05).
 *
 * Renderiza la lista completa: imagen de contexto y de pregunta, en orden,
 * con su texto alternativo y una vista ampliada al tocar. No dibuja nada
 * cuando no hay URLs publicadas (pregunta solo-texto).
 *
 * @param destacada resalta el borde cuando `opciones_en_imagen = true`
 *   (D-06: imagen visible + botones A–D), para que el estudiante sepa que
 *   las opciones están dentro de la figura.
 */
@Composable
fun FiguraPregunta(
    imagenes: List<ImagenPregunta>,
    modifier: Modifier = Modifier,
    destacada: Boolean = false
) {
    val visibles = imagenes.filter { it.esVisible() }
    if (visibles.isEmpty()) return

    var ampliada by remember { mutableStateOf<ImagenPregunta?>(null) }
    val borde = if (destacada) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (destacada) {
            Text(
                text = "Las opciones A–D están en la imagen. Toca la figura para ampliarla.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        visibles.forEach { imagen ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = borde,
                    onClick = { ampliada = imagen }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imagen.url)
                            .crossfade(true)
                            .build(),
                        contentDescription = descripcion(imagen),
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                    )
                }
                Text(
                    text = when (imagen.rol) {
                        ImagenPregunta.ROL_CONTEXTO -> "Figura del contexto · toca para ampliar"
                        else -> "Figura de la pregunta · toca para ampliar"
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    ampliada?.let { imagen ->
        Dialog(
            onDismissRequest = { ampliada = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { ampliada = null }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imagen.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = descripcion(imagen),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/** Compatibilidad con llamadas que solo tienen la URL principal. */
@Composable
fun FiguraPregunta(
    imagenUrl: String?,
    modifier: Modifier = Modifier
) {
    if (imagenUrl.isNullOrBlank()) return
    FiguraPregunta(
        imagenes = listOf(ImagenPregunta(url = imagenUrl, orden = 1)),
        modifier = modifier
    )
}

private fun descripcion(imagen: ImagenPregunta): String =
    imagen.alt?.takeIf { it.isNotBlank() }
        ?: if (imagen.rol == ImagenPregunta.ROL_CONTEXTO) {
            "Figura del contexto de la pregunta"
        } else {
            "Figura de la pregunta"
        }
