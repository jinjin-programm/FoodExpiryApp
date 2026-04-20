# Architecture

**Analysis Date:** 2026-04-21

## Pattern Overview

**Overall:** Clean Architecture with MVVM presentation layer

**Key Characteristics:**
- Three-layer separation: Presentation → Domain → Data
- Unidirectional data flow from Repository through UseCases to ViewModel
- Reactive state management with StateFlow/SharedFlow
- Dependency inversion via domain interface + data implementation
- Manual dependency injection via Hilt (compile-time DI)

## Layers

**Presentation Layer:**
- Purpose: UI rendering, user interaction handling, view state management
- Location: `app/src/main/java/com/example/foodexpiryapp/presentation/`
- Contains: Fragments, ViewModels, Adapters, UI utilities
- Depends on: Domain layer (UseCases, Repository interfaces, Models)
- Used by: Android framework (Activity, Fragment lifecycle)
- Pattern: MVVM with ViewState (single `UiState` data class per screen)

**Domain Layer:**
- Purpose: Business logic, domain models, repository interfaces, use cases
- Location: `app/src/main/java/com/example/foodexpiryapp/domain/`
- Contains: Models, Repository interfaces, UseCase classes, Domain engines
- Depends on: Nothing (pure Kotlin, no Android dependencies except Bitmap in DetectionResult)
- Used by: Presentation layer via constructor injection

**Data Layer:**
- Purpose: Data persistence, API communication, data mapping
- Location: `app/src/main/java/com/example/foodexpiryapp/data/`
- Contains: Room entities/DAOs, Retrofit services, Repository implementations, Mappers
- Depends on: Domain layer (implements domain interfaces), Room, Retrofit, DataStore
- Used by: Domain layer (via interface binding in Hilt modules)

**Inference Layer (cross-cutting):**
- Purpose: On-device ML model lifecycle and execution
- Location: `app/src/main/java/com/example/foodexpiryapp/inference/`
- Contains: MNN YOLO engine, MNN LLM engine, TFLite/ONNX engines, Detection pipeline
- Depends on: Native C++ via JNI, Domain models
- Used by: Data layer (repository implementations), Presentation layer (ViewModel)

## Data Flow

**Food Item CRUD Flow:**

1. User interacts with Fragment (e.g., `InventoryFragment`)
2. Fragment calls ViewModel method (e.g., `InventoryViewModel.onAddFoodItem()`)
3. ViewModel invokes UseCase (e.g., `AddFoodItemUseCase`)
4. UseCase validates input, calls Repository interface (e.g., `FoodRepository.insertFoodItem()`)
5. RepositoryImpl delegates to DAO (e.g., `FoodItemDao.insertFoodItem()`)
6. Mapper converts between domain model (`FoodItem`) and entity (`FoodItemEntity`)
7. Room persists to SQLite
8. Reactive Flow propagates back through `getAllFoodItems()` → ViewModel StateFlow → Fragment UI

**AI Food Detection Pipeline Flow:**

1. User opens scan screen → `ScanContainerFragment` with ViewPager2 tabs
2. Camera frame captured by CameraX
3. `YoloDetector` (backed by `OnnxYoloEngine`) runs object detection
4. `DetectionPipeline.detectAndClassify()` emits `PipelineState` updates:
   - `PipelineState.Detecting` → YOLO running
   - `PipelineState.Detected` → crops saved, bounding boxes available
   - `PipelineState.Classifying` → LLM classifying each crop
   - `PipelineState.Complete` → `BatchDetectionResult` ready
5. Each detection crop sent to Ollama or LM Studio vision API
6. Results flow to `ConfirmationFragment` via `YoloScanViewModel`
7. User confirms → `SaveDetectedFoodsUseCase` persists to database

**State Management:**
- Reactive via Kotlin `StateFlow` for UI state
- `SharedFlow` for one-shot events (show message, item deleted)
- `MutableStateFlow` for filter/search state with `combine` + `debounce` + `flatMapLatest`
- Room `Flow` queries for automatic UI updates

## Key Abstractions

**Repository Pattern:**
- Purpose: Decouple domain from data sources
- Examples: `FoodRepository` (domain) → `FoodRepositoryImpl` (data)
- Pattern: Interface in domain layer, `@Binds` in Hilt to concrete implementation
- All repositories follow this pattern (Food, Recipe, Shopping, MealPlan, Analytics, etc.)

**UseCase Pattern:**
- Purpose: Encapsulate single business operations with validation
- Examples: `app/src/main/java/com/example/foodexpiryapp/domain/usecase/`
  - `FoodUseCases.kt` - GetAllFoodItems, GetExpiringFoodItems, AddFoodItem, UpdateFoodItem, DeleteFoodItem, SearchFoodItems
  - `RecipeUseCases.kt` - Recipe search and matching
  - `ShoppingUseCases.kt` - Shopping list CRUD + templates
  - `MealPlanUseCases.kt` - Meal plan CRUD
  - `SaveDetectedFoodsUseCase.kt` - Batch save from detection pipeline
  - `IdentifyFoodUseCase.kt` - Food identification orchestration
  - `LookupShelfLifeUseCase.kt` - Shelf life lookup
