package com.udea.rutaudea.ui.screen.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.udea.rutaudea.domain.model.MotivoReporte
import com.udea.rutaudea.domain.model.ReportePregunta

/**
 * Diálogo compartido para reportar una inconsistencia en una pregunta.
 *
 * Se usa tanto en el simulacro como en la práctica. No revela la respuesta
 * correcta ni pausa el cronómetro; solo recoge el motivo y un comentario
 * breve. El guardado local-first lo hace el ViewModel correspondiente.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportePreguntaDialog(
    preguntaId: String,
    enviando: Boolean,
    mensaje: String?,
    error: String?,
    onDismiss: () -> Unit,
    onEnviar: (MotivoReporte, String) -> Unit
) {
    if (mensaje != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Reporte de pregunta") },
            text = { Text(mensaje) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        )
        return
    }

    var motivoClave by rememberSaveable { mutableStateOf<String?>(null) }
    var comentario by rememberSaveable { mutableStateOf("") }
    val motivo = MotivoReporte.entries.firstOrNull { it.clave == motivoClave }
    val comentarioObligatorio = motivo == MotivoReporte.OTRA

    AlertDialog(
        onDismissRequest = { if (!enviando) onDismiss() },
        title = { Text("Reportar pregunta") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ID: $preguntaId",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "¿Qué encontraste?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MotivoReporte.entries.forEach { opcion ->
                        FilterChip(
                            selected = motivo == opcion,
                            onClick = { motivoClave = opcion.clave },
                            enabled = !enviando,
                            label = { Text(opcion.etiqueta, fontSize = 12.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = comentario,
                    onValueChange = { nuevo ->
                        if (nuevo.length <= ReportePregunta.MAX_COMENTARIO) {
                            comentario = nuevo
                        }
                    },
                    enabled = !enviando,
                    label = {
                        Text(
                            if (comentarioObligatorio) {
                                "Cuéntanos qué encontraste"
                            } else {
                                "Comentario (opcional)"
                            }
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    isError = error != null,
                    supportingText = {
                        Text("${comentario.length}/${ReportePregunta.MAX_COMENTARIO}")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { motivo?.let { onEnviar(it, comentario) } },
                enabled = motivo != null &&
                    !enviando &&
                    (!comentarioObligatorio || comentario.isNotBlank())
            ) {
                Text(if (enviando) "Enviando..." else "Enviar reporte")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !enviando
            ) {
                Text("Cancelar")
            }
        }
    )
}
