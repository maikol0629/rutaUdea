# PLAN DE DESARROLLO — RutaUdeA

**Aplicación móvil de preparación para el examen de admisión de la Universidad de Antioquia**
*Ingeniería de Sistemas — Proyecto Integrador 2*
*Documento vivo — actualizado al progreso actual del repositorio*

---

## 1. Propósito

Este documento define el plan técnico y funcional para **RutaUdeA**: una aplicación móvil que ayuda a los aspirantes a prepararse para el examen de admisión de la Universidad de Antioquia mediante simulacros, práctica y análisis de desempeño. Incluye el alcance, la arquitectura, el modelo de datos, el estado actual del código y el plan de trabajo restante.

> Este documento vive en `docs/plan_de_desarrollo.md` y refleja **el estado real del repositorio** en cada versión. Las secciones marcadas como «✅ Implementado» corresponden a lo ya construido.

---

## 2. Alcance del proyecto

### Aspirante
- Crear cuenta e iniciar sesión.
- Consultar información educativa sobre componentes y subtemas.
- Realizar simulacros de 80 preguntas: 40 de razonamiento lógico y 40 de lectura crítica.
- Responder preguntas con cronómetro y condiciones semejantes al examen real.
- Recibir retroalimentación inmediata en el modo práctica.
- Consultar resultados y análisis detallado de cada simulacro.
- Consultar evolución histórica de su desempeño.
- Recibir recomendaciones personalizadas de estudio y práctica.

### Administrador
- Administrar usuarios con rol administrativo.
- Cargar preguntas mediante CSV/JSON.
- Crear, editar y eliminar preguntas.
- Gestionar componentes, subtemas y dificultades.
- Revisar y aprobar explicaciones generadas con asistencia de IA.
- Consultar indicadores básicos de calidad y uso del banco de preguntas.

---

## 3. Decisiones tecnológicas

| Elemento | Decisión |
| :--- | :--- |
| **Aplicación móvil** | Android nativo |
| **Lenguaje** | Kotlin |
| **Interfaz** | Jetpack Compose |
| **Patrón arquitectónico** | MVVM |
| **Base de datos local** | Room (SQLite) — banco de preguntas |
| **Backend / Auth** | Firebase (Authentication + Firestore) — ✅ implementado (email/contraseña, reglas por rol) |
| **Panel administrativo** | Aplicación web (futuro) |
| **Contenido inicial** | JSONL/JSON de preguntas validadas empaquetado como asset |
| **Asistencia inteligente** | Servicio de generación asistida para explicaciones, sujeto a revisión administrativa |

> **Nota de consistencia:** El desarrollo se fija en Kotlin + Jetpack Compose. No se utilizará Flutter en la implementación.

---

## 4. Arquitectura

La aplicación se organiza en capas para separar la interfaz, la lógica de presentación, el acceso a datos y los servicios externos.

- **UI (Jetpack Compose):** Pantallas, componentes visuales, navegación y estados.
- **ViewModel:** Estado de cada pantalla, validaciones y coordinación de casos de uso.
- **Repository:** Punto único de acceso a datos y servicios.
- **Data sources:** Room (local), Firestore y Firebase Authentication.
- **Domain/services:** Reglas del simulacro, selección de preguntas, cálculo de resultados y recomendaciones.

**Flujo general:**
```
Usuario → UI (Compose) → ViewModel → Repository → Room/Firebase → procesamiento → UI
```

Los diagramas arquitectónicos, de casos de uso, modelo de datos, secuencia, navegación, despliegue y consultas se encuentran en `docs/diagramas/`.

---

## 5. Estado actual del código (progreso)

> Estado de la rama principal al momento de la última actualización de este documento.

