package com.udea.rutaudea.data.mapper

import com.udea.rutaudea.data.local.entity.QuestionEntity
import com.udea.rutaudea.domain.model.Question
import org.junit.Test
import com.udea.rutaudea.domain.model.ImagenPregunta
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
    fun entityToDomain_mapsImagenUrl() {
        val withImg = entity.copy(imagenUrl = "https://storage/fig.png")
        val domain = QuestionMapper.entityToDomain(withImg)
        assertEquals("https://storage/fig.png", domain.imagenUrl)
        assertTrue(domain.tieneImagen())
    }

    @Test
    fun entityToDomain_sinImagen_tieneImagenFalse() {
        val domain = QuestionMapper.entityToDomain(entity)
        assertNull(domain.imagenUrl)
        assertFalse(domain.tieneImagen())
    }

    @Test
    fun domainToEntity_mapsImagenUrl() {
        val domain = QuestionMapper.entityToDomain(entity).copy(
            imagenUrl = "https://storage/fig.png"
        )
        val e = QuestionMapper.domainToEntity(domain)
        assertEquals("https://storage/fig.png", e.imagenUrl)
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

    @Test
    fun domainToEntity_preservaEstadoYProcedencia() {
        val domain = QuestionMapper.entityToDomain(entity).copy(
            estado = "pendiente",
            fuente = "pdf",
            tipoFuente = "original",
            verificada = false,
            contextoId = "T2-C01",
            bancoVersion = "udea-2026-10-v1",
            tarea = "T2",
            numero = 1,
            confianzaModelo = "alta",
            revisadoPor = "Michael",
            fechaRevision = "2026-10-22",
            notas = "revisión"
        )
        val e = QuestionMapper.domainToEntity(domain)

        assertEquals("pendiente", e.estado)
        assertEquals("pdf", e.fuente)
        assertEquals("original", e.tipoFuente)
        assertEquals("T2-C01", e.contextoId)
        assertEquals("udea-2026-10-v1", e.bancoVersion)
        assertEquals("T2", e.tarea)
        assertEquals(1, e.numero)
        assertEquals("alta", e.confianzaModelo)
        assertEquals("Michael", e.revisadoPor)
        assertEquals("2026-10-22", e.fechaRevision)
        assertEquals("revisión", e.notas)
    }

    @Test
    fun mapper_idaYVueltaConservaImagenesConRol() {
        val domain = QuestionMapper.entityToDomain(entity).copy(
            imagenes = listOf(
                ImagenPregunta(
                    url = "https://host/contexto.png",
                    rol = ImagenPregunta.ROL_CONTEXTO,
                    orden = 1,
                    alt = "Tabla"
                ),
                ImagenPregunta(
                    url = "https://host/pregunta.png",
                    rol = ImagenPregunta.ROL_PREGUNTA,
                    orden = 1,
                    alt = null
                )
            ),
            opcionesEnImagen = true,
            requiereImagen = true
        )
        val e = QuestionMapper.domainToEntity(domain)
        val vuelta = QuestionMapper.entityToDomain(e)

        assertEquals(2, vuelta.imagenes.size)
        assertEquals(ImagenPregunta.ROL_CONTEXTO, vuelta.imagenes[0].rol)
        assertEquals("Tabla", vuelta.imagenes[0].alt)
        assertTrue(vuelta.opcionesEnImagen)
        assertTrue(vuelta.requiereImagen)
        assertTrue(vuelta.tieneImagen())
        assertEquals(1, vuelta.imagenesDeContexto().size)
        assertEquals(1, vuelta.imagenesDePregunta().size)
    }

    @Test
    fun imagenesDesdeJson_toleraJsonInvalido() {
        assertTrue(QuestionMapper.imagenesDesdeJson("no-es-json").isEmpty())
        assertTrue(QuestionMapper.imagenesDesdeJson("[]").isEmpty())
        assertTrue(QuestionMapper.imagenesDesdeJson(null).isEmpty())
    }

    @Test
    fun entidadPorDefectoNoEsAprobada() {
        val e = entity.copy(estado = Question.ESTADO_PENDIENTE)
        assertEquals(Question.ESTADO_PENDIENTE, QuestionMapper.entityToDomain(e).estado)
    }
}
