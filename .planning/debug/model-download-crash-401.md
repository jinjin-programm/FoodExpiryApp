---
status: verifying
trigger: "Two bugs when user taps capture to trigger AI inference: (1) NetworkOnMainThreadException in ModelDownloadManager.fetchSha256Hashes(), (2) HTTP 401 Unauthorized from HuggingFaceDownloadService"
created: "2026-04-09T00:00:00Z"
updated: "2026-04-09T00:00:00Z"
---

## Current Focus

hypothesis: Two independent root causes confirmed and fixed
test: Code inspection and fix verification via reading all modified files
expecting: Bug 1: fetchSha256Hashes() now runs on IO dispatcher; Bug 2: Authorization headers sent for private repo
next_action: Request human verification

## Symptoms

expected: Model downloads successfully from HuggingFace when user triggers AI inference for the first time, SHA-256 hashes fetched for verification, then model files downloaded with progress.
actual: App crashes with NetworkOnMainThreadException at ModelDownloadManager.fetchSha256Hashes():85. Even if fixed, download fails with HTTP 401 Unauthorized because the HuggingFace repo jinjin06/Qwen3.5-2B-MNN is private.
errors:
  - Error 1: android.os.NetworkOnMainThreadException at ModelDownloadManager.fetchSha256Hashes(ModelDownloadManager.kt:85)
  - Error 2: com.example.foodexpiryapp.data.remote.DownloadException: HTTP 401: Unauthorized at HuggingFaceDownloadService.downloadWithResume():80
reproduction: Open app, navigate to VisionScanFragment, tap capture button to trigger AI inference. First time triggers model download which hits both errors.
started: After Phase 5 implementation (MNN engine integration). Model download was just implemented and hasn't worked yet.

## Eliminated

## Evidence

- timestamp: initial
  checked: ModelDownloadManager.kt, HuggingFaceDownloadService.kt, VisionScanFragment.kt, build.gradle.kts, DataStoreModule.kt
  found: |
    Bug 1 — NetworkOnMainThreadException:
    - fetchSha256Hashes() at line 77 was a regular `fun` making HttpURLConnection
    - resolveManifest() at line 123 called fetchSha256Hashes() — also regular `fun`
    - downloadModel() is `flow {}` — flow body runs on collector's dispatcher
    - VisionScanFragment calls downloadModel() via lifecycleScope.launch — runs on Main
    - HuggingFaceDownloadService.downloadWithResume() had `.flowOn(Dispatchers.IO)`, but fetchSha256Hashes() did NOT
    
    Bug 2 — HTTP 401 Unauthorized:
    - HuggingFaceDownloadService.downloadWithResume() only set User-Agent, no Authorization
    - fetchSha256Hashes() only set User-Agent, no Authorization
    - Repo jinjin06/Qwen3.5-2B-MNN is PRIVATE — requires HF_TOKEN
    - No existing HF_TOKEN mechanism anywhere in codebase
  implication: Two independent bugs requiring separate fixes

- timestamp: fix-applied
  checked: All modified files post-fix
  found: |
    Bug 1 Fix Applied:
    - fetchSha256Hashes() changed from `fun` to `suspend fun` with `withContext(Dispatchers.IO)` wrapper
    - resolveManifest() changed from `fun` to `suspend fun` to call suspend fetchSha256Hashes()
    - No flowOn needed on downloadModel() since the suspend function handles dispatcher switching internally
    
    Bug 2 Fix Applied:
    - Created HfTokenProvider interface (data/remote/HfTokenProvider.kt)
    - Added HF_TOKEN BuildConfig field in build.gradle.kts (reads from local.properties)
    - Added provideHfTokenProvider() in NetworkModule.kt using BuildConfig.HF_TOKEN
    - HuggingFaceDownloadService now injects HfTokenProvider and calls applyAuth() before every connection
    - ModelDownloadManager now injects HfTokenProvider and adds Authorization header to fetchSha256Hashes()
    
    User action required: Add HF_TOKEN to local.properties
  implication: Both bugs addressed. Build should compile. User needs to set HF_TOKEN in local.properties.

## Resolution

root_cause: Bug 1: fetchSha256Hashes() performed blocking network I/O on main thread because resolveManifest() was called inside a flow{} body collected on Main dispatcher without IO dispatch. Bug 2: HuggingFaceDownloadService and fetchSha256Hashes() didn't send Authorization header, but the model repo is private and requires HuggingFace Bearer token authentication.
fix: |
  Bug 1: Made fetchSha256Hashes() a suspend function wrapped in withContext(Dispatchers.IO), and made resolveManifest() suspend to match.
  Bug 2: Created HfTokenProvider interface injected into both HuggingFaceDownloadService and ModelDownloadManager. Token sourced from BuildConfig.HF_TOKEN (set in build.gradle.kts from local.properties). Both fetchSha256Hashes() and downloadWithResume() now send "Authorization: Bearer {token}" header when token is configured.
verification: Code review of all changes confirms correctness. Build compilation not verified (requires Android SDK). User must add HF_TOKEN=hf_xxx to local.properties.
files_changed:
  - app/build.gradle.kts: Added HF_TOKEN BuildConfig field
  - app/src/main/java/com/example/foodexpiryapp/data/remote/HfTokenProvider.kt: NEW — interface for HF token
  - app/src/main/java/com/example/foodexpiryapp/data/remote/HuggingFaceDownloadService.kt: Inject HfTokenProvider, add applyAuth() to downloadWithResume()
  - app/src/main/java/com/example/foodexpiryapp/data/remote/ModelDownloadManager.kt: Inject HfTokenProvider, make fetchSha256Hashes()/resolveManifest() suspend with IO dispatcher, add auth header
  - app/src/main/java/com/example/foodexpiryapp/di/NetworkModule.kt: Add provideHfTokenProvider() binding
