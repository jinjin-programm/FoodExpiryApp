# Roadmap: FoodExpiryApp

## Milestones

- ✅ **v1.0 MVP** — 3 phases, 11 plans (shipped 2026-04-08)
- 🔄 **v2.0 AI Vision Engine Overhaul** — 4 phases (in progress)

## Phases

<details>
<summary>✅ v1.0 MVP (Phases 1-3) — SHIPPED 2026-04-08</summary>

- [x] Phase 1: Main Flow First (6/6 plans) — Unified data model, empty state, photo scan, expiring soon, search
- [x] Phase 2: Connect Modules (2/2 plans) — Expiry-aware recipe scoring, planner recipe picker
- [x] Phase 3: Polish & Trust (3/3 plans) — Shopping templates, privacy section, optional Google sign-in, notification settings

</details>

Full details: [.planning/milestones/v1.0-ROADMAP.md](milestones/v1.0-ROADMAP.md)

<details>
<summary>🔄 v2.0 AI Vision Engine Overhaul (Phases 4-7) — IN PROGRESS</summary>

- [ ] **Phase 4: Foundation** — Backup, MNN AAR integration, llama.cpp removal
- [ ] **Phase 5: Engine** — LLM inference wrapper, dynamic model download from HuggingFace
- [ ] **Phase 6: Detection** — YOLO+LLM batch pipeline, new scan tab, multi-item detection
- [ ] **Phase 7: Scan UI Overhaul** — Title bar removal, horizontal frame, capture animation, barcode redesign

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
**Plans**: TBD

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
**Plans**: TBD

### Phase 6: Detection
**Goal**: Users can scan a photo with multiple food items and get all of them identified and listed for confirmation
**Depends on**: Phase 5
**Requirements**: YOLO-01, YOLO-02, YOLO-03, YOLO-04, YOLO-05, YOLO-06, YOLO-07, YOLO-08
**Success Criteria** (what must be TRUE):
  1. User can switch to a new "YOLO Scan" tab and see bounding boxes drawn around detected food items on the camera preview
  2. After capture, the app crops detected regions, classifies each with the local LLM, and displays a list of identified foods
  3. User can review and confirm detected items before saving to inventory
  4. Processing caps at 5-8 items per scan and recycles bitmaps immediately — no memory issues on 6GB devices
**Plans**: TBD

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
**Plans**: TBD
**UI hint**: yes

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 4. Foundation | 0/0 | Not started | - |
| 5. Engine | 0/0 | Not started | - |
| 6. Detection | 0/0 | Not started | - |
| 7. Scan UI Overhaul | 0/0 | Not started | - |

---
*Roadmap created: 2026-04-08 for v2.0 AI Vision Engine Overhaul*
