# Codebase Concerns

**Analysis Date:** 2026-04-21

## Tech Debt

**Navigation Architecture Hybrid (ViewPager2 + NavController):**
- Issue: `MainActivity` simultaneously manages a `ViewPager2` for 5 main tabs AND a `NavController` for detail screens. The two navigation systems fight for visibility toggling.
- Files: `app/src/main/java/com/example/foodexpiryapp/MainActivity.kt` (lines 61-124)
- Impact: Fragile navigation state. Users can get stuck or see wrong UI when navigating between tabs and detail screens. Heavy `Log.d("NAV_DEBUG", ...)` suggests this was recently debugged.
- Fix approach: Consolidate to single navigation system. Either use Navigation Component with `BottomNavigationView` properly, or manage all navigation through the ViewPager2 with child fragment transactions.

**15 Room Database Migrations:**
- Issue: Database at version 15 with 14 manual migration objects, each containing raw SQL. Some migrations in `DatabaseModule` (MIGRATION_1_2), others in `AppDatabase` companion object. Inconsistent location.
- Files: `app/src/main/java/com/example/foodexpiryapp/data/local/database/AppDatabase.kt` (lines 37-224), `app/src/main/java/com/example/foodexpiryapp/di/DatabaseModule.kt` (lines 27-32)
- Impact: High maintenance burden. Each new column/table requires manual SQL. `exportSchema = false` means no schema history for testing migrations.
- Fix approach: Enable schema export. Use Room's auto-migration feature. Consolidate all migrations into AppDatabase.

**Placeholder API Keys in Build Config:**
- Issue: 5 API keys hardcoded as "test_key" in build config fields. These are not functional but are compiled into the APK.
- Files: `app/build.gradle.kts` (lines 78-82)
- Impact: Dead code. If any code path uses these keys, it will fail silently. Misleading for developers who expect them to work.
- Fix approach: Remove unused API key build config fields. Move needed keys to `local.properties` or a secrets management plugin.

**Multiple Redundant Adapters:**
- Issue: 4 different food item adapters exist: `FoodListAdapter`, `FoodItemAdapter`, `FoodItemCuteAdapter`, `FoodCardAdapter`, plus `ExpiringCuteAdapter`. Suggests iterative development without cleanup.
- Files: `app/src/main/java/com/example/foodexpiryapp/presentation/adapter/`
- Impact: Code duplication, maintenance overhead. Unclear which adapter is canonical.
- Fix approach: Consolidate to 1-2 adapters using sealed class or delegate pattern for different view types.

**Empty `ui/` Directory:**
- Issue: `app/src/main/java/com/example/foodexpiryapp/ui/` exists but is empty (or contains only one file). All UI code is in `presentation/ui/`.
- Files: `app/src/main/java/com/example/foodexpiryapp/ui/`
- Impact: Confusion about where UI code lives.
- Fix approach: Remove empty `ui/` directory.

## Known Bugs

**No confirmed open bugs from code inspection, but several risk areas identified below.**

## Security Considerations

**API Keys in Build Config:**
- Risk: "test_key" values compiled into APK are extractable via reverse engineering
- Files: `app/build.gradle.kts` (lines 78-84)
- Current mitigation: Values are placeholders, not real keys
- Recommendations: Remove unused keys entirely. Use `local.properties` or a secrets gradle plugin for any real keys. Never commit real API keys.

**HF_TOKEN Build Config:**
- Risk: HuggingFace token flows through `BuildConfig.HF_TOKEN` and is accessible in code. If a real token is set, it's in the APK.
- Files: `app/build.gradle.kts` (line 84), `app/src/main/java/com/example/foodexpiryapp/di/NetworkModule.kt` (lines 29-33)
- Current mitigation: Defaults to empty string. Read from `local.properties` (git-ignored).
- Recommendations: Acceptable for now since the token is for downloading public models. Consider using a backend proxy for production.

**Network Security:**
- Risk: Self-hosted LLM servers (Ollama/LM Studio) communicate over plain HTTP by default
- Files: `app/src/main/res/xml/network_security_config.xml`, `app/src/main/java/com/example/foodexpiryapp/data/remote/ollama/OllamaServerConfig.kt`
- Current mitigation: Network security config exists. Ollama/LM Studio typically run on localhost.
- Recommendations: Ensure network security config allows cleartext only for localhost. Warn users about remote server security.

