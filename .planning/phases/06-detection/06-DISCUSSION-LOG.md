# Phase 6: Detection - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-09
**Phase:** 06-detection
**Areas discussed:** YOLO Model Integration, Tab Structure, Batch Processing Flow, Result Confirmation UI, Edge Cases & Error States, Navigation Architecture, Single vs Multi-Item Unification

---

## YOLO Model Integration

| Option | Description | Selected |
|--------|-------------|----------|
| Bundled in APK | YOLO model (~5-20MB) in assets/, no download needed | ✓ |
| Download on first use | Like LLM model, download from HuggingFace | |
| MNN native YOLO | Use MNN's built-in YOLO support if available | |

**User's choice:** Bundled in APK — YOLO is small enough to bundle, faster first-use experience.

| Option | Description | Selected |
|--------|-------------|----------|
| MNN for both | Run YOLO through existing MNN infrastructure, same lifecycle manager | ✓ |
| ONNX Runtime for YOLO | Separate ONNX Runtime for YOLO detection | |
| TFLite for YOLO | TensorFlow Lite for YOLO | |

**User's choice:** MNN for both — reuses existing infrastructure, no additional dependency.

---

## Tab Structure

| Option | Description | Selected |
|--------|-------------|----------|
| Three separate tabs | Photo / Barcode / YOLO tabs in scan screen | |
| Single screen with mode toggle | Mode switch without tab navigation | |
| Separate Fragment entirely | YOLO in completely independent Fragment | |

**User's choice:** Three separate tabs concept, but with ViewPager2 horizontal swipe between modes (no visible tab bar).

---

## Batch Processing Flow

| Option | Description | Selected |
|--------|-------------|----------|
| Staged progress screen | "Detecting..." → "Analyzing 1/5..." → "Done" | |
| Progressive results list | Results appear as they arrive | |
| Simple loading spinner | Loading until all done | |

**User's choice:** Hybrid of staged progress + progressive results. Stage 1: "Detecting food items..." → Stage 2: "Identifying item (1/5)..." with progress → Stage 3: Auto-transition to confirmation screen.

**Note:** User specified all UI text must be English (not Chinese). Chinese used in discussion only.

---

## Result Confirmation UI

| Option | Description | Selected |
|--------|-------------|----------|
| Full-screen list | Scrollable item list, edit/delete per row, bottom confirm bar | ✓ |
| Bottom sheet | Slides up with detected items | |
| Simple dialog | Basic list with Confirm/Cancel | |

**User's choice:** Full-screen list with detailed layout: top bar (back + title + count), optional photo thumbnail (120dp), scrollable items with edit/delete, bottom fixed bar.

**Additional detail:** Smart single-item optimization (auto-selected, "Add to Fridge" button, 2 touches total). DefaultAttributeEngine for auto category/expiry. Save → popBackStack → Snackbar "View" → inventory highlight.

---

## Edge Cases & Error States

| Option | Description | Selected |
|--------|-------------|----------|
| Show placeholder | "Unknown item (tap to edit)" in review list | ✓ |
| Drop failed items | Only show successfully identified items | |
| Pause and retry | Retry per failed item before continuing | |

**User's choice:** Show placeholder with distinct UI: gray italic text, light orange/yellow background, prominent "Fix" button, cropped thumbnail still visible.

**Additional concerns raised:**
- Process death during confirmation: addressed with Room temp table
- Camera resource conflict during tab switch: ModelLifecycleManager must handle model unload + task cancellation

---

## Navigation Architecture

| Option | Description | Selected |
|--------|-------------|----------|
| New Fragment + Navigation Component | ConfirmationFragment in nav graph | ✓ |
| In-place view swap | Swap views within scan fragment | |
| Full-screen DialogFragment | Overlay dialog | |

**Data passing:**

| Option | Description | Selected |
|--------|-------------|----------|
| Safe Args (Parcelable) | Fast but lost on process death | |
| Shared ViewModel | Survives config change, not process death | |
| Room temp table | Survives process death completely | ✓ |

**Post-save navigation:**

| Option | Description | Selected |
|--------|-------------|----------|
| Navigate to inventory | Direct navigation with highlighted items | |
| Stay on scan screen | Clear and show camera again | |
| Back to scan + Snackbar | PopBackStack + "View" action Snackbar | ✓ |

---

## Single vs Multi-Item Unification

| Option | Description | Selected |
|--------|-------------|----------|
| Unified: YOLO always runs | Same pipeline for 1 or N items | ✓ |
| Separate: Photo Scan stays as-is | Two independent flows | |
| Hybrid: auto-switch based on count | Branch based on detection count | |

**User's choice:** Unified pipeline with smart single-item optimization. Quick Mode (off by default): 3-second auto-confirm countdown for single items, cancelable by touch.

---

## the agent's Discretion

- YOLO model variant selection and MNN conversion approach
- Bounding box rendering approach
- DefaultAttributeEngine implementation details
- Error threshold for failed LLM results
- Quick Mode countdown configuration

## Deferred Ideas

- Advanced bounding box editing (user adjusts boxes manually)
- Real-time continuous YOLO detection overlay
- Confidence-based auto-retry for low-confidence items
