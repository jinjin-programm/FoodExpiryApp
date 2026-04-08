# Phase 5: Engine - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-08
**Phase:** 05-engine
**Areas discussed:** LLM API design

---

## LLM API design

| Option | Description | Selected |
|--------|-------------|----------|
| Hilt singleton direct | @Singleton MnnLlmEngine injected directly into ViewModels. Simpler, fewer layers. | |
| UseCase/Repository pattern (Recommended) | Domain interface + data implementation. Engine-agnostic — swap MNN for MediaPipe without touching ViewModels. | ✓ |
| Repository + UseCase | Full 3-layer indirection (Repository interface + impl + UseCase). Maximum Clean Architecture. | |

**User's choice:** UseCase/Repository pattern
**Notes:** User explicitly wants engine-agnostic abstraction. Rationale: "將來如果你由 MNN 轉去 MediaPipe LLM Inference API 或者其他引擎，只需要換 Repository 實現，ViewModel 一行都唔使改" (If you switch from MNN to MediaPipe LLM Inference API or other engine later, just swap the Repository implementation, ViewModels don't need to change at all)

---

## Repository interface location

| Option | Description | Selected |
|--------|-------------|----------|
| Domain interface + data impl (Recommended) | Interface in domain/repository/, implementation in data/repository/. Matches existing FoodItemRepository pattern. | ✓ |
| Data-layer only | Both interface and implementation in data/. Simpler but less clean. | |

**User's choice:** Domain interface + data implementation

---

## API surface

| Option | Description | Selected |
|--------|-------------|----------|
| Single method: inferFood (Recommended) | inferFood(Bitmap): Flow<FoodIdentification>. One method, Repository handles everything internally. | ✓ |
| Multiple granular methods | loadModel(), isModelReady, infer(), parseOutput(). More control but pushes complexity to callers. | |

**User's choice:** Single method inferFood

---

## Agent's Discretion

Model download UX flow, prompt engineering, model lifecycle specifics, structured output parsing, download service implementation — all deferred to agent/researcher/planner discretion.
