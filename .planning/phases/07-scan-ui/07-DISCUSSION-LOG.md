# Phase 7: Scan UI Overhaul - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-09
**Phase:** 07-scan-ui
**Areas discussed:** Title bar removal scope, Capture frame redesign, Capture animation feel, Barcode scan redesign

---

## Title bar removal scope

| Option | Description | Selected |
|--------|-------------|----------|
| NoActionBar theme | Change theme globally, every fragment manages its own toolbar | ✓ |
| Per-screen hide | Keep DarkActionBar, hide on scan screens via supportActionBar?.hide() | |

**User's choice:** NoActionBar theme
**Notes:** User chose global theme change. Main tabs (Inventory, Shopping, etc.) get no toolbar at all — just bottom nav. Detail screens manage their own. Floating back arrow on all scan screens replaces existing Close buttons.

---

## Capture frame redesign

| Option | Description | Selected |
|--------|-------------|----------|
| All three modes horizontal | All scan modes get horizontal frame | ✓ |
| YOLO + Photo only | Barcode keeps different shape | |

**Frame aspect ratio:**

| Option | Description | Selected |
|--------|-------------|----------|
| 16:9 | Widescreen, good for multi-item | ✓ |
| 4:3 | More balanced | |
| You decide | Planner chooses | |

**Frame visual style:**

| Option | Description | Selected |
|--------|-------------|----------|
| Rounded rectangle (existing) | Keep current style, just change dimensions | |
| Corner brackets only | L-shaped corners, no full border | |
| No frame (full bleed) | No visible frame, full-bleed camera | ✓ |

**User's choice:** Full-bleed camera with no visible frame on any scan mode. 16:9 aspect ratio was selected initially but overridden by full-bleed decision — no frame at all means no aspect ratio constraint on UI. Note: YOLO DetectionOverlayView (bounding boxes during detection) stays — only static framing guides are removed.

---

## Capture animation feel

| Option | Description | Selected |
|--------|-------------|----------|
| White screen flash | Quick 100-150ms white overlay | ✓ |
| Preview flash | Flash camera preview to white | |
| No flash, just freeze | Minimal | |

**Freeze + overlay transition:**

| Option | Description | Selected |
|--------|-------------|----------|
| Freeze then overlay fades in | Flash → freeze (300ms) → semi-transparent overlay on frozen frame | ✓ |
| Flash then full overlay | Flash → immediate full-screen progress | |

**Scope:**

| Option | Description | Selected |
|--------|-------------|----------|
| Flash on all, progress on AI modes | All modes get flash. Photo + YOLO get progress stages. Barcode gets result card. | ✓ |
| YOLO only | Only YOLO gets flash | |

**User's choice:** White screen flash on all three modes. AI modes (Photo, YOLO) get staged progress overlay. Barcode gets result card after detection.

---

## Barcode scan redesign

**Capture approach:**

| Option | Description | Selected |
|--------|-------------|----------|
| Center capture button | User taps to capture, barcode runs on that frame | ✓ |
| Keep auto-scan | Keep auto-detect, just remove flash/close buttons | |

**Result flow:**

| Option | Description | Selected |
|--------|-------------|----------|
| Show result, auto-navigate | Brief toast then auto-navigate | |
| Show result, manual confirm | Result card with Confirm button | ✓ |

**Layout:**

| Option | Description | Selected |
|--------|-------------|----------|
| Clean minimal | Remove Flash/Close FABs, add floating back arrow + center capture button | ✓ |
| Something else | Custom layout | |

**User's choice:** Manual capture with center button, result shown in card with manual confirm. Clean minimal layout — floating back arrow, full-bleed camera, center capture button.

---

## Agent's Discretion

- White flash animation implementation (View animation, ObjectAnimator, or Lottie)
- Freeze frame implementation details
- Semi-transparent overlay fade-in timing and opacity
- Barcode result card layout specifics
- Capture button exact size and visual treatment
- Floating back arrow exact styling
- Status bar color handling on scan screens
- DetectionOverlayView bounding box behavior during full-bleed

## Deferred Ideas

None — discussion stayed within phase scope
