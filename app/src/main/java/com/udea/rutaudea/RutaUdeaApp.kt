package com.udea.rutaudea

import android.app.Application
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

    override fun onCreate() {
        super.onCreate()
        BankSyncScheduler.programar(this)
        appScope.launch {
            // Primero el asset (offline garantizado); después Firestore.
            seeder.seedIfNeeded()
            bankSync.sincronizar()
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
        /** Tope defensivo: el banco v2 tiene 93 imágenes publicadas. */
        private const val MAX_IMAGENES_PRECACHE = 200
    }
}
