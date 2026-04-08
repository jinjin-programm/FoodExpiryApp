---
phase: 02-connect-modules
verified: 2026-04-08T18:30:00Z
status: human_needed
score: 3/3
overrides_applied: 0
human_verification:
  - test: "Open Recipes tab with inventory items expiring within 7 days"
    expected: "Expiring Soon card is visible, showing item names and days-to-expiry. Recipe list has recipes with 7-day matches sorted to the top."
    why_human: "Visual rendering and sort order are UI-level behaviors that require a running device/emulator."
  - test: "Verify recipe card performance indicators display correctly"
    expected: "Each recipe card shows: match count badge (e.g., '3 items matched'), money saved (e.g., 'Save ~$6.30'), waste score (e.g., '2 items rescued' in green if >=2), matched ingredient names, and conditional badges (Urgent, Quick, Waste Buster)."
    why_human: "Visual appearance, color coding, and badge visibility are UI-level."
  - test: "Assign a recipe to a meal slot in the Planner"
    expected: "Recipe picker bottom sheet opens showing match info ('X ingredients rescued' in green). Tapping a recipe assigns it to the selected slot. The slot card then displays the recipe name and 'Recipe' type label."
    why_human: "End-to-end flow through bottom sheet dialog, persistence, and UI update requires running the app."
  - test: "Test fallback behavior with no expiring items"
    expected: "When no inventory items expire within 7 days, the recipe picker still shows all available recipes (without match info text) and allows assignment."
    why_human: "Fallback path logic is code-verified but UI behavior needs manual confirmation."
---

# Phase 2: Connect Modules Verification Report

**Phase Goal:** Make the app explain itself through connected data (recipes and planning).
**Verified:** 2026-04-08T18:30:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Recipes module prominently suggests meals using ingredients expiring within 7 days | ✓ VERIFIED | `ScoreRecipesForInventoryUseCase` computes `expiryUrgencyBonus` (expired=10, 1d=8, 2-3d=5, 4-7d=3, 8d+=1). `RecipesViewModel` partitions ALL filter results: `withExpiry` (matches with any item ≤7 days) sorted before `withoutExpiry` (lines 128-135). `expiringItems` filtered at `daysUntilExpiry <= 7` (line 122). |
| 2 | Recipe cards display realistic performance indicators ("ingredients rescued") | ✓ VERIFIED | `RecipeAdapter.bind()` populates: `textWasteScore` = "{matchCount} items rescued" with green/orange color coding (lines 50-53), `textMoneySaved` = formatted currency (line 49), `textMatchCount` with badge background (lines 62-82), `textMatchedIngredients` with names (lines 55-60), plus Urgent/Quick/WasteBuster badges. `item_recipe.xml` contains all required views. |
| 3 | User can assign a chosen recipe to a specific date and meal slot in the Planner | ✓ VERIFIED | `PlannerViewModel` injects `ScoreRecipesForInventoryUseCase`, computes `suggestedRecipes` filtered to matches with items ≤7 days (lines 111-114). `RecipePickerAdapter` accepts `RecipeMatch` and displays "X ingredients rescued" (lines 42-44). `PlannerFragment.showRecipePicker()` uses `getFilteredRecipesWithMatchInfo()` with fallback to all recipes (lines 215-260). `onAddRecipe()` creates `MealPlan` and persists via `SaveMealPlanUseCase` (lines 150-163). Slot displays recipe name + "Recipe" type (line 167). |

