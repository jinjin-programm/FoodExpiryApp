# FoodExpiry App - Project Documentation

## Overview

**FoodExpiry App** (also known as FreshAlert) is an Android application that helps users track their food inventory, manage expiry dates, reduce food waste, and save money. The app provides smart expiry notifications, recipe suggestions for expiring ingredients, and a shopping list with statistics tracking.

---

## Project Structure

```
FoodExpiryApp/
├── app/src/main/java/com/example/foodexpiryapp/
│   ├── MainActivity.kt                    # Main entry point with bottom navigation
│   ├── FoodExpiryApp.kt                  # Application class (Hilt + WorkManager)
│   │
│   ├── data/                             # DATA LAYER
│   │   ├── local/
│   │   │   ├── database/
│   │   │   │   ├── AppDatabase.kt        # Room database
│   │   │   │   └── FoodItemEntity.kt     # Room entity for food items
│   │   │   ├── dao/
│   │   │   │   ├── FoodItemDao.kt        # Data access object
│   │   │   │   └── AnalyticsEventDao.kt # Analytics DAO
│   │   │   └── RecipeJsonDto.kt          # Recipe JSON data transfer object
│   │   ├── remote/
│   │   │   ├── OpenFoodFactsApi.kt       # OpenFoodFacts API interface
│   │   │   ├── OpenFoodFactsService.kt  # API service implementation
│   │   │   └── dto/OpenFoodFactsResponse.kt
│   │   ├── repository/
│   │   │   ├── FoodRepositoryImpl.kt    # Food repository implementation
│   │   │   ├── RecipeRepositoryImpl.kt  # Recipe repository implementation
│   │   │   ├── UserRepositoryImpl.kt    # User profile repository
│   │   │   ├── AnalyticsRepositoryImpl.kt
│   │   │   ├── NotificationSettingsRepositoryImpl.kt
│   │   │   └── BarcodeRepository.kt
│   │   ├── mapper/
│   │   │   ├── FoodItemMapper.kt         # Entity ↔ Domain mapper
│   │   │   └── AnalyticsMapper.kt
│   │   └── analytics/
│   │       └── FoodAnalyticsService.kt   # Analytics tracking service
│   │
│   ├── domain/                           # DOMAIN LAYER (pure Kotlin)
│   │   ├── model/
│   │   │   ├── FoodItem.kt               # Food item domain model
│   │   │   ├── Recipe.kt                 # Recipe domain model
│   │   │   ├── UserProfile.kt            # User profile model
│   │   │   ├── NotificationSettings.kt  # Notification preferences
│   │   │   ├── AnalyticsEvent.kt         # Analytics event model
│   │   │   ├── WeeklyStats.kt            # Weekly statistics model
│   │   │   └── EventType.kt              # Event type enum
│   │   ├── repository/
│   │   │   ├── FoodRepository.kt         # Food CRUD interface
│   │   │   ├── RecipeRepository.kt       # Recipe search interface
│   │   │   ├── UserRepository.kt         # User profile interface
│   │   │   ├── AnalyticsRepository.kt   # Analytics interface
│   │   │   └── NotificationSettingsRepository.kt
│   │   └── usecase/
│   │       ├── FoodUseCases.kt           # Add, get, search, consume, discard
│   │       └── RecipeUseCases.kt          # Recipe operations
│   │
│   ├── presentation/                     # PRESENTATION LAYER
│   │   ├── ui/
│   │   │   ├── MainTabsFragment.kt       # Tab container fragment
│   │   │   ├── inventory/
│   │   │   │   └── InventoryFragment.kt  # Food inventory list screen
│   │   │   ├── shopping/
│   │   │   │   └── ShoppingFragment.kt   # Shopping list + stats screen
│   │   │   ├── recipes/
│   │   │   │   └── RecipesFragment.kt   # Recipes list screen
│   │   │   ├── planner/
│   │   │   │   └── PlannerFragment.kt    # Meal planner (EMPTY - needs work)
│   │   │   ├── profile/
│   │   │   │   ├── ProfileFragment.kt         # Main profile screen
│   │   │   │   ├── ProfileAccountFragment.kt  # Account settings
│   │   │   │   ├── ProfileSettingsFragment.kt  # App settings
│   │   │   │   ├── ProfileHelpFragment.kt     # Help & FAQ
│   │   │   │   └── ProfileFeedbackFragment.kt  # Send feedback
│   │   │   ├── scan/
│   │   │   │   └── ScanFragment.kt       # Barcode/text scanning
│   │   │   ├── yolo/
│   │   │   │   ├── YoloScanFragment.kt   # YOLO AI food detection
│   │   │   │   ├── YoloDetector.kt       # YOLO model wrapper
│   │   │   │   └── DetectionOverlayView.kt
│   │   │   ├── llm/
│   │   │   │   ├── LlmScanFragment.kt    # Local LLM vision scanning
│   │   │   │   ├── LlamaBridge.kt        # Native LLM bridge
│   │   │   │   └── LlmVisionService.kt   # LLM vision service
│   │   │   ├── vision/
│   │   │   │   └── VisionScanFragment.kt # ML Kit vision scanning
│   │   │   ├── chat/
│   │   │   │   ├── ChatFragment.kt       # Chat with LLM
│   │   │   │   ├── ChatViewModel.kt
│   │   │   │   ├── ChatAdapter.kt
│   │   │   │   └── ChatMessage.kt
│   │   │   └── scanner/
│   │   │       └── ScannerActivity.kt    # Camera scanner activity
│   │   ├── viewmodel/
│   │   │   ├── InventoryViewModel.kt
│   │   │   ├── ShoppingViewModel.kt
│   │   │   ├── RecipesViewModel.kt
│   │   │   ├── ProfileViewModel.kt
│   │   │   └── ChatViewModel.kt
│   │   ├── adapter/
│   │   │   ├── FoodItemAdapter.kt        # RecyclerView adapter for food items
│   │   │   ├── RecipeAdapter.kt          # RecyclerView adapter for recipes
│   │   │   ├── MainPagerAdapter.kt       # ViewPager adapter
│   │   │   └── ChatAdapter.kt            # Chat messages adapter
│   │   └── util/
│   │       ├── FirstTimeSetupHelper.kt
│   │       ├── PhotoStorageHelper.kt
│   │       └── ValidationHelper.kt
│   │
│   ├── di/                               # DEPENDENCY INJECTION (Hilt)
│   │   ├── DatabaseModule.kt             # Room database + DAOs
│   │   ├── NetworkModule.kt              # Retrofit + OkHttp
│   │   ├── RepositoryModule.kt           # Interface → Implementation bindings
│   │   ├── DataStoreModule.kt
│   │   └── AnalyticsModule.kt
│   │
│   ├── worker/                           # BACKGROUND WORK
│   │   └── ExpiryNotificationWorker.kt   # Periodic expiry notification worker
│   │
│   ├── receiver/
│   │   └── BootReceiver.kt               # Reschedule after device reboot
│   │
│   └── util/
│       ├── NotificationScheduler.kt       # Notification scheduling utility
│       └── ShelfLifeEstimator.kt          # Estimate food shelf life
│
├── app/src/main/res/
│   ├── layout/
│   │   ├── activity_main.xml
│   │   ├── fragment_inventory.xml
│   │   ├── fragment_shopping.xml
│   │   ├── fragment_recipes.xml
│   │   ├── fragment_planner.xml          # EMPTY - just placeholder text
│   │   ├── fragment_profile.xml
│   │   ├── fragment_scan.xml
│   │   ├── fragment_yolo_scan.xml
│   │   ├── fragment_llm_scan.xml
│   │   ├── fragment_vision_scan.xml
│   │   ├── fragment_chat.xml
│   │   ├── item_food.xml
│   │   ├── item_recipe.xml
│   │   ├── item_stat_card.xml
│   │   └── dialog_add_food.xml
│   ├── navigation/
│   │   └── nav_graph.xml
│   ├── menu/
│   │   └── bottom_nav_menu.xml
│   └── values/
│       ├── strings.xml
│       ├── colors.xml
│       └── themes.xml
│
├── app/src/main/jni/                      # Native LLM bridge (JNI)
├── app/src/main/assets/                   # Bundled models, labels, recipes JSON
└── yolo11n_saved_model/                   # YOLO model files
```

