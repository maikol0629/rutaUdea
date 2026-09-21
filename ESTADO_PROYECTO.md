# Estado del Proyecto RutaUdeA - MVP Simulacro

## Resumen General
El proyecto implementa un MVP para una app de preparación de examen de admisión UdeA con arquitectura MVVM + Jetpack Compose + Room + KSP.

**Estado actual: ✅ MVP COMPLETO Y FUNCIONAL** - Todas las pantallas principales operativas. Build SUCCESS, tests PASS.

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
| **SimulacroScreen** | ✅ Funciona | `SimulacroScreen.kt` + `SimulacroViewModel` (6 preguntas, timer 5min) |
| **ResultadoScreen** | ✅ Funciona | `ResultadoScreen.kt` + `ResultadoViewModel` (score, %, tiempo, detalle ✓/✗ + 💡) |
| **ComingSoonScreen** | ✅ Placeholder | Para Práctica, Progreso, Info, Perfil |

### 4. Funcionalidades Core del Simulacro
| Función | Estado | Detalle |
|---------|--------|---------|
| Timer 5 min cuenta regresiva | ✅ | `timeRemainingSeconds` en ViewModel |
| Persistencia timer (rotación/background) | ✅ | `SavedStateHandle` sobrevive recreación |
| Overlay "Continuar/Abandonar" | ✅ | Modal centrado al volver de background |
| Selección opciones A/B/C/D | ✅ | Visual feedback ✓, color primario |
| Navegación siguiente/anterior/final | ✅ | Botón contextual "Siguiente →" / "Finalizar" |
| Auto-finalizar a 0:00 | ✅ | Navega a Resultado automáticamente |
| Pantalla Resultado | ✅ | Score X/6, %, tiempo, lista expandible ✓/✗ + 💡 explicación |

### 5. Navegación (5 tabs Bottom Bar)
| Tab | Ruta | Estado |
|-----|------|--------|
| 🧪 Simulacro | `simulacro` | ✅ Funcional |
| 📝 Práctica | `practica` | 🟡 Placeholder |
| 📈 Progreso | `progreso` | 🟡 Placeholder |
| 📚 Info | `info` | 🟡 Placeholder |
| 👤 Perfil | `perfil` | 🟡 Placeholder |