### ✅ Implementado
- **Estructura del proyecto Android** con Gradle (Kotlin DSL), Kotlin, Jetpack Compose, Room, Coroutines y Gson.
- **Capa de datos completa:**
  - `RutaUdeaApp.kt`: `Application` que expone el repositorio y siembra el banco inicial.
  - `data/local/RutaUdeaDatabase.kt`: base Room con la tabla `questions`.
  - `data/local/entity/QuestionEntity.kt`: entidad que refleja la estructura del JSONL final.
  - `data/local/dao/QuestionDao.kt`: consultas de práctica y simulacro.
  - `data/mapper/QuestionMapper.kt`: conversión entidad ↔ dominio.
  - `data/repository/QuestionRepository.kt`: punto único de acceso a las preguntas.
  - `data/source/local/QuestionJsonlLoader.kt`: parseo del JSONL empaquetado.
  - `data/source/local/DatabaseSeeder.kt`: carga inicial del banco en la primera ejecución.
  - `domain/model/Question.kt`: modelo de dominio.
- **Banco de preguntas validado empaquetado** en `app/src/main/assets/questions/questions.jsonl` (200 preguntas: 100 razonamiento lógico + 100 competencia lectora).
- **Consulta del banco en local**: práctica filtrada por área/subtema/dificultad, preguntas para simulacro (40 RL / 40 CL), consulta por subtema, flujo reactivo y conteos.
- **UI (Compose) y ViewModels**: Splash (espera seed), Home con 5 tabs, Simulacro, Resultado y Perfil; navegación con `AppNavHost`.
- **Motor de selección de las 80 preguntas** (`domain/services/QuestionSelector.kt`, clase pura testeable):
  - 40 RL + 40 CL, distribución de dificultad proporcional al banco (método de mayor residuo) y cuotas proporcionales por subtema.
  - Sin repeticiones dentro del simulacro; exclusión de preguntas de los últimos 3 simulacros del usuario (Firestore), con relajación gradual si el banco no alcanza; cuotas flexibles.
  - Validación de las 80 preguntas antes de iniciar; selección persistida en `SavedStateHandle` (supervivencia a rotación/minimización).
- **Cronómetro de 120 minutos** con cuenta regresiva, persistencia en `SavedStateHandle` y overlay de continuar/abandonar al volver de segundo plano.
- **Registro de respuestas y resultados** (`SimulationRepository`): simulacro completado guardado en Firestore (`simulations` + subcolección `simulationQuestions` con respuesta usuario/correcta por pregunta); sin sesión se omite el guardado en la nube.
- **Resultado**: puntaje, porcentaje, tiempo empleado y detalle expandible por pregunta (correcta/incorrecta + explicación).
- **Autenticación — ✅ implementado con gate obligatorio** (rama `feat/auth-gate`):
  - `google-services.json` integrado; Auth email/contraseña con `AuthRepository` (`register`, `login`, `restoreSession`, `logout`, creación de perfil en `users` con rol aspirante).
  - Pantalla de **Perfil** con login/registro funcional (toggle), perfil del usuario y cierre de sesión.
  - **Gate**: el splash verifica la sesión tras el seed; sin sesión se entra al **login como raíz** (sin acceso a Home), con sesión se entra directo a Home; el logout devuelve al login como raíz.
- **Firebase — ✅ implementado** (rama `feat/firebase-config`, ya en `main`):
  - **Reglas de seguridad Firestore** (`firestore.rules`) completas: users, questions, contexts, components, subtopics, simulations/simulationQuestions, educationalContent, settings, con roles aspirante/admin.
  - **Sincronización del banco**: `QuestionRepository.updateBankFromFirestore()` descarga preguntas aprobadas y reemplaza el banco local.
