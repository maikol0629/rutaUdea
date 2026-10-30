package com.udea.rutaudea

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.udea.rutaudea.ui.navigation.AppNavHost
import com.udea.rutaudea.ui.theme.RutaUdeaTheme

class MainActivity : ComponentActivity() {

    private val permisoNotificaciones = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* El recordatorio es opcional: si se niega, la app sigue igual. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RutaUdeaTheme {
                // targetSdk 35 → Android 15 fuerza edge-to-edge: sin padding
                // de insets el contenido cae bajo la barra de estado (notch)
                // y la barra de navegación. safeDrawingPadding las cubre globalmente.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                        AppNavHost()
                    }
                }
            }
        }
        solicitarPermisoNotificaciones()
    }

    /** Android 13+ exige permiso explícito para mostrar el recordatorio local. */
    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val concedido = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!concedido) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
