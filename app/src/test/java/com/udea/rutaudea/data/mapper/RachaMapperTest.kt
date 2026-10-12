package com.udea.rutaudea.data.mapper

import com.udea.rutaudea.data.local.entity.RachaEntity
import com.udea.rutaudea.domain.model.Racha
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests de la conversión entre la entidad de racha (Room) y el dominio. */
class RachaMapperTest {

    @Test
    fun `una entidad nula se convierte en una racha vacia`() {
        val racha = RachaMapper.toDomain(null)

        assertEquals(Racha(), racha)
        assertFalse(racha.tieneActividad)
    }

    @Test
    fun `convierte una entidad a dominio`() {
        val entidad = RachaEntity(
            uid = "u1",
            actual = 4,
            mejor = 9,
            ultimoDia = 20_000L,
            ultimaActividadMs = 123L,
            synced = true
        )

        val racha = RachaMapper.toDomain(entidad)

        assertEquals(4, racha.actual)
        assertEquals(9, racha.mejor)
        assertEquals(20_000L, racha.ultimoDia)
        assertEquals(123L, racha.ultimaActividadMs)
        assertTrue(racha.tieneActividad)
    }

    @Test
    fun `convierte el dominio a entidad conservando el uid y el estado`() {
        val racha = Racha(actual = 2, mejor = 6, ultimoDia = 100L, ultimaActividadMs = 999L)

        val entidad = RachaMapper.toEntity("u7", racha, synced = false)

        assertEquals("u7", entidad.uid)
        assertEquals(2, entidad.actual)
        assertEquals(6, entidad.mejor)
        assertEquals(100L, entidad.ultimoDia)
        assertEquals(999L, entidad.ultimaActividadMs)
        assertFalse(entidad.synced)
    }

    @Test
    fun `una racha sin actividad no tiene ultimo dia`() {
        val entidad = RachaMapper.toEntity("u8", Racha(), synced = true)

        assertNull(entidad.ultimoDia)
        assertTrue(entidad.synced)
    }
}
