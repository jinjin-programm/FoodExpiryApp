# Codebase Structure

**Analysis Date:** 2026-04-21

## Directory Layout

```
FoodExpiryApp/
├── app/                              # Main application module
│   ├── build.gradle.kts              # App-level build configuration (dependencies, SDK, signing)
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml   # App declaration, permissions, launcher activity
│   │   │   ├── java/
│   │   │   │   ├── FoodExpiryApp.kt  # Application class (Hilt, WorkManager, seeding)
│   │   │   │   └── com/example/foodexpiryapp/
│   │   │   │       ├── MainActivity.kt           # Launcher activity (navigation host)
│   │   │   │       ├── data/                     # Data layer
│   │   │   │       │   ├── analytics/            # Analytics service
│   │   │   │       │   ├── local/                # Local data (Room DB, DAOs, entities)
│   │   │   │       │   │   ├── dao/              # Room DAO interfaces
│   │   │   │       │   │   └── database/         # Room entities + AppDatabase
│   │   │   │       │   ├── mapper/               # Entity↔Domain mappers
│   │   │   │       │   ├── remote/               # Remote data (APIs, services)
│   │   │   │       │   │   ├── dto/              # API response DTOs
│   │   │   │       │   │   ├── lmstudio/         # LM Studio LLM client
│   │   │   │       │   │   └── ollama/           # Ollama LLM client + DTOs
│   │   │   │       │   └── repository/           # Repository implementations
│   │   │   │       ├── di/                       # Hilt dependency injection modules
│   │   │   │       ├── domain/                   # Domain layer (pure Kotlin)
│   │   │   │       │   ├── client/               # Domain client interfaces
│   │   │   │       │   ├── engine/               # Domain engines (attribute inference)
│   │   │   │       │   ├── model/                # Domain models
│   │   │   │       │   ├── repository/           # Repository interfaces
│   │   │   │       │   ├── usecase/              # Use case classes
│   │   │   │       │   └── vision/               # Vision classifier interface
│   │   │   │       ├── inference/                # ML inference layer
│   │   │   │       │   ├── mnn/                  # MNN engine (LLM + lifecycle)
│   │   │   │       │   ├── pipeline/             # Detection pipeline orchestrator
│   │   │   │       │   ├── tflite/               # TFLite/ONNX YOLO engines
│   │   │   │       │   └── yolo/                 # MNN YOLO engine + postprocessor
│   │   │   │       ├── presentation/             # Presentation layer
│   │   │   │       │   ├── adapter/              # RecyclerView adapters
│   │   │   │       │   ├── ui/                   # UI Fragments organized by feature
│   │   │   │       │   │   ├── chat/             # AI chat feature
│   │   │   │       │   │   ├── detection/        # Detection confirmation screen
│   │   │   │       │   │   ├── inventory/        # Food inventory (main screen)
│   │   │   │       │   │   ├── planner/          # Meal planner
│   │   │   │       │   │   ├── profile/          # Profile/settings tabs
│   │   │   │       │   │   ├── recipes/          # Recipe browser + detail
│   │   │   │       │   │   ├── scan/             # Scan container + barcode/photo
│   │   │   │       │   │   ├── scanner/          # Scanner activity
│   │   │   │       │   │   ├── shelflife/        # Shelf life management
│   │   │   │       │   │   ├── shopping/         # Shopping list
│   │   │   │       │   │   ├── vision/           # Vision scan (Ollama settings)
│   │   │   │       │   │   └── yolo/             # YOLO scan + detection overlay
│   │   │   │       │   ├── util/                 # Presentation utilities
│   │   │   │       │   └── viewmodel/            # ViewModels
│   │   │   │       ├── receiver/                 # Broadcast receivers (boot)
│   │   │   │       ├── ui/                       # (Legacy/empty - presentation/ui used instead)
│   │   │   │       ├── util/                     # Shared utilities
│   │   │   │       └── worker/                   # WorkManager workers
│   │   │   ├── cpp/                              # Native C++ code
│   │   │   │   ├── CMakeLists.txt                # CMake build configuration
│   │   │   │   ├── mnn_llm_bridge.cpp            # JNI bridge for MNN LLM
│   │   │   │   └── mnn_yolo_bridge.cpp           # JNI bridge for MNN YOLO
│   │   │   ├── jniLibs/                          # Prebuilt native libraries
│   │   │   └── res/                              # Android resources
│   │   │       ├── layout/                       # 45+ XML layout files
│   │   │       ├── navigation/nav_graph.xml      # Navigation graph
│   │   │       ├── xml/                          # Config files (network security, backup rules)
│   │   │       └── ...                           # (drawable, values, mipmap, etc.)
│   │   ├── test/                                 # Unit tests
│   │   │   └── java/com/example/foodexpiryapp/
│   │   │       ├── inference/                    # Inference-related tests
│   │   │       ├── worker/                       # Worker tests
│   │   │       ├── domain/model/                 # Domain model tests
│   │   │       └── ExampleUnitTest.kt            # Default template test
│   │   └── androidTest/                          # Instrumented tests
│   │       └── java/com/example/foodexpiryapp/
│   │           └── inference/yolo/               # Android-specific inference tests
│   │           └── ExampleInstrumentedTest.kt    # Default template test
│   └── proguard-rules.pro                        # ProGuard configuration
├── build.gradle.kts                              # Root build file (plugin versions)
├── settings.gradle.kts                           # Module includes + repositories
├── gradle.properties                             # JVM args, AndroidX config
├── docs/                                         # Project documentation
├── tools/                                        # Development tools
├── archive/                                      # Archived code
├── runs/                                         # Experiment/run artifacts
├── .planning/                                    # Planning documents (this file)
├── .gsd/                                         # GSD tooling config
├── PROJECT_DOCUMENTATION.md                      # High-level project docs
├── PROJECT_PROGRESS.md                           # Development progress tracking
├── PROFILE_MODULE_IMPLEMENTATION.md              # Profile feature docs
├── RECIPE_FEATURE_IMPLEMENTATION.md              # Recipe feature docs
├── expirynotificationper-item.md                 # Per-item notification spec
├── project_memory.md                             # Project memory/context
└── README.md                                     # Project readme
```

