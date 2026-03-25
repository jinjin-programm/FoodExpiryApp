[README.md](https://github.com/user-attachments/files/25194604/README.md)
# 🥗 FreshAlert — Food Expiry Date Reminder

**Never waste food again.** FreshAlert tracks your food items, sends smart expiry notifications, suggests recipes using expiring ingredients, monitors nutrition, and scans for allergens with RED/GREEN safety alerts.

---

## Architecture

**Clean Architecture + MVVM** with unidirectional data flow:

```
┌──────────────────────────────────────────────────────────┐
│  PRESENTATION LAYER                                      │
│  ┌──────────┐  ┌────────────┐  ┌──────────────────────┐ │
│  │ Fragments │→ │ ViewModels │→ │ UI State (StateFlow) │ │
│  └──────────┘  └────────────┘  └──────────────────────┘ │
│        ↕              ↕                                  │
├──────────────────────────────────────────────────────────┤
│  DOMAIN LAYER (pure Kotlin — no Android dependencies)    │
│  ┌──────────┐  ┌──────────────────┐                      │
│  │ Use Cases │→ │ Repository Ifaces │                     │
│  └──────────┘  └──────────────────┘                      │
│  ┌──────────────────────────────────┐                    │
│  │ Domain Models (FoodItem, Recipe…)│                     │
│  └──────────────────────────────────┘                    │
├──────────────────────────────────────────────────────────┤
│  DATA LAYER                                              │
│  ┌──────────────┐  ┌─────────────────┐  ┌────────────┐  │
│  │ Room Database │  │ Retrofit APIs   │  │ Mappers    │  │
│  │ (Entities/DAO)│  │ (DTOs/Services) │  │ Entity↔Dom │  │
│  └──────────────┘  └─────────────────┘  └────────────┘  │
│  ┌──────────────────────────────────────┐                │
│  │ Repository Implementations           │                │
│  └──────────────────────────────────────┘                │
├──────────────────────────────────────────────────────────┤
│  DI LAYER (Hilt)                                         │
│  DatabaseModule │ NetworkModule │ RepositoryModule        │
└──────────────────────────────────────────────────────────┘
```

---

## Project Structure

```
app/src/main/java/com/freshalert/
├── FreshAlertApp.kt                    # Application class (Hilt + WorkManager)
│
├── data/                               # DATA LAYER
│   ├── local/
│   │   ├── dao/Daos.kt                 # Room DAOs (6 DAOs)
│   │   ├── database/
│   │   │   ├── Entities.kt             # Room entities (6 tables)
│   │   │   └── FreshAlertDatabase.kt   # Room database class
│   │   └── converter/                  # Type converters (if needed)
│   ├── remote/
│   │   ├── api/ApiServices.kt          # 5 Retrofit API interfaces
│   │   ├── dto/ApiDtos.kt              # All API response DTOs
│   │   └── interceptor/Interceptors.kt # OkHttp auth interceptors
│   ├── repository/
│   │   └── FoodRepositoryImpl.kt       # Repository implementation
│   └── mapper/Mappers.kt              # Entity ↔ Domain mappers
│
├── domain/                             # DOMAIN LAYER
│   ├── model/
│   │   ├── FoodItem.kt                 # Food item + enums
│   │   ├── NutritionInfo.kt            # Nutrition + daily log
│   │   ├── Allergen.kt                 # Allergen types + scan result
│   │   ├── UserProfile.kt              # User profile + preferences
│   │   ├── Recipe.kt                   # Recipe model
│   │   └── Resource.kt                 # Result wrapper (Success/Error/Loading)
│   ├── repository/
│   │   ├── FoodRepository.kt           # Food CRUD interface
│   │   ├── RecipeRepository.kt         # Recipe search interface
│   │   ├── NutritionRepository.kt      # Nutrition lookup interface
│   │   ├── UserRepository.kt           # User profile interface
│   │   └── ScannerRepository.kt        # AI scanning interface
│   └── usecase/
│       ├── food/FoodUseCases.kt        # Add, get, search, consume, discard
│       ├── recipe/RecipeUseCases.kt    # Suggest recipes for expiring items
│       ├── allergen/ScanForAllergensUseCase.kt  # RED/GREEN alert scanning
│       ├── nutrition/                  # Nutrition tracking use cases
│       └── notification/               # Notification scheduling use cases
│
├── presentation/                       # PRESENTATION LAYER
│   ├── ui/
│   │   ├── home/MainActivity.kt       # Main activity + bottom nav
│   │   ├── home/HomeFragment.kt       # Dashboard
│   │   ├── scanner/                    # Camera/barcode scanning
│   │   ├── foodlist/                   # Pantry list + add/edit/detail
│   │   ├── recipe/                     # Recipe suggestions + detail
│   │   ├── nutrition/                  # Nutrition dashboard + log
│   │   ├── allergen/                   # Allergen settings + scanner
│   │   ├── settings/                   # App settings
│   │   └── profile/                    # User profile management
│   ├── viewmodel/HomeViewModel.kt      # Main dashboard ViewModel
│   ├── adapter/                        # RecyclerView adapters
│   └── common/                         # Base classes, shared UI components
│
├── di/                                 # DEPENDENCY INJECTION
│   ├── DatabaseModule.kt              # Room database + DAOs
│   ├── NetworkModule.kt               # 5 Retrofit instances + OkHttp clients
│   └── RepositoryModule.kt            # Interface → Implementation bindings
│
├── worker/                             # BACKGROUND WORK
│   ├── ExpiryCheckWorker.kt           # Periodic expiry notification worker
│   └── BootReceiver.kt                # Reschedule after device reboot
│
└── util/
    ├── constants/Constants.kt          # Notification channels, prefs keys
    └── extensions/Extensions.kt        # View, Date, String extensions
```

---

## External APIs

| API | Purpose | Auth Method |
|-----|---------|-------------|
| **Spoonacular (RapidAPI)** | Recipe search, ingredient lookup, barcode scan | `X-RapidAPI-Key` header |
| **TheMealDB** | Free recipe database with categories/areas | None (free tier, planned integration) |
| **API Ninjas** | Quick nutrition lookup by food name | `X-Api-Key` header |
| **USDA FoodData Central** | Comprehensive USDA nutrition database | `api_key` query param |
| **OpenAI Vision** | AI food identification from photos | `Bearer` token |
| **ML Kit (on-device)** | Text recognition (OCR) + barcode scanning | Bundled (no API key) |

---

## Setup Instructions

1. **Clone and open** in Android Studio (Hedgehog or newer)

2. **Add API keys** to `local.properties` (never commit this file):
   ```properties
   RAPIDAPI_KEY=your_key_here
   GOOGLE_VISION_API_KEY=your_key_here
   OPENAI_API_KEY=your_key_here
   API_NINJAS_KEY=your_key_here
   FOODDATA_CENTRAL_KEY=your_key_here
   ```

3. **Sync Gradle** and build

4. **Add placeholder drawable icons** for bottom navigation:
   - `ic_home.xml`, `ic_pantry.xml`, `ic_scan.xml`, `ic_recipe.xml`, `ic_profile.xml`

5. **Create Fragment classes** for each navigation destination (stubs provided in nav_graph)

---

## TheMealDB Integration Plan

Use TheMealDB as an optional online recipe source to expand search results while keeping the app fast and offline-friendly.

1. Keep local `assets/recipes.json` as the default offline recipe source.
2. Add TheMealDB search by meal name and ingredient.
3. Map TheMealDB meals into the existing `Recipe` model.
4. Cache fetched recipes in Room so repeat loads stay smooth.
5. Merge local and remote recipes in the Recipes and Planner screens.
6. Rank suggestions using expiring inventory items.
7. Fall back to local recipes if the network is unavailable.

---

## Key Design Decisions

- **Room over SQLite**: Type-safe queries, Flow support, compile-time verification
- **Hilt over manual DI**: Compile-time safety, scoped lifecycle, WorkManager integration
- **StateFlow over LiveData**: Kotlin-first, null-safe, better coroutine integration
- **Separate OkHttp clients per API**: Each API has different auth headers and timeouts
- **Domain layer has zero Android dependencies**: Pure Kotlin, fully unit-testable
- **WorkManager for notifications**: Survives process death, battery-optimized, Doze-aware

---

## Target User Adaptations

| User Type | Key Features |
|-----------|-------------|
| **Fitness Enthusiast** | Detailed nutrition tracking, macro goals, calorie logging |
| **Family Chef** | Recipe suggestions, household size scaling, batch operations |
| **New Parent** | Baby food category, strict allergen scanning, safety-first alerts |
| **Chronically Ill** | Health condition filters, sodium/sugar tracking, dietary restrictions |
