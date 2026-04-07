# Testing Patterns

**Analysis Date:** 2026-04-08

## Test Framework

**Runner:**
- JUnit 4/5 (Standard for Android)
- Config: `app/build.gradle.kts` (test dependencies)

**Assertion Library:**
- Google Truth or standard JUnit assertions.

**Run Commands:**
```bash
./gradlew testDebugUnitTest       # Run all local unit tests
./gradlew connectedAndroidTest    # Run instrumented tests on device
```

## Test File Organization

**Location:**
- Unit tests: `app/src/test/java/com/example/foodexpiryapp/`
- Instrumented tests: `app/src/androidTest/java/com/example/foodexpiryapp/`

**Naming:**
- Matches the class being tested with a `Test` suffix (e.g., `FoodItemTest.kt`, `ExpiryNotificationWorkerTest.kt`).

**Structure:**
```
app/src/test/java/com/example/foodexpiryapp/domain/model/
```

## Test Structure

**Suite Organization:**
```kotlin
class FoodItemTest {
    @Test
    fun testFoodItemCreation() { ... }
}
```

**Patterns:**
- Typically arrange-act-assert (Given-When-Then).
- Mocking repositories in ViewModels tests.

## Mocking

**Framework:**
- Mockito or MockK (Common for Android Kotlin projects).

**Patterns:**
```kotlin
// Example mock usage
val mockRepository = mockk<FoodItemRepository>()
```

**What to Mock:**
- Network requests, local database interactions, Context, and external services.

**What NOT to Mock:**
- Pure domain models or simple utility functions.

## Fixtures and Factories

**Test Data:**
```kotlin
val dummyFoodItem = FoodItem(id = 1, name = "Apple", expiryDate = ...)
```

**Location:**
- Usually created within test classes or in test-specific utility objects.

## Coverage

**Requirements:** None enforced currently, likely relying on manual or PR-based checking.

**View Coverage:**
```bash
./gradlew jacocoTestReport # If Jacoco is configured
```

## Test Types

**Unit Tests:**
- Focus on Domain Models (e.g., `FoodItemTest.kt`, `NotificationSettingsTest.kt`) and Workers (`ExpiryNotificationWorkerTest.kt`).

**Integration Tests:**
- Room Database DAOs and Repositories (if instrumented tests are present).

**E2E Tests:**
- Not explicitly discovered (Compose UI testing).

## Common Patterns

**Async Testing:**
```kotlin
@Test
fun testCoroutine() = runTest {
    // Coroutine testing block
}
```

**Error Testing:**
```kotlin
// Testing failure paths or exceptions
assertThrows(Exception::class.java) {
    // Failing code
}
```

---

*Testing analysis: 2026-04-08*
