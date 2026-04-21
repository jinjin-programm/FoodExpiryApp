# FoodExpiryApp Project Structure & Progress

## Current Workflow

UI Fragment / Activity
    -> ViewModel
    -> Use Case
    -> Repository Interface
    -> Repository Impl
    -> Data Source (Room / API / Assets / JNI)

## Main Project Structure

app/src/main/java/com/example/foodexpiryapp/
�u�w�w MainActivity.kt
�u�w�w FoodExpiryApp.kt
�u�w�w data/
�x   �u�w�w local/
�x   �u�w�w remote/
�x   �u�w�w repository/
�x   �u�w�w mapper/
�x   �|�w�w analytics/
�u�w�w domain/
�x   �u�w�w model/
�x   �u�w�w repository/
�x   �|�w�w usecase/
�u�w�w presentation/
�x   �u�w�w ui/
�x   �u�w�w viewmodel/
�x   �u�w�w adapter/
�x   �|�w�w util/
�u�w�w di/
�u�w�w worker/
�u�w�w receiver/
�|�w�w util/

## What Each Part Does

- presentation/ contains the screens, adapters, and UI state handling.
- domain/ contains the core models, repository contracts, and business rules.
- data/ contains Room, remote API, mappers, analytics storage, and repository implementations.
- di/ wires dependencies with Hilt.
- worker/ handles background expiry notifications.
- receiver/ restores scheduled work after reboot.
- app/src/main/jni/ connects the native LLM bridge.
- app/src/main/assets/ stores bundled model files and labels.
- app/src/main/res/ stores layouts, drawables, menus, navigation, and theme values.
- app/src/test/ and app/src/androidTest/ hold unit and instrumented tests.

## Progress Snapshot

### Done

 - App foundation and navigation structure
 - Clean Architecture + MVVM layering
 - Profile, inventory, shopping, recipe, planner, and scan screens
 - Local LLM / vision scan support (llama.cpp + GGUF)
 - YOLO model integration
 - Analytics tracking and swipe navigation
 - Expiry notification foundation
 - Recipes Page Optimization (Phase 2-5)
   - I Cooked This button now consumes inventory items
   - CookedRecipeEntity + DAO + Repository for persistent history
   - Money Saved and Waste Rescued now track real data from cooked recipes
   - Recipe diversity: Added category/area filtering (Breakfast, Dessert, Chinese, Mexican, Japanese, Indian, Italian)
   - GetRecipesByCategoryUseCase and GetRecipesByAreaUseCase implemented
   - OpenFoodFacts API integration for real ingredient pricing (PriceUseCases.kt)
   - Local recipe storage infrastructure (LocalRecipeEntity, LocalRecipeDao, LocalRecipeRepository)
   - FAB button added to Recipes page for adding custom recipes
 - Food Image System (2026-04-18)
   - 99 high-quality food images downloaded to drawable/ (apple, banana, bread, cheese, chicken, beef, salmon, etc.)
   - FoodImageResolver.kt — smart name→drawable mapping with fuzzy matching + category fallback
   - FoodListAdapter, FoodItemAdapter updated to show real food images via Glide
   - item_food.xml redesigned with 48x48 rounded food image + eaten checkbox overlay
   - InventoryFragment + AddFoodBottomSheet dynamically update food preview based on typed name
 - Phase 5: Local Recipe Storage + Manual Add UI (DONE - 2026-04-19)
   - Wiring up FAB click handler in RecipesFragment
   - Implementing AddRecipe dialog with data extraction and validation
   - Merging local recipes with remote ones in RecipesViewModel

### In Progress

- UI polishing and screen refinement
- Feature stabilization across scan / chat / inventory flows
- Test coverage expansion
- TheMealDB integration plan (local-first, cached online recipes)

### Next

- Finalize repository cleanup and code consistency
- Improve error handling and edge cases
- Verify notifications, analytics, and model loading on device
- Complete Phase 5: AddRecipe dialog integration with RecipesFragment (DONE - 2026-04-19)

## Recipes Page Optimization - Detailed Progress

### Phase 1: Fix I Cooked This Button
- File: RecipesViewModel.kt
- Injected ConsumeIngredientsUseCase into RecipesViewModel
- Modified onRecipeCooked() to call consumeIngredients(matchedItems) which decrements inventory

### Phase 2: Persistent Cooked Recipe History
- Created:
  - CookedRecipeEntity.kt - Room Entity (id, recipeId, recipeName, cookedAt, moneySaved, wasteRescuedPercent, matchedIngredients, imageUrl)
  - CookedRecipeDao.kt - DAO with queries for count, sum money, average waste
  - CookedRecipeRepository.kt - Interface
  - CookedRecipeRepositoryImpl.kt - Implementation
