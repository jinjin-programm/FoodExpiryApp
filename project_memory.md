# project_memory.md — FoodExpiryApp Agent Memory

> **Purpose:** This file gives any AI coding agent full project context.
> Read this file first before making any changes. Never ask the user to re-explain what's documented here.

---

## Project Overview

**FoodExpiry App** (aka FreshAlert) — Android app for tracking food inventory, managing expiry dates, reducing food waste, and saving money.

- **Developer:** jinjin
- **Started:** February 2026
- **Status:** 🚧 Active Development (Alpha)
- **Package:** `com.example.foodexpiryapp`
- **Min SDK:** 26 | **Target SDK:** 34 | **Compile SDK:** 36

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin (JVM target 17) |
| UI | Android Views + Material Design 3 + ViewBinding |
| Navigation | Navigation Component + SafeArgs + BottomNavigationView |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt (`@HiltAndroidApp`) |
| Database | Room (Jetpack) |
| Preferences | Jetpack DataStore |
| Background | WorkManager (expiry notifications) |
| Barcode API | OpenFoodFacts (Retrofit + OkHttp) |
| ML — YOLO | TensorFlow Lite (`foodvision_yolov8.tflite`, 55 food classes) |
| ML — Vision | ML Kit (Google) |
| ML — LLM | Local Qwen3.5-0.8b via JNI bridge |
| Build | Gradle Kotlin DSL, kapt, parcelize |

---

## Architecture Pattern

```
UI (Fragment/Activity)
  → ViewModel (StateFlow)
    → UseCase
      → Repository Interface (domain/)
        → Repository Implementation (data/)
          → Data Source (Room / Retrofit / Assets / JNI)
```

**Strict rules:**
- Domain layer is pure Kotlin — zero Android imports
- Repository interfaces live in `domain/repository/`, implementations in `data/repository/`
- ViewModels expose `StateFlow`, never hold View references
- Use Hilt `@Inject` for all dependencies

---

## Project Structure (Key Paths)

```
app/src/main/java/com/example/foodexpiryapp/
├── MainActivity.kt                  # Entry point, bottom nav
├── FoodExpiryApp.kt                 # @HiltAndroidApp + WorkManager init
├── data/
│   ├── local/database/              # Room DB + Entity
│   ├── local/dao/                   # FoodItemDao, AnalyticsEventDao
│   ├── remote/                      # OpenFoodFacts API (Retrofit)
│   ├── repository/                  # Repo implementations
│   ├── mapper/                      # Entity ↔ Domain mappers
│   └── analytics/                   # FoodAnalyticsService
├── domain/
│   ├── model/                       # Pure Kotlin models
│   ├── repository/                  # Repo interfaces
│   └── usecase/                     # Business logic
├── presentation/
│   ├── ui/                          # Fragments (inventory, shopping, recipes, planner, profile, scan, yolo, llm, vision, chat)
│   ├── viewmodel/                   # ViewModels per screen
│   ├── adapter/                     # RecyclerView adapters
│   └── util/                        # UI helpers
├── di/                              # Hilt modules (Database, Network, Repository, DataStore, Analytics)
├── worker/                          # ExpiryNotificationWorker
└── receiver/                        # BootReceiver (reschedule after reboot)
```

**Assets:** `app/src/main/assets/` — model files, labels (`yolo_labels.txt`), recipes (`recipes.json`)
**ML models:** `app/src/main/ml/` — `foodvision_yolov8.tflite`
**JNI:** `app/src/main/jni/` — native LLM bridge

---

## Features Status

### ✅ Done
- Inventory screen (CRUD, search, filter, color-coded urgency badges, swipe-to-delete)
- Shopping screen (weekly stats, shopping list)
- Recipes screen (20 local recipes, inventory matching, waste reduction scoring)
- Profile screen (Google Sign-In, photo upload, dietary preferences, household size, notification settings, first-time setup dialog)
- Scan screens: Barcode (OpenFoodFacts), YOLO (TFLite), LLM (Qwen3.5 JNI), Vision (ML Kit)
- Chat screen (conversational LLM interface)
- Analytics tracking (SCREEN_VIEW, ITEM_ADDED, ITEM_EATEN, ITEM_EXPIRED, RECIPE_VIEWED, RECIPE_COOKED, NOTIFICATION_SENT)
- Expiry notifications (WorkManager + BootReceiver)
- Swipe navigation, Material Design 3 theming

