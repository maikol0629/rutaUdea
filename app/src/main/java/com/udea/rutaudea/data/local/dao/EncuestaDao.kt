package com.udea.rutaudea.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.udea.rutaudea.data.local.entity.EncuestaEntity

/**
 * DAO del cuestionario de satisfacción (una fila por usuario).
 *
 * Es la fuente primaria de lectura (funciona sin conexión); Firestore se
 * usa para respaldar y analizar las respuestas de la prueba piloto.
 */
@Dao
interface EncuestaDao {

    @Query("SELECT * FROM encuestas_cache WHERE uid = :uid")
    suspend fun getEncuesta(uid: String): EncuestaEntity?

    @Upsert
    suspend fun upsert(encuesta: EncuestaEntity)

    @Query("UPDATE encuestas_cache SET synced = 1 WHERE uid = :uid")
    suspend fun marcarSincronizada(uid: String)
}
