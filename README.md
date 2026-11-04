# RutaUdeA — Proyecto Android

Aplicación móvil de preparación para el examen de admisión de la Universidad de Antioquia.

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose
- **Arquitectura:** MVVM
- **Base de datos local:** Room (SQLite) — banco de preguntas
- **Backend:** Firebase (Authentication + Firestore) — ✅ implementado

---

## Estructura actual

```
app/src/main/java/com/udea/rutaudea/
├── RutaUdeaApp.kt                 # Application: expone repositorio y siembra el banco
├── MainActivity.kt                # Entry point - usa AppNavHost()
├── di/
│   ├── AppModule.kt               # Punto de acceso a repositories (local manual)
│   └── ViewModelFactories.kt      # Factories: Splash/Simulacro/Resultado/Perfil
├── data/
│   ├── local/                     # Room: RutaUdeaDatabase, QuestionEntity, QuestionDao
│   ├── mapper/QuestionMapper.kt   # Conversión entidad <-> dominio
│   ├── mapper/UserMapper.kt       # Conversión documento users <-> dominio
│   ├── repository/
│   │   ├── QuestionRepository.kt  # Banco local + sincronización desde Firestore
│   │   ├── AuthRepository.kt      # Firebase Auth + perfil (colección users)
│   │   ├── SimulationRepository.kt# Registro de simulacros completos
│   │   ├── PruebaRepository.kt    # Registro de pruebas cortas "Pruébate"
│   │   ├── PracticeRepository.kt  # Registro de sesiones de práctica (batch)
│   │   └── ProgressRepository.kt  # Lectura del historial para Progreso
│   └── source/local/              # JsonL loader + Seeder
├── domain/
│   ├── model/Question.kt          # Modelo de dominio de preguntas
│   ├── model/TipoSimulacro.kt     # COMPLETO (80) / PRUEBA (20) y duración
│   ├── model/User.kt              # Usuario + perfil (nombre, institución, onboarding)
│   ├── model/DatosPerfil.kt       # Validación pura del onboarding/edición de perfil
│   ├── model/Progreso.kt          # Modelos del módulo de Progreso
│   ├── services/QuestionSelector.kt # Motor de selección de 80 preguntas
│   ├── services/PracticeSelector.kt # Estratificación proporcional por dificultad
│   └── services/AnalizadorProgreso.kt # Reglas de analítica y recomendaciones
├── ui/
│   ├── navigation/                # AppNavHost + AppNavGraph
│   ├── screen/
│   │   ├── splash/                # Splash (espera seed + decide sesión)
│   │   ├── home/                  # Home con 5 tabs
│   │   ├── simulacro/             # Simulacro 80 preguntas y prueba corta 20
│   │   ├── resultado/             # Score, análisis y detalle por pregunta
│   │   ├── perfil/                # Login / Registro / Perfil / Google Sign-In
│   │   ├── onboarding/            # Onboarding obligatorio y edición de perfil
│   │   ├── practica/              # Filtros → sesión con feedback → resultado
│   │   ├── progreso/              # Dashboard: evolución, subtemas, recomendaciones, historial
│   │   └── info/                  # Contenido educativo por componente (acordeón)
│   └── theme/                     # Material3 Forest Green
└── tests unitarios: data/mapper, data/source/local, domain/services, ui/screen
```

## Capa de datos

La consulta de preguntas se realiza contra la **base local Room** para
garantizar rapidez, filtrado y funcionamiento offline. Firebase se usa para
autenticación, sincronización del banco canónico (`questions_v2` +
`bank_manifest`, D-02) y persistencia de resultados: simulacros completos en
`simulations` / `simulationQuestions` y pruebas cortas en `pruebas` /
`pruebaQuestions` (colecciones separadas para no mezclar métricas).

Los resúmenes y detalles también se cachean en Room (`simulations_cache`,
`pruebas_cache`, `practice_sessions_cache` y sus tablas de preguntas) con
estrategia **local-first**: se guarda primero en local y luego se intenta
subir a Firestore; si falla, la sesión queda pendiente y se reintenta en la
siguiente sincronización.

