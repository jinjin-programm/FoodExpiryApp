---
phase: 02-connect-modules
plan: 02
subsystem: planner
tags: [planner, recipe-picker, expiry-aware, dependency-injection, adapter]
dependency_graph:
  requires: [02-01, 01-01, 01-03]
  provides: []
  affects: [planner-tab, recipe-picker-dialog]
tech_stack:
  added: []
  patterns: [fallback-adapter-wrapping, match-info-display]
key_files:
  created: []
  modified:
    - app/src/main/java/com/example/foodexpiryapp/presentation/viewmodel/PlannerViewModel.kt
    - app/src/main/java/com/example/foodexpiryapp/presentation/adapter/RecipePickerAdapter.kt
    - app/src/main/res/layout/item_recipe_picker.xml
    - app/src/main/java/com/example/foodexpiryapp/presentation/ui/planner/PlannerFragment.kt
key_decisions:
  - "Wrapped all-recipes fallback in minimal RecipeMatch objects to maintain single adapter type"
  - "Kept getFilteredRecipes() intact for backward compatibility"
  - "Match info shows count of rescued ingredients in green text"
metrics:
  duration: "4 min"
  completed: "2026-04-08"
  tasks_completed: 2
  tasks_total: 2
  files_modified: 4
---

# Phase 2 Plan 02: Planner Expiry-Aware Recipe Picker & Slot Assignment Summary

Connected the Planner's recipe picker to `ScoreRecipesForInventoryUseCase` so users see recipes ranked by expiry impact, with match indicators on each picker row.

## What Was Done

### Task 1: Add expiry-aware recipe suggestions to PlannerViewModel
- Injected `ScoreRecipesForInventoryUseCase` into `PlannerViewModel` constructor
- Added `suggestedRecipes: List<RecipeMatch>` to `PlannerUiState`
- In `loadData()`, computed suggested recipes by scoring all recipes against inventory, filtering to matches with at least one item expiring within 7 days
- Added `getSuggestedRecipes()` public method
- Added `getFilteredRecipesWithMatchInfo(query)` public method for search-filtered match results

### Task 2: Update recipe picker to show match info and rescue indicators
- Added `text_match_info` TextView to `item_recipe_picker.xml` (green text, 11sp, hidden by default)
- Rewrote `RecipePickerAdapter` to accept `RecipeMatch` instead of `Recipe`
- Adapter shows "{matchCount} ingredients rescued" when matchCount > 0
- Updated `PlannerFragment.showRecipePicker()` to use `getFilteredRecipesWithMatchInfo()`
- Implemented fallback: when no suggested recipes exist (no expiring items), wraps all recipes in minimal `RecipeMatch` objects (matchCount=0)
- Search filtering works for both suggested and fallback paths

## Deviations from Plan

None - plan executed exactly as written.

## Commits

| Commit | Message |
|--------|---------|
| b62f389 | feat(02-02): connect planner recipe picker to inventory expiry data |

## Self-Check: PASSED
