package com.udea.rutaudea.ui.screen.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sección de texto de apoyo de una pregunta (texto_base / contexto).
 *
 * Sin tarjeta ni recuadro anidado (una Card dentro de otra Card añadía
 * ~32dp de padding doble y restaba legibilidad): solo una etiqueta sutil y
 * el texto. Los textos de Competencia Lectora superan los 2.000 caracteres,
 * así que el texto tiene altura acotada con scroll interno para no empujar
 * las opciones fuera del área visible.
 */
@Composable
fun TextoApoyo(
    texto: String,
    modifier: Modifier = Modifier,
    /** Altura máxima del texto scrolleable (compacto en listas de repaso). */
    alturaMaxima: Dp = 260.dp,
    /** Tamaño de la fuente del texto (compacto en listas de repaso). */
    tamanoFuente: TextUnit = 16.sp
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Texto de apoyo",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = texto,
            fontSize = tamanoFuente,
            lineHeight = tamanoFuente * 1.5f,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = alturaMaxima)
                .verticalScroll(rememberScrollState())
        )
    }
}

/** Texto de apoyo efectivo de una pregunta (texto_base o contexto), o null. */
fun textoApoyoDe(question: com.udea.rutaudea.domain.model.Question): String? {
    val texto = question.textoBase?.takeIf { it.isNotBlank() }
        ?: question.contexto?.takeIf { it.isNotBlank() }
    return texto
}
