package com.udea.rutaudea.data.sync

/**
 * Documento `bank_manifest/active` (D-02 §3.1).
 *
 * Solo se publica cuando el lote pasó los gates de validación; la app lo
 * lee para decidir si debe sincronizar y qué versión mostrar.
 */
data class BankManifest(
    val bankVersion: String,
    val schemaVersion: Int,
    val status: String,
    val hash: String? = null,
    val counts: BankCounts = BankCounts(),
    val publishedAt: String? = null,
    val minAppVersion: Int = 0,
    val imageBaseUrl: String? = null
) {
    val estaActivo: Boolean get() = status == "active"
    val versionSoportada: Boolean get() = schemaVersion == SCHEMA_VERSION_SOPORTADO

    companion object {
        const val COLECCION = "bank_manifest"
        const val DOCUMENTO_ACTIVO = "active"
        const val SCHEMA_VERSION_SOPORTADO = 2

        /** Convierte el mapa de Firestore; `null` si falta lo mínimo. */
        fun desdeMapa(data: Map<String, Any?>?): BankManifest? {
            if (data == null) return null
            val version = data["bank_version"] as? String
                ?: data["bankVersion"] as? String
                ?: return null
            val status = data["status"] as? String ?: return null
            val schema = (data["schema_version"] as? Number)?.toInt()
                ?: (data["schemaVersion"] as? Number)?.toInt()
                ?: 0
            return BankManifest(
                bankVersion = version,
                schemaVersion = schema,
                status = status,
                hash = data["hash"] as? String,
                counts = BankCounts.desdeMapa(data["counts"] as? Map<*, *>),
                publishedAt = data["published_at"] as? String,
                minAppVersion = (data["min_app_version"] as? Number)?.toInt() ?: 0,
                imageBaseUrl = data["image_base_url"] as? String
            )
        }
    }
}

/** Conteos declarados por el manifiesto. */
data class BankCounts(
    val total: Int = 0,
    val razonamientoLogico: Int = 0,
    val competenciaLectora: Int = 0,
    val aprobadas: Int = 0
) {
    /** Cantidad que la app debe descargar (solo estados publicables). */
    val esperadas: Int get() = if (aprobadas > 0) aprobadas else total

    companion object {
        fun desdeMapa(map: Map<*, *>?): BankCounts {
            if (map == null) return BankCounts()
            return BankCounts(
                total = (map["total"] as? Number)?.toInt() ?: 0,
                razonamientoLogico = (map["razonamiento_logico"] as? Number)?.toInt() ?: 0,
                competenciaLectora = (map["competencia_lectora"] as? Number)?.toInt() ?: 0,
                aprobadas = (map["aprobadas"] as? Number)?.toInt() ?: 0
            )
        }
    }
}
