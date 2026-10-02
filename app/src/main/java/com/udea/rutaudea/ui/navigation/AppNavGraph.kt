package com.udea.rutaudea.ui.navigation

object AppNavGraph {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val SIMULACRO = "simulacro"
    const val RESULTADO = "resultado"
    const val PRACTICA = "practica"
    const val PRACTICA_SESION = "practica/sesion"
    const val PRACTICA_RESULTADO = "practica/resultado"
    const val PROGRESO = "progreso"
    const val INFO = "info"
    const val PERFIL = "perfil"

    /** Ruta con argumento `gate` (true = pantalla usada como gate de login). */
    const val PERFIL_ARGS = "perfil?gate={gate}"

    /** Ruta navegable del tab Perfil / gate de login. */
    fun perfilRoute(gate: Boolean = false): String = "perfil?gate=$gate"
}