- Pattern: Each UseCase is a `class` with `@Inject constructor`, exposes `operator fun invoke()`

**Mapper Pattern:**
- Purpose: Convert between Entity (data) and Domain models
- Examples: `FoodItemMapper` at `app/src/main/java/com/example/foodexpiryapp/data/mapper/FoodItemMapper.kt`, `MealPlanMapper`, `AnalyticsMapper`
- Pattern: Kotlin `object` with static `entityToDomain()` / `domainToEntity()` methods

**Vision Client Abstraction:**
- Purpose: Abstract LLM food classification provider
- Interface: `FoodVisionClient` at `app/src/main/java/com/example/foodexpiryapp/domain/client/FoodVisionClient.kt`
- Implementations: `OllamaVisionClient`, `LmStudioVisionClient`
- Runtime selection via `ProviderConfig`

**Detection Engine Abstraction:**
- Purpose: Abstract YOLO detection backend
- Interface: `YoloDetector` at `app/src/main/java/com/example/foodexpiryapp/inference/tflite/YoloDetector.kt`
- Implementations: `TFLiteYoloEngine`, `OnnxYoloEngine` (production default per `InferenceModule`)

**Model Lifecycle Management:**
- Purpose: Prevent simultaneous loading of YOLO and LLM models (memory constraint)
- Implementation: `ModelLifecycleManager` at `app/src/main/java/com/example/foodexpiryapp/inference/mnn/ModelLifecycleManager.kt`
- Pattern: Acquire/release with mutual exclusion between `ModelType.YOLO` and `ModelType.LLM`

## Entry Points

**Application Class:**
- Location: `app/src/main/java/FoodExpiryApp.kt` (note: package is `com.example.foodexpiryapp`)
- Triggers: Android framework on app startup
- Responsibilities:
  - Configures Hilt (`@HiltAndroidApp`)
  - Configures WorkManager with `HiltWorkerFactory`
  - Schedules daily expiry notifications
  - Seeds local recipe database (`HkRecipeSeeder`)
  - Cleans up incomplete model downloads

**Launcher Activity:**
- Location: `app/src/main/java/com/example/foodexpiryapp/MainActivity.kt`
- Triggers: Launcher intent (MAIN/LAUNCHER)
- Responsibilities:
  - Sets up Navigation Component + ViewPager2 hybrid navigation
  - Requests notification permission (Android 13+)
  - Tracks app_opened analytics event
  - Syncs BottomNavigationView with ViewPager2 page changes
  - Toggles visibility between ViewPager2 (main tabs) and NavHostFragment (detail screens)

**Boot Receiver:**
- Location: `app/src/main/java/com/example/foodexpiryapp/receiver/BootReceiver.kt`
- Triggers: `BOOT_COMPLETED` system broadcast
- Responsibilities: Re-schedule daily notification work after device reboot

## Error Handling

**Strategy: Try-catch with user-facing messages**

**Patterns:**
- ViewModels wrap operations in try-catch, emit error messages via `SharedFlow`
- UseCases use `require()` for input validation (throws `IllegalArgumentException`)
- Workers catch exceptions and return `Result.retry()` for transient failures
- Repository implementations do NOT catch exceptions (propagate to ViewModel)
- Detection pipeline catches per-item failures, marks items as `DetectionStatus.FAILED`, continues processing remaining items
- MNN engine operations guarded with lifecycle checks and try-catch with native resource cleanup in `finally` blocks

## Cross-Cutting Concerns

**Logging:** Android `android.util.Log` throughout (no structured logging framework). Many raw `android.util.Log.d()` calls instead of importing the class.

**Validation:** `ValidationHelper` at `app/src/main/java/com/example/foodexpiryapp/presentation/util/ValidationHelper.kt`. UseCases use `require()` for preconditions.

**Authentication:** Firebase Auth configured in dependencies but no visible login flow in current fragments.

**Analytics:** Custom local analytics system tracking events to Room database with 30-day retention cleanup. `AnalyticsRepository` interface in domain, `AnalyticsRepositoryImpl` in data.

**Notifications:** WorkManager-based daily notifications via `ExpiryNotificationWorker`. Settings stored via `NotificationSettingsRepository` backed by DataStore.

**Image Storage:** `FoodImageStorage` at `app/src/main/java/com/example/foodexpiryapp/util/FoodImageStorage.kt` manages food item photos in internal storage.

---

*Architecture analysis: 2026-04-21*