- **Módulo de Práctica — ✅ implementado** (rama `feat/practica`):
  - Filtros: área (RL/CL) → subtema (dinámico desde Room, u "Todos") → cantidad (5/10/20). La **dificultad no es filtrable**: la selección se estratifica por dificultad **proporcional a la composición del banco filtrado** (`PracticeSelector`, método de mayor residuo).
  - Sesión con **retroalimentación inmediata** (plan §10): ✓/✗, respuesta dada vs correcta, explicación, subtema/dificultad y recomendación estática por subtema; respuesta bloqueada al revelarse; sin límite de tiempo; progreso persistido en `SavedStateHandle`.
  - Resultado: score, %, tiempo y repaso por pregunta; "Nueva práctica" conserva los filtros.
  - Persistencia: colección `practiceSessions` + subcolección `practiceQuestions` (escritura en batch) vía `PracticeRepository` — alimenta el futuro módulo de Progreso.
- **Módulo de Progreso — ✅ implementado** (rama `feat/progreso`):
  - Dashboard (tab 📈): nº de simulacros/prácticas, promedio global y mejor puntaje.
  - **Evolución**: gráfica de línea con el % de cada simulacro en orden cronológico.
  - **Acierto por subtema**: agregado de los detalles por pregunta (`simulationQuestions` + `practiceQuestions`) de las sesiones recientes.
  - **Fortalezas, áreas de mejora y recomendaciones** según §12: solo subtemas con evidencia ≥3 respuestas («una pregunta no hace un tema débil»).
  - Historial unificado (simulacros + prácticas) con score, % y tiempo; empty state con CTAs.
  - Arquitectura: `ProgressRepository` (Firestore, degrada a vacío sin conexión) + `AnalizadorProgreso` (dominio puro, testeado).
- **Módulo Info (educativo) — ✅ implementado** (rama `feat/info`):
  - Contenido de los **29 componentes del examen** (15 RL + 14 CL): descripción, estrategia de resolución y errores comunes, empaquetado como asset (`assets/info/contenido_educativo.json`, offline-first; la colección `educationalContent` queda para el futuro panel admin).
  - Tab 📚 con filtro por área y acordeón expandible por componente.
  - `ComingSoonScreen` eliminado: las 5 pestañas de la app son funcionales.
- **Tests unitarios**: 66 tests (mapper, loader, seeder, motor de selección, gate de autenticación, selector/sesión/filtros de práctica, analítica de progreso, módulo Info) — `./gradlew test` PASS.

### 🔶 Pendiente / En construcción
- **Recuperación de contraseña** (`sendPasswordResetEmail`).
- **Inactividad del cronómetro**: cierre por inactividad configurable y registro de abandono.
- **Análisis por componente (área) y dificultad**: el acierto por subtema ya está en Progreso; falta el desglose por componente y por dificultad (hoy: detalle por pregunta en resultados).
- **Vista detalle** de un simulacro del historial de Progreso.
- **Panel administrativo web** (CRUD de preguntas, carga CSV, estados).
- **Asistencia IA** para explicaciones con revisión administrativa.
- **Iconos de lanzador**: revisar adaptive icon (se ve cuadrado verde en algunos launchers).
- **Migrar DI manual a Hilt**; **tests de instrumentación UI/Room**; **deploy de `firestore.rules`**.

---

## 6. Banco de preguntas validado

El banco inicial está compuesto por **200 preguntas validadas** (100 de razonamiento lógico y 100 de competencia lectora), preparadas a partir de las fuentes inventariadas.

- Archivo fuente empaquetado en la app: `app/src/main/assets/questions/questions.jsonl`
- Copia de respaldo en formato JSON: `docs/preguntas_validadas.json`
- Estructura por pregunta: `id`, `area`, `subtema`, `competencia`, `dificultad`, `contexto`, `pregunta`, `opciones` (A–D), `respuesta_correcta`, `explicacion`, `fuente`, `tipo_fuente`, `es_original`, `verificada`, `duplicado`.

> **Nota de calidad:** **32 preguntas RL** aún tienen `respuesta_correcta` pendiente (o enunciado incompleto) y quedan excluidas por los selectores — banco válido: 168 de 200. El objetivo es completar la validación para que el motor disponga de más margen por subtema.

