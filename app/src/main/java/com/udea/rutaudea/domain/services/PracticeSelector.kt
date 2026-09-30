package com.udea.rutaudea.domain.services

import com.udea.rutaudea.domain.model.Question
import kotlin.random.Random

/**
 * Motor de selección de preguntas para el módulo de Práctica
 * (plan de desarrollo, sección 10).
 *
 * Diferencias con el simulacro ([QuestionSelector]):
 * - El usuario NO elige dificultad: las preguntas se **estratifican por
 *   dificultad proporcionalmente a la composición real del pool** filtrado
 *   (área + subtema), usando el método de mayor residuo.
 * - La cantidad es elegida por el usuario (p. ej. 5/10/20).
 * - No hay exclusión de preguntas recientes ni cuotas por subtema:
 *   el pool ya viene filtrado; la práctica admite repetición entre sesiones
 *   (nunca dentro de la misma sesión).
 */
class PracticeSelector(
    private val random: Random = Random.Default
) {

    /**
     * Selecciona [cantidad] preguntas de [pool] (ya filtrado por área y
     * subtema), estratificadas por dificultad de forma proporcional.
     *
     * Si el pool tiene menos preguntas que [cantidad], devuelve todas las
     * disponibles. Si el pool está vacío (o no hay válidas), devuelve una
     * lista vacía.
     */
    fun select(pool: List<Question>, cantidad: Int): List<Question> {
        val validas = pool.filter { esValida(it) }
        if (validas.isEmpty() || cantidad <= 0) return emptyList()

        val objetivo = minOf(cantidad, validas.size)
        val porDificultad = validas.groupBy { it.dificultad }

        // Cuota proporcional por dificultad (método de mayor residuo).
        val cuotas = cuotasProporcionales(porDificultad.mapValues { it.value.size }, objetivo)

        val seleccion = mutableListOf<Question>()
        for ((dificultad, cuota) in cuotas) {
            val subPool = porDificultad[dificultad].orEmpty().shuffled(random)
            seleccion += subPool.take(cuota)
        }

        // Relleno flexible: si algún estrato quedó corto, completar con
        // preguntas aún no seleccionadas (cualquier dificultad).
        if (seleccion.size < objetivo) {
            val usados = seleccion.map { it.id }.toSet()
            val restantes = validas.filter { it.id !in usados }.shuffled(random)
            seleccion += restantes.take(objetivo - seleccion.size)
        }

        // Sin repetidos (defensivo) y orden aleatorio final.
        return seleccion.distinctBy { it.id }.shuffled(random)
    }

    /**
     * Reparte [objetivo] unidades proporcionalmente según [conteos]
     * usando el método de mayor residuo (largest remainder).
     */
    private fun cuotasProporcionales(conteos: Map<String, Int>, objetivo: Int): Map<String, Int> {
        val total = conteos.values.sum()
        if (total == 0 || objetivo <= 0) return emptyMap()
        val bruto = conteos.mapValues { it.value.toDouble() * objetivo / total }
        val base = bruto.mapValues { it.value.toInt() }.toMutableMap()
        var residuo = objetivo - base.values.sum()
        val orden = bruto.entries
            .sortedByDescending { it.value - it.value.toInt() }
            .map { it.key }
        var i = 0
        while (residuo > 0 && orden.isNotEmpty()) {
            val clave = orden[i % orden.size]
            base[clave] = base.getValue(clave) + 1
            residuo--
            i++
        }
        return base
    }

    /** Una pregunta es válida si tiene 4 opciones y respuesta A–D. */
    private fun esValida(question: Question): Boolean =
        question.opciones.size >= 4 && question.respuestaCorrecta in LETRAS_VALIDAS

    companion object {
        /** Cantidades ofrecidas en la UI de práctica. */
        val CANTIDADES = listOf(5, 10, 20)
        private val LETRAS_VALIDAS = listOf("A", "B", "C", "D")
    }
}
