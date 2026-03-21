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

### Next

- Finalize repository cleanup and code consistency
- Improve error handling and edge cases
- Verify notifications, analytics, and model loading on device

## Git Timeline

- 2026-03-06: native LLM inference, crash fixes, and scan stability work
- 2026-03-07: Qwen3-VL multimodal vision support
- 2026-03-10: model loading fix and filename / mmproj correction
- 2026-03-11: speed-dial UI update
- 2026-03-17: initial project baseline with LLM support and notifications
- 2026-03-19: analytics system, mark-as-eaten flow, and swipe navigation

## Short Version For Team

The app is organized by Clean Architecture: UI in `presentation/`, business rules in `domain/`, and storage/API/native integration in `data/`. The current work is centered on scan/LLM features, analytics, and keeping the inventory and notification flows stable.