> **Normalización v2 (2026-10):** los subtemas de RL fueron unificados a los **componentes oficiales del examen** RL01–RL18 (antes conviven 27 etiquetas con duplicados: "Porcentajes" vs "RL02 Porcentajes", "Geometria" vs "RL13 Geometría", el comodín "Razonamiento logico"…). Las 15 preguntas del comodín fueron reclasificadas por contenido. CL ya usaba CL01–CL14 consistentemente. Cambios replicados en `questions.jsonl` y `docs/preguntas_validadas.json`; **falta replicar en el banco maestro de Firestore**.

---

## 7. Modelo de datos

### Tabla local (Room) — implementada
| Campo | Tipo | Notas |
| :--- | :--- | :--- |
| `id` | String (PK) | Identificador único |
| `area` | String | `razonamiento_logico` / `competencia_lectora` |
| `componente` | String | Derivado del área (Razonamiento Lógico / Competencia Lectora) |
| `subtema` | String | Subtema temático |
| `competencia` | String | Competencia evaluada |
| `tipo_texto` | String | Solo lectura crítica |
| `dificultad` | String | `baja` / `media` / `alta` |
| `texto_base` | String | Texto de apoyo (lectura crítica) |
| `contexto` / `contexto_id` | String | Contexto compartido |
| `pregunta` | String | Enunciado |
| `opcion_a` … `opcion_d` | String | Opciones |
| `respuesta_correcta` | String | Clave correcta |
| `explicacion` | String | Explicación |
| `fuente` / `tipo_fuente` | String | Origen de la pregunta |
| `es_original` | Boolean | Si fue generada |
| `verificada` | Boolean | Si fue verificada |
| `estado` | String | `aprobado` / `pendiente` / `deshabilitado` |

### Colecciones Firestore (✅ reglas desplegadas; `users`, `simulations`/`simulationQuestions` y `practiceSessions`/`practiceQuestions` en uso; sincronización de `questions` implementada)
| Colección | Propósito |
| :--- | :--- |
| `users` | `uid`, `nombre`, `correo`, `rol`, `fechaCreacion`, `estado` |
| `questions` | Banco maestro con `estado` y `metadatos` |
| `contexts` | Contextos compartidos |
| `components` / `subtopics` | Componentes y subtemas |
| `simulations` / `simulationQuestions` | Simulacros y respuestas con tiempos |
| `practiceSessions` | Sesiones de práctica |
| `recommendations` | Recomendaciones personalizadas |
| `educationalContent` | Contenido informativo |
| `settings` | Reglas del simulacro, inactividad y distribución |

---

## 8. Diseño del simulacro de 80 preguntas

Cada simulacro tendrá **80 preguntas totales**: 40 de razonamiento lógico y 40 de lectura crítica. La selección usará reglas de distribución para garantizar cobertura de componentes, subtemas y niveles de dificultad, en lugar de una selección completamente aleatoria.

**Algoritmo implementado** (`QuestionSelector`, ver tests en `QuestionSelectorTest`):
1. ✅ La matriz de distribución se **deriva proporcionalmente de la composición real del banco** (método de mayor residuo), en lugar de inventar porcentajes oficiales no documentados.
2. ✅ Separación del banco por área → subtema (cuotas proporcionales con secuencia entrelazada).
3. ✅ Selección de las cantidades requeridas de cada grupo, con **cuotas flexibles**: si un grupo no alcanza, se rellena con sobrantes del mismo área primero.
4. ✅ Sin preguntas repetidas dentro del mismo simulacro.
5. ✅ Exclusión de preguntas de los **últimos 3 simulacros** del usuario (`simulationQuestions` en Firestore), con relajación gradual por área y global si el banco no alcanza.
6. 🔶 Dependencias de contexto compartido: pendiente resolver como unidades coherentes.
7. ✅ Validación de las 80 preguntas antes de iniciar (error explícito si el banco válido < 80).
8. ✅ Persistencia de la selección en `SavedStateHandle` (JSON de IDs) para que no cambie al rotar/minimizar.

