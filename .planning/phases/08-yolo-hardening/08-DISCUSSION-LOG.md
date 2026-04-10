# Phase 8: YOLO Detection Hardening - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-11
**Phase:** 08-yolo-hardening
**Areas discussed:** YOLO Model + Native Bridge, Bounding Box Rendering, Code Hardening & Safety, End-to-End Testing Approach

---

## YOLO Model + Native Bridge

| Option | Description | Selected |
|--------|-------------|----------|
| Food YOLO from reference repo | Use food-specific YOLO from YOLO_CLIP_targetDetection, convert to MNN | ✓ |
| Standard YOLOv5/v8-nano + MNN | Broader COCO detection, not food-specific | |
| Agent researches best option | Researcher investigates models and compatibility | |

**User's choice:** Food YOLO from reference repo. Also interested in YOLOv11 (YOLO26) if feasible.
**Notes:** Model should be converted to MNN format, bundled as `yolo_food.mnn` in assets (~5-20MB).

| Option | Description | Selected |
|--------|-------------|----------|
| Custom JNI bridge | New MnnYoloNative following MnnLlmNative pattern | ✓ |
| MNN Java API (MNNNetInstance) | Use existing AAR Java API directly | |
| Agent researches | Researcher determines best approach | |

**User's choice:** Custom JNI bridge — full control, consistent with existing LLM pattern.

| Option | Description | Selected |
|--------|-------------|----------|
| Keep existing postprocessor format | 6+ values per row (x1,y1,x2,y2,conf,class_id) | ✓ |
| Adapt postprocessor to model | Flexible but requires understanding output tensors | |

**User's choice:** Keep existing format — MnnYoloPostprocessor already handles it, researcher just needs matching MNN conversion.

---

## Bounding Box Rendering

| Option | Description | Selected |
|--------|-------------|----------|
| Show detections during YOLO stage | Wire overlay immediately after detection, before LLM | ✓ |
| Show only after LLM complete | Less visual noise, slower feedback | |
| Two-phase: YOLO boxes → LLM labels | Show raw detections, then update with labels | |

**User's choice:** Show detections during YOLO stage with two-phase visual states (enhanced from option 1).
**Notes:** User provided detailed visual design:
- DETECTED: white/light gray border (indicating "awaiting identification")
- CLASSIFIED: green solid border + food name label
- Use DetectionResult.status field (PENDING/CLASSIFIED/FAILED) to drive visual state
- Simplified to color changes only (no dashed animation)

---

## Code Hardening & Safety

| Option | Description | Selected |
|--------|-------------|----------|
| Fix all issues | All 5 identified issues (bitmap, error, colors, cancel) | ✓ |
| Critical fixes only | Bitmap safety + error reporting, defer colors | |

**User's choice:** Fix all issues.
**Notes:** Five specific issues identified:
1. DetectionPipeline:132 — bitmap double-recycle risk
2. YoloScanFragment:289 — missing defensive bitmap copy
3. ConfirmationViewModel:104 — silent error swallowing
4. DetectionResultAdapter — 6 hardcoded Color.parseColor() calls
5. cancelDetection — verify propagation to pipeline coroutine

---

## End-to-End Testing Approach

| Option | Description | Selected |
|--------|-------------|----------|
| Manual on-device testing | Real device, real camera, real Logcat | ✓ (Phase 1) |
| Unit tests + mocks | Automated tests with mocked MNN engine | ✓ (Phase 3) |
| Agent researches test strategy | Researcher designs testing approach | ✓ (Phase 2 for fixes) |

**User's choice:** Three-phase strategy:
1. Manual device testing first (plug in phone, run YOLO, capture Logcat)
2. Agent fixes for compilation/linking errors
3. Unit tests for MnnYoloPostprocessor (pure functions only)

| Option | Description | Selected |
|--------|-------------|----------|
| Single device testing | Test on SD855 only | |
| Multi-device testing | Test on both available devices | ✓ |

**User's choice:** Test on two Android devices.

| Option | Description | Selected |
|--------|-------------|----------|
| Postprocessor tests only | parseDetections, applyNms, cropDetection, mapLabelToCategory | ✓ |
| Broader test coverage | Also mock engines and test pipeline/viewmodel | |

**User's choice:** MnnYoloPostprocessor tests only — pure functions, no native dependencies.

---

## Agent's Discretion

- YOLO model variant selection (reference vs YOLOv11 based on MNN conversion feasibility)
- JNI bridge implementation details
- Exact color shades for bounding box states
- DetectionOverlayView redraw strategy
- Bitmap lifecycle management approach
- Test file structure and naming

## Deferred Ideas

None — discussion stayed within phase scope
