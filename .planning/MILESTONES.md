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
  - Fixed StructuredOutputParser: robust JSON extraction from thinking output
  - Qwen3.5-2B-MNN correctly identifies banana via vision scan

**Known issues (in progress):**
- Inference time ~30-40s per scan (model generates thinking before JSON) — acceptable for now
- StructuredOutputParser rewritten to handle model's chain-of-thought + JSON output format

**Remaining:** Phase 8 (YOLO Hardening), Phase 9 (Verification)
**Timeline:** Apr 8, 2026 → in progress