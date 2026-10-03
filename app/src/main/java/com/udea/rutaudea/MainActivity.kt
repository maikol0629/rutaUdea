package com.udea.rutaudea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.udea.rutaudea.ui.navigation.AppNavHost
import com.udea.rutaudea.ui.theme.RutaUdeaTheme

class MainActivity : ComponentActivity() {

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
    }
}
