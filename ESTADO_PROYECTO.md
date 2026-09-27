# Estado del Proyecto RutaUdeA - MVP Simulacro

## Resumen General
El proyecto implementa un MVP para una app de preparación de examen de admisión UdeA con arquitectura MVVM + Jetpack Compose + Room + KSP + Firebase.

**Estado actual: ✅ MVP COMPLETO Y FUNCIONAL + Firebase + gate de autenticación** - Todas las pantallas principales operativas, login/registro obligatorio al iniciar (rama `feat/auth-gate`), motor de selección de 80 preguntas. Build SUCCESS, tests PASS (27).

## 🌿 Ramas
| Rama | Estado | Contenido |
|------|--------|-----------|
| `main` | ✅ Actualizada (PR #2) | MVP + Firebase completo + motor de 80 preguntas (el merge del motor incluyó la integración Firebase al ser rama apilada) |
| `feat/firebase-config` | ✅ En origin | Firebase Auth + Firestore + sincronización (ya en main vía PR #2) |
| `feat/motor-seleccion-preguntas` | ✅ En origin | Motor de selección 80 preguntas (ya en main vía PR #2) |
| `feat/auth-gate` | 🔨 Rama actual de trabajo | Gate de autenticación: splash decide Home/login, logout vuelve al login |

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
| ID | Área | Subtema | Tipo |
|----|------|---------|------|
| profe_alex_4 | RL | Series | Suma/Resta |
| profe_alex_7 | RL | Porcentajes | Suma/Resta |
| profe_alex_23 | RL | Posibilidades lógicas | Lógica simple |
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
| **ComingSoonScreen** | ✅ Placeholder | Para Práctica, Progreso, Info |

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
| 📝 Práctica | `practica` | 🟡 Placeholder |
| 📈 Progreso | `progreso` | 🟡 Placeholder |
| 📚 Info | `info` | 🟡 Placeholder |
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
- **`google-services.json`** presente en `app/` (reglas completas en `firestore.rules`: users, questions, simulations, simulationQuestions, contexts, components, subtopics, educationalContent, settings; roles aspirante/admin).
- **`QuestionRepository.updateBankFromFirestore()`**: descarga el banco maestro (`questions` con `estado=aprobado`) y reemplaza Room.
- **`SimulationRepository.saveSimulation()`**: guarda simulacro completado (resumen + detalle por pregunta); requiere sesión (si no hay, se omite el guardado).
- **`SimulationRepository.getQuestionIdsFromRecentSimulacros(3)`**: usado por el motor para excluir preguntas de los últimos 3 simulacros.

### 6. Tema Material3
- **Primary**: Forest Green (#1B4D3E)
- **Secondary**: Blue Accent (#2196F3)
- Bordes redondeados 16dp, elevación 2-4dp
- Logo vectorial personalizado (`ic_splash_logo.xml`)

### 7. Tests Unitarios
- ✅ `QuestionMapperTest` - 7 tests pasan
- ✅ `QuestionJsonlLoaderTest` - 5 tests pasan
- ✅ `QuestionSelectorTest` - 11 tests pasan (motor de selección)
- ✅ `SplashViewModelTest` - 4 tests pasan (gate de autenticación)

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
- ✅ Tests unitarios: `./gradlew test` - **PASAN (27 tests)**

---

## ❌ PROBLEMAS PENDIENTES / NO IMPLEMENTADOS

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
│   │   └── SimulationRepository.kt    # ✅ Guardado de simulacros en Firestore
│   └── source/local/                  # ✅ JsonL loader + Seeder (maneja JsonNull)
├── domain/
│   ├── model/Question.kt              # ✅ Modelo dominio
│   ├── model/User.kt                  # ✅ Modelo usuario (uid, nombre, correo, rol)
│   └── services/QuestionSelector.kt   # ✅ Motor de selección 80 preguntas (testeado)
├── ui/
│   ├── navigation/
│   │   ├── AppNavHost.kt              # ✅ NavHost principal (8 destinos)
│   │   └── AppNavGraph.kt             # ✅ Rutas
│   ├── screen/
│   │   ├── splash/                    # ✅ Splash + ViewModel (poll DB seed)
│   │   ├── home/                      # ✅ Home + 5 tabs
│   │   ├── simulacro/                 # ✅ 80 preguntas, timer 120 min
│   │   ├── resultado/                 # ✅ Resultado + ViewModel (lee SavedStateHandle previo)
│   │   ├── perfil/                    # ✅ Login/Registro/Perfil (Firebase Auth)
│   │   └── common/ComingSoonScreen.kt # ✅ Placeholder (Práctica, Progreso, Info)
│   └── theme/                         # ✅ Material3 Forest Green
```

---

## 🚀 PRÓXIMOS PASOS PRIORITARIOS

| Prioridad | Tarea | Esfuerzo |
|-----------|-------|----------|
| **P0** | PR de `feat/auth-gate` a `main` cuando el gate esté validado en dispositivo | 15 min |
| ~~**P0**~~ | ~~Gate de login al inicio~~ | ✅ HECHO (feat/auth-gate) |
| ~~**P0**~~ | ~~Eliminar código basura sin uso~~ | ✅ HECHO |
| **P1** | Migrar a Hilt para DI | 2 horas |
| **P1** | Implementar Práctica filtrada real | 4 horas |
| ~~**P2**~~ | ~~Motor selección 80 preguntas (40 RL + 40 CL)~~ | ✅ HECHO (en main) |
| **P2** | Pantalla Práctica con feedback inmediato | 4 horas |
| **P2** | Pantalla Progreso con gráficas (datos de `simulations` Firestore) | 6 horas |
| **P2** | Contenido educativo Info por subtema | 4 horas |
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

*Última actualización: 2026-09-27 - Rama `feat/auth-gate` (desde main con PR #2: motor + firebase): gate de autenticación completo (splash decide Home/login, logout vuelve al login, login/registro navega a Home como raíz). Verificado: `test` (27 PASS) + `assembleDebug` BUILD SUCCESSFUL (JBR 21). Docs actualizados a estado real.*