# Estado del Proyecto RutaUdeA - MVP Simulacro

## Resumen General
El proyecto implementa un MVP para una app de preparación de examen de admisión UdeA con arquitectura MVVM + Jetpack Compose + Room + KSP + Firebase.

**Estado actual: ✅ MVP DEL ASPIRANTE COMPLETO + MEJORAS POST-MVP** - Todas las pestañas operativas (Simulacro, Práctica, Progreso, Info, Perfil); autenticación manual + Google Sign-In, onboarding obligatorio, motor de 80 preguntas, práctica con feedback inmediato, dashboard de progreso offline-first con analítica, contenido educativo por componente, pausa con overlay al cambiar de app, racha diaria de actividad, Home renovado y cuestionario de satisfacción para la prueba piloto (Room local-first + sync Firestore). Build SUCCESS, tests PASS (192).

## 🌿 Ramas
| Rama | Estado | Contenido |
|------|--------|-----------|
| `main` | ✅ Actualizada (PR #12) | MVP + Firebase + motor 80 preguntas + auth gate + práctica + progreso offline-first + pausa + refactor UI/rotación + racha |
| `feat/firebase-config` | ✅ En origin | Firebase Auth + Firestore + sincronización (ya en main vía PR #2) |
| `feat/motor-seleccion-preguntas` | ✅ En origin | Motor de selección 80 preguntas (ya en main vía PR #2) |
| `feat/auth-gate` | ✅ En origin | Gate de autenticación (ya en main vía PR #3) |
| `feat/practica` | ✅ En main (PR #4) | Módulo de Práctica completo + normalización del banco + seeder versionado |
| `feat/progreso` | ✅ En main (PR #5) | Módulo de Progreso: dashboard, evolución, acierto por subtema, recomendaciones, historial |
| `feat/info` | ✅ En main (PR #6) | Módulo Info: contenido educativo de los 29 componentes (asset offline-first) |
| `fix/ui-textos-notch-banco` | ✅ En main (PR #7) | Texto de apoyo visible en simulacro/resultado/práctica (scroll), fix notch edge-to-edge, limpieza banco v4 |
| `feat/progreso-offline-room` | ✅ En main (PR #8) | Progreso offline-first (Room): caches locales, guardado local-first, sync Firebase, índices compuestos |
| `feat/pausa-persistencia-sesion` | ✅ En main (PR #9) | Overlay de pausa al cambiar de app (Simulacro + Práctica), persistencia en SavedStateHandle |
| `rotacion-ui-warning` | ✅ En main (PR #11) | Layouts scrollables/responsivos para rotación, componentes UI compartidos, fix warnings |
| `feat/racha-usuario` | ✅ En main (PR #12) | Racha diaria de actividad por usuario (Room + sync Firebase), tarjeta en Home y Progreso |
| `feat/home-visual` | ✅ En origin | Home renovado: marca bitono + logosímbolo UdeA, saludo personalizado por momento del día, hero de simulacro, tarjeta de racha |
| `feat/encuesta-satisfaccion` | ✅ En main | Cuestionario de satisfacción de la prueba piloto (Room local-first + sync Firestore, accesos en Home y Perfil) |
| `feat/auth-google-onboarding` | 🔨 Rama actual | Registro manual + Google Sign-In (Credential Manager) + onboarding obligatorio + edición de perfil + recuperación de contraseña |

---

## ✅ Lo que ESTÁ LISTO y FUNCIONA

### 1. Capa de Datos (100% completo)
- **Room Database** configurada con:
  - `QuestionEntity` con índices por área, subtema, dificultad, componente
  - `QuestionDao` con consultas para práctica filtrada y simulacro (40 RL / 40 CL)
  - `QuestionRepository` como punto único de acceso
  - `QuestionJsonlLoader` parsea 200 preguntas desde `assets/questions/questions.jsonl` (maneja `JsonNull` correctamente)
  - `DatabaseSeeder` carga inicial automática en primera ejecución
  - Modelo de dominio `Question` con helpers (`respuestaCorrectaTexto()`, `opcionPorLetra()`)

### 2. Preguntas MVP (6 seleccionadas del banco de 200)
| ID | Área | Subtema (normalizado) | Tipo |
|----|------|---------|------|
| profe_alex_4 | RL | RL07 Sucesiones/Series | Suma/Resta |
| profe_alex_7 | RL | RL02 Porcentajes | Suma/Resta |
| profe_alex_23 | RL | RL11 Posibilidades lógicas | Lógica simple |
| CL-ORG-001 | CL | CL11 Supuestos | Texto base |
| CL-ORG-002 | CL | CL02 Inferencia | Texto base |
| CL-ORG-007 | CL | CL03 Idea principal | Texto base |

### 3. UI - Pantallas Implementadas
| Pantalla | Estado | Archivo |
|----------|--------|---------|
| **SplashScreen** | ✅ Funciona | `SplashScreen.kt` + `SplashViewModel` (espera DB seed ≤3s) |
| **HomeScreen** | ✅ Funciona | `HomeScreen.kt` - 5 tabs con botón "Iniciar Simulacro" |
| **SimulacroScreen** | ✅ Funciona | `SimulacroScreen.kt` + `SimulacroViewModel` (80 preguntas motor de selección, timer 120min) |
| **ResultadoScreen** | ✅ Funciona | `ResultadoScreen.kt` + `ResultadoViewModel` (score, %, tiempo, detalle ✓/✗ + 💡) |
| **PerfilScreen** | ✅ Funciona | `PerfilScreen.kt` + `PerfilViewModel` (login/registro Firebase, perfil, logout) |
| **PracticaFiltrosScreen** | ✅ Funciona | `PracticaFiltrosScreen.kt` + `PracticaFiltrosViewModel` (área → subtema → cantidad 5/10/20) |
| **PracticaSesionScreen** | ✅ Funciona | `PracticaSesionScreen.kt` + `PracticaSesionViewModel` (feedback inmediato, respuesta bloqueada, sin límite de tiempo) |
| **PracticaResultadoScreen** | ✅ Funciona | `PracticaResultadoScreen.kt` + `PracticaResultadoViewModel` (score, %, tiempo, repaso con explicación y recomendación) |
| **ProgresoScreen** | ✅ Funciona | `ProgresoScreen.kt` + `ProgresoViewModel` (métricas, gráfica evolución, áreas de mejora/fortalezas, recomendaciones, historial) |
| **InfoScreen** | ✅ Funciona | `InfoScreen.kt` + `InfoViewModel` (contenido educativo por componente, acordeón expandible) |

### 4. Funcionalidades Core del Simulacro
| Función | Estado | Detalle |
|---------|--------|---------|
| Timer 120 min cuenta regresiva | ✅ | `timeRemainingSeconds` en ViewModel (80 preguntas) |
| Persistencia timer (rotación/background) | ✅ | `SavedStateHandle` sobrevive recreación |
| Overlay "Continuar/Abandonar" | ✅ | Modal centrado al volver de background |
| Selección opciones A/B/C/D | ✅ | Visual feedback ✓, color primario |
| Navegación siguiente/anterior/final | ✅ | Botón contextual "Siguiente →" / "Finalizar" |
| Auto-finalizar a 0:00 | ✅ | Navega a Resultado automáticamente |
| Texto de apoyo CL visible | ✅ | `TextoApoyo` (sin recuadro anidado, 16sp, scroll, máx 260.dp) en simulacro, práctica y repaso de resultados |
| Pantalla Resultado | ✅ | Score X/80, %, tiempo, detalle por pregunta con `RepasoPreguntaCard` compartida (sin recuadros internos), enunciado + texto de apoyo compacto |
| Compatibilidad edge-to-edge | ✅ | `safeDrawingPadding()` global en `MainActivity` (Android 15 / targetSdk 35) |

### 5. Navegación (5 tabs Bottom Bar)
| Tab | Ruta | Estado |
|-----|------|--------|
| 🧪 Simulacro | `simulacro` | ✅ Funcional |
| 📝 Práctica | `practica` (+`practica/sesion`, `practica/resultado`) | ✅ Funcional (filtros → sesión → resultado) |
| 📈 Progreso | `progreso` | ✅ Funcional (dashboard + evolución + recomendaciones + historial) |
| 📚 Info | `info` | ✅ Funcional (contenido educativo por componente, 29 temas) |
| 👤 Perfil | `perfil` | ✅ Funcional (login/registro Firebase Auth) |

### 5.1 Autenticación, Google Sign-In y onboarding — IMPLEMENTADO
- **`AuthRepository`**: `register()` (nombre, apellido, correo, contraseña), `login()`, `signInWithGoogle()`, `completeProfile()`, `sendPasswordReset()`, `restoreSession()`, `logout()`. Crea el perfil en `users` con rol `aspirante` y errores mapeados a español.
- **Google Sign-In con Credential Manager** (`GetGoogleIdOption` + `androidx.credentials` + `googleid`):
  - La primera vez Firebase Auth crea la cuenta automáticamente; la app crea `users/{uid}` con `onboardingCompletado=false`.
  - Si el correo ya existe con email/contraseña se muestra un mensaje de colisión (no se vinculan cuentas automáticamente).
  - Requiere el Web Client ID que el plugin genera en `default_web_client_id`.
- **Onboarding obligatorio** (`ui/screen/onboarding/`): nombre, apellido, tipo de institución, institución y grado/semestre. Teléfono, ciudad y fecha del examen son opcionales. Con **“Otra”** se ocultan y limpian grado/semestre. La misma pantalla sirve para **editar el perfil**.
- **`PerfilScreen`**:
  - Login/registro manual con nombre, apellido, correo, contraseña, confirmación y aceptación de términos.
  - Botón **Continuar con Google**, recuperación de contraseña por correo y vista de perfil con datos completos, edición y cierre de sesión.
- **Gate de autenticación**: la app exige sesión y onboarding:
  - `SplashViewModel.initialize()` espera el seed del banco y decide entre **Login**, **Onboarding** y **Home**.
  - Sin sesión → login como raíz (sin acceso a Home).
  - Sesión con `onboardingCompletado=false` → onboarding obligatorio.
  - Sesión con perfil completo → Home.
  - Logout desde cualquier pantalla → login como raíz.
  - Timeout del seed no bloquea el gate (fallback a la verificación de sesión).
- **Modelo `User`** ampliado: `apellido`, `telefono`, `ciudad`, `institucion`, `tipoInstitucion`, `gradoSemestre`, `fechaExamen`, `fotoUrl`, `proveedor`, `onboardingCompletado`.
- **`UserMapper`** centraliza la conversión documento ↔ dominio; los usuarios antiguos sin `onboardingCompletado` se consideran completos.
- **`DatosPerfil`** contiene la validación pura (sin Android) del onboarding.
- **Reglas Firestore**: el dueño solo puede actualizar campos de perfil; no puede cambiar `rol`, `estado`, `correo` ni `fechaCreacion`. La creación exige `rol=aspirante` y `correo == request.auth.token.email`.
- **Tests**: `DatosPerfilTest` (13), `ValidadorCorreoTest` (2), `UserMapperTest` (4), `PerfilViewModelTest` (13), `OnboardingViewModelTest` (6) y `SplashViewModelTest` (5).
- **Pendiente**: vinculación manual de una cuenta Google desde el perfil y tests de instrumentación del flujo.

### 5.2 Sincronización y persistencia (Firestore) — IMPLEMENTADO
- **`google-services.json`** presente en `app/` (reglas completas en `firestore.rules`: users, questions, simulations, simulationQuestions, practiceSessions + practiceQuestions, contexts, components, subtopics, educationalContent, settings; roles aspirante/admin).
- **Fix `firestore.rules`** (rama feat/practica): `isOwner()` estaba definido dentro del bloque `users` pero se usaba en `simulations`/`practiceSessions`/`recommendations` — las funciones dentro de un `match` son locales a ese bloque, el deploy habría fallado. Movida a scope global; añadida subcolección `practiceQuestions` (legible por el dueño de la sesión). **✅ Verificado desplegado en producción (oct 2026): las reglas del proyecto coinciden byte a byte con el repo.**
- **`QuestionRepository.updateBankFromFirestore()`**: descarga el banco maestro (`questions` con `estado=aprobado`) y reemplaza Room.
- **`SimulationRepository.saveSimulation()`**: guarda simulacro completado (resumen + detalle por pregunta); requiere sesión (si no hay, se omite el guardado).
- **`SimulationRepository.getQuestionIdsFromRecentSimulacros(3)`**: usado por el motor para excluir preguntas de los últimos 3 simulacros.

### 5.3 Módulo de Práctica — IMPLEMENTADO (rama `feat/practica`)
- **Decisiones de diseño** (validadas con el equipo):
  - La **dificultad NO es filtrable** por el usuario: la selección se **estratifica por dificultad proporcionalmente a la composición real del banco filtrado** (área + subtema), método de mayor residuo — `PracticeSelector` (clase pura de dominio, testeable).
  - Cantidad elegible: **5 / 10 / 20** preguntas (ajustada a las disponibles con esos filtros).
- **Flujo**: Filtros (área RL/CL → subtema dinámico desde Room o "Todos" → cantidad) → **Sesión** → **Resultado**.
- **Sesión con feedback inmediato** (plan §10): al responder se revela ✓/✗, tu respuesta vs la correcta, 💡 explicación, chips de subtema/dificultad y 📚 recomendación estática por subtema. La respuesta revelada **queda bloqueada**. Sin cronómetro límite (reloj informativo ascendente). Navegación anterior/siguiente, abandono y "terminar ahora" (no reveladas = omitidas). Progreso persistido en `SavedStateHandle` (sobrevive rotación/background).
- **Resultado**: score, %, tiempo usado, repaso por pregunta con explicación + recomendación; "Nueva práctica" (vuelve a filtros conservando filtros) y "Volver al inicio".
- **Persistencia**: `PracticeRepository.savePracticeSession()` guarda en Firestore `practiceSessions` (uid, filtros, score, tiempo) + subcolección `practiceQuestions` (batch, detalle por pregunta) — alimentará el módulo de Progreso.
- **Consultas nuevas**: `QuestionDao.getDistinctSubtemasByArea()` y `countByFilters()` (+ métodos en `QuestionRepository`).
- **Tests**: `PracticeSelectorTest` (7), `PracticaSesionViewModelTest` (7), `PracticaFiltrosViewModelTest` (5).

### 5.5 Módulo de Progreso — IMPLEMENTADO + OFFLINE-FIRST (en main vía PR #8)
- **Dashboard** (tab 📈): métricas globales (nº simulacros, nº prácticas, promedio %, mejor puntaje).
- **Evolución**: gráfica de línea (Canvas, sin dependencias externas) con el % de cada simulacro en orden cronológico; requiere ≥2 simulacros.
- **Acierto por subtema**: agregado de `simulationQuestions` + `practiceQuestions` de las últimas 20 simulacros y 30 prácticas (solo preguntas respondidas); barras con % y evidencia (correctas/respondidas).
- **Áreas de mejora / Fortalezas / Recomendaciones** (plan §12): solo subtemas con **evidencia ≥3 respuestas** («una pregunta no hace un tema débil»); top 5 en mejora/fortalezas, top 3 en recomendaciones.
- **Historial unificado**: simulacros + prácticas ordenados por fecha desc, con score, % y tiempo.
- **Empty state**: sin actividad → CTA a Simulacro/Práctica.
- **Arquitectura offline-first (Room)**: `simulations_cache`, `practice_sessions_cache`, `simulation_questions_cache`, `practice_questions_cache` en Room (`RutaUdeaDatabase` v2, `ProgressDao`). Guardar es **local-first** (Room) y luego best-effort a Firestore; el dashboard **lee siempre de Room** y `ProgressRepository.sincronizar()` sube pendientes y descarga lo de la nube.
- **Índices Firestore**: `firebase.json` + `firestore.indexes.json` (compuestos `uid ASC, fechaCreacion DESC` en `simulations` y `practiceSessions`). La descarga tiene **fallback sin `orderBy`** si el índice no está desplegado.
- **Refresco**: pull-to-refresh + recarga silenciosa al reanudar la pantalla.
- **Bug corregido**: el simulacro que se auto-finaliza al agotar los 120 min ahora sí persiste su resultado (antes se perdía).
- **Tests**: `ProgressMapperTest`, `ProgresoViewModelTest`, `AnalizadorProgresoTest`.
- **Pendiente del módulo**: análisis por componente (área) y por dificultad, vista detalle de un simulacro histórico, limpieza de cache al cerrar sesión.

### 5.6 Módulo Info (educativo) — IMPLEMENTADO (rama `feat/info`)
- **Contenido de los 29 componentes del examen** (15 RL + 14 CL) autorado en `assets/info/contenido_educativo.json`: descripción del componente, estrategia de resolución ("Cómo resolverlo") y errores comunes.
- **Offline-first** (coherente con la decisión del banco): el contenido viaja en la APK; la colección `educationalContent` de Firestore queda para el futuro panel admin.
- **UI** (tab 📚): chips de área (RL/CL) + acordeón expandible por componente (Card con ▼/▲ y AnimatedVisibility).
- **Arquitectura**: `TemaEducativo` (dominio) + `InfoContentLoader` (parseo Gson, degrada a vacío) + `InfoViewModel` (filtro por área, puro y testeable) + `InfoScreen`.
- **Limpieza**: `ComingSoonScreen` **eliminado** (ya no quedan placeholders — las 5 pestañas son funcionales).
- **Tests**: `InfoViewModelTest` (7: parseo, inferencia de área por código, filtros, JSON inválido, estructura del asset real: 29/15/14 sin duplicados).

### 5.8 Pausa y persistencia de sesión (en main vía PR #9)
- **Objetivo**: al cambiar de app y volver, tanto Simulacro como Práctica quedan "en pausa" con un overlay de **Salir / Resumir**.
- **Detección de background real**: `AppBackgroundEffect` observa `ProcessLifecycleOwner` (ON_STOP/ON_START), así la **rotación no** dispara la pausa (a diferencia del ciclo de vida de la Activity).
- **Simulacro** (`feat`): el cronómetro de 120 min **sigue corriendo** en segundo plano (realista). El overlay es informativo y muestra el tiempo restante. `PauseOverlay` compartido reemplaza el overlay propio (que no cubría la pantalla). Botón atrás abre la pausa.
- **Práctica**: el reloj informativo se **congela** al salir y se descuenta el tiempo pausado al resumir (`practica_paused_at_ms`).
- **Persistencia**: `SavedStateHandle` (sobrevive rotación y muerte de proceso del sistema); si el proceso muere en pausa, se reanuda en pausa.
- **Fixes**: `resumeTimer()` ya no borra el overlay en cada tick; si el tiempo se agotó mientras el proceso estaba muerto, el simulacro se finaliza y guarda en vez de quedar atascado en 0:00.
- **UI compartida**: `ui/screen/common/PauseOverlay.kt` (scrim a pantalla completa que bloquea toques y consume el back) + `AppBackgroundEffect.kt`.
- **Dependencia**: `androidx.lifecycle:lifecycle-process`.
- **Tests**: `SimulacroViewModelTest` (5) + 4 casos de pausa en `PracticaSesionViewModelTest`.

### 5.4 Normalización de subtemas del banco (rama `feat/practica`)
- **Problema**: el banco RL mezclaba 2 convenciones (52 preguntas de `profe_alex` con nombres genéricos: "Series", "Geometria", el comodín "Razonamiento logico"... y 48 generadas con códigos oficiales RL01–RL18) → el filtro de práctica mostraba 27 subtemas con conceptos duplicados.
- **Fix**: fusión de nombres genéricos a componentes oficiales + **reclasificación por contenido de las 15 preguntas del comodín** (mapeo id→subtema documentado en el commit). RL quedó en **15 subtemas oficiales** (RL01–RL18) y CL en 14 (CL01–CL14, ya era consistente).
- **RLnn = componentes oficiales del examen de admisión UdeA** (RL01 Proporcionalidad, RL02 Porcentajes... RL18 Lógica; CLnn análogo para Competencia Lectora).
- Archivos migrados: `app/src/main/assets/questions/questions.jsonl` (52 registros) y `docs/preguntas_validadas.json` (v2).
- **Seeder versionado** (`DatabaseSeeder`, v2): los dispositivos con la app ya instalada **se re-siembran automáticamente** al detectar `BANK_SEED_VERSION >` instalada — no hace falta reinstalar ni borrar datos. Regla: incrementar `BANK_SEED_VERSION` en cada cambio del JSONL.
- **Modelo de contenido decidido**: el banco viaja en la APK (offline-first); Firestore `questions` queda para el futuro panel administrativo. La colección **no está poblada** y ninguna funcionalidad activa la lee (`updateBankFromFirestore()` es contrato sin uso). Script de carga listo para ese día: `scripts/upload_bank_to_firestore.js` (Admin SDK, marca `aprobado`/`pendiente` según validez).

### 5.7 Limpieza del banco v3/v4 (rama `fix/ui-textos-notch-banco`)
- **Frase relleno eliminada**: los 92 `texto_base` de CL-ORG repetían 10 veces la frase *"El tema sigue siendo objeto de estudio…"* (relleno sin aporte) → **eliminada por completo** (`scripts/limpiar_banco.py`, banco v4).
- **Scraping truncado**: `profe_alex_52` traía ~28.000 caracteres de comentarios/JS de Blogger en el enunciado → truncado al primer párrafo coherente.
- **12 preguntas RL deshabilitadas** (`estado="deshabilitado"`): referencian figuras/diagramas/tablas que no existen en la app (profe_alex_12/14/15/18/19/36/51/52, RL-ORG-035/037/038/039). El DAO ya filtra `estado='aprobado'` en todas las consultas, así que quedan fuera de selección sin tocar los selectores.
- **Loader respeta `estado` del JSONL** (antes hardcodeaba `"aprobado"`). **Banco aprobado resultante: 88 RL + 100 CL = 188** (el simulacro requiere 40/40: margen sano).
- **`BANK_SEED_VERSION = 4`**: re-siembra automática en dispositivos instalados.
- **UI**: `TextoApoyo` (componente común, sin recuadro anidado, 16sp, scroll acotado 260.dp) — el simulacro ya muestra los textos de apoyo de las preguntas CL, la práctica ya no desborda con textos largos, y el resultado permite releer el texto al repasar. Home actualizado: "Simulacro — 120 min • 80 preguntas • 40 RL + 40 CL".

### 5.9 Racha diaria de actividad (en main vía PR #12, rama `feat/racha-usuario`)
- **Reglas puras de dominio** (`AnalizadorRacha`, testeable, con `Reloj` inyectable): días calendario consecutivos con ≥1 actividad completada (simulacro o práctica de cualquier tamaño); idempotente por día; hueco >1 día reinicia en 1; día anterior se ignora; `mejor` histórico nunca disminuye. Racha «vigente»: viva si la última actividad fue hoy o ayer (tolerancia 1 día), si no se muestra 0 conservando el récord.
- **Persistencia local-first** (Room `rachas_cache`, migración 2→3, `RachaDao` + `RachaMapper`) con `RachaRepository`: registra actividad al completar simulacros y prácticas, guarda en local y sincroniza best-effort a `rachas/{uid}` en Firestore (fusión local-nube que conserva lo más favorable).
- **UI**: tarjeta de racha (actual + mejor) en Home (`HomeViewModel`) y en Progreso (`ProgressRepository.cargarRacha` incluida en el sync). El Home refresca la racha al volver de otra pantalla (`alReanudar()` en el ViewModel, no en `remember` de la UI — la composición se destruye al navegar).
- **Reglas Firestore**: colección `rachas` (dueño + admin).
- **Tests**: `AnalizadorRachaTest` (13), `RachaMapperTest` (4), `RachaRepositoryTest` (10), `HomeViewModelTest` (3), `ProgresoRachaTest` (2).

### 5.10 Refactor UI compartida + rotación (en main vía PR #11, rama `rotacion-ui-warning`)
- **Componentes extraídos a `ui/screen/common`** (el quality gate fallaba por código duplicado): `BarraProgresoSesion` (encabezado + progreso con slot para cronómetro/abandono), `OpcionesPreguntaCard` (opciones A/B/C/D con selección y feedback ✓/✗), `ResumenScoreCard` (cabecera de resultado: aciertos, anillo %, tiempo). Simulacro/Práctica y ambos Resultados los consumen sin cambios de comportamiento.
- **Layouts scrollables y responsivos para rotación**: en horizontal el `Column` raíz no scrolleable dejaba listas internas inalcanzables → Home en un único `LazyColumn`; resultados con raíz `LazyColumn` (detalle por pregunta como items, sin `weight` que colapse); Simulacro con el mismo patrón que Práctica (`Column` con `verticalScroll`, enunciado y opciones en cards separadas); Splash adapta logo/paddings a la orientación.
- **Calidad**: indicador de opción unificado (un solo `✓/✗`, fix warning Sonar de duplicación); indicadores de progreso migrados a la sobrecarga con lambda (no deprecada); `*Content` sin estado extraídos + `@Preview` vertical/horizontal en Home, Splash, Simulacro y ambos Resultados.
- **Fix banco**: condición muerta en `QuestionJsonlLoader` (`this == null` sobre receptor no nulo).

### 5.11 Home renovado (rama actual `feat/home-visual`, pendiente de PR)
- **Marca**: título bitono «Ruta UdeA» + logosímbolo vertical UdeA (`drawable-nodpi/logosimbolo_vertical.png`, reducido a 256px).
- **Saludo personalizado**: `Saludo` (dominio puro, testeado) según momento del día (Buenos días 5–11 / Buenas tardes 12–18 / Buenas noches) + primer nombre del usuario (vía `AuthRepository.restoreSession()` en `HomeViewModel`); momento y nombre con estilos distintos, prompt «¿Qué quieres practicar hoy?» + descripción de la propuesta de valor.
- **Hero de simulacro** en `primaryContainer` con insignia, tarjeta de racha en primaria oscura con mensaje motivacional y récord, secciones con insignias tonales.
- **Tests**: `SaludoTest`, `HomeViewModelTest` actualizado.

### 5.12 Cuestionario de satisfacción piloto (rama actual `feat/encuesta-satisfaccion`)
- **Cuestionario** (ruta `encuesta`, respuesta única por usuario): A. Datos — tipo de institución (Colegio/Universidad/Otra), nombre de la institución, grado (colegio) o semestre (universidad, desplegable condicional) y si ya presentó el examen real; B. Satisfacción por módulo (Simulacro/Práctica/Progreso/Info, Likert 1–5 con emojis); C. Calidad de las preguntas (claridad RL, claridad CL, dificultad adecuada); D. Global (facilidad de uso + NPS 0–10); E. Abiertas opcionales (lo que más gustó / qué mejorar).
- **Accesos llamativos**: tarjeta «🎓 Tu opinión cuenta» al final del Home (con estado pendiente/completada) + sección «Prueba piloto» en el Perfil debajo de los datos del usuario.
- **Persistencia local-first** (patrón racha): Room `encuestas_cache` (migración 3→4, `EncuestaDao` + `EncuestaMapper`) y `EncuestaRepository` (upsert local + subida best-effort a `encuestasSatisfaccion/{uid}`, un doc por usuario para el análisis; reintento de pendientes; precarga local→nube para actualizar respuestas).
- **UI atractiva y horizontal**: encabezado con barra de avance (14 pasos), 4 secciones en cards con insignias tonales, Likert con caritas seleccionables, NPS 0–10 en `FlowRow`, desplegables M3, errores en card, agradecimiento con estado de sincronización (☁️/⏳). Raíz `LazyColumn` + `@Preview` vertical/horizontal.
- **Reglas Firestore**: colección `encuestasSatisfaccion` (dueño escribe/lee, admin lee todo).
- **Tests**: `EncuestaSatisfaccionTest` (7), `EncuestaRepositoryTest` (7), `EncuestaViewModelTest` (6), `HomeViewModelTest` +2 (estado de encuesta).

### 6. Tema Material3
- **Primary**: Forest Green (#1B4D3E)
- **Secondary**: Blue Accent (#2196F3)
- Bordes redondeados 16dp, elevación 2-4dp
- Logo vectorial personalizado (`ic_splash_logo.xml`)

### 7. Tests Unitarios
- ✅ `QuestionMapperTest` - 7 tests pasan
- ✅ `QuestionJsonlLoaderTest` - 5 tests pasan
- ✅ `DatabaseSeederTest` - 4 tests pasan (política de siembra versionada)
- ✅ `QuestionSelectorTest` - 11 tests pasan (motor de selección)
- ✅ `SplashViewModelTest` - 5 tests pasan (destinos login/onboarding/home + fallback del seed)
- ✅ `DatosPerfilTest` - 13 tests pasan (campos obligatorios, “Otra” sin grado/semestre, teléfono, mapeo)
- ✅ `ValidadorCorreoTest` - 2 tests pasan (formatos válidos e inválidos)
- ✅ `UserMapperTest` - 4 tests pasan (documento ↔ dominio, usuarios antiguos, campos inmutables)
- ✅ `PerfilViewModelTest` - 13 tests pasan (registro, login, Google, recuperación, eventos)
- ✅ `OnboardingViewModelTest` - 6 tests pasan (precarga, “Otra”, validación, guardado, error, logout)
- ✅ `PracticeSelectorTest` - 7 tests pasan (estratificación proporcional, mayor residuo)
- ✅ `PracticaSesionViewModelTest` - 7 tests pasan (feedback, bloqueo, navegación, SAH, restauración)
- ✅ `PracticaFiltrosViewModelTest` - 5 tests pasan (subtemas por área, disponibles, iniciar)
- ✅ `AnalizadorProgresoTest` - 9 tests pasan (evidencia mínima, orden, evolución, historial)
- ✅ `ProgressMapperTest` - tests pasan (entidad ↔ dominio de caches)
- ✅ `ProgresoViewModelTest` - tests pasan (dashboard, sync, racha)
- ✅ `InfoViewModelTest` - 7 tests pasan (parseo, filtro por área, estructura del asset)
- ✅ `BancoIntegridadTest` - 5 tests pasan (200 preguntas, sin frase repetida, 12 deshabilitadas exactas, cobertura simulacro, scraping truncado)
- ✅ `SimulacroViewModelTest` - tests pasan (motor + 5 de pausa/persistencia)
- ✅ `AnalizadorRachaTest` - 13 tests pasan (días consecutivos, idempotencia, vigencia, fusión local-nube)
- ✅ `RachaMapperTest` - 4 tests pasan
- ✅ `RachaRepositoryTest` - 10 tests pasan (registro, local-first, sync best-effort)
- ✅ `HomeViewModelTest` - 3 tests pasan (racha vigente + saludo con nombre)
- ✅ `ProgresoRachaTest` - 2 tests pasan (racha en dashboard de progreso)
- ✅ `SaludoTest` - tests pasan (momentos del día, primer nombre, sin nombre)
- ✅ `EncuestaSatisfaccionTest` - 7 tests pasan (validez, grado/semestre condicional, abiertas opcionales, rangos)
- ✅ `EncuestaRepositoryTest` - 7 tests pasan (local-first sin red, pendiente, validación, sin sesión, sync)
- ✅ `EncuestaViewModelTest` - 6 tests pasan (precarga, validación, envío, sync pendiente, limpieza de errores)

### 8. Motor de selección de preguntas (100% completo)
- **`QuestionSelector`** (`domain/services/QuestionSelector.kt`) — clase pura de dominio, testeable:
  - 80 preguntas: 40 RL + 40 CL, distribución de dificultad **proporcional al banco** (método de mayor residuo)
  - Cobertura de subtemas: cuotas proporcionales por subtema y secuencia entrelazada
  - Sin preguntas repetidas dentro del simulacro
  - **Excluye preguntas de los últimos 3 simulacros** del usuario (Firestore `simulationQuestions`, relajación gradual por área y global si el banco no alcanza)
  - Cuotas **flexibles**: si un grupo no alcanza, rellena con preguntas sobrantes (mismo área primero)
  - Valida las 80 antes de iniciar (falla con mensaje si el banco válido < 80)
- **Selección persistida** en `SavedStateHandle` (JSON de IDs): no cambia al rotar/minimizar; se restaura por ID desde Room
- **Timer 120 min** para 80 preguntas (antes 5 min / 6 preguntas)
- **ResultadoViewModel** recibe las preguntas seleccionadas vía `SavedStateHandle` (ya no las 6 del MVP)
- **Firestore rules**: `simulationQuestions` legible por el dueño del simulacro (necesario para la exclusión)
- `MvpQuestionProvider` eliminado (sin uso tras la integración del motor)

### 8. Build & Deploy
- ✅ `./gradlew compileDebugKotlin` - **EXITOSO**
- ✅ `./gradlew assembleDebug` - **APK generado**
- ✅ `./gradlew installDebug` - **Instala en dispositivo físico**
- ✅ `./gradlew test` - **PASAN (192 tests)**

---

## ❌ PROBLEMAS PENDIENTES / NO IMPLEMENTADOS

### 0. Calidad del banco (Alto)
- **Resuelto (v3)**: frase relleno duplicada en 92 textos CL, scraping de `profe_alex_52`, y 12 preguntas RL con figuras inexistentes → deshabilitadas. Banco aprobado: **88 RL + 100 CL = 188**.
- **Pendiente**: ~20 preguntas RL aprobadas siguen sin `respuesta_correcta` o enunciado (los selectores las excluyen con `esValida`; pool válido efectivo ≈ 68 RL). Completar con curación manual o el panel administrativo.

### 1. Iconos de la App (Crítico)
- **Problema**: Adaptive icons configurados (`ic_launcher_foreground.xml` + `launcher_background #1B4D3E`) pero se ve cuadrado verde en launcher
- **Causa probable**: `ic_launcher_foreground` sin alpha channel correcto o vector no renderiza en algunos launchers; falta generar PNG reales (hdpi/mdpi/xhdpi/xxhdpi/xxxhdpi) en `app/src/main/res/mipmap-*/ic_launcher.png` y `ic_launcher_round.png`
- **Impacto**: App se instala pero icono no visible correctamente

### 2. Limpieza de código (Menor)
- ✅ `ViewModelFactory.kt` vacío sin uso → **ELIMINADO**
- ✅ `BottomBar.kt` sin uso (HomeScreen no lo renderiza) → **ELIMINADO**
- ✅ `HomeViewModel.kt` vacío sin uso → **ELIMINADO**
- ✅ Funciones `create*` duplicadas y `HomeFactory` en `ViewModelFactories.kt` → **ELIMINADAS**
- ✅ `resultadoRoute()` / `SIMULACRO_START_DEST` (era JSON-en-URL) → **ELIMINADOS**
- ✅ Métodos muertos en `SimulacroViewModel` (`getSaved*`, `onAbandon`, `clearSavedState`, getter duplicado, `isRunning`) → **ELIMINADOS**
- ✅ ~20 imports y variables muertas en screens/nav → **LIMPIADOS**

### 3. Dagger/Hilt NO configurado
- **Estado**: Inyección manual via `ViewModelFactories` object
- **Futuro**: Migrar a Hilt para producción

### 4. Firebase configurado
- **Estado**: ✅ Auth (login/registro en Perfil) + Firestore (reglas completas) + sincronización del banco + persistencia de simulacros (rama feat/firebase-config)
- **Pendiente**: Analytics, Crashlytics, push a origin

### 6. Nota sobre JDK local
- `.mise.toml` pide temurin-17 pero en esta máquina no hay `java` en PATH.
- Compilar con: `export JAVA_HOME=/home/michael/.jdks/jbr-21.0.11 && ./gradlew ...` (JBR 21 de Android Studio; el JDK 25 de `/opt/android-studio/jbr` NO es compatible con Gradle 8.11).

### 5. Tests de Instrumentación
- **Estado**: Solo tests unitarios (JUnit + MockK)
- **Falta**: Tests de UI (Compose Test), tests de integración Room

---

## 📁 Estructura de Archivos Clave

```
app/src/main/java/com/udea/rutaudea/
├── MainActivity.kt                    # Entry point - usa AppNavHost()
├── RutaUdeaApp.kt                     # Application class - expone repository y siembra banco
├── di/
│   ├── AppModule.kt                   # provideQuestion/Auth/Simulation repositories
│   └── ViewModelFactories.kt          # Splash/Simulacro/Resultado/Perfil factories
├── data/
│   ├── local/                         # Room (v3) - COMPLETO (questions + caches de progreso + racha)
│   ├── mapper/                        # ✅
│   ├── repository/
│   │   ├── QuestionRepository.kt      # ✅ Banco local + updateBankFromFirestore()
│   │   ├── AuthRepository.kt          # ✅ Firebase Auth + colección users
│   │   ├── SimulationRepository.kt    # ✅ Guardado local-first (Room + Firestore)
│   │   ├── PracticeRepository.kt      # ✅ Guardado local-first (Room + Firestore batch)
│   │   ├── ProgressRepository.kt      # ✅ Dashboard offline-first + sync + racha
│   │   └── RachaRepository.kt         # ✅ Racha diaria local-first + sync best-effort
│   │   └── EncuestaRepository.kt      # ✅ Encuesta piloto local-first + sync best-effort
│   └── source/local/                  # ✅ JsonL loader + Seeder versionado (v4)
├── domain/
│   ├── model/Question.kt              # ✅ Modelo dominio
│   ├── model/User.kt                  # ✅ Modelo usuario (uid, nombre, correo, rol)
│   ├── services/QuestionSelector.kt   # ✅ Motor de selección 80 preguntas (testeado)
│   └── services/PracticeSelector.kt   # ✅ Estratificación proporcional de práctica (testeado)
│   │   ├── services/AnalizadorRacha.kt    # ✅ Reglas de racha diaria (testeado)
│   │   └── services/Saludo.kt             # ✅ Saludo por momento del día (testeado)
│   │   ├── model/Racha.kt                 # ✅ Racha (actual, mejor, último día)
│   │   ├── model/EncuestaSatisfaccion.kt  # ✅ Cuestionario piloto + validación (testeado)
├── ui/
│   ├── navigation/
│   │   ├── AppNavHost.kt              # ✅ NavHost principal (10 destinos)
│   │   ├── screen/common/               # ✅ PauseOverlay, BarraProgresoSesion, OpcionesPreguntaCard, ResumenScoreCard
│   │   └── AppNavGraph.kt             # ✅ Rutas
│   ├── screen/
│   │   ├── splash/                    # ✅ Splash + ViewModel (poll DB seed + gate auth)
│   │   ├── home/                      # ✅ Home renovado + HomeViewModel (saludo, racha, marca UdeA)
│   │   ├── simulacro/                 # ✅ 80 preguntas, timer 120 min, overlay de pausa
│   │   ├── resultado/                 # ✅ Resultado + ViewModel (lee SavedStateHandle previo)
│   │   ├── perfil/                    # ✅ Login/Registro/Perfil (Firebase Auth)
│   │   ├── practica/                  # ✅ Filtros + Sesión feedback + Resultado
│   │   ├── info/                     # ✅ Contenido educativo por componente (acordeón)
│   └── theme/                         # ✅ Material3 Forest Green
```

---

## 🚀 PRÓXIMOS PASOS PRIORITARIOS

| Prioridad | Tarea | Esfuerzo |
|-----------|-------|----------|
| **P0** | PR a `main`: `feat/home-visual` (Home renovado) + `feat/encuesta-satisfaccion` (cuestionario piloto) | 30 min |
| ~~**P0**~~ | ~~PRs a `main`: `feat/practica` → `feat/progreso` → `feat/info`~~ | ✅ HECHO (PRs #4, #5, #6) |
| ~~**P0**~~ | ~~Progreso offline-first (Room local-first + sync)~~ | ✅ HECHO (PR #8) |
| ~~**P0**~~ | ~~Pausa con overlay al cambiar de app~~ | ✅ HECHO (PR #9) |
| ~~**P0**~~ | ~~Layouts responsivos para rotación + componentes UI compartidos~~ | ✅ HECHO (PR #11) |
| ~~**P1**~~ | ~~Racha diaria (gamificación inicial)~~ | ✅ HECHO (PR #12) |
| ~~**P0**~~ | ~~Gate de login al inicio~~ | ✅ HECHO (en main vía PR #3) |
| ~~**P0**~~ | ~~Eliminar código basura sin uso~~ | ✅ HECHO (ComingSoonScreen eliminado en feat/info) |
| **P1** | Análisis por componente y dificultad en Progreso (por subtema ya está) | 3 horas |
| **P1** | Inactividad del cronómetro + registro de abandono (P0 del backlog) | 4 horas |
| **P1** | Migrar a Hilt para DI | 2 horas |
| ~~**P1**~~ | ~~Contenido educativo Info por subtema~~ | ✅ HECHO (feat/info) |
| **P2** | Vista detalle de un simulacro del historial | 3 horas |
| **P2** | Recuperación de contraseña (sendPasswordResetEmail) | 1 hora |
| **P2** | Tests UI Compose + Instrumentación | 4 horas |
| **P3** | Generar iconos PNG reales (mipmap-*) / fix adaptive icon | 30 min |

---

## 📱 Cómo Probar Ahora

```bash
# 1. Compilar
./gradlew assembleDebug

# 2. Instalar en dispositivo conectado (USB debugging ON)
./gradlew installDebug

# 3. O abrir APK generado
# app/build/outputs/apk/debug/app-debug.apk
```

**Verificar en dispositivo**:
1. Splash → "Cargando banco..." → **SIN sesión: login** (raíz, sin "Volver al inicio") / **CON sesión: Home directo**
2. Login/Registro → al autenticarse entra a Home (Home queda como raíz)
3. Home → 5 tabs abajo, botón verde "Iniciar Simulacro"
4. Tab 👤 Perfil → perfil logueado; "Cerrar sesión" → vuelve al login como raíz
5. Simulacro → 80 preguntas, timer 120:00, 4 opciones, progreso n/80
6. Timer → cuenta 120:00 → 00:00 (rojo < 1 min)
7. Responder → ✓ visual → "Siguiente →" / "Finalizar"
8. Background → Home → volver → Overlay "Continuar/Abandonar" → Continua timer
9. Final → Resultado → Score X/80, %, tiempo, lista expandible ✓/✗ + 💡
10. Con sesión (garantizada por el gate) → el simulacro se guarda en Firestore
11. Tab 📝 Práctica → filtros (área RL/CL, subtema dinámico, cantidad 5/10/20, disponibles visibles)
12. Iniciar → sesión sin límite de tiempo; al responder: ✓/✗ inmediato + 💡 explicación + 📚 recomendación; respuesta bloqueada; ←/→ navegar; Abandonar/Terminar
13. Resultado práctica → score/%, tiempo, repaso por pregunta; "Nueva práctica" (conserva filtros) / "Volver al inicio"
14. Firestore → colección `practiceSessions` con la sesión + subcolección `practiceQuestions`
15. Tab 📈 Progreso → dashboard: nº simulacros/prácticas, promedio, mejor; gráfica de evolución (≥2 simulacros); áreas de mejora y fortalezas por subtema (solo con ≥3 respuestas); recomendaciones; historial unificado
16. Sin actividad → Progreso muestra empty state con botones a Simulacro/Práctica
17. Tab 📚 Info → chips RL/CL; 29 componentes en acordeón; expandir muestra descripción + "Cómo resolverlo" + "Errores comunes"
18. Cambiar de app durante el simulacro → al volver, overlay de pausa a pantalla completa (Salir/Resumir); el cronómetro de 120 min siguió corriendo. En práctica, el reloj se congeló y se descuenta al resumir
19. Rotar el dispositivo en Home/Simulacro/Resultados → todo el contenido sigue alcanzable con scroll; sin solapamientos
20. Completar un simulacro o práctica → Home y Progreso muestran la racha (1 día); completar otro día consecutivo → racha +1; mismo día → sin cambios
21. Home → marca «Ruta UdeA» + logosímbolo, saludo según la hora («Buenas tardes, <nombre>»), hero de simulacro y tarjeta de racha
22. Final del Home → tarjeta «🎓 Tu opinión cuenta» → encuesta piloto (datos → módulos → preguntas → opinión, barra de avance, caritas Likert, NPS); enviar sin red → «pendiente de sincronizar»
23. Tab 👤 Perfil → sección «Prueba piloto» debajo de los datos → misma encuesta; si ya respondió, precarga para actualizar
24. Firestore → colección `encuestasSatisfaccion/{uid}` con un documento por usuario para el análisis

---

## 📝 Notas Técnicas

- **KSP** configurado para Room (no KAPT)
- **Mise** gestiona JDK 17 (temurin-17) y Android SDK
- **Kotlin 2.0.20** + Compose Compiler Plugin
- **Gradle 8.11** + AGP 8.5.2
- **Min SDK 26**, Target/Compile SDK 35

---

## 🔧 Fixes Recientes (Sept 2024)

| Issue | Solución |
|-------|----------|
| Splash se quedaba pillado | `SplashViewModel.loadQuestions()` hace poll a `countQuestions()` hasta >0 (timeout 3s) |
| Crash `JsonNull` en JSONL | Extensions `getStringOrNull()` / `getBooleanOrNull()` en `QuestionJsonlLoader` |
| Resultados vacío (0/0) | `ResultadoViewModel` recibe `userAnswers` via `previousBackStackEntry.savedStateHandle` |
| Crash navegación resultado | Eliminado JSON en URL; usa `SavedStateHandle` directo entre destinos |
| `SimulacroScreen` no reactiva | `collectAsStateWithLifecycle()` en lugar de `.value` snapshot |
| Imports ambiguos `NavType` | Limpieza en `AppNavHost.kt` |
| ViewModel factories incorrectos | `ViewModelProvider.Factory` implementations en `ViewModelFactories.kt` |
| Botón "Finalizar" cerraba app | `navigate(HOME)` con `popUpTo(HOME, inclusive=false)` en lugar de `popBackStack(inclusive=true)` |
| Layout detalle/resultados | LazyColumn con `.weight(1f, fill=false)` para no empujar botón fuera de pantalla |
| Async results no renderizaban | Usar `questionResults` StateFlow directo en `items()` en lugar de `.value` |
| Crash al pulsar "Finalizar" en Resultado (app se cerraba) | `getBackStackEntry(SIMULACRO)` se reevaluaba durante la animación de salida (ya poppeado) → `IllegalArgumentException`. Fix: lectura con `remember {}` una sola vez + `popBackStack(HOME, inclusive=false)` en vez de `navigate(HOME)` |
| Logo no se mostraba en SplashScreen | `ic_splash_logo.xml` pinta todo el canvas verde oscuro de fondo; el `ColorFilter.tint(ForestGreen)` teñía TODO el vector (cuadrado verde sólido). Fix: eliminado el tint, logo se muestra con sus colores originales a 120.dp |

| Botón "Abandonar" del overlay cerraba la app | `popBackStack(HOME, inclusive=true)` vaciaba el back stack → `inclusive=false` para volver a Home |
| Simulacro no mostraba textos CL | `SimulacroScreen` solo renderizaba `pregunta` → añadido `TextoApoyoCard` (componente común, scroll acotado 240.dp) con `textoBase ?: contexto` |
| Frase repetida al final de textos CL | Era el **dato**, no la UI: 92 `texto_base` traían la frase plantilla 10 veces → **eliminada por completo** con `scripts/limpiar_banco.py` (banco v4) |
| Info bajo el notch | `targetSdk=35` fuerza edge-to-edge en Android 15 y no había insets en ninguna pantalla → `safeDrawingPadding()` global en `MainActivity` |
| Card doble/padding excesivo en texto de apoyo | `TextoApoyoCard` anidaba una Card con fondo dentro de la Card de pregunta (doble padding ~32dp, fuente 14sp) → reemplazado por `TextoApoyo` plano: etiqueta + texto 16sp, sin recuadro |
| Detalle de resultados con recuadros anidados | Unificados ambos resultados (simulacro y práctica) en el componente compartido `RepasoPreguntaCard` (`ui/screen/common/`): Card tintada por estado (correcta/incorrecta/omitida) con contenido plano — encabezado, enunciado, texto de apoyo compacto, corrección, 💡 y 📚 — sin ningún cuadrado interno que duplique márgenes |
| Home decía "Simulacro MVP / 5 min / 6 preguntas" | Texto obsoleto del MVP → "Simulacro / 120 min • 80 preguntas • 40 RL + 40 CL" |
| Simulacro auto-finalizado por tiempo no se guardaba | Se perdía el resultado al llegar a 0:00 → ahora persiste en Room + Firestore antes de navegar a Resultado |
| Rotación dejaba contenido inalcanzable | `Column` raíz no scrolleable + listas internas sin espacio en horizontal → Home en un único `LazyColumn`, resultados con raíz `LazyColumn`, Simulacro con `verticalScroll` (PR #11) |
| Home no refrescaba la racha al volver | Guard de primera vez en `remember` de la UI (se destruye al navegar) → movido al ViewModel con `alReanudar()` en Home y Progreso |

---

*Última actualización: 2026-10-25 - Rama `feat/auth-google-onboarding`: registro manual + Google Sign-In con Credential Manager + onboarding obligatorio (con opción “Otra” sin grado/semestre) + edición de perfil + recuperación de contraseña + reglas Firestore endurecidas. Verificado: `testDebugUnitTest` + `assembleDebug` BUILD SUCCESSFUL (192 tests PASS) y `firebase deploy --only firestore:rules` OK.*