### 🚧 In Progress
- UI polishing across screens
- Scan/chat/inventory flow stabilization
- Test coverage expansion

### ❌ Not Started / Placeholder
- **Planner screen** — currently just placeholder text "📅 Planner Screen"
- Database migrations (Room setup exists but may need migration strategy)
- Error handling edge cases

---

## YOLO Model Info

- **Model:** FoodVision YOLOv8 → `foodvision_yolov8.tflite` (~15 MB)
- **Classes:** 55 food types (fruits + vegetables)
- **Location:** `app/src/main/ml/foodvision_yolov8.tflite`
- **Labels:** `app/src/main/assets/yolo_labels.txt`
- **Input:** 640×640 RGB
- **Old model (deprecated):** `yolo11n_float32.tflite` (80 generic classes) — do NOT use
- **Setup script:** `python setup_foodvision_model.py`

---

## LLM / JNI Bridge

- **Model:** Qwen3.5-0.8b (local inference)
- **Bridge:** `LlamaBridge.kt` (JNI) → `app/src/main/jni/`
- **Service:** `LlmVisionService.kt`
- **Screen:** `LlmScanFragment.kt`, `ChatFragment.kt`
- **NDK:** arm64-v8a only

---

## API Keys (BuildConfig)

All keys are placeholder `"test_key"` — not yet configured for production:
- `RAPIDAPI_KEY`, `GOOGLE_VISION_API_KEY`, `OPENAI_API_KEY`, `API_NINJAS_KEY`, `FOODDATA_CENTRAL_KEY`

---

## Coding Conventions

- **Language:** Kotlin, follow Android Kotlin style guide
- **Naming:** PascalCase for classes, camelCase for functions/properties
- **Architecture:** Strict Clean Architecture — no shortcuts between layers
- **DI:** Use Hilt, never manual service locators
- **Async:** Kotlin Coroutines + Flow (StateFlow in ViewModels)
- **Layouts:** XML (not Compose) — ViewBinding enabled
- **Navigation:** SafeArgs for type-safe arguments
- **Tests:** JUnit for unit tests, AndroidX Test for instrumented tests

---

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Clean build
./gradlew clean assembleDebug

# Run tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

---

## Documentation Index

| File | Topic |
|------|-------|
| `PROJECT_DOCUMENTATION.md` | Full architecture & feature docs |
| `PROJECT_PROGRESS.md` | Progress snapshot & workflow |
| `README_FOODEXPIRYAPP.md` | Detailed changelog & feature list |
| `FOOD_MODEL_SETUP.md` | YOLO FoodVision model setup |
| `YOLO_SETUP.md` | YOLO11 (legacy) setup |
| `RECIPE_FEATURE_IMPLEMENTATION.md` | Recipes screen implementation details |
| `PROFILE_MODULE_IMPLEMENTATION.md` | Profile module implementation details |
| `Qwen3.5-0.8bwithMLkit.md` | LLM + ML Kit integration notes |
| `YOLO_FOOD_SCANNER_DOCS.md` | YOLO scanner documentation |
| `expirynotificationper-item.md` | Per-item expiry notification design |

---

## Important Notes for Agents

1. **Do NOT modify `build.gradle.kts`** without asking — dependency changes can break the build
2. **Planner screen is empty** — any work on it is building from scratch
3. **All ML models are local** — no cloud API calls for inference
4. **Recipes are hardcoded** in `assets/recipes.json` — no external API
5. **OpenFoodFacts is the only remote API** used for barcode lookup
6. **Room DB exists** but migration strategy may be incomplete
7. **This is a learning project** — code quality matters but simplicity is valued over over-engineering
