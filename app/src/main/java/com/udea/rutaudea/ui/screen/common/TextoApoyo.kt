package com.udea.rutaudea.ui.screen.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sección de texto de apoyo de una pregunta (texto_base / contexto).
 *
 * Los textos de Competencia Lectora superan los 2.000 caracteres, así que
 * la sección tiene altura acotada y scroll interno para no desbordar la
 * pantalla ni empujar las opciones fuera del área visible.
 */
@Composable
fun TextoApoyoCard(
    texto: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

/** Texto de apoyo efectivo de una pregunta (texto_base o contexto), o null. */
fun textoApoyoDe(question: com.udea.rutaudea.domain.model.Question): String? {
    val texto = question.textoBase?.takeIf { it.isNotBlank() }
        ?: question.contexto?.takeIf { it.isNotBlank() }
    return texto
}