---

## Current Features

### ✅ Inventory Screen
- Display list of food items with name, category, expiry date, quantity
- Color-coded badges showing urgency (Urgent = red, Quick = orange, Normal = green)
- Search and filter food items
- Add new food items manually
- Mark items as Eaten or Discarded
- Swipe to delete items
- Navigate to scan screens (Barcode, YOLO, LLM, Vision)

### ✅ Shopping Screen
- Shows weekly statistics with 3 stat cards:
  - **Added** (Green) - Number of items added this week
  - **Eaten** (Orange) - Number of items consumed this week
  - **Expired** (Red) - Number of items expired this week
- Notifications sent counter
- Shopping list display (RecyclerView)
- Empty state handling

### ✅ Recipes Screen
- List of recipe suggestions
- Recipe cards showing name, image, brief description
- Links to recipes that help avoid food waste
- Recipe detail view

### ⚠️ Planner Screen (EMPTY - Needs Implementation)
- Currently shows only placeholder text: "📅 Planner Screen"
- **This screen needs to be built out**

### ✅ Profile Screen
- **Personal Information Section:**
  - Google Sign-In / Sign-Out
  - Profile photo display
  - Name and Email input fields
  
- **Household & Servings Section:**
  - Household size slider (1-10 people)
  - Description text showing current selection
  
- **Notification Settings Section:**
  - Enable/Disable expiry notifications toggle
  - Days before expiry slider (1-14 days, default 3)
  - Daily reminder time picker
  - Test notification button
  
