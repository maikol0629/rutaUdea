package com.udea.rutaudea.domain.services

import com.udea.rutaudea.domain.model.Question
import kotlin.random.Random

/**
 * Motor de selección de las 80 preguntas del simulacro
 * (plan de desarrollo, sección 8).
 *
 * Reglas de selección:
 * 1. Distribución proporcional al banco: 40 RL + 40 CL, con dificultad
 *    repartida proporcionalmente a la composición real del banco
 *    (método de mayor residuo para el redondeo).
 * 2. Cobertura de subtemas: dentro de cada grupo (área + dificultad),
 *    la cuota se reparte proporcionalmente entre subtemas y la secuencia
 *    final se entrelaza para alternar subtemas.
 * 3. Sin preguntas repetidas dentro del mismo simulacro.
 * 4. Exclusión de preguntas usadas en los últimos 3 simulacros del
 *    usuario, cuando el banco lo permite (relajación gradual por área).
 * 5. Cuotas flexibles: si un grupo no tiene suficientes preguntas, se
 *    toma todo el grupo disponible y el faltante se rellena con las
 *    preguntas sobrantes (mismo área primero).
 * 6. Validación: la selección solo es exitosa si se alcanzan las 80
 *    preguntas con un banco válido.
 *
 * Clase pura de dominio (sin dependencias de Android/Firestore) para
 * facilitar las pruebas unitarias; la aleatoriedad es inyectable.
 */
class QuestionSelector(
    private val random: Random = Random.Default
) {

    // ----- API pública -----

    /**
     * Selecciona las [TOTAL_PREGUNTAS] preguntas del simulacro a partir
     * del banco aprobado, excluyendo [idsExcluidos] (preguntas de los
     * últimos simulacros) cuando el banco lo permite.
     */
    fun select(bank: List<Question>, idsExcluidos: Set<String> = emptySet()): Result<List<Question>> {
        val validas = bank.filter { esValida(it) }
        if (validas.size < TOTAL_PREGUNTAS) {
            return Result.failure(
                Exception(
                    "El banco válido tiene ${validas.size} preguntas y se necesitan $TOTAL_PREGUNTAS"
                )
            )
        }

        // Intento 1: respetando la exclusión de simulacros recientes.
        // Intento 2 (relajación): sin exclusión, si el banco excluido no alcanza.
        val seleccion = seleccionar(validas, idsExcluidos)
        val final = if (seleccion.size >= TOTAL_PREGUNTAS) seleccion else seleccionar(validas, emptySet())
        return Result.success(final.distinctBy { it.id })
    }

    // ----- NÚCLEO DEL ALGORITMO -----

    private fun seleccionar(validas: List<Question>, idsExcluidos: Set<String>): List<Question> {
        val porArea = validas.groupBy { it.area }
        val rl = seleccionarArea(porArea[AREA_RL].orEmpty(), PREGUNTAS_POR_AREA, idsExcluidos)
        val cl = seleccionarArea(porArea[AREA_CL].orEmpty(), PREGUNTAS_POR_AREA, idsExcluidos)

        val seleccion = (rl + cl).toMutableList()
        val faltantes = TOTAL_PREGUNTAS - seleccion.size
        if (faltantes > 0) {
            val usados = seleccion.map { it.id }.toSet()
            val sobrantes = validas
                .filter { it.id !in usados && it.id !in idsExcluidos }
                .ifEmpty { validas.filter { it.id !in usados } }
            seleccion += seleccionarPorSubtema(sobrantes, faltantes)
        }
        return seleccion
    }

    /**
     * Selecciona [objetivo] preguntas de un área con dificultad
     * proporcional al banco. Si el área no alcanza (cuotas flexibles),
     * devuelve todo el pool disponible.
     */
    private fun seleccionarArea(pool: List<Question>, objetivo: Int, idsExcluidos: Set<String>): List<Question> {
        if (pool.isEmpty()) return emptyList()
        val candidatos = pool.filter { it.id !in idsExcluidos }
        val usable = if (candidatos.size >= objetivo) candidatos else pool

        val porDificultad = usable.groupBy { it.dificultad }
        val cuotas = cuotasProporcionales(porDificultad.mapValues { it.value.size }, objetivo)
        val resultado = mutableListOf<Question>()
        for ((dificultad, cuota) in cuotas) {
            val subPool = porDificultad[dificultad].orEmpty()
            resultado += seleccionarPorSubtema(subPool, cuota)
        }
        return resultado
    }

    /**
     * Selecciona [cuota] preguntas de un pool repartiéndolas
     * proporcionalmente entre subtemas y entrelazando la secuencia.
     */
    private fun seleccionarPorSubtema(pool: List<Question>, cuota: Int): List<Question> {
        if (pool.isEmpty() || cuota <= 0) return emptyList()
        val objetivo = minOf(cuota, pool.size)
        val porSubtema = pool.groupBy { it.subtema }
        val cuotasSubtema = cuotasProporcionales(porSubtema.mapValues { it.value.size }, objetivo)

        val colas = porSubtema.mapValues { (_, preguntas) -> ArrayDeque(preguntas.shuffled(random)) }
        val restante = cuotasSubtema.toMutableMap()
        val seleccion = mutableListOf<Question>()

        while (seleccion.size < objetivo && restante.any { it.value > 0 }) {
            val orden = restante.entries.filter { it.value > 0 }.sortedByDescending { it.value }
            for ((subtema, _) in orden) {
                val siguiente = colas[subtema]?.removeFirstOrNull() ?: continue
                seleccion += siguiente
                restante[subtema] = restante[subtema]!! - 1
            }
        }
        return seleccion
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

    // ----- VALIDACIÓN -----

    /** Una pregunta es válida si tiene 4 opciones y respuesta A–D. */
    private fun esValida(question: Question): Boolean =
        question.opciones.size >= 4 && question.respuestaCorrecta in LETRAS_VALIDAS

    companion object {
        const val TOTAL_PREGUNTAS = 80
        const val PREGUNTAS_POR_AREA = 40
        const val AREA_RL = "razonamiento_logico"
        const val AREA_CL = "competencia_lectora"
        const val SIMULACROS_RECIENTES = 3
        private val LETRAS_VALIDAS = listOf("A", "B", "C", "D")
    }
}
