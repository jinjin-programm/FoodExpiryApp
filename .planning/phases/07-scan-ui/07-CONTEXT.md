# Phase 7: Scan UI Overhaul - Context

**Gathered:** 2026-04-09
**Status:** Ready for planning

<domain>
## Phase Boundary

Scan screens have a cleaner, more modern look with no top title bar, horizontal capture frame, and satisfying capture-to-result flow. This covers all three scan modes (Photo Scan, Barcode Scan, YOLO Scan) and the main app toolbar. No new features — purely visual and interaction improvements.

</domain>

<decisions>
## Implementation Decisions

### Title Bar Removal
- **D-01:** Switch theme from `DarkActionBar` to `NoActionBar` globally in `themes.xml`. Every fragment manages its own top bar if needed.
- **D-02:** Main tabs (Inventory, Shopping, Recipes, Planner, Profile) get NO toolbar at all — just bottom nav. The app feels more immersive. The "FoodExpiryApp" title never appears anywhere.
- **D-03:** Detail screens (Recipe Detail, Chat, Scan, Confirmation) that need navigation add their own Toolbar. Existing ConfirmationFragment already has its own top bar — no change needed there.
- **D-04:** All three scan screens (Photo, Barcode, YOLO) get a floating back arrow button in the top-left corner with semi-transparent circular background. Replaces existing Close buttons. Same style across all scan modes.

### Capture Frame Redesign
- **D-05:** Remove ALL visual frames/bounding boxes from camera preview. Full-bleed camera — no rounded rectangle, no corner brackets, no scanning line. Users just point and capture.
- **D-06:** Applies to all three scan modes (Photo, Barcode, YOLO). Consistent full-bleed camera experience.
- **D-07:** Note: YOLO's `DetectionOverlayView` (bounding boxes drawn during detection) is SEPARATE from the capture frame guide and should remain — it shows detected items, not a framing guide. Only the static framing guides (`simulated_box`, `frame_card`, `scan_box` MaterialCardViews) are removed.

### Capture Animation
- **D-08:** White screen flash animation on capture for ALL three scan modes. Quick 100-150ms full-screen white overlay, then fade out. Classic camera shutter feel.
- **D-09:** After flash: freeze the camera frame briefly (300ms), then a semi-transparent dark overlay fades in on top of the frozen frame showing progress text. User sees their captured photo behind the progress. Smoother than immediate full-screen replacement.
- **D-10:** Progress stages for AI modes (Photo Scan and YOLO Scan): show staged text like "Detecting food items..." → "Identifying item (1/N)..." → "Done". YOLO already has this via `progressOverlay`.
- **D-11:** Barcode scan: flash animation + freeze, then show detected barcode result in a card (not auto-navigate). No progress stages needed since barcode detection is instant.

### Barcode Scan Redesign
- **D-12:** Switch from auto-scan to manual capture: user taps center capture button (same style as YOLO scan shutter), then barcode detection runs on the captured frame.
- **D-13:** Remove Flash button and Close button FABs. Replace with floating back arrow (top-left, per D-04).
- **D-14:** After barcode detected: show result in a bottom card with barcode value + "Confirm" button. User must tap confirm to save. Consistent with the deliberate confirmation pattern from YOLO flow.
- **D-15:** Layout: floating back arrow (top-left), full-bleed camera (no frame per D-05), center capture button at bottom. Clean and minimal.

### Consistent Bottom Capture Button
- **D-16:** All three scan modes get the same bottom-center capture button style — circular shutter button matching existing YOLO scan design. This addresses UI-08.

### Agent's Discretion
- White flash animation implementation (View animation, ObjectAnimator, or Lottie)
- Freeze frame implementation details (stop camera feed vs show captured bitmap)
- Semi-transparent overlay fade-in timing and opacity
- Barcode result card layout specifics (Material Card style matching existing result cards)
- Capture button exact size and visual treatment
- Floating back arrow exact styling (size, opacity, background tint)
- Status bar color handling on scan screens (transparent? dark?)
- How to handle the YOLO `DetectionOverlayView` bounding boxes during full-bleed camera

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Existing Scan UI Code
- `app/src/main/res/layout/fragment_vision_scan.xml` — Photo scan layout with AppBarLayout, simulated_box (160x240dp vertical frame), capture button
- `app/src/main/res/layout/fragment_scan.xml` — Barcode scan layout with Flash/Close FABs, square scan_box, auto-scan behavior
- `app/src/main/res/layout/fragment_yolo_scan.xml` — YOLO scan layout with progressOverlay, frame_card (3:4 vertical), capture button
- `app/src/main/res/layout/fragment_scan_container.xml` — ViewPager2 container for all scan modes
- `app/src/main/res/layout/fragment_confirmation.xml` — ConfirmationFragment layout (already has its own toolbar)

