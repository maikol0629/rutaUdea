package com.udea.rutaudea.data.mapper

import com.udea.rutaudea.data.local.entity.QuestionEntity
import com.udea.rutaudea.domain.model.Question
import org.junit.Test
import org.junit.Assert.*

class QuestionMapperTest {

    private val entity = QuestionEntity(
        id = "test-1",
        area = "razonamiento_logico",
        componente = "Razonamiento Lógico",
        subtema = "Series",
        competencia = "Razonamiento numerico",
        tipoTexto = null,
        dificultad = "baja",
        textoBase = null,
        contexto = null,
        contextoId = null,
        pregunta = "1 - 2 + 3 - 4",
        opcionA = "50",
        opcionB = "0",
        opcionC = "-20",
        opcionD = "-50",
        respuestaCorrecta = "D",
        explicacion = "50 pares suman -1 cada uno => -50",
        fuente = "test",
        tipoFuente = "test",
        esOriginal = false,
        verificada = true,
        estado = "aprobado"
    )

    private val entityWithContext = entity.copy(
        id = "test-2",
        area = "competencia_lectora",
        componente = "Competencia Lectora",
        subtema = "CL02 Inferencia",
        textoBase = "Texto de apoyo para lectura crítica",
        contexto = "Contexto compartido",
        contextoId = "ctx-1",
        pregunta = "¿Qué se infiere?",
        opcionA = "Opción A",
        opcionB = "Opción B",
        opcionC = "Opción C",
        opcionD = "Opción D",
        respuestaCorrecta = "A"
    )

    @Test
    fun entityToDomain_mapsAllFieldsCorrectly() {
        val domain = QuestionMapper.entityToDomain(entity)

        assertEquals(entity.id, domain.id)
        assertEquals(entity.area, domain.area)
        assertEquals(entity.componente, domain.componente)
        assertEquals(entity.subtema, domain.subtema)
        assertEquals(entity.competencia, domain.competencia)
        assertEquals(entity.tipoTexto, domain.tipoTexto)
        assertEquals(entity.dificultad, domain.dificultad)
        assertEquals(entity.textoBase, domain.textoBase)
        assertEquals(entity.contexto, domain.contexto)
        assertEquals(entity.pregunta, domain.pregunta)
        assertEquals(listOf(entity.opcionA, entity.opcionB, entity.opcionC, entity.opcionD), domain.opciones)
        assertEquals(entity.respuestaCorrecta, domain.respuestaCorrecta)
        assertEquals(entity.explicacion, domain.explicacion)
        assertEquals(entity.esOriginal, domain.esOriginal)
    }

    @Test
    fun entityToDomain_withContext_mapsTextoBaseAndContexto() {
        val domain = QuestionMapper.entityToDomain(entityWithContext)

        assertEquals("Texto de apoyo para lectura crítica", domain.textoBase)
        assertEquals("Contexto compartido", domain.contexto)
        assertEquals("competencia_lectora", domain.area)
    }

    @Test
    fun domainToEntity_mapsAllFieldsCorrectly() {
        val domain = Question(
            id = "domain-1",
            area = "razonamiento_logico",
            componente = "Razonamiento Lógico",
            subtema = "Porcentajes",
            competencia = "Razonamiento cuantitativo",
            tipoTexto = null,
            dificultad = "media",
            textoBase = null,
            contexto = null,
            pregunta = "Test pregunta",
            opciones = listOf("A", "B", "C", "D"),
            respuestaCorrecta = "B",
            explicacion = "Explicación test",
            esOriginal = true
        )

        val entity = QuestionMapper.domainToEntity(domain)

        assertEquals(domain.id, entity.id)
        assertEquals(domain.area, entity.area)
        assertEquals(domain.componente, entity.componente)
        assertEquals(domain.subtema, entity.subtema)
        assertEquals(domain.competencia, entity.competencia)
        assertEquals(domain.tipoTexto, entity.tipoTexto)
        assertEquals(domain.dificultad, entity.dificultad)
        assertEquals(domain.textoBase, entity.textoBase)
        assertEquals(domain.contexto, entity.contexto)
        assertEquals(domain.pregunta, entity.pregunta)
        assertEquals(domain.opciones[0], entity.opcionA)
        assertEquals(domain.opciones[1], entity.opcionB)
        assertEquals(domain.opciones[2], entity.opcionC)
        assertEquals(domain.opciones[3], entity.opcionD)
        assertEquals(domain.respuestaCorrecta, entity.respuestaCorrecta)
        assertEquals(domain.explicacion, entity.explicacion)
        assertEquals(domain.esOriginal, entity.esOriginal)
    }

    @Test
    fun domainToEntity_withLessThan4Options_padsWithEmptyString() {
        val domain = Question(
            id = "domain-2",
            area = "test",
            componente = null,
            subtema = "test",
            competencia = null,
            tipoTexto = null,
            dificultad = "baja",
            textoBase = null,
            contexto = null,
            pregunta = "Test",
            opciones = listOf("Solo A", "Solo B"),
            respuestaCorrecta = "A",
            explicacion = null,
            esOriginal = false
        )

        val entity = QuestionMapper.domainToEntity(domain)

        assertEquals("Solo A", entity.opcionA)
        assertEquals("Solo B", entity.opcionB)
        assertEquals("", entity.opcionC)
        assertEquals("", entity.opcionD)
    }

    @Test
    fun listToDomain_mapsListCorrectly() {
        val entities = listOf(entity, entityWithContext)
        val domains = QuestionMapper.listToDomain(entities)

        assertEquals(2, domains.size)
        assertEquals(entity.id, domains[0].id)
        assertEquals(entityWithContext.id, domains[1].id)
    }

    @Test
    fun respuestaCorrectaTexto_returnsCorrectOption() {
        val domain = QuestionMapper.entityToDomain(entity)
        assertEquals("-50", domain.respuestaCorrectaTexto())
    }

    @Test
    fun opcionPorLetra_returnsCorrectOption() {
        val domain = QuestionMapper.entityToDomain(entity)
        assertEquals("50", domain.opcionPorLetra("A"))
        assertEquals("0", domain.opcionPorLetra("B"))
        assertEquals("-20", domain.opcionPorLetra("C"))
        assertEquals("-50", domain.opcionPorLetra("D"))
        assertNull(domain.opcionPorLetra("E"))
    }
}