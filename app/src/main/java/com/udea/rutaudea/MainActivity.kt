package com.udea.rutaudea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.udea.rutaudea.ui.navigation.AppNavHost
import com.udea.rutaudea.ui.theme.RutaUdeaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RutaUdeaTheme {
                AppNavHost()
            }
        }
    }
}