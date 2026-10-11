package com.udea.rutaudea.ui.screen.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Observa el ciclo de vida del **proceso** (no el de la Activity) para
 * distinguir «la app pasó a segundo plano» de una simple rotación.
 *
 * - Al rotar, [ProcessLifecycleOwner] no se detiene → no dispara [onBackground].
 * - Al cambiar de app (home/recents), sí emite `ON_STOP` y, al volver, `ON_START`.
 *
 * Nota: al registrar el observador con la app ya en primer plano, el
 * lifecycle emite `ON_START` de inmediato. Los ViewModels ignoran esa
 * primera llamada porque solo muestran la pausa si antes hubo un
 * `onAppBackgrounded()` real.
 */
@Composable
fun AppBackgroundEffect(
    onBackground: () -> Unit,
    onForeground: () -> Unit
) {
    val currentOnBackground by rememberUpdatedState(onBackground)
    val currentOnForeground by rememberUpdatedState(onForeground)

    DisposableEffect(Unit) {
        val owner = ProcessLifecycleOwner.get()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> currentOnBackground()
                Lifecycle.Event.ON_START -> currentOnForeground()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
