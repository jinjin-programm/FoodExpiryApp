# Phase 6: Detection - Context

**Gathered:** 2026-04-09
**Status:** Ready for planning

<domain>
## Phase Boundary

Users can scan a photo with multiple food items and get all of them identified and listed for confirmation. This adds multi-object detection (YOLO via MNN) to the existing single-item LLM inference from Phase 5, with a unified detection pipeline, batch confirmation UI, and smart default attribute engine.

</domain>

<decisions>
## Implementation Decisions

### YOLO Model Integration
- **D-01:** YOLO model bundled in APK assets (~5-20MB). No download needed — YOLO is small enough to bundle unlike the ~1.2GB LLM. Faster first-use experience.
- **D-02:** YOLO runs through MNN engine (same AAR as LLM). Reuses existing MNN infrastructure and ModelLifecycleManager mutual exclusion. No additional inference framework dependency.
- **D-03:** YOLO model format must be MNN-compatible (converted from standard YOLO format if needed). The agent determines the specific YOLO variant and model file.

### Tab Structure & Scan Navigation
- **D-04:** Three scan modes accessible via ViewPager2 horizontal swipe: Photo Scan (existing OCR), Barcode Scan (existing), YOLO Scan (new). No visible tab bar — swipe between modes.
- **D-05:** Unified detection pipeline: YOLO always runs first when on the YOLO scan tab. Single-item result uses the same batch flow with smart UI optimization (see D-09).
- **D-06:** Existing Photo Scan and Barcode Scan modes remain unchanged. YOLO scan is an additional mode, not a replacement.

### Batch Processing Flow
- **D-07:** Staged progress screen after capture on YOLO tab:
  - Stage 1: Full-screen preview darkens, center shows "Detecting food items..." (YOLO detection)
  - Stage 2: Updates to "Identifying item (1/5)..." with progress bar/circle, Cancel button visible
  - Stage 3: Auto-transition to Batch Confirmation screen (new Fragment via Navigation Component)
- **D-08:** Sequential processing per YOLO-07 (max 5-8 items) and YOLO-04 (sequential LLM classification). Bitmaps recycled immediately after each classification per YOLO-08.

### Result Confirmation UI
- **D-09:** Full-screen confirmation page (ConfirmationFragment) with:
  - Top bar: ← Back | "Confirm Items (N)" | ⋮ More
  - Optional original photo thumbnail (120dp, tappable to expand for reference)
  - Scrollable item list: each row shows food name, confidence badge (High/Medium/Low), edit/delete icons
  - Bottom fixed bar: [Cancel] [Add All to Fridge] (or "Add to Fridge" for single item)
- **D-10:** Smart single-item optimization: when only 1 item detected, item auto-selected, button says "Add to Fridge", no keyboard pops up. User: glance → tap. 2 touches total.
- **D-11:** Failed items show as placeholder in the list:
  - Thumbnail: cropped food image (normal)
  - Primary text: "Unknown item" (gray italic)
  - Secondary text: "Tap to edit name" (blue clickable) or failure reason
  - Right buttons: only Edit (prominent "Fix" button)
  - Background: light orange/yellow tint to draw attention
- **D-12:** Quick Mode (optional, off by default): Settings toggle via DataStore. When ON + exactly 1 item: 3-second auto-confirm countdown on button ("Auto-add in 3s..."). Any user interaction cancels countdown. Countdown cancels on `onStop()`. When OFF: standard manual confirm.

### Smart Default Attributes
- **D-13:** DefaultAttributeEngine: local lookup table mapping food names (EN + ZH keywords) to category and default shelf life. Uses `contains` matching on LLM-returned name. Examples:
  - Milk/奶/鮮奶 → Beverages/Dairy → +7 days
  - Cheese/芝士/起司 → Dairy → +14 days
  - Tomato/蕃茄/西紅柿 → Vegetables → +5 days
  - Chicken/雞 → Meat → +3 days (refrigerated)
  - Egg/蛋 → Eggs → +21 days
  - Rice/米 → Dry goods → +180 days
- **D-14:** Save flow: "Add All" → iterate items, apply DefaultAttributeEngine → batch insert to Room → popBackStack to YoloScanFragment → Snackbar with "View" action → optional Lottie checkmark animation.
- **D-15:** Snackbar "View" action navigates to inventory with `highlightNew=true` — scrolls to top, blinks new items.

### Navigation Architecture
- **D-16:** New ConfirmationFragment in nav graph. Navigation Component + Safe Args for navigation from scan to confirmation.
- **D-17:** Detection results stored in a temporary Room table (not passed via Safe Args or SharedViewModel). Survives process death / memory pressure — critical for 6-8GB devices after LLM inference.
- **D-18:** Post-save: pop back to YoloScanFragment, camera preview resumes, Snackbar provides "View" shortcut to inventory.

### Edge Cases
- **D-19:** 0 items detected: show "No food items detected" message, offer to retry or switch to Photo Scan tab.
- **D-20:** Process death during confirmation: temp Room table ensures data persists. ConfirmationFragment reads from DB on recreation.
- **D-21:** Model lifecycle during tab switch: ModelLifecycleManager must unload YOLO model before Photo/Barcode tabs access camera. Cancel pending analysis tasks during model switch. Prevent CameraAccessException or brief black screen.

