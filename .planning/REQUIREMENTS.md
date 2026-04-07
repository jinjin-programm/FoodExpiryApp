# Requirements

## v1 Requirements

### Inventory & Onboarding
- [ ] **INVN-01**: Display a clear "Get Started" entry point on the main inventory screen for immediate onboarding.
- [ ] **INVN-02**: Support "Photo Scan" as the primary input method with an OCR confirmation flow before saving.
- [ ] **INVN-03**: Display "Expiring Soon" section prominently, sorted by risk level (already expired, 1 day, 2-3 days, 4-7 days).
- [ ] **INVN-04**: Inventory search must operate on the current tab's content only.
- [ ] **INVN-05**: Provide quick actions dropdown (Photo scan, Manual entry, View expiring, View analysis).

### Data Model
- [ ] **DATA-01**: Implement Unified Food Data Model (name, category, qty, purchase/expiry date, scan source, confidence, notes, risk, recipe relevance).

### Recipes
- [ ] **RECP-01**: Recommend recipes based specifically on soon-to-expire ingredients.
- [ ] **RECP-02**: Display concrete performance indicators on recipe cards (e.g., ingredients rescued this week).

### Planner
- [ ] **PLAN-01**: Support adding a chosen recipe to a specific date and meal slot (breakfast, lunch, dinner, snack).

### Shopping
- [ ] **SHOP-01**: Support common shopping list templates (e.g., weekly restock, breakfast essentials).
- [ ] **SHOP-02**: Connect shopping items with inventory data to show items added, purchased, and remaining.

### Profile & Settings
- [ ] **PROF-01**: Place notification settings near the top of the Profile module to emphasize food protection.
- [ ] **PROF-02**: Add clear privacy and data usage explanations (how AI is handled, local vs online).
- [ ] **PROF-03**: Make Google Sign-In an optional choice rather than an entry barrier.

## Out of Scope
- **Full App Redesign**: The goal is UX optimization, not changing the visual identity. Maintain current card layouts.
- **Complex 2-way Planner/Recipe Sync**: Planner automatically pushing items to recipes is deferred to avoid complexity in this milestone.
- **Flash Mode in Scanning**: Flash can be removed if it does not add value to the photo scan flow.