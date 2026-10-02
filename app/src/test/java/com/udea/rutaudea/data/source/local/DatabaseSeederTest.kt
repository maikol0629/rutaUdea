package com.udea.rutaudea.data.source.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de la política de siembra versionada del banco:
 * primera ejecución, re-siembra por cambio de versión y no-op
 * cuando ya está en la versión actual.
 */
class DatabaseSeederTest {

    @Test
    fun `siembra en la primera ejecucion (tabla vacia)`() {
        assertTrue(DatabaseSeeder.debeSembrar(preguntasEnBd = 0, versionInstalada = 0))
    }

    @Test
    fun `re-siembra si el banco instalado es de una version anterior`() {
        // Dispositivo con la app ya instalada y banco v1 → se actualiza a v2
        // sin reinstalar ni borrar datos.
        assertTrue(DatabaseSeeder.debeSembrar(preguntasEnBd = 200, versionInstalada = 1))
    }

    @Test
    fun `no re-siembra si ya esta en la version actual del banco`() {
        assertFalse(DatabaseSeeder.debeSembrar(preguntasEnBd = 200, versionInstalada = 2))
    }

    @Test
    fun `no re-siembra si la version instalada es mayor (downgrade de APK)`() {
        // Evita sobrescribir un banco más nuevo (p. ej. APK vieja instalada
        // sobre datos de una versión superior).
        assertFalse(DatabaseSeeder.debeSembrar(preguntasEnBd = 200, versionInstalada = 3))
    }
}
