# Roadmap: FoodExpiryApp

## Milestones

- ✅ **v1.0 MVP** — 3 phases, 11 plans (shipped 2026-04-08)
- 🔄 **v2.0 AI Vision Engine Overhaul** — 6 phases (in progress)

## Phases

<details>
<summary>✅ v1.0 MVP (Phases 1-3) — SHIPPED 2026-04-08</summary>

- [x] Phase 1: Main Flow First (6/6 plans) — Unified data model, empty state, photo scan, expiring soon, search
- [x] Phase 2: Connect Modules (2/2 plans) — Expiry-aware recipe scoring, planner recipe picker
- [x] Phase 3: Polish & Trust (3/3 plans) — Shopping templates, privacy section, optional Google sign-in, notification settings

</details>

Full details: [.planning/milestones/v1.0-ROADMAP.md](milestones/v1.0-ROADMAP.md)

<details>
<summary>🔄 v2.0 AI Vision Engine Overhaul (Phases 4-9) — IN PROGRESS</summary>

- [x] **Phase 4: Foundation** — Backup, MNN AAR integration, llama.cpp removal
- [x] **Phase 5: Engine** — LLM inference wrapper, dynamic model download from HuggingFace
- [x] **Phase 6: Detection** — YOLO+LLM batch pipeline, new scan tab, multi-item detection
- [x] **Phase 7: Scan UI Overhaul** — Title bar removal, horizontal frame, capture animation, barcode redesign
- [ ] **Phase 8: Remote Detection Hardening** — ⚠️ 重新規劃中（原 MNN 方案已擱置，改用 Ollama/LM Studio 遠端方案）
- [ ] **Phase 9: Verification & Artifact Cleanup** — Missing verification docs, planning state sync

</details>

## Phase Details

### Phase 4: Foundation
**Goal**: Project is safely backed up and MNN v3.5.0 replaces llama.cpp as the inference runtime with zero conflicts
**Depends on**: v1.0 shipped
**Requirements**: SAFE-01, SAFE-02, MNN-01, MNN-02
**Success Criteria** (what must be TRUE):
  1. Full project backup exists and a `v1.1.0-backup` git tag points to the current stable state
  2. MNN v3.5.0 AAR is added to project dependencies and builds successfully
  3. All llama.cpp artifacts are removed — no native `.so` files, no JNI code, no CMake references
  4. App launches and existing features (inventory, recipes, planner, shopping, barcode scan) still work after the engine swap
**Plans**: 3 plans
- [x] 04-01-PLAN.md — Create v1.1.0-backup git tag and update .gitignore
- [x] 04-02-PLAN.md — Remove all llama.cpp artifacts (native libs, JNI, Kotlin wrappers)
- [x] 04-03-PLAN.md — Integrate MNN v3.5.0 AAR and verify build

### Phase 5: Engine
**Goal**: MNN engine loads Qwen3.5-2B-MNN and runs LLM inference with structured JSON output, and users can download the model on demand
**Depends on**: Phase 4
**Requirements**: MNN-03, MNN-04, MNN-05, MNN-06, DL-01, DL-02, DL-03, DL-04, DL-05, DL-06, DL-07
**Success Criteria** (what must be TRUE):
  1. User can download the Qwen3.5-2B-MNN model from HuggingFace on first use, see download progress with percentage and ETA
  2. Download resumes automatically after interruption (app kill, network loss) and verifies integrity with SHA-256
  3. User is warned before downloading over cellular and can wait for WiFi
  4. LLM inference returns structured JSON with food name and Chinese name for a single food image
  5. Model lifecycle manager prevents OOM — only one heavy model loaded at a time, auto-cleanup on backgrounding
  6. MNN engine is accessible as a Hilt singleton with a clean Kotlin API