## Directory Purposes

**`data/`** - Data layer (implements domain interfaces)
- `data/local/dao/` - 10 Room DAO interfaces (FoodItemDao, MealPlanDao, ShoppingItemDao, etc.)
- `data/local/database/` - 10 Room Entity classes + `AppDatabase` with 14 migrations
- `data/local/` - `HkRecipeSeeder` (HK recipe seed data), `ModelStorageManager`
- `data/mapper/` - `FoodItemMapper`, `MealPlanMapper`, `AnalyticsMapper`
- `data/remote/` - API interfaces (`OpenFoodFactsApi`, `TheMealDbApi`), LLM clients, model download
- `data/repository/` - 12+ repository implementations (Food, Recipe, Shopping, MealPlan, Analytics, etc.)

**`domain/`** - Pure business logic (no Android dependencies)
- `domain/model/` - 18+ domain data classes and enums (FoodItem, Recipe, ShoppingItem, MealPlan, DetectionResult, etc.)
- `domain/repository/` - 11+ repository interfaces (Food, Recipe, Shopping, MealPlan, Analytics, YoloDetection, etc.)
- `domain/usecase/` - 6 UseCase files containing 20+ individual use case classes
- `domain/engine/` - `DefaultAttributeEngine` (keyword-based food attribute inference), `SeedData`
- `domain/client/` - `FoodVisionClient` interface (LLM abstraction)
- `domain/vision/` - `FoodClassifier` interface

**`inference/`** - ML inference engines
- `inference/mnn/` - MNN LLM engine, lifecycle manager, config, native bridge, output parser
- `inference/yolo/` - MNN YOLO engine, config, native bridge, postprocessor
- `inference/tflite/` - TFLite engine, ONNX engine, `YoloDetector` interface
- `inference/pipeline/` - `DetectionPipeline` (orchestrates YOLO → crop → LLM classify)

**`presentation/`** - UI layer
- `presentation/ui/` - 12 feature packages with Fragments + custom views
- `presentation/viewmodel/` - 10 ViewModels (Inventory, Recipes, Profile, Shopping, Planner, YoloScan, Confirmation, RecipeDetail, Chat)
- `presentation/adapter/` - 14 RecyclerView adapters
- `presentation/util/` - `ValidationHelper`, `PhotoStorageHelper`, `FirstTimeSetupHelper`

**`di/`** - 7 Hilt modules (Database, Repository, Network, DataStore, Analytics, Detection, Inference)

## Key File Locations

**Entry Points:**
- `app/src/main/java/FoodExpiryApp.kt`: Application class
- `app/src/main/java/com/example/foodexpiryapp/MainActivity.kt`: Launcher activity
- `app/src/main/AndroidManifest.xml`: Manifest configuration

