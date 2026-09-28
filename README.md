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
│   │   └── SimulationRepository.kt# Registro de simulacros en Firestore
│   └── source/local/              # JsonL loader + Seeder
├── domain/
│   ├── model/Question.kt          # Modelo de dominio de preguntas
│   ├── model/User.kt              # Modelo de usuario (uid, nombre, correo, rol)
│   └── services/QuestionSelector.kt # Motor de selección de 80 preguntas
├── ui/
│   ├── navigation/                # AppNavHost + AppNavGraph
│   ├── screen/
│   │   ├── splash/                # Splash (espera seed del banco)
│   │   ├── home/                  # Home con 5 tabs
│   │   ├── simulacro/             # Simulacro 80 preguntas, timer 120 min
│   │   ├── resultado/             # Score, análisis y detalle por pregunta
│   │   ├── perfil/                # Login / Registro / Perfil (Firebase Auth)
│   │   └── common/ComingSoonScreen.kt
│   └── theme/                     # Material3 Forest Green
└── tests unitarios: data/mapper, data/source/local, domain/services
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
listas para cargarse en Room en la primera ejecución.
La sincronización `updateBankFromFirestore()` permite reemplazar el banco local
con la versión maestra aprobada en Firestore.

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

## Próximos pasos

1. Implementar módulo de **Práctica** filtrada con retroalimentación inmediata.
2. Implementar pantalla de **Progreso** con historial y gráficas.
3. Contenido educativo **Info** por componente/subtema.
4. Panel administrativo web (CRUD del banco, carga CSV, roles admin).
5. Migrar inyección manual a **Hilt**.
