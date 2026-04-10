---
gsd_state_version: 1.0
milestone: v2.0
milestone_name: AI Vision Engine Overhaul
status: in_progress
last_updated: "2026-04-11T00:00:00.000Z"
progress:
  total_phases: 9
  completed_phases: 7
  total_plans: 25
  completed_plans: 18
  percent: 72
---

# Project State: FoodExpiryApp

**Status:** MNN LLM WORKING — First successful food identification!

## Project Reference

See: .planning/PROJECT.md (updated 2026-04-08)

**Core value:** Let a new user add their first food item within 30 seconds and reduce the mental burden of food management.
**Current focus:** Phase 08 — YOLO Detection Hardening

## Current Position

Phase: 08 (YOLO Detection Hardening) — READY
Plan: 0 of 3

- **Milestone:** v2.0 AI Vision Engine Overhaul
- **Phase:** 08
- **Plan:** Not started
- **Status:** MNN LLM inference confirmed working on device

### v2.0 Progress

```
Phase 4: Foundation       [██████████] 100% ✅
Phase 5: Engine           [██████████] 100% ✅ ← LLM WORKING!
Phase 6: Detection        [██████████] 100% ✅
Phase 7: Scan UI Overhaul [██████████] 100% ✅
Phase 8: YOLO Hardening   [          ] 0%
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
- **BREAKTHROUGH 2026-04-10:** MNN LLM first successful food identification on device!
  - Fixed: missing `libMNNAudio.so` causing UnsatisfiedLinkError crash
  - Fixed: prompt engineering — food-specific JSON-only prompt replaces generic "What is in this image?"
  - Fixed: StructuredOutputParser — robust JSON extraction that skips template placeholders (e.g. `{"name": ..., "category": ...}`)
  - Result: Qwen3.5-2B-MNN correctly identifies "banana" via vision scan, outputs valid JSON

## Accumulated Context

### Key Decisions

| Decision | Rationale | Status |
|----------|-----------|--------|
| MNN Chat official component | Avoid custom inference layer, battle-tested | ✅ Done |
| Qwen3.5-2B-MNN model | 4-bit quantized, lightweight for mobile | ✅ Working |
| YOLO + LLM (skip CLIP) | CLIP overlaps with LLM capability | ✅ Done |
| Dynamic model download from HF | Reduce APK size by ~40MB | ✅ Done |
| Sequential YOLO→LLM processing | Prevent OOM on <6GB devices | ✅ Done |
| Complete llama.cpp removal | libllm.so naming collision | ✅ Done |

### Critical Constraints

- MNN AAR + llama.cpp removal (Phase 4) blocks ALL MNN work
- Model download (DL) must precede inference (MNN-03)
- YOLO pipeline (Phase 6) depends on MNN engine (Phase 5)
- Scan UI (Phase 7) is independent of ML work — can run parallel

### References

- MNN v3.5.0: https://github.com/alibaba/MNN
- Model: taobao-mnn/Qwen3.5-2B-MNN (4-bit quantized)
- MNN Chat Android: apps/Android/MnnLlmChat
- User HuggingFace: jinjin06/Qwen3.5-2B-MNN

### Blockers

- None currently

### TODO

- [ ] Create Phase 8 plans with `/gsd-plan-phase 8`
- [ ] End-to-end YOLO detection hardening
- [ ] Phase 9 verification & artifact cleanup

### Quick Tasks Completed

| # | Description | Date | Directory |
|---|-------------|------|-----------|
| 260410-parser-fix | Rewrite StructuredOutputParser for robust JSON extraction from LLM thinking output | 2026-04-11 | Debug session |
| 260410-mnn-fix | Fix MNN native crash (missing libMNNAudio.so) + food-specific prompt | 2026-04-10 | Debug session |
| 260409-vki | Improve LLM few-shot prompt with visual description examples | 2026-04-09 | [260409-vki-improve-llm-few-shot-prompt-with-visual-](./quick/260409-vki-improve-llm-few-shot-prompt-with-visual-/) |

## Session Continuity

**Last session:** 2026-04-11T00:00:00.000Z

- Rewrote StructuredOutputParser to robustly extract JSON from LLM thinking output
  - Old parser: used fragile regex chain, matched template placeholders like `{"name": ..., "category": ...}`
  - New parser: finds all `{...}` blocks, validates each is real JSON with non-placeholder values, picks first valid
  - max_tokens kept at 512 (not optimizing speed per user request)
- Added diagnostic logging to MnnLlmNative init block for future debugging
- Updated STATE.md, ROADMAP.md progress tracking

**Next action:** `/gsd-plan-phase 8`