**Configuration:**
- `app/build.gradle.kts`: All dependencies, SDK versions, build config
- `build.gradle.kts`: Plugin version declarations
- `settings.gradle.kts`: Repository sources, module includes
- `app/src/main/res/navigation/nav_graph.xml`: Navigation destinations and actions

**Core Domain Models:**
- `domain/model/FoodItem.kt`: Central domain model with enums
- `domain/model/Recipe.kt`: Recipe model with ingredient matching
- `domain/model/DetectionResult.kt`: YOLO detection result
- `domain/model/MealPlan.kt`: Meal planning model
- `domain/model/ShoppingItem.kt`: Shopping list model

**Database:**
- `data/local/database/AppDatabase.kt`: Room database (v15, 14 migrations)
- `data/local/database/FoodItemEntity.kt`: Core entity
- `data/local/dao/FoodItemDao.kt`: Core DAO

**DI Modules:**
- `di/DatabaseModule.kt`: Database + DAO providers
- `di/RepositoryModule.kt`: 10 repository bindings
- `di/NetworkModule.kt`: Retrofit API providers
- `di/InferenceModule.kt`: ML engine providers
- `di/DetectionModule.kt`: Detection pipeline providers

**Testing:**
- `app/src/test/java/`: 8 unit test files
- `app/src/androidTest/java/`: 2 instrumented test files

## Naming Conventions

**Files:**
- Fragments: `{Feature}Fragment.kt` (e.g., `InventoryFragment.kt`, `ShoppingFragment.kt`)
- ViewModels: `{Feature}ViewModel.kt` (e.g., `InventoryViewModel.kt`)
- Adapters: `{Type}Adapter.kt` (e.g., `FoodItemAdapter.kt`, `RecipeAdapter.kt`)
- Use cases: `{Verb}{Noun}UseCase.kt` or grouped in `{Feature}UseCases.kt`
- Repositories (interface): `{Feature}Repository.kt` in `domain/repository/`
- Repositories (impl): `{Feature}RepositoryImpl.kt` in `data/repository/`
- Entities: `{Feature}Entity.kt` in `data/local/database/`
- DAOs: `{Feature}Dao.kt` in `data/local/dao/`
- DI modules: `{Feature}Module.kt` in `di/`
- Layouts: `fragment_{feature}.xml`, `item_{type}.xml`, `dialog_{name}.xml`

**Directories:**
- Feature directories under `presentation/ui/{feature}/`
- Package: `com.example.foodexpiryapp`

## Where to Add New Code

**New Feature (e.g., "Pantry Zones"):**
- Domain model: `domain/model/PantryZone.kt`
- Repository interface: `domain/repository/PantryZoneRepository.kt`
- Repository impl: `data/repository/PantryZoneRepositoryImpl.kt`
- Room entity: `data/local/database/PantryZoneEntity.kt`
- Room DAO: `data/local/dao/PantryZoneDao.kt`
- Mapper: `data/mapper/PantryZoneMapper.kt`
- Use cases: `domain/usecase/PantryZoneUseCases.kt`
- ViewModel: `presentation/viewmodel/PantryZoneViewModel.kt`
- Fragment: `presentation/ui/pantry/PantryZoneFragment.kt`
- Layout: `res/layout/fragment_pantry_zone.xml`
- DI binding: Add to `di/RepositoryModule.kt`, add DAO to `di/DatabaseModule.kt`
- Navigation: Add destination to `res/navigation/nav_graph.xml`
- DB migration: Add to `AppDatabase` companion object + `DatabaseModule`

**New Component/Module:**
- Adapter: `presentation/adapter/{Name}Adapter.kt`
- Layout: `res/layout/item_{name}.xml`

**Utilities:**
- Shared helpers: `util/{HelperName}.kt`
- Presentation helpers: `presentation/util/{HelperName}.kt`

## Special Directories

**`app/src/main/jniLibs/`**:
- Purpose: Prebuilt `.so` native libraries for arm64-v8a
- Generated: No (manually placed from MNN AAR)
- Committed: Yes

**`app/src/main/cpp/`**:
- Purpose: C++ JNI bridge source code
- Generated: No
- Committed: Yes

**`data/local/HkRecipeSeeder.kt`**:
- Purpose: Seeds HK-style recipes on first launch
- Contains: Hardcoded recipe data for initial app population

**`inference/mnn/`**:
- Purpose: On-device LLM inference via MNN framework
- Critical: Model lifecycle mutual exclusion with YOLO engine

---

*Structure analysis: 2026-04-21*
