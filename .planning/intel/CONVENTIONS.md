# Coding Conventions

**Analysis Date:** 2026-04-21

## Naming Patterns

**Files:**
- PascalCase matching class name: `InventoryViewModel.kt`, `FoodItemMapper.kt`
- Grouped use cases in plural: `FoodUseCases.kt` contains multiple `*UseCase` classes
- Feature-based directory grouping under `presentation/ui/{feature}/`

**Functions:**
- ViewModel public methods: `on{Action}` pattern: `onAddFoodItem()`, `onSearchQueryChanged()`, `onCategorySelected()`
- UseCase invoke: `operator fun invoke()` for single-operation use cases
- DAO methods: Verb-based: `getAllFoodItems()`, `getFoodItemById()`, `insertFoodItem()`, `searchFoodItems()`
- Repository methods: Match DAO interface, delegate internally
- Private helpers: camelCase descriptive: `filterItemsForNotification()`, `createNotificationChannel()`

**Variables:**
- Private mutable state: `_{name}` prefix with backing property: `_uiState` / `uiState`
- Flows: `_events` (MutableSharedFlow) exposed as `events` (SharedFlow)
- Constants: `COMPANION_OBJECT` upper snake case in companion objects
- Config keys: private val `KEY_PROVIDER = stringPreferencesKey(...)`

**Types:**
- Domain models: Plain data classes with no framework annotations: `data class FoodItem(...)`
- Entities: `@Entity` annotated data classes with String dates (not LocalDate)
- Enums: PascalCase with displayName property: `enum class FoodCategory(val displayName: String)`
- UI State: Separate data class per screen: `data class InventoryUiState(...)`
- Events: Sealed class hierarchy: `sealed class InventoryEvent { data class ShowMessage(...) }`

## Code Style

**Formatting:**
- No Prettier or ktlint config detected
- Kotlin official code style (set in `gradle.properties`: `kotlin.code.style=official`)
- 4-space indentation (Kotlin default)

**Linting:**
- No custom lint rules detected
- Default Android lint via AGP

## Import Organization

**Order:**
1. Android framework imports (`android.*`, `androidx.*`)
2. Third-party imports (`com.google.*`, `dagger.*`, `kotlinx.*`)
3. Project imports (`com.example.foodexpiryapp.*`)

**Path Aliases:**
- None used (full package imports throughout)

## Error Handling

**Patterns:**
- ViewModels: try-catch wrapping with SharedFlow error emission
```kotlin
viewModelScope.launch {
    try {
        addFoodItem(foodItem)
        _events.emit(InventoryEvent.ShowMessage("${foodItem.name} added"))
    } catch (e: Exception) {
        _events.emit(InventoryEvent.ShowMessage("Error: ${e.message}"))
    }
}
```
- UseCases: `require()` for precondition validation
```kotlin
suspend operator fun invoke(foodItem: FoodItem): Long {
    require(foodItem.name.isNotBlank()) { "Food item name cannot be blank" }
    require(foodItem.quantity > 0) { "Quantity must be positive" }
    return repository.insertFoodItem(foodItem)
}
```
- Workers: catch-all with `Result.retry()` for recoverable failures
- MNN/ML engines: Try-catch with native resource cleanup in `finally`
- Repository implementations: No error handling (exceptions propagate to caller)
- Mapper enum parsing: `try { valueOf(str) } catch (e: IllegalArgumentException) { DEFAULT }`

## Logging

**Framework:** Android `android.util.Log` (no wrapper)

**Patterns:**
- TAG constants in companion objects: `companion object { private const val TAG = "DetectionPipeline" }`
- Verbose debug logging in API clients (OllamaApiClient logs full request/response)
- Some files use fully-qualified `android.util.Log.d()` instead of importing
- No structured logging, no log levels management

## Comments

**When to Comment:**
- KDoc on public classes and methods explaining purpose
- Per-reference comments linking to design decisions (e.g., "Per D-01:", "Per PITFALL-1:")
- Inline comments explaining WHY, not WHAT

**KDoc:**
- Used on domain models: `/** Domain model representing a food item in the user's pantry. */`
- Used on complex methods in inference engines
- Not consistently applied across all public APIs

## Function Design

**Size:** Variable - ViewModel methods range from 5 to 40 lines. UseCase `invoke()` methods are typically 3-10 lines.

**Parameters:** Named parameters in data class constructors. UseCases take domain models or primitives.

**Return Values:**
- Queries: `Flow<List<T>>` for reactive data, `suspend fun` for one-shot operations
- ViewModel: Updates `MutableStateFlow<UiState>`, emits events via `SharedFlow`
- UseCases: Return domain model or `Long` (insert ID)

## Module Design

**Exports:** Each class is self-contained. No barrel/index files.

**Barrel Files:** Not used. Classes are imported individually by full path.

## Dependency Injection

**Pattern:**
- Constructor injection with `@Inject constructor()`
- Hilt ViewModels: `@HiltViewModel` + `@Inject constructor`
- Hilt Workers: `@HiltWorker` + `@AssistedInject constructor`
- Hilt Fragments: `@AndroidEntryPoint` annotation
- Repository binding: `@Binds abstract fun bindX(impl: XImpl): XInterface`
- Singleton scoping: `@Singleton` on all repository implementations and engines
- Module organization: One Hilt module per concern (Database, Network, Repository, etc.)

## Coroutines

**Patterns:**
- `viewModelScope.launch` for ViewModel async work
- `withContext(Dispatchers.IO)` for blocking/suspend operations
- `Dispatchers.Default` for CPU-intensive inference
- `Flow` for reactive data streams
- `StateFlow` for UI state, `SharedFlow` for one-shot events
- `combine()`, `flatMapLatest()`, `debounce()` for reactive composition
- `channelFlow` for complex multi-step async operations (DetectionPipeline)
- `isActive` check for cancellation in long loops

## Date Handling

**Pattern:**
- `java.time.LocalDate` in domain layer (desugared for API < 26)
- String ISO format (`"2024-01-15"`) in Room entities
- Conversion in mappers: `LocalDate.parse(entity.date)` / `domain.date.toString()`

---

*Convention analysis: 2026-04-21*
