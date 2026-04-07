---
phase: 02-connect-modules
plan: 01
subsystem: recipes
tags: [recipes, scoring, expiry, performance-indicators, viewmodel, adapter]
dependency_graph:
  requires: [01-01, 01-02, 01-05]
  provides: [02-02]
  affects: [recipes-tab, planner-recipe-picker]
tech_stack:
  added: []
  patterns: [expiry-weighted-scoring, promoted-section-sorting]
key_files:
  created: []
  modified:
    - app/src/main/java/com/example/foodexpiryapp/domain/usecase/RecipeUseCases.kt
    - app/src/main/java/com/example/foodexpiryapp/presentation/viewmodel/RecipesViewModel.kt
    - app/src/main/java/com/example/foodexpiryapp/presentation/adapter/RecipeAdapter.kt
key_decisions:
  - "Used weighted scoring (expired=10, 1d=8, 2-3d=5, 4-7d=3, 8d+=1) instead of binary flag"
  - "Extended expiringItems from 3 to 7 days to match scoring window"
  - "Kept ALL filter promotion simple: partition into with/without 7-day matches"
metrics:
  duration: "3 min"
  completed: "2026-04-08"
  tasks_completed: 2
  tasks_total: 2
  files_modified: 3
---

# Phase 2 Plan 01: Enhance Recipes with Expiry-Aware Scoring & Performance Indicators Summary

Expiry-weighted recipe scoring using `ScoreRecipesForInventoryUseCase` with a tiered urgency bonus, and corrected `RecipeAdapter` binding to display `RecipeMatch`-derived performance indicators.

## What Was Done

### Task 1: Enhance recipe scoring to prioritise 7-day expiring ingredients
- Updated `ScoreRecipesForInventoryUseCase.invoke()` to calculate an `expiryUrgencyBonus` per matched inventory item based on `daysUntilExpiry` proximity (expired=10, 1 day=8, 2-3 days=5, 4-7 days=3, 8+ days=1)
- Extended `expiringItems` in `RecipesUiState` from `daysUntilExpiry <= 3` to `daysUntilExpiry <= 7`
- Added promoted-section sorting in the ALL filter: recipes with any matched item within 7 days appear first

### Task 2: Ensure recipe cards display rescue performance indicators
- Fixed `text_waste_score` to use `match.wasteRescuePercent` (was using `recipe.wasteRescueScore`)
- Added color highlighting: green for >=50% waste rescue, orange otherwise
- Fixed `text_urgent_badge` to show when any matched inventory item has `daysUntilExpiry <= 1` (was checking recipe tag)
- Fixed `text_quick_badge` to show when `totalTimeMinutes < 30` (was checking recipe tag)
- RecipesFragment "Expiring Soon" card now shows all items within 7-day window (automatic via ViewModel change)

## Deviations from Plan

None - plan executed exactly as written.

## Commits

| Commit | Message |
|--------|---------|
| c748b44 | feat(02-01): enhance recipe scoring with 7-day expiry prioritisation and rescue indicators |

## Self-Check: PASSED
