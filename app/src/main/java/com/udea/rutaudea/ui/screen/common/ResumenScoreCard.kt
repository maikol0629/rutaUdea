package com.udea.rutaudea.ui.screen.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Cabecera de resultado: título (y subtítulo opcional), aciertos, anillo de
 * porcentaje y tiempo usado. Compartida por el resultado de Simulacro y el
 * de Práctica.
 */
@Composable
fun ResumenScoreCard(
    titulo: String,
    score: Int,
    total: Int,
    timeUsedMs: Long,
    modifier: Modifier = Modifier,
    subtitulo: String? = null
) {
    val percentage = if (total > 0) (score * 100 / total) else 0
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titulo,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = onContainer,
                textAlign = TextAlign.Center
            )
            if (subtitulo != null) {
                Text(
                    text = subtitulo,
                    fontSize = 14.sp,
                    color = onContainer.copy(alpha = 0.8f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$score / $total",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = onContainer
                    )
                    Text(
                        text = "correctas",
                        fontSize = 14.sp,
                        color = onContainer.copy(alpha = 0.8f)
                    )
                }
                Box(modifier = Modifier.size(80.dp)) {
                    CircularProgressIndicator(
                        progress = { percentage / 100f },
                        strokeWidth = 8.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = onContainer.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$percentage%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = onContainer
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatTime((timeUsedMs / 1000).toInt()),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = onContainer
                    )
                    Text(
                        text = "Tiempo usado",
                        fontSize = 12.sp,
                        color = onContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
