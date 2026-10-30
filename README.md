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
│   │   ├── SimulationRepository.kt# Registro de simulacros en Firestore
│   │   ├── PracticeRepository.kt  # Registro de sesiones de práctica (batch)
│   │   └── ProgressRepository.kt    # Lectura del historial para Progreso
│   └── source/local/              # JsonL loader + Seeder
├── domain/
│   ├── model/Question.kt          # Modelo de dominio de preguntas
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
│   │   ├── simulacro/             # Simulacro 80 preguntas, timer 120 min
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
`bank_manifest`, D-02) y persistencia de resultados de simulacros
(`simulations` / `simulationQuestions`).

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
- Con sesión, los simulacros y la encuesta se guardan en Firestore; el logout
  devuelve al login.

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
