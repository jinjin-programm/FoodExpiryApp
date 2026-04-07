# External Integrations

**Analysis Date:** 2026-04-08

## APIs & External Services

**Machine Learning & Computer Vision:**
- Google Vision API - Cloud-based vision processing (fallback for ML Kit)
  - Auth: `GOOGLE_VISION_API_KEY` (in `local.properties`)
- ML Kit (On-Device) - Barcode scanning, OCR, Image labeling
- TensorFlow Lite - Custom object detection models (YOLO)
- OpenAI API - LLM integration
  - Auth: `OPENAI_API_KEY` (in `local.properties`)

**Food & Recipe Data:**
- RapidAPI - Likely food/recipe gateway
  - Auth: `RAPIDAPI_KEY` (in `local.properties`)
- API Ninjas - General purpose API data
  - Auth: `API_NINJAS_KEY` (in `local.properties`)
- FoodData Central (USDA) - Nutritional/food database
  - Auth: `FOODDATA_CENTRAL_KEY` (in `local.properties`)
- TheMealDB - Recipe API
  - Auth: `THEMEALDB_API_KEY` (in `local.properties`)

## Data Storage

**Databases:**
- SQLite (Local)
  - Client: Room (`androidx.room:room-runtime`)
- DataStore (Preferences) - Key-value storage for app settings

**File Storage:**
- Local filesystem only (image saving, model storage)

**Caching:**
- Memory/Disk caching via Glide/Coil for images
- Retrofit OkHttp Cache for network requests

## Authentication & Identity

**Auth Provider:**
- Firebase Authentication - User management
  - Implementation: `com.google.firebase:firebase-auth-ktx`
- Google Sign-In - Single Sign-On
  - Implementation: `com.google.android.gms:play-services-auth:20.7.0`

## Monitoring & Observability

**Error Tracking:**
- None detected natively; likely relying on local logcat

**Logs:**
- OkHttp Logging Interceptor (`com.squareup.okhttp3:logging-interceptor`)

**Analytics:**
- Firebase Analytics (`com.google.firebase:firebase-analytics-ktx`)

## CI/CD & Deployment

**Hosting:**
- Google Play Store (Signing Config setup in `app/build.gradle.kts`)

**CI Pipeline:**
- None detected in immediate file search (no `.github/workflows` or similar checked explicitly, but standard Gradle builds apply).

## Environment Configuration

**Required env vars:**
Configured via `local.properties` file:
- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`
- `RAPIDAPI_KEY`
- `GOOGLE_VISION_API_KEY`
- `OPENAI_API_KEY`
- `API_NINJAS_KEY`
- `FOODDATA_CENTRAL_KEY`
- `THEMEALDB_API_KEY`

**Secrets location:**
- `local.properties` at project root (excluded from source control)

## Webhooks & Callbacks

**Incoming:**
- None

**Outgoing:**
- None

---

*Integration audit: 2026-04-08*