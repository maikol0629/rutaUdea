package com.udea.rutaudea.ui.screen.info

import com.google.gson.Gson
import com.udea.rutaudea.data.source.local.InfoContentLoader
import com.udea.rutaudea.domain.model.TemaEducativo
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests del módulo Info: parseo del contenido educativo (JSON del asset)
 * y filtrado por área.
 */
class InfoViewModelTest {

    private val loader = InfoContentLoader(context = mockk(), gson = Gson())

    private val jsonFixture = """
        [
          {"id": "RL01", "nombre": "RL01 Proporcionalidad", "descripcion": "d", "estrategia": "e", "erroresComunes": "x"},
          {"id": "RL02", "nombre": "RL02 Porcentajes", "descripcion": "d", "estrategia": "e", "erroresComunes": "x"},
          {"id": "CL02", "nombre": "CL02 Inferencia", "descripcion": "d", "estrategia": "e", "erroresComunes": "x"}
        ]
    """.trimIndent()

    @Test
    fun `parser carga todos los temas del fixture`() {
        val temas = loader.parse(jsonFixture)

        assertEquals(3, temas.size)
        assertEquals("RL01 Proporcionalidad", temas[0].nombre)
        assertEquals("CL02 Inferencia", temas[2].nombre)
    }

    @Test
    fun `el area se infiere del codigo cuando no viene en el json`() {
        val temas = loader.parse(jsonFixture)

        assertEquals(TemaEducativo.AREA_RL, temas[0].area)
        assertEquals(TemaEducativo.AREA_CL, temas[2].area)
    }

    @Test
    fun `filtra por area razonamiento logico`() {
        val temas = loader.parse(jsonFixture)

        val rl = InfoViewModel.filtrarPorArea(temas, TemaEducativo.AREA_RL)

        assertEquals(listOf("RL01", "RL02"), rl.map { it.id })
    }

    @Test
    fun `filtra por area competencia lectora`() {
        val temas = loader.parse(jsonFixture)

        val cl = InfoViewModel.filtrarPorArea(temas, TemaEducativo.AREA_CL)

        assertEquals(listOf("CL02"), cl.map { it.id })
    }

    @Test
    fun `json invalido devuelve lista vacia sin lanzar`() {
        assertTrue(loader.parse("no es json").isEmpty())
        assertTrue(loader.parse("").isEmpty())
    }

    @Test
    fun `temas con id o nombre nulo se omiten`() {
        val jsonConNulos = """
            [
              {"id": null, "nombre": "sin id"},
              {"id": "RL03", "nombre": null},
              {"id": "RL04", "nombre": "RL04 Válido"}
            ]
        """.trimIndent()

        val temas = loader.parse(jsonConNulos)

        assertEquals(1, temas.size)
        assertEquals("RL04", temas[0].id)
    }

    @Test
    fun `estructura del asset real - 29 componentes, 15 RL y 14 CL`() {
        val asset = java.io.File(
            "src/main/assets/info/contenido_educativo.json"
        )
        if (!asset.exists()) return // en CI el working dir puede variar

        val temas = loader.parse(asset.readText())
        val ids = temas.map { it.id }

        assertEquals(29, temas.size)
        assertEquals(15, temas.count { it.area == TemaEducativo.AREA_RL })
        assertEquals(14, temas.count { it.area == TemaEducativo.AREA_CL })
        assertTrue(ids.distinct().size == ids.size) // sin duplicados
    }
}
