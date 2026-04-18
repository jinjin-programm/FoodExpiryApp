# Milestones

## v1.0 MVP (Shipped: 2026-04-08)

**Phases completed:** 3 phases, 11 plans

**Key accomplishments:**
- Unified Food Data Model with single source of truth across Inventory, Recipes, Planner, Shopping
- Photo Scan as primary input with OCR confirmation bottom sheet and barcode lookup
- Expiring Soon section with 7-day risk visibility and tab-aware search
- Recipe scoring with expiry-weighted prioritization and "items rescued" indicators
- Planner recipe picker connected to inventory expiry data for meal planning
- Shopping templates (Weekly Restock, Breakfast Essentials, Fresh Produce) with "In fridge" inventory status
- Privacy & Data section with 5 transparency points
- Google Sign-In marked as optional with local account support
- Notification Settings at top of Profile for immediate food protection access

**UAT:** 14/14 tests passed across all phases
**Timeline:** Feb 10, 2026 → Apr 8, 2026 (~58 days)

## v2.0 AI Vision Engine Overhaul (In Progress)

**Phases completed:** 4/6 (Phases 4-7), 18/25 plans

**Key accomplishments:**
- MNN v3.5.0 replaces llama.cpp as inference runtime
- Qwen3.5-2B-MNN (4-bit quantized) model with dynamic download from HuggingFace
- YOLO+LLM batch detection pipeline with bounding box rendering
- Full scan UI overhaul: NoActionBar theme, horizontal frame, capture animations
- **BREAKTHROUGH (2026-04-10):** First successful on-device food identification via MNN LLM!
  - Fixed missing `libMNNAudio.so` (DT_NEEDED dependency of libllm.so)
  - Fixed prompt engineering: food-specific JSON-only output
  - Fixed StructuredOutputParser: switched to [FOOD]...[/FOOD] tag-based extraction
  - Qwen3.5-2B-MNN correctly identifies banana via vision scan
- **Remote Ollama API (2026-04-15):** Migrated from local MNN to remote qwen3.5:9b
  - LM Studio dual-provider support added
  - Cloudflare Tunnel for public access
- **Shelf Life Database + Auto-Learn (2026-04-18):**
  - Room DB v12 with `shelf_life_entries` table (~200 seed entries, EN+ZH)
  - Lookup cascade: DB exact → DB contains → LLM fallback → default 7 days
  - Auto-learn from LLM output, user confirmation flow
  - Management UI with filter chips (All/AI Learned/Confirmed), search, CRUD
  - "AI 預估" badge on confirmation screen for auto-learned items
- **Gallery Photo Picker (2026-04-18):**
  - VisionScanFragment: gallery thumbnail (latest photo preview) + PickVisualMedia
  - YoloScanFragment: wired existing gallery_container to PickVisualMedia
  - Gallery photos sent as full image; camera captures cropped to focus rectangle
- **Bug Fix (2026-04-18):** AI Vision Scan button not responding after returning
  - Root cause: stale `currentDestination` after ViewPager2/NavHostFragment toggling

**Known issues (in progress):**
- Output format: [FOOD]food name[/FOOD] — simple and reliable
- Performance optimizations applied: early stopping + 8 threads — awaiting test results
- YOLO batch scan gallery feature incomplete (YOLO scan itself still in development)

**Remaining:** Phase 8 (YOLO Hardening), Phase 9 (Verification)
**Timeline:** Apr 8, 2026 → in progress