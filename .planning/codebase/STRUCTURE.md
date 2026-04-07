# Codebase Structure

**Analysis Date:** 2026-04-08

## Directory Layout

```
app/src/main/java/com/example/foodexpiryapp/
├── data/               # Implementations for Data, Network, Cache
│   ├── local/          # Room DB, DAOs, Entities
│   ├── remote/         # Retrofit API, DTOs
│   ├── repository/     # Repository implementations
│   └── mapper/         # Data model to Domain model converters
├── di/                 # Hilt Dagger Dependency Injection modules
├── domain/             # Core Business Logic
│   ├── model/          # Pure Kotlin data classes
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Single-responsibility action classes
└── presentation/       # UI Layer
    ├── adapter/        # RecyclerView and ViewPager adapters
    ├── ui/             # Fragments and Activities grouped by feature
    ├── viewmodel/      # Hilt ViewModels
    └── util/           # UI-related utility functions
```

## Directory Purposes

**Data (`data/`):**
- Purpose: Abstracting the source of data (network, local cache).
- Contains: Room databases (`local/database`), DAOs (`local/dao`), API definitions (`remote`), and mappers to convert between entities, DTOs, and Domain models.
- Key files: `data/local/database/AnalyticsEventEntity.kt`, `data/local/dao/FoodItemDao.kt`

**Domain (`domain/`):**
- Purpose: Represents the core business rules of the application. Should be pure Kotlin, isolated from Android SDK.
- Contains: Entities/Models, Repository Interfaces, Use Cases.
- Key files: `domain/repository/FoodItemRepository.kt`

**Presentation (`presentation/`):**
- Purpose: Displays information to the user and handles interactions.
- Contains: Activities, Fragments, ViewModels, ViewBindings, UI Components.
- Key files: `presentation/ui/MainTabsFragment.kt`, `presentation/ui/inventory/`, `presentation/ui/shopping/`

## Key File Locations

**Entry Points:**
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/MainTabsFragment.kt`: Main UI coordinator for tabs.
- `app/src/main/AndroidManifest.xml`: Application entry configuration.

**Configuration:**
- `app/build.gradle.kts` (or `build.gradle`): Android build configuration, plugin and dependency declarations.
- `app/src/main/java/com/example/foodexpiryapp/di/`: Hilt dependency injection configuration.

**Core Logic:**
- `app/src/main/java/com/example/foodexpiryapp/domain/usecase/`: Reusable business logic commands.

**Testing:**
- `app/src/test/java/`: Unit tests.
- `app/src/androidTest/java/`: Instrumentation and UI tests.

## Naming Conventions

**Files/Classes:**
- Fragments: `[Feature]Fragment` (e.g., `MainTabsFragment`)
- ViewModels: `[Feature]ViewModel`
- UseCases: `[Action][Entity]UseCase`
- DAOs: `[Entity]Dao` (e.g., `FoodItemDao`)
- Adapters: `[ItemType]Adapter` (e.g., `MainPagerAdapter`)

**Directories:**
- Packages: Lowercase standard Kotlin naming (e.g., `presentation.ui.inventory`)

## Where to Add New Code

**New Feature (e.g., UX optimization UI updates):**
- Primary UI logic: `presentation/ui/[feature_name]/[FeatureName]Fragment.kt`
- ViewModel: `presentation/viewmodel/[FeatureName]ViewModel.kt`
- Use Case (if new action): `domain/usecase/[Action][Entity]UseCase.kt`

**New Component/Module:**
- Data Models: `domain/model/`
- Data Persistance: `data/local/dao/` and `data/local/database/`

**Utilities:**
- Shared UI helpers: `presentation/util/`
- General logic helpers: `util/`

## Special Directories

**`di/`:**
- Purpose: Contains Hilt modules to provide dependencies across the entire application (e.g., `AppModule`, `DatabaseModule`, `NetworkModule`).
- Generated: No
- Committed: Yes

---

*Structure analysis: 2026-04-08*