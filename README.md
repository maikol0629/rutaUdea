# RutaUdeA — Proyecto Android

Aplicación móvil de preparación para el examen de admisión de la Universidad de Antioquia.

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose
- **Arquitectura:** MVVM
- **Base de datos local:** Room (SQLite) — banco de preguntas
- **Backend:** Firebase (Auth, Firestore) — fase posterior

---

## Capa de datos (implementada)

La consulta de preguntas se realiza contra la **base local Room** para
garantizar rapidez, filtrado y funcionamiento offline. Firebase queda para
autenticación, resultados de usuario y sincronización del banco.

```
app/src/main/java/com/udea/rutaudea/
├── RutaUdeaApp.kt                 # Application: expone repositorio y siembra el banco
├── data/
│   ├── local/
│   │   ├── RutaUdeaDatabase.kt    # Base Room (questions)
│   │   ├── entity/QuestionEntity.kt
│   │   └── dao/QuestionDao.kt     # Consultas de práctica y simulacro
│   ├── mapper/QuestionMapper.kt   # Conversión entidad <-> dominio
│   ├── repository/QuestionRepository.kt  # Punto único de acceso
│   └── source/local/
│       ├── QuestionJsonlLoader.kt # Parseo del JSONL empaquetado
│       └── DatabaseSeeder.kt      # Carga inicial del banco
└── domain/model/Question.kt       # Modelo de dominio
```

## Banco de preguntas

El dataset (JSONL) se empaqueta en:
`app/src/main/assets/questions/questions.jsonl`

Contiene **200 preguntas** (100 Razonamiento Lógico + 100 Competencia Lectora),
listas para cargarse en Room en la primera ejecución.

## Consultas disponibles (`QuestionRepository`)

| Método | Uso |
| :--- | :--- |
| `getQuestions(area, subtema, dificultad, limit)` | Práctica filtrada |
| `getQuestionsForSimulacro(area, limit)` | Motor de selección (40 RL / 40 CL) |
| `getQuestionsBySubtema(subtema)` | Práctica por subtema |
| `observeQuestions()` | Flujo reactivo |
| `countByArea(area)` | Validar disponibilidad para simulacro |
| `replaceBank(questions)` | Carga inicial / sincronización |

## Próximos pasos

1. Configurar Firebase (Auth + Firestore) y descargar `google-services.json`.
2. Implementar `updateBankFromFirestore()` en el repositorio.
3. Construir las capas ViewModel y UI (Compose).
4. Implementar el motor de selección de 80 preguntas.