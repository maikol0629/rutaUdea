package com.udea.rutaudea.ui.screen.splash

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.udea.rutaudea.R
import com.udea.rutaudea.di.AppModule
import com.udea.rutaudea.di.ViewModelFactories
import com.udea.rutaudea.ui.theme.ForestGreen
import com.udea.rutaudea.ui.theme.RutaUdeaTheme

@Composable
fun SplashScreen(
    onReady: (sesionActiva: Boolean) -> Unit
) {
    val app = LocalContext.current.applicationContext as android.app.Application
    val repository = AppModule.provideQuestionRepository(app)
    val authRepository = AppModule.provideAuthRepository(app)
    val viewModel: SplashViewModel = viewModel(
        factory = ViewModelFactories.SplashFactory(repository, authRepository)
    )

    LaunchedEffect(Unit) {
        viewModel.initializeAsync { sesionActiva ->
            onReady(sesionActiva)
        }
    }

    SplashContent()
}

/**
 * Contenido puro (sin ViewModel) para poder previsualizar y validar la
 * orientación. En horizontal el alto es reducido: el logo y los paddings
 * se ajustan para que la tarjeta no desborde la pantalla.
 */
@Composable
private fun SplashContent() {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val logoSize = if (isLandscape) 72.dp else 120.dp
    val cardPadding = if (isLandscape) 24.dp else 48.dp
    val contentSpacing = if (isLandscape) 16.dp else 24.dp
    val titleSize = if (isLandscape) 26.sp else 32.sp
    val subtitleSize = if (isLandscape) 14.sp else 16.sp

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(cardPadding),
                verticalArrangement = Arrangement.spacedBy(contentSpacing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_splash_logo),
                    contentDescription = "Logo RutaUdeA",
                    modifier = Modifier.size(logoSize)
                )
                Text(
                    text = "RutaUdeA",
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Preparación para admisión UdeA",
                    fontSize = subtitleSize,
                    color = ForestGreen.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                CircularProgressIndicator(
                    color = ForestGreen,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Cargando banco de preguntas…",
                    fontSize = 14.sp,
                    color = ForestGreen.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Splash · vertical")
@Preview(showBackground = true, widthDp = 800, heightDp = 360, name = "Splash · horizontal")
@Composable
private fun SplashContentPreview() {
    RutaUdeaTheme {
        SplashContent()
    }
}
