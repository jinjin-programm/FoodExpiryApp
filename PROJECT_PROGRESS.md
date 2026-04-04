# FoodExpiryApp Project Structure & Progress

## Current Workflow

```text
UI Fragment / Activity
    -> ViewModel
    -> Use Case
    -> Repository Interface
    -> Repository Impl
    -> Data Source (Room / API / Assets / JNI)
```

## Main Project Structure

```text
app/src/main/java/com/example/foodexpiryapp/
├── MainActivity.kt
├── FoodExpiryApp.kt
├── data/
│   ├── local/
│   ├── remote/
│   ├── repository/
│   ├── mapper/
│   └── analytics/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── presentation/
│   ├── ui/
│   ├── viewmodel/
│   ├── adapter/
│   └── util/
├── di/
├── worker/
├── receiver/
└── util/
```

## What Each Part Does

- `presentation/` contains the screens, adapters, and UI state handling.
- `domain/` contains the core models, repository contracts, and business rules.
- `data/` contains Room, remote API, mappers, analytics storage, and repository implementations.
- `di/` wires dependencies with Hilt.
- `worker/` handles background expiry notifications.
- `receiver/` restores scheduled work after reboot.
- `app/src/main/jni/` connects the native LLM bridge.
- `app/src/main/assets/` stores bundled model files and labels.
- `app/src/main/res/` stores layouts, drawables, menus, navigation, and theme values.
- `app/src/test/` and `app/src/androidTest/` hold unit and instrumented tests.

## Progress Snapshot

### Done

- App foundation and navigation structure
- Clean Architecture + MVVM layering
- Profile, inventory, shopping, recipe, planner, and scan screens
- Local LLM / vision scan support
- YOLO model integration
- Analytics tracking and swipe navigation
- Expiry notification foundation

### In Progress

- UI polishing and screen refinement
- Feature stabilization across scan / chat / inventory flows
- Test coverage expansion
- TheMealDB integration plan (local-first, cached online recipes)

### Next

- Finalize repository cleanup and code consistency
- Improve error handling and edge cases
- Verify notifications, analytics, and model loading on device
- Add TheMealDB integration after docs/plan approval

## Git Timeline

- 2026-03-06: native LLM inference, crash fixes, and scan stability work
- 2026-03-07: Qwen3-VL multimodal vision support
- 2026-03-10: model loading fix and filename / mmproj correction
- 2026-03-11: speed-dial UI update
- 2026-03-17: initial project baseline with LLM support and notifications
- 2026-03-19: analytics system, mark-as-eaten flow, and swipe navigation
- 2026-04-02: Fix Navigation crash (IllegalArgumentException) in InventoryFragment when repeatedly clicking scan actions. Added findNavController().currentDestination?.getAction guard.
- 2026-04-02: Fix camera ERROR_MAX_CAMERAS_IN_USE issue. Added cameraProvider?.unbindAll() to onDestroyView across all scanner fragments (VisionScanFragment, ScanFragment, YoloScanFragment).
- 2026-04-02: Redesigned barcode scan UI (`fragment_scan.xml`) for a cleaner card-based look.
- 2026-04-02: Fixed YOLO scan UI constraints (`fragment_yolo_scan.xml`) by breaking the vertical constraint chain. Moved top controls just below the status bar (`24dp` margin), and adjusted the scanning frame's bias (`0.2`) to pull the frame and its attached instruction text cleanly upward, ensuring no overlap with bottom camera controls. Updated YOLO instruction text color to a vibrant red for better visibility.
- 2026-04-05: Reverted LLM inference model to GGUF (Qwen3-VL-2B-Instruct) from MNN due to previous experiments. Addressed "thinking mode" leakage by enforcing strict ChatML prompt (`<|im_start|>system...`) to suppress internal reasoning output and guarantee direct structured answers (`FOOD: xxx \n EXPIRY: xxx`).

## Short Version For Team

The app is organized by Clean Architecture: UI in `presentation/`, business rules in `domain/`, and storage/API/native integration in `data/`. The current work is centered on scan/LLM features, analytics, and keeping the inventory and notification flows stable.