## Banco de preguntas

El dataset (JSONL) se empaqueta en:
`app/src/main/assets/questions/questions.jsonl`

Es el banco canónico v2 (`udea-2026-10-v1`, schema 2): **400 preguntas**
(200 RL + 200 CL), **55 contextos** y **93 imágenes** publicadas. Estados:
269 `aprobado`, 127 `aprobado_con_imagen`, 3 `pendiente` y 1
`deshabilitado`; solo los dos primeros se sirven a la app. Se carga en Room
en la primera ejecución.

**Actualizaciones del banco:** el contenido viaja en la APK (offline-first).
El `DatabaseSeeder` es **versionado**: al cambiar el JSONL, incrementa
`BANK_SEED_VERSION` (`v6` en Fase 7) y las instalaciones existentes se
re-siembran solas.

**Sincronización híbrida (D-02):** en segundo plano, `BankSyncManager` lee
`bank_manifest/active`, compara versión/hash/conteo y, si cambió, descarga
`questions_v2`, valida el lote y reemplaza Room de forma atómica. Sin
manifiesto la app sigue usando el asset sin mostrar errores; un lote inválido
conserva la versión anterior. `BankSyncWorker` (WorkManager) programa la
ejecución con red disponible y Coil pre-cachea las imágenes.

El banco legacy `questions` queda congelado para rollback y la app ya no lo
consulta.

## Consultas disponibles (`QuestionRepository`)

| Método | Uso |
| :--- | :--- |
| `getQuestions(area, subtema, dificultad, limit)` | Práctica filtrada |
| `getQuestionsForSimulacro(area, limit)` | Motor de selección (40 RL / 40 CL) |
| `getQuestionsBySubtema(subtema)` | Práctica por subtema |
| `observeQuestions()` | Flujo reactivo |
| `countByArea(area)` | Validar disponibilidad para simulacro |
| `replaceBank(questions)` | Carga inicial / sincronización |
| `countAll()` | Total del banco (para validar el lote descargado) |
| `updateBankFromFirestore()` | Sincronizar `questions_v2` según `bank_manifest` (D-02) |

## Autenticación y onboarding (Firebase) — obligatorios

- **Gate**: el splash verifica la sesión y el estado del onboarding.
  - Sin sesión → **Login** como raíz (sin acceso a Home).
  - Con sesión y onboarding pendiente → **Onboarding**.
  - Con sesión y perfil completo → **Home**.
  - El logout devuelve al login como raíz.
- **Registro manual** email+contraseña: nombre, apellido, correo, contraseña con
  confirmación y aceptación de términos. Los campos de contraseña permiten
  **mostrarla u ocultarla** para verificar lo escrito.
- **Google Sign-In** con **Credential Manager** (`GetGoogleIdOption` +
  `androidx.credentials`): la primera vez Firebase crea la cuenta
  automáticamente y la app crea `users/{uid}` con `onboardingCompletado=false`.
  Si el correo ya existe con email/contraseña se muestra un mensaje claro
  (no se vinculan cuentas automáticamente). En el login es la opción más
  visible: botón destacado con el logotipo de Google.
- **Onboarding obligatorio** (`ui/screen/onboarding/`): nombre, apellido, tipo
  de institución, institución y grado/semestre. Teléfono, ciudad y fecha del
  examen son opcionales. Al elegir **“Otra”** la institución también es
  opcional y no se piden grado ni semestre.
- **Perfil** (`ui/screen/perfil/`): datos del usuario, edición, encuesta piloto
  y cierre de sesión. También permite **recuperar la contraseña** por correo.
- **`AuthRepository`**: `register()`, `login()`, `signInWithGoogle()`,
  `completeProfile()`, `sendPasswordReset()`, `restoreSession()`, `logout()`.
