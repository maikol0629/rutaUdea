package com.udea.rutaudea.ui.screen.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun PerfilScreen(
    viewModel: PerfilViewModel,
    esGate: Boolean = false,
    onAuthSuccess: () -> Unit = {},
    onLogoutNavigate: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    // Eventos de navegación del gate de autenticación.
    LaunchedEffect(Unit) {
        viewModel.evento.collect { evento ->
            when (evento) {
                PerfilViewModel.PerfilEvento.Autenticado ->
                    if (esGate) onAuthSuccess()
                PerfilViewModel.PerfilEvento.SesionCerrada -> onLogoutNavigate()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

            state.user != null -> PerfilLogueado(
                nombre = state.user!!.nombre.ifEmpty { state.user!!.correo },
                correo = state.user!!.correo,
                rol = state.user!!.rol,
                esGate = esGate,
                onLogout = { viewModel.logout() },
                onBack = onBack
            )

            else -> PerfilFormulario(
                state = state,
                esGate = esGate,
                onNombreChange = viewModel::onNombreChange,
                onCorreoChange = viewModel::onCorreoChange,
                onPasswordChange = viewModel::onPasswordChange,
                onToggleModo = viewModel::toggleModo,
                onSubmit = viewModel::submit,
                onBack = onBack
            )
        }
    }
}

@Composable
private fun PerfilFormulario(
    state: PerfilViewModel.UiState,
    esGate: Boolean,
    onNombreChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleModo: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier.padding(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "👤", fontSize = 48.sp)
            Text(
                text = if (state.esRegistro) "Crear cuenta" else "Iniciar sesión",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            if (esGate) {
                Text(
                    text = "Inicia sesión o crea una cuenta para continuar",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (state.esRegistro) {
                OutlinedTextField(
                    value = state.nombre,
                    onValueChange = onNombreChange,
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = state.correo,
                onValueChange = onCorreoChange,
                label = { Text("Correo electrónico") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

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
                onClick = onSubmit,
                enabled = !state.isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (state.esRegistro) "Registrarme" else "Entrar",
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            TextButton(onClick = onToggleModo) {
                Text(
                    text = if (state.esRegistro)
                        "¿Ya tienes cuenta? Inicia sesión"
                    else
                        "¿No tienes cuenta? Regístrate",
                    fontSize = 14.sp
                )
            }

            if (!esGate) {
                OutlinedButton(onClick = onBack) {
                    Text("Volver al inicio", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun PerfilLogueado(
    nombre: String,
    correo: String,
    rol: String,
    esGate: Boolean,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier.padding(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "👤", fontSize = 48.sp)
            Text(
                text = nombre,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = correo,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(
                        text = "Rol: $rol",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar sesión", fontWeight = FontWeight.Medium)
            }
            if (!esGate) {
                OutlinedButton(onClick = onBack) {
                    Text("Volver al inicio", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
