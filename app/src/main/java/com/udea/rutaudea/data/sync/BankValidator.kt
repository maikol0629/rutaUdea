package com.udea.rutaudea.data.sync

import com.udea.rutaudea.data.local.entity.QuestionEntity
import com.udea.rutaudea.domain.model.Question

/**
 * Validación en cliente del lote descargado (D-02 §4.2 paso 7).
 *
 * Es una función pura para poder cubrirla con tests JVM: la app no debe
 * reemplazar Room si el lote no cumple el contrato.
 */
sealed class ResultadoValidacion {
    data class Valido(val total: Int) : ResultadoValidacion()
    data class Invalido(val motivos: List<String>) : ResultadoValidacion()
}

object BankValidator {

    fun validar(
        manifest: BankManifest,
        preguntas: List<QuestionEntity>,
        appVersionCode: Int
    ): ResultadoValidacion {
        val motivos = mutableListOf<String>()

        if (!manifest.estaActivo) motivos += "manifiesto_no_activo:${manifest.status}"
        if (!manifest.versionSoportada) {
            motivos += "schema_no_soportado:${manifest.schemaVersion}"
        }
        if (manifest.minAppVersion > appVersionCode) {
            motivos += "app_desactualizada:min=${manifest.minAppVersion},actual=$appVersionCode"
        }
        if (preguntas.isEmpty()) motivos += "lote_vacio"

        val ids = mutableSetOf<String>()
        var aprobadas = 0
        preguntas.forEach { p ->
            if (!ids.add(p.id)) motivos += "id_duplicado:${p.id}"
            if (p.bancoVersion.isBlank()) {
                motivos += "sin_banco_version:${p.id}"
            } else if (p.bancoVersion != manifest.bankVersion) {
                motivos += "version_distinta:${p.id}"
            }
            if (p.estado !in Question.ESTADOS_PUBLICABLES) {
                motivos += "estado_no_publicable:${p.id}:${p.estado}"
            } else {
                aprobadas++
            }
            if (p.subtema.isBlank()) motivos += "sin_subtema:${p.id}"
            if (p.dificultad.isBlank()) motivos += "sin_dificultad:${p.id}"
            if (!p.verificada) motivos += "sin_verificar:${p.id}"
            if (p.respuestaCorrecta !in LETRAS) motivos += "respuesta_invalida:${p.id}"
            if (listOf(p.opcionA, p.opcionB, p.opcionC, p.opcionD).any { it.isBlank() }) {
                motivos += "opciones_incompletas:${p.id}"
            }
            val necesitaImagen = p.requiereImagen ||
                p.estado == Question.ESTADO_APROBADO_CON_IMAGEN
            val tieneImagen = (p.imagenesJson.isNotBlank() && p.imagenesJson != "[]") ||
                !p.imagenUrl.isNullOrBlank()
            if (necesitaImagen && !tieneImagen) motivos += "sin_imagen:${p.id}"
        }

        if (motivos.isEmpty() && manifest.counts.esperadas > 0 && aprobadas != manifest.counts.esperadas) {
            motivos += "conteo_no_coincide:esperado=${manifest.counts.esperadas},recibido=$aprobadas"
        }

        return if (motivos.isEmpty()) {
            ResultadoValidacion.Valido(aprobadas)
        } else {
            ResultadoValidacion.Invalido(motivos.distinct().take(20))
        }
    }

    private val LETRAS = setOf("A", "B", "C", "D")
}