---

## 9. Cronómetro e inactividad
- ✅ Cronómetro de **120 minutos** en cuenta regresiva; el estado persiste en `SavedStateHandle` y sobrevive recreación y segundo plano.
- ✅ Al volver de segundo plano se muestra un overlay «Continuar / Abandonar» y el tiempo continúa.
- ✅ Auto-finalización del simulacro al llegar a 0:00 (navega a Resultado).
- 🔶 Pendiente: marcas temporales absolutas persistentes (hoy el contador vive en el ViewModel/SAH del destino), registro de última actividad, cierre por inactividad configurable y registro de abandono.

---

## 10. Retroalimentación y aprendizaje
- ✅ En **práctica**: retroalimentación inmediata (respuesta correcta, respuesta seleccionada, explicación, componente, subtema, dificultad y recomendación estática por subtema; el motor de recomendaciones es P1 futuro).
- En **simulacro**: preservar las condiciones de evaluación y mostrar la retroalimentación al finalizar (✅ implementado).

---

## 11. Asistencia de IA
La IA se incorporará como herramienta de apoyo al administrador, no como publicador autónomo:
1. Administrador selecciona una pregunta.
2. Sistema solicita una explicación propuesta.
3. IA genera borrador.
4. Administrador revisa exactitud, claridad y coherencia.
5. Administrador edita o aprueba.
6. Solo una explicación aprobada pasa al contenido visible.

---

## 12. Analítica y recomendaciones
**Indicadores:** acierto global, por componente, subtema y dificultad; respondidas/incorrectas/omitidas; tiempos; evolución entre simulacros; completados/abandonados; fortalezas y áreas de mejora.

> ✅ **Implementado en el módulo de Progreso** (rama `feat/progreso`): acierto global y por subtema (con evidencia ≥3), evolución entre simulacros, fortalezas/áreas de mejora y recomendaciones basadas en evidencia. Pendiente: desglose por componente y dificultad, y tiempos por pregunta (se guardan como 0).

Las recomendaciones priorizarán subtemas con bajo desempeño y suficiente evidencia, evitando recomendar un tema como «débil» por una sola pregunta. ✅ (regla de evidencia mínima implementada y testeada).

---

## 13. Panel web administrativo (futuro)
Inicio de sesión, carga de CSV, vista/búsqueda del banco, CRUD de preguntas, filtros, gestión de estados (pendiente/aprobado/deshabilitado), gestión de contenidos, generación/revisión de explicaciones asistidas y configuración de parámetros del simulacro.

---

## 14. Backlog priorizado

| Prioridad | Historia / funcionalidad | Área | Estado |
| :---: | :--- | :--- | :--- |
| **P0** | Configuración del proyecto Android y Firebase | Base técnica | ✅ |
| **P0** | Modelo Room + banco de preguntas | Datos | ✅ |
| **P0** | Repositorio y carga inicial (JSONL) | Datos | ✅ |
| **P0** | Autenticación y roles | Acceso | ✅ (login/registro + gate obligatorio; 🔶 recuperación de contraseña) |
| **P0** | Modelo Firestore | Datos | ✅ (reglas desplegadas en `firestore.rules`) |
| **P0** | Importador y validador CSV/JSON | Contenido | 🔶 (sincronización de banco implementada; importador admin pendiente) |
| **P0** | Panel CRUD de preguntas | Administración | 🔶 |
| **P0** | Motor de selección de 80 preguntas | Simulacro | ✅ |
| **P0** | Pantallas del simulacro | Simulacro | ✅ |
| **P0** | Cronómetro + persistencia + inactividad | Simulacro | ✅ / 🔶 (falta cierre por inactividad registrada) |
| **P0** | Registro de respuestas y tiempos | Resultados | ✅ (respuestas; 🔶 tiempo por pregunta guardado como 0) |
| **P0** | Cálculo de resultados | Resultados | ✅ (score, %, detalle por pregunta) |
| **P0** | Análisis por componente/subtema/dificultad | Analítica | 🔶 (por subtema ✅ en Progreso; falta componente y dificultad) |
| **P0** | Contenido informativo | Aprendizaje | ✅ (29 componentes: descripción, estrategia y errores comunes) |
| **P1** | Práctica filtrada con feedback inmediato | Aprendizaje | ✅ (estratificación proporcional; recomendación estática hasta tener motor P1) |
| **P1** | Historial y gráficas de progreso | Analítica | ✅ (dashboard, evolución, acierto por subtema, historial) |
| **P1** | Motor de recomendaciones | Personalización | 🔶 (V1 basada en evidencia incluida en Progreso; motor personalizado pendiente) |
| **P1** | Asistencia IA para explicaciones | Administración | 🔶 |
| **P1** | Pruebas con 15 estudiantes | Validación | 🔶 |
| **P2** | Mejoras avanzadas de IA | Evolución | 🔶 |
| **P2** | Gamificación | Evolución | 🔶 |