### 6. Tema Material3
- **Primary**: Forest Green (#1B4D3E)
- **Secondary**: Blue Accent (#2196F3)
- Bordes redondeados 16dp, elevación 2-4dp
- Logo vectorial personalizado (`ic_splash_logo.xml`)

### 7. Tests Unitarios
- ✅ `QuestionMapperTest` - 8 tests pasan
- ✅ `QuestionJsonlLoaderTest` - 5 tests pasan

### 8. Build & Deploy
- ✅ `./gradlew compileDebugKotlin` - **EXITOSO**
- ✅ `./gradlew assembleDebug` - **APK generado**
- ✅ `./gradlew installDebug` - **Instala en dispositivo físico**
- ✅ Tests unitarios: `./gradlew test` - **PASAN (13 tests)**

---

## ❌ PROBLEMAS PENDIENTES / NO IMPLEMENTADOS

### 1. Iconos de la App (Crítico)
- **Problema**: Adaptive icons configurados (`ic_launcher_foreground.xml` + `launcher_background #1B4D3E`) pero se ve cuadrado verde en launcher
- **Causa probable**: `ic_launcher_foreground` sin alpha channel correcto o vector no renderiza en algunos launchers; falta generar PNG reales (hdpi/mdpi/xhdpi/xxhdpi/xxxhdpi) en `app/src/main/res/mipmap-*/ic_launcher.png` y `ic_launcher_round.png`
- **Impacto**: App se instala pero icono no visible correctamente

### 2. Limpieza de código (Menor)
- `ViewModelFactory.kt` duplicado sin usar → **Eliminar**
- `AppModule.kt` con funciones no usadas → **Eliminar o adaptar**
- Warning `Modifier.weight(1f)` API interna → Usar `Modifier.weight(1f, fill = true)`

### 3. Dagger/Hilt NO configurado
- **Estado**: Inyección manual via `ViewModelFactories` object
- **Futuro**: Migrar a Hilt para producción

### 4. Firebase NO configurado
- **Estado**: Solo placeholders en tabs Info/Perfil/Progreso/Práctica
- **Pendiente**: Auth, Firestore, Analytics, Crashlytics

### 5. Tests de Instrumentación
- **Estado**: Solo tests unitarios (JUnit + MockK)
- **Falta**: Tests de UI (Compose Test), tests de integración Room

---

## 📁 Estructura de Archivos Clave

```
app/src/main/java/com/udea/rutaudea/
├── MainActivity.kt                    # Entry point - usa AppNavHost()
├── RutaUdeaApp.kt                     # Application class - expone repository
├── di/
│   ├── AppModule.kt                   # ⚠️ Sin uso real - LIMPIAR
│   ├── ViewModelFactory.kt            # ⚠️ Sin uso - ELIMINAR
│   └── ViewModelFactories.kt          # ✅ USADO - Factory functions
├── data/
│   ├── local/                         # Room - COMPLETO
│   ├── mapper/                        # ✅
│   ├── repository/                    # ✅
│   └── source/
│       ├── local/                     # ✅ JsonL loader + Seeder (maneja JsonNull)
│       └── mvp/                       # ✅ MvpQuestionProvider
├── domain/model/Question.kt           # ✅ Modelo dominio
├── ui/
│   ├── navigation/
│   │   ├── AppNavHost.kt              # ✅ NavHost principal (imports limpios)
│   │   ├── AppNavGraph.kt             # ✅ Rutas
│   │   └── BottomBar.kt               # ✅ 5 tabs
│   ├── screen/
│   │   ├── splash/                    # ✅ Splash + ViewModel (poll DB seed)
│   │   ├── home/                      # ✅ Home + 5 tabs
│   │   ├── simulacro/                 # ✅ Simulacro completo (6 preguntas, timer)
│   │   ├── resultado/                 # ✅ Resultado + ViewModel (lee SavedStateHandle previo)
│   │   └── common/ComingSoonScreen.kt # ✅ Placeholder
│   └── theme/                         # ✅ Material3 Forest Green
└── data/source/mvp/MvpQuestionProvider.kt  # ✅ Filtra 6 preguntas MVP
```

---

## 🚀 PRÓXIMOS PASOS PRIORITARIOS

| Prioridad | Tarea | Esfuerzo |
|-----------|-------|----------|
| **P0** | Generar iconos PNG reales (mipmap-*) / fix adaptive icon | 30 min |
| **P0** | Eliminar ViewModelFactory.kt y AppModule sin uso | 5 min |
| **P1** | Migrar a Hilt para DI | 2 horas |
| **P1** | Configurar Firebase (Auth + Firestore) | 4 horas |
| **P1** | Implementar Práctica filtrada real | 4 horas |
| **P2** | Motor selección 80 preguntas (40 RL + 40 CL) | 8 horas |
| **P2** | Pantalla Práctica con feedback inmediato | 4 horas |
| **P2** | Pantalla Progreso con gráficas | 6 horas |
| **P2** | Contenido educativo Info por subtema | 4 horas |
| **P2** | Tests UI Compose + Instrumentación | 4 horas |

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
1. Splash → "Cargando banco..." → Home en ~1.5-3s (espera DB seed)
2. Home → 5 tabs abajo, botón verde "Iniciar Simulacro"
3. Simulacro → 6 preguntas, timer 05:00, 4 opciones, progreso 1/6
4. Timer → cuenta 05:00 → 00:00 (rojo < 1 min)
5. Responder → ✓ visual → "Siguiente →" / "Finalizar"
6. Background → Home → volver → Overlay "Continuar/Abandonar" → Continua timer
7. Final → Resultado → Score X/6, %, tiempo, lista expandible ✓/✗ + 💡
8. Botón "Finalizar" → Home con BottomBar activo

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

---

*Última actualización: 2024-09-21 - MVP COMPLETO, build SUCCESS, tests PASS, iconos pendientes*