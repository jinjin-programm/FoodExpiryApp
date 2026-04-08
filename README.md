# FoodExpiryApp

FoodExpiryApp is an Android app for tracking food inventory, managing expiry dates, reducing food waste, and turning available ingredients into useful meals.

## Release Version

- `v1.0 MVP` shipped on `2026-04-08`
- Current repo includes the shipped app, planning docs, and cleanup/model work in progress

## Architecture

FoodExpiryApp uses Clean Architecture with MVVM and a layered flow:

```text
UI Fragment / Activity
    -> ViewModel
    -> Use Case
    -> Repository Interface
    -> Repository Implementation
    -> Data Source (Room / API / Assets / JNI)
```

- `presentation` handles screens, adapters, and UI state
- `domain` contains models, repository contracts, and business rules
- `data` handles Room, remote APIs, mappers, and repository implementations
- `di` wires dependencies with Hilt
- `worker` handles expiry notification jobs
- `receiver` restores scheduled work after device reboot
- `util` provides shared helpers and estimators

## Project Structure

```text
FoodExpiryApp/
├── app/
│   └── src/main/
│       ├── java/com/example/foodexpiryapp/
│       │   ├── MainActivity.kt                  # App entry activity with bottom navigation
│       │   ├── FoodExpiryApp.kt                # Application class
│       │   │
│       │   ├── data/                           # Data layer
│       │   │   ├── local/
│       │   │   │   ├── database/               # Room entities and database classes
│       │   │   │   └── dao/                    # Data access objects
│       │   │   ├── remote/                     # API services and DTOs
│       │   │   ├── repository/                 # Repository implementations
│       │   │   ├── mapper/                     # Entity <-> domain mapping
│       │   │   └── analytics/                  # Event tracking and stats services
│       │   │
│       │   ├── domain/                         # Domain layer
│       │   │   ├── model/                      # Core business models
│       │   │   ├── repository/                 # Repository interfaces
│       │   │   ├── usecase/                    # Business logic use cases
│       │   │   └── vision/                     # Domain-level ML / classifier logic
│       │   │
│       │   ├── presentation/                   # UI layer
│       │   │   ├── ui/
│       │   │   │   ├── inventory/              # Inventory screens and add-food flow
│       │   │   │   ├── shopping/               # Shopping list screens
│       │   │   │   ├── recipes/                # Recipe list and recipe detail screens
│       │   │   │   ├── profile/                # Profile and settings screens
│       │   │   │   ├── scan/                   # Barcode / camera scan flow
│       │   │   │   ├── yolo/                   # YOLO object detection scan flow
│       │   │   │   ├── llm/                    # Local LLM vision/chat flow
│       │   │   │   ├── vision/                 # ML Kit vision scan flow
│       │   │   │   └── chat/                   # Chat interface with local model
│       │   │   ├── viewmodel/                  # UI state and event handling
│       │   │   ├── adapter/                    # RecyclerView adapters and list binders
│       │   │   └── util/                       # UI helpers and setup utilities
│       │   │
│       │   ├── di/                             # Hilt modules
│       │   ├── worker/                         # WorkManager background jobs
│       │   ├── receiver/                       # Boot recovery receiver
│       │   └── util/                           # Shared utilities and helpers
│       │
│       └── res/
│           ├── layout/                         # Screens, dialogs, cards, list items
│           ├── menu/                           # Bottom nav and quick actions
│           ├── navigation/                     # Navigation graph
│           ├── drawable/                       # Icons, shapes, vector assets
│           └── values/                         # Strings, colors, themes, styles
│
├── docs/                                       # Technical documentation
├── .planning/                                  # Roadmap, milestones, verification, project state
├── README.md                                   # Main project overview
└── build.gradle.kts                            # Root build configuration
```

## External APIs

- **OpenFoodFacts API** - barcode lookup and product data
- **TheMealDB API** - recipe search and meal data
- **Google Sign-In** - optional profile authentication flow
- **Google ML Kit** - OCR, text recognition, and on-device scanning support

## Key Design Decisions

- **Clean Architecture** keeps UI, business logic, and data access separated
- **MVVM + StateFlow** gives the UI reactive and testable state handling
- **Room** is used for local inventory, recipe, shopping, and analytics storage
- **Hilt** simplifies dependency wiring and module boundaries
- **WorkManager** is used for reliable expiry notifications and reboot recovery
- **Local model support** keeps vision/LLM features usable offline when needed

## Target User Adaptations

- **Busy households** - quick item entry, expiry tracking, and shopping templates
- **Home cooks** - recipe suggestions based on ingredients about to expire
- **Health-conscious users** - profile settings, dietary preferences, and planned nutrition work
- **Safety-first users** - scan flows, expiry alerts, and clear status handling

## Setup

1. Open the project in Android Studio.
2. Sync Gradle.
3. Add required keys to `local.properties` if you use networked features.
4. Run on an emulator or device.

## Notes

- Keep `local.properties` out of version control.
- Large model files, training outputs, and temporary build files should stay ignored or archived.
- See `.planning/PROJECT.md` for the current project definition and scope.