**Score:** 3/3 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `RecipeUseCases.kt` (ScoreRecipesForInventoryUseCase) | Expiry-weighted scoring with 7-day window | ✓ VERIFIED | 108 lines. `expiryUrgencyBonus` computed per matched item with tiered weights (lines 70-78). Composite sort score includes bonus (line 79). |
| `RecipesViewModel.kt` | Promoted sorting for expiring matches | ✓ VERIFIED | 263 lines. Partitions matches by 7-day expiry (lines 128-135), `expiringItems` at ≤7 days (line 122), feeds `scoreRecipesForInventory` (line 125). |
| `RecipeAdapter.kt` | Performance indicator binding | ✓ VERIFIED | 103 lines. Binds all `RecipeMatch` fields: matchCount, estimatedMoneySaved, wasteRescue (as "items rescued"), matchedIngredients, urgency/quick/wasteBuster badges. Color-coded match badge (lines 68-82). |
| `item_recipe.xml` | Recipe card layout with rescue indicators | ✓ VERIFIED | 257 lines. Contains `text_waste_score`, `text_match_count`, `text_money_saved`, `text_matched_ingredients`, `text_urgent_badge`, `text_quick_badge`, `text_waste_buster_badge`. |
| `PlannerViewModel.kt` | Expiry-aware recipe suggestions | ✓ VERIFIED | 237 lines. `ScoreRecipesForInventoryUseCase` injected (line 61). `suggestedRecipes` computed from scored matches filtered to ≤7 day items (lines 111-114). `getSuggestedRecipes()` and `getFilteredRecipesWithMatchInfo()` public methods (lines 224-236). |
| `RecipePickerAdapter.kt` | Recipe picker with match info | ✓ VERIFIED | 77 lines. Accepts `RecipeMatch` callback (line 14). Shows "{matchCount} ingredients rescued" when >0 (lines 42-44). |
| `item_recipe_picker.xml` | Picker row with match indicator | ✓ VERIFIED | 72 lines. Contains `text_match_info` (id verified, green text, 11sp, visibility=gone by default). |
| `PlannerFragment.kt` | Updated recipe picker with match display | ✓ VERIFIED | 357 lines. `showRecipePicker()` uses `getFilteredRecipesWithMatchInfo()` (line 215). Fallback wraps all recipes in minimal RecipeMatch (lines 219-232). Search filtering for both paths (lines 235-260). |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| RecipesViewModel → ScoreRecipesForInventoryUseCase | RecipeUseCases.kt | Constructor injection + `scoreRecipesForInventory()` call | ✓ WIRED | `scoreRecipesForInventory` declared as constructor param (line 57), called in uiState combine (line 125). |
| RecipesViewModel → RecipeAdapter | RecipeAdapter | `submitList(recipeMatches)` | ✓ WIRED | `recipeAdapter.submitList(state.recipeMatches)` in RecipesFragment.observeState (line 158). Adapter receives `RecipeMatch` list. |
| PlannerViewModel → ScoreRecipesForInventoryUseCase | RecipeUseCases.kt | Constructor injection + `scoreRecipesForInventory()` call | ✓ WIRED | Constructor param (line 61), called in loadData (line 111). |
| PlannerFragment → RecipePickerAdapter | RecipePickerAdapter | `showRecipePicker()` instantiation | ✓ WIRED | Adapter created in showRecipePicker (line 205), submitList called with suggestedRecipes (line 217) or fallback (line 232). |
| PlannerFragment → PlannerViewModel.onAddRecipe | PlannerViewModel | Recipe selection callback | ✓ WIRED | `viewModel.onAddRecipe(slot, match.recipe)` called in adapter callback (line 206). onAddRecipe creates MealPlan and calls saveMealPlan (lines 150-163). |
| PlannerViewModel → SaveMealPlanUseCase | MealPlanRepository | `onAddRecipe()` | ✓ WIRED | `saveMealPlan(mealPlan)` called in onAddRecipe (line 161). SaveMealPlanUseCase inserts or updates via repository. |
| RecipeRepositoryImpl → TheMealDbApi | Remote API | `getRecipesMatchingInventory()` | ✓ WIRED | Fetches recipes by ingredient from API, then fetches full details via `getMealDetails()` (lines 82-139). Falls back to random meals if <5 results (line 133). |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|--------------------|--------|
| RecipesViewModel.uiState | `recipeMatches` | TheMealDbApi via RecipeRepositoryImpl → ScoreRecipesForInventoryUseCase | ✓ FLOWING | API fetches real recipes, scoring computes real match counts from local inventory. Expiry dates from `FoodItem.expiryDate` are real user-entered/barcode data. |
| RecipeAdapter | `match.matchCount`, `match.estimatedMoneySaved` | ScoreRecipesForInventoryUseCase | ✓ FLOWING | `estimatedSaved` = `matchedInvItems.sumOf { avgItemCost * 0.7 }` — derived from real match count. Not hardcoded. |
| PlannerViewModel.suggestedRecipes | `suggestedRecipes` | scoreRecipesForInventory → filter by daysUntilExpiry | ✓ FLOWING | Computed from allRecipes (API) + inventoryItems (local DB). Filter applies real 7-day threshold. |
| RecipePickerAdapter | `match.matchCount` | PlannerViewModel.getSuggestedRecipes() | ✓ FLOWING | Data flows from ViewModel through Fragment to Adapter. When matchCount > 0, "X ingredients rescued" is shown. |

