package com.udea.rutaudea.ui.screen.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.udea.rutaudea.domain.model.User
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pantalla de autenticación y perfil.
 *
 * - Como gate ([esGate] = true) permite iniciar sesión, registrarse o usar
 *   Google. Tras autenticarse avisa si hace falta onboarding.
 * - Como pestaña muestra los datos del perfil, la encuesta piloto y el
 *   acceso a editar el perfil.
 */
@Composable
fun PerfilScreen(
    viewModel: PerfilViewModel,
    esGate: Boolean = false,
    onAuthSuccess: (requiereOnboarding: Boolean) -> Unit = {},
    onLogoutNavigate: () -> Unit = {},
    onBack: () -> Unit = {},
    onEncuestaClick: () -> Unit = {},
    onEditarPerfil: () -> Unit = {},
    onCompletarPerfil: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Recarga al volver a la pantalla (por ejemplo, tras responder la
    // encuesta o editar el perfil): la entrada del back stack se conserva
    // y el init no se repite, así que el estado quedaría desactualizado.
    LifecycleResumeEffect(Unit) {
        viewModel.recargar()
        onPauseOrDispose { }
    }

    // Eventos de navegación del gate de autenticación.
    LaunchedEffect(Unit) {
        viewModel.evento.collect { evento ->
            when (evento) {
                is PerfilViewModel.PerfilEvento.Autenticado ->
                    onAuthSuccess(evento.requiereOnboarding)
                PerfilViewModel.PerfilEvento.SesionCerrada -> onLogoutNavigate()
            }
        }
    }

    // El Web Client ID lo genera el plugin de Google Services a partir de
    // google-services.json. Se busca en runtime para no depender de que el
    // recurso exista al compilar (por ejemplo, si se cambió el JSON).
    val webClientId = remember {
        val id = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (id != 0) context.getString(id) else null
    }

    val onGoogleClick: () -> Unit = {
        if (webClientId.isNullOrBlank()) {
            viewModel.onGoogleError(
                "Google Sign-In no está configurado en este build. " +
                    "Descarga de nuevo google-services.json"
            )
        } else {
            viewModel.onGoogleIniciado()
            scope.launch {
                when (val resultado = obtenerIdTokenGoogle(context, webClientId)) {
                    is ResultadoGoogle.Exito -> viewModel.onGoogleIdToken(resultado.idToken)
                    ResultadoGoogle.Cancelado -> viewModel.onGoogleCancelado()
                    is ResultadoGoogle.Error -> viewModel.onGoogleError(resultado.mensaje)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            state.isLoading -> {
                Spacer(modifier = Modifier.height(48.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            state.user != null -> PerfilLogueado(
                user = state.user!!,
                esGate = esGate,
                encuestaRespondida = state.encuestaRespondida,
                onLogout = viewModel::logout,
                onBack = onBack,
                onEncuestaClick = onEncuestaClick,
                onEditarPerfil = onEditarPerfil,
                onCompletarPerfil = onCompletarPerfil
            )

            else -> PerfilFormulario(
                state = state,
                esGate = esGate,
                onNombreChange = viewModel::onNombreChange,
                onApellidoChange = viewModel::onApellidoChange,
                onCorreoChange = viewModel::onCorreoChange,
                onPasswordChange = viewModel::onPasswordChange,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                onAceptaTerminosChange = viewModel::onAceptaTerminosChange,
                onToggleModo = viewModel::toggleModo,
                onSubmit = viewModel::submit,
                onRecuperar = viewModel::abrirRecuperacion,
                onGoogleClick = onGoogleClick,
                onBack = onBack
            )
        }
    }

    if (state.mostrarRecuperacion) {
        DialogoRecuperacion(
            state = state,
            onCorreoChange = viewModel::onCorreoRecuperacionChange,
            onEnviar = viewModel::enviarRecuperacion,
            onCerrar = viewModel::cerrarRecuperacion
        )
    }
}

@Composable
private fun PerfilFormulario(
    state: PerfilViewModel.UiState,
    esGate: Boolean,
    onNombreChange: (String) -> Unit,
    onApellidoChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onAceptaTerminosChange: (Boolean) -> Unit,
    onToggleModo: () -> Unit,
    onSubmit: () -> Unit,
    onRecuperar: () -> Unit,
    onGoogleClick: () -> Unit,
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                OutlinedTextField(
                    value = state.apellido,
                    onValueChange = onApellidoChange,
                    label = { Text("Apellido") },
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

            if (state.esRegistro) {
                OutlinedTextField(
                    value = state.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirmar contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.aceptaTerminos,
                        onCheckedChange = onAceptaTerminosChange
                    )
                    Text(
                        text = "Acepto los términos y la política de privacidad",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                TextButton(onClick = onRecuperar) {
                    Text("¿Olvidaste tu contraseña?", fontSize = 14.sp)
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

            state.mensaje?.let { mensaje ->
                Text(
                    text = mensaje,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = onSubmit,
                enabled = !state.isSubmitting && !state.googleCargando,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
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

            Text(
                text = "o",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onGoogleClick,
                enabled = !state.isSubmitting && !state.googleCargando,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.googleCargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Continuar con Google", fontWeight = FontWeight.Medium)
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
    user: User,
    esGate: Boolean,
    encuestaRespondida: Boolean,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onEncuestaClick: () -> Unit,
    onEditarPerfil: () -> Unit,
    onCompletarPerfil: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "👤", fontSize = 48.sp)
            Text(
                text = user.nombreCompleto.ifEmpty { user.correo },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = user.correo,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            val etiquetaProveedor = if (user.proveedor == User.PROVEEDOR_GOOGLE) {
                "Cuenta de Google"
            } else {
                "Correo y contraseña"
            }
            Text(
                text = etiquetaProveedor,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (user.institucion.isNotBlank()) {
                Text(
                    text = user.institucion,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                val detalle = listOf(user.tipoInstitucion, user.gradoSemestre)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                if (detalle.isNotBlank()) {
                    Text(
                        text = detalle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (user.telefono.isNotBlank()) {
                Text(
                    text = "Teléfono: ${user.telefono}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (user.ciudad.isNotBlank()) {
                Text(
                    text = "Ciudad: ${user.ciudad}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (user.fechaExamen != null) {
                Text(
                    text = "Fecha del examen: ${formatearFecha(user.fechaExamen)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(
                        text = "Rol: ${user.rol}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (user.requiereOnboarding) {
                Button(
                    onClick = onCompletarPerfil,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Completar perfil", fontWeight = FontWeight.Medium)
                }
            }

            OutlinedButton(
                onClick = onEditarPerfil,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Editar perfil", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(4.dp))
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

    // Encuesta piloto: llamativa para que la diligencien.
    Card(
        onClick = onEncuestaClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text(
                    text = if (encuestaRespondida) "✅" else "🎓",
                    fontSize = 28.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "PRUEBA PILOTO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = if (encuestaRespondida) "¡Gracias por tu opinión!" else "Tu opinión cuenta",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = if (encuestaRespondida) "Toca para actualizar tus respuestas"
                    else "Encuesta de satisfacción · 2 min",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
        Button(
            onClick = onEncuestaClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            Text(
                text = if (encuestaRespondida) "Actualizar respuestas" else "Responder encuesta",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DialogoRecuperacion(
    state: PerfilViewModel.UiState,
    onCorreoChange: (String) -> Unit,
    onEnviar: () -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Recuperar contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.recuperacionEnviada) {
                    Text(
                        "Si el correo está registrado, recibirás un enlace para " +
                            "restablecer tu contraseña."
                    )
                } else {
                    Text("Escribe tu correo y te enviaremos un enlace para restablecerla.")
                    OutlinedTextField(
                        value = state.correoRecuperacion,
                        onValueChange = onCorreoChange,
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    state.error?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (state.recuperacionEnviada) {
                TextButton(onClick = onCerrar) { Text("Cerrar") }
            } else {
                TextButton(onClick = onEnviar, enabled = !state.isSubmitting) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Enviar")
                    }
                }
            }
        },
        dismissButton = {
            if (!state.recuperacionEnviada) {
                TextButton(onClick = onCerrar) { Text("Cancelar") }
            }
        }
    )
}

private fun formatearFecha(ms: Long): String {
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return formato.format(Date(ms))
}
