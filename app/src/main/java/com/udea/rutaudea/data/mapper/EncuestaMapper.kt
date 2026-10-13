package com.udea.rutaudea.data.mapper

import com.udea.rutaudea.data.local.entity.EncuestaEntity
import com.udea.rutaudea.domain.model.EncuestaSatisfaccion

/**
 * Conversiones entre la cache local de la encuesta (Room) y el modelo de
 * dominio [EncuestaSatisfaccion].
 */
object EncuestaMapper {

    /** Entidad → dominio. `null` = el usuario aún no responde la encuesta. */
    fun toDomain(entity: EncuestaEntity?): EncuestaSatisfaccion? = entity?.let {
        EncuestaSatisfaccion(
            tipoInstitucion = it.tipoInstitucion,
            nombreInstitucion = it.nombreInstitucion,
            grado = it.grado,
            semestre = it.semestre,
            presentoExamen = it.presentoExamen,
            likertSimulacro = it.likertSimulacro,
            likertPractica = it.likertPractica,
            likertProgreso = it.likertProgreso,
            likertInfo = it.likertInfo,
            claridadRL = it.claridadRL,
            claridadCL = it.claridadCL,
            dificultadAdecuada = it.dificultadAdecuada,
            facilidadUso = it.facilidadUso,
            nps = it.nps,
            gustoMas = it.gustoMas,
            mejorar = it.mejorar,
            fechaMs = it.fechaMs
        )
    }

    /** Dominio → entidad, asociada al [uid] y con su estado de sincronización. */
    fun toEntity(uid: String, encuesta: EncuestaSatisfaccion, synced: Boolean): EncuestaEntity =
        EncuestaEntity(
            uid = uid,
            fechaMs = encuesta.fechaMs,
            tipoInstitucion = encuesta.tipoInstitucion,
            nombreInstitucion = encuesta.nombreInstitucion,
            grado = encuesta.grado,
            semestre = encuesta.semestre,
            presentoExamen = encuesta.presentoExamen,
            likertSimulacro = encuesta.likertSimulacro,
            likertPractica = encuesta.likertPractica,
            likertProgreso = encuesta.likertProgreso,
            likertInfo = encuesta.likertInfo,
            claridadRL = encuesta.claridadRL,
            claridadCL = encuesta.claridadCL,
            dificultadAdecuada = encuesta.dificultadAdecuada,
            facilidadUso = encuesta.facilidadUso,
            nps = encuesta.nps,
            gustoMas = encuesta.gustoMas,
            mejorar = encuesta.mejorar,
            synced = synced
        )

    /** Dominio → mapa para el documento de Firestore. */
    fun toMap(encuesta: EncuestaSatisfaccion): Map<String, Any?> = mapOf(
        "tipoInstitucion" to encuesta.tipoInstitucion,
        "nombreInstitucion" to encuesta.nombreInstitucion,
        "grado" to encuesta.grado,
        "semestre" to encuesta.semestre,
        "presentoExamen" to encuesta.presentoExamen,
        "likertSimulacro" to encuesta.likertSimulacro,
        "likertPractica" to encuesta.likertPractica,
        "likertProgreso" to encuesta.likertProgreso,
        "likertInfo" to encuesta.likertInfo,
        "claridadRL" to encuesta.claridadRL,
        "claridadCL" to encuesta.claridadCL,
        "dificultadAdecuada" to encuesta.dificultadAdecuada,
        "facilidadUso" to encuesta.facilidadUso,
        "nps" to encuesta.nps,
        "gustoMas" to encuesta.gustoMas,
        "mejorar" to encuesta.mejorar,
        "fechaMs" to encuesta.fechaMs
    )

    /** Documento de Firestore → dominio. `null` si el documento no existe. */
    fun fromMap(map: Map<String, Any?>?): EncuestaSatisfaccion? {
        if (map == null) return null
        return EncuestaSatisfaccion(
            tipoInstitucion = map["tipoInstitucion"] as? String ?: "",
            nombreInstitucion = map["nombreInstitucion"] as? String ?: "",
            grado = map["grado"] as? String ?: "",
            semestre = map["semestre"] as? String ?: "",
            presentoExamen = map["presentoExamen"] as? Boolean,
            likertSimulacro = (map["likertSimulacro"] as? Long)?.toInt() ?: 0,
            likertPractica = (map["likertPractica"] as? Long)?.toInt() ?: 0,
            likertProgreso = (map["likertProgreso"] as? Long)?.toInt() ?: 0,
            likertInfo = (map["likertInfo"] as? Long)?.toInt() ?: 0,
            claridadRL = (map["claridadRL"] as? Long)?.toInt() ?: 0,
            claridadCL = (map["claridadCL"] as? Long)?.toInt() ?: 0,
            dificultadAdecuada = (map["dificultadAdecuada"] as? Long)?.toInt() ?: 0,
            facilidadUso = (map["facilidadUso"] as? Long)?.toInt() ?: 0,
            nps = (map["nps"] as? Long)?.toInt() ?: -1,
            gustoMas = map["gustoMas"] as? String ?: "",
            mejorar = map["mejorar"] as? String ?: "",
            fechaMs = (map["fechaMs"] as? Long) ?: 0L
        )
    }
}
