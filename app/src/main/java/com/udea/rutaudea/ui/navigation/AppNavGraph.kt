package com.udea.rutaudea.ui.navigation

object AppNavGraph {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val SIMULACRO = "simulacro"
    const val RESULTADO = "resultado"
    const val PRACTICA = "practica"
    const val PROGRESO = "progreso"
    const val INFO = "info"
    const val PERFIL = "perfil"

    const val SIMULACRO_START_DEST = SIMULACRO

    // Routes with arguments
    fun resultadoRoute(score: Int, total: Int, timeUsed: Long) =
        "$RESULTADO?score=$score&total=$total&timeUsed=$timeUsed"
}