**Plans**: 7 plans
- [x] 05-01-PLAN.md — Domain contracts: FoodIdentification model + LlmInferenceRepository interface (Wave 1)
- [x] 05-02-PLAN.md — Download infrastructure: HTTP Range resume, SHA-256, .part files, Room persistence (Wave 1)
- [x] 05-03-PLAN.md — Download orchestrator: ModelDownloadManager, WiFi check, progress Flow (Wave 2)
- [x] 05-04-PLAN.md — MNN engine: MnnLlmEngine, ModelLifecycleManager, StructuredOutputParser (Wave 2)
- [x] 05-05-PLAN.md — Repository wiring: LlmInferenceRepositoryImpl, IdentifyFoodUseCase, InferenceModule (Wave 3)
- [x] 05-06-PLAN.md — UI integration: VisionScanFragment, ChatViewModel, FoodExpiryApp updates (Wave 4)
- [ ] 05-07-PLAN.md — Verification: File existence, build, requirement coverage, human checkpoint (Wave 5)

### Phase 6: Detection
**Goal**: Users can scan a photo with multiple food items and get all of them identified and listed for confirmation
**Depends on**: Phase 5
**Requirements**: YOLO-01, YOLO-02, YOLO-03, YOLO-04, YOLO-05, YOLO-06, YOLO-07, YOLO-08
**Success Criteria** (what must be TRUE):
  1. User can switch to a new "YOLO Scan" tab and see bounding boxes drawn around detected food items on the camera preview
  2. After capture, the app crops detected regions, classifies each with the local LLM, and displays a list of identified foods
  3. User can review and confirm detected items before saving to inventory
  4. Processing caps at 5-8 items per scan and recycles bitmaps immediately — no memory issues on 6GB devices
**Plans**: 4 plans
- [ ] 06-01-PLAN.md — YOLO engine + batch detection pipeline (domain contracts, MnnYoloEngine, DetectionPipeline)
- [ ] 06-02-PLAN.md — Data persistence layer (Room entity, DAO, migration v10→v11, DetectionResultRepository)
- [ ] 06-03-PLAN.md — Scan UI (ViewPager2 container, YOLO scan tab, ConfirmationFragment)
- [ ] 06-04-PLAN.md — Save flow (DefaultAttributeEngine, Quick Mode, batch save, Snackbar)

### Phase 7: Scan UI Overhaul
**Goal**: Scan screens have a cleaner, more modern look with no top title bar, horizontal capture frame, and satisfying capture-to-result flow
**Depends on**: Phase 4 (can be done in parallel with Phases 5-6)
**Requirements**: UI-01, UI-02, UI-03, UI-04, UI-05, UI-06, UI-07, UI-08
**Success Criteria** (what must be TRUE):
  1. No "FoodExpiryApp" title bar appears on any page — only a back button for navigation
  2. Camera scan frame is horizontal (landscape) orientation for better multi-item framing
  3. Pressing capture shows a shutter flash animation and freezes the frame
  4. After capture, a loading screen shows staged AI inference progress ("Detecting objects..." → "Analyzing N items..." → "Done")
  5. Barcode scan uses a manual tap-to-capture button — no auto-scan, no flash button, no close button
**Plans**: 4 plans
- [x] 07-01-PLAN.md — Switch to NoActionBar theme, remove title bar from all main tabs (Wave 1)
- [x] 07-02-PLAN.md — Remove static frames, add floating back arrows, unify capture button across all scan modes (Wave 1)
- [x] 07-03-PLAN.md — Add white flash animation and staged progress overlay to Photo + YOLO scan (Wave 2)
- [x] 07-04-PLAN.md — Redesign barcode scan to manual capture with result card + human checkpoint (Wave 2)
**UI hint**: yes

