---
gsd_state_version: 1.0
milestone: v2.0
milestone_name: AI Vision Engine Overhaul
status: executing
last_updated: "2026-04-18T18:00:00.000Z"
progress:
  total_phases: 6
  completed_phases: 4
  total_plans: 21
  completed_plans: 20
  percent: 95
---

# Project State: FoodExpiryApp

**Status:** Executing Phase 8 (YOLO Hardening) — 2/3 plans complete

## Project Reference

See: .planning/PROJECT.md (updated 2026-04-08)

**Core value:** Let a new user add their first food item within 30 seconds and reduce the mental burden of food management.
**Current focus:** Phase 8 YOLO Hardening + post-phase feature additions

## Current Position

Phase: 08 (yolo-hardening) — 2/3 plans done
Migration: Ollama + LM Studio dual provider — COMPLETE
UI Enhancement: Food image system — COMPLETE
Feature: Shelf Life Database + Auto-Learn — COMPLETE
Feature: Gallery Photo Picker for AI Vision Scan — COMPLETE
Bugfix: AI Vision Scan navigation button — COMPLETE

- **Milestone:** v2.0 AI Vision Engine Overhaul
- **Active Task:** Phase 8 Plan 03 (tests + ViewModel detections)
- **Status:** All features building and passing, multiple post-phase features added

### v2.0 Progress

```
Phase 4: Foundation       [██████████] 100% ✅
Phase 5: Engine           [██████████] 100% ✅
Phase 6: Detection        [██████████] 100% ✅
Phase 7: Scan UI Overhaul [██████████] 100% ✅
Phase 8: YOLO Hardening   [██████░░░░] 67%  ← 2/3 plans done
Phase 9: Verification     [          ] 0%
```

## Milestone History

### v1.0 MVP (Shipped: 2026-04-08)

- **Phases:** 3/3 complete
- **Plans:** 11/11 complete
- **Requirements:** 14/14 validated
- **UAT:** 14/14 passed
- **Duration:** ~58 days

### v2.0 AI Vision Engine Overhaul (In Progress)

- **Phases:** 4/6 complete (Phases 4-7)
- **Plans:** 18/25 complete
- **Requirements:** 24/27 validated

#### Major Events

| Date | Event | Impact |
|------|-------|--------|
| 2026-04-10 | MNN LLM first successful food identification on device | Phase 5 complete |
| 2026-04-12 | Snapdragon 8 Gen 3 garbage output fix | Cross-device compatibility |
| 2026-04-15 | Decision: migrate to remote Ollama API | Architecture change |
| 2026-04-18 | Shelf Life Database + Auto-Learn system | Room v12, seed data, management UI |
| 2026-04-18 | Gallery Photo Picker for AI Vision Scan | Pick Visual Media, thumbnail preview |
| 2026-04-18 | Fix AI Vision Scan navigation bug | Try-catch replaces stale currentDestination guard |

#### Ollama Migration Rationale

1. **Hallucination**: Qwen3.5-2B-VL via MNN produces unreliable results across devices
2. **Deployment complexity**: ~1.2GB model download, native .so files, JNI bridges, ABI matching
3. **Device compatibility**: Works on some devices, breaks on others
4. **Remote server benefits**: Larger model (qwen3.5:9b), structured JSON Schema output, no local resources needed

## Accumulated Context

### Key Decisions

| Decision | Rationale | Status |
|----------|-----------|--------|
| MNN Chat official component | Avoid custom inference layer, battle-tested | ✅ Done → Now migrating away |
| Qwen3.5-2B-MNN model | 4-bit quantized, lightweight for mobile | ✅ Working → Replaced by remote qwen3.5:9b |
| Qwen3.5-0.8B-MNN evaluated | Too weak for visual food classification | ❌ Rejected |
| YOLO + LLM (skip CLIP) | CLIP overlaps with LLM capability | ✅ Done |
| Dynamic model download from HF | Reduce APK size by ~40MB | ✅ Done → No longer needed |
| Sequential YOLO→LLM processing | Prevent OOM on <6GB devices | ✅ Done |
| Complete llama.cpp removal | libllm.so naming collision | ✅ Done |
| **Remote Ollama API** | Solve hallucination, simplify deployment, use larger model | ✅ Done |
| **Ollama + JSON Schema** | Force structured output to prevent hallucination | ✅ Done |
| **Cloudflare Tunnel** | Public access to home Ollama server | ✅ Done |
| **Ollama think:false** | Disable thinking mode for faster inference | ✅ Done (~16% faster) |
| **LM Studio provider** | OpenAI-compatible alternative, faster inference | ✅ Done |
| **Dual-provider architecture** | User can switch Ollama/LM Studio in settings | ✅ Done |
| **99 food UI images** | Real food photos for all list/card items instead of placeholder icons | ✅ Done |
| **FoodImageResolver** | Smart name→drawable mapping with fuzzy matching + category fallback | ✅ Done |
| **Shelf Life DB (Room v12)** | Replace hardcoded shelf life with Room-backed auto-learn system | ✅ Done |
| **Gallery Photo Picker** | Users can select photos from device gallery for AI analysis | ✅ Done |
| **PickVisualMedia API** | No permissions needed, system photo picker | ✅ Done |

### Critical Constraints

- Inference requires network connectivity (remote server)
- DetectionPipeline uses FoodVisionClient interface — supports Ollama + LM Studio
- Provider selection persisted in DataStore, switchable at runtime
- MNN code preserved but DI bindings commented out
- Gallery photos sent as full image to LLM; camera captures are cropped to focus rectangle
- Database at version 12 — any schema changes need MIGRATION_12_13

