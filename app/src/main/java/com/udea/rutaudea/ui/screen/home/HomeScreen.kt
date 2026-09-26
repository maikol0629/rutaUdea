package com.udea.rutaudea.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onSimulacroClick: () -> Unit,
    onPracticaClick: () -> Unit,
    onProgresoClick: () -> Unit,
    onInfoClick: () -> Unit,
    onPerfilClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "RutaUdeA",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Tu camino a la UdeA",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Main Action - Simulacro MVP
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    Column {
                        Text(
                            text = "Simulacro MVP",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "5 min • 6 preguntas • 3 RL + 3 CL",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
Text(
                        text = "▶",
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Button(
                    onClick = onSimulacroClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Iniciar Simulacro", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Quick access to other sections
        Text(
            text = "Otras secciones",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
items(listOf(
                SectionItem("Práctica", "Práctica filtrada por tema y dificultad", "📝") { onPracticaClick() },
                SectionItem("Progreso", "Historial y evolución de tus simulacros", "📈") { onProgresoClick() },
                SectionItem("Info", "Contenido educativo por componente", "📚") { onInfoClick() },
                SectionItem("Perfil", "Tu cuenta y configuración", "👤") { onPerfilClick() }
            )) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    onClick = item.onClick
                ) {
                    ListItem(
                        headlineContent = { Text(item.title, fontWeight = FontWeight.Medium) },
                        supportingContent = { Text(item.description, fontSize = 14.sp) },
                        leadingContent = { Text(text = item.icon, fontSize = 24.sp) },
                        trailingContent = { Text(text = "▶", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private data class SectionItem(
    val title: String,
    val description: String,
    val icon: String,
    val onClick: () -> Unit
)