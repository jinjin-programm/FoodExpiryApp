# External Integrations

**Analysis Date:** 2026-04-21

## APIs & External Services

**Barcode Lookup:**
- Open Food Facts - Free open database for barcode-based product lookup
  - SDK/Client: Retrofit (`app/src/main/java/com/example.foodexpiryapp/data/remote/OpenFoodFactsApi.kt`)
  - Base URL: `https://world.openfoodfacts.org/`
  - Auth: None (free, public API)

**Recipe Search:**
- TheMealDB - Free meal/recipe database with ingredient-based search
  - SDK/Client: Retrofit (`app/src/main/java/com/example.foodexpiryapp/data/remote/TheMealDbApi.kt`)
  - Base URL: `https://www.themealdb.com/api/json/v1/1/`
  - Auth: API key "1" (free tier, hardcoded in build config)

**AI/LLM Food Classification (Local Server):**
- Ollama - Self-hosted LLM server for vision-based food identification
  - SDK/Client: Raw OkHttp (`app/src/main/java/com/example.foodexpiryapp/data/remote/ollama/OllamaApiClient.kt`)
  - Endpoints: `/api/chat`, `/api/version`, `/api/tags`
  - Auth: Optional bearer token via DataStore config
  - Config: `OllamaServerConfig` reads server URL + token from DataStore
  - Vision client: `app/src/main/java/com/example.foodexpiryapp/data/remote/ollama/OllamaVisionClient.kt`

- LM Studio - Alternative self-hosted LLM server (OpenAI-compatible API)
  - SDK/Client: Raw OkHttp (`app/src/main/java/com/example.foodexpiryapp/data/remote/lmstudio/LmStudioApiClient.kt`, `LmStudioVisionClient.kt`)
  - Auth: Optional bearer token via DataStore config
  - Config: `LmStudioServerConfig` reads server URL + token from DataStore

**Provider Selection:**
- `ProviderConfig` (`app/src/main/java/com/example.foodexpiryapp/data/remote/ProviderConfig.kt`) - Stores selected provider ("ollama" or "lmstudio") in DataStore

**Model Downloads:**
- HuggingFace - Downloads YOLO/LLM model files
  - Client: `app/src/main/java/com/example.foodexpiryapp/data/remote/HuggingFaceDownloadService.kt`
  - Token: `HF_TOKEN` from BuildConfig (sourced from `local.properties`)
  - Token provider: `app/src/main/java/com/example.foodexpiryapp/data/remote/HfTokenProvider.kt`
  - Download manager: `app/src/main/java/com/example.foodexpiryapp/data/remote/ModelDownloadManager.kt`

## Data Storage

**Databases:**
- Room (SQLite) - Local-only database
  - Database name: `food_expiry_db`
  - Version: 15 (with 14 manual migrations)
  - Connection: Room-provided singleton via Hilt (`DatabaseModule`)
  - 10 tables: food_items, analytics_events, meal_plans, shopping_items, cooked_recipes, local_recipes, shopping_templates, download_state, detection_results, shelf_life_entries

**File Storage:**
- Internal app storage for food images (`FoodImageStorage` at `app/src/main/java/com/example.foodexpiryapp/util/FoodImageStorage.kt`)
- Detection crop images in `filesDir/detection_crops/`
- MNN model files managed by `ModelStorageManager` at `app/src/main/java/com/example.foodexpiryapp/data/local/ModelStorageManager.kt`
- Debug images for MNN inference at `externalFilesDir(null)/mnn_debug_images/`

**Caching:**
- None explicit (no LRU cache, no in-memory caching layer)

## Authentication & Identity

**Auth Provider:**
- Firebase Auth (configured but not deeply integrated in current codebase)
  - Implementation: Firebase Auth KTX + Google Sign-In via Play Services Auth
  - Dependencies declared in `app/build.gradle.kts` (lines 202-205)

## Monitoring & Observability

**Error Tracking:**
- None (no Crashlytics, Sentry, or similar)

**Logs:**
- Android `Log.d`/`Log.e`/`Log.w` throughout the codebase
- Verbose debug logging in `OllamaApiClient` (request/response/timing details)

**Analytics:**
- Custom local analytics via Room database (`analytics_events` table)
- Service: `FoodAnalyticsService` at `app/src/main/java/com/example.foodexpiryapp/data/analytics/FoodAnalyticsService.kt`
- Repository: `AnalyticsRepositoryImpl` at `app/src/main/java/com/example.foodexpiryapp/data/repository/AnalyticsRepositoryImpl.kt`
- Events tracked: app_opened, item_added, item_deleted, item_eaten, item_expired, search_query, notification_sent

## CI/CD & Deployment

**Hosting:**
- Mobile app only (no backend)

**CI Pipeline:**
- None detected (no `.github/workflows/`, no `.gitlab-ci.yml`, no `Jenkinsfile`)

**Build Variants:**
- Debug: debuggable, no signing config
- Release: optional signing via local.properties keys, ProGuard configured but minification disabled

## Environment Configuration

**Required env vars / local.properties keys:**
- `HF_TOKEN` - HuggingFace model download authentication
- `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` - Release signing (optional)
- `MNN_SOURCE_ROOT` - MNN native library path (optional, for CMake)

**Secrets location:**
- `local.properties` - HF_TOKEN and release signing config (git-ignored)
- Build config fields contain placeholder API keys ("test_key") that are not functional

**Runtime configuration (DataStore):**
- Ollama server URL and auth token
- LM Studio server URL and auth token
- Inference provider selection (ollama/lmstudio)
- UI style preference
- Notification settings

## Webhooks & Callbacks

**Incoming:**
- None

**Outgoing:**
- None (all API calls are request-response, no webhook registration)

## Native Libraries

**MNN (Alibaba):**
- JNI bridges: `app/src/main/cpp/mnn_llm_bridge.cpp`, `app/src/main/cpp/mnn_yolo_bridge.cpp`
- Java wrappers: `app/src/main/java/com/taobao/android/mnn/` (MNNNetInstance, MNNImageProcess, etc.)
- Kotlin native interfaces: `MnnLlmNative` (`app/src/main/java/com/example.foodexpiryapp/inference/mnn/MnnLlmNative.kt`), `MnnYoloNative` (`app/src/main/java/com/example.foodexpiryapp/inference/yolo/MnnYoloNative.kt`)
- Prebuilt `.so` files expected in `app/src/main/jniLibs/arm64-v8a/`

---

*Integration audit: 2026-04-21*