**No Certificate Pinning:**
- Risk: API calls to Open Food Facts and TheMealDB have no certificate pinning
- Files: `app/src/main/java/com/example/foodexpiryapp/di/NetworkModule.kt`
- Current mitigation: HTTPS enforced by default
- Recommendations: Low risk for public free APIs. Not a priority.

## Performance Bottlenecks

**ML Model Loading on Main Thread Risk:**
- Problem: While YOLO/LLM loading uses `withContext(Dispatchers.IO)`, the detection pipeline's `detect()` method is NOT a suspend function - it runs synchronously
- Files: `app/src/main/java/com/example/foodexpiryapp/inference/yolo/MnnYoloEngine.kt` (line 113 - `fun detect(bitmap: Bitmap)` is not suspend)
- Cause: The `detect()` method performs bitmap encoding, native JNI calls, and NMS postprocessing on whatever thread calls it
- Improvement path: Make `detect()` a suspend function with `withContext(Dispatchers.Default)` for CPU-intensive work. The `DetectionPipeline` already wraps it in `withContext(Dispatchers.Default)`, but the API is error-prone.

**Database Migration Chain:**
- Problem: 15 sequential migrations must all succeed for a user updating from v1. Any failure loses data.
- Files: `app/src/main/java/com/example/foodexpiryapp/data/local/database/AppDatabase.kt`
- Cause: Incremental ALTER TABLE + CREATE TABLE over many versions
- Improvement path: Add `fallbackToDestructiveMigration()` for development. Test migration paths. Consider a "fresh start" migration for major versions.

**Verbose Debug Logging:**
- Problem: Heavy `Log.d()` calls throughout, especially in `OllamaApiClient` (logs full request/response bodies, timing, token stats)
- Files: `app/src/main/java/com/example/foodexpiryapp/data/remote/ollama/OllamaApiClient.kt` (lines 42-78)
- Cause: Development-time debugging left in
- Improvement path: Use Timber or a wrapper with configurable log levels. Strip debug logs in release builds.

## Fragile Areas

**MainActivity Navigation Switching:**
- Files: `app/src/main/java/com/example/foodexpiryapp/MainActivity.kt` (lines 89-124)
- Why fragile: Visibility toggling between ViewPager2 and NavHostFragment based on destination ID matching. Any new destination must be added to the `isMainTab` list or it breaks navigation.
- Safe modification: When adding new detail fragments, ensure they are NOT in the `isMainTab` list. When adding new main tabs, add to both the ViewPager2 adapter AND the `isMainTab` check.
- Test coverage: None

**Room Entity ↔ Domain Mapper Error Swallowing:**
- Files: `app/src/main/java/com/example/foodexpiryapp/data/mapper/FoodItemMapper.kt` (lines 20-24)
- Why fragile: `valueOf()` enum parsing wrapped in try-catch with silent fallback. Corrupted data silently becomes "OTHER"/"FRIDGE"/"MANUAL"/"LOW".
- Safe modification: If adding new enum values, ensure backward compatibility with existing database strings.
- Test coverage: No mapper tests

**Detection Pipeline State Machine:**
- Files: `app/src/main/java/com/example/foodexpiryapp/inference/pipeline/DetectionPipeline.kt`
- Why fragile: Multi-step async pipeline with `channelFlow` that can fail at any stage. Error handling is per-item but overall pipeline state could be inconsistent.
- Safe modification: Each `PipelineState` variant must be handled by all consumers. Adding new states requires updating all `when` expressions.
- Test coverage: None (no pipeline integration tests)

## Scaling Limits

**Room Database:**
- Current capacity: No indexes on most columns. `searchFoodItems` uses LIKE query.
- Limit: Will slow down significantly with 10,000+ food items (no FTS, no pagination).
- Scaling path: Add Room FTS4 for search. Implement paging with Paging 3 library. Add indexes on frequently queried columns (category, location, expiryDate).

