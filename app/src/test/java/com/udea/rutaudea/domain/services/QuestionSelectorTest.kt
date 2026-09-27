package com.udea.rutaudea.domain.services

import com.udea.rutaudea.domain.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuestionSelectorTest {

    private val selector = QuestionSelector(random = Random(42))

    private fun pregunta(
        id: String,
        area: String,
        subtema: String,
        dificultad: String
    ) = Question(
        id = id,
        area = area,
        subtema = subtema,
        dificultad = dificultad,
        pregunta = "Pregunta $id",
        opciones = listOf("A1", "B1", "C1", "D1"),
        respuestaCorrecta = "A"
    )

    private fun bancoValido(): List<Question> {
        val rl = mutableListOf<Question>()
        val cl = mutableListOf<Question>()
        val dificultadRl = listOf("baja", "media", "alta")
        val dificultadCl = listOf("baja", "media", "alta")
        for (i in 1..60) {
            rl += pregunta("rl_$i", QuestionSelector.AREA_RL, "RL${(i % 5) + 1}", dificultadRl[i % 3])
            cl += pregunta("cl_$i", QuestionSelector.AREA_CL, "CL${(i % 5) + 1}", dificultadCl[i % 3])
        }
        return rl + cl
    }

    @Test
    fun `selecciona exactamente 80 preguntas 40 por area`() {
        val resultado = selector.select(bancoValido())
        assertTrue(resultado.isSuccess)
        val seleccion = resultado.getOrThrow()
        assertEquals(QuestionSelector.TOTAL_PREGUNTAS, seleccion.size)
        assertEquals(40, seleccion.count { it.area == QuestionSelector.AREA_RL })
        assertEquals(40, seleccion.count { it.area == QuestionSelector.AREA_CL })
    }

    @Test
    fun `distribucion de dificultad proporcional al banco`() {
        // Banco RL: 10 baja / 30 media / 10 alta => cuota 40: 8 baja / 24 media / 8 alta
        val rl = mutableListOf<Question>()
        for (i in 1..10) rl += pregunta("rl_b_$i", QuestionSelector.AREA_RL, "RL1", "baja")
        for (i in 1..30) rl += pregunta("rl_m_$i", QuestionSelector.AREA_RL, "RL2", "media")
        for (i in 1..10) rl += pregunta("rl_a_$i", QuestionSelector.AREA_RL, "RL3", "alta")
        val cl = bancoValido().filter { it.area == QuestionSelector.AREA_CL }
        val resultado = selector.select(rl + cl)
        val seleccion = resultado.getOrThrow()
        val rlSeleccionadas = seleccion.filter { it.area == QuestionSelector.AREA_RL }
        assertEquals(8, rlSeleccionadas.count { it.dificultad == "baja" })
        assertEquals(24, rlSeleccionadas.count { it.dificultad == "media" })
        assertEquals(8, rlSeleccionadas.count { it.dificultad == "alta" })
    }

    @Test
    fun `sin preguntas repetidas dentro del simulacro`() {
        val seleccion = selector.select(bancoValido()).getOrThrow()
        assertEquals(seleccion.size, seleccion.map { it.id }.distinct().size)
    }

    @Test
    fun `excluye preguntas de simulacros recientes cuando el banco lo permite`() {
        val banco = bancoValido()
        // Excluye 20 RL + 20 CL: cada área conserva 40 preguntas no excluidas
        val excluidas = (
            banco.filter { it.area == QuestionSelector.AREA_RL }.take(20) +
                banco.filter { it.area == QuestionSelector.AREA_CL }.take(20)
            ).map { it.id }.toSet()
        val seleccion = selector.select(banco, excluidas).getOrThrow()
        assertTrue(seleccion.none { it.id in excluidas })
        assertEquals(QuestionSelector.TOTAL_PREGUNTAS, seleccion.size)
    }

    @Test
    fun `relaja la exclusion si el banco excluido no alcanza`() {
        val banco = bancoValido()
        val excluidas = banco.take(130).map { it.id }.toSet()
        val seleccion = selector.select(banco, excluidas).getOrThrow()
        assertEquals(QuestionSelector.TOTAL_PREGUNTAS, seleccion.size)
    }

    @Test
    fun `cuotas flexibles rellena con preguntas sobrantes de otro area`() {
        // Solo 30 RL válidas: las 10 restantes se rellenan con CL
        val rl = (1..30).map { pregunta("rl_$it", QuestionSelector.AREA_RL, "RL1", "media") }
        val cl = (1..60).map { pregunta("cl_$it", QuestionSelector.AREA_CL, "CL1", "media") }
        val seleccion = selector.select(rl + cl).getOrThrow()
        assertEquals(QuestionSelector.TOTAL_PREGUNTAS, seleccion.size)
        assertEquals(30, seleccion.count { it.area == QuestionSelector.AREA_RL })
    }

    @Test
    fun `falla la validacion si el banco valido no alcanza las 80`() {
        val banco = (1..60).map { pregunta("q_$it", QuestionSelector.AREA_RL, "RL1", "media") }
        val resultado = selector.select(banco)
        assertTrue(resultado.isFailure)
    }

    @Test
    fun `filtra preguntas invalidas del banco`() {
        val banco = bancoValido() + listOf(
            pregunta("invalida_1", QuestionSelector.AREA_RL, "RL1", "baja").copy(
                opciones = listOf("A1", "B1")
            ),
            pregunta("invalida_2", QuestionSelector.AREA_CL, "CL1", "baja").copy(
                respuestaCorrecta = "E"
            )
        )
        val seleccion = selector.select(banco).getOrThrow()
        assertTrue(seleccion.none { it.id.startsWith("invalida") })
        assertEquals(QuestionSelector.TOTAL_PREGUNTAS, seleccion.size)
    }

    @Test
    fun `cubre todos los subtemas cuando el banco lo permite`() {
        val seleccion = selector.select(bancoValido()).getOrThrow()
        val subtemasRl = seleccion.filter { it.area == QuestionSelector.AREA_RL }.map { it.subtema }.distinct()
        val subtemasCl = seleccion.filter { it.area == QuestionSelector.AREA_CL }.map { it.subtema }.distinct()
        assertTrue(subtemasRl.size >= 5)
        assertTrue(subtemasCl.size >= 5)
    }

    @Test
    fun `determinista con la misma semilla`() {
        val s1 = QuestionSelector(random = Random(7)).select(bancoValido()).getOrThrow()
        val s2 = QuestionSelector(random = Random(7)).select(bancoValido()).getOrThrow()
        assertEquals(s1.map { it.id }, s2.map { it.id })
    }

    @Test
    fun `la seleccion respeta el orden RL primero y CL despues`() {
        val seleccion = selector.select(bancoValido()).getOrThrow()
        val areas = seleccion.map { it.area }.distinct()
        assertEquals(listOf(QuestionSelector.AREA_RL, QuestionSelector.AREA_CL), areas)
        assertFalse(seleccion.isEmpty())
    }
}
