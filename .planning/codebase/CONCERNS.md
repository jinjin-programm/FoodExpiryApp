# Codebase Concerns

**Analysis Date:** 2026-04-08

## Tech Debt

**Inventory Module / Empty States:**
- Issue: The Inventory view completely lacks an explicit empty state or "Get Started" onboarding area when a user has no food items.
- Files: `app/src/main/res/layout/fragment_inventory.xml`, `app/src/main/java/com/example/foodexpiryapp/presentation/ui/inventory/InventoryFragment.kt`
- Impact: New users are greeted with a blank list instead of immediate guidance to add their first item in < 30 seconds, failing the core UX goal.
- Fix approach: Implement an explicit "Get Started" layout overlay/view with "Photo Scan" and "Manual Entry" buttons to handle the zero-item state.

**Scan Flow UX & Naming:**
- Issue: The main scan CTA uses the technical name "Vision AI Scan" instead of the approachable "Photo Scan". The scanner instantly pops the back stack and uses a generic dialog in the parent fragment rather than a dedicated "confirm-before-save" step inside the scanner. A flash button also exists but is noted to be unhelpful.
- Files: `app/src/main/res/layout/fragment_inventory.xml`, `app/src/main/java/com/example/foodexpiryapp/presentation/ui/scan/ScanFragment.kt`, `app/src/main/res/layout/fragment_scan.xml`
- Impact: Confusing scan experience and potential for accidental saves.
- Fix approach: Rename UI elements to "Photo Scan", remove the unused Flash button, and implement an integrated verification screen inside the scan module before saving.

**Expiring Soon Sorting Logic:**
- Issue: "Expiring Soon" items are sorted purely by date (`ORDER BY expiryDate ASC`) without calculating or considering a semantic "risk level" (e.g., already expired, 1 day left). Food cards lack a risk-level badge or suggested actions.
- Files: `app/src/main/java/com/example/foodexpiryapp/data/local/dao/FoodItemDao.kt`, `app/src/main/res/layout/item_food_card.xml`
- Impact: Urgent items lack emotional/visual prioritization beyond raw dates.
- Fix approach: Implement a "risk level" grouping at the ViewModel or DAO layer, and update `item_food_card.xml` to reflect this semantic priority.

## Missing Critical Features

**Unified Food Data Model Gaps:**
- Problem: The core `FoodItemEntity` is missing fields critical to the UX optimization plan.
- Files: `app/src/main/java/com/example/foodexpiryapp/data/local/database/FoodItemEntity.kt`
- Blocks: Cannot properly track `scanSource`, `confidenceLevel`, `riskLevel`, or `recipeRelevance`. These are prerequisites for building a unified, intelligent tracking system.

**Planner & Recipe Disconnect:**
- Problem: The `PlannerFragment` uses a crude `BottomSheetDialog` to pick recipes but does not suggest recipes based on soon-to-expire items, nor does it flag missing ingredients.
- Files: `app/src/main/java/com/example/foodexpiryapp/presentation/ui/planner/PlannerFragment.kt`
- Blocks: The UX plan's goal of "Recipes act as a food rescue helper" and seamlessly connecting inventory to meal slots.

**Shopping Templates & History:**
- Problem: The Shopping module has no concept of common templates (weekly restock, breakfast essentials) or batch-adding from previous history.
- Files: `app/src/main/java/com/example/foodexpiryapp/presentation/ui/shopping/ShoppingFragment.kt`, `app/src/main/res/layout/fragment_shopping.xml`
- Blocks: Efficient replenishment planning.

## Privacy & Trust

**Missing Data Usage Explanations:**
- Risk: No "Privacy" or "Data Usage" explanations exist in the Profile screens, especially critical for an app relying on local/AI camera scanning.
- Files: `app/src/main/java/com/example/foodexpiryapp/presentation/ui/profile/ProfileSettingsFragment.kt`, `app/src/main/res/layout/fragment_profile.xml`
- Current mitigation: None.
- Recommendations: Add a clear data usage and privacy section explaining local storage, AI usage, and giving users a clear way to delete data.

---

*Concerns audit: 2026-04-08*
