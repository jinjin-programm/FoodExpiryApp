# Coding Conventions

**Analysis Date:** 2026-04-08

## Naming Patterns

**Files:**
- PascalCase for Kotlin classes and interfaces (e.g., `FoodAnalyticsService.kt`, `AnalyticsEventDao.kt`).
- snake_case for XML layouts and resources (if any).

**Functions:**
- camelCase for functions and methods.
- PascalCase for Jetpack Compose UI functions (e.g., `FoodItemScreen`, `Navigation`).

**Variables:**
- camelCase for local variables and properties.

**Types:**
- PascalCase for classes, interfaces, enums, data classes.

## Code Style

**Formatting:**
- Default Kotlin style (likely detekt/ktlint if configured, otherwise standard Android Studio formatting).

**Linting:**
- Android Lint is typical for this stack.

## Import Organization

**Order:**
1. Standard Java/Kotlin library imports
2. Android/AndroidX imports
3. Third-party library imports (e.g., Dagger/Hilt, Room, Retrofit)
4. Local project imports

## Error Handling

**Patterns:**
- Try-catch blocks for network and database operations.
- Kotlin Result/Flow error handling used in Repositories.

## Logging

**Framework:** `android.util.Log` or Timber (if integrated).

**Patterns:**
- Typically tag-based logging for errors or debug info during development.

## Comments

**When to Comment:**
- KDoc for public APIs and complex business logic.

**JSDoc/TSDoc:**
- Not applicable (Kotlin uses KDoc).

## Function Design

**Size:**
- Typical Jetpack Compose functions should be small and composable.
- ViewModels encapsulate business logic efficiently.

**Parameters:**
- Data classes are often passed to avoid too many parameters.

**Return Values:**
- Repositories return `Flow<T>` or `Result<T>` for asynchronous operations.

## Module Design

**Exports:** Not applicable (Kotlin package visibility).

**Barrel Files:** Not applicable.

---

*Convention analysis: 2026-04-08*
