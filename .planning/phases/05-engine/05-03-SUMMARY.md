---
phase: 05-engine
plan: 03
subsystem: download
tags: [huggingface, download, wifi-check, resume, sha256, flow, room, hilt]

# Dependency graph
requires:
  - phase: 05-02
    provides: "HuggingFaceDownloadService, ModelStorageManager, DownloadStateDao, DownloadStateEntity"
provides:
  - "ModelDownloadManager orchestrating full download flow"
  - "ModelManifest with per-file SHA-256 hashes"
  - "DownloadUiState sealed class for all download UI states"
  - "WiFi check before cellular download (DL-06)"
  - "Progress Flow with percentage + ETA (DL-05)"
  - "Room-persisted resume support (DL-07)"
  - "SHA-256 verification via HuggingFace API hash resolution (DL-03)"
affects: [05-04, 05-05, presentation-download-ui]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Sealed class UI state pattern for download lifecycle"
    - "Mutex + AtomicBoolean for concurrent download prevention"
    - "HuggingFace API hash resolution at download start"

key-files:
  created:
    - "app/src/main/java/com/example/foodexpiryapp/domain/model/ModelManifest.kt"
    - "app/src/main/java/com/example/foodexpiryapp/domain/model/DownloadUiState.kt"
    - "app/src/main/java/com/example/foodexpiryapp/data/remote/ModelDownloadManager.kt"
  modified: []

key-decisions:
  - "Used HttpURLConnection instead of OkHttpClient for HuggingFace API hash fetch (consistent with HuggingFaceDownloadService pattern, no new deps)"
  - "resolveManifest() is synchronous not suspend (uses HttpURLConnection, called from coroutine context)"
  - "downloadModel() takes no okHttpClient param — hashes fetched internally via resolveManifest()"

patterns-established:
  - "Sealed class for download UI state (DownloadUiState)"
  - "Orchestrator pattern: ModelDownloadManager delegates to HuggingFaceDownloadService, ModelStorageManager, DownloadStateDao"

requirements-completed: [DL-01, DL-05, DL-06, DL-07]

# Metrics
duration: 2min
completed: 2026-04-09
---

# Phase 5 Plan 03: Model Download Orchestrator Summary

**ModelDownloadManager orchestrating sequential HuggingFace model downloads with WiFi check, resume-from-Room, progress Flow, and SHA-256 verification**

## Performance

- **Duration:** 2 min
- **Started:** 2026-04-08T16:54:35Z
- **Completed:** 2026-04-08T16:56:36Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- ModelManifest + ModelFile domain models with per-file SHA-256 hash fields
- DownloadUiState sealed class covering all download lifecycle states (Ready, NotDownloaded, WifiCheckRequired, Downloading, Verifying, Complete, Error, Paused)
- ModelDownloadManager @Singleton orchestrating full download: WiFi check → SHA-256 hash resolution → sequential file download → verification → atomic rename
- Resume support via Room-persisted download state (DL-07)
- Progress emission with percentage, ETA, speed, current file, overall progress (DL-05)
- WiFi connectivity check before cellular download (DL-06)

## Task Commits

Each task was committed atomically:

1. **Task 1: Create ModelManifest and DownloadUiState models** - `3418ea1` (feat)
2. **Task 2: Create ModelDownloadManager orchestrator** - `6f6f229` (feat)

## Files Created/Modified
- `app/src/main/java/com/example/foodexpiryapp/domain/model/ModelManifest.kt` - Model file manifest with version, modelId, per-file SHA-256 hashes and size estimates
- `app/src/main/java/com/example/foodexpiryapp/domain/model/DownloadUiState.kt` - Sealed class for download UI state (8 states covering full lifecycle)
- `app/src/main/java/com/example/foodexpiryapp/data/remote/ModelDownloadManager.kt` - Download orchestrator with WiFi check, progress Flow, resume, SHA-256 verification, cancel/reset

## Decisions Made
- **HttpURLConnection for HuggingFace API**: Used `HttpURLConnection` instead of `OkHttpClient` for the SHA-256 hash fetch, consistent with `HuggingFaceDownloadService` pattern. Avoids adding a new dependency when only logging-interceptor is present.
- **Synchronous resolveManifest()**: Made `resolveManifest()` non-suspend since it uses blocking `HttpURLConnection`. Always called from coroutine context so this is safe.
- **No OkHttpClient parameter on downloadModel()**: Plan specified `okHttpClient: OkHttpClient?` param, but adapted to internal `resolveManifest()` call using `HttpURLConnection` to avoid coupling ViewModels to HTTP client details.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Adapted to actual DAO signature**
- **Found during:** Task 2 (ModelDownloadManager creation)
- **Issue:** Plan's interface showed `downloadStateDao.updateProgress(filePath, bytes, status)` with 3 params, but actual DAO has 5 params: `(filePath, bytes, totalBytes, status, timestamp)`
- **Fix:** Updated `downloadModel()` to pass `progress.totalBytes` as third param to match actual `DownloadStateDao.updateProgress()` signature
- **Files modified:** ModelDownloadManager.kt
- **Committed in:** `6f6f229` (Task 2 commit)

**2. [Rule 2 - Missing Critical] Corrected DownloadStateEntity import path**
- **Found during:** Task 2 (ModelDownloadManager creation)
- **Issue:** Plan specified import `com.example.foodexpiryapp.data.local.entity.DownloadStateEntity` but actual entity is in `data.local.database` package
- **Fix:** Used correct import `com.example.foodexpiryapp.data.local.database.DownloadStateEntity`
- **Files modified:** ModelDownloadManager.kt
- **Committed in:** `6f6f229` (Task 2 commit)

**3. [Rule 2 - Missing Critical] Used HttpURLConnection instead of OkHttpClient for API fetch**
- **Found during:** Task 2 (fetchSha256Hashes implementation)
- **Issue:** Plan specified `OkHttpClient` for HuggingFace API calls, but project only has `okhttp3:logging-interceptor` (not core OkHttp), and `HuggingFaceDownloadService` uses `HttpURLConnection`
- **Fix:** Implemented `fetchSha256Hashes()` using `HttpURLConnection` consistent with project patterns; removed `okHttpClient` parameter from `resolveManifest()` and `downloadModel()`
- **Files modified:** ModelDownloadManager.kt
- **Committed in:** `6f6f229` (Task 2 commit)

---

**Total deviations:** 3 auto-fixed (3 missing critical — interface adaptations)
**Impact on plan:** All adaptations necessary for correctness against actual codebase interfaces. No scope creep.

## Issues Encountered
None — plan executed smoothly with interface adaptations handled inline.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- ModelDownloadManager ready for ViewModel integration (download UI)
- HuggingFaceDownloadService + ModelStorageManager + DownloadStateDao all wired
- Next plans (05-04, 05-05) can use ModelDownloadManager for download orchestration

## Self-Check: PASSED

All files exist, all commits verified:
- ✅ ModelManifest.kt
- ✅ DownloadUiState.kt
- ✅ ModelDownloadManager.kt
- ✅ Commit 3418ea1 (Task 1)
- ✅ Commit 6f6f229 (Task 2)

---
*Phase: 05-engine*
*Completed: 2026-04-09*
