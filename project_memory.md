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
| Recipe API (planned) | TheMealDB (free dev key `1`, supporter key for public release) |
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

**Assets:** `app/src/main/assets/` — model files, labels (`yolo_labels.txt`), recipes (`recipes.json`), LLM models (`llm/model.gguf`, `llm/mmproj.gguf`)
**ML models:** `app/src/main/ml/` — `foodvision_yolov8.tflite`, `yolo11n_float32.tflite`, `crawled_grocery_yolo11n.tflite`
**JNI:** `app/src/main/jni/` — native LLM bridge (`libllama_jni.so`)

---

## Features Status

### ✅ Done
- Inventory screen (CRUD, search, filter, color-coded urgency badges, swipe-to-delete, mark as eaten)
- Shopping screen (weekly stats — items added, eaten, expired, notifications sent)
- Recipes screen (20 local recipes, inventory matching, filters: Best Match, Use Soon, Waste Buster, Quick, Vegetarian, Vegan, money saved tracking)
- Profile screen (Google Sign-In, photo upload, dietary preferences, household size, notification settings, first-time setup dialog)
  - ProfileSettingsFragment (detailed settings with validation, notification time)
  - ProfileAccountFragment (Google Sign-In management)
- Scan screens:
  - Barcode scanning (ML Kit + OpenFoodFacts API)
  - OCR date scanning (ML Kit text recognition)
  - YOLO object detection (TFLite — YOLO26n/YOLO11n, multiple models available)
  - LLM vision scan (Qwen3.5-0.8b via JNI, text + vision with mmproj)
  - Vision scan (ML Kit)
- Chat screen (conversational LLM interface with Qwen3.5)
- Analytics tracking (SCREEN_VIEW, ITEM_ADDED, ITEM_EATEN, ITEM_DELETED, ITEM_EXPIRED, RECIPE_VIEWED, RECIPE_COOKED, SCAN_SUCCESS, NOTIFICATION_SENT)
- Expiry notifications (WorkManager + BootReceiver)
- Swipe navigation, Material Design 3 theming, ViewPager for main tabs
- Planner screen (meal slots: Breakfast/Lunch/Dinner/Snack, product/recipe picker dialogs, MealPlanDao/Entity/Repository, PlannerViewModel)
- ShelfLifeEstimator (smart expiry date estimation based on food type)
- LlmVisionService (color-based food detection fallback using HSV histogram)

### 🚧 In Progress
- UI polishing across screens
- Scan/chat/inventory flow stabilization
- Test coverage expansion
- Planning TheMealDB integration for richer recipe search/suggestions

### ❌ Not Started / Placeholder
- **ProfileHelpFragment** — empty stub (34 lines, no content)
- Database migrations (Room setup exists but may need migration strategy)
- Error handling edge cases

---

## YOLO Model Info

Multiple YOLO models available:

| Model | Location | Classes | Purpose |
|-------|----------|---------|---------|
| `foodvision_yolov8.tflite` | `app/src/main/ml/` | 55 | Food-specific detection (fruits + vegetables) |
| `food_yolo26n_float32.tflite` | `app/src/main/assets/` | 34 | Custom food detection |
| `crawled_grocery_yolo11n.tflite` | `app/src/main/assets/` + `ml/` | 51 | Grocery product detection |
| `yolo11n_float32.tflite` | `app/src/main/ml/` + `assets/` | 80 | Generic COCO classes |
| `yolo26n_float32.tflite` | `app/src/main/assets/` | 80 | Generic COCO classes |

**Labels:** `app/src/main/assets/yolo_labels.txt`, `food_categories.txt`, `yolo_labels_crawled_grocery.txt`, `coco_labels.txt`
**Input:** 640×640 RGB

---

## LLM / JNI Bridge

- **Model:** Qwen3.5-0.8b (local inference)
- **Files:** `app/src/main/assets/llm/model.gguf` (text), `mmproj.gguf` (vision projection)
- **Bridge:** `LlamaBridge.kt` (JNI) → `app/src/main/jni/` → native C++ (`libllama_jni.so`)
- **Service:** `LlmVisionService.kt` (also has color-based food detection fallback using HSV)
- **Screens:** `LlmScanFragment.kt`, `VisionScanFragment.kt`, `ChatFragment.kt`
- **NDK:** arm64-v8a only
- **Supports:** Text-only inference + Vision (with mmproj)

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
2. **Planner screen is implemented** — includes MealPlanDao, Entity, Repository, ViewModel, and UI
3. **All ML models are local** — no cloud API calls for inference
4. **Recipes are hardcoded** in `assets/recipes.json` today — no external API yet
5. **OpenFoodFacts is the only remote API currently used** for barcode lookup
6. **TheMealDB is planned** as an optional recipe source with local caching
7. **Room DB exists** but migration strategy may be incomplete
8. **Feedback screen is implemented** — email intent to `123qwerty100@gmail.com`
9. **Duplicate code** — there's an `app/app/` subdirectory with older/duplicate versions of some files (can be cleaned up)
10. **This is a learning project** — code quality matters but simplicity is valued over over-engineering
