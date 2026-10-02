package com.udea.rutaudea.domain.services

import com.udea.rutaudea.domain.model.Question
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests del motor de selección de práctica: estratificación de
 * dificultad proporcional al pool (mayor residuo), sin repetidos
 * y con relleno flexible.
 */
class PracticeSelectorTest {

    private fun pregunta(
        id: String,
        dificultad: String,
        area: String = "razonamiento_logico",
        subtema: String = "Series"
    ) = Question(
        id = id,
        area = area,
        subtema = subtema,
        dificultad = dificultad,
        pregunta = "¿$id?",
        opciones = listOf("a", "b", "c", "d"),
        respuestaCorrecta = "A"
    )

    @Test
    fun `estratifica la dificultad proporcionalmente al pool`() {
        // Pool: 6 baja, 3 media, 1 alta → cantidad 10 (todo el pool).
        val pool = (1..6).map { pregunta("b$it", "baja") } +
            (1..3).map { pregunta("m$it", "media") } +
            listOf(pregunta("a1", "alta"))

        val seleccion = PracticeSelector(Random(42)).select(pool, 10)

        assertEquals(10, seleccion.size)
        assertEquals(6, seleccion.count { it.dificultad == "baja" })
        assertEquals(3, seleccion.count { it.dificultad == "media" })
        assertEquals(1, seleccion.count { it.dificultad == "alta" })
    }

    @Test
    fun `mayor residuo asigna la unidad restante al estrato con mayor fraccion`() {
        // Pool: 4 baja, 1 media → cantidad 3.
        // Bruto: baja = 3*4/5 = 2.4, media = 3*1/5 = 0.6.
        // Base: 2 baja + 0 media; residuo 1 → media (0.6 > 0.4).
        val pool = (1..4).map { pregunta("b$it", "baja") } +
            listOf(pregunta("m1", "media"))

        val seleccion = PracticeSelector(Random(7)).select(pool, 3)

        assertEquals(3, seleccion.size)
        assertEquals(2, seleccion.count { it.dificultad == "baja" })
        assertEquals(1, seleccion.count { it.dificultad == "media" })
    }

    @Test
    fun `cantidad mayor que el pool devuelve todo el pool sin repetidos`() {
        val pool = (1..7).map { pregunta("q$it", "media") }

        val seleccion = PracticeSelector(Random(1)).select(pool, 20)

        assertEquals(7, seleccion.size)
        assertEquals(7, seleccion.distinctBy { it.id }.size)
    }

    @Test
    fun `pool vacio devuelve lista vacia`() {
        val seleccion = PracticeSelector(Random(1)).select(emptyList(), 10)
        assertTrue(seleccion.isEmpty())
    }

    @Test
    fun `cantidad invalida devuelve lista vacia`() {
        val pool = listOf(pregunta("q1", "baja"))
        assertTrue(PracticeSelector(Random(1)).select(pool, 0).isEmpty())
        assertTrue(PracticeSelector(Random(1)).select(pool, -5).isEmpty())
    }

    @Test
    fun `ignora preguntas invalidas`() {
        val invalida1 = pregunta("bad1", "baja").copy(opciones = listOf("a", "b"))
        val invalida2 = pregunta("bad2", "baja").copy(respuestaCorrecta = "Z")
        val validas = (1..4).map { pregunta("q$it", "baja") }

        val seleccion = PracticeSelector(Random(3)).select(validas + invalida1 + invalida2, 4)

        assertEquals(4, seleccion.size)
        assertTrue(seleccion.none { it.id in listOf("bad1", "bad2") })
    }

    @Test
    fun `sin repetidos dentro de la misma sesion`() {
        val pool = (1..10).map { pregunta("q$it", "media") }

        val seleccion = PracticeSelector(Random(5)).select(pool, 10)

        assertEquals(10, seleccion.distinctBy { it.id }.size)
    }
}
