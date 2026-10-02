package com.udea.rutaudea.data.source.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Carga el contenido educativo del módulo Info desde el asset
 * `info/contenido_educativo.json` (arreglo JSON de componentes).
 *
 * El contenido viaja empaquetado en la APK (offline-first), igual que
 * el banco de preguntas; la colección `educationalContent` de Firestore
 * queda para el futuro panel administrativo.
 */
class InfoContentLoader(private val context: Context, private val gson: Gson) {

    suspend fun loadFromAsset(assetPath: String = ASSET_PATH): List<com.udea.rutaudea.domain.model.TemaEducativo> =
        withContext(Dispatchers.IO) {
            val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            parse(text)
        }

    /** Parsea el texto JSON (arreglo de objetos) a temas educativos. */
    fun parse(text: String): List<com.udea.rutaudea.domain.model.TemaEducativo> = try {
        val array = gson.fromJson(text, com.google.gson.JsonArray::class.java) ?: return emptyList()
        array.mapNotNull { element ->
            val json = element as? JsonObject ?: return@mapNotNull null
            val id = json.getStringOrNull("id") ?: return@mapNotNull null
            val nombre = json.getStringOrNull("nombre") ?: return@mapNotNull null
            com.udea.rutaudea.domain.model.TemaEducativo(
                id = id,
                nombre = nombre,
                area = json.getStringOrNull("area")
                    ?: if (id.startsWith("RL")) com.udea.rutaudea.domain.model.TemaEducativo.AREA_RL
                    else com.udea.rutaudea.domain.model.TemaEducativo.AREA_CL,
                descripcion = json.getStringOrNull("descripcion").orEmpty(),
                estrategia = json.getStringOrNull("estrategia").orEmpty(),
                erroresComunes = json.getStringOrNull("erroresComunes").orEmpty()
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun JsonObject.getStringOrNull(key: String): String? {
        val element = get(key) ?: return null
        return if (element is JsonNull) null else element.asString
    }

    companion object {
        const val ASSET_PATH = "info/contenido_educativo.json"
    }
}