- **Colección `users`**: perfil completo (nombre, apellido, correo, rol,
  teléfono, ciudad, institución, tipo, grado/semestre, fecha del examen,
  proveedor, `onboardingCompletado`, `fechaCreacion`, `estado`).
- **Roles** en reglas Firestore: `aspirante` (dueño) / `admin` (escritura).
  El usuario no puede cambiar su propio `rol`, `estado`, `correo` ni
  `fechaCreacion`.
- Con sesión, los simulacros, las pruebas y la encuesta se guardan en Firestore;
  el logout devuelve al login.

---

## Simulacro completo y "Pruébate"

- **Simulacro completo**: 80 preguntas (40 RL + 40 CL) y 120 min por defecto.
- **Pruébate**: versión corta de 20 preguntas (10 RL + 10 CL) pensada para
  sesiones rápidas. Se inicia desde la card **⚡ Pruébate** del Home (mismo
  tamaño que las cards de "Explora la app", con borde azul).
- **Duración configurable**: el tiempo de la prueba se lee de
  `settings/config` (`tiempo_prueba_minutos`, 30 min por defecto) y el del
  simulacro de `tiempo_simulacro_minutos` (120 min por defecto). Si la
  lectura falla o el documento no existe, la app usa esos valores seguros.
- **Mismas reglas de selección**: `QuestionSelector` aplica a ambos tipos la
  distribución proporcional por dificultad, cobertura de subtemas, cuotas
  flexibles y exclusión de preguntas recientes. La exclusión es **por tipo**:
  una prueba solo evita preguntas de pruebas recientes y un simulacro solo de
  simulacros recientes.
- **Persistencia separada**: simulacros en `simulations` /
  `simulationQuestions` y Room `simulations_cache` /
  `simulation_questions_cache`; pruebas en `pruebas` / `pruebaQuestions` y
  Room `pruebas_cache` / `prueba_questions_cache` (DB v9). Así Progreso
  muestra métricas y etiquetas de **Simulacro** y **Prueba** por separado.
- **Historial y racha**: ambos tipos cuentan como actividad para la racha y
  aparecen en el historial con su etiqueta (`⚡ Prueba` / `🧪 Simulacro`).
- **Salida de resultados**: el gesto atrás y el botón de volver llevan
  siempre al Home, sin regresar a la sesión ya finalizada.
- **Guardado idempotente**: el id de sesión se reutiliza; si ya existe en
  Room, no se duplica el registro, la subida a Firestore ni la racha.
- **Datos del build intermedio**: las pruebas que se hubieran guardado en
  `simulations` con `tipo='prueba'` se ignoran al descargar y no se migran a
  las nuevas tablas.

---

## Práctica dirigida

- **Filtros**: área (RL/CL) + subtema (dinámico desde el banco) + cantidad (5/10/20).
- **Estratificación**: la dificultad no se filtra — `PracticeSelector` reparte
  las preguntas proporcionalmente a la composición real del banco filtrado
  (método de mayor residuo), con relleno flexible.
- **Feedback inmediato** al responder: ✓/✗, explicación, subtema/dificultad y
  recomendación. La respuesta queda bloqueada. Sin límite de tiempo.
- **Persistencia**: cada sesión completada se guarda en Firestore
  (`practiceSessions` + subcolección `practiceQuestions`) para el futuro
  módulo de Progreso.

---

## Próximos pasos

1. Prueba en dispositivo/emulador del banco v2 (imágenes, contexto largo,
   opciones en imagen, offline y rotación); pasa a Fase 8 por falta de
   KVM/ADB en la máquina de desarrollo.
2. Primera publicación real de `bank_manifest/active` desde el panel y
   verificación de la sincronización en staging.
3. Análisis por componente y dificultad en Progreso.
4. Inactividad del cronómetro + vinculación manual de cuentas Google desde el perfil.
5. Migrar inyección manual a **Hilt**.
6. Tests de instrumentación del flujo de autenticación/onboarding.