### Phase 8: Remote Detection Hardening (重新規劃)
**Goal**: ONNX Runtime YOLOE 本地偵測 + 遠端 Ollama/LM Studio LLM 分類 pipeline 端到端穩定運作
**Depends on**: Phase 6, Phase 7
**Architecture Change**: MNN/TFLite 本地推論已擱置 (2026-04-18)，改用 ONNX Runtime YOLOE-26n-seg-pf + 遠端 LLM
**Previous Plans (superseded)**:
- ~~08-01-PLAN.md~~ — JNI Bridge (基於 MNN，已擱置)
- ~~08-02-PLAN.md~~ — Overlay + 程式碼強化 (部分成果仍可用)
- ~~08-03-PLAN.md~~ — MNN 單元測試 (基於 MNN，已擱置)
- ~~08-04-PLAN.md~~ — TFLite YOLOv8n engine (已擱置，改用 ONNX Runtime)
- ~~08-05-PLAN.md~~ — TFLite pipeline hardening (已擱置)
- ~~08-06-PLAN.md~~ — TFLite VisionScan toggle (已擱置)
- ~~08-07-PLAN.md~~ — TFLite testing (已擱置)
**Plans**: 3 plans
- [ ] 08-08-PLAN.md — ONNX Runtime YOLOE engine + model asset + post-processor (Wave 1)
- [ ] 08-9-PLAN.md — DI wiring + debug overlay for detection verification (Wave 2)
- [ ] 08-10-PLAN.md — Unit tests + real device verification checkpoint (Wave 3)

### Phase 9: Verification & Artifact Cleanup
**Goal**: All phases have formal verification artifacts, planning state reflects reality, untracked files committed
**Depends on**: Phase 8
**Gap Closure**: Closes audit gaps MNN-03, untracked files, ROADMAP/STATE sync
**Success Criteria** (what must be TRUE):
  1. `05-VERIFICATION.md` exists in `.planning/phases/05-engine/` with 11/11 requirements verified
  2. `06-REVIEW.md` and `06-VERIFICATION.md` are tracked in git
  3. ROADMAP.md progress table shows actual completion percentages
  4. STATE.md milestone progress matches reality (4/4 phases, 18/18 plans)
  5. REQUIREMENTS.md checkboxes reflect audit findings (24/27 satisfied)
**Plans**: 2 plans

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 4. Foundation | 3/3 | Complete | 2026-04-09 |
| 5. Engine | 7/7 | Complete ✅ LLM WORKING | 2026-04-10 |
| 6. Detection | 4/4 | Complete | 2026-04-09 |
| 7. Scan UI Overhaul | 4/4 | Complete | 2026-04-09 |
| 8. Remote Detection Hardening | 0/3 | Planned (ONNX Runtime) | - |
| 9. Verification & Artifact Cleanup | 0/2 | Not started | - |

---
*Roadmap updated: 2026-04-15 — Ollama remote migration in progress: replacing local MNN inference with remote Ollama API (qwen3.5:9b)*

### Architecture Change: MNN → Ollama (2026-04-15)

**Why:** Local MNN model (Qwen3.5-2B-VL) has persistent hallucination issues across devices. Remote Ollama server with qwen3.5:9b provides better results with JSON Schema enforcement.

**Impact on Phases:**
- Phase 5 (Engine): MNN engine retained in code but DI disabled. LlmInferenceRepositoryImpl now uses OllamaVisionClient.
- Phase 6 (Detection): DetectionPipeline needs update — currently depends on MnnLlmEngine for YOLO+LLM batch classification.
- Phase 8 (YOLO Hardening): Paused until DetectionPipeline is updated.

**Migration Progress:**
- [x] Ollama API client (OkHttp dynamic URL)
- [x] OllamaVisionClient (Bitmap→Base64→API→FoodIdentification)
- [x] OllamaServerConfig (DataStore persistence)
- [x] LlmInferenceRepositoryImpl rewritten for Ollama
- [x] DI modules updated (InferenceModule, DataStoreModule)
- [x] VisionScanFragment updated (removed model download, added server check)
- [x] ChatViewModel/ChatFragment updated
- [x] OllamaSettingsDialog (server URL, model name, API token, test connection)
- [ ] DetectionPipeline updated to use OllamaVisionClient
- [ ] Build verified
- [ ] End-to-end tested with real Ollama server
