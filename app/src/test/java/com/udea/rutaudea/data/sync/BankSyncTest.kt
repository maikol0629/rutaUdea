package com.udea.rutaudea.data.sync

import com.udea.rutaudea.data.local.entity.QuestionEntity
import com.udea.rutaudea.domain.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de la sincronización híbrida (D-02): manifiesto, mapper de
 * Firestore y validación del lote antes de reemplazar Room.
 */
class BankSyncTest {

    private val manifiesto = BankManifest(
        bankVersion = "udea-2026-10-v1",
        schemaVersion = 2,
        status = "active",
        hash = "sha256:abc",
        counts = BankCounts(total = 2, aprobadas = 2, razonamientoLogico = 1, competenciaLectora = 1)
    )

    private fun entidad(
        id: String = "T1-P01",
        estado: String = Question.ESTADO_APROBADO,
        requiereImagen: Boolean = false,
        imagenesJson: String = "[]",
        version: String = "udea-2026-10-v1"
    ) = QuestionEntity(
        id = id,
        area = "razonamiento_logico",
        componente = "Razonamiento Lógico",
        subtema = "RL01 Porcentajes",
        dificultad = "media",
        pregunta = "¿Cuánto es 10 % de 100?",
        opcionA = "10",
        opcionB = "20",
        opcionC = "30",
        opcionD = "40",
        respuestaCorrecta = "A",
        verificada = true,
        estado = estado,
        imagenesJson = imagenesJson,
        requiereImagen = requiereImagen,
        bancoVersion = version
    )

    @Test
    fun manifiesto_seParseaDesdeFirestore() {
        val map = mapOf(
            "bank_version" to "udea-2026-10-v1",
            "schema_version" to 2L,
            "status" to "active",
            "hash" to "sha256:abc",
            "published_at" to "2026-10-25T00:00:00Z",
            "min_app_version" to 1L,
            "image_base_url" to "https://rutaudea-25fe0.web.app/questions/udea-2026-10-v1",
            "counts" to mapOf(
                "total" to 400L,
                "aprobadas" to 396L,
                "razonamiento_logico" to 197L,
                "competencia_lectora" to 199L
            )
        )
        val m = BankManifest.desdeMapa(map)
        assertNotNull(m)
        assertEquals("udea-2026-10-v1", m!!.bankVersion)
        assertEquals(2, m.schemaVersion)
        assertTrue(m.estaActivo)
        assertEquals(396, m.counts.esperadas)
        assertEquals(1, m.minAppVersion)
    }

    @Test
    fun manifiesto_sinCamposMinimosEsNulo() {
        assertNull(BankManifest.desdeMapa(mapOf("status" to "active")))
        assertNull(BankManifest.desdeMapa(null))
    }

    @Test
    fun validador_aceptaLoteCompleto() {
        val lote = listOf(
            entidad(id = "T1-P01"),
            entidad(id = "T1-P02", estado = Question.ESTADO_APROBADO_CON_IMAGEN, requiereImagen = true, imagenesJson = "[{\"url\":\"https://host/x.png\",\"rol\":\"pregunta\",\"orden\":1}]")
        )
        val resultado = BankValidator.validar(manifiesto, lote, appVersionCode = 1)
        assertTrue(resultado is ResultadoValidacion.Valido)
        assertEquals(2, (resultado as ResultadoValidacion.Valido).total)
    }

    @Test
    fun validador_rechazaConteoDistinto() {
        val resultado = BankValidator.validar(manifiesto, listOf(entidad()), appVersionCode = 1)
        assertTrue(resultado is ResultadoValidacion.Invalido)
        assertTrue(
            (resultado as ResultadoValidacion.Invalido).motivos
                .any { it.startsWith("conteo_no_coincide") }
        )
    }

    @Test
    fun validador_rechazaVersionDistinta() {
        val lote = listOf(
            entidad(id = "T1-P01", version = "udea-2026-09-v1"),
            entidad(id = "T1-P02")
        )
        val resultado = BankValidator.validar(manifiesto, lote, appVersionCode = 1)
        assertTrue(resultado is ResultadoValidacion.Invalido)
        assertTrue(
            (resultado as ResultadoValidacion.Invalido).motivos
                .any { it.startsWith("version_distinta") }
        )
    }