### Kotlin Source
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/scan/ScanFragment.kt` — Barcode scan with auto-scan (BarcodeAnalyzer, TextAnalyzer)
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/vision/VisionScanFragment.kt` — Photo scan with capture, crop, AI inference
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/yolo/YoloScanFragment.kt` — YOLO scan with staged progress overlay
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/yolo/DetectionOverlayView.kt` — Bounding box overlay (KEEP for detection, only remove static frames)
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/scan/ScanContainerFragment.kt` — ViewPager2 container
- `app/src/main/java/com/example/foodexpiryapp/presentation/ui/scan/ScanPagerAdapter.kt` — Tab adapter

### Theme & Navigation
- `app/src/main/res/values/themes.xml` — Current DarkActionBar theme (change to NoActionBar)
- `app/src/main/res/navigation/nav_graph.xml` — Navigation graph with all scan destinations
- `app/src/main/java/com/example/foodexpiryapp/MainActivity.kt` — Main activity (may need toolbar handling changes)

### Architecture References
- `.planning/codebase/ARCHITECTURE.md` — Clean Architecture layers, data flow patterns
- `.planning/codebase/STRUCTURE.md` — Directory layout, naming conventions
- `.planning/codebase/CONVENTIONS.md` — Coding patterns, Hilt usage

### Requirements
- `.planning/REQUIREMENTS.md` §Scan UI Overhaul (UI-01 through UI-08) — Title bar removal, horizontal frame, capture animation, loading progress, barcode redesign

### Prior Phase Context
- `.planning/phases/06-detection/06-CONTEXT.md` — Phase 6 decisions (ViewPager2 scan container, staged progress, ConfirmationFragment, tab structure)
- `.planning/phases/05-engine/05-CONTEXT.md` — Phase 5 decisions (MNN engine patterns)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `YoloScanFragment.btn_capture` + `btn_shutter_container`: Existing circular capture button with ring design — reuse this pattern for all scan modes
- `YoloScanFragment.progressOverlay`: Existing staged progress overlay with ProgressBar, title, detail, cancel button — reuse for Photo scan too
- `ScanContainerFragment` + `ScanPagerAdapter`: ViewPager2 container already works — no structural change needed
- `DetectionOverlayView`: Bounding box overlay for YOLO — keep for detection results, only remove static framing guides
- `ConfirmationFragment`: Already has its own toolbar with back + title + count — no change needed

### Established Patterns
- CameraX with PreviewView + ImageAnalysis: same pattern in all three scan fragments
- FragmentResultListener for cross-fragment communication
- Navigation Component with Safe Args
- MaterialCardView for overlays and result cards
- MaterialButton with rounded corners for capture/action buttons
- ViewBinding in all fragments

### Integration Points
- `themes.xml`: Change `Theme.MaterialComponents.DayNight.DarkActionBar` → `NoActionBar`
- `MainActivity.kt`: May need adjustments since it uses DarkActionBar features
- `fragment_vision_scan.xml`: Remove `AppBarLayout` + `Toolbar`, remove `simulated_box` MaterialCardView, add floating back button
- `fragment_scan.xml`: Remove `scan_box` MaterialCardView, remove Flash/Close FABs, add capture button + floating back button
- `fragment_yolo_scan.xml`: Remove `frame_card` MaterialCardView, remove `top_controls` LinearLayout with mode pills and close button, add floating back button
- `ScanFragment.kt`: Major rewrite — switch from ImageAnalysis auto-scan to capture-button-triggered single-frame analysis
- `VisionScanFragment.kt`: Add flash animation before inference starts, remove simulated_box cropping logic
- `YoloScanFragment.kt`: Add flash animation before detection starts

</code_context>

<specifics>
## Specific Ideas

- Full-bleed camera with no visible frame — users just point and capture. Very clean, modern feel.
- White screen flash on capture (100-150ms) — classic camera shutter satisfaction.
- Freeze frame behind semi-transparent dark overlay during AI processing — user sees what they captured.
- Barcode result card with manual confirm — consistent deliberate pattern.
- No toolbar on main tabs — the app feels more immersive, content-first.
- Floating back arrow on scan screens — semi-transparent, doesn't distract from camera.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 07-scan-ui*
*Context gathered: 2026-04-09*
