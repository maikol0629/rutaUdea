package com.udea.rutaudea.data.mapper

import com.udea.rutaudea.data.local.entity.ReportePreguntaEntity
import com.udea.rutaudea.domain.model.ModoReporte
import com.udea.rutaudea.domain.model.MotivoReporte
import com.udea.rutaudea.domain.model.ReportePregunta

/**
 * Conversiones entre la cache local del reporte (Room) y el modelo de
 * dominio [ReportePregunta], además del mapa que se escribe en
 * `reportesPreguntas` de Firestore.
 */
object ReportePreguntaMapper {

    fun toDomain(entity: ReportePreguntaEntity): ReportePregunta = ReportePregunta(
        id = entity.id,
        questionId = entity.questionId,
        motivo = MotivoReporte.fromClave(entity.motivo),
        comentario = entity.comentario,
        modo = ModoReporte.fromClave(entity.modo),
        sesionId = entity.sesionId,
        area = entity.area,
        subtema = entity.subtema,
        bancoVersion = entity.bancoVersion,
        respuestaUsuario = entity.respuestaUsuario,
        respuestaCorrecta = entity.respuestaCorrecta,
        fechaMs = entity.fechaMs,
        appVersionCode = entity.appVersionCode,
        estado = entity.estado
    )

    fun toEntity(
        uid: String,
        reporte: ReportePregunta,
        synced: Boolean
    ): ReportePreguntaEntity = ReportePreguntaEntity(
        id = reporte.id,
        uid = uid,
        questionId = reporte.questionId,
        motivo = reporte.motivo.clave,
        comentario = reporte.comentario,
        modo = reporte.modo.clave,
        sesionId = reporte.sesionId,
        area = reporte.area,
        subtema = reporte.subtema,
        bancoVersion = reporte.bancoVersion,
        respuestaUsuario = reporte.respuestaUsuario,
        respuestaCorrecta = reporte.respuestaCorrecta,
        fechaMs = reporte.fechaMs,
        appVersionCode = reporte.appVersionCode,
        estado = reporte.estado,
        synced = synced
    )

    /**
     * Documento de Firestore. Los campos opcionales nulos se omiten para
     * no enviar `null` y mantener las reglas de seguridad simples.
     */
    fun toMap(uid: String, reporte: ReportePregunta): Map<String, Any?> = buildMap {
        put("uid", uid)
        put("questionId", reporte.questionId)
        put("motivo", reporte.motivo.clave)
        put("comentario", reporte.comentario)
        put("modo", reporte.modo.clave)
        put("sesionId", reporte.sesionId)
        put("area", reporte.area)
        put("subtema", reporte.subtema)
        put("fechaMs", reporte.fechaMs)
        put("appVersionCode", reporte.appVersionCode)
        put("estado", reporte.estado)

        reporte.bancoVersion?.takeIf { it.isNotBlank() }?.let { put("bancoVersion", it) }
        reporte.respuestaUsuario?.takeIf { it.isNotBlank() }?.let { put("respuestaUsuario", it) }
        reporte.respuestaCorrecta?.takeIf { it.isNotBlank() }?.let { put("respuestaCorrecta", it) }
    }
}
