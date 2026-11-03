package com.udea.rutaudea.ui.screen.common

/**
 * Estado del diálogo de reporte de pregunta, compartido por las sesiones
 * de simulacro y práctica.
 *
 * - [visible]: el diálogo está abierto.
 * - [enviando]: se está guardando/sincronizando el reporte.
 * - [mensaje]: confirmación tras un envío correcto (incluye el estado de
 *   sincronización).
 * - [error]: error de validación o de guardado local.
 */
data class ReporteUiState(
    val visible: Boolean = false,
    val enviando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null
)