---

## 15. Orden recomendado de implementación restante

1. ✅ Configurar repositorios y estructura de proyectos.
2. ✅ Crear modelos de dominio y Room; cargar banco inicial.
3. ✅ Empaquetar y validar el banco de preguntas.
4. ✅ Configurar Firebase (Auth + Firestore) y seguridad.
5. 🔶 Construir importación/validación del CSV/JSON (panel admin).
6. 🔶 Crear panel administrativo básico.
7. ✅ Implementar autenticación móvil (login/registro en Perfil + gate obligatorio de sesión; pendiente recuperación de contraseña).
8. ✅ Implementar navegación y pantallas base (Compose).
9. ✅ Implementar motor de selección de simulacro.
10. ✅ Implementar simulacro y persistencia (local + Firestore con sesión).
11. ✅ Implementar cronómetro (120 min) — 🔶 pendiente inactividad/abandono.
12. ✅ Implementar cálculo y almacenamiento de resultados.
13. 🔶 Implementar análisis detallado por componente/subtema/dificultad.
14. ✅ Implementar información educativa (módulo Info con 29 componentes, asset offline-first).
15. ✅ Implementar práctica con feedback (filtros, estratificación, sesión con feedback inmediato, resultado y persistencia).
16. ✅ Implementar historial y recomendaciones (dashboard de Progreso con regla de evidencia ≥3).
17. 🔶 Integrar asistencia de IA.
18. 🔶 Ejecutar pruebas técnicas (66 unitarias ✅; faltan UI/integración).
19. 🔶 Ejecutar piloto con ~15 estudiantes.
20. 🔶 Corregir, documentar y preparar entrega.

> **Próximo hito sugerido:** PRs de `feat/practica` → `feat/progreso` → `feat/info` a `main` → **análisis por componente/dificultad** e **inactividad del cronómetro** (P0 restantes del simulacro).

---

## 16. Cronograma sugerido del semestre

| Semana | Entregable principal |
| :---: | :--- |
| **1** | Levantamiento final de requisitos, revisión del examen oficial y definición de matriz de simulacro. |
| **2** | Arquitectura, Firebase, modelo de datos y estructura de proyectos. |
| **3** | Importador CSV/JSON, validaciones y carga inicial. |
| **4** | Panel administrativo: preguntas y contenidos. |
| **5** | Autenticación, navegación y pantallas base. |
| **6** | Motor de selección de preguntas. |
| **7** | Interfaz y flujo completo del simulacro. |
| **8** | Cronómetro, persistencia e inactividad. |
| **9** | Resultados y análisis detallado. |
| **10** | Módulo de información y práctica. |
| **11** | Progreso histórico y visualizaciones. |
| **12** | Recomendaciones personalizadas. |
| **13** | Asistencia IA y revisión administrativa. |
| **14** | Pruebas funcionales, rendimiento y seguridad. |
| **15** | Piloto con ~15 estudiantes y recopilación de retroalimentación. |
| **16** | Correcciones, documentación, despliegue y sustentación. |