### the agent's Discretion
- YOLO model variant selection (YOLOv5-nano, YOLOv8-nano, etc.) and MNN conversion
- Bounding box rendering approach (Canvas overlay vs custom View)
- DefaultAttributeEngine data structure and lookup implementation details
- ConfirmationFragment layout details (XML vs Compose, specific Material components)
- Error threshold for marking an LLM result as "failed" (confidence < X? null name?)
- Quick Mode countdown duration (3s default, configurable)
- Lottie animation specifics

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### MNN Integration (existing from Phase 5)
- `app/src/main/java/com/example/foodexpiryapp/inference/mnn/MnnLlmEngine.kt` — Current MNN engine pattern (loadModel, runInference, unloadModel, lifecycle management)
- `app/src/main/java/com/example/foodexpiryapp/inference/mnn/ModelLifecycleManager.kt` — Mutual exclusion between models, acquire/release pattern
- `app/src/main/java/com/example/foodexpiryapp/inference/mnn/MnnLlmNative.kt` — JNI bridge pattern for native inference
- `app/src/main/java/com/example/foodexpiryapp/inference/mnn/MnnLlmConfig.kt` — Config pattern for MNN models

### Existing Inference Pipeline
- `app/src/main/java/com/example/foodexpiryapp/domain/repository/LlmInferenceRepository.kt` — Domain interface (inferFood, modelState)
- `app/src/main/java/com/example/foodexpiryapp/domain/model/FoodIdentification.kt` — Structured output model (name, nameZh, confidence, expiryHint)
- `app/src/main/java/com/example/foodexpiryapp/domain/usecase/IdentifyFoodUseCase.kt` — Existing single-item use case
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/vision/VisionScanFragment.kt` — Current scan fragment (camera, capture, inference flow, FragmentResult bridge)

### Data Layer
- `app/src/main/java/com/example/foodexpiryapp/data/local/ModelStorageManager.kt` — Model file storage pattern
- `app/src/main/java/com/example/foodexpiryapp/di/InferenceModule.kt` — Hilt DI module for inference dependencies

### Architecture References
- `.planning/codebase/ARCHITECTURE.md` — Clean Architecture layers, data flow patterns
- `.planning/codebase/STRUCTURE.md` — Directory layout, naming conventions, where to add new code
- `.planning/codebase/CONVENTIONS.md` — Coding patterns, Hilt usage, coroutine patterns

### Requirements
- `.planning/REQUIREMENTS.md` §YOLO+LLM Food Detection (YOLO-01 through YOLO-08) — Batch pipeline, scan tab, bounding boxes, sequential classification, result confirmation, item cap, bitmap recycling

### Research
- `.planning/research/SUMMARY.md` — Full research findings including YOLO integration approach
- `.planning/research/PITFALLS.md` — Critical pitfalls: OOM from simultaneous models, bitmap memory leaks

### Prior Phase Context
- `.planning/phases/05-engine/05-CONTEXT.md` — Phase 5 decisions (UseCase/Repository pattern, single API surface, domain interface location)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `MnnLlmEngine`: MNN inference engine with load/run/unload pattern — YOLO engine should follow same pattern
- `ModelLifecycleManager`: Mutual exclusion with acquire/release — already supports multiple ModelType values, needs YOLO type added
- `IdentifyFoodUseCase`: Single-item food identification — new batch use case wraps this for multi-item
- `VisionScanFragment`: Camera setup, capture, image proxy → bitmap conversion — YOLO fragment needs similar camera code
- `LlmInferenceRepository` / `FoodIdentification`: Domain model for single result — needs batch equivalent
- FragmentResultListener `"llm_scan_result"` in InventoryFragment: bridge for scan results — batch results need similar mechanism
- Room database: existing infrastructure for persistence — temp table for detection results

### Established Patterns
- Clean Architecture: domain/repository/ for interfaces, data/repository/ for implementations
- UseCases in domain/usecase/: single-responsibility action classes
- HiltViewModel with StateFlow for reactive UI state
- FragmentResultListener for cross-fragment communication
- ViewPager2 for tab-like navigation (already used in MainTabsFragment)
- Navigation Component with Safe Args for inter-fragment navigation
- DataStore for user preferences (SettingsDataStore pattern exists)

### Integration Points
- VisionScanFragment: existing scan fragment — YOLO scan fragment parallels this
- MainTabsFragment or scan container: where ViewPager2 for scan modes lives
- InventoryFragment: receives scan results via FragmentResult or nav args
- Nav graph: needs new ConfirmationFragment destination
- Room database: needs temp detection results table + existing FoodItem table for batch insert

</code_context>

<specifics>
## Specific Ideas

- Confirmation screen layout: top bar with back + title + count, optional photo thumbnail (120dp), scrollable item list with edit/delete per row, bottom fixed bar with Cancel + Confirm All
- Failed item UI: gray italic "Unknown item", light orange/yellow background, prominent "Fix" button
- Quick Mode: 3-second auto-confirm countdown for single items, cancelable by any touch, stored in DataStore
- DefaultAttributeEngine: keyword-based lookup table (EN + ZH) → category + shelf life days
- Save flow: brief loading animation → popBackStack → Snackbar "View" → inventory with highlight animation
- Bounding box rendering on camera preview during YOLO detection phase

</specifics>

<deferred>
## Deferred Ideas

- Advanced bounding box editing (user can manually adjust boxes before classification) — future enhancement
- Real-time YOLO detection overlay (continuous detection while camera is open) — performance concern, defer to future phase
- Confidence-based auto-retry for low-confidence items — add complexity, can be added later
- Multi-language UI localization (currently English-only per user requirement)

</deferred>

---

*Phase: 06-detection*
*Context gathered: 2026-04-09*
