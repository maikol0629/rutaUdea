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
| **Backend / Auth** | Firebase (Authentication + Firestore) — fase posterior |
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
- **Data sources:** Room (local), Firestore y Firebase Authentication (fase posterior).
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

### 🔶 Pendiente / En construcción
- Capas **ViewModel** y **UI (Compose)**: pantallas y navegación.
- **Motor de selección** de las 80 preguntas con reglas de distribución (40 + 40).
- **Cronómetro** con persistencia de timestamps e inactividad.
- **Registro de respuestas, tiempos y resultados** (colecciones `simulations`, `simulationQuestions`).
- **Cálculo de resultados y análisis** por componente/subtema/dificultad.
- **Información educativa** por componente/subtema.
- **Práctica** con retroalimentación inmediata.
- **Progreso histórico y recomendaciones.**
- **Firebase** (Auth + Firestore) y `google-services.json`; sincronización `updateBankFromFirestore()` (contrato ya definido en el repositorio).
- **Panel administrativo web** (CRUD de preguntas, carga CSV, estados).
- **Asistencia IA** para explicaciones con revisión administrativa.

---

## 6. Banco de preguntas validado

El banco inicial está compuesto por **200 preguntas validadas** (100 de razonamiento lógico y 100 de competencia lectora), preparadas a partir de las fuentes inventariadas.

- Archivo fuente empaquetado en la app: `app/src/main/assets/questions/questions.jsonl`
- Copia de respaldo en formato JSON: `docs/preguntas_validadas.json`
- Estructura por pregunta: `id`, `area`, `subtema`, `competencia`, `dificultad`, `contexto`, `pregunta`, `opciones` (A–D), `respuesta_correcta`, `explicacion`, `fuente`, `tipo_fuente`, `es_original`, `verificada`, `duplicado`.

> **Nota de calidad:** Parte del banco aún está marcado con `respuesta_correcta` pendiente de verificación. El objetivo es completar la validación para que el motor del simulacro disponga de suficientes preguntas con respuesta correcta y explicación.

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

### Colecciones Firestore (fase posterior — plan)
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

**Algoritmo conceptual:**
1. Definir la matriz de distribución del simulacro.
2. Separar el banco por componente → subtema → dificultad.
3. Seleccionar las cantidades requeridas de cada grupo.
4. Evitar preguntas repetidas dentro del mismo simulacro.
5. Evitar, cuando el banco lo permita, repetir preguntas usadas en simulacros recientes.
6. Resolver dependencias de contexto compartido como unidades coherentes.
7. Validar que se cumplan las 80 preguntas antes de iniciar.
8. Persistir la selección para que no cambie si el usuario cierra/minimiza la aplicación.

> La matriz exacta de porcentajes por dificultad y subtema deberá definirse a partir del formato oficial del examen y de la estructura real del banco disponible. No se debe inventar una distribución oficial si no está documentada.

---

## 9. Cronómetro e inactividad
- El tiempo se calculará con marcas temporales persistentes, no solo con un contador visual.
- Al pasar la aplicación a segundo plano, el tiempo continuará transcurriendo.
- El sistema registrará la última actividad relevante del usuario.
- Si se supera el tiempo máximo configurable de inactividad, el simulacro se cerrará.
- El cierre quedará registrado como abandono por inactividad y la penalización será configurable.

---

## 10. Retroalimentación y aprendizaje
- En **práctica**: retroalimentación inmediata (respuesta correcta, respuesta seleccionada, explicación, componente, subtema, dificultad y recomendación).
- En **simulacro**: preservar las condiciones de evaluación y mostrar la retroalimentación al finalizar.

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

Las recomendaciones priorizarán subtemas con bajo desempeño y suficiente evidencia, evitando recomendar un tema como «débil» por una sola pregunta.

---

## 13. Panel web administrativo (futuro)
Inicio de sesión, carga de CSV, vista/búsqueda del banco, CRUD de preguntas, filtros, gestión de estados (pendiente/aprobado/deshabilitado), gestión de contenidos, generación/revisión de explicaciones asistidas y configuración de parámetros del simulacro.

---

## 14. Backlog priorizado

| Prioridad | Historia / funcionalidad | Área | Estado |
| :---: | :--- | :--- | :--- |
| **P0** | Configuración del proyecto Android y Firebase | Base técnica | ✅ / 🔶 |
| **P0** | Modelo Room + banco de preguntas | Datos | ✅ |
| **P0** | Repositorio y carga inicial (JSONL) | Datos | ✅ |
| **P0** | Autenticación y roles | Acceso | 🔶 |
| **P0** | Modelo Firestore | Datos | 🔶 |
| **P0** | Importador y validador CSV/JSON | Contenido | 🔶 |
| **P0** | Panel CRUD de preguntas | Administración | 🔶 |
| **P0** | Motor de selección de 80 preguntas | Simulacro | 🔶 |
| **P0** | Pantallas del simulacro | Simulacro | 🔶 |
| **P0** | Cronómetro + persistencia + inactividad | Simulacro | 🔶 |
| **P0** | Registro de respuestas y tiempos | Resultados | 🔶 |
| **P0** | Cálculo de resultados | Resultados | 🔶 |
| **P0** | Análisis por componente/subtema/dificultad | Analítica | 🔶 |
| **P0** | Contenido informativo | Aprendizaje | 🔶 |
| **P1** | Práctica filtrada con feedback inmediato | Aprendizaje | 🔶 |
| **P1** | Historial y gráficas de progreso | Analítica | 🔶 |
| **P1** | Motor de recomendaciones | Personalización | 🔶 |
| **P1** | Asistencia IA para explicaciones | Administración | 🔶 |
| **P1** | Pruebas con 15 estudiantes | Validación | 🔶 |
| **P2** | Mejoras avanzadas de IA | Evolución | 🔶 |
| **P2** | Gamificación | Evolución | 🔶 |

---

## 15. Orden recomendado de implementación restante

1. ✅ Configurar repositorios y estructura de proyectos.
2. ✅ Crear modelos de dominio y Room; cargar banco inicial.
3. ✅ Empaquetar y validar el banco de preguntas.
4. 🔶 Configurar Firebase (Auth + Firestore) y seguridad.
5. 🔶 Construir importación/validación del CSV/JSON.
6. 🔶 Crear panel administrativo básico.
7. 🔶 Implementar autenticación móvil.
8. 🔶 Implementar navegación y pantallas base (Compose).
9. 🔶 Implementar motor de selección de simulacro.
10. 🔶 Implementar simulacro y persistencia.
11. 🔶 Implementar cronómetro e inactividad.
12. 🔶 Implementar cálculo y almacenamiento de resultados.
13. 🔶 Implementar análisis detallado.
14. 🔶 Implementar información educativa.
15. 🔶 Implementar práctica con feedback.
16. 🔶 Implementar historial y recomendaciones.
17. 🔶 Integrar asistencia de IA.
18. 🔶 Ejecutar pruebas técnicas.
19. 🔶 Ejecutar piloto con ~15 estudiantes.
20. 🔶 Corregir, documentar y preparar entrega.

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
1. Confirmar mediante la fuente oficial la duración y estructura exacta a emular.
2. Definir la matriz de distribución de las 80 preguntas por subtema y dificultad.
3. Inventariar cuántas preguntas válidas existen por componente/subtema/dificultad.
4. Definir el tiempo máximo de inactividad y la penalización.
5. Definir reglas para repetir preguntas entre simulacros.
6. Definir el contenido informativo disponible por componente/subtema.
7. Definir los permisos exactos del rol administrador.

---

*Documento de trabajo — RutaUdeA. Actualícese conforme avance el desarrollo.*