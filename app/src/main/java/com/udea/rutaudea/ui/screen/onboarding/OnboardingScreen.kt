package com.udea.rutaudea.ui.screen.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.udea.rutaudea.domain.model.DatosPerfil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pantalla de onboarding obligatorio. También se usa para editar el perfil
 * cuando [esEdicion] es `true`.
 *
 * Cuando el tipo de institución es "Otra", el campo de grado/semestre no se
 * muestra ni se envía.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    esEdicion: Boolean = false,
    onGuardado: () -> Unit = {},
    onLogout: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.evento.collect { evento ->
            when (evento) {
                OnboardingViewModel.Evento.Guardado -> onGuardado()
                OnboardingViewModel.Evento.SesionCerrada -> onLogout()
            }
        }
    }

    if (state.cargando) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    val datos = state.datos
    val opcionesGrado = DatosPerfil.opcionesGrado(datos.tipoInstitucion)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = if (esEdicion) "✏️" else "🎓", fontSize = 44.sp)
        Text(
            text = if (esEdicion) "Edita tu perfil" else "Completa tu perfil",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (esEdicion) {
                "Actualiza tus datos cuando lo necesites"
            } else {
                "Necesitamos estos datos para personalizar tu preparación. Solo toma un minuto."
            },
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = datos.nombre,
                    onValueChange = viewModel::onNombreChange,
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = datos.apellido,
                    onValueChange = viewModel::onApellidoChange,
                    label = { Text("Apellido") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Tipo de institución",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DatosPerfil.TIPOS_INSTITUCION.forEach { tipo ->
                        FilterChip(
                            selected = datos.tipoInstitucion == tipo,
                            onClick = { viewModel.onTipoInstitucionChange(tipo) },
                            label = { Text(tipo) }
                        )
                    }
                }

                OutlinedTextField(
                    value = datos.institucion,
                    onValueChange = viewModel::onInstitucionChange,
                    label = { Text("Nombre de la institución") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (opcionesGrado.isNotEmpty()) {
                    CampoDesplegable(
                        etiqueta = if (datos.tipoInstitucion == DatosPerfil.TIPO_COLEGIO) {
                            "Grado"
                        } else {
                            "Semestre"
                        },
                        opciones = opcionesGrado,
                        valor = datos.gradoSemestre,
                        onCambio = viewModel::onGradoSemestreChange
                    )
                } else if (datos.tipoInstitucion == DatosPerfil.TIPO_OTRA) {
                    Text(
                        text = "No necesitas indicar grado ni semestre",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = datos.telefono,
                    onValueChange = viewModel::onTelefonoChange,
                    label = { Text("Teléfono (opcional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = datos.ciudad,
                    onValueChange = viewModel::onCiudadChange,
                    label = { Text("Ciudad (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = formatearFecha(datos.fechaExamen),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha del examen (opcional)") },
                    trailingIcon = {
                        TextButton(onClick = { mostrarSelectorFecha = true }) {
                            Text("Elegir")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (datos.fechaExamen != null) {
                    TextButton(onClick = { viewModel.onFechaExamenChange(null) }) {
                        Text("Quitar fecha")
                    }
                }
            }
        }

        if (state.errores.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.errores.forEach { error ->
                        Text(
                            text = "• $error",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        state.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = viewModel::guardar,
            enabled = !state.guardando,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.guardando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (esEdicion) "Guardar cambios" else "Guardar y continuar",
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (esEdicion) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar", fontWeight = FontWeight.Medium)
            }
        } else {
            TextButton(onClick = viewModel::cerrarSesion) {
                Text("Cerrar sesión")
            }
        }
    }

    if (mostrarSelectorFecha) {
        val estadoFecha = rememberDatePickerState(
            initialSelectedDateMillis = datos.fechaExamen
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFecha = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onFechaExamenChange(estadoFecha.selectedDateMillis)
                        mostrarSelectorFecha = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorFecha = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = estadoFecha)
        }
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

private fun formatearFecha(ms: Long?): String {
    if (ms == null) return ""
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return formato.format(Date(ms))
}
