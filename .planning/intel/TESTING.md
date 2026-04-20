# Testing Patterns

**Analysis Date:** 2026-04-21

## Test Framework

**Runner:**
- JUnit 4.13.2
- Robolectric 4.12.2 for Android-context unit tests
- AndroidX Test Core KTX 1.5.0
- Config: `app/build.gradle.kts` (testOptions: `isIncludeAndroidResources = true`)

**Assertion Library:**
- JUnit assertions (`assertEquals`, `assertTrue`, etc.)
- No Truth or AssertJ library

**Mocking:**
- Mockito Core 5.8.0
- Mockito Kotlin 5.2.1

**Run Commands:**
```bash
./gradlew test                           # Run all unit tests
./gradlew testDebugUnitTest              # Run debug unit tests
./gradlew connectedAndroidTest           # Run instrumented tests
./gradlew test --tests "com.example.foodexpiryapp.inference.mnn.StructuredOutputParserTest"  # Single test class
```

## Test File Organization

**Location:**
- Co-located mirror: `app/src/test/java/com/example/foodexpiryapp/` mirrors main source structure
- Instrumented tests: `app/src/androidTest/java/com/example/foodexpiryapp/`

**Naming:**
- Pattern: `{ClassName}Test.kt` (e.g., `FoodItemTest.kt`, `MnnYoloPostprocessorTest.kt`)

**Structure:**
```
app/src/test/java/com/example/foodexpiryapp/
├── ExampleUnitTest.kt                      # Default template test
├── domain/model/
│   ├── FoodItemTest.kt                     # FoodItem domain model tests
│   └── NotificationSettingsTest.kt         # NotificationSettings tests
├── inference/
│   ├── mnn/
│   │   ├── StructuredOutputParserTest.kt   # LLM output parsing tests
│   │   └── DebugImageSaverTest.kt          # Debug image saver tests
│   └── yolo/
│       ├── MnnYoloPostprocessorTest.kt     # YOLO postprocessor tests
│       └── YoloEPostprocessorTest.kt       # YOLO-E postprocessor tests
└── worker/
    └── ExpiryNotificationWorkerTest.kt     # Notification worker tests

app/src/androidTest/java/com/example/foodexpiryapp/
├── ExampleInstrumentedTest.kt              # Default template test
└── inference/yolo/
    └── MnnYoloPostprocessorTest.kt         # Android-specific YOLO tests
```

## Test Structure

**Suite Organization:**
```kotlin
// Domain model test pattern (from FoodItemTest.kt)
class FoodItemTest {
    @Test
    fun `isExpired returns true when expiryDate is before today`() {
        val item = FoodItem(
            name = "Milk",
            category = FoodCategory.DAIRY,
            expiryDate = LocalDate.now().minusDays(1),
            // ...
        )
        assertTrue(item.isExpired)
    }
}
```

**Patterns:**
- Backtick test names with descriptive phrases (Kotlin convention)
- Arrange-Act-Assert structure
- Tests target domain models and inference utilities (pure logic)
- No test fixtures or shared setup classes

## Mocking

**Framework:** Mockito + Mockito Kotlin

**Patterns:**
```kotlin
// Standard mocking pattern (from Worker tests)
@Mock
private lateinit var mockFoodRepository: FoodRepository

// Mockito Kotlin extension
val repository = mock<FoodRepository> {
    on { getAllFoodItems() } doReturn flowOf(emptyList())
}
```

**What to Mock:**
- Repository interfaces for ViewModel/UseCase tests
- DAO interfaces for Repository tests

**What NOT to Mock:**
- Domain models (construct directly)
- Mappers (test directly)
- Pure utility classes (test directly)

## Fixtures and Factories

**Test Data:**
- Domain models constructed inline in test methods
- No shared fixture files or factory functions

**Location:**
- No dedicated test fixtures directory

## Coverage

**Requirements:** None enforced (no JaCoCo or coverage plugin)

**Coverage Gaps (see CONCERNS.md for details):**
- Zero test files for: All ViewModels, All Fragments, All Repository implementations, All Adapters
- Tests exist only for: Domain models (FoodItem, NotificationSettings), Inference utilities (postprocessors, parsers), Worker (ExpiryNotification)

**View Coverage:**
```bash
# No coverage report available - no JaCoCo configured
```

## Test Types

**Unit Tests:**
- Scope: Domain models, utility classes, postprocessors, parsers
- Approach: Direct instantiation, no Android framework dependency
- Count: 8 test files

**Integration Tests:**
- None present (no Room database integration tests despite `room-testing` dependency)

**E2E Tests:**
- Not used (no Espresso tests beyond default template)

## Common Patterns

**Async Testing:**
```kotlin
// Coroutines test support via kotlinx-coroutines-test
@Test
fun testAsync() = runTest {
    val result = useCase.invoke()
    // assert on result
}
```

**Error Testing:**
```kotlin
@Test
fun `require throws on blank name`() {
    val item = FoodItem(name = "", ...)
    assertThrows<IllegalArgumentException> {
        useCase.invoke(item)
    }
}
```

---

*Testing analysis: 2026-04-21*
