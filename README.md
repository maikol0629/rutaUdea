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
│   ├── repository/
│   │   ├── QuestionRepository.kt  # Banco local + sincronización desde Firestore
│   │   ├── AuthRepository.kt      # Firebase Auth + perfil (colección users)
│   │   ├── SimulationRepository.kt# Registro de simulacros en Firestore
│   │   ├── PracticeRepository.kt  # Registro de sesiones de práctica (batch)
│   │   └── ProgressRepository.kt    # Lectura del historial para Progreso
│   └── source/local/              # JsonL loader + Seeder
├── domain/
│   ├── model/Question.kt          # Modelo de dominio de preguntas
│   ├── model/User.kt              # Modelo de usuario (uid, nombre, correo, rol)
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
│   │   ├── perfil/                # Login / Registro / Perfil (Firebase Auth)
│   │   ├── practica/              # Filtros → sesión con feedback → resultado
│   │   ├── progreso/              # Dashboard: evolución, subtemas, recomendaciones, historial
│   │   └── common/ComingSoonScreen.kt
│   └── theme/                     # Material3 Forest Green
└── tests unitarios: data/mapper, data/source/local, domain/services, ui/screen
```

## Capa de datos

La consulta de preguntas se realiza contra la **base local Room** para
garantizar rapidez, filtrado y funcionamiento offline. Firebase se usa para
autenticación, sincronización del banco maestro (`questions`) y persistencia
de resultados de simulacros (`simulations` / `simulationQuestions`).

## Banco de preguntas

El dataset (JSONL) se empaqueta en:
`app/src/main/assets/questions/questions.jsonl`

Contiene **200 preguntas** (100 Razonamiento Lógico + 100 Competencia Lectora),
clasificadas por los **componentes oficiales del examen UdeA** (RL01–RL18 y
CL01–CL14). Se cargan en Room en la primera ejecución.

**Actualizaciones del banco:** el contenido viaja en la APK (offline-first).
El `DatabaseSeeder` es **versionado**: al cambiar el JSONL, incrementa
`BANK_SEED_VERSION` y las instalaciones existentes se re-siembran solas.

**Banco maestro en la nube (opcional/futuro):** la colección `questions` de
Firestore no se usa en ningún flujo activo; `scripts/upload_bank_to_firestore.js`
está listo para poblarla cuando arranque el panel administrativo.

## Consultas disponibles (`QuestionRepository`)

| Método | Uso |
| :--- | :--- |
| `getQuestions(area, subtema, dificultad, limit)` | Práctica filtrada |
| `getQuestionsForSimulacro(area, limit)` | Motor de selección (40 RL / 40 CL) |
| `getQuestionsBySubtema(subtema)` | Práctica por subtema |
| `observeQuestions()` | Flujo reactivo |
| `countByArea(area)` | Validar disponibilidad para simulacro |
| `replaceBank(questions)` | Carga inicial / sincronización |
| `updateBankFromFirestore()` | Sincronizar banco maestro aprobado |

## Autenticación (Firebase) — con gate obligatorio

- **Gate**: el splash verifica la sesión; sin sesión se entra al login como raíz (sin acceso a Home).
- **Login/Registro** email+contraseña en `ui/screen/perfil/`
- **`AuthRepository`**: `register()`, `login()`, `restoreSession()`, `logout()`
- **Colección `users`**: perfil (nombre, correo, rol, fechaCreacion, estado)
- **Roles** en reglas Firestore: `aspirante` (dueño) / `admin` (escritura)
- Con sesión: los simulacros se guardan en Firestore; el logout devuelve al login.

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

1. Módulo de **Información educativa** por componente/subtema (último tab placeholder).
2. Análisis por componente y dificultad en Progreso.
3. Inactividad del cronómetro + recuperación de contraseña.
4. Panel administrativo web (CRUD del banco, carga CSV, roles admin).
5. Migrar inyección manual a **Hilt**.
