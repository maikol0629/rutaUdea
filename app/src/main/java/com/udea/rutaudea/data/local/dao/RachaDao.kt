package com.udea.rutaudea.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.udea.rutaudea.data.local.entity.RachaEntity

/**
 * DAO de la racha de actividad (una fila por usuario).
 *
 * Es la fuente primaria de lectura (funciona sin conexión); Firestore se
 * usa para respaldar y sincronizar entre dispositivos.
 */
@Dao
interface RachaDao {

    @Query("SELECT * FROM rachas_cache WHERE uid = :uid")
    suspend fun getRacha(uid: String): RachaEntity?

    @Upsert
    suspend fun upsert(racha: RachaEntity)

    @Query("UPDATE rachas_cache SET synced = 1 WHERE uid = :uid")
    suspend fun marcarSincronizada(uid: String)

    @Query("DELETE FROM rachas_cache WHERE uid = :uid")
    suspend fun limpiar(uid: String)
}