### Behavioral Spot-Checks

Step 7b: SKIPPED (Android app — no runnable CLI entry points; requires device/emulator to test behaviors)

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| RECP-01 | 02-01 | Recommend recipes based specifically on soon-to-expire ingredients | ✓ SATISFIED | ScoreRecipesForInventoryUseCase weights by expiry proximity. RecipesViewModel partitions 7-day matches to top. PlannerViewModel filters suggestedRecipes to ≤7 day matches. |
| RECP-02 | 02-01 | Display concrete performance indicators on recipe cards | ✓ SATISFIED | RecipeAdapter binds matchCount ("X items rescued"), estimatedMoneySaved, wasteRescuePercent, matchedIngredients. Color-coded badges for urgency, speed, waste. |
| PLAN-01 | 02-02 | Support adding a chosen recipe to a specific date and meal slot | ✓ SATISFIED | PlannerFragment.showRecipePicker() → RecipePickerAdapter selection → PlannerViewModel.onAddRecipe(slot, recipe) → SaveMealPlanUseCase → MealPlanRepository. Date selection via MaterialDatePicker. |

### Anti-Patterns Found

No anti-patterns detected. No TODO/FIXME/PLACEHOLDER comments found in modified files. No empty implementations or hardcoded empty data flows in the connected module paths.

### Human Verification Required

### 1. Recipes Tab — Expiry-Prominent Suggestion Order

**Test:** Launch app with inventory items expiring within 7 days. Navigate to Recipes tab.
**Expected:** "Expiring Soon" card is visible at the top, listing item names and days-to-expiry. The default recipe list shows recipes with 7-day ingredient matches at the top, before recipes without expiring matches.
**Why human:** Sort ordering and card visibility are UI behaviors requiring visual confirmation on a running device.

### 2. Recipe Card Performance Indicators

**Test:** On the Recipes tab, inspect recipe cards that have ingredient matches.
**Expected:** Each matched recipe card displays: a match count badge (e.g., "3 items matched" with colored background), "Save ~$X.XX" in green, "X items rescued" with green text (if ≥2) or orange (if <2), matched ingredient names (e.g., "✅ Matches: Banana, Oats"), and conditional badges (⚠️ Urgent for ≤1 day matches, ⚡ Quick for <30 min, 🏆 Waste Buster for tagged recipes).
**Why human:** Visual rendering, color coding, and conditional badge visibility are UI-level.

### 3. Planner Recipe Assignment End-to-End

**Test:** Navigate to Planner tab. Tap "+ Recipe" on any meal slot. Select a recipe from the picker.
**Expected:** Bottom sheet opens showing recipes ranked by expiry relevance. Each row shows "X ingredients rescued" in green text for matching recipes. After tapping a recipe, the bottom sheet dismisses and the meal slot card displays the recipe name and "Recipe" type label. A snackbar confirms "Added {slot}: {name}".
**Why human:** Full dialog flow, persistence, and UI update chain require a running app.

### 4. Planner Fallback (No Expiring Items)

**Test:** Clear all inventory or ensure no items expire within 7 days. Navigate to Planner and tap "+ Recipe".
**Expected:** Recipe picker still opens showing all available recipes (without "X ingredients rescued" text). Search filtering works. Selecting a recipe still assigns it to the slot.
**Why human:** Fallback behavior is code-verified but needs UI confirmation.

### Gaps Summary

No gaps found. All 3 success criteria are substantively implemented and wired end-to-end. The codebase shows:

1. **Expiry-weighted scoring** is implemented in `ScoreRecipesForInventoryUseCase` with a 5-tier urgency bonus system that directly rewards recipes rescuing items expiring within 7 days.

2. **Performance indicators** are fully bound in `RecipeAdapter` — match count, money saved, items rescued (with color coding), matched ingredient names, and conditional badges. The `text_waste_score` view uses "X items rescued" (not just a percentage) as specified.

3. **Planner recipe assignment** is fully connected — `PlannerViewModel` generates expiry-aware suggestions, `RecipePickerAdapter` displays match info, and the selection callback persists via `SaveMealPlanUseCase` to the Room database. The meal slot card displays the recipe name and type after assignment.

The only remaining verification items are UI-level visual behaviors that require a running Android device/emulator.

---

_Verified: 2026-04-08T18:30:00Z_
_Verifier: the agent (gsd-verifier)_