### References

- Ollama API: https://github.com/ollama/ollama/blob/main/docs/api.md
- LM Studio API: OpenAI-compatible (/v1/chat/completions)
- Models: qwen3.5:9b (Ollama), qwen/qwen3.5-9b (LM Studio)
- Server: User's home PC (AMD RX 9070 XT 16GB + iGPU)
- Network: Cloudflare Tunnel → public domain → localhost:11434 (Ollama) / :1234 (LM Studio)

### Blockers

None — build passing, both providers functional.

### TODO

- [ ] Phase 8 Plan 03: Unit tests + ViewModel detections + device verification
- [ ] Phase 9: Verification & Artifact Cleanup
- [ ] Shelf Life: Unit tests for LookupShelfLifeUseCase cascade
- [ ] Shelf Life: Deprecate/remove DefaultAttributeEngine and ShelfLifeEstimator
- [ ] Shelf Life: Wire ShelfLifeEditDialog to actually read updated fields from dialog inputs
- [ ] YOLO Gallery: Wire gallery picker for YOLO batch scan (currently only UI shell)

### Quick Tasks Completed

| # | Description | Date | Directory |
|---|-------------|------|-----------|
| 260418-gallery-picker | Add gallery photo picker to VisionScan + YoloScan, fix "Receipt Scan" label | 2026-04-18 | See git log |
| 260418-nav-fix | Fix AI Vision Scan button not responding after returning | 2026-04-18 | See git log |
| 260418-shelf-life | Shelf Life DB + Auto-Learn (Room v12, 200 seed entries, management UI) | 2026-04-18 | `docs/superpowers/plans/2026-04-18-shelf-life-auto-learn.md` |
| 260418-food-images | Add 99 food images + FoodImageResolver + update all adapters/UI | 2026-04-18 | See git log |
| 260418-lmstudio | Add LM Studio dual-provider support + Ollama think:false + debug logging | 2026-04-18 | See git log |
| 260415-ollama | Migrate from local MNN inference to remote Ollama API server | 2026-04-15 | [ollama-remote-migration](./debug/ollama-remote-migration.md) |

## Session Continuity

**Last session:** 2026-04-18T18:00:00.000Z

- **Shelf Life Database + Auto-Learn System**
  - Created `ShelfLifeEntity` + `ShelfLifeDao` with CRUD, search, hit count tracking
  - Created `ShelfLifeRepository` interface + impl with lookup cascade (exact → contains match)
  - Created `SeedData.kt` merging DefaultAttributeEngine + ShelfLifeEstimator (~200 entries, EN+ZH)
  - Created `LookupShelfLifeUseCase`: DB lookup → LLM fallback → default 7 days
  - Added shelf life management UI: list with filter chips (All/AI Learned/Confirmed), search, FAB add/edit/delete
  - Added "AI 預估" badge on confirmation screen for auto-learned items
  - Room migration v11→v12: creates `shelf_life_entries` table + adds `shelfLifeDays`/`shelfLifeSource` to `detection_results`
  - Wired LLM `shelfLifeDays` through detection pipeline to auto-learn new entries
  - Navigation entry from Profile → Shelf Life Database
  - Commit: `3a0e6ef`

- **Gallery Photo Picker for AI Vision Scan**
  - Fixed label: "Receipt Scan" → "AI Vision Scan" on inventory card
  - Added gallery thumbnail button (56x56 MaterialCardView) to VisionScanFragment layout, left of shutter button
  - Loads latest photo from MediaStore as thumbnail preview on view creation
  - Uses `PickVisualMedia` (no permissions needed) to let user pick any image
  - Gallery images sent as full image to LLM; camera captures still cropped to focus rectangle
  - Wired existing `gallery_container` in YoloScanFragment to `PickVisualMedia` + latest thumbnail
  - Removed hardcoded "4" badge from YOLO scan gallery thumbnail
  - Commit: `42c2a6c`

- **Bug Fix: AI Vision Scan Navigation**
  - Root cause: `currentDestination?.getAction()` guard silently failing after ViewPager2/NavHostFragment visibility toggling
  - Fix: Replaced all `currentDestination?.getAction()?.let` patterns with try-catch for robust navigation
  - Commit: `0458977`
  - **Build status:** PASSING ✅

**Previous session:** 2026-04-18T15:00:00.000Z

- **Food Image System (UI Enhancement)**
  - Downloaded 99 high-quality food images (256x256 JPEG) from Lorem Flickr → `app/src/main/res/drawable/food_*.jpg`
  - Created `FoodImageResolver.kt` in `util/` — maps food names to drawable resources with fuzzy matching and category fallback
  - Covers all 34 original food_categories.txt items + 65 new common foods (apple, banana, bread, egg, cheese, chicken, beef, salmon, etc.)
  - Updated `FoodListAdapter.kt` — list items now show real food images via Glide
  - Updated `FoodItemAdapter.kt` — replaced category color bar with 48x48 rounded food image + eaten checkbox overlay
  - Updated `item_food.xml` — new layout with MaterialCardView image container replacing old View indicator
  - Updated `InventoryFragment.kt` — add/edit dialog dynamically updates food preview image based on typed name
  - Updated `AddFoodBottomSheet.kt` — same dynamic image behavior when adding scanned foods
  - **Build status:** PASSING ✅

**Next action:** Phase 8 Plan 03 — unit tests + ViewModel detections + device verification
