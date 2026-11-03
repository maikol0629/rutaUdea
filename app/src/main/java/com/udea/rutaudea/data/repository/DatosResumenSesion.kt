package com.udea.rutaudea.data.repository

import java.util.UUID

/**
 * Datos comunes del resumen de una sesión (simulacro o práctica):
 * identificador, fecha y conteo de preguntas respondidas/omitidas.
 *
 * Compartido por [SimulationRepository] y [PracticeRepository] para que
 * ambos registros calculen el resumen de la misma forma y reutilicen el
 * id de sesión de los reportes de pregunta.
 */
internal data class DatosResumenSesion(
    val id: String,
    val fechaMs: Long,
    val respondidas: Int,
    val omitidas: Int
) {
    companion object {
        fun crear(
            total: Int,
            respondidas: Int,
            sessionId: String?,
            ahoraMs: Long = System.currentTimeMillis(),
            nuevoId: () -> String = { UUID.randomUUID().toString() }
        ) = DatosResumenSesion(
            id = sessionId ?: nuevoId(),
            fechaMs = ahoraMs,
            respondidas = respondidas,
            omitidas = total - respondidas
        )
    }
}
