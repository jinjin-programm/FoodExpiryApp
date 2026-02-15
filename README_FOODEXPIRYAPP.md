# 📦 FoodExpiryApp — Never Waste Food Again

**Track food expiration dates, manage your pantry, discover recipes, and reduce food waste.**

A beginner-friendly Android app built from scratch using Clean Architecture + MVVM pattern.

---

## 🎯 About This Project

This is my **first Android app**, built as a learning project to master modern Android development practices including Clean Architecture, MVVM, dependency injection with Hilt, and Material Design.

**Developer:** jinjin-programm  
**Started:** February 2026  
**Status:** 🚧 Active Development (Alpha)

---

## 🚀 Current Progress

### ✅ Completed Features

**Foundation (Phase 1)**
- [x] Project setup with Gradle Kotlin DSL
- [x] Clean Architecture folder structure (18 packages across data, domain, presentation layers)
- [x] Hilt dependency injection configured
- [x] Application class (`FoodExpiryApp.kt`) with `@HiltAndroidApp`
- [x] Material Design 3 theme setup

**Navigation & UI (Phase 2)**
- [x] Bottom navigation bar with 5 tabs
- [x] Navigation Component setup with nav_graph
- [x] 5 Fragment screens created:
  - Inventory (food list)
  - Shopping (shopping list)
  - Recipes (recipe suggestions)
  - Planner (meal planning)
  - Profile (user settings)
- [x] Fragment-based navigation working
- [x] MainActivity with BottomNavigationView

**Domain Layer (Phase 3)**
- [x] `FoodItem` data class with properties:
  - id, name, category, expiryDate, quantity, location
- [x] `FoodCategory` enum (DAIRY, MEAT, VEGETABLES, FRUITS, etc.)
- [x] `StorageLocation` enum (FRIDGE, FREEZER, PANTRY)

**Profile Screen (Phase 4)**
- [x] User input form with Material Design components
- [x] Name and email text inputs
- [x] Household size slider (1-10 people)
- [x] Dietary preference chips (Vegetarian, Vegan, Gluten-Free, etc.)
- [x] Save button with persistence (Jetpack DataStore)
- [x] MVVM architecture with StateFlow
- [x] Interactive UI with real-time updates

### 🚧 In Progress

**Database Layer**
- [ ] Room database setup
- [ ] FoodItem entity and DAO
- [ ] Database migrations
- [ ] Repository implementation

**ViewModels & State Management**
- [ ] Create ViewModels for each screen
- [ ] StateFlow for reactive UI updates
- [ ] Handle user actions and events

### 📋 Planned Features

**Short-term (Next Steps)**
- [ ] RecyclerView adapter for food list
- [ ] Add/Edit/Delete food items
- [ ] Search and filter functionality
- [ ] Sort by expiry date

**Medium-term**
- [x] Camera scanning (barcode/OCR)
- [x] YOLO Object Detection (FoodVision v8)
- [ ] Expiry notifications with WorkManager
- [ ] Recipe API integration
- [ ] Shopping list management
- [ ] Meal planner calendar view

**Long-term**
- [ ] Nutrition tracking
- [ ] Allergen scanning
- [ ] Data export/import
- [ ] Dark theme
- [ ] Widget for home screen

---

## 🏗️ Architecture

**Clean Architecture + MVVM** with clear separation of concerns:

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

## 📂 Project Structure

```
app/src/main/java/com/example/foodexpiryapp/
├── FoodExpiryApp.kt                    # ✅ Application class (@HiltAndroidApp)
├── MainActivity.kt                     # ✅ Main activity with bottom navigation
│
├── data/                               # DATA LAYER
│   ├── local/
│   │   ├── dao/                        # Room DAOs (planned)
│   │   └── database/                   # Room database (planned)
│   ├── remote/
│   │   ├── api/                        # Retrofit APIs (planned)
│   │   └── dto/                        # Data Transfer Objects (planned)
│   ├── repository/                     # Repository implementations (planned)
│   └── mapper/                         # Entity ↔ Domain mappers (planned)
│
├── domain/                             # DOMAIN LAYER
│   ├── model/
│   │   └── FoodItem.kt                 # ✅ Food item data class with enums
│   ├── repository/                     # Repository interfaces (planned)
│   └── usecase/                        # Business logic use cases (planned)
│
├── presentation/                       # PRESENTATION LAYER
│   ├── ui/
│   │   ├── inventory/
│   │   │   └── InventoryFragment.kt    # ✅ Food inventory screen
│   │   ├── shopping/
│   │   │   └── ShoppingFragment.kt     # ✅ Shopping list screen
│   │   ├── recipes/
│   │   │   └── RecipesFragment.kt      # ✅ Recipe suggestions screen
│   │   ├── planner/
│   │   │   └── PlannerFragment.kt      # ✅ Meal planner screen
│   │   └── profile/
│   │       └── ProfileFragment.kt      # ✅ User profile with input form
│   ├── viewmodel/                      # ViewModels (planned)
│   └── adapter/                        # RecyclerView adapters (planned)
│
├── di/                                 # DEPENDENCY INJECTION
│   ├── DatabaseModule.kt              # Hilt database module (planned)
│   ├── NetworkModule.kt               # Hilt network module (planned)
│   └── RepositoryModule.kt            # Hilt repository module (planned)
│
└── util/                              # UTILITIES
    └── extensions/                    # Extension functions (planned)
```

