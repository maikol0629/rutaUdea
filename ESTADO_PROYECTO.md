# Estado del Proyecto RutaUdeA - MVP Simulacro

## Resumen General
El proyecto implementa un MVP para una app de preparación de examen de admisión UdeA con arquitectura MVVM + Jetpack Compose + Room + KSP + Firebase.

**Estado actual: ✅ MVP DEL ASPIRANTE COMPLETO** - Todas las pestañas operativas (Simulacro, Práctica, Progreso, Info, Perfil); login/registro obligatorio, motor de 80 preguntas, práctica con feedback inmediato, dashboard de progreso con analítica, contenido educativo por componente. Banco normalizado a componentes oficiales. Build SUCCESS, tests PASS (66).

## 🌿 Ramas
| Rama | Estado | Contenido |
|------|--------|-----------|
| `main` | ✅ Actualizada (PR #3) | MVP + Firebase completo + motor de 80 preguntas + gate de autenticación |
| `feat/firebase-config` | ✅ En origin | Firebase Auth + Firestore + sincronización (ya en main vía PR #2) |
| `feat/motor-seleccion-preguntas` | ✅ En origin | Motor de selección 80 preguntas (ya en main vía PR #2) |
| `feat/auth-gate` | ✅ En origin | Gate de autenticación (ya en main vía PR #3) |
| `feat/practica` | ✅ En origin | Módulo de Práctica completo + normalización del banco + seeder versionado |
| `feat/progreso` | ✅ En local, apilada | Módulo de Progreso: dashboard, evolución, acierto por subtema, recomendaciones, historial |
| `feat/info` | 🔨 Rama actual de trabajo | Módulo Info: contenido educativo de los 29 componentes (asset offline-first) |

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
| Pantalla Resultado | ✅ | Score X/80, %, tiempo, lista expandible ✓/✗ + 💡 explicación |

### 5. Navegación (5 tabs Bottom Bar)
| Tab | Ruta | Estado |
|-----|------|--------|
| 🧪 Simulacro | `simulacro` | ✅ Funcional |
| 📝 Práctica | `practica` (+`practica/sesion`, `practica/resultado`) | ✅ Funcional (filtros → sesión → resultado) |
| 📈 Progreso | `progreso` | ✅ Funcional (dashboard + evolución + recomendaciones + historial) |
| 📚 Info | `info` | ✅ Funcional (contenido educativo por componente, 29 temas) |
| 👤 Perfil | `perfil` | ✅ Funcional (login/registro Firebase Auth) |

### 5.1 Autenticación (Firebase Auth) — IMPLEMENTADO (incluye gate)
- **`AuthRepository`**: `register()`, `login()`, `restoreSession()`, `logout()` con email/contraseña; crea perfil en colección `users` (rol `aspirante`); errores mapeados a español.
- **`PerfilScreen`**: toggle Iniciar sesión / Crear cuenta, validación de correo/contraseña, vista de perfil logueado (nombre, correo, rol) y "Cerrar sesión".
- **Gate de autenticación (rama feat/auth-gate)**: la app **exige sesión** para entrar:
  - `SplashViewModel.initialize()` espera el seed del banco y verifica `auth.currentUser`.
  - Sin sesión → splash navega al **login (Perfil en modo gate)** como raíz; el botón "Volver al inicio" se oculta.
  - Login/registro exitoso → evento `Autenticado` → **Home se convierte en la raíz** (back stack limpio).
  - Logout desde cualquier pantalla → evento `SesionCerrada` → vuelve al login como raíz.
  - Timeout del seed no bloquea el gate (fallback a la verificación de sesión).
  - Tests: `SplashViewModelTest` (4 tests del gate).
- **Pendiente**: recuperación de contraseña, tests de instrumentación del flujo.

### 5.2 Sincronización y persistencia (Firestore) — IMPLEMENTADO
- **`google-services.json`** presente en `app/` (reglas completas en `firestore.rules`: users, questions, simulations, simulationQuestions, practiceSessions + practiceQuestions, contexts, components, subtopics, educationalContent, settings; roles aspirante/admin).
- **Fix `firestore.rules`** (rama feat/practica): `isOwner()` estaba definido dentro del bloque `users` pero se usaba en `simulations`/`practiceSessions`/`recommendations` — las funciones dentro de un `match` son locales a ese bloque, el deploy habría fallado. Movida a scope global; añadida subcolección `practiceQuestions` (legible por el dueño de la sesión).
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

### 5.5 Módulo de Progreso — IMPLEMENTADO (rama `feat/progreso`)
- **Dashboard** (tab 📈): métricas globales (nº simulacros, nº prácticas, promedio %, mejor puntaje).
- **Evolución**: gráfica de línea (Canvas, sin dependencias externas) con el % de cada simulacro en orden cronológico; requiere ≥2 simulacros.
- **Acierto por subtema**: agregado de `simulationQuestions` + `practiceQuestions` de las últimas 5 simulacros y 10 prácticas (solo preguntas respondidas); barras con % y evidencia (correctas/respondidas).
- **Áreas de mejora / Fortalezas / Recomendaciones** (plan §12): solo subtemas con **evidencia ≥3 respuestas** («una pregunta no hace un tema débil»); top 5 en mejora/fortalezas, top 3 en recomendaciones.
- **Historial unificado**: simulacros + prácticas ordenados por fecha desc, con score, % y tiempo.
- **Empty state**: sin actividad → CTA a Simulacro/Práctica.
- **Arquitectura**: `ProgressRepository` (lectura Firestore, graceful sin conexión), `AnalizadorProgreso` (dominio puro, 9 tests), `ProgresoViewModel`, `ProgresoScreen`.
- **Pendiente del módulo**: análisis por componente (área) y por dificultad, vista detalle de un simulacro histórico, pull-to-refresh.

### 5.6 Módulo Info (educativo) — IMPLEMENTADO (rama `feat/info`)
- **Contenido de los 29 componentes del examen** (15 RL + 14 CL) autorado en `assets/info/contenido_educativo.json`: descripción del componente, estrategia de resolución ("Cómo resolverlo") y errores comunes.
- **Offline-first** (coherente con la decisión del banco): el contenido viaja en la APK; la colección `educationalContent` de Firestore queda para el futuro panel admin.
- **UI** (tab 📚): chips de área (RL/CL) + acordeón expandible por componente (Card con ▼/▲ y AnimatedVisibility).
- **Arquitectura**: `TemaEducativo` (dominio) + `InfoContentLoader` (parseo Gson, degrada a vacío) + `InfoViewModel` (filtro por área, puro y testeable) + `InfoScreen`.
- **Limpieza**: `ComingSoonScreen` **eliminado** (ya no quedan placeholders — las 5 pestañas son funcionales).
- **Tests**: `InfoViewModelTest` (7: parseo, inferencia de área por código, filtros, JSON inválido, estructura del asset real: 29/15/14 sin duplicados).

### 5.4 Normalización de subtemas del banco (rama `feat/practica`)
- **Problema**: el banco RL mezclaba 2 convenciones (52 preguntas de `profe_alex` con nombres genéricos: "Series", "Geometria", el comodín "Razonamiento logico"... y 48 generadas con códigos oficiales RL01–RL18) → el filtro de práctica mostraba 27 subtemas con conceptos duplicados.
- **Fix**: fusión de nombres genéricos a componentes oficiales + **reclasificación por contenido de las 15 preguntas del comodín** (mapeo id→subtema documentado en el commit). RL quedó en **15 subtemas oficiales** (RL01–RL18) y CL en 14 (CL01–CL14, ya era consistente).
- **RLnn = componentes oficiales del examen de admisión UdeA** (RL01 Proporcionalidad, RL02 Porcentajes... RL18 Lógica; CLnn análogo para Competencia Lectora).
- Archivos migrados: `app/src/main/assets/questions/questions.jsonl` (52 registros) y `docs/preguntas_validadas.json` (v2).
- **Seeder versionado** (`DatabaseSeeder`, v2): los dispositivos con la app ya instalada **se re-siembran automáticamente** al detectar `BANK_SEED_VERSION >` instalada — no hace falta reinstalar ni borrar datos. Regla: incrementar `BANK_SEED_VERSION` en cada cambio del JSONL.
- **Modelo de contenido decidido**: el banco viaja en la APK (offline-first); Firestore `questions` queda para el futuro panel administrativo. La colección **no está poblada** y ninguna funcionalidad activa la lee (`updateBankFromFirestore()` es contrato sin uso). Script de carga listo para ese día: `scripts/upload_bank_to_firestore.js` (Admin SDK, marca `aprobado`/`pendiente` según validez).

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
- ✅ `SplashViewModelTest` - 4 tests pasan (gate de autenticación)
- ✅ `PracticeSelectorTest` - 7 tests pasan (estratificación proporcional, mayor residuo)
- ✅ `PracticaSesionViewModelTest` - 7 tests pasan (feedback, bloqueo, navegación, SAH, restauración)
- ✅ `PracticaFiltrosViewModelTest` - 5 tests pasan (subtemas por área, disponibles, iniciar)
- ✅ `AnalizadorProgresoTest` - 9 tests pasan (evidencia mínima, orden, evolución, historial)
- ✅ `InfoViewModelTest` - 7 tests pasan (parseo, filtro por área, estructura del asset)

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
- ✅ Tests unitarios: `./gradlew test` - **PASAN (66 tests)**

---

## ❌ PROBLEMAS PENDIENTES / NO IMPLEMENTADOS

### 0. Calidad del banco: 32 preguntas RL sin respuesta/enunciado (Alto)
- **Problema**: 32 preguntas RL del banco (mayoría `profe_alex_*`, varias del extinto comodín "Razonamiento logico") no tienen `respuesta_correcta` o enunciado → los selectores las excluyen (banco válido: 68 RL + 100 CL = 168; el simulacro requiere 80 y RL ≥ 40: OK hoy, margen justo).
- **Impacto**: no aparecen en simulacro ni práctica; fragmentan los pools por subtema; subutilizan las fuentes originales.
- **Solución pendiente**: completar la validación (responderlas) con el panel administrativo o curación manual. El plan (§6) ya lo registra como riesgo "Preguntas sin validar".

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
│   ├── local/                         # Room - COMPLETO
│   ├── mapper/                        # ✅
│   ├── repository/
│   │   ├── QuestionRepository.kt      # ✅ Banco local + updateBankFromFirestore()
│   │   ├── AuthRepository.kt          # ✅ Firebase Auth + colección users
│   │   ├── SimulationRepository.kt    # ✅ Guardado de simulacros en Firestore
│   │   └── PracticeRepository.kt      # ✅ Guardado de sesiones de práctica (batch)
│   └── source/local/                  # ✅ JsonL loader + Seeder (maneja JsonNull)
├── domain/
│   ├── model/Question.kt              # ✅ Modelo dominio
│   ├── model/User.kt                  # ✅ Modelo usuario (uid, nombre, correo, rol)
│   ├── services/QuestionSelector.kt   # ✅ Motor de selección 80 preguntas (testeado)
│   └── services/PracticeSelector.kt   # ✅ Estratificación proporcional de práctica (testeado)
├── ui/
│   ├── navigation/
│   │   ├── AppNavHost.kt              # ✅ NavHost principal (10 destinos)
│   │   └── AppNavGraph.kt             # ✅ Rutas
│   ├── screen/
│   │   ├── splash/                    # ✅ Splash + ViewModel (poll DB seed + gate auth)
│   │   ├── home/                      # ✅ Home + 5 tabs
│   │   ├── simulacro/                 # ✅ 80 preguntas, timer 120 min
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
| **P0** | PRs a `main`: `feat/practica` (en origin) → luego `feat/progreso` → `feat/info` (⚠️ deploy de `firestore.rules` incluido) | 30 min |
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

---

*Última actualización: 2026-10-02 - Rama `feat/info` (apilada sobre feat/progreso): módulo Info educativo completo — contenido de los 29 componentes del examen (15 RL + 14 CL: descripción, estrategia y errores comunes) empaquetado como asset offline-first; acordeón por componente con filtro de área. ComingSoonScreen eliminado: las 5 pestañas son funcionales (MVP del aspirante completo). Verificado: `test` (66 PASS) + `assembleDebug` BUILD SUCCESSFUL (JBR 21).*