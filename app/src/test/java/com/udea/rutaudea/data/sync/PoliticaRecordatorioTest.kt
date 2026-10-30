package com.udea.rutaudea.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests de la política del recordatorio local (sin Android). */
class PoliticaRecordatorioTest {

    private val ahora = 1_800_000_000_000L
    private val dia = 24L * 60L * 60L * 1000L

    @Test
    fun `sin datos no se recuerda`() {
        assertFalse(
            PoliticaRecordatorio.debeRecordar(
                ultimaSyncMs = 0L,
                primeraUsoMs = 0L,
                ultimoAvisoMs = 0L,
                ahoraMs = ahora
            )
        )
    }

    @Test
    fun `dos dias desde el primer uso no alcanzan`() {
        assertFalse(
            PoliticaRecordatorio.debeRecordar(
                ultimaSyncMs = 0L,
                primeraUsoMs = ahora - 2 * dia,
                ultimoAvisoMs = 0L,
                ahoraMs = ahora
            )
        )
    }

    @Test
    fun `tres dias desde el primer uso activan el aviso`() {
        assertTrue(
            PoliticaRecordatorio.debeRecordar(
                ultimaSyncMs = 0L,
                primeraUsoMs = ahora - 3 * dia,
                ultimoAvisoMs = 0L,
                ahoraMs = ahora
            )
        )
    }

    @Test
    fun `una sincronizacion reciente evita el aviso aunque el uso sea viejo`() {
        assertFalse(
            PoliticaRecordatorio.debeRecordar(
                ultimaSyncMs = ahora - 1 * dia,
                primeraUsoMs = ahora - 30 * dia,
                ultimoAvisoMs = 0L,
                ahoraMs = ahora
            )
        )
    }

    @Test
    fun `una sincronizacion vieja activa el aviso`() {
        assertTrue(
            PoliticaRecordatorio.debeRecordar(
                ultimaSyncMs = ahora - 4 * dia,
                primeraUsoMs = ahora - 60 * dia,
                ultimoAvisoMs = 0L,
                ahoraMs = ahora
            )
        )
    }

    @Test
    fun `el aviso respeta el enfriamiento de 24 horas`() {
        assertFalse(
            PoliticaRecordatorio.debeRecordar(
                ultimaSyncMs = ahora - 10 * dia,
                primeraUsoMs = ahora - 60 * dia,
                ultimoAvisoMs = ahora - 23 * 60 * 60 * 1000L,
                ahoraMs = ahora
            )
        )
        assertTrue(
            PoliticaRecordatorio.debeRecordar(
                ultimaSyncMs = ahora - 10 * dia,
                primeraUsoMs = ahora - 60 * dia,
                ultimoAvisoMs = ahora - 25 * 60 * 60 * 1000L,
                ahoraMs = ahora
            )
        )
    }

    @Test
    fun `los dias se calculan por dias completos`() {
        assertEquals(
            2L,
            PoliticaRecordatorio.diasSinSincronizar(
                ultimaSyncMs = ahora - (2 * dia + 5 * 60 * 60 * 1000L),
                primeraUsoMs = 0L,
                ahoraMs = ahora
            )
        )
        assertEquals(
            0L,
            PoliticaRecordatorio.diasSinSincronizar(
                ultimaSyncMs = ahora + 1000L,
                primeraUsoMs = 0L,
                ahoraMs = ahora
            )
        )
    }
}
