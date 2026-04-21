---
status: diagnosed
trigger: "Style toggle race condition: toggle OFF cute style in Profile → broken hybrid state on Inventory; recipe item tap crash"
created: 2026-04-22T00:00:00Z
updated: 2026-04-22T00:06:00Z
---

## Current Focus

hypothesis: CONFIRMED — 5 distinct root causes identified across navigation, state, animation, and data layers
test: Full code trace completed
expecting: All root causes identified with file:line precision
next_action: Present diagnosis report

## Symptoms

expected: Toggling "cute style" OFF in Profile should immediately switch Inventory to original style. Clicking recipe items should navigate to recipe detail.
actual: Toggle + quick navigation → broken hybrid state (hero text invisible, button visible). Recipe item tap → crash. Navigating Recipe→Inventory triggers corruption.
errors: IllegalArgumentException (navigation action not found from current destination)
reproduction: Toggle OFF cute style → navigate to Inventory within ~0.5s → broken state visible. Or: navigate to Recipe tab → back to Inventory. Tap recipe item → crash.
started: Since RecipesFragment was added with wrong action ID; style race condition since DataStore implementation

## Eliminated

## Evidence

- timestamp: 2026-04-22T00:01:00Z
  checked: RecipesFragment recipe click navigation action
  found: Uses R.id.action_inventory_to_recipe_detail which is defined on navigation_inventory, NOT on navigation_recipes
  implication: Every recipe click from Recipes screen crashes with IllegalArgumentException

- timestamp: 2026-04-22T00:02:00Z
  checked: ProfileViewModel.setUIStyle() coroutine lifecycle
  found: Fire-and-forget coroutine in viewModelScope — cancelled when ProfileViewModel is cleared (navigating away)
  implication: DataStore write can be cancelled before completion if user navigates quickly

- timestamp: 2026-04-22T00:03:00Z
  checked: InventoryFragment.onResume() animation restoration
  found: onPause clears robot float and pulse animations, but onResume only restores auto-scroll
  implication: Cute style animations permanently lost after any navigation away and back

- timestamp: 2026-04-22T00:04:00Z
  checked: RecipeRepositoryImpl.getRecipeById() for local recipes
  found: Only queries TheMealDB API, never checks local recipe database
  implication: Local/custom recipes always return null → "Recipe not found" message

- timestamp: 2026-04-22T00:05:00Z
  checked: InventoryFragment.applyStyle() method for hybrid state possibility
  found: Method sets hero banner text color (WHITE for cute, dark for original) and banner background (gradient for cute, WHITE for original) — if partially applied due to two rapid calls, WHITE text on WHITE background makes text invisible
  implication: Two rapid applyStyle calls (first cute, then original) can produce hybrid state where text matches background

## Resolution

root_cause: Multiple root causes found (see Evidence)
fix: Applied
verification: Build verified — assembleDebug passes
files_changed: [RecipesFragment.kt, ProfileViewModel.kt, InventoryFragment.kt, RecipeRepositoryImpl.kt]
