package com.udea.rutaudea

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil.Coil
import coil.request.ImageRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.udea.rutaudea.data.local.RutaUdeaDatabase
import com.udea.rutaudea.data.local.entity.QuestionEntity
import com.udea.rutaudea.data.mapper.QuestionMapper
import com.udea.rutaudea.data.repository.QuestionRepository
import com.udea.rutaudea.data.source.local.DatabaseSeeder
import com.udea.rutaudea.data.sync.BankSyncManager
import com.udea.rutaudea.data.sync.BankSyncScheduler
import com.udea.rutaudea.data.sync.Conectividad
import com.udea.rutaudea.data.sync.RecordatorioSync
import com.udea.rutaudea.data.sync.ResultadoSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Punto de entrada de la aplicación. Expone los singletons de
 * la base de datos, el repositorio y la sincronización del banco.
 *
 * Flujo de arranque (D-02):
 * 1. Se siembra el asset si hace falta (primera instalación o versión nueva).
 * 2. Se muestra Room de inmediato; la sincronización con Firestore corre
 *    en segundo plano y nunca bloquea la UI.
 * 3. Se programa el trabajo periódico de 24 h con WorkManager.
 */
class RutaUdeaApp : Application() {

    private val database by lazy { RutaUdeaDatabase.getInstance(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Sincronización híbrida con `questions_v2` + `bank_manifest/active`. */
    val bankSync: BankSyncManager by lazy {
        BankSyncManager(
            firestore = FirebaseFirestore.getInstance(),
            questionDao = database.questionDao(),
            prefs = getSharedPreferences(BankSyncManager.PREFS, MODE_PRIVATE),
            appVersionCode = BuildConfig.VERSION_CODE,
            alActualizar = ::precachearImagenes
        )
    }

    val questionRepository: QuestionRepository by lazy {
        QuestionRepository(database.questionDao(), bankSync)
    }

    private val seeder by lazy { DatabaseSeeder(this, database.questionDao()) }

    /** Cache local del historial de progreso (simulacros y prácticas). */
    val progressDao by lazy { database.progressDao() }

    /** Cache local de la racha de actividad del usuario. */
    val rachaDao by lazy { database.rachaDao() }

    /** Cache local del cuestionario de satisfacción (prueba piloto). */
    val encuestaDao by lazy { database.encuestaDao() }

    /** Evita repetir la revisión automática en cada cambio de foreground. */
    @Volatile
    private var ultimaRevision = 0L

    override fun onCreate() {
        super.onCreate()
        RecordatorioSync.registrarPrimerUso(this)
        BankSyncScheduler.programar(this)

        // Cada vez que la app vuelve a primer plano se comprueba el
        // manifiesto (con red y con una ventana mínima para no repetir).
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    sincronizarSiHayRed()
                }
            }
        )

        // El primer arranque conserva la prioridad del seed local antes de
        // consultar Firestore.
        ultimaRevision = System.currentTimeMillis()
        appScope.launch {
            seeder.seedIfNeeded()
            if (Conectividad.hayRed(this@RutaUdeaApp)) {
                aplicarSincronizacion()
            } else {
                Log.i(TAG, "Sin red al iniciar; se conserva el banco local")
            }
        }
    }

    /**
     * Revisa el banco al volver a primer plano, solo si hay red y no se
     * revisó hace menos de [VENTANA_REVISION_MS].
     */
    private fun sincronizarSiHayRed() {
        appScope.launch {
            val ahora = System.currentTimeMillis()
            if (ahora - ultimaRevision < VENTANA_REVISION_MS) return@launch
            if (!Conectividad.hayRed(this@RutaUdeaApp)) return@launch
            ultimaRevision = ahora
            aplicarSincronizacion()
        }
    }

    private suspend fun aplicarSincronizacion() {
        when (bankSync.sincronizar()) {
            is ResultadoSync.Actualizado, is ResultadoSync.SinCambios ->
                RecordatorioSync.cancelar(this)
            else -> Unit
        }
    }

    /**
     * Pre-carga las imágenes del lote recién sincronizado para que la
     * práctica y el simulacro funcionen offline (D-02 §4.2 paso 9).
     * Coil las guarda en su caché de disco; si una falla, la pregunta de
     * texto sigue disponible.
     */
    private fun precachearImagenes(entities: List<QuestionEntity>) {
        val loader = Coil.imageLoader(this)
        entities.asSequence()
            .flatMap { QuestionMapper.imagenesDesdeJson(it.imagenesJson).asSequence() }
            .mapNotNull { it.url?.takeIf(String::isNotBlank) }
            .distinct()
            .take(MAX_IMAGENES_PRECACHE)
            .forEach { url ->
                loader.enqueue(
                    ImageRequest.Builder(this)
                        .data(url)
                        .build()
                )
            }
    }

    companion object {
        private const val TAG = "RutaUdeaApp"

        /** Tope defensivo: el banco v2 tiene 93 imágenes publicadas. */
        private const val MAX_IMAGENES_PRECACHE = 200

        /** No repetir la revisión automática si ya se hizo hace 5 minutos. */
        private const val VENTANA_REVISION_MS = 5L * 60L * 1000L
    }
}
