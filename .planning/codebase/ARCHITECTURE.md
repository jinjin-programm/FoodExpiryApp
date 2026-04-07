# Architecture

**Analysis Date:** 2026-04-08

## Pattern Overview

**Overall:** Clean Architecture with MVVM (Model-View-ViewModel)

**Key Characteristics:**
- Separation of concerns across `data`, `domain`, and `presentation` layers.
- Uses Android Architecture Components (ViewModel, Room, ViewBinding).
- Dependency Injection via Hilt/Dagger.
- Reactive data flow using Kotlin Coroutines and Flows.
- Designed with offline-first support (Room database is the single source of truth for local data).

## Layers

**Presentation Layer:**
- Purpose: Handles UI rendering and user interactions. Contains Fragments, Activities, ViewModels, and Adapters.
- Location: `app/src/main/java/com/example/foodexpiryapp/presentation/`
- Contains: ViewModels, Fragments (UI), ViewBindings, Adapters (RecyclerView/ViewPager).
- Depends on: `domain` layer (UseCases and Models).
- Used by: Android Framework.

**Domain Layer:**
- Purpose: Core business logic and use cases. Pure Kotlin, no Android framework dependencies.
- Location: `app/src/main/java/com/example/foodexpiryapp/domain/`
- Contains: Models, UseCases, Repository Interfaces.
- Depends on: None.
- Used by: `presentation` layer (ViewModels).

**Data Layer:**
- Purpose: Data fetching, caching, and persistence.
- Location: `app/src/main/java/com/example/foodexpiryapp/data/`
- Contains: Room Database, DAOs, Retrofit API calls, Repository Implementations, Mappers.
- Depends on: `domain` layer (implements its interfaces).
- Used by: `domain` layer (through dependency injection).

## Data Flow

**Standard UI to Data Flow:**
1. UI triggers an action in the ViewModel (e.g., user adds a new food item).
2. ViewModel calls the corresponding UseCase in the `domain` layer.
3. UseCase delegates the data operation to the Repository.
4. Repository fetches or updates data via the local database (Room) or remote API (Retrofit).
5. The result flows back as a `Flow` or `suspend` function result to the ViewModel.
6. ViewModel updates LiveData/StateFlow, which the View observes to update the UI.

**State Management:**
- ViewModels expose state to the Views using Kotlin Flows (`StateFlow`/`SharedFlow`) or `LiveData`.
- UI observes the state and reacts to changes, enforcing unidirectional data flow.

## Key Abstractions

**Repository Pattern:**
- Purpose: Abstracts data sources (local vs remote) from the domain logic.
- Examples: `app/src/main/java/com/example/foodexpiryapp/domain/repository/FoodItemRepository.kt`
- Pattern: Interface defined in `domain`, implementation provided in `data` layer.

**UseCases:**
- Purpose: Encapsulates specific business rules or actions.
- Examples: `app/src/main/java/com/example/foodexpiryapp/domain/usecase/`
- Pattern: Single-responsibility classes.

## Entry Points

**Main Activity:**
- Location: `app/src/main/java/com/example/foodexpiryapp/presentation/MainActivity.kt` (assumed based on standard Android project structure and MainTabsFragment)
- Triggers: App launch.
- Responsibilities: Hosts the BottomNavigationView and NavHostFragment/ViewPager.

**Main Tabs Fragment:**
- Location: `app/src/main/java/com/example/foodexpiryapp/presentation/ui/MainTabsFragment.kt`
- Triggers: Navigation to the main dashboard.
- Responsibilities: Manages ViewPager2 for `inventory` and `shopping` tabs. Integrates with the bottom navigation.

## Error Handling

**Strategy:** Result wrappers and Kotlin exceptions.

**Patterns:**
- Use of Kotlin `Result` or custom sealed classes (e.g., `Resource.Success`, `Resource.Error`) to pass success/failure states from Repository to ViewModel.
- ViewModels catch exceptions from Coroutines and emit UI error states.

## Cross-Cutting Concerns

**Dependency Injection:** Hilt is used heavily (`@AndroidEntryPoint`, `@HiltViewModel`, `@Inject`).
**Threading:** Kotlin Coroutines (`viewModelScope` for UI, `Dispatchers.IO` for data layer).
**View Binding:** Replaces `findViewById` across Fragments/Activities (e.g., `FragmentMainTabsBinding`).

---

*Architecture analysis: 2026-04-08*