**Image Storage:**
- Current capacity: Individual JPEG files in internal storage
- Limit: No cleanup policy beyond manual deletion. Device storage could fill up.
- Scaling path: Implement image size limits, auto-compression, and periodic cleanup of orphaned images.

**ML Model Memory:**
- Current capacity: Mutual exclusion between YOLO and LLM models via `ModelLifecycleManager`
- Limit: Only one model loaded at a time. Loading/unloading adds latency.
- Scaling path: Accept the trade-off for mobile devices. Consider model quantization for smaller memory footprint.

## Dependencies at Risk

**MNN (Alibaba Mobile Neural Network):**
- Risk: Niche framework, primarily Chinese-language documentation, limited community support. Vendor code (`com.taobao.android.mnn`) bundled directly.
- Impact: If MNN becomes incompatible with newer Android versions, the core AI detection feature breaks.
- Migration plan: The codebase already supports TFLite and ONNX as alternatives. `OnnxYoloEngine` is the production default. MNN could be deprecated in favor of ONNX entirely.

**ONNX Runtime 1.20.0:**
- Risk: Relatively new version, Android support may have edge cases
- Impact: Core YOLO detection depends on this
- Migration plan: TFLite engine exists as fallback

**Speed Dial FAB 3.3.0:**
- Risk: Third-party library not commonly used
- Impact: Only affects the FAB button, not core functionality
- Migration plan: Replace with Material Design FAB or custom implementation

## Missing Critical Features

**No Crash Reporting:**
- Problem: No Firebase Crashlytics, Sentry, or similar crash reporting
- Blocks: Production monitoring and user issue diagnosis

**No CI/CD Pipeline:**
- Problem: No automated builds, tests, or deployment
- Blocks: Quality assurance at scale, automated release process

**No User Authentication Flow:**
- Problem: Firebase Auth dependencies exist but no visible login/register UI
- Blocks: Multi-device support, cloud backup, personalized features

**No Data Export/Backup:**
- Problem: All data stored locally with no export or cloud backup mechanism
- Blocks: Users losing all data on device loss/reset

## Test Coverage Gaps

**ViewModels - Untested (HIGH priority):**
- What's not tested: All 10 ViewModels (Inventory, Recipes, Profile, Shopping, Planner, YoloScan, Confirmation, RecipeDetail, Chat, ShelfLifeManagement)
- Files: `app/src/main/java/com/example/foodexpiryapp/presentation/viewmodel/`
- Risk: Business logic errors in state management, event handling, and data transformation go undetected
- Priority: High

**Repository Implementations - Untested (HIGH priority):**
- What's not tested: All 12+ repository implementations
- Files: `app/src/main/java/com/example/foodexpiryapp/data/repository/`
- Risk: Data mapping errors, query failures, and API integration bugs go undetected
- Priority: High

**Fragments - Untested (MEDIUM priority):**
- What's not tested: All 20+ Fragments
- Files: `app/src/main/java/com/example/foodexpiryapp/presentation/ui/`
- Risk: UI logic errors, navigation issues, lifecycle bugs
- Priority: Medium

**DAOs - Untested (MEDIUM priority):**
- What's not tested: All Room DAOs (despite `room-testing` dependency being declared)
- Files: `app/src/main/java/com/example/foodexpiryapp/data/local/dao/`
- Risk: SQL query errors, migration failures, data integrity issues
- Priority: Medium

**Detection Pipeline - Untested (HIGH priority):**
- What's not tested: `DetectionPipeline.detectAndClassify()` end-to-end flow
- Files: `app/src/main/java/com/example/foodexpiryapp/inference/pipeline/DetectionPipeline.kt`
- Risk: Pipeline state transitions, error recovery, cancellation handling
- Priority: High

**Mappers - Untested (LOW priority):**
- What's not tested: `FoodItemMapper`, `MealPlanMapper`, `AnalyticsMapper`
- Files: `app/src/main/java/com/example/foodexpiryapp/data/mapper/`
- Risk: Enum fallback masking data corruption
- Priority: Low (mappers are simple but the error swallowing pattern is concerning)

---

*Concerns audit: 2026-04-21*
