package com.udea.rutaudea.data.mapper

import com.udea.rutaudea.data.local.entity.ReportePreguntaEntity
import com.udea.rutaudea.domain.model.ModoReporte
import com.udea.rutaudea.domain.model.MotivoReporte
import com.udea.rutaudea.domain.model.ReportePregunta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests de las conversiones Room ↔ dominio ↔ documento de Firestore. */
class ReportePreguntaMapperTest {

    private val dominio = ReportePregunta(
        id = "r1",
        questionId = "q1",
        motivo = MotivoReporte.FALTAN_DATOS,
        comentario = "Falta la imagen",
        modo = ModoReporte.PRACTICA,
        sesionId = "s1",
        area = "razonamiento_logico",
        subtema = "Series",
        bancoVersion = "v2",
        respuestaUsuario = "A",
        respuestaCorrecta = "B",
        fechaMs = 123L,
        appVersionCode = 7,
        estado = ReportePregunta.ESTADO_PENDIENTE
    )

    @Test
    fun `toEntity y toDomain conservan los campos`() {
        val entity = ReportePreguntaMapper.toEntity("u1", dominio, synced = false)

        assertEquals("u1", entity.uid)
        assertFalse(entity.synced)
        assertEquals(dominio, ReportePreguntaMapper.toDomain(entity))
    }

    @Test
    fun `toMap incluye los campos obligatorios y omite opcionales vacios`() {
        val map = ReportePreguntaMapper.toMap("u1", dominio)

        assertEquals("u1", map["uid"])
        assertEquals("q1", map["questionId"])
        assertEquals("faltan_datos", map["motivo"])
        assertEquals("practica", map["modo"])
        assertEquals("s1", map["sesionId"])
        assertEquals("pendiente", map["estado"])
        assertEquals("v2", map["bancoVersion"])
    }

    @Test
    fun `toMap omite bancoVersion cuando es nulo`() {
        val map = ReportePreguntaMapper.toMap("u1", dominio.copy(bancoVersion = null))

        assertNull(map["bancoVersion"])
    }

    @Test
    fun `un valor desconocido degrada al motivo otra`() {
        val entity = ReportePreguntaEntity(
            id = "r1",
            uid = "u1",
            questionId = "q1",
            motivo = "desconocido",
            comentario = "",
            modo = "desconocido",
            sesionId = "s1",
            area = "",
            subtema = "",
            bancoVersion = null,
            respuestaUsuario = null,
            respuestaCorrecta = null,
            fechaMs = 1L,
            appVersionCode = 1,
            estado = ReportePregunta.ESTADO_PENDIENTE,
            synced = false
        )

        val reporte = ReportePreguntaMapper.toDomain(entity)

        assertEquals(MotivoReporte.OTRA, reporte.motivo)
        assertEquals(ModoReporte.PRACTICA, reporte.modo)
    }
}
