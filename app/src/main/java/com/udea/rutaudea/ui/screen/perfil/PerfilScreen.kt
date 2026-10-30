package com.udea.rutaudea.ui.screen.perfil

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.udea.rutaudea.R
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

    when {
        state.isLoading -> {
            FondoAuth {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        state.user != null -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PerfilLogueado(
                user = state.user!!,
                esGate = esGate,
                encuestaRespondida = state.encuestaRespondida,
                banco = state.banco,
                onLogout = viewModel::logout,
                onBack = onBack,
                onEncuestaClick = onEncuestaClick,
                onEditarPerfil = onEditarPerfil,
                onCompletarPerfil = onCompletarPerfil,
                onBuscarActualizaciones = viewModel::buscarActualizaciones
            )
        }

        else -> AuthScreen(
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

    if (state.mostrarRecuperacion) {
        DialogoRecuperacion(
            state = state,
            onCorreoChange = viewModel::onCorreoRecuperacionChange,
            onEnviar = viewModel::enviarRecuperacion,
            onCerrar = viewModel::cerrarRecuperacion
        )
    }
}

/**
 * Fondo con degradado de marca usado por el gate de autenticación.
 */
@Composable
private fun FondoAuth(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Pantalla de login / registro rediseñada: encabezado de marca, selector de
 * modo, botón de Google destacado, campos con iconos y visibilidad de
 * contraseña.
 */
@Composable
private fun AuthScreen(
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
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }
    val habilitado = !state.isSubmitting && !state.googleCargando

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EncabezadoAuth()

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SelectorModo(
                            esRegistro = state.esRegistro,
                            onToggleModo = onToggleModo
                        )

                        Text(
                            text = if (state.esRegistro) {
                                "Crea tu cuenta gratis"
                            } else {
                                "¡Hola de nuevo!"
                            },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (state.esRegistro) {
                                "Guarda tu progreso y prepárate con simulacros reales."
                            } else {
                                "Retoma tu preparación justo donde la dejaste."
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (state.esRegistro) {
                            BeneficiosRegistro()
                        }

                        BotonGoogle(
                            cargando = state.googleCargando,
                            habilitado = habilitado,
                            onClick = onGoogleClick
                        )

                        DivisorTexto("o continúa con tu correo")

                        if (state.esRegistro) {
                            OutlinedTextField(
                                value = state.nombre,
                                onValueChange = onNombreChange,
                                label = { Text("Nombre") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Person, contentDescription = null)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = state.apellido,
                                onValueChange = onApellidoChange,
                                label = { Text("Apellido") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Person, contentDescription = null)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = state.correo,
                            onValueChange = onCorreoChange,
                            label = { Text("Correo electrónico") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Email, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        CampoPassword(
                            value = state.password,
                            onValueChange = onPasswordChange,
                            label = "Contraseña",
                            visible = passwordVisible,
                            onToggleVisible = { passwordVisible = !passwordVisible },
                            imeAction = if (state.esRegistro) ImeAction.Next else ImeAction.Done
                        )

                        if (state.esRegistro) {
                            CampoPassword(
                                value = state.confirmPassword,
                                onValueChange = onConfirmPasswordChange,
                                label = "Confirmar contraseña",
                                visible = confirmVisible,
                                onToggleVisible = { confirmVisible = !confirmVisible },
                                imeAction = ImeAction.Done
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onAceptaTerminosChange(!state.aceptaTerminos)
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = state.aceptaTerminos,
                                    onCheckedChange = onAceptaTerminosChange
                                )
                                Text(
                                    text = "Acepto los términos y la política de privacidad",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onRecuperar) {
                                    Text("¿Olvidaste tu contraseña?", fontSize = 13.sp)
                                }
                            }
                        }

                        MensajesAuth(state)

                        Button(
                            onClick = onSubmit,
                            enabled = habilitado,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (state.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (state.esRegistro) {
                                        "Crear mi cuenta"
                                    } else {
                                        "Entrar"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (state.esRegistro) {
                            Text(
                                text = "Al registrarte aceptas nuestros términos y la " +
                                    "política de privacidad.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (!esGate) {
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Volver al inicio", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (state.esRegistro) {
                            "¿Ya tienes cuenta?"
                        } else {
                            "¿Es tu primera vez aquí?"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onToggleModo) {
                        Text(
                            text = if (state.esRegistro) {
                                "Inicia sesión"
                            } else {
                                "Crea tu cuenta gratis"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EncabezadoAuth() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.ic_splash_logo),
            contentDescription = "Logo RutaUdeA",
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(22.dp))
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "RutaUdeA",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Tu ruta hacia la UdeA, paso a paso",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SelectorModo(
    esRegistro: Boolean,
    onToggleModo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        SelectorModoItem(
            texto = "Iniciar sesión",
            seleccionado = !esRegistro,
            modifier = Modifier.weight(1f)
        ) {
            if (esRegistro) onToggleModo()
        }
        SelectorModoItem(
            texto = "Crear cuenta",
            seleccionado = esRegistro,
            modifier = Modifier.weight(1f)
        ) {
            if (!esRegistro) onToggleModo()
        }
    }
}

@Composable
private fun SelectorModoItem(
    texto: String,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(
                if (seleccionado) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                }
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = texto,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (seleccionado) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/**
 * Botón de Google destacado: superficie blanca elevada con el logotipo "G"
 * para que sea la opción más visible y rápida de usar.
 */
@Composable
private fun BotonGoogle(
    cargando: Boolean,
    habilitado: Boolean,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(16.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(3.dp, forma)
                .clip(forma)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, forma)
                .clickable(enabled = habilitado, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Continuar con Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "La forma más rápida: sin contraseñas",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DivisorTexto(texto: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 10.dp),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BeneficiosRegistro() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Beneficio("Simulacros reales")
        Beneficio("Práctica con feedback")
        Beneficio("Tu progreso")
    }
}

@Composable
private fun Beneficio(texto: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = texto,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun CampoPassword(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    imeAction: ImeAction
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(Icons.Outlined.Lock, contentDescription = null)
        },
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    imageVector = if (visible) {
                        Icons.Outlined.VisibilityOff
                    } else {
                        Icons.Outlined.Visibility
                    },
                    contentDescription = if (visible) {
                        "Ocultar contraseña"
                    } else {
                        "Mostrar contraseña"
                    }
                )
            }
        },
        singleLine = true,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun MensajesAuth(state: PerfilViewModel.UiState) {
    state.error?.let { error ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = error,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }

    state.mensaje?.let { mensaje ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = mensaje,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun PerfilLogueado(
    user: User,
    esGate: Boolean,
    encuestaRespondida: Boolean,
    banco: PerfilViewModel.BancoUiState,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onEncuestaClick: () -> Unit,
    onEditarPerfil: () -> Unit,
    onCompletarPerfil: () -> Unit,
    onBuscarActualizaciones: () -> Unit
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

    // Diagnóstico del banco: permite comprobar la sincronización sin Logcat.
    TarjetaEstadoBanco(
        banco = banco,
        onBuscarActualizaciones = onBuscarActualizaciones
    )

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
private fun TarjetaEstadoBanco(
    banco: PerfilViewModel.BancoUiState,
    onBuscarActualizaciones: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Estado del banco",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            FilaDiagnostico(
                etiqueta = "Versión",
                valor = banco.version ?: "Asset local (sin sincronizar)"
            )
            FilaDiagnostico(etiqueta = "Preguntas", valor = banco.total.toString())
            FilaDiagnostico(
                etiqueta = "Última sincronización",
                valor = BancoDiagnostico.formatearFechaHora(banco.ultimaSincronizacion)
                    ?: "Nunca"
            )
            BancoDiagnostico.huellaCorta(banco.hash)?.let { huella ->
                FilaDiagnostico(etiqueta = "Huella", valor = huella)
            }
            if (banco.schemaVersion > 0) {
                FilaDiagnostico(etiqueta = "Esquema", valor = "v${banco.schemaVersion}")
            }

            Text(
                text = "La app revisa el banco al abrirse o volver a primer plano " +
                    "(con internet) y cada 24 horas. Si pasas más de 3 días sin " +
                    "sincronizar, te enviaremos un recordatorio local.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            banco.error?.let { TiraMensajeBanco(texto = it, esError = true) }
            banco.mensaje?.let { TiraMensajeBanco(texto = it, esError = false) }

            Button(
                onClick = onBuscarActualizaciones,
                enabled = !banco.sincronizando,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (banco.sincronizando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buscando…", fontWeight = FontWeight.Medium)
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buscar actualizaciones", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun FilaDiagnostico(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = etiqueta,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = valor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun TiraMensajeBanco(texto: String, esError: Boolean) {
    val fondo = if (esError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val color = if (esError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(fondo)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (esError) {
                Icons.Outlined.WarningAmber
            } else {
                Icons.Outlined.CheckCircle
            },
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = texto, fontSize = 13.sp, color = color)
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
