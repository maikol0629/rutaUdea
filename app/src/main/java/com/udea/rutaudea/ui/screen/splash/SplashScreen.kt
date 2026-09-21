package com.udea.rutaudea.ui.screen.splash

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.udea.rutaudea.R
import com.udea.rutaudea.di.AppModule
import com.udea.rutaudea.di.ViewModelFactories
import com.udea.rutaudea.ui.theme.ForestGreen
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SplashScreen(
    onLoadingComplete: () -> Unit
) {
    val app = LocalContext.current.applicationContext as android.app.Application
    val repository = AppModule.provideQuestionRepository(app)
    val viewModel: SplashViewModel = viewModel(
        factory = ViewModelFactories.SplashFactory(repository)
    )

    LaunchedEffect(Unit) {
        viewModel.loadQuestions()
        onLoadingComplete()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(48.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.ic_splash_logo),
                    contentDescription = null,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(ForestGreen),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "RutaUdeA",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Preparación para admisión UdeA",
                    fontSize = 16.sp,
                    color = ForestGreen.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                CircularProgressIndicator(
                    color = ForestGreen,
                    modifier = Modifier.padding(top = 16.dp)
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