    @Test
    fun validador_rechazaEsquemaNoSoportado() {
        val otro = manifiesto.copy(schemaVersion = 3)
        val resultado = BankValidator.validar(otro, listOf(entidad()), appVersionCode = 1)
        assertTrue(resultado is ResultadoValidacion.Invalido)
        assertTrue(
            (resultado as ResultadoValidacion.Invalido).motivos
                .any { it.startsWith("schema_no_soportado") }
        )
    }

    @Test
    fun validador_rechazaAppVieja() {
        val otro = manifiesto.copy(minAppVersion = 9)
        val resultado = BankValidator.validar(otro, listOf(entidad()), appVersionCode = 1)
        assertTrue(resultado is ResultadoValidacion.Invalido)
        assertTrue(
            (resultado as ResultadoValidacion.Invalido).motivos
                .any { it.startsWith("app_desactualizada") }
        )
    }

    @Test
    fun validador_rechazaPreguntaSinImagenCuandoLaRequiere() {
        val lote = listOf(
            entidad(
                id = "T1-P01",
                estado = Question.ESTADO_APROBADO_CON_IMAGEN,
                requiereImagen = true
            )
        )
        val dos = manifiesto.copy(counts = BankCounts(total = 1, aprobadas = 1))
        val resultado = BankValidator.validar(dos, lote, appVersionCode = 1)
        assertTrue(resultado is ResultadoValidacion.Invalido)
        assertTrue(
            (resultado as ResultadoValidacion.Invalido).motivos.any { it.startsWith("sin_imagen") }
        )
    }

    @Test
    fun mapperFirestore_aceptaOpcionesObjetoEImagenes() {
        val data = mapOf(
            "area" to "razonamiento_logico",
            "subtema" to "RL15 Gráficas y tablas",
            "dificultad" to "media",
            "pregunta" to "¿Cuántos partidos?",
            "opciones" to mapOf("A" to "3", "B" to "4", "C" to "5", "D" to "6"),
            "respuesta_correcta" to "D",
            "estado" to "aprobado_con_imagen",
            "verificada" to true,
            "bank_version" to "udea-2026-10-v1",
            "contexto_ids" to listOf("T2-C01"),
            "imagenes" to listOf(
                mapOf(
                    "url" to "https://host/T2-C01.png",
                    "rol" to "contexto",
                    "orden" to 1L,
                    "alt" to "Tabla",
                    "path_origen" to "imagenes/T2/T2-C01.png"
                )
            ),
            "requiere_imagen" to true,
            "opciones_en_imagen" to false,
            "numero" to 1L,
            "confianza_modelo" to "alta",
            "revisado_por" to "Michael"
        )

        val e = PreguntaFirestoreMapper.desdeDocumento("T2-P01", data)
        assertNotNull(e)
        assertEquals("6", e!!.opcionD)
        assertEquals("T2-C01", e.contextoId)
        assertEquals("aprobado_con_imagen", e.estado)
        assertTrue(e.requiereImagen)
        assertEquals(1, e.numero)
        assertTrue(e.imagenesJson.contains("\"rol\":\"contexto\""))
    }

    @Test
    fun mapperFirestore_aceptaOpcionesListaDelBancoViejo() {
        val data = mapOf(
            "area" to "competencia_lectora",
            "subtema" to "CL01",
            "dificultad" to "baja",
            "pregunta" to "p",
            "opciones" to listOf("1", "2", "3", "4"),
            "respuesta_correcta" to "A"
        )
        val e = PreguntaFirestoreMapper.desdeDocumento("viejo-1", data)
        assertNotNull(e)
        assertEquals("4", e!!.opcionD)
        assertEquals("pendiente", e.estado)
    }

    @Test
    fun mapperFirestore_descartaDocumentosSinCamposMinimos() {
        assertNull(PreguntaFirestoreMapper.desdeDocumento("x", mapOf("area" to "otra")))
        assertNull(PreguntaFirestoreMapper.desdeDocumento("x", mapOf("area" to "razonamiento_logico")))
        assertNull(PreguntaFirestoreMapper.desdeDocumento("", mapOf("area" to "razonamiento_logico")))
    }
}