### XML Resources

```
app/src/main/res/
├── layout/
│   ├── activity_main.xml               # ✅ Main activity with bottom nav
│   ├── fragment_inventory.xml          # ✅ Inventory screen layout
│   ├── fragment_shopping.xml           # ✅ Shopping screen layout
│   ├── fragment_recipes.xml            # ✅ Recipes screen layout
│   ├── fragment_planner.xml            # ✅ Planner screen layout
│   └── fragment_profile.xml            # ✅ Profile screen with input form
├── menu/
│   └── bottom_nav_menu.xml             # ✅ Bottom navigation menu items
├── navigation/
│   └── nav_graph.xml                   # ✅ Navigation graph with 5 destinations
└── values/
    └── themes.xml                      # ✅ Material Design theme
```

---

## 🛠️ Tech Stack

### Core
- **Language:** Kotlin
- **Min SDK:** API 26 (Android 8.0 Oreo)
- **Target SDK:** API 34 (Android 14)
- **Build System:** Gradle Kotlin DSL

### Android Jetpack
- **Navigation Component:** Fragment-based navigation ✅
- **Lifecycle:** ViewModel, LiveData (planned)
- **Room:** Local SQLite database (planned)
- **WorkManager:** Background tasks for notifications (planned)

### Dependency Injection
- **Hilt:** Compile-time dependency injection ✅

### UI/UX
- **Material Design 3:** Modern UI components ✅
- **ViewBinding:** Type-safe view access ✅
- **ConstraintLayout:** Flexible layouts ✅
- **Bottom Navigation:** Multi-screen navigation ✅

### Architecture
- **MVVM:** Model-View-ViewModel pattern
- **Clean Architecture:** Separation of concerns
- **Repository Pattern:** Data abstraction (planned)
- **Use Cases:** Business logic encapsulation (planned)

### Async Programming
- **Coroutines:** Asynchronous operations (planned)
- **StateFlow:** Reactive state management (planned)

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Git

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/jinjin-programm/FoodExpiryApp.git
   cd FoodExpiryApp
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - File → Open → Select the `FoodExpiryApp` folder
   - Click OK

3. **Sync Gradle**
   - Android Studio will prompt "Gradle files have changed"
   - Click **"Sync Now"**
   - Wait for dependencies to download (first time may take a few minutes)

4. **Build the project**
   - Build → Make Project (Ctrl+F9)
   - Wait for "BUILD SUCCESSFUL"

5. **Run the app**
   - Click the green ▶️ button (Shift+F10)
   - Select an emulator or connected device
   - Wait for app to install and launch

### Troubleshooting

**If you see "Hilt Activity must be attached to @HiltAndroidApp Application":**
- Make sure `AndroidManifest.xml` has `android:name=".FoodExpiryApp"` in the `<application>` tag

**If build fails:**
- File → Invalidate Caches → Invalidate and Restart
- Then rebuild

---

## 📱 Current Features

### Bottom Navigation
Navigate between 5 main screens:
- **📦 Inventory:** View and manage your food items
- **🛒 Shopping:** Create and manage shopping lists
- **🍳 Recipes:** Discover recipes using your ingredients
- **📅 Planner:** Plan your meals for the week
- **👤 Profile:** Set your preferences and household info

### Profile Screen
Configure your settings:
- **Name & Email:** Basic user information
- **Household Size:** Adjust servings (1-10 people) with slider
- **Dietary Preferences:** Select from:
  - Vegetarian
  - Vegan
  - Gluten Free
  - Dairy Free
- **Save Button:** Stores preferences (currently shows toast confirmation)

---

## 🗺️ Development Roadmap

### ✅ Phase 1: Foundation (Completed)
- [x] Project setup with Gradle
- [x] Clean Architecture structure
- [x] Hilt dependency injection
- [x] Material Design theme

### ✅ Phase 2: Navigation & Basic UI (Completed)
- [x] Bottom navigation implementation
- [x] 5 Fragment screens
- [x] Navigation graph setup
- [x] Profile screen with user input

### ✅ Phase 3: Domain Models (Completed)
- [x] FoodItem data class
- [x] Enums for categories and locations

