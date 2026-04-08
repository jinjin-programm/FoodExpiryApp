---
phase: "05"
plan: "01"
subsystem: "domain-layer"
tags: ["domain-model", "repository-interface", "clean-architecture", "mnn"]
dependency_graph:
  requires: []
  provides: ["LlmInferenceRepository interface", "FoodIdentification model", "ModelState sealed class"]
  affects: ["05-02 (engine implementation)", "06-01 (YOLO+LLM pipeline)", "07-01 (scan UI)"]
tech_stack:
  added: []
  patterns: ["Flow-based reactive streams", "sealed class state machine"]
key_files:
  created:
    - "app/src/main/java/com/example/foodexpiryapp/domain/model/FoodIdentification.kt"
    - "app/src/main/java/com/example/foodexpiryapp/domain/model/ModelState.kt"
    - "app/src/main/java/com/example/foodexpiryapp/domain/repository/LlmInferenceRepository.kt"
  modified: []
decisions: []
metrics:
  duration_seconds: 36
  completed_date: "2026-04-09"
---

# Phase 05 Plan 01: Domain Layer Contracts Summary

**One-liner:** Pure Kotlin domain interface for LLM food identification — FoodIdentification output model, ModelState lifecycle sealed class, and LlmInferenceRepository with single-method inference API (per D-01/D-02/MNN-04).

## What Was Built

Three domain-layer files establishing the contract boundary between presentation (ViewModels) and data (MNN engine implementation):

1. **FoodIdentification.kt** — Structured output data class with `name`, `nameZh`, `confidence`, `expiryHint`, and `rawResponse` fields. Pure Kotlin, no Android dependencies. Per MNN-04 requirement for structured JSON output from LLM.

2. **ModelState.kt** — Sealed class with 7 states covering the full model lifecycle: `NotDownloaded`, `Downloading(progress, file)`, `Verifying(file)`, `WifiCheckRequired(size)`, `Loading`, `Ready`, `Error(message)`. Rich state objects carry data needed by UI (progress percentage, file names, error messages).

3. **LlmInferenceRepository.kt** — Domain interface with `inferFood(Bitmap): Flow<FoodIdentification>` as the single inference method (D-02 constraint) and `modelState: StateFlow<ModelState>` as a reactive property for UI status observation. No `isModelReady()` or `getModelStatus()` methods — status flows reactively through the StateFlow.

## Decisions Made

None — plan executed exactly as designed. Architecture decisions (D-01/D-02) were made during research phase.

## Deviations from Plan

None - plan executed exactly as written.

## Verification

- [x] FoodIdentification.kt exists with required fields (name, nameZh, confidence)
- [x] ModelState.kt exists with all 7 lifecycle states
- [x] LlmInferenceRepository.kt exists in domain/repository/
- [x] Interface follows existing repository pattern (Flow-based)
- [x] D-02 constraint satisfied (single inferFood method; status via modelState StateFlow)
- [x] D-03 constraint satisfied (interface in domain/repository/)
- [x] KDoc references MNN-04, D-01, D-02

## Known Stubs

None — these are pure interface/model contracts with no implementation stubs.

## Threat Surface

No new threat surface introduced. Per threat register T-05-02: implementation (not this interface) will validate bitmap input and handle malformed JSON. The domain interface is a pure contract boundary.

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| 1 | `6f5c25d` | feat(05-01): create FoodIdentification domain model |
| 2 | `cf1a4c3` | feat(05-01): create LlmInferenceRepository interface and ModelState |

## Self-Check: PASSED