---

## 17. Estrategia de pruebas
- Pruebas unitarias para reglas de selección, cálculo de resultados y recomendaciones.
- Pruebas de integración con Room y Firebase.
- Pruebas de importación de datos con casos válidos e inválidos.
- Pruebas de persistencia al cerrar/minimizar la aplicación.
- Pruebas del límite de inactividad.
- Pruebas de distribución del simulacro: 40 + 40 y cobertura de criterios.
- Pruebas de interfaz en diferentes tamaños de pantalla Android.
- Pruebas de seguridad de reglas de Firestore y roles.
- Pruebas de usabilidad con ~15 estudiantes.

---

## 18. Criterios de aceptación del MVP
- Un usuario puede registrarse e iniciar sesión.
- Un administrador puede cargar y gestionar el banco de preguntas.
- El sistema genera un simulacro válido de 80 preguntas (40 por componente).
- La selección cubre los criterios de distribución definidos.
- El cronómetro continúa aunque la aplicación pase a segundo plano.
- La inactividad puede finalizar el simulacro según el parámetro configurado.
- Las respuestas, tiempos y resultados quedan almacenados.
- El usuario recibe un análisis detallado y recomendaciones asociadas a sus debilidades.
- El contenido informativo puede consultarse por componente/subtema.
- El administrador revisa explicaciones asistidas por IA antes de publicarlas.
- El sistema puede ser probado con ~15 estudiantes.

---

## 19. Riesgos y mitigaciones

| Riesgo | Problema | Mitigación |
| :--- | :--- | :--- |
| **Banco insuficiente** | No hay suficientes preguntas para cumplir la distribución. | Definir matriz según el inventario real y permitir configuración. |
| **Preguntas sin validar** | Preguntas sin `respuesta_correcta` o sin opciones completas. | Completar validación del banco antes del motor del simulacro. |
| **Preguntas de mala calidad** | Errores en respuestas o explicaciones. | Validación administrativa y revisión por expertos. |
| **Inconsistencia del formato oficial** | El simulacro no representa el examen. | Verificar la estructura contra documentación oficial. |
| **Costo de Firebase/IA** | Uso superior al presupuesto. | Monitorear consumo, límites y generación IA bajo demanda. |
| **IA genera errores** | Contenido educativo potencialmente erróneo. | Revisión humana obligatoria. |
| **Interrupciones del simulacro** | Usuario intenta pausar o manipular el tiempo. | Cronometría basada en timestamps y persistencia. |
| **Pocos participantes piloto** | Muestra pequeña para conclusiones. | Usar el piloto para usabilidad y detección de errores. |

---

## 20. Decisiones pendientes antes de programar el motor del simulacro
> El motor ya está implementado; varias de estas decisiones se tomaron de forma práctica y quedan por validar con la fuente oficial.
1. 🔶 Confirmar mediante la fuente oficial la duración y estructura exacta a emular (hoy: 120 min / 80 preguntas).
2. ✅ Matriz de distribución: **proporcional a la composición real del banco** (mayor residuo), con cuotas flexibles. Pendiente validar contra el formato oficial.
3. ✅ Inventario implícito: el motor consulta el banco real y relaja cuotas/exclusiones si no alcanza.
4. 🔶 Tiempo máximo de inactividad y penalización: pendiente (no implementado).
5. ✅ Repetición entre simulacros: excluir preguntas de los **últimos 3 simulacros** del usuario, con relajación gradual.
6. 🔶 Contenido informativo disponible por componente/subtema.
7. ✅ Permisos por rol ya definidos en `firestore.rules` (aspirante: dueño de sus datos; admin: gestión del banco y contenidos).

---

*Documento de trabajo — RutaUdeA. Actualícese conforme avance el desarrollo.*