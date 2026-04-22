---
status: awaiting_human_verify
trigger: "Inventory page display bug — items disappear, hero banner text missing, No adapter attached warnings"
created: 2026-04-22T00:00:00Z
updated: 2026-04-22T00:00:00Z
---

## Current Focus

hypothesis: currentStyle retained across view recreation causes applyStyle() to be skipped, leaving new RecyclerViews without adapters and hero banner with white-on-white text
test: trace currentStyle lifecycle and applyStyle() invocation on view recreation
expecting: currentStyle == state.uiStyle after view recreation → applyStyle() skipped → adapters never attached
next_action: apply fix — reset currentStyle in onDestroyView(), set default adapters in setupRecyclerViews()

## Symptoms

expected: Inventory page shows hero banner text, expiring items, food items, or empty state correctly after navigating between tabs
actual: Hero banner text disappears, expiration section blank, food items blank, NOT showing empty state. Persistent once triggered.
errors: "No adapter attached; skipping layout" (8+ pairs), "Setting a custom background is not supported"
reproduction: Navigate between tabs in ViewPager2/BottomNav, toggle style, return to inventory — fragments with view recreation trigger the bug
started: After commit dc7cd32 which restructured applyStyle() to reset-then-apply pattern

## Eliminated

## Evidence

- timestamp: 2026-04-22T00:00:00Z
  checked: InventoryFragment.kt setupRecyclerViews() (line 130-168)
  found: Adapters created but NEVER set on RecyclerViews in setupRecyclerViews(). Only applyStyle() sets them (lines 296-297, 341-342).
  implication: Without applyStyle(), RecyclerViews have no adapters

- timestamp: 2026-04-22T00:00:00Z
  checked: InventoryFragment.kt observeState() (line 405-453)
  found: applyStyle() only called when currentStyle != state.uiStyle (line 409). repeatOnLifecycle(STARTED) restarts on view recreation, but currentStyle retains old value.
  implication: If currentStyle == state.uiStyle after view recreation, applyStyle() is never called for new view

- timestamp: 2026-04-22T00:00:00Z
  checked: InventoryFragment.kt currentStyle field (line 103) and onDestroyView() (line 832-838)
  found: currentStyle = "" initially, set in observeState(). onDestroyView() does NOT reset currentStyle. Fragment survives view destruction in ViewPager2.
  implication: On view recreation, currentStyle retains previous value, matches state.uiStyle, applyStyle() skipped

- timestamp: 2026-04-22T00:00:00Z
  checked: fragment_inventory.xml hero banner text defaults (lines 97, 112)
  found: textHeroTitle textColor="#FFFFFF" (WHITE), textHeroSubtitle textColor="#CCFFFFFF" (WHITE alpha). MaterialCardView default background is white/surface.
  implication: Without applyStyle() running, hero banner has WHITE text on WHITE background = INVISIBLE

- timestamp: 2026-04-22T00:00:00Z
  checked: observeState() visibility logic (lines 414-421)
  found: isEmpty = state.foodItems.isEmpty(). When not empty: recyclerExpiringSoon VISIBLE, layoutEmptyState GONE. But without adapters, visible RecyclerViews show nothing.
  implication: Explains "NOT showing empty state" + "sections blank" — state has items but adapters not attached to RecyclerViews

## Resolution

root_cause: currentStyle is NOT reset in onDestroyView(). When the fragment's view is destroyed and recreated (e.g., ViewPager2 off-screen recycling), currentStyle retains its previous value. On the next observeState() collection, currentStyle == state.uiStyle, so applyStyle() is never called. The new RecyclerViews never get adapters attached (setupRecyclerViews only creates adapter objects, never assigns them). The hero banner defaults to white text on white background from XML. The empty state logic sees foodItems.isNotEmpty() so hides empty state, but the RecyclerViews are visible with no adapters → blank sections.
fix: 1) Reset currentStyle="" in onDestroyView() so applyStyle() always runs on view recreation. 2) Set default adapters in setupRecyclerViews() to prevent transient "No adapter attached" warnings at initial startup.
verification: build compiles (assembleDebug BUILD SUCCESSFUL). Fix 1 ensures applyStyle() always runs on view recreation. Fix 2 prevents transient "No adapter attached" at startup. Awaiting human verification on device.
files_changed: [InventoryFragment.kt]