- Updated:
  - AppDatabase.kt - Added CookedRecipeEntity, version 6, MIGRATION_5_6
  - DatabaseModule.kt - Added provideCookedRecipeDao() and MIGRATION_5_6
  - RepositoryModule.kt - Added bindCookedRecipeRepository
  - RecipesViewModel.kt - Added cookedStats flow, modified onRecipeCooked() to save to repository

### Phase 3: Recipe Diversity (Category/Area Filtering)
- Created/Updated:
  - RecipeRepositoryImpl.kt - Added getRecipesByCategory() and getRecipesByArea() implementations
  - RecipeUseCases.kt - Added GetRecipesByCategoryUseCase and GetRecipesByAreaUseCase
  - RecipesViewModel.kt - Added new RecipeFilter enum values (BREAKFAST, DESSERT, CHINESE, MEXICAN, JAPANESE, INDIAN, ITALIAN), updated recipesFlow to call API for cuisine filters
  - fragment_recipes.xml - Added 8 new filter chips
  - RecipesFragment.kt - Updated setupFilterChips() and updateSelectedFilterChip() for new filters

### Phase 4: OpenFoodFacts API for Real Pricing
- Files (already existed):
  - OpenFoodFactsApi.kt - Retrofit interface with getProductByBarcode()
  - OpenFoodFactsService.kt - Retrofit service interface
  - OpenFoodFactsResponse.kt - DTO with ProductDto
- Created:
  - PriceUseCases.kt - GetIngredientPriceUseCase for fetching ingredient prices

### Phase 5: Local Recipe Storage + Manual Add UI (In Progress)
- Created:
  - LocalRecipeEntity.kt - Room entity for user-added recipes
  - LocalRecipeDao.kt - DAO with CRUD operations
  - LocalRecipeRepository.kt - Interface
  - LocalRecipeRepositoryImpl.kt - Implementation with Gson serialization
  - dialog_add_recipe.xml - Layout for add recipe dialog
- Updated:
  - AppDatabase.kt - Added LocalRecipeEntity, version 7, MIGRATION_6_7
  - DatabaseModule.kt - Added provideLocalRecipeDao()
  - RepositoryModule.kt - Added bindLocalRecipeRepository
  - fragment_recipes.xml - Added FAB button for adding recipes

### Build Fix: BarcodeRepository
- Fixed BarcodeRepository to use OpenFoodFactsApi instead of OpenFoodFactsService
- Updated scanBarcode() to handle Response wrapper from getProductByBarcode()

## Git Timeline

- 2026-03-06: native LLM inference, crash fixes, and scan stability work
- 2026-03-07: Qwen3-VL multimodal vision support
- 2026-03-10: model loading fix and filename / mmproj correction
- 2026-03-11: speed-dial UI update
- 2026-03-17: initial project baseline with LLM support and notifications
- 2026-03-19: analytics system, mark-as-eaten flow, and swipe navigation
- 2026-04-02: Fix Navigation crash (IllegalArgumentException) in InventoryFragment when repeatedly clicking scan actions. Added findNavController().currentDestination?.getAction guard.
- 2026-04-02: Fix camera ERROR_MAX_CAMERAS_IN_USE issue. Added cameraProvider?.unbindAll() to onDestroyView across all scanner fragments (VisionScanFragment, ScanFragment, YoloScanFragment).
- 2026-04-02: Redesigned barcode scan UI (fragment_scan.xml) for a cleaner card-based look.
- 2026-04-02: Fixed YOLO scan UI constraints (fragment_yolo_scan.xml) by breaking the vertical constraint chain. Moved top controls just below the status bar (24dp margin), and adjusted the scanning frame bias (0.2) to pull the frame and its attached instruction text cleanly upward, ensuring no overlap with bottom camera controls. Updated YOLO instruction text color to a vibrant red for better visibility.
- 2026-04-05: Migrated LLM inference from MNN to llama.cpp with GGUF models (Qwen3-VL-2B-Instruct). Addressed thinking mode leakage by enforcing strict ChatML prompt to suppress internal reasoning output and guarantee direct structured answers.
- 2026-04-05: Recipes page optimization - Phase 1-4 complete, Phase 5 in progress. I Cooked This now consumes inventory, persistent cooked recipe history with Money Saved/Waste Rescued tracking, expanded recipe filtering with 8 new cuisine/category options, OpenFoodFacts API integration for real pricing, local recipe storage infrastructure.

## Short Version For Team

The app is organized by Clean Architecture: UI in presentation/, business rules in domain/, and storage/API/native integration in data/. The current work is centered on scan/LLM features, analytics, and keeping the inventory and notification flows stable. Recent work focused on the Recipes page - fixing the I Cooked This button to actually consume inventory, adding persistent tracking of Money Saved and Waste Rescued stats, expanding recipe diversity with cuisine/category filters (Chinese, Italian, Mexican, etc.), and setting up infrastructure for user-added recipes. Migrated LLM from MNN to llama.cpp plus GGUF for cleaner AI agent integration.
