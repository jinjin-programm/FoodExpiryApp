# Phase 5: Engine - Context

**Gathered:** 2026-04-08
**Status:** Ready for planning

<domain>
## Phase Boundary

MNN engine loads Qwen3.5-2B-MNN and runs LLM inference with structured JSON output, and users can download the model on demand from HuggingFace. This is infrastructure only — no new user-facing screens.

</domain>

<decisions>
## Implementation Decisions

### LLM API Architecture
- **D-01:** UseCase/Repository pattern — do NOT expose a raw Hilt singleton MnnLlmEngine directly to ViewModels. Create a domain-layer `LlmInferenceRepository` interface with data-layer implementation wrapping MNN. This enables swapping MNN for other engines (e.g. MediaPipe LLM Inference API) without changing ViewModels.
- **D-02:** Single method API surface — `inferFood(Bitmap): Flow<FoodIdentification>`. The Repository/UseCase handles model loading, prompt building, inference, and JSON parsing internally. Callers just collect the Flow.
- **D-03:** Domain interface + data implementation — Repository interface lives in `domain/repository/`, implementation in `data/repository/`. Matches existing pattern (e.g. `FoodItemRepository`).

### Agent's Discretion
- Model download UX flow (dialog vs bottom sheet vs full-screen gate, trigger timing)
- Prompt engineering details (system prompt design, output format constraints, max_new_tokens)
- Model lifecycle specifics (load on app start vs lazy, memory thresholds, Activity lifecycle handling)
- Structured output parsing strategy (regex vs JSON parser, fallback for malformed output)
- Download service implementation details (state machine, progress reporting, WiFi detection)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### MNN Integration
- https://github.com/alibaba/MNN (v3.5.0, MNN LLM documentation) — Official MNN engine, mmap deadlock fix, LLM config options
- https://github.com/alibaba/MNN/tree/master/apps/Android/MnnLlmChat — Proven Android integration pattern, Kotlin wrapper reference
- https://huggingface.co/taobao-mnn/Qwen3.5-2B-MNN — Qwen3.5-2B-MNN model (4-bit quantized, Apache-2.0)
- https://huggingface.co/jinjin06/Qwen3.5-2B-MNN — User's HuggingFace mirror for dynamic download

### Research
- `.planning/research/SUMMARY.md` — Full research findings, 15 pitfalls, architecture approach, confidence assessment
- `.planning/research/PITFALLS.md` — Critical pitfalls: OOM from simultaneous models, mmap deadlock, download without resume
- `.planning/research/ARCHITECTURE.md` — Recommended component structure (inference/mnn/, inference/pipeline/, data/remote/)

### Requirements
- `.planning/REQUIREMENTS.md` §MNN Inference Engine (MNN-03 through MNN-06) — LLM inference, structured JSON, lifecycle, Hilt singleton
- `.planning/REQUIREMENTS.md` §Dynamic Model Download (DL-01 through DL-07) — HuggingFace download, resume, SHA-256, WiFi prompt

### Existing Code
- `app/src/main/java/com/taobao/android/mnn/MNNNetInstance.java` — Low-level MNN net wrapper (createFromFile, createFromBuffer, Config)
- `app/src/main/java/com/taobao/android/mnn/MNNForwardType.java` — Forward type constants (CPU, GPU, etc.)
- `app/src/main/java/com/taobao/android/mnn/MNNNetNative.java` — JNI native bridge
- `app/src/main/java/com/taobao/android/mnn/MNNImageProcess.java` — Image preprocessing utilities
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/chat/ChatViewModel.kt` — Existing HiltViewModel with Phase 5 TODO stubs
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/vision/VisionScanFragment.kt` — Vision scan with Phase 5 TODO stubs (loadModelIfNeeded, AI inference)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- MNN Java wrappers (5 classes in `com.taobao.android.mnn`): MNNNetInstance, MNNForwardType, MNNImageProcess, MNNNetNative, MNNPortraitNative — low-level JNI wrappers from official MNN demo app
- Hilt DI modules: DatabaseModule, NetworkModule, DataStoreModule, RepositoryModule, AnalyticsModule — need new MnnModule or extend existing
- OkHttp (v4.12.0) + WorkManager (v2.9.0): already in project, usable for HTTP Range downloads and background scheduling
- DataStore preferences: already configured for settings persistence, can store download state

### Established Patterns
- Clean Architecture: domain/repository/ for interfaces, data/repository/ for implementations
- UseCases in domain/usecase/: single-responsibility action classes
- HiltViewModel with StateFlow for reactive UI state
- FragmentResultListener pattern for cross-fragment communication (InventoryFragment listens for "llm_scan_result")
- Error handling: try-catch + Flow/Result wrappers in repositories

### Integration Points
- VisionScanFragment.loadModelIfNeeded() — TODO stub waiting for Phase 5 model loading
- ChatViewModel.init / sendMessage() — TODO stubs waiting for Phase 5 LLM integration
- FoodExpiryApp.onCreate() — TODO stub for Phase 5 model warmup
- InventoryFragment FragmentResultListener "llm_scan_result" — existing bridge for LLM scan results
- DI module registration in di/ — need new module for MNN dependencies

</code_context>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 05-engine*
*Context gathered: 2026-04-08*