- **Dietary Preferences Section:**
  - Chip group for dietary restrictions (Vegetarian, Vegan, Gluten-Free, etc.)
  
- **Save Changes button**

### ✅ Scanning Features
- **Barcode/Text Scan** - Manual entry with barcode lookup via OpenFoodFacts API
- **YOLO Scan** - AI-powered food detection using TensorFlow Lite YOLO model
- **LLM Scan** - Local LLM (Qwen3.5) for vision-based food identification via JNI
- **Vision Scan** - ML Kit for text recognition and object detection
- **Chat** - Conversational interface with local LLM

### ✅ Analytics & Notifications
- Track events: SCREEN_VIEW, ITEM_ADDED, ITEM_EATEN, ITEM_EXPIRED, NOTIFICATION_SENT
- Weekly statistics calculation
- Expiry notification worker (WorkManager)
- Boot receiver to reschedule notifications after reboot

---

## Architecture

**Clean Architecture + MVVM** with unidirectional data flow:

```
UI Fragment / Activity
    → ViewModel
    → Use Case
    → Repository Interface
    → Repository Impl
    → Data Source (Room / API / Assets / JNI)
```

---

## What's Done

| Feature | Status | Notes |
|---------|--------|-------|
| App foundation & navigation | ✅ Done | Bottom navigation with 5 tabs |
| Clean Architecture + MVVM | ✅ Done | Proper layer separation |
| Inventory management | ✅ Done | Full CRUD with Room database |
| Shopping list + stats | ✅ Done | Added/Eaten/Expired counters |
| Recipe list | ✅ Done | Basic recipe display |
| Profile management | ✅ Done | Google Sign-In, notifications, dietary prefs |
| Barcode scanning | ✅ Done | ML Kit + OpenFoodFacts API |
| YOLO food detection | ✅ Done | TensorFlow Lite integration |
| Local LLM integration | ✅ Done | Qwen3.5 via llama.cpp JNI |
| Vision scanning | ✅ Done | ML Kit text recognition |
| Analytics tracking | ✅ Done | Room-based event logging |
| Expiry notifications | ✅ Done | WorkManager + BootReceiver |
| Planner screen | ❌ Empty | Just placeholder text |

---

## What Should Be Done Next

### 1. **Planner Screen (HIGH PRIORITY)**
The Planner screen is completely empty. Suggested features:
- **Meal Calendar View** - Weekly/monthly calendar showing planned meals
- **Meal Planning** - Plan meals for breakfast, lunch, dinner
- **Link to Inventory** - Auto-suggest recipes based on expiring items
- **Shopping List Generation** - Generate shopping list from meal plans
- **Grocery Planning** - Plan grocery shopping trips

### 2. **Shopping List Functionality (HIGH PRIORITY)**
Currently shows stats but shopping list is not functional:
- Add items to shopping list
- Check off purchased items
- Sync with inventory (auto-add when purchased)
- Categorize shopping items

### 3. **Recipe Improvements**
- Connect recipes to inventory items
- Suggest recipes for expiring foods
- Recipe detail view with instructions
- Save favorite recipes
- Filter by dietary preferences

### 4. **UI/UX Polish**
- Replace placeholder icons with proper vector drawables
- Add empty states with illustrations
- Improve color scheme and theming
- Add animations and transitions

### 5. **Data Sync & Backup**
- Cloud backup with Firebase
- Sync across devices
- Export/Import data

### 6. **Testing**
- Add unit tests for ViewModels and UseCases
- Add UI tests for critical flows
- Instrumented tests for Room database

---

## External APIs Used

| API | Purpose | Status |
|-----|---------|--------|
| OpenFoodFacts | Barcode lookup, nutrition data | Integrated |
| Spoonacular (RapidAPI) | Recipe search | Referenced in docs |
| TheMealDB | Free recipe database | Referenced in docs |
| ML Kit | Barcode/Text/Vision scanning | Integrated |
| Firebase Auth | Google Sign-In | Integrated |
| Firebase Analytics | Event tracking | Integrated |
| Local LLM (Qwen3.5) | AI food identification | Integrated via JNI |
| YOLO (TensorFlow Lite) | Object detection | Integrated |

---

## Technology Stack

- **Language:** Kotlin
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34
- **Architecture:** Clean Architecture + MVVM
- **DI:** Hilt
- **Database:** Room
- **Networking:** Retrofit + OkHttp
- **Async:** Kotlin Coroutines + Flow
- **Navigation:** Navigation Component + SafeArgs
- **Background:** WorkManager
- **Camera:** CameraX
- **ML:** TensorFlow Lite, ML Kit, llama.cpp
- **Auth:** Firebase Auth + Google Sign-In

---

## 📝 Update Log

| Date | Update Description |
|------|--------------------|
| 2026-04-02 | Completely removed the top "Scan Barcode" app bar from `fragment_scan.xml` to free up more screen space for a cleaner, full-screen scanning experience. Updated local project documentation. |