### 🚧 Phase 4: Database Layer (In Progress)
- [ ] Room database setup
- [ ] FoodItem Entity
- [ ] FoodDAO with CRUD operations
- [ ] Database version and migrations

### 📋 Phase 5: Repository & UseCase (Planned)
- [ ] FoodRepository interface
- [ ] FoodRepositoryImpl
- [ ] Use cases (AddFoodItem, GetFoodItems, etc.)
- [ ] Mappers (Entity ↔ Domain)

### 📋 Phase 6: ViewModels & State (Planned)
- [ ] InventoryViewModel
- [ ] ProfileViewModel
- [ ] StateFlow for reactive UI
- [ ] Event handling

### 📋 Phase 7: UI Enhancement (Planned)
- [ ] RecyclerView adapter for food list
- [ ] Add/Edit dialogs
- [ ] Search functionality
- [ ] Swipe to delete
- [ ] Sort and filter options

### 📋 Phase 8: Advanced Features (Planned)
- [ ] Camera scanning
- [ ] Barcode detection
- [ ] Expiry notifications
- [ ] Recipe API integration
- [ ] Shopping list generation

### 📋 Phase 9: Polish & Optimization (Planned)
- [ ] Dark theme support
- [ ] Animations and transitions
- [ ] Performance optimization
- [ ] Unit tests
- [ ] UI tests

---

## 🎓 What I'm Learning

### Completed
- ✅ Android project structure
- ✅ Gradle build configuration
- ✅ Clean Architecture principles
- ✅ Kotlin syntax and data classes
- ✅ Fragments and Navigation Component
- ✅ Hilt dependency injection basics
- ✅ Material Design components
- ✅ XML layouts with ConstraintLayout
- ✅ Git version control

### Currently Learning
- 🚧 Room database and DAOs
- 🚧 MVVM pattern implementation
- 🚧 Repository pattern
- 🚧 Kotlin Coroutines
- 🚧 StateFlow for reactive programming

### Next Up
- 📋 RecyclerView and adapters
- 📋 LiveData vs StateFlow
- 📋 Use case pattern
- 📋 Testing (Unit & UI)
- 📋 API integration with Retrofit

---

## 🤝 Contributing

This is a personal learning project, but feedback and suggestions are always welcome!

If you find any issues or have suggestions:
1. Open an issue on GitHub
2. Describe the problem or suggestion
3. I'll review and respond

---

## 📝 Development Notes

### Key Decisions
- **Clean Architecture:** Chose this to learn proper separation of concerns and make the codebase maintainable
- **Hilt over Koin:** Selected Hilt for compile-time safety and better Android integration
- **Fragments over Compose:** Started with traditional Views to learn fundamentals first
- **Room over SQLite:** Type-safe queries and better Kotlin integration

### Challenges Overcome
1. ✅ Understanding package structure (kotlin+java folder confusion)
2. ✅ Fixing Hilt setup (forgot Application class in manifest)
3. ✅ Theme errors (Material3 vs AppCompat conflicts)
4. ✅ Navigation component setup
5. ✅ Understanding Layouts vs Fragments vs Activities

### Current Challenges
- 🚧 Understanding Room database relationships
- 🚧 Implementing MVVM correctly
- 🚧 Grasping Kotlin Coroutines

---

## 📚 Resources & Learning Materials

### Documentation
- [Android Developer Guides](https://developer.android.com/)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Material Design 3](https://m3.material.io/)
- [Hilt Documentation](https://dagger.dev/hilt/)

### Tutorials & Courses
- Android Basics with Compose (Google)
- Clean Architecture Guide (Robert C. Martin)
- Kotlin Coroutines Tutorial

### Tools
- Android Studio Hedgehog
- Git & GitHub
- Gradle

---

## 📄 License

This project is open source and available for educational purposes.

---

## 👨‍💻 About the Developer

**First Android Project** | **Learning in Public**

I'm a beginner Android developer documenting my learning journey. This project represents:
- My first complete Android application
- Learning Clean Architecture from scratch
- Understanding modern Android development practices
- Building something practical and useful

**GitHub:** [@jinjin-programm](https://github.com/jinjin-programm)

---

## 🙏 Acknowledgments

Special thanks to:
- The Android development community
- Stack Overflow contributors
- Android documentation team
- Open source library maintainers

---

## 📊 Project Stats

- **Lines of Code:** ~500 (and growing!)
- **Commits:** Multiple daily commits
- **Development Time:** Started February 2026
- **Current Version:** 0.1.0-alpha
- **Screens:** 5 functional screens
- **Packages:** 18 organized packages

---

**Last Updated:** February 16, 2026  
**Status:** 🚧 Active Development  
**Next Milestone:** Shopping List & Recipe API Implementation

---

*Built with ❤️ by a beginner learning Android development, one commit at a